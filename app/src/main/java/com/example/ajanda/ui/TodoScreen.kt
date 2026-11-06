@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ajanda.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ajanda.data.Repo
import com.example.ajanda.data.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class TodoFilter(val label: String) {
    ALL("Açık"), TODAY("Bugün"), UPCOMING("Yaklaşan"), NODATE("Tarihsiz"), DONE("Tamamlanan")
}

private data class TaskGroup(val label: String, val color: Color, val items: List<Task>)

@Composable
fun TodoScreen() {
    var filter by rememberSaveable { mutableStateOf(TodoFilter.ALL) }
    var editing by remember { mutableStateOf<Task?>(null) }
    val today = todayEpoch()

    val sorted = Repo.tasks.sortedWith(
        compareBy<Task>({ it.dueEpochDay ?: Long.MAX_VALUE }, { -it.priority }, { it.createdAt })
    )
    val open = sorted.filter { !it.done }

    val groups: List<TaskGroup> = when (filter) {
        TodoFilter.ALL -> listOf(
            TaskGroup("Gecikmiş", B.Danger, open.filter { val d = it.dueEpochDay; d != null && d < today }),
            TaskGroup("Bugün", B.Accent, open.filter { it.dueEpochDay == today }),
            TaskGroup("Yarın", B.Text2, open.filter { it.dueEpochDay == today + 1 }),
            TaskGroup("Sonra", B.Text2, open.filter { val d = it.dueEpochDay; d != null && d > today + 1 }),
            TaskGroup("Tarihsiz", B.Muted, open.filter { it.dueEpochDay == null })
        ).filter { it.items.isNotEmpty() }
        TodoFilter.TODAY -> listOf(TaskGroup("Bugün ve gecikmiş", B.Accent, open.filter { val d = it.dueEpochDay; d != null && d <= today }))
        TodoFilter.UPCOMING -> listOf(TaskGroup("Yaklaşan", B.Text2, open.filter { val d = it.dueEpochDay; d != null && d > today }))
        TodoFilter.NODATE -> listOf(TaskGroup("Tarihsiz", B.Muted, open.filter { it.dueEpochDay == null }))
        TodoFilter.DONE -> listOf(TaskGroup("Tamamlanan", B.Muted, sorted.filter { it.done }.reversed()))
    }.filter { it.items.isNotEmpty() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 40.dp, vertical = 30.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", TR)),
                    fontSize = 14.sp, color = B.Muted
                )
                Text("Görevler", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            AccentButton("Yeni görev", BIcons.Plus) {
                editing = Task(Repo.newId(), "", dueEpochDay = if (filter == TodoFilter.TODAY) today else null)
            }
        }
        VSpace(20.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TodoFilter.entries.forEach { f -> PillChip(f.label, filter == f) { filter = f } }
        }
        VSpace(20.dp)

        if (groups.isEmpty()) {
            Text("Burada görev yok.", color = B.Muted, modifier = Modifier.padding(top = 24.dp))
        }
        groups.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                pair.forEach { g ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel(g.label, g.color)
                        g.items.forEach { t -> TaskRow(t) { editing = t } }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            VSpace(24.dp)
        }
    }

    editing?.let { t -> TaskDialog(t) { editing = null } }
}

fun taskMeta(t: Task): String {
    val parts = mutableListOf<String>()
    t.dueEpochDay?.let { parts.add(fmtShort(it)) }
    if (t.priority == 2) parts.add("Yüksek öncelik")
    if (t.priority == 0) parts.add("Düşük öncelik")
    if (t.note.isNotBlank()) parts.add(t.note.lineSequence().first())
    return parts.joinToString(" · ")
}

@Composable
fun TaskRow(t: Task, onClick: () -> Unit) {
    val today = todayEpoch()
    val due = t.dueEpochDay
    val ringColor = when {
        t.done -> B.Accent
        due != null && due < today -> B.Danger
        t.priority == 2 || due == today -> B.Accent
        else -> Color(0xFF6B7280)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1A1C20),
        border = BorderStroke(1.dp, Color(0xFF25282D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .border(2.dp, ringColor, CircleShape)
                    .background(if (t.done) B.Accent else Color.Transparent)
                    .clickable { Repo.saveTask(t.copy(done = !t.done)) },
                contentAlignment = Alignment.Center
            ) {
                if (t.done) Text("✓", color = B.OnAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    textDecoration = if (t.done) TextDecoration.LineThrough else null,
                    color = if (t.done) B.Muted else B.Text,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                val meta = taskMeta(t)
                if (meta.isNotEmpty()) {
                    Text(meta, fontSize = 13.sp, color = B.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun TaskDialog(task: Task, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(task.title) }
    var note by remember { mutableStateOf(task.note) }
    var due by remember { mutableStateOf(task.dueEpochDay) }
    var pri by remember { mutableStateOf(task.priority) }
    val exists = Repo.tasks.any { it.id == task.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exists) "Görevi düzenle" else "Yeni görev") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Not") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                DateField("Tarih ekle", due) { due = it }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PRIORITY_LABELS.forEachIndexed { i, l ->
                        FilterChip(selected = pri == i, onClick = { pri = i }, label = { Text(l) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    Repo.saveTask(task.copy(title = title.trim(), note = note.trim(), dueEpochDay = due, priority = pri))
                }
                onDismiss()
            }) { Text("Kaydet") }
        },
        dismissButton = {
            Row {
                if (exists) {
                    TextButton(onClick = { Repo.deleteTask(task.id); onDismiss() }) {
                        Text("Sil", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("İptal") }
            }
        }
    )
}
