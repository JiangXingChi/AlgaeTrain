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
    var currentItem by mutableStateOf<AlgaeItem?>(null); private set
    var flipped by mutableStateOf(false); private set
    private var cardQueue = emptyList<AlgaeItem>()
    private var queueDate = 0L

    // ── Undo ──
    private data class Snap(val item: AlgaeItem, val wasKnown: Boolean,
                            val wasLevel: Int, val wasDue: Long, val marked: Boolean, val wasNew: Boolean)
    private val history = mutableListOf<Snap>()
    val canUndo get() = history.isNotEmpty()

    // ── Stats ──
    var knownCount by mutableIntStateOf(0); private set
    var isLoading by mutableStateOf(false); private set

    // ── 今日任务进度 ──
    var newTotal by mutableIntStateOf(0); private set
    var newDone by mutableIntStateOf(0); private set
    var reviewTotal by mutableIntStateOf(0); private set
    var reviewDone by mutableIntStateOf(0); private set

    // ── Filters（预留未用）──
    var filter by mutableStateOf("all"); private set
    var phylumFilter by mutableStateOf<String?>(null); private set

    // ── 打卡 ──
    var checkinVersion by mutableIntStateOf(0); private set
    private var checkinCache: List<Long>? = null

    // ── Load ──
    fun loadData() {
        if (allAlgaeItems.isNotEmpty() && allZooItems.isNotEmpty()) return
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
    }

    // 重建今日队列：到期复习卡（按到期先后）+ 今日新卡（配额内），先复习后新学
    private fun rebuildDailyQueue() {
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
        val reviews = due.take(dailyQuota * reviewRatio)
        // 新卡：从未排期（d_ == 0）
        val newCards = scope.filter { dueMap[it.id] == 0L }
            .shuffled().take(dailyQuota)
        cardQueue = reviews + newCards
        queueDate = t
        newTotal = newCards.size; newDone = 0
        reviewTotal = reviews.size; reviewDone = 0
        saveQueue()
    }

    private fun loadQueue() = prefs().getString("queue_ids", null)?.let { ids ->
        try { JSONArray(ids).let { arr -> (0 until arr.length()).map { arr.getString(it) } } }
        catch (_: Exception) { null }
    }?.mapNotNull { id -> allItems.find { it.id == id } }

    private fun loadSavedQueue() {
        cardQueue = loadQueue() ?: emptyList()
        newTotal = prefs().getInt("queue_new_total", 0)
        newDone = prefs().getInt("queue_new_done", 0)
        reviewTotal = prefs().getInt("queue_review_total", 0)
        reviewDone = prefs().getInt("queue_review_done", 0)
    }

    private fun saveQueue() {
        prefs().edit()
            .putString("queue_ids", JSONArray(cardQueue.map { it.id }).toString())
            .putLong("queue_date", queueDate)
            .putInt("queue_new_total", newTotal)
            .putInt("queue_new_done", newDone)
            .putInt("queue_review_total", reviewTotal)
            .putInt("queue_review_done", reviewDone)
            .apply()
    }

    fun nextCard() {
        if (cardQueue.isEmpty()) { currentItem = null; return }
        currentItem = cardQueue.first(); flipped = false
    }

    // 再学一组：今日任务学完后，从剩余新卡（未排期）再取一组加练
    // 不影响当日打卡记录；返回实际加入的张数（0 = 图谱已全部学完）
    fun addMoreCards(): Int {
        if (currentItem != null || cardQueue.isNotEmpty()) return 0
        val mode = currentMode
        val dueMap = HashMap<String, Long>(allItems.size)
        allItems.forEach { dueMap[it.id] = prefsFor(mode).getLong("d_${it.id}", 0L) }
        val remaining = allItems.filter { dueMap[it.id] == 0L }.shuffled().take(dailyQuota)
        if (remaining.isEmpty()) return 0
        cardQueue = remaining
        queueDate = today()
        newTotal += remaining.size
        saveQueue()
        nextCard()
        return remaining.size
    }

    fun flip() { flipped = !flipped }

    fun mark(known: Boolean) {
        val item = currentItem ?: return
        val lv = level(item.id); val due = dueDayFor(currentMode, item.id)
        val isNew = lv == 0 && due == 0L
        history.add(Snap(item, isKnown(item.id), lv, due, known, isNew))
        val e = prefs().edit().putBoolean("k_${item.id}", known)
        if (known) {
            // 认识：等级 +1（封顶 5），按间隔表排下次复习
            e.putInt("s_${item.id}", (lv + 1).coerceAtMost(5))
            e.putLong("d_${item.id}", today() + INTERVAL_DAYS[lv.coerceAtMost(5)])
            cardQueue = cardQueue.drop(1)
        } else {
            // 不认识：等级归 0，明天再排，卡回队尾当天重练
            e.putInt("s_${item.id}", 0)
            e.putLong("d_${item.id}", today() + 1)
            cardQueue = cardQueue.drop(1) + item
        }
        e.apply()
        if (isNew) newDone++ else if (lv >= 1) reviewDone++
        updateStats()
        checkCheckIn()
        saveQueue(); nextCard()
    }

    fun undo() {
        if (history.isEmpty()) return
        val s = history.removeLast()
        prefs().edit()
            .putBoolean("k_${s.item.id}", s.wasKnown)
            .putInt("s_${s.item.id}", s.wasLevel)
            .putLong("d_${s.item.id}", s.wasDue)
            .apply()
        if (!s.marked) cardQueue = cardQueue.dropLast(1) // 移除"不认识"放回队尾的那份
        cardQueue = listOf(s.item) + cardQueue
        currentItem = s.item; flipped = false
        if (s.wasNew) newDone = (newDone - 1).coerceAtLeast(0)
        else if (s.wasLevel >= 1) reviewDone = (reviewDone - 1).coerceAtLeast(0)
        updateStats()
        saveQueue()
    }

    fun restart() {
        val app = getApplication<Application>()
        listOf("algae_train", "zoo_train").forEach { name ->
            app.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
        }
        history.clear()
        initQueue()
    }

    // ── 过滤器（预留未用：UI 未调用，按门类筛选整本书）──
    fun selectPhylum(p: String?) {
        phylumFilter = p
        initQueue()
    }

    // ── 打卡设置（改后立即重建今日队列，当天生效）──
    fun setQuota(q: Int) {
        val v = q.coerceIn(QUOTA_MIN, QUOTA_MAX)
        if (v == dailyQuota) return
        dailyQuota = v
        appPrefs.edit().putInt("daily_quota", v).apply()
        rebuildDailyQueue(); nextCard()
    }

    fun setRatio(r: Int) {
        if (r == reviewRatio) return
        reviewRatio = r
        appPrefs.edit().putInt("review_ratio", r).apply()
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

    // 今日新卡配额完成即打卡（复习卡不阻塞）
    private fun checkCheckIn() {
        val t = today()
        if (newTotal > 0 && newDone >= newTotal && !isCheckedIn(t)) addCheckIn(t)
    }

    // ── Stats ──
    private fun updateStats() { knownCount = allItems.count { isKnown(it.id) } }
}
