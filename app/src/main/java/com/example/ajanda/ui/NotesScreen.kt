@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ajanda.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ajanda.data.NotePage
import com.example.ajanda.data.Repo
import com.example.ajanda.ink.InkView
import com.example.ajanda.ink.PdfExport
import java.time.Instant
import kotlin.math.roundToInt
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val INK_COLORS = listOf(0xFF1B1B1B, 0xFF1E5BD8, 0xFFD32F2F).map { it.toInt() }
private val HIGHLIGHT_YELLOW = 0xFFF9A825.toInt()
private val PALETTE = listOf(
    0xFF1B1B1B, 0xFF4B5563, 0xFF9CA3AF, 0xFFFFFFFF, 0xFF7F1D1D, 0xFFD32F2F,
    0xFFF57C00, 0xFFF9A825, 0xFFFDE047, 0xFF2E7D32, 0xFF22C55E, 0xFF00897B,
    0xFF0EA5E9, 0xFF1E5BD8, 0xFF3730A3, 0xFF8E24AA, 0xFFDB2777, 0xFFF9A8D4,
    0xFF8D6E63, 0xFF5D4037, 0xFFA3E635, 0xFF14B8A6, 0xFF60A5FA, 0xFFC4B5FD
).map { it.toInt() }
private val WIDTHS = listOf(2f, 3.5f, 6f)
private val PAPER_LABELS = listOf("Düz", "Çizgili", "Kareli")

/** Yeni boş not oluşturur ve kimliğini döndürür. */
fun createNote(): Long {
    val id = Repo.newId()
    Repo.savePage(NotePage(id, "Not – " + fmtDate(todayEpoch())))
    return id
}

@Composable
fun NotesScreen(openId: Long?, onOpen: (Long?) -> Unit) {
    val page = openId?.let { id -> Repo.pages.firstOrNull { it.id == id } }
    if (page != null) {
        BackHandler { onOpen(null) }
        key(page.id) { NoteEditor(page) { onOpen(null) } }
    } else {
        NoteGrid(onOpen = { onOpen(it) })
    }
}

@Composable
private fun NoteGrid(onOpen: (Long) -> Unit) {
    var confirmDelete by remember { mutableStateOf<NotePage?>(null) }
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", TR)

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 30.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("${Repo.pages.size} not", fontSize = 14.sp, color = B.Muted)
                Text("Notlar", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            AccentButton("Yeni not", BIcons.Plus) { onOpen(createNote()) }
        }
        VSpace(20.dp)
        if (Repo.pages.isEmpty()) {
            Text("Henüz not yok. \"Yeni not\" ile başla.", color = B.Muted)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(230.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(Repo.pages.sortedByDescending { it.updatedAt }, key = { it.id }) { p ->
                Surface(
                    onClick = { onOpen(p.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1C20),
                    border = BorderStroke(1.dp, Color(0xFF25282D)),
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(Color(0xFFFBF8F1))
                        )
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Text(p.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    Instant.ofEpochMilli(p.updatedAt).atZone(ZoneId.systemDefault()).format(fmt) +
                                        if (p.pageCount > 1) " · ${p.pageCount} sayfa" else "",
                                    fontSize = 12.sp, color = B.Muted, modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { confirmDelete = p }) {
                                    Icon(BIcons.Trash, "Sil", Modifier.size(18.dp), tint = B.Muted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("\"${p.title}\" silinsin mi?") },
            text = { Text("Bu nottaki tüm sayfalar kalıcı olarak silinir.") },
            confirmButton = {
                TextButton(onClick = { Repo.deletePage(p.id); confirmDelete = null }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("İptal") } }
        )
    }
}

@Composable
private fun NoteEditor(page: NotePage, onBack: () -> Unit) {
    val context = LocalContext.current
    var sheet by rememberSaveable { mutableIntStateOf(0) }
    var tool by rememberSaveable { mutableStateOf(InkView.Tool.PEN) }
    var color by rememberSaveable { mutableIntStateOf(INK_COLORS[0]) }
    var penWidth by rememberSaveable { mutableFloatStateOf(3.5f) }
    var showPalette by remember { mutableStateOf(false) }
    var finger by rememberSaveable { mutableStateOf(false) }
    var title by remember { mutableStateOf(page.title) }
    var textReq by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var zoom by remember { mutableFloatStateOf(1f) }
    val viewRef = remember { mutableStateOf<InkView?>(null) }
    if (sheet >= page.pageCount) sheet = page.pageCount - 1

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            try {
                val (w, h) = viewRef.value?.sizeDp() ?: Pair(1000f, 620f)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    PdfExport.write(Repo.pages.firstOrNull { it.id == page.id } ?: page, out, w, h)
                }
                Toast.makeText(context, "PDF kaydedildi", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "PDF kaydedilemedi: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Üst çubuk (ince)
        Row(
            Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(onClick = onBack, shape = RoundedCornerShape(10.dp), color = Color(0x0DFFFFFF), contentColor = B.Text2) {
                Icon(BIcons.Back, "Notlara dön", Modifier.padding(8.dp).size(18.dp))
            }
            BasicTextField(
                value = title,
                onValueChange = { title = it; Repo.savePage((Repo.pages.firstOrNull { p -> p.id == page.id } ?: page).copy(title = it)) },
                singleLine = true,
                textStyle = TextStyle(color = B.Text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(B.Accent),
                modifier = Modifier.weight(1f).padding(start = 6.dp)
            )
            GhostButton("Parmakla çiz", accent = finger, height = 30.dp) { finger = !finger }
            TopIcon(BIcons.Undo, "Geri al", false) { viewRef.value?.undo() }
            TopIcon(BIcons.Redo, "Yinele", false) { viewRef.value?.redo() }
            TopIcon(BIcons.Trash, "Seçileni sil / sayfayı temizle", false) {
                val v = viewRef.value
                if (tool == InkView.Tool.LASSO && v != null) v.deleteSelection() else confirmClear = true
            }
            GhostButton("%" + (zoom * 100).toInt(), height = 30.dp) { viewRef.value?.resetView() }
            GhostButton("PDF", BIcons.Download, height = 30.dp) {
                pdfLauncher.launch(title.ifBlank { "Not" }.replace(Regex("[\\\\/:*?\"<>|]"), "_") + ".pdf")
            }
            GhostButton("Kâğıt: " + PAPER_LABELS[page.paper.coerceIn(0, 2)], height = 30.dp) {
                Repo.savePage(page.copy(paper = (page.paper + 1) % 3))
            }
            for (i in 0 until page.pageCount) {
                Surface(
                    onClick = { sheet = i },
                    shape = RoundedCornerShape(8.dp),
                    color = if (i == sheet) Color(0x24FFFFFF) else Color(0x0DFFFFFF),
                    contentColor = if (i == sheet) B.Text else B.Text2,
                    modifier = Modifier.defaultMinSize(minWidth = 30.dp).height(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("${i + 1}", fontSize = 12.sp, fontWeight = if (i == sheet) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Surface(
                onClick = { sheet = Repo.addSheet(page.id) },
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                contentColor = B.Text2,
                border = BorderStroke(1.dp, Color(0xFF4B5563)),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { Icon(BIcons.Plus, "Yeni sayfa", Modifier.size(16.dp)) }
            }
        }

        // Kâğıt + yüzen araç çubuğu
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            key(page.id, sheet) {
                AndroidView(
                    factory = { ctx ->
                        InkView(ctx).also { v ->
                            v.setStrokes(Repo.loadStrokes(page.id, sheet))
                            val s = sheet
                            v.onChanged = { list -> Repo.saveStrokes(page.id, s, list) }
                            v.onTextRequest = { x, y -> textReq = Pair(x, y) }
                            v.onZoomChanged = { z -> zoom = z }
                            zoom = 1f
                            viewRef.value = v
                        }
                    },
                    update = { v ->
                        v.tool = tool
                        v.color = color
                        v.baseWidth = penWidth
                        v.fingerDraws = finger
                        v.paper = page.paper
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = B.Toolbar,
                border = BorderStroke(1.dp, Color(0x14FFFFFF)),
                shadowElevation = 6.dp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp)
            ) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ToolBtn(BIcons.Pen, "Kalem", tool == InkView.Tool.PEN) { tool = InkView.Tool.PEN }
                    ToolBtn(BIcons.Highlighter, "Fosforlu", tool == InkView.Tool.HIGHLIGHTER) {
                        tool = InkView.Tool.HIGHLIGHTER
                        if (color == INK_COLORS[0]) color = HIGHLIGHT_YELLOW
                    }
                    ToolBtn(BIcons.Eraser, "Silgi", tool == InkView.Tool.ERASER) { tool = InkView.Tool.ERASER }
                    ToolBtn(BIcons.Lasso, "Kement (seç ve taşı)", tool == InkView.Tool.LASSO) { tool = InkView.Tool.LASSO }
                    ToolBtn(BIcons.Hand, "Kaydır (el)", tool == InkView.Tool.PAN) { tool = InkView.Tool.PAN }
                    ToolBtn(BIcons.Text, "Metin", tool == InkView.Tool.TEXT) { tool = InkView.Tool.TEXT }
                    Sep()
                    ToolBtn(BIcons.Line, "Çizgi", tool == InkView.Tool.LINE) { tool = InkView.Tool.LINE }
                    ToolBtn(BIcons.Arrow, "Ok", tool == InkView.Tool.ARROW) { tool = InkView.Tool.ARROW }
                    ToolBtn(BIcons.Rect, "Dikdörtgen", tool == InkView.Tool.RECT) { tool = InkView.Tool.RECT }
                    ToolBtn(BIcons.Circle, "Daire", tool == InkView.Tool.CIRCLE) { tool = InkView.Tool.CIRCLE }
                    ToolBtn(BIcons.Ellipse, "Elips", tool == InkView.Tool.ELLIPSE) { tool = InkView.Tool.ELLIPSE }
                    ToolBtn(BIcons.Triangle, "Üçgen", tool == InkView.Tool.TRIANGLE) { tool = InkView.Tool.TRIANGLE }
                    Sep()
                    INK_COLORS.forEach { c ->
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .border(if (c == color) 2.dp else 1.dp, if (c == color) B.Text else Color(0x1FFFFFFF), CircleShape)
                                .clickable {
                                    color = c
                                    showPalette = false
                                    if (tool == InkView.Tool.ERASER || tool == InkView.Tool.LASSO) tool = InkView.Tool.PEN
                                }
                        )
                    }
                    val custom = color !in INK_COLORS
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (custom) Color(color) else Color.Transparent)
                            .border(if (custom) 2.dp else 1.dp, if (custom) B.Text else Color(0x73FFFFFF), CircleShape)
                            .clickable { showPalette = !showPalette },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!custom) Icon(BIcons.Plus, "Daha fazla renk", Modifier.size(13.dp), tint = B.Text2)
                    }
                    Sep()
                    listOf(5.dp, 8.dp, 12.dp).forEachIndexed { i, dot ->
                        val sel = kotlin.math.abs(penWidth - WIDTHS[i]) < 0.01f
                        Surface(
                            onClick = { penWidth = WIDTHS[i] },
                            shape = RoundedCornerShape(10.dp),
                            color = if (sel) Color(0x1AFFFFFF) else Color.Transparent,
                            modifier = Modifier.size(width = 30.dp, height = 36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(Modifier.size(dot).clip(CircleShape).background(if (sel) B.Text else B.Muted))
                            }
                        }
                    }
                    Slider(
                        value = penWidth,
                        onValueChange = { penWidth = (it * 2f).roundToInt() / 2f },
                        valueRange = 1f..12f,
                        colors = SliderDefaults.colors(thumbColor = B.Accent, activeTrackColor = B.Accent, inactiveTrackColor = Color(0x33FFFFFF)),
                        modifier = Modifier.width(96.dp).height(36.dp)
                    )
                    Text(
                        String.format(TR, "%.1f", penWidth), fontSize = 11.sp, color = B.Text2,
                        modifier = Modifier.width(26.dp)
                    )
                }
            }

            if (showPalette) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xEB1A1C20),
                    border = BorderStroke(1.dp, Color(0x1AFFFFFF)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 74.dp).width(250.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Daha fazla renk", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = B.Text2)
                        PALETTE.chunked(6).forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                row.forEach { c ->
                                    Box(
                                        Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color(c))
                                            .border(if (c == color) 2.dp else 1.dp, if (c == color) B.Accent else Color(0x24FFFFFF), CircleShape)
                                            .clickable {
                                                color = c
                                                showPalette = false
                                                if (tool == InkView.Tool.ERASER || tool == InkView.Tool.LASSO) tool = InkView.Tool.PEN
                                            }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    textReq?.let { (x, y) ->
        showPalette = false
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { textReq = null },
            title = { Text("Metin ekle") },
            text = {
                OutlinedTextField(text, { text = it }, minLines = 2, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Yazını gir…") })
            },
            confirmButton = {
                TextButton(onClick = { viewRef.value?.addText(x, y, text); textReq = null }) { Text("Ekle") }
            },
            dismissButton = { TextButton(onClick = { textReq = null }) { Text("İptal") } }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Sayfa temizlensin mi?") },
            text = { Text("\"Geri al\" ile geri getirebilirsin.") },
            confirmButton = {
                TextButton(onClick = { viewRef.value?.clearAll(); confirmClear = false }) { Text("Temizle") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("İptal") } }
        )
    }
}

@Composable
private fun ToolBtn(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) B.AccentSoft else Color.Transparent,
        contentColor = if (selected) B.Accent else B.Text2,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(18.dp)) }
    }
}

@Composable
private fun TopIcon(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) B.AccentSoft else Color(0x0DFFFFFF),
        contentColor = if (selected) B.Accent else B.Text2,
        modifier = Modifier.size(30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(16.dp)) }
    }
}

@Composable
private fun Sep() {
    Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(22.dp).background(Color(0x1AFFFFFF)))
}
