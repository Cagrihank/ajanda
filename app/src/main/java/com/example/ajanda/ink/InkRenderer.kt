package com.example.ajanda.ink

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.ajanda.data.Stroke

/** Çizimleri ekrana ve PDF'e aynı şekilde (vektör) çizer. k: dp → piksel/nokta ölçeği. */
object InkRenderer {
    const val PAPER_COLOR = 0xFFFBF8F1.toInt()
    const val GRID_DP = 32f

    private val seg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val hl = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE6E1D6.toInt() }
    private val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF2B8B5.toInt() }

    fun pf(p: Float) = 0.35f + 0.9f * p.coerceIn(0f, 1f)

    fun drawSeg(c: Canvas, color: Int, w: Float, x0: Float, y0: Float, p0: Float, x1: Float, y1: Float, p1: Float, k: Float) {
        seg.color = color
        seg.strokeWidth = w * k * pf((p0 + p1) / 2f)
        c.drawLine(x0 * k, y0 * k, x1 * k, y1 * k, seg)
    }

    fun drawStroke(c: Canvas, s: Stroke, k: Float) {
        val p = s.points
        val n = p.size / 3
        if (n == 0) return
        val text = s.text
        if (text != null) {
            txt.color = s.color
            txt.textSize = s.width * k
            text.split('\n').forEachIndexed { i, line ->
                c.drawText(line, p[0] * k, (p[1] + i * s.width * 1.25f) * k, txt)
            }
            return
        }
        if (s.highlighter) {
            hl.color = s.color; hl.alpha = 90; hl.strokeWidth = s.width * k
            val path = Path()
            path.moveTo(p[0] * k, p[1] * k)
            if (n == 1) path.lineTo(p[0] * k + 0.1f, p[1] * k)
            for (i in 1 until n) path.lineTo(p[i * 3] * k, p[i * 3 + 1] * k)
            c.drawPath(path, hl)
            return
        }
        if (n == 1) {
            seg.color = s.color
            seg.strokeWidth = s.width * k * pf(p[2])
            c.drawPoint(p[0] * k, p[1] * k, seg)
            return
        }
        for (i in 1 until n) {
            val a = (i - 1) * 3
            val b = i * 3
            drawSeg(c, s.color, s.width, p[a], p[a + 1], p[a + 2], p[b], p[b + 1], p[b + 2], k)
        }
    }

    /** Kâğıt çizgilerini (l,t,r,b) alanına çizer; koordinatlar sayfa pikseli (dp × k). */
    fun drawPaper(c: Canvas, l: Float, t: Float, r: Float, b: Float, paper: Int, k: Float) {
        if (paper == 0) return
        val s = GRID_DP * k
        linePaint.strokeWidth = maxOf(1f, 0.5f * k)
        val minY = if (paper == 1) s * 2 else s
        var y = maxOf(minY, kotlin.math.ceil(t / s) * s)
        while (y < b) { c.drawLine(l, y, r, y, linePaint); y += s }
        if (paper == 2) {
            var x = maxOf(s, kotlin.math.ceil(l / s) * s)
            while (x < r) { c.drawLine(x, maxOf(0f, t), x, b, linePaint); x += s }
        } else {
            marginPaint.strokeWidth = maxOf(1f, 0.75f * k)
            if (64f * k in l..r) c.drawLine(64f * k, maxOf(0f, t), 64f * k, b, marginPaint)
        }
    }

    /** Tüm öğelerin dp cinsinden kapsayan alanı (boşsa null). */
    fun contentBounds(list: List<Stroke>): RectF? {
        if (list.isEmpty()) return null
        val r = RectF(bounds(list[0]))
        list.forEach { r.union(bounds(it)) }
        return r
    }

    /** Öğenin dp cinsinden sınırları. */
    fun bounds(s: Stroke): RectF {
        val p = s.points
        val text = s.text
        if (text != null && p.size >= 2) {
            val lines = text.split('\n')
            val maxLen = lines.maxOf { it.length }
            return RectF(p[0], p[1] - s.width, p[0] + maxLen * s.width * 0.6f, p[1] + (lines.size - 1) * s.width * 1.25f + s.width * 0.3f)
        }
        val r = RectF(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
        var i = 0
        while (i + 1 < p.size) {
            r.left = minOf(r.left, p[i]); r.right = maxOf(r.right, p[i])
            r.top = minOf(r.top, p[i + 1]); r.bottom = maxOf(r.bottom, p[i + 1])
            i += 3
        }
        val half = s.width / 2f
        r.inset(-half, -half)
        return r
    }
}
