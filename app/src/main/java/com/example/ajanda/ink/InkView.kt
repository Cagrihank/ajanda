package com.example.ajanda.ink

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Build
import android.view.MotionEvent
import android.view.View
import com.example.ajanda.data.Stroke
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Kalemle çizim yüzeyi: kalem, fosforlu, silgi, kement (seç-taşı), metin ve temel şekiller.
 * Varsayılan olarak yalnızca kalem çizer (avuç içi reddi). Kalemin silgi ucu / yan tuşu silgi olur.
 * Koordinatlar dp cinsinden saklanır.
 */
class InkView(context: Context) : View(context) {

    enum class Tool { PEN, HIGHLIGHTER, ERASER, LASSO, PAN, TEXT, LINE, ARROW, RECT, CIRCLE, ELLIPSE, TRIANGLE }

    private fun isShape(t: Tool) = t == Tool.LINE || t == Tool.ARROW || t == Tool.RECT ||
        t == Tool.CIRCLE || t == Tool.ELLIPSE || t == Tool.TRIANGLE

    var tool = Tool.PEN
        set(v) {
            if (field != v) {
                field = v
                if (v != Tool.LASSO) clearSelection()
            }
        }
    var color = Color.BLACK
    var baseWidth = 3.5f
    var fingerDraws = false
    var paper = 2
        set(v) {
            if (field != v) { field = v; invalidate() }
        }
    var onChanged: ((List<Stroke>) -> Unit)? = null
    var onTextRequest: ((Float, Float) -> Unit)? = null
    var onZoomChanged: ((Float) -> Unit)? = null

    // Görünüm dönüşümü (piksel): ekran = sayfa × s + t
    private var zs = 1f
    private var tx = 0f
    private var ty = 0f
    // Önbellek bitmap'inin çizildiği dönüşüm
    private var bs = 1f
    private var btx = 0f
    private var bty = 0f
    private val drawMatrix = Matrix()
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    // Kaydırma / yakınlaştırma hareketi
    private var gesturing = false
    private var g0s = 1f
    private var g0tx = 0f
    private var g0ty = 0f
    private var g0cx = 0f
    private var g0cy = 0f
    private var g0span = 0f
    private var g0count = 0
    private var curIsFinger = false

    private val d = resources.displayMetrics.density
    private var strokes: MutableList<Stroke> = ArrayList()
    private val undoStack = ArrayList<List<Stroke>>()
    private val redoStack = ArrayList<List<Stroke>>()

    private var bmp: Bitmap? = null
    private var bmpCanvas: Canvas? = null

    // Aktif dokunuş
    private var activeId = -1
    private var curTool = Tool.PEN
    private var cur: FloatList? = null
    private var curColor = Color.BLACK
    private var curWidth = 3.5f
    private val curPath = Path()

    // Silgi
    private var eraseSnapshot: List<Stroke>? = null
    private var erasedAny = false
    private var eraserX = -1f
    private var eraserY = -1f
    private val eraseRadiusDp = 10f

    // Şekil (dp)
    private var sx = 0f
    private var sy = 0f
    private var ex = 0f
    private var ey = 0f

    // Metin
    private var downX = 0f
    private var downY = 0f

    // Kement
    private val lassoPts = FloatList()
    private var selected: Set<Stroke> = emptySet()
    private var selBounds: RectF? = null
    private var moving = false
    private var movedAny = false
    private var moveSnapshot: List<Stroke>? = null
    private var lastX = 0f
    private var lastY = 0f

    // Kement: döndürme
    private var rotating = false
    private var rotCx = 0f
    private var rotCy = 0f
    private var rotStart = 0f
    private var rotBase: List<Stroke> = emptyList()
    private var rotCur: List<Stroke> = emptyList()
    private val handleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = InkRenderer.PAPER_COLOR }
    private val handleRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = 0xFFF5A524.toInt() }

    private val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val eraserPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0x99777777.toInt(); strokeWidth = 2f
    }
    private val lassoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0xFF6B7280.toInt(); strokeWidth = 2f
        pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f)
    }
    private val selPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0xFFF5A524.toInt(); strokeWidth = 2.5f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    fun setStrokes(list: List<Stroke>) {
        strokes = list.toMutableList()
        undoStack.clear(); redoStack.clear()
        clearSelection()
        rebuild()
    }

    fun sizeDp(): Pair<Float, Float> = Pair(width / d, height / d)

    fun resetView() {
        zs = 1f; tx = 0f; ty = 0f
        rebuild()
        onZoomChanged?.invoke(zs)
    }

    private fun pxX(x: Float) = (x - tx) / (zs * d)
    private fun pxY(y: Float) = (y - ty) / (zs * d)

    /** Döndürme tutamacının sayfa (dp) konumu: seçimin üst ortasının biraz üstü. */
    private fun handleX(b: RectF) = b.centerX()
    private fun handleY(b: RectF) = b.top - 6f - 28f / zs

    /** Bitmap'e mevcut görünüm dönüşümüyle çizer. */
    private inline fun onBmp(f: (Canvas) -> Unit) {
        val c = bmpCanvas ?: return
        c.save(); c.translate(tx, ty); c.scale(zs, zs)
        f(c)
        c.restore()
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(ArrayList(strokes))
        strokes = undoStack.removeAt(undoStack.lastIndex).toMutableList()
        clearSelection(); rebuild(); notifyChanged()
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(ArrayList(strokes))
        strokes = redoStack.removeAt(redoStack.lastIndex).toMutableList()
        clearSelection(); rebuild(); notifyChanged()
    }

    fun clearAll() {
        if (strokes.isEmpty()) return
        pushUndo(ArrayList(strokes))
        strokes = ArrayList()
        clearSelection(); rebuild(); notifyChanged()
    }

    fun deleteSelection() {
        if (selected.isEmpty()) return
        pushUndo(ArrayList(strokes))
        val sel = selected
        strokes = strokes.filterNot { it in sel }.toMutableList()
        clearSelection(); rebuild(); notifyChanged()
    }

    fun addText(xDp: Float, yDp: Float, text: String) {
        if (text.isBlank()) return
        pushUndo(ArrayList(strokes))
        val size = 8f + baseWidth * 3f
        val s = Stroke(color, size, false, floatArrayOf(xDp, yDp + size * 0.8f, 0.6f), text.trimEnd())
        strokes.add(s)
        onBmp { InkRenderer.drawStroke(it, s, d) }
        invalidate(); notifyChanged()
    }

    private fun notifyChanged() = onChanged?.invoke(strokes.toList())

    private fun pushUndo(snapshot: List<Stroke>) {
        undoStack.add(snapshot)
        if (undoStack.size > 60) undoStack.removeAt(0)
        redoStack.clear()
    }

    private fun clearSelection() {
        selected = emptySet()
        selBounds = null
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w <= 0 || h <= 0) return
        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmpCanvas = Canvas(bmp!!)
        rebuild()
    }

    private fun rebuild() {
        val c = bmpCanvas ?: return
        bmp?.eraseColor(Color.TRANSPARENT)
        c.save(); c.translate(tx, ty); c.scale(zs, zs)
        strokes.forEach { InkRenderer.drawStroke(c, it, d) }
        c.restore()
        bs = zs; btx = tx; bty = ty
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(InkRenderer.PAPER_COLOR)
        canvas.save(); canvas.translate(tx, ty); canvas.scale(zs, zs)
        InkRenderer.drawPaper(canvas, -tx / zs, -ty / zs, (width - tx) / zs, (height - ty) / zs, paper, d)
        canvas.restore()

        bmp?.let {
            if (zs == bs && tx == btx && ty == bty) {
                canvas.drawBitmap(it, 0f, 0f, null)
            } else {
                val r = zs / bs
                drawMatrix.reset()
                drawMatrix.postScale(r, r)
                drawMatrix.postTranslate(tx - btx * r, ty - bty * r)
                canvas.drawBitmap(it, drawMatrix, bitmapPaint)
            }
        }

        if (activeId != -1 && curTool == Tool.HIGHLIGHTER && cur != null) {
            hlPaint.color = curColor; hlPaint.alpha = 90; hlPaint.strokeWidth = curWidth * d * zs
            canvas.drawPath(curPath, hlPaint)
        }
        if (activeId != -1 && curTool == Tool.ERASER && eraserX >= 0) {
            canvas.drawCircle(eraserX, eraserY, eraseRadiusDp * d * zs, eraserPaint)
        }

        canvas.save(); canvas.translate(tx, ty); canvas.scale(zs, zs)
        if (activeId != -1) {
            when {
                isShape(curTool) -> {
                    val pts = shapePoints(curTool, sx, sy, ex, ey)
                    if (pts.size >= 6) InkRenderer.drawStroke(canvas, Stroke(curColor, curWidth, false, pts), d)
                }
                curTool == Tool.LASSO && !moving && lassoPts.size >= 4 -> {
                    val path = Path()
                    path.moveTo(lassoPts.a[0] * d, lassoPts.a[1] * d)
                    var i = 2
                    while (i + 1 < lassoPts.size) { path.lineTo(lassoPts.a[i] * d, lassoPts.a[i + 1] * d); i += 2 }
                    canvas.drawPath(path, lassoPaint)
                }
            }
        }
        selBounds?.let { b ->
            canvas.drawRoundRect(
                (b.left - 6f) * d, (b.top - 6f) * d, (b.right + 6f) * d, (b.bottom + 6f) * d,
                8f * d, 8f * d, selPaint
            )
            val hx = handleX(b) * d
            val hy = handleY(b) * d
            val r = 11f / zs * d
            handleRing.strokeWidth = 2.5f / zs * d
            canvas.drawLine(hx, hy + r, hx, (b.top - 6f) * d, handleRing)
            canvas.drawCircle(hx, hy, r, handleFill)
            canvas.drawCircle(hx, hy, r, handleRing)
            canvas.drawArc(hx - r * 0.5f, hy - r * 0.5f, hx + r * 0.5f, hy + r * 0.5f, -60f, 270f, false, handleRing)
        }
        canvas.restore()
    }

    // ---------- Kaydırma / yakınlaştırma

    /** [cx, cy, span, adet] — upIndex hariç tüm parmaklar. */
    private fun measure(e: MotionEvent, upIndex: Int): FloatArray {
        var sx0 = 0f; var sy0 = 0f; var n = 0
        for (i in 0 until e.pointerCount) {
            if (i == upIndex) continue
            sx0 += e.getX(i); sy0 += e.getY(i); n++
        }
        if (n == 0) return floatArrayOf(0f, 0f, 0f, 0f)
        val cx = sx0 / n; val cy = sy0 / n
        var span = 0f
        if (n >= 2) {
            var sum = 0f
            for (i in 0 until e.pointerCount) {
                if (i == upIndex) continue
                sum += hypot(e.getX(i) - cx, e.getY(i) - cy)
            }
            span = sum / n
        }
        return floatArrayOf(cx, cy, span, n.toFloat())
    }

    private fun anchorGesture(e: MotionEvent, upIndex: Int) {
        val m = measure(e, upIndex)
        g0cx = m[0]; g0cy = m[1]; g0span = m[2]; g0count = m[3].toInt()
        g0s = zs; g0tx = tx; g0ty = ty
    }

    private fun startGesture(e: MotionEvent) {
        gesturing = true
        parent?.requestDisallowInterceptTouchEvent(true)
        anchorGesture(e, -1)
    }

    private fun handleGesture(e: MotionEvent) {
        when (e.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> anchorGesture(e, -1)
            MotionEvent.ACTION_POINTER_UP -> anchorGesture(e, e.actionIndex)
            MotionEvent.ACTION_MOVE -> {
                val m = measure(e, -1)
                val n = m[3].toInt()
                if (n == 0) return
                var ns = g0s
                if (n >= 2 && g0count >= 2 && g0span > 10f) ns = (g0s * m[2] / g0span).coerceIn(0.5f, 4f)
                val pxp = (g0cx - g0tx) / g0s
                val pyp = (g0cy - g0ty) / g0s
                zs = ns
                tx = minOf(m[0] - pxp * ns, 80f * d)
                ty = minOf(m[1] - pyp * ns, 80f * d)
                invalidate()
                onZoomChanged?.invoke(zs)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                gesturing = false
                rebuild()
            }
        }
    }

    private fun fingerCount(e: MotionEvent): Int {
        var n = 0
        for (i in 0 until e.pointerCount) if (e.getToolType(i) == MotionEvent.TOOL_TYPE_FINGER) n++
        return n
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (gesturing) { handleGesture(e); return true }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = e.actionIndex
                val tt = e.getToolType(idx)
                val isPen = tt == MotionEvent.TOOL_TYPE_STYLUS || tt == MotionEvent.TOOL_TYPE_ERASER
                if (tt == MotionEvent.TOOL_TYPE_FINGER) {
                    if (activeId != -1) {
                        // Parmakla çizerken ikinci parmak gelirse çizimi bırak, kaydırmaya geç
                        if (curIsFinger) { finish(false); startGesture(e) }
                        return true // kalemle yazarken avuç içi: yok say
                    }
                    if (!fingerDraws || fingerCount(e) >= 2) { startGesture(e); return true }
                }
                if (activeId != -1) return true
                // El aracı: kalemle de sürükleyerek kaydır
                if (tool == Tool.PAN) { startGesture(e); return true }
                if (!isPen && !(fingerDraws && tt == MotionEvent.TOOL_TYPE_FINGER)) return true
                curIsFinger = tt == MotionEvent.TOOL_TYPE_FINGER

                requestUnbufferedDispatch(e)
                parent?.requestDisallowInterceptTouchEvent(true)
                activeId = e.getPointerId(idx)
                curTool = if (tt == MotionEvent.TOOL_TYPE_ERASER ||
                    (e.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
                ) Tool.ERASER else tool
                curColor = color
                curWidth = if (curTool == Tool.HIGHLIGHTER) baseWidth * 4f else baseWidth

                val x = pxX(e.getX(idx))
                val y = pxY(e.getY(idx))
                when (curTool) {
                    Tool.ERASER -> {
                        eraseSnapshot = ArrayList(strokes)
                        erasedAny = false
                        addPoint(e.getX(idx), e.getY(idx), e.getPressure(idx), tt)
                    }
                    Tool.PEN, Tool.HIGHLIGHTER -> {
                        cur = FloatList()
                        curPath.reset()
                        addPoint(e.getX(idx), e.getY(idx), e.getPressure(idx), tt)
                    }
                    Tool.LASSO -> {
                        val b = selBounds
                        if (b != null && hypot(x - handleX(b), y - handleY(b)) <= 24f / zs) {
                            rotating = true
                            moving = false
                            movedAny = false
                            moveSnapshot = ArrayList(strokes)
                            rotCx = b.centerX(); rotCy = b.centerY()
                            rotStart = atan2(y - rotCy, x - rotCx)
                            val sel = selected
                            rotBase = strokes.filter { it in sel }
                            rotCur = rotBase
                        } else if (b != null && x >= b.left - 14f && x <= b.right + 14f && y >= b.top - 14f && y <= b.bottom + 14f) {
                            moving = true
                            movedAny = false
                            moveSnapshot = ArrayList(strokes)
                            lastX = x; lastY = y
                        } else {
                            clearSelection()
                            moving = false
                            lassoPts.clear()
                            lassoPts.add(x); lassoPts.add(y)
                        }
                    }
                    Tool.TEXT -> { downX = x; downY = y }
                    else -> { sx = x; sy = y; ex = x; ey = y }
                }
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeId == -1) return true
                val idx = e.findPointerIndex(activeId)
                if (idx < 0) return true
                val tt = e.getToolType(idx)
                when (curTool) {
                    Tool.PEN, Tool.HIGHLIGHTER, Tool.ERASER -> {
                        for (h in 0 until e.historySize) {
                            addPoint(e.getHistoricalX(idx, h), e.getHistoricalY(idx, h), e.getHistoricalPressure(idx, h), tt)
                        }
                        addPoint(e.getX(idx), e.getY(idx), e.getPressure(idx), tt)
                    }
                    Tool.LASSO -> {
                        val x = pxX(e.getX(idx))
                        val y = pxY(e.getY(idx))
                        if (rotating) {
                            rotateSelected(atan2(y - rotCy, x - rotCx) - rotStart)
                        } else if (moving) {
                            translateSelected(x - lastX, y - lastY)
                            lastX = x; lastY = y
                        } else {
                            lassoPts.add(x); lassoPts.add(y)
                        }
                    }
                    Tool.TEXT -> {}
                    else -> { ex = pxX(e.getX(idx)); ey = pxY(e.getY(idx)) }
                }
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (activeId == -1) return true
                if (e.getPointerId(e.actionIndex) != activeId) return true
                val canceled = Build.VERSION.SDK_INT >= 33 && (e.flags and MotionEvent.FLAG_CANCELED) != 0
                finish(!canceled)
            }

            MotionEvent.ACTION_CANCEL -> if (activeId != -1) finish(false)
        }
        return true
    }

    private fun addPoint(x: Float, y: Float, p: Float, toolType: Int) {
        val pr = if (toolType == MotionEvent.TOOL_TYPE_FINGER) 0.6f else p
        if (curTool == Tool.ERASER) {
            eraserX = x; eraserY = y
            eraseAt(pxX(x), pxY(y))
            return
        }
        val list = cur ?: return
        val xd = pxX(x)
        val yd = pxY(y)
        if (list.size >= 3) {
            val px = list.a[list.size - 3]
            val py = list.a[list.size - 2]
            val pp = list.a[list.size - 1]
            if (hypot(xd - px, yd - py) < 0.3f) return
            if (curTool == Tool.PEN) {
                onBmp { InkRenderer.drawSeg(it, curColor, curWidth, px, py, pp, xd, yd, pr, d) }
            } else {
                curPath.lineTo(x, y)
            }
        } else {
            if (curTool == Tool.HIGHLIGHTER) {
                curPath.moveTo(x, y)
            } else {
                onBmp {
                    InkRenderer.drawStroke(it, Stroke(curColor, curWidth, false, floatArrayOf(xd, yd, pr)), d)
                }
            }
        }
        list.add(xd); list.add(yd); list.add(pr)
    }

    private fun eraseAt(xd: Float, yd: Float) {
        val keep = strokes.filterNot { s -> hits(s, xd, yd, eraseRadiusDp + s.width / 2f) }
        if (keep.size != strokes.size) {
            if (!erasedAny) {
                eraseSnapshot?.let { pushUndo(it) }
                erasedAny = true
            }
            strokes = keep.toMutableList()
            rebuild()
        }
    }

    private fun hits(s: Stroke, x: Float, y: Float, r: Float): Boolean {
        if (s.text != null) {
            val b = InkRenderer.bounds(s)
            return x >= b.left - r && x <= b.right + r && y >= b.top - r && y <= b.bottom + r
        }
        val p = s.points
        var i = 0
        while (i + 1 < p.size) {
            if (hypot(p[i] - x, p[i + 1] - y) <= r) return true
            i += 3
        }
        return false
    }

    private fun translateSelected(dx: Float, dy: Float) {
        if (selected.isEmpty() || (dx == 0f && dy == 0f)) return
        val newSel = HashSet<Stroke>()
        val sel = selected
        strokes = strokes.map { s ->
            if (s in sel) {
                val np = s.points.copyOf()
                var i = 0
                while (i + 1 < np.size) { np[i] += dx; np[i + 1] += dy; i += 3 }
                Stroke(s.color, s.width, s.highlighter, np, s.text).also { newSel.add(it) }
            } else s
        }.toMutableList()
        selected = newSel
        selBounds?.offset(dx, dy)
        movedAny = true
        rebuild()
    }

    private fun rotateSelected(a: Float) {
        if (rotBase.isEmpty()) return
        val ca = cos(a)
        val sa = sin(a)
        val newCur = rotBase.map { s ->
            val np = s.points.copyOf()
            var i = 0
            while (i + 1 < np.size) {
                val dx = np[i] - rotCx
                val dy = np[i + 1] - rotCy
                np[i] = rotCx + dx * ca - dy * sa
                np[i + 1] = rotCy + dx * sa + dy * ca
                i += 3
            }
            Stroke(s.color, s.width, s.highlighter, np, s.text)
        }
        val map = HashMap<Stroke, Stroke>()
        rotCur.forEachIndexed { i, s -> map[s] = newCur[i] }
        strokes = strokes.map { map[it] ?: it }.toMutableList()
        rotCur = newCur
        selected = newCur.toSet()
        val r = RectF(InkRenderer.bounds(newCur[0]))
        newCur.forEach { r.union(InkRenderer.bounds(it)) }
        selBounds = r
        movedAny = true
        rebuild()
    }

    private fun selectInLasso() {
        if (lassoPts.size < 6) { clearSelection(); return }
        val poly = lassoPts.toArray()
        val sel = strokes.filter { s ->
            val p = s.points
            if (s.text != null) {
                p.size >= 2 && inside(poly, p[0], p[1])
            } else {
                var inCount = 0
                var total = 0
                var i = 0
                while (i + 1 < p.size) { total++; if (inside(poly, p[i], p[i + 1])) inCount++; i += 3 }
                total > 0 && inCount * 10 >= total * 6
            }
        }.toSet()
        selected = sel
        selBounds = if (sel.isEmpty()) null else {
            val r = RectF(Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
            sel.forEach { r.union(InkRenderer.bounds(it)) }
            r
        }
    }

    private fun inside(poly: FloatArray, x: Float, y: Float): Boolean {
        var c = false
        val n = poly.size / 2
        var j = n - 1
        for (i in 0 until n) {
            val xi = poly[i * 2]; val yi = poly[i * 2 + 1]
            val xj = poly[j * 2]; val yj = poly[j * 2 + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi + 1e-6f) + xi) c = !c
            j = i
        }
        return c
    }

    private fun shapePoints(t: Tool, x0: Float, y0: Float, x1: Float, y1: Float): FloatArray {
        val pts = FloatList()
        fun p(x: Float, y: Float) { pts.add(x); pts.add(y); pts.add(0.6f) }
        val l = min(x0, x1); val r = max(x0, x1); val tp = min(y0, y1); val b = max(y0, y1)
        when (t) {
            Tool.LINE -> { p(x0, y0); p(x1, y1) }
            Tool.ARROW -> {
                p(x0, y0); p(x1, y1)
                val a = atan2(y1 - y0, x1 - x0)
                val len = max(12f, curWidth * 4f)
                val a1 = a + PI.toFloat() * 5f / 6f
                val a2 = a - PI.toFloat() * 5f / 6f
                p(x1 + cos(a1) * len, y1 + sin(a1) * len)
                p(x1, y1)
                p(x1 + cos(a2) * len, y1 + sin(a2) * len)
            }
            Tool.RECT -> { p(l, tp); p(r, tp); p(r, b); p(l, b); p(l, tp) }
            Tool.CIRCLE -> {
                val rad = hypot(x1 - x0, y1 - y0)
                for (i in 0..72) {
                    val ang = 2.0 * PI * i / 72
                    p(x0 + cos(ang).toFloat() * rad, y0 + sin(ang).toFloat() * rad)
                }
            }
            Tool.ELLIPSE -> {
                val cx = (l + r) / 2f; val cy = (tp + b) / 2f
                val rx = abs(r - l) / 2f; val ry = abs(b - tp) / 2f
                for (i in 0..72) {
                    val ang = 2.0 * PI * i / 72
                    p(cx + cos(ang).toFloat() * rx, cy + sin(ang).toFloat() * ry)
                }
            }
            Tool.TRIANGLE -> { p(l, b); p((l + r) / 2f, tp); p(r, b); p(l, b) }
            else -> {}
        }
        return pts.toArray()
    }

    private fun finish(commit: Boolean) {
        when (curTool) {
            Tool.ERASER -> {
                if (erasedAny) {
                    if (commit) {
                        notifyChanged()
                    } else {
                        eraseSnapshot?.let { strokes = it.toMutableList() }
                        if (undoStack.isNotEmpty()) undoStack.removeAt(undoStack.lastIndex)
                        rebuild()
                    }
                }
                eraseSnapshot = null
                eraserX = -1f
            }
            Tool.PEN, Tool.HIGHLIGHTER -> {
                val list = cur
                if (commit && list != null && list.size >= 3) {
                    pushUndo(ArrayList(strokes))
                    val s = Stroke(curColor, curWidth, curTool == Tool.HIGHLIGHTER, list.toArray())
                    strokes.add(s)
                    if (s.highlighter) onBmp { InkRenderer.drawStroke(it, s, d) }
                    notifyChanged()
                } else if (curTool == Tool.PEN) {
                    rebuild()
                }
            }
            Tool.LASSO -> {
                if (moving || rotating) {
                    if (movedAny) {
                        if (commit) {
                            moveSnapshot?.let { pushUndo(it) }
                            notifyChanged()
                        } else {
                            moveSnapshot?.let { strokes = it.toMutableList() }
                            clearSelection()
                            rebuild()
                        }
                    }
                    moving = false
                    rotating = false
                    moveSnapshot = null
                } else if (commit) {
                    selectInLasso()
                }
                lassoPts.clear()
            }
            Tool.TEXT -> if (commit) onTextRequest?.invoke(downX, downY)
            else -> {
                if (commit && hypot(ex - sx, ey - sy) > 3f) {
                    pushUndo(ArrayList(strokes))
                    val s = Stroke(curColor, curWidth, false, shapePoints(curTool, sx, sy, ex, ey))
                    strokes.add(s)
                    onBmp { InkRenderer.drawStroke(it, s, d) }
                    notifyChanged()
                }
            }
        }
        cur = null
        curPath.reset()
        activeId = -1
        invalidate()
    }

    class FloatList {
        var a = FloatArray(384)
        var size = 0
        fun add(v: Float) {
            if (size == a.size) a = a.copyOf(size * 2)
            a[size++] = v
        }
        fun clear() { size = 0 }
        fun toArray(): FloatArray = a.copyOf(size)
    }
}
