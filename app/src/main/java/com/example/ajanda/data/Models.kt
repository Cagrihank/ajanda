package com.example.ajanda.data

data class Task(
    val id: Long,
    val title: String,
    val note: String = "",
    val dueEpochDay: Long? = null,
    val priority: Int = 1, // 0 düşük, 1 normal, 2 yüksek
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class AgendaEvent(
    val id: Long,
    val title: String,
    val epochDay: Long,
    val startMin: Int? = null,
    val endMin: Int? = null,
    val note: String = ""
)

data class TrackList(
    val id: Long,
    val name: String,
    val statuses: List<String>
)

data class TrackItem(
    val id: Long,
    val listId: Long,
    val title: String,
    val status: String,
    val note: String = "",
    val dueEpochDay: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/** Bir not; birden fazla sayfası olabilir. */
data class NotePage(
    val id: Long,
    val title: String,
    val paper: Int = 2, // 0 düz, 1 çizgili, 2 kareli
    val updatedAt: Long = System.currentTimeMillis(),
    val pageCount: Int = 1
)

/**
 * Çizim öğesi. points: x, y, basınç üçlüleri (dp).
 * text doluysa bu bir metin öğesidir: points[0..1] taban çizgisi başlangıcı, width yazı boyu (dp).
 */
class Stroke(
    val color: Int,
    val width: Float,
    val highlighter: Boolean,
    val points: FloatArray,
    val text: String? = null
)
