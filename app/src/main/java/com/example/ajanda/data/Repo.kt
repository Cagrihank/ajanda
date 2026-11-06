package com.example.ajanda.data

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

val DEFAULT_STATUSES = listOf("Açık", "Devam ediyor", "Beklemede", "Kapalı")

/** Tüm veriler tablette, uygulamanın kendi klasöründe saklanır. */
object Repo {
    private lateinit var dir: File

    val tasks = mutableStateListOf<Task>()
    val events = mutableStateListOf<AgendaEvent>()
    val trackLists = mutableStateListOf<TrackList>()
    val trackItems = mutableStateListOf<TrackItem>()
    val pages = mutableStateListOf<NotePage>()

    private var lastId = 0L

    fun newId(): Long {
        val t = System.currentTimeMillis()
        lastId = if (t > lastId) t else lastId + 1
        return lastId
    }

    fun init(ctx: Context) {
        if (::dir.isInitialized) return
        dir = File(ctx.filesDir, "data").apply { mkdirs() }

        readArr("tasks.json").forEachObj { o ->
            tasks.add(
                Task(
                    id = o.getLong("id"),
                    title = o.optString("title"),
                    note = o.optString("note"),
                    dueEpochDay = o.longOrNull("due"),
                    priority = o.optInt("pri", 1),
                    done = o.optBoolean("done"),
                    createdAt = o.optLong("created")
                )
            )
        }
        readArr("events.json").forEachObj { o ->
            events.add(
                AgendaEvent(
                    id = o.getLong("id"),
                    title = o.optString("title"),
                    epochDay = o.getLong("day"),
                    startMin = o.intOrNull("start"),
                    endMin = o.intOrNull("end"),
                    note = o.optString("note")
                )
            )
        }
        readArr("tracklists.json").forEachObj { o ->
            val st = o.optJSONArray("statuses")
            val statuses = if (st == null) DEFAULT_STATUSES else List(st.length()) { st.getString(it) }
            trackLists.add(TrackList(o.getLong("id"), o.optString("name"), statuses))
        }
        readArr("trackitems.json").forEachObj { o ->
            trackItems.add(
                TrackItem(
                    id = o.getLong("id"),
                    listId = o.getLong("list"),
                    title = o.optString("title"),
                    status = o.optString("status"),
                    note = o.optString("note"),
                    dueEpochDay = o.longOrNull("due"),
                    updatedAt = o.optLong("updated")
                )
            )
        }
        readArr("pages.json").forEachObj { o ->
            pages.add(
                NotePage(
                    id = o.getLong("id"),
                    title = o.optString("title"),
                    paper = o.optInt("paper", 2),
                    updatedAt = o.optLong("updated"),
                    pageCount = o.optInt("count", 1).coerceAtLeast(1)
                )
            )
        }

        if (trackLists.isEmpty()) {
            trackLists.add(TrackList(newId(), "Genel", DEFAULT_STATUSES))
            writeTrackLists()
        }
    }

    // ---------- Görevler
    fun saveTask(t: Task) { tasks.upsert(t) { it.id }; writeTasks() }
    fun deleteTask(id: Long) { tasks.removeAll { it.id == id }; writeTasks() }

    private fun writeTasks() = write("tasks.json", JSONArray().apply {
        tasks.forEach { t ->
            put(JSONObject().apply {
                put("id", t.id); put("title", t.title); put("note", t.note)
                put("due", t.dueEpochDay ?: JSONObject.NULL); put("pri", t.priority)
                put("done", t.done); put("created", t.createdAt)
            })
        }
    })

    // ---------- Ajanda
    fun saveEvent(e: AgendaEvent) { events.upsert(e) { it.id }; writeEvents() }
    fun deleteEvent(id: Long) { events.removeAll { it.id == id }; writeEvents() }

    private fun writeEvents() = write("events.json", JSONArray().apply {
        events.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id); put("title", e.title); put("day", e.epochDay)
                put("start", e.startMin ?: JSONObject.NULL); put("end", e.endMin ?: JSONObject.NULL)
                put("note", e.note)
            })
        }
    })

    // ---------- Takip
    fun saveTrackList(l: TrackList) { trackLists.upsert(l) { it.id }; writeTrackLists() }
    fun deleteTrackList(id: Long) {
        trackLists.removeAll { it.id == id }
        trackItems.removeAll { it.listId == id }
        writeTrackLists(); writeTrackItems()
    }
    fun saveTrackItem(i: TrackItem) { trackItems.upsert(i.copy(updatedAt = System.currentTimeMillis())) { it.id }; writeTrackItems() }
    fun deleteTrackItem(id: Long) { trackItems.removeAll { it.id == id }; writeTrackItems() }

    private fun writeTrackLists() = write("tracklists.json", JSONArray().apply {
        trackLists.forEach { l ->
            put(JSONObject().apply {
                put("id", l.id); put("name", l.name)
                put("statuses", JSONArray().apply { l.statuses.forEach { put(it) } })
            })
        }
    })

    private fun writeTrackItems() = write("trackitems.json", JSONArray().apply {
        trackItems.forEach { i ->
            put(JSONObject().apply {
                put("id", i.id); put("list", i.listId); put("title", i.title); put("status", i.status)
                put("note", i.note); put("due", i.dueEpochDay ?: JSONObject.NULL); put("updated", i.updatedAt)
            })
        }
    })

    // ---------- Not defteri
    fun savePage(p: NotePage) { pages.upsert(p) { it.id }; writePages() }

    fun deletePage(id: Long) {
        val count = pages.firstOrNull { it.id == id }?.pageCount ?: 1
        pages.removeAll { it.id == id }
        for (i in 0 until maxOf(count, 1)) inkFile(id, i).delete()
        writePages()
    }

    fun addSheet(id: Long): Int {
        val i = pages.indexOfFirst { it.id == id }
        if (i < 0) return 0
        val p = pages[i].copy(pageCount = pages[i].pageCount + 1, updatedAt = System.currentTimeMillis())
        pages[i] = p
        writePages()
        return p.pageCount - 1
    }

    fun lastEditedNote(): NotePage? = pages.maxByOrNull { it.updatedAt }

    /** Metni bir notun son sayfasına, mevcut içeriğin altına ekler. Not yoksa "Hesaplar" notu açar. */
    fun appendTextToNote(text: String): NotePage {
        val note = lastEditedNote() ?: NotePage(newId(), "Hesaplar").also { savePage(it) }
        val sheet = note.pageCount - 1
        val list = loadStrokes(note.id, sheet).toMutableList()
        var maxY = 40f
        list.forEach { s ->
            var i = 1
            while (i < s.points.size) { if (s.points[i] > maxY) maxY = s.points[i]; i += 3 }
        }
        val size = 18f
        list.add(Stroke(0xFF1B1B1B.toInt(), size, false, floatArrayOf(48f, maxY + size * 1.8f, 0.6f), text))
        saveStrokes(note.id, sheet, list)
        return note
    }

    private fun writePages() = write("pages.json", JSONArray().apply {
        pages.forEach { p ->
            put(JSONObject().apply {
                put("id", p.id); put("title", p.title); put("paper", p.paper)
                put("updated", p.updatedAt); put("count", p.pageCount)
            })
        }
    })

    private fun inkFile(id: Long, sheet: Int) =
        File(dir, if (sheet == 0) "page_$id.ink" else "page_${id}_$sheet.ink")

    fun loadStrokes(pageId: Long, sheet: Int = 0): List<Stroke> {
        val f = inkFile(pageId, sheet)
        if (!f.exists()) return emptyList()
        return try {
            DataInputStream(f.inputStream().buffered()).use { inp ->
                val ver = inp.readInt()
                val n = inp.readInt()
                List(n) {
                    val c = inp.readInt()
                    val w = inp.readFloat()
                    val h = inp.readBoolean()
                    val m = inp.readInt()
                    val pts = FloatArray(m) { inp.readFloat() }
                    val text = if (ver >= 2 && inp.readBoolean()) inp.readUTF() else null
                    Stroke(c, w, h, pts, text)
                }
            }
        } catch (e: Exception) {
            f.copyTo(File(dir, f.name + ".bad"), overwrite = true)
            emptyList()
        }
    }

    fun saveStrokes(pageId: Long, sheet: Int, strokes: List<Stroke>) {
        val target = inkFile(pageId, sheet)
        val tmp = File(dir, target.name + ".tmp")
        DataOutputStream(tmp.outputStream().buffered()).use { out ->
            out.writeInt(2)
            out.writeInt(strokes.size)
            strokes.forEach { s ->
                out.writeInt(s.color); out.writeFloat(s.width); out.writeBoolean(s.highlighter)
                out.writeInt(s.points.size)
                s.points.forEach { out.writeFloat(it) }
                out.writeBoolean(s.text != null)
                if (s.text != null) out.writeUTF(s.text)
            }
        }
        replace(tmp, target)
        val i = pages.indexOfFirst { it.id == pageId }
        if (i >= 0) { pages[i] = pages[i].copy(updatedAt = System.currentTimeMillis()); writePages() }
    }

    // ---------- Yardımcılar
    private fun readArr(name: String): JSONArray {
        val f = File(dir, name)
        if (!f.exists()) return JSONArray()
        return try {
            JSONArray(f.readText())
        } catch (e: Exception) {
            f.copyTo(File(dir, "$name.bad"), overwrite = true)
            JSONArray()
        }
    }

    private fun write(name: String, arr: JSONArray) {
        val tmp = File(dir, "$name.tmp")
        tmp.writeText(arr.toString())
        replace(tmp, File(dir, name))
    }

    private fun replace(tmp: File, target: File) {
        if (!tmp.renameTo(target)) {
            target.delete()
            tmp.renameTo(target)
        }
    }
}

private inline fun <T> MutableList<T>.upsert(item: T, id: (T) -> Long) {
    val i = indexOfFirst { id(it) == id(item) }
    if (i >= 0) this[i] = item else add(item)
}

private fun JSONObject.longOrNull(k: String): Long? = if (has(k) && !isNull(k)) getLong(k) else null
private fun JSONObject.intOrNull(k: String): Int? = if (has(k) && !isNull(k)) getInt(k) else null
private inline fun JSONArray.forEachObj(f: (JSONObject) -> Unit) {
    for (i in 0 until length()) f(getJSONObject(i))
}
