package com.example.ajanda.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ajanda.calc.Expr
import com.example.ajanda.data.Repo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.roundToInt

data class CalcEntry(val expr: String, val result: String)

/** Hesap geçmişi, bölümler arası geçişte kaybolmasın diye burada tutulur. */
object CalcState {
    var expr by mutableStateOf("")
    var deg by mutableStateOf(true)
    val history = mutableStateListOf<CalcEntry>()
    val slopeHistory = mutableStateListOf<CalcEntry>()
}

private val Mono = FontFamily.Monospace

@Composable
fun CalcScreen() {
    var mode by rememberSaveable { mutableIntStateOf(1) } // 0 standart, 1 bilimsel, 2 eğim
    val context = LocalContext.current
    fun copy(text: String) {
        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("Ajanda", text))
        Toast.makeText(context, "Kopyalandı", Toast.LENGTH_SHORT).show()
    }
    fun toNote(text: String) {
        val n = Repo.appendTextToNote(text)
        Toast.makeText(context, "Nota eklendi: ${n.title}", Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hesap makinesi", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            SegGroup {
                SegButton("Standart", mode == 0) { mode = 0 }
                SegButton("Bilimsel", mode == 1) { mode = 1 }
                SegButton("Eğim", mode == 2) { mode = 2 }
            }
            if (mode == 1) {
                Spacer(Modifier.width(12.dp))
                GhostButton(if (CalcState.deg) "DEG" else "RAD", height = 30.dp) { CalcState.deg = !CalcState.deg }
            }
        }
        VSpace(16.dp)
        if (mode == 2) SlopeCalc(::copy, ::toNote) else BasicCalc(mode == 1, ::copy, ::toNote)
    }
}

// ---------------------------------------------------------------- Standart / bilimsel

private class Key(val label: String, val insert: String?, val kind: Int) // kind: 0 sayı, 1 fonksiyon, 2 işlem, 3 eşittir

@Composable
private fun BasicCalc(scientific: Boolean, copy: (String) -> Unit, toNote: (String) -> Unit) {
    val expr = CalcState.expr
    val preview = remember(expr, CalcState.deg) {
        if (expr.isBlank()) "0" else try { Expr.pretty(Expr(expr, CalcState.deg).eval()) } catch (e: Exception) { "" }
    }

    fun press(k: Key) {
        when (k.label) {
            "C" -> CalcState.expr = ""
            "⌫" -> CalcState.expr = expr.dropLast(1)
            "±" -> CalcState.expr = if (expr.startsWith("−(") && expr.endsWith(")")) expr.drop(2).dropLast(1) else if (expr.isBlank()) "−" else "−($expr)"
            "1/x" -> if (expr.isNotBlank()) CalcState.expr = "1÷($expr)"
            "=" -> {
                if (expr.isBlank()) return
                try {
                    val v = Expr(expr, CalcState.deg).eval()
                    if (v.isNaN() || v.isInfinite()) throw ArithmeticException()
                    CalcState.history.add(0, CalcEntry(expr, Expr.pretty(v)))
                    if (CalcState.history.size > 50) CalcState.history.removeAt(CalcState.history.lastIndex)
                    CalcState.expr = Expr.raw(v)
                } catch (e: Exception) {
                    CalcState.expr = expr // hatalı ifadeyi koru
                }
            }
            else -> CalcState.expr = expr + (k.insert ?: k.label)
        }
    }

    val sci = listOf(
        Key("sin", "sin(", 1), Key("cos", "cos(", 1), Key("tan", "tan(", 1), Key("ln", "ln(", 1),
        Key("log", "log(", 1), Key("π", "π", 1), Key("√", "√", 1), Key("x²", "²", 1),
        Key("xʸ", "^", 1), Key("(", "(", 1), Key(")", ")", 1), Key("1/x", null, 1)
    )
    val keys = listOf(
        Key("C", null, 1), Key("⌫", null, 1), Key("%", "%", 1), Key("÷", "÷", 2),
        Key("7", null, 0), Key("8", null, 0), Key("9", null, 0), Key("×", "×", 2),
        Key("4", null, 0), Key("5", null, 0), Key("6", null, 0), Key("−", "−", 2),
        Key("1", null, 0), Key("2", null, 0), Key("3", null, 0), Key("+", "+", 2),
        Key("±", null, 1), Key("0", null, 0), Key(",", ",", 0), Key("=", null, 3)
    )

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BCard(Modifier.fillMaxWidth(), PaddingValues(horizontal = 22.dp, vertical = 16.dp)) {
                Text(
                    expr.ifBlank { " " }, fontFamily = Mono, fontSize = 18.sp, color = B.Muted,
                    textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    preview.ifBlank { "…" }, fontFamily = Mono, fontSize = 44.sp, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth(), maxLines = 1
                )
            }
            if (scientific) {
                sci.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { k ->
                            Surface(
                                onClick = { press(k) },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x0AFFFFFF),
                                contentColor = B.Text2,
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) { Text(k.label, fontFamily = Mono, fontSize = 15.sp) }
                            }
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                keys.chunked(4).forEach { row ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { k ->
                            val (bg, fg) = when (k.kind) {
                                0 -> Color(0x12FFFFFF) to B.Text
                                1 -> Color(0x1FFFFFFF) to B.Text2
                                2 -> B.AccentSoft to B.Accent
                                else -> B.Accent to B.OnAccent
                            }
                            Surface(
                                onClick = { press(k) },
                                shape = RoundedCornerShape(14.dp),
                                color = bg,
                                contentColor = fg,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        k.label, fontFamily = Mono, fontSize = 24.sp,
                                        fontWeight = if (k.kind >= 2) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(Modifier.width(300.dp).fillMaxHeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Geçmiş", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (CalcState.history.isNotEmpty()) GhostButton("Temizle", height = 30.dp) { CalcState.history.clear() }
            }
            VSpace(10.dp)
            if (CalcState.history.isEmpty()) Text("Hesapların burada listelenir.", color = B.Muted, fontSize = 13.sp)
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CalcState.history.forEach { h ->
                    BCard(Modifier.fillMaxWidth(), PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                            Text(h.expr, fontFamily = Mono, fontSize = 13.sp, color = B.Muted)
                            Text(h.result, fontFamily = Mono, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            VSpace(6.dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GhostButton("Kopyala", BIcons.Copy, height = 28.dp) { copy(h.result) }
                                GhostButton("Nota ekle", accent = true, height = 28.dp) { toNote("${h.expr} = ${h.result}") }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Eğim

private val UNITS = listOf("m" to 1.0, "cm" to 0.01, "mm" to 0.001)

@Composable
private fun SlopeCalc(copy: (String) -> Unit, toNote: (String) -> Unit) {
    var target by rememberSaveable { mutableIntStateOf(0) } // 0 eğim, 1 yatay, 2 yükseklik
    var lText by rememberSaveable { mutableStateOf("12,00") }
    var hText by rememberSaveable { mutableStateOf("0,30") }
    var eText by rememberSaveable { mutableStateOf("2") }
    var unitIdx by rememberSaveable { mutableIntStateOf(0) }
    val unit = UNITS[unitIdx].first

    val L = Expr.parseNumber(lText)
    val H = Expr.parseNumber(hText)
    val E = Expr.parseNumber(eText) // yüzde

    // Sonuç: eğim oranı (H/L), yatay ve yükseklik
    var ratio: Double? = null
    var outL: Double? = null
    var outH: Double? = null
    when (target) {
        0 -> if (L != null && H != null && L != 0.0) { ratio = H / L; outL = L; outH = H }
        1 -> if (E != null && H != null && E != 0.0) { ratio = E / 100.0; outH = H; outL = H / (E / 100.0) }
        2 -> if (E != null && L != null) { ratio = E / 100.0; outL = L; outH = L * (E / 100.0) }
    }

    val percent = ratio?.let { it * 100.0 }
    val angle = ratio?.let { atan(it) * 180.0 / PI }
    val oneTo = ratio?.let { if (abs(it) < 1e-12) null else 1.0 / abs(it) }
    val mainLabel = when (target) { 0 -> "Eğim"; 1 -> "Yatay uzunluk (L)"; else -> "Yükseklik farkı (H)" }
    val mainValue = when (target) {
        0 -> percent?.let { "%" + Expr.pretty(it, 2) }
        1 -> outL?.let { Expr.pretty(it, 3) + " " + unit }
        else -> outH?.let { Expr.pretty(it, 3) + " " + unit }
    } ?: "—"
    val summary = when (target) {
        0 -> "Eğim: L ${lText} $unit, H ${hText} $unit → $mainValue"
        1 -> "Yatay uzunluk: %${eText} sabit, H ${hText} $unit → L = $mainValue"
        else -> "Yükseklik: %${eText} sabit, L ${lText} $unit → H = $mainValue"
    }

    fun saveHist(entry: String) {
        CalcState.slopeHistory.add(0, CalcEntry(entry.substringBefore("→").trim(), entry.substringAfter("→").trim()))
        if (CalcState.slopeHistory.size > 20) CalcState.slopeHistory.removeAt(CalcState.slopeHistory.lastIndex)
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Ne bulmak istiyorsun?", fontSize = 13.sp, color = B.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TargetCard("Eğim", "e = H ÷ L", target == 0, Modifier.weight(1f)) { target = 0 }
                TargetCard("Yatay uzunluk", "L = H ÷ e", target == 1, Modifier.weight(1f)) { target = 1 }
                TargetCard("Yükseklik", "H = L × e", target == 2, Modifier.weight(1f)) { target = 2 }
            }

            SlopeDiagram(
                lLabel = "L = " + (outL?.let { Expr.pretty(it, 2) } ?: "?") + " $unit",
                hLabel = "H = " + (outH?.let { Expr.pretty(it, 2) } ?: "?") + " $unit",
                eLabel = percent?.let { "%" + Expr.pretty(it, 2) } ?: "%?"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (target != 0) NumField("Eğim (%)", eText, "%", Modifier.weight(1f), highlight = false) { eText = it }
                if (target != 1) NumField("Yatay uzunluk (L)", lText, unit, Modifier.weight(1f), highlight = target == 2) { lText = it }
                if (target != 2) NumField("Yükseklik farkı (H)", hText, unit, Modifier.weight(1f), highlight = target != 2) { hText = it }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Birim", fontSize = 13.sp, color = B.Muted)
                SegGroup { UNITS.forEachIndexed { i, u -> SegButton(u.first, unitIdx == i) { unitIdx = i } } }
            }
        }

        Column(Modifier.width(300.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BCard(Modifier.fillMaxWidth(), PaddingValues(18.dp)) {
                Text(mainLabel, fontSize = 13.sp, color = B.Muted)
                VSpace(8.dp)
                Text(mainValue, fontFamily = Mono, fontSize = 38.sp, fontWeight = FontWeight.Medium, color = B.Accent)
                VSpace(12.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("Eğim", percent?.let { "%" + Expr.pretty(it, 2) } ?: "—", Modifier.weight(1f))
                    MiniStat("Açı", angle?.let { Expr.pretty(it, 2) + "°" } ?: "—", Modifier.weight(1f))
                }
                VSpace(8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniStat("Oran", oneTo?.let { "1:" + Expr.pretty(it, if (abs(it - it.roundToInt()) < 0.005) 0 else 2) } ?: "—", Modifier.weight(1f))
                    MiniStat("Binde", ratio?.let { "‰" + Expr.pretty(it * 1000.0, 1) } ?: "—", Modifier.weight(1f))
                }
                VSpace(12.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GhostButton("Kopyala", BIcons.Copy, modifier = Modifier.weight(1f), height = 34.dp) {
                        copy(mainValue); saveHist(summary)
                    }
                    GhostButton("Nota ekle", accent = true, modifier = Modifier.weight(1f), height = 34.dp) {
                        toNote(summary); saveHist(summary)
                    }
                }
            }

            Text("Hazır eğimler", fontSize = 13.sp, color = B.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("1", "1,5", "2", "5").forEach { p ->
                    GhostButton("%$p", height = 30.dp) { eText = p; if (target == 0) target = 1 }
                }
                GhostButton("1:12 rampa", height = 30.dp) { eText = "8,333"; if (target == 0) target = 1 }
            }

            if (CalcState.slopeHistory.isNotEmpty()) {
                Text("Son hesaplar", fontSize = 13.sp, color = B.Muted)
                CalcState.slopeHistory.forEach { h ->
                    BCard(Modifier.fillMaxWidth(), PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(h.expr, fontSize = 11.sp, color = B.Muted)
                        Text(h.result, fontFamily = Mono, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetCard(title: String, formula: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) B.AccentSoft else Color(0x08FFFFFF),
        border = BorderStroke(1.dp, if (selected) B.Accent else Color(0x14FFFFFF)),
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold, color = B.Text)
            Text(formula, fontFamily = Mono, fontSize = 12.sp, color = if (selected) B.Text2 else B.Muted)
        }
    }
}

@Composable
private fun NumField(label: String, value: String, suffix: String, modifier: Modifier, highlight: Boolean, onChange: (String) -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, color = B.Muted)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0x0DFFFFFF),
            border = BorderStroke(1.dp, if (highlight) B.Accent else Color(0x14FFFFFF))
        ) {
            Row(Modifier.height(52.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = { v -> onChange(v.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }) },
                    singleLine = true,
                    textStyle = TextStyle(color = B.Text, fontFamily = Mono, fontSize = 22.sp),
                    cursorBrush = SolidColor(B.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Text(suffix, fontSize = 14.sp, color = B.Muted)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = B.Muted)
        Text(value, fontFamily = Mono, fontSize = 16.sp, maxLines = 1)
    }
}

@Composable
private fun SlopeDiagram(lLabel: String, hLabel: String, eLabel: String) {
    val measurer = rememberTextMeasurer()
    BCard(Modifier.fillMaxWidth().height(190.dp), PaddingValues(0.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val x0 = size.width * 0.1f
            val x1 = size.width * 0.78f
            val yb = size.height * 0.78f
            val yt = size.height * 0.22f
            drawLine(Color(0xFF9CA3AF), Offset(x0, yb), Offset(x1, yb), strokeWidth = 2.dp.toPx())
            drawLine(
                Color(0xFF9CA3AF), Offset(x1, yb), Offset(x1, yt), strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
            drawLine(B.Accent, Offset(x0, yb), Offset(x1, yt), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            val style = TextStyle(color = B.Text, fontFamily = Mono, fontSize = 14.sp)
            val lm = measurer.measure(lLabel, style)
            drawText(lm, topLeft = Offset((x0 + x1) / 2f - lm.size.width / 2f, yb + 8.dp.toPx()))
            drawText(measurer.measure(hLabel, style), topLeft = Offset(x1 + 10.dp.toPx(), (yb + yt) / 2f - 10.dp.toPx()))
            val em = measurer.measure(eLabel, style.copy(color = B.Accent, fontWeight = FontWeight.SemiBold))
            drawText(em, topLeft = Offset((x0 + x1) / 2f - em.size.width / 2f - 30.dp.toPx(), (yb + yt) / 2f - 30.dp.toPx()))
            drawText(
                measurer.measure("ölçeksiz", TextStyle(color = B.Muted, fontSize = 11.sp)),
                topLeft = Offset(size.width - 70.dp.toPx(), 10.dp.toPx())
            )
        }
    }
}
