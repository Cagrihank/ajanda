package com.example.ajanda.ink

import android.graphics.Color
import android.graphics.pdf.PdfDocument
import com.example.ajanda.data.NotePage
import com.example.ajanda.data.Repo
import java.io.OutputStream

/**
 * Notu vektör PDF olarak yazar: çizgiler, şekiller ve metinler PDF'e yol/metin olarak geçer,
 * büyütünce bulanıklaşmaz. Sayfa genişliği A4 yatay (842 nokta) olacak şekilde ölçeklenir.
 */
object PdfExport {
    fun write(note: NotePage, out: OutputStream, widthDp: Float, heightDp: Float, withPaper: Boolean = true) {
        val sheets = (0 until note.pageCount).map { Repo.loadStrokes(note.id, it) }
        // Sayfa boyutu: ekran alanı ya da (kaydırılmış) içerik, hangisi büyükse
        var wDp = if (widthDp > 50f) widthDp else 1000f
        var hDp = if (heightDp > 50f) heightDp else 620f
        sheets.forEach { list ->
            InkRenderer.contentBounds(list)?.let { b ->
                wDp = maxOf(wDp, b.right + 40f)
                hDp = maxOf(hDp, b.bottom + 40f)
            }
        }
        val k = 842f / wDp
        val pw = 842
        val ph = (hDp * k).toInt().coerceAtLeast(100)
        val doc = PdfDocument()
        try {
            sheets.forEachIndexed { i, list ->
                val page = doc.startPage(PdfDocument.PageInfo.Builder(pw, ph, i + 1).create())
                val c = page.canvas
                c.drawColor(Color.WHITE)
                if (withPaper) InkRenderer.drawPaper(c, 0f, 0f, pw.toFloat(), ph.toFloat(), note.paper, k)
                list.forEach { InkRenderer.drawStroke(c, it, k) }
                doc.finishPage(page)
            }
            doc.writeTo(out)
        } finally {
            doc.close()
        }
    }
}
