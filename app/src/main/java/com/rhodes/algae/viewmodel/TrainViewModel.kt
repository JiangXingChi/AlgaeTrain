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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader

class TrainViewModel(application: Application) : AndroidViewModel(application) {

    // ── persistent state via SharedPreferences ──
    private val prefs = application.getSharedPreferences("algae_train", Context.MODE_PRIVATE)

    var allItems: List<AlgaeItem> = emptyList()
        private set

    var remaining by mutableStateOf<List<AlgaeItem>>(emptyList())
        private set
    var currentItem by mutableStateOf<AlgaeItem?>(null)
        private set
    var flipped by mutableStateOf(false)
        private set

    // undo history
    private data class MarkSnap(val item: AlgaeItem, val wasKnown: Boolean,
                                 val wasErrCount: Int, val markedKnown: Boolean)
    private val markHistory = mutableListOf<MarkSnap>()
    val canUndo: Boolean get() = markHistory.isNotEmpty()

    var seenCount by mutableIntStateOf(0)
        private set
    var knownCount by mutableIntStateOf(0)
        private set

    // filter state
    var filter by mutableStateOf("all")
        private set   // "all" | "unknown" | "known"
    var phylumFilter by mutableStateOf<String?>(null)
        private set

    // ── load ──
    fun loadData() {
        if (allItems.isNotEmpty()) return
        try {
            val reader = InputStreamReader(getApplication<Application>().assets.open("algae_data.json"))
            val root = JSONObject(reader.readText())
            val arr = root.getJSONArray("items")
            val list = mutableListOf<AlgaeItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(AlgaeItem(
                    id = o.getString("id"),
                    file = o.getString("file"),
                    phylum = o.getString("phylum"),
                    phylumLatin = o.optString("phylumLatin", ""),
                    genus = o.getString("genus"),
                    genusLatin = o.optString("genusLatin", ""),
                    number = o.getInt("number"),
                    known = isKnown(o.getString("id"))
                ))
            }
            allItems = list
            initPool()
        } catch (e: Exception) {
            Log.e("AlgaeTrain", "Failed to load data", e)
        }
    }

    // ── SharedPreferences helpers ──
    fun isKnown(id: String): Boolean = prefs.getBoolean("k_$id", false)
    private fun setKnown(id: String, v: Boolean) = prefs.edit().putBoolean("k_$id", v).apply()
    fun errCount(id: String): Int = prefs.getInt("e_$id", 0)
    private fun addErr(id: String) {
        prefs.edit().putInt("e_$id", errCount(id) + 1).apply()
    }

    // ── pool management ──
    private fun buildPool(): List<AlgaeItem> {
        var p = allItems
        p = when (filter) {
            "known" -> p.filter { isKnown(it.id) }
            "unknown" -> p.filter { !isKnown(it.id) }
            else -> p
        }
        phylumFilter?.let { pf -> p = p.filter { it.phylum == pf } }
        return p
    }

    private fun initPool() {
        val saved = loadRemaining()
        if (saved != null && saved.isNotEmpty()) {
            remaining = saved
        } else {
            val pool = buildPool()
            remaining = pool.filter { !isKnown(it.id) }.shuffled() +
                        pool.filter { isKnown(it.id) }.shuffled()
        }
        updateStats()
        nextCard()
    }

    private fun loadRemaining(): List<AlgaeItem>? {
        val ids = prefs.getString("remaining_ids", null) ?: return null
        val idList = try {
            JSONArray(ids).let { arr -> (0 until arr.length()).map { arr.getString(it) } }
        } catch (_: Exception) { return null }
        return idList.mapNotNull { id -> allItems.find { it.id == id } }
    }

    private fun saveRemaining() {
        val arr = JSONArray(remaining.map { it.id })
        prefs.edit().putString("remaining_ids", arr.toString()).apply()
    }

    fun clearSavedRemaining() {
        prefs.edit().remove("remaining_ids").apply()
    }

    // ── card flow ──
    fun nextCard() {
        if (remaining.isEmpty()) {
            currentItem = null
            clearSavedRemaining()
            return
        }
        currentItem = remaining.random()
        flipped = false
    }

    fun flip() { flipped = !flipped }

    fun mark(known: Boolean) {
        val item = currentItem ?: return
        markHistory.add(MarkSnap(item, isKnown(item.id), errCount(item.id), known))
        setKnown(item.id, known)
        if (known) {
            remaining = remaining.filter { it.id != item.id }
        } else {
            addErr(item.id)
        }
        seenCount++
        updateStats()
        saveRemaining()
        nextCard()
    }

    fun undo() {
        if (markHistory.isEmpty()) return
        val snap = markHistory.removeLast()
        setKnown(snap.item.id, snap.wasKnown)
        prefs.edit().putInt("e_${snap.item.id}", snap.wasErrCount).apply()
        if (snap.markedKnown) {
            remaining = remaining + snap.item
        }
        currentItem = snap.item
        flipped = false
        seenCount--
        updateStats()
        saveRemaining()
    }

    // ── restart ──
    fun restart() {
        prefs.edit().clear().apply()
        markHistory.clear()
        val pool = buildPool()
        remaining = pool.shuffled()
        seenCount = 0
        clearSavedRemaining()
        updateStats()
        nextCard()
    }

    fun fullReset() {
        prefs.edit().clear().apply()
        remaining = allItems.shuffled()
        seenCount = 0
        clearSavedRemaining()
        updateStats()
        nextCard()
    }

    // ── filters ──
    fun selectFilter(f: String) {
        filter = f
        val pool = buildPool()
        remaining = pool.shuffled()
        seenCount = 0
        markHistory.clear()
        clearSavedRemaining()
        updateStats()
        nextCard()
    }
    fun selectPhylum(p: String?) {
        phylumFilter = p
        val pool = buildPool()
        remaining = pool.shuffled()
        seenCount = 0
        markHistory.clear()
        clearSavedRemaining()
        updateStats()
        nextCard()
    }

    // ── stats ──
    private fun updateStats() {
        knownCount = allItems.count { isKnown(it.id) }
    }

    fun poolTotal(): Int = buildPool().size
    fun poolDone(): Int = poolTotal() - remaining.size

    // ── error book ──
    var errorBookVersion by mutableIntStateOf(0)
        private set

    fun errorBookEntries(): List<Pair<AlgaeItem, Int>> =
        allItems.filter { errCount(it.id) > 0 }
            .map { it to errCount(it.id) }
            .sortedByDescending { it.second }

    fun clearErrors() {
        val editor = prefs.edit()
        allItems.forEach { editor.remove("e_${it.id}") }
        editor.apply()
        errorBookVersion++
    }
}
