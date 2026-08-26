package com.rhodes.algae.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.rhodes.algae.data.AlgaeItem
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class TrainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        // 间隔表：等级 0→1 天，1→2 天，2→4 天，3→7 天，4→15 天，5→30 天（艾宾浩斯遗忘曲线复习节点）
        val INTERVAL_DAYS = intArrayOf(1, 2, 4, 7, 15, 30)
        val QUOTA_OPTIONS = listOf(10, 20) // 快捷档位，另有自定义
        const val DEFAULT_QUOTA = 20
        const val QUOTA_MIN = 1
        const val QUOTA_MAX = 100
        val RATIO_OPTIONS = listOf(1, 2, 3) // 新学:复习 = 1:1 / 1:2 / 1:3
        const val DEFAULT_RATIO = 2
    }

    // ── Book / Mode ──
    var currentMode by mutableStateOf("algae"); private set
    val displayPrefix get() = if (currentMode == "algae") "images/" else "zooplankton/"
    val bookTitle get() = if (currentMode == "algae") "浮游植物图谱" else "浮游动物图谱"

    // ── Persistence ──
    private val prefsAlgae = application.getSharedPreferences("algae_train", Context.MODE_PRIVATE)
    private val prefsZoo = application.getSharedPreferences("zoo_train", Context.MODE_PRIVATE)
    private val appPrefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    private fun prefsFor(mode: String) = if (mode == "algae") prefsAlgae else prefsZoo
    private fun prefs() = prefsFor(currentMode)
    private fun today() = LocalDate.now().toEpochDay()

    // ── 打卡设置 ──
    var dailyQuota by mutableIntStateOf(appPrefs.getInt("daily_quota", DEFAULT_QUOTA)); private set
    var reviewRatio by mutableIntStateOf(appPrefs.getInt("review_ratio", DEFAULT_RATIO)); private set
    val bookSelected get() = appPrefs.contains("selected_book")

    // ── Data ──
    var allAlgaeItems = emptyList<AlgaeItem>(); private set
    var allZooItems = emptyList<AlgaeItem>(); private set
    val allItems get() = if (currentMode == "algae") allAlgaeItems else allZooItems
    private var phylumCountAlgae = 0
    private var phylumCountZoo = 0
    val currentPhylumCount get() = if (currentMode == "algae") phylumCountAlgae else phylumCountZoo
    fun phylumCountFor(mode: String) = if (mode == "algae") phylumCountAlgae else phylumCountZoo

    // ── Card state ──
    // 今日两组：先复习组（到期卡 配额×比例），后新卡组（未学卡 配额）
    // 组内「不认识」回本组队尾重练，认完（点认识）才出组
    // 加练两队列：每日任务学完后手动追加，独立计数不占每日配额口径
    private var reviewQueue = emptyList<AlgaeItem>()
    private var newQueue = emptyList<AlgaeItem>()
    private var extraReviewQueue = emptyList<AlgaeItem>()
    private var extraNewQueue = emptyList<AlgaeItem>()
    private var queueDate = 0L

    // ── Undo ──
    // 组别：0=今日复习 1=今日新卡 2=加练复习 3=加练新卡
    private data class Snap(val item: AlgaeItem, val wasKnown: Boolean,
                            val wasLevel: Int, val wasDue: Long,
                            val marked: Boolean, val group: Int)
    private val history = mutableListOf<Snap>()
    val canUndo get() = history.isNotEmpty()

    // ── Card state ──
    var currentItem by mutableStateOf<AlgaeItem?>(null); private set
    var flipped by mutableStateOf(false); private set

    // ── Stats ──
    var knownCount by mutableIntStateOf(0); private set
    var isLoading by mutableStateOf(false); private set

    // ── 今日任务进度 ──
    var newTotal by mutableIntStateOf(0); private set
    var newDone by mutableIntStateOf(0); private set
    var reviewTotal by mutableIntStateOf(0); private set
    var reviewDone by mutableIntStateOf(0); private set
    // ── 加练计数（再学一组/再复习一组）：独立于每日配额口径 ──
    var extraNewTotal by mutableIntStateOf(0); private set
    var extraNewDone by mutableIntStateOf(0); private set
    var extraReviewTotal by mutableIntStateOf(0); private set
    var extraReviewDone by mutableIntStateOf(0); private set

    // 组别 → (队列, 计数器) 统一访问：消除散落各处的 when 分支
    private fun queueOf(group: Int) = when (group) {
        0 -> reviewQueue; 1 -> newQueue; 2 -> extraReviewQueue; else -> extraNewQueue
    }

    private fun setQueue(group: Int, q: List<AlgaeItem>) {
        when (group) {
            0 -> reviewQueue = q; 1 -> newQueue = q
            2 -> extraReviewQueue = q; else -> extraNewQueue = q
        }
    }

    private fun bumpDone(group: Int, delta: Int) {
        when (group) {
            0 -> reviewDone += delta
            1 -> newDone += delta
            2 -> extraReviewDone = (extraReviewDone + delta).coerceAtLeast(0)
            else -> extraNewDone = (extraNewDone + delta).coerceAtLeast(0)
        }
    }

    // ── Filters（预留未用）──
    var filter by mutableStateOf("all"); private set
    var phylumFilter by mutableStateOf<String?>(null); private set

    // 今日训练进度（训练页显示）
    val queueRemaining get() = reviewQueue.size + newQueue.size + extraReviewQueue.size + extraNewQueue.size
    val currentGroupLabel: String? get() = when {
        reviewQueue.isNotEmpty() -> "今日复习"
        newQueue.isNotEmpty() -> "今日新卡"
        // 每日配额完成后队列里剩下的都是加练组
        extraReviewQueue.isNotEmpty() -> "加练复习"
        else -> "加练新卡"
    }

    // ── 打卡 ──
    var checkinVersion by mutableIntStateOf(0); private set
    private var checkinCache: List<Long>? = null

    // ── Load ──
    fun loadData() {
        if (allAlgaeItems.isNotEmpty()) return // 动物书缺失时不重复解析植物书
        isLoading = true
        try {
            // 恢复上次选择的图谱书（否则默认植物书）
            currentMode = appPrefs.getString("selected_book", "algae") ?: "algae"
            val app = getApplication<Application>()
            allAlgaeItems = parseItems(app.assets.open("algae_data.json").reader().readText(), "algae")
            phylumCountAlgae = allAlgaeItems.map { it.phylum }.distinct().size
            try {
                allZooItems = parseItems(app.assets.open("zooplankton_data.json").reader().readText(), "zooplankton")
                phylumCountZoo = allZooItems.map { it.phylum }.distinct().size
            } catch (_: Exception) { Log.w("识浮游", "zooplankton_data.json missing") }
            initQueue()
            // 清理 90 天前的按书记账（防无限增长）
            val cutoff = today() - 90
            appPrefs.all.keys.filter { key ->
                (key.startsWith("checkin_done_") || key.startsWith("checkin_sealed_")) &&
                    (key.substringAfterLast('_').toLongOrNull() ?: 0L) < cutoff
            }.forEach { appPrefs.edit().remove(it).apply() }
        } catch (e: Exception) { Log.e("识浮游", "Load failed", e) }
        finally { isLoading = false }
    }

    private fun parseItems(json: String, mode: String) = JSONObject(json).getJSONArray("items").let { arr ->
        (0 until arr.length()).map { arr.getJSONObject(it).run {
            val id = getString("id")
            migrateLegacy(id, mode)
            AlgaeItem(id, getString("file"), getString("phylum"),
                optString("phylumLatin"), getString("genus"), optString("genusLatin"),
                getInt("number"))
        }}
    }

    // 旧版数据迁移：V0.3.1 及更早的"已掌握"标记 → SRS 初始 1 级（今天复习）
    // 注：V0.3.1 浮游动物的 known 被误写入 algae_train，迁移时从那里读取
    private fun migrateLegacy(id: String, mode: String) {
        val p = prefsFor(mode)
        if (p.contains("s_$id")) return
        val known = if (mode == "zooplankton") prefsAlgae.getBoolean("k_$id", false)
                    else p.getBoolean("k_$id", false)
        if (known) p.edit().putInt("s_$id", 1).putLong("d_$id", today()).apply()
    }

    fun switchMode(mode: String) {
        if (mode == currentMode) { appPrefs.edit().putString("selected_book", mode).apply(); return }
        currentMode = mode
        appPrefs.edit().putString("selected_book", mode).apply()
        history.clear()
        initQueue()
    }

    // ── SRS 状态（按书隔离）──
    fun level(id: String) = prefsFor(currentMode).getInt("s_$id", 0)
    fun dueDayFor(mode: String, id: String) = prefsFor(mode).getLong("d_$id", 0L)

    // ── 掌握 ──
    fun isKnownFor(mode: String, id: String) = prefsFor(mode).getBoolean("k_$id", false)
    fun isKnown(id: String) = prefsFor(currentMode).getBoolean("k_$id", false)

    // ── 每日队列调度 ──
    private fun initQueue() {
        val t = today()
        if (prefs().getLong("queue_date", 0L) == t) {
            loadSavedQueue()
        } else {
            rebuildDailyQueue()
        }
        updateStats()
        nextCard()
        sealIdleBooks() // 空档日兜底：loadData / 切书 / 重置进度三条路统一覆盖
    }

    // ── 跨天守卫：App 常驻内存过夜后，任何训练操作先把队列滚到今天 ──
    private fun ensureToday() {
        if (queueDate != today()) {
            rebuildDailyQueue()
            nextCard() // 重建后必须刷新当前卡，否则 UI 停留在旧的完成态
            sealIdleBooks() // 跨天后的空档日也要重新兜底，否则零操作日断签
        }
    }

    // 空档日兜底：学完且当日既无新卡也无到期复习的书，视为当日达标。
    // 否则「零操作日」（没卡可练自然不会触发 mark）会误断连续打卡
    private fun sealIdleBooks() {
        val t = today()
        for ((mode, items) in listOf("algae" to allAlgaeItems, "zooplankton" to allZooItems)) {
            if (items.isEmpty()) continue
            val p = prefsFor(mode)
            val hasNew = items.any { p.getLong("d_${it.id}", 0L) == 0L }
            val hasDue = items.any { val d = p.getLong("d_${it.id}", 0L); d != 0L && d <= t }
            if (!hasNew && !hasDue) {
                val done = bookSet("checkin_done", t)
                if (mode !in done) {
                    done.add(mode)
                    appPrefs.edit().putStringSet(dayKey("checkin_done", t), done).apply()
                }
            }
        }
        if (!isCheckedIn(t) && bookSet("checkin_done", t).isNotEmpty()) addCheckIn(t)
    }

    // 重建今日队列：到期复习卡（按到期先后）+ 今日新卡（配额内），先复习后新学
    private fun rebuildDailyQueue() {
        history.clear() // 队列已换新（跨天/改设置），旧撤销快照会污染新队列
        val t = today()
        val mode = currentMode
        // 范围：整本书，或按过滤条件（预留：filter / phylumFilter）
        var scope = allItems
        scope = when (filter) {
            "known" -> scope.filter { isKnown(it.id) }
            "unknown" -> scope.filter { !isKnown(it.id) }
            else -> scope
        }
        phylumFilter?.let { pf -> scope = scope.filter { it.phylum == pf } }
        // 一次性读取到期日，避免逐卡多次访问 SharedPreferences
        val dueMap = HashMap<String, Long>(scope.size)
        scope.forEach { dueMap[it.id] = prefsFor(mode).getLong("d_${it.id}", 0L) }
        // 到期卡：已排期且到期（含忘记归零的卡，等级 0 但 d_ 已设置）
        val due = scope.filter { val d = dueMap[it.id] ?: 0L; d != 0L && d <= t }
            .sortedBy { dueMap[it.id] }
        // 今日复习组：到期卡取 配额×比例；今日新卡组：未学卡取 配额。先复习后新卡
        reviewQueue = due.take(dailyQuota * reviewRatio)
        newQueue = scope.filter { dueMap[it.id] == 0L }
            .shuffled().take(dailyQuota)
        queueDate = t
        newTotal = newQueue.size; newDone = 0
        reviewTotal = reviewQueue.size; reviewDone = 0
        extraNewTotal = 0; extraNewDone = 0
        extraReviewTotal = 0; extraReviewDone = 0
        extraReviewQueue = emptyList(); extraNewQueue = emptyList()
        saveQueue()
    }

    private fun loadQueue(key: String) = prefs().getString(key, null)?.let { ids ->
        try { JSONArray(ids).let { arr -> (0 until arr.length()).map { arr.getString(it) } } }
        catch (_: Exception) { null }
    }?.mapNotNull { id -> allItems.find { it.id == id } }

    private fun loadSavedQueue() {
        // V0.6.0 起两组队列分开存；旧版单队列数据无法区分组别，直接重建
        val r = loadQueue("queue_review_ids"); val n = loadQueue("queue_new_ids")
        if (r == null || n == null) { rebuildDailyQueue(); return }
        reviewQueue = r; newQueue = n
        // 加练队列（V0.6.3 起持久化；旧数据缺失时为空不影响每日任务）
        extraReviewQueue = loadQueue("queue_extra_review_ids") ?: emptyList()
        extraNewQueue = loadQueue("queue_extra_new_ids") ?: emptyList()
        queueDate = prefs().getLong("queue_date", today()) // 恢复字段，防 saveQueue 写坏 queue_date
        newTotal = prefs().getInt("queue_new_total", 0)
        newDone = prefs().getInt("queue_new_done", 0)
        reviewTotal = prefs().getInt("queue_review_total", 0)
        reviewDone = prefs().getInt("queue_review_done", 0)
        extraNewTotal = prefs().getInt("queue_extra_total", 0)
        extraNewDone = prefs().getInt("queue_extra_done", 0)
        extraReviewTotal = prefs().getInt("queue_extra_rv_total", 0)
        extraReviewDone = prefs().getInt("queue_extra_rv_done", 0)
    }

    private fun saveQueue() {
        prefs().edit()
            .putString("queue_review_ids", JSONArray(reviewQueue.map { it.id }).toString())
            .putString("queue_new_ids", JSONArray(newQueue.map { it.id }).toString())
            .putString("queue_extra_review_ids", JSONArray(extraReviewQueue.map { it.id }).toString())
            .putString("queue_extra_new_ids", JSONArray(extraNewQueue.map { it.id }).toString())
            .putLong("queue_date", queueDate)
            .putInt("queue_new_total", newTotal)
            .putInt("queue_new_done", newDone)
            .putInt("queue_review_total", reviewTotal)
            .putInt("queue_review_done", reviewDone)
            .putInt("queue_extra_total", extraNewTotal)
            .putInt("queue_extra_done", extraNewDone)
            .putInt("queue_extra_rv_total", extraReviewTotal)
            .putInt("queue_extra_rv_done", extraReviewDone)
            .apply()
    }

    fun nextCard() {
        val next = reviewQueue.firstOrNull() ?: newQueue.firstOrNull()
            ?: extraReviewQueue.firstOrNull() ?: extraNewQueue.firstOrNull()
        if (next == null) { currentItem = null; return }
        currentItem = next; flipped = false
    }

    // 加练通用：每日任务学完后从取卡函数追加一组；独立计数，不影响当日打卡记录
    private fun appendExtra(take: (HashMap<String, Long>) -> List<AlgaeItem>,
                            onAppend: (List<AlgaeItem>) -> Unit): Int {
        ensureToday() // 过夜后先滚到今天，避免以昨日状态为基底加练
        if (currentItem != null || reviewQueue.isNotEmpty() || newQueue.isNotEmpty() ||
            extraReviewQueue.isNotEmpty() || extraNewQueue.isNotEmpty()) return 0
        val mode = currentMode
        val dueMap = HashMap<String, Long>(allItems.size)
        allItems.forEach { dueMap[it.id] = prefsFor(mode).getLong("d_${it.id}", 0L) }
        val remaining = take(dueMap)
        if (remaining.isEmpty()) return 0
        history.clear() // 加练是新一组，旧撤销记录会污染加练队列
        onAppend(remaining)
        queueDate = today()
        saveQueue()
        nextCard()
        return remaining.size
    }

    // 再学一组：从未排期的新卡中取（0 = 图谱已全部学完）
    fun addMoreCards(): Int =
        appendExtra({ dueMap -> allItems.filter { dueMap[it.id] == 0L }.shuffled().take(dailyQuota) }) { remaining ->
            extraNewQueue = remaining // 与加练复习对称：独立队列，不混入每日新卡组
            extraNewTotal += remaining.size
        }

    // 再复习一组：从今日未进入复习组的到期卡中取（0 = 没有更多到期的了）。
    // 按到期先后排序（与每日复习组一致），不随机打乱
    fun addMoreReview(): Int =
        appendExtra({ dueMap ->
            allItems.filter { val d = dueMap[it.id] ?: 0L; d != 0L && d <= today() }
                .sortedBy { dueMap[it.id] }
                .take(dailyQuota * reviewRatio)
        }) { remaining ->
            extraReviewQueue = remaining
            extraReviewTotal += remaining.size
        }

    fun flip() { flipped = !flipped }

    fun mark(known: Boolean) {
        ensureToday() // 常驻过夜后第一次操作：先把队列滚到今天
        val item = currentItem ?: return
        val group = when {
            reviewQueue.firstOrNull() == item -> 0
            newQueue.firstOrNull() == item -> 1
            extraReviewQueue.firstOrNull() == item -> 2
            else -> 3
        }
        val lv = level(item.id)
        history.add(Snap(item, isKnown(item.id), lv, dueDayFor(currentMode, item.id), known, group))
        val e = prefs().edit().putBoolean("k_${item.id}", known)
        if (known) {
            // 认识：等级 +1（封顶 5），按间隔表排下次复习，卡出组
            e.putInt("s_${item.id}", (lv + 1).coerceAtMost(5))
            e.putLong("d_${item.id}", today() + INTERVAL_DAYS[lv.coerceIn(0, 5)]) // coerceIn 防 prefs 损坏越界
            setQueue(group, queueOf(group).drop(1))
        } else {
            // 不认识：等级归 0，明天再排，卡回本组队尾当天重练（认完为止）
            e.putInt("s_${item.id}", 0)
            e.putLong("d_${item.id}", today() + 1)
            setQueue(group, queueOf(group).drop(1) + item)
        }
        e.apply()
        // 只有「认识」才计入完成进度（不认识回队尾继续练）；加练组计各自的加练数
        if (known) bumpDone(group, +1)
        updateStats()
        syncCheckIn()
        saveQueue(); nextCard()
    }

    fun undo() {
        ensureToday() // 跨天后队列已重建、历史已清空，此处自然短路
        if (history.isEmpty()) return
        val s = history.removeLast()
        prefs().edit()
            .putBoolean("k_${s.item.id}", s.wasKnown)
            .putInt("s_${s.item.id}", s.wasLevel)
            .putLong("d_${s.item.id}", s.wasDue)
            .apply()
        // 回滚队列：移除「不认识」放回队尾的那份，再把卡放回本组队首
        if (!s.marked) setQueue(s.group, queueOf(s.group).dropLast(1))
        setQueue(s.group, listOf(s.item) + queueOf(s.group))
        currentItem = s.item; flipped = false
        if (s.marked) bumpDone(s.group, -1) // 只有「认识」计过数，撤销时才回滚
        updateStats()
        syncCheckIn()
        saveQueue()
    }

    // 重置指定图谱的学习进度（打卡记录保留；非当前书不影响当前队列）
    fun restartBook(mode: String) {
        prefsFor(mode).edit().clear().apply()
        history.clear()
        // 冻结该书当天的打卡贡献：重置导致的条件回落不得回收已达成的打卡
        val t = today()
        val sealed = bookSet("checkin_sealed", t)
        if (mode !in sealed) {
            sealed.add(mode)
            appPrefs.edit().putStringSet(dayKey("checkin_sealed", t), sealed).apply()
        }
        if (mode == currentMode) initQueue()
    }

    // 重置打卡记录：清空打卡日期、连续天数与按书记账（学习进度保留）
    fun restartCheckIn() {
        val e = appPrefs.edit()
        e.remove("checkin_dates")
        appPrefs.all.keys.filter {
            it.startsWith("checkin_done_") || it.startsWith("checkin_sealed_")
        }.forEach { e.remove(it) }
        e.apply()
        checkinCache = emptyList()
        checkinVersion++
    }

    // ── 过滤器（预留未用：UI 未调用，按门类筛选整本书）──
    fun selectPhylum(p: String?) {
        phylumFilter = p
        history.clear() // 队列已重建，旧撤销记录会破坏新队列
        rebuildDailyQueue(); nextCard()
    }

    // ── 打卡设置（改后立即重建今日队列，当天生效）──
    fun setQuota(q: Int) {
        val v = q.coerceIn(QUOTA_MIN, QUOTA_MAX)
        if (v == dailyQuota) return
        dailyQuota = v
        appPrefs.edit().putInt("daily_quota", v).apply()
        history.clear() // 队列已重建，旧撤销记录会破坏新队列
        rebuildDailyQueue(); nextCard()
    }

    fun setRatio(r: Int) {
        if (r == reviewRatio) return
        reviewRatio = r
        appPrefs.edit().putInt("review_ratio", r).apply()
        history.clear() // 队列已重建，旧撤销记录会破坏新队列
        rebuildDailyQueue(); nextCard()
    }

    // ── 打卡 ──
    fun isCheckedIn(day: Long = today()) = checkedInDays().contains(day)
    fun streakDays(): Int {
        val days = checkedInDays().toSet()
        var d = today(); if (d !in days) d--
        var n = 0
        while (d in days) { n++; d-- }
        return n
    }
    // 内存缓存打卡日期（写时失效），避免重组期间反复解析 JSONArray
    fun checkedInDays(): List<Long> {
        checkinCache?.let { return it }
        return loadCheckins().also { checkinCache = it }
    }

    private fun loadCheckins(): List<Long> = appPrefs.getString("checkin_dates", null)?.let { raw ->
        try { JSONArray(raw).let { a -> (0 until a.length()).map { a.getLong(it) } } }
        catch (_: Exception) { emptyList() }
    } ?: emptyList()

    private fun addCheckIn(day: Long) {
        if (isCheckedIn(day)) return
        val list = checkedInDays().toMutableList().apply { add(day) }
        appPrefs.edit().putString("checkin_dates", JSONArray(list).toString()).apply()
        checkinCache = list
        checkinVersion++
    }

    // 当日打卡条件（派生值）：有未学新卡 → 新卡配额完成；有到期复习 → 全部完成；
    // 既无新卡也无到期复习（学完后卡片错峰排期的空档日）→ 视为达标，由 sealIdleBooks
    // 在队列初始化时自动记账，保证零操作日不断签。
    private fun todayCheckInEarned(): Boolean {
        val hasNew = allItems.any { dueDayFor(currentMode, it.id) == 0L }
        return when {
            hasNew -> newTotal > 0 && newDone >= newTotal
            reviewTotal > 0 -> reviewDone >= reviewTotal
            else -> true
        }
    }

    // 当日按书记账：
    // done   = 当前达标的书（达标回落可移出）
    // sealed = 已冻结贡献的书（重置图谱进度后，对话框承诺「打卡记录保留」，当天不再回收）
    private fun dayKey(prefix: String, day: Long) = "${prefix}_$day"
    private fun bookSet(prefix: String, day: Long): MutableSet<String> =
        appPrefs.getStringSet(dayKey(prefix, day), emptySet())?.toMutableSet() ?: mutableSetOf()

    private fun syncCheckIn() {
        val t = today()
        val done = bookSet("checkin_done", t)
        if (todayCheckInEarned()) {
            if (currentMode !in done) {
                done.add(currentMode)
                appPrefs.edit().putStringSet(dayKey("checkin_done", t), done).apply()
            }
            if (!isCheckedIn(t)) addCheckIn(t)
            return
        }
        if (currentMode !in done) return // 本书未持账：要么别的书撑着，要么本就未达标
        // 当前书达标回落（如撤销）：移出记账；仅当无任何书的贡献留存（含冻结）才回收当天打卡
        done.remove(currentMode)
        val e = appPrefs.edit()
        if (done.isEmpty()) e.remove(dayKey("checkin_done", t)) else e.putStringSet(dayKey("checkin_done", t), done)
        e.apply()
        val sealed = bookSet("checkin_sealed", t)
        if (done.isEmpty() && sealed.isEmpty() && isCheckedIn(t)) {
            val list = checkedInDays().filterNot { it == t }
            appPrefs.edit().putString("checkin_dates", JSONArray(list).toString()).apply()
            checkinCache = list
            checkinVersion++
        }
    }

    // ── Stats ──
    // 一次读取全部 prefs 统计已掌握数（避免逐卡多次 getBoolean）
    private fun updateStats() {
        knownCount = prefs().all.count { (k, v) -> k.startsWith("k_") && v == true }
    }
}
