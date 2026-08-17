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

class TrainViewModel(application: Application) : AndroidViewModel(application) {

    // ── Mode ──
    var currentMode by mutableStateOf("algae"); private set
    val displayPrefix get() = if (currentMode == "algae") "images/" else "zooplankton/"

    // ── Persistence ──
    private val prefsAlgae = application.getSharedPreferences("algae_train", Context.MODE_PRIVATE)
    private val prefsZoo = application.getSharedPreferences("zoo_train", Context.MODE_PRIVATE)
    private fun prefs() = if (currentMode == "algae") prefsAlgae else prefsZoo

    // ── Data ──
    var allAlgaeItems = emptyList<AlgaeItem>(); private set
    var allZooItems = emptyList<AlgaeItem>(); private set
    val allItems get() = if (currentMode == "algae") allAlgaeItems else allZooItems

    // ── Card state (queue-based, no random repeating) ──
    var currentItem by mutableStateOf<AlgaeItem?>(null); private set
    var flipped by mutableStateOf(false); private set
    private var cardQueue = emptyList<AlgaeItem>()

    // ── Undo ──
    private data class Snap(val item: AlgaeItem, val wasKnown: Boolean, val wasErr: Int, val marked: Boolean)
    private val history = mutableListOf<Snap>()
    val canUndo get() = history.isNotEmpty()

    // ── Stats ──
    var seenCount by mutableIntStateOf(0); private set
    var knownCount by mutableIntStateOf(0); private set
    var isLoading by mutableStateOf(false); private set

    // ── Filters ──
    var filter by mutableStateOf("all"); private set
    var phylumFilter by mutableStateOf<String?>(null); private set

    // ── Load ──
    fun loadData() {
        if (allAlgaeItems.isNotEmpty() && allZooItems.isNotEmpty()) return
        isLoading = true
        try {
            val app = getApplication<Application>()
            allAlgaeItems = parseItems(app.assets.open("algae_data.json").reader().readText())
            try {
                allZooItems = parseItems(app.assets.open("zooplankton_data.json").reader().readText())
            } catch (_: Exception) { Log.w("识浮游", "zooplankton_data.json missing") }
            initQueue()
        } catch (e: Exception) { Log.e("识浮游", "Load failed", e) }
        finally { isLoading = false }
    }

    private fun parseItems(json: String) = JSONObject(json).getJSONArray("items").let { arr ->
        (0 until arr.length()).map { arr.getJSONObject(it).run {
            AlgaeItem(getString("id"), getString("file"), getString("phylum"),
                optString("phylumLatin"), getString("genus"), optString("genusLatin"),
                getInt("number"), isKnown(getString("id")))
        }}
    }

    fun switchMode(mode: String) {
        if (mode == currentMode) return
        currentMode = mode; history.clear(); seenCount = 0; initQueue()
    }

    // ── Persistence helpers ──
    fun isKnown(id: String) = prefs().getBoolean("k_$id", false)
    private fun setKnown(id: String, v: Boolean) = prefs().edit().putBoolean("k_$id", v).apply()
    fun errCount(id: String) = prefs().getInt("e_$id", 0)
    private fun addErr(id: String) = prefs().edit().putInt("e_$id", errCount(id) + 1).apply()

    // ── Queue ──
    private fun buildPool(): List<AlgaeItem> {
        var p = allItems
        p = when (filter) { "known" -> p.filter { isKnown(it.id) }; "unknown" -> p.filter { !isKnown(it.id) }; else -> p }
        phylumFilter?.let { pf -> p = p.filter { it.phylum == pf } }
        return p
    }

    private fun initQueue() {
        val saved = loadQueue()
        cardQueue = if (saved != null && saved.isNotEmpty()) saved
        else buildPool().let { it.filter { !isKnown(it.id) }.shuffled() + it.filter { isKnown(it.id) }.shuffled() }
        updateStats(); nextCard()
    }

    private fun loadQueue() = prefs().getString("queue_ids", null)?.let { ids ->
        try { JSONArray(ids).let { arr -> (0 until arr.length()).map { arr.getString(it) } } }
        catch (_: Exception) { null }
    }?.mapNotNull { id -> allItems.find { it.id == id } }

    private fun saveQueue() {
        prefs().edit().putString("queue_ids", JSONArray(cardQueue.map { it.id }).toString()).apply()
    }

    fun nextCard() {
        if (cardQueue.isEmpty()) { currentItem = null; prefs().edit().remove("queue_ids").apply(); return }
        currentItem = cardQueue.first(); flipped = false
    }

    fun flip() { flipped = !flipped }

    fun mark(known: Boolean) {
        val item = currentItem ?: return
        history.add(Snap(item, isKnown(item.id), errCount(item.id), known))
        setKnown(item.id, known)
        if (known) {
            cardQueue = cardQueue.drop(1)
            seenCount++; updateStats()
        } else {
            addErr(item.id); errorBookVersion++
            cardQueue = cardQueue.drop(1) + item  // 不认识 → 移到队尾，不推进进度
            seenCount++
        }
        saveQueue(); nextCard()
    }

    fun undo() {
        if (history.isEmpty()) return
        val s = history.removeLast()
        setKnown(s.item.id, s.wasKnown)
        prefs().edit().putInt("e_${s.item.id}", s.wasErr).apply()
        if (!s.marked) {
            errorBookVersion++
            cardQueue = cardQueue.dropLast(1)  // 从队尾移除"不认识"放回的那份
        }
        cardQueue = listOf(s.item) + cardQueue
        currentItem = s.item; flipped = false
        seenCount = (seenCount - 1).coerceAtLeast(0)
        if (s.marked) updateStats()
        saveQueue()
    }

    fun restart() {
        val app = getApplication<Application>()
        listOf("algae_train", "zoo_train").forEach { name ->
            app.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().apply()
        }
        history.clear()
        cardQueue = buildPool().shuffled(); seenCount = 0
        prefs().edit().remove("queue_ids").apply(); updateStats(); nextCard()
    }

    fun selectPhylum(p: String?) {
        phylumFilter = p
        cardQueue = buildPool().shuffled()
        history.clear(); seenCount = 0
        prefs().edit().remove("queue_ids").apply(); updateStats(); nextCard()
    }

    // ── Stats ──
    private fun updateStats() { knownCount = allItems.count { isKnown(it.id) } }
    fun poolTotal() = buildPool().size
    fun poolDone() = poolTotal() - cardQueue.size

    // ── Error book ──
    var errorBookVersion by mutableIntStateOf(0); private set
    fun errorBook() = allItems.filter { errCount(it.id) > 0 }.map { it to errCount(it.id) }.sortedByDescending { it.second }
    fun clearErrors() {
        val app = getApplication<Application>()
        listOf("algae_train", "zoo_train").forEach { name ->
            app.getSharedPreferences(name, Context.MODE_PRIVATE).edit().also { e ->
                (allAlgaeItems + allZooItems).distinctBy { it.id }.forEach { e.remove("e_${it.id}") }
            }.apply()
        }
        errorBookVersion++
    }
}
