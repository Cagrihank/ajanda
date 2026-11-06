package com.example.ajanda.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Öneri B · Koyu, odaklı tasarımın renkleri. */
object B {
    val Bg = Color(0xFF111214)
    val Card = Color(0x0AFFFFFF)
    val CardHi = Color(0x14FFFFFF)
    val Border = Color(0x14FFFFFF)
    val Text = Color(0xFFF3F4F6)
    val Text2 = Color(0xFFD1D5DB)
    val Muted = Color(0xFF9CA3AF)
    val Accent = Color(0xFFF5A524)
    val AccentSoft = Color(0x26F5A524)
    val OnAccent = Color(0xFF1A1200)
    val Danger = Color(0xFFF87171)
    val Toolbar = Color(0x9E1A1C20)
}

val BColorScheme = darkColorScheme(
    primary = B.Accent,
    onPrimary = B.OnAccent,
    primaryContainer = Color(0xFF3A2E14),
    onPrimaryContainer = B.Accent,
    secondary = B.Accent,
    onSecondary = B.OnAccent,
    secondaryContainer = Color(0xFF26292E),
    onSecondaryContainer = B.Text,
    tertiary = Color(0xFF38BDF8),
    background = B.Bg,
    onBackground = B.Text,
    surface = B.Bg,
    onSurface = B.Text,
    surfaceVariant = Color(0xFF1A1C20),
    onSurfaceVariant = B.Muted,
    outline = Color(0xFF3A3D44),
    outlineVariant = Color(0xFF2B2E34),
    error = B.Danger,
    surfaceContainerLowest = Color(0xFF0D0E10),
    surfaceContainerLow = Color(0xFF16171A),
    surfaceContainer = Color(0xFF1A1C20),
    surfaceContainerHigh = Color(0xFF1F2125),
    surfaceContainerHighest = Color(0xFF26292E)
)

@Composable
fun BTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BColorScheme, content = content)
}

/** 24×24 çizgi ikonları (SVG yol verisinden). */
private fun svg(name: String, vararg paths: String): ImageVector {
    val b = ImageVector.Builder(
        name = name, defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    )
    paths.forEach { d ->
        b.addPath(
            pathData = addPathNodes(d),
            fill = null,
            stroke = SolidColor(Color.White),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    return b.build()
}

object BIcons {
    val Home = svg("home", "M3 10.5L12 3l9 7.5", "M5 9.5V21h5v-6h4v6h5V9.5")
    val Tasks = svg("tasks", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0", "M8.5 12l2.5 2.5 4.5-5")
    val Calendar = svg("cal", "M5 5h14a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", "M3 10h18M8 3v4M16 3v4")
    val Pen = svg("pen", "M4 20h4L19 9l-4-4L4 16v4z", "M13.5 6.5l4 4")
    val Track = svg("track", "M9 6h11M9 12h11M9 18h11", "M3.5 6l1 1 2-2M3.5 12l1 1 2-2M3.5 18l1 1 2-2")
    val Calc = svg(
        "calc", "M7 3h10a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M8 7h8",
        "M8 11h.01M12 11h.01M16 11h.01M8 14.5h.01M12 14.5h.01M16 14.5h.01M8 18h.01M12 18h.01M16 18h.01"
    )
    val Highlighter = svg("hl", "M9 11l6-6 4 4-6 6", "M9 11L5 15v4h4l4-4", "M14 20h6")
    val Eraser = svg("eraser", "M7 21l-4-4 10-10 7 7-7 7z", "M7 21h13")
    val Lasso = svg("lasso", "M3 10a9 6 0 1 0 18 0a9 6 0 1 0 -18 0", "M7 15c-1.5 2-1 4.5 1.5 5.5")
    val Text = svg("text", "M5 7V4h14v3", "M12 4v16", "M9 20h6")
    val Line = svg("line", "M5 19L19 5")
    val Arrow = svg("arrow", "M5 19L19 5", "M10 5h9v9")
    val Rect = svg("rect", "M4 6h16v12H4z")
    val Circle = svg("circle", "M4 12a8 8 0 1 0 16 0a8 8 0 1 0 -16 0")
    val Ellipse = svg("ellipse", "M3 12a9 6 0 1 0 18 0a9 6 0 1 0 -18 0")
    val Triangle = svg("tri", "M12 4l9 16H3z")
    val Hand = svg(
        "hand", "M8 13V5.5a1.5 1.5 0 0 1 3 0V12", "M11 11V4.5a1.5 1.5 0 0 1 3 0V12",
        "M14 11.5V6a1.5 1.5 0 0 1 3 0v8a7 7 0 0 1-7 7h-.5A6.5 6.5 0 0 1 4 15.5V12a1.5 1.5 0 0 1 3 0v1"
    )
    val Undo = svg("undo", "M9 14L4 9l5-5", "M4 9h11a5 5 0 0 1 0 10h-3")
    val Redo = svg("redo", "M15 14l5-5-5-5", "M20 9H9a5 5 0 0 0 0 10h3")
    val Back = svg("back", "M15 18l-6-6 6-6")
    val Plus = svg("plus", "M12 5v14M5 12h14")
    val Download = svg("download", "M12 4v11", "M7 10l5 5 5-5", "M5 20h14")
    val Copy = svg("copy", "M9 9h11v11H9z", "M5 15H4V4h11v1")
    val Trash = svg("trash", "M4 7h16", "M10 11v6M14 11v6", "M6 7l1 13h10l1-13", "M9 7V4h6v3")
}

/** Sol menü öğesi. */
@Composable
fun RailItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) B.AccentSoft else Color.Transparent,
        contentColor = if (selected) B.Accent else B.Muted,
        modifier = Modifier.width(52.dp)
    ) {
        Column(
            Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, Modifier.size(20.dp))
            Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

/** Hap şeklinde filtre düğmesi (seçiliyse açık zemin). */
@Composable
fun PillChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) B.Text else Color.Transparent,
        contentColor = if (selected) B.Bg else B.Text2,
        border = if (selected) null else BorderStroke(1.dp, Color(0xFF33363C))
    ) {
        Text(
            label, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

/** Küçük kutulu segment düğmesi. */
@Composable
fun SegButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0x1FFFFFFF) else Color.Transparent,
        contentColor = if (selected) B.Text else B.Text2
    ) {
        Text(
            label, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun SegGroup(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(10.dp), color = Color(0x0DFFFFFF)) {
        Row(Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) { content() }
    }
}

/** Amber dolgulu ana düğme. */
@Composable
fun AccentButton(label: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = B.Accent, contentColor = B.OnAccent) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(18.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Sınırlı küçük düğme (ör. "Kopyala"). */
@Composable
fun GhostButton(
    label: String,
    icon: ImageVector? = null,
    accent: Boolean = false,
    height: Dp = 32.dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = if (accent) B.AccentSoft else Color.Transparent,
        contentColor = if (accent) B.Accent else B.Text2,
        border = if (accent) null else BorderStroke(1.dp, Color(0x1AFFFFFF)),
        modifier = modifier.height(height)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(15.dp))
            Text(label, fontSize = 12.sp, fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

/** Yarı saydam kart. */
@Composable
fun BCard(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(16.dp), content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = B.Card,
        border = BorderStroke(1.dp, B.Border)
    ) {
        Column(Modifier.padding(padding)) { content() }
    }
}

@Composable
fun SectionLabel(text: String, color: Color = B.Muted) {
    Text(text.uppercase(TR), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = color)
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))
