package com.example.ajanda.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ajanda.data.NotePage
import com.example.ajanda.data.Repo
import com.example.ajanda.data.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onGo: (Int) -> Unit,          // bölüm sırası: 1 görev, 2 ajanda, 3 not, 4 takip, 5 hesap
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit
) {
    val today = todayEpoch()
    var editing by remember { mutableStateOf<Task?>(null) }
    val hour = LocalTime.now().hour
    val greet = when (hour) { in 5..11 -> "Günaydın"; in 12..17 -> "İyi günler"; else -> "İyi akşamlar" }

    val dueTasks = Repo.tasks.filter { t -> val d = t.dueEpochDay; !t.done && d != null && d <= today }
        .sortedWith(compareBy<Task>({ it.dueEpochDay ?: 0L }, { -it.priority }))
    val todayEvents = Repo.events.filter { it.epochDay == today }.sortedBy { it.startMin ?: -1 }
    val recentNotes = Repo.pages.sortedByDescending { it.updatedAt }.take(4)
    val openTracks = Repo.trackItems.count { item ->
        val l = Repo.trackLists.firstOrNull { it.id == item.listId }
        l != null && item.status != l.statuses.lastOrNull()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 30.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", TR)), fontSize = 14.sp, color = B.Muted)
                Text(greet, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Yeni görev", BIcons.Plus, height = 40.dp) { editing = Task(Repo.newId(), "", dueEpochDay = today) }
                GhostButton("Yeni not", BIcons.Pen, height = 40.dp) { onNewNote() }
                GhostButton("Hesap", BIcons.Calc, accent = true, height = 40.dp) { onGo(5) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Bugün / gecikmiş", dueTasks.size.toString(), BIcons.Tasks, Modifier.weight(1f)) { onGo(1) }
            StatTile("Bugünkü etkinlik", todayEvents.size.toString(), BIcons.Calendar, Modifier.weight(1f)) { onGo(2) }
            StatTile("Not", Repo.pages.size.toString(), BIcons.Pen, Modifier.weight(1f)) { onGo(3) }
            StatTile("Açık takip", openTracks.toString(), BIcons.Track, Modifier.weight(1f)) { onGo(4) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Bugün yapılacaklar", B.Accent)
                if (dueTasks.isEmpty()) Text("Bugün için görev yok.", color = B.Muted, fontSize = 14.sp)
                dueTasks.take(6).forEach { t -> TaskRow(t) { editing = t } }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Ajanda", B.Text2)
                if (todayEvents.isEmpty()) Text("Bugün etkinlik yok.", color = B.Muted, fontSize = 14.sp)
                todayEvents.forEach { e ->
                    BCard(Modifier.fillMaxWidth(), PaddingValues(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val time = when {
                                e.startMin == null -> "Tüm gün"
                                e.endMin == null -> fmtTime(e.startMin)
                                else -> "${fmtTime(e.startMin)}–${fmtTime(e.endMin)}"
                            }
                            Text(time, color = B.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(96.dp))
                            Text(e.title, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                VSpace(8.dp)
                SectionLabel("Son notlar", B.Text2)
                if (recentNotes.isEmpty()) Text("Henüz not yok.", color = B.Muted, fontSize = 14.sp)
                recentNotes.forEach { n -> NoteLine(n) { onOpenNote(n.id) } }
            }
        }
    }

    editing?.let { t -> TaskDialog(t) { editing = null } }
}

@Composable
private fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = B.Card, modifier = modifier) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = B.AccentSoft, contentColor = B.Accent) {
                Icon(icon, null, Modifier.padding(10.dp).size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(label, fontSize = 12.sp, color = B.Muted)
            }
        }
    }
}

@Composable
private fun NoteLine(n: NotePage, onClick: () -> Unit) {
    val fmt = DateTimeFormatter.ofPattern("d MMM HH:mm", TR)
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = Color(0xFF1A1C20), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(BIcons.Pen, null, Modifier.size(18.dp), tint = B.Muted)
            Spacer(Modifier.width(10.dp))
            Text(n.title, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                Instant.ofEpochMilli(n.updatedAt).atZone(ZoneId.systemDefault()).format(fmt),
                fontSize = 12.sp, color = B.Muted
            )
        }
    }
}
