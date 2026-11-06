@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ajanda.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ajanda.data.AgendaEvent
import com.example.ajanda.data.Repo
import com.example.ajanda.data.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun AgendaScreen() {
    var selected by rememberSaveable { mutableStateOf(todayEpoch()) }
    var editingEvent by remember { mutableStateOf<AgendaEvent?>(null) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var pickDate by remember { mutableStateOf(false) }
    val today = todayEpoch()
    val selDate = LocalDate.ofEpochDay(selected)
    val weekStart = selDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    selDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", TR)),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { selected -= 7 }) { Icon(Icons.Filled.KeyboardArrowLeft, "Önceki hafta") }
                TextButton(onClick = { selected = today }) { Text("Bugün") }
                IconButton(onClick = { selected += 7 }) { Icon(Icons.Filled.KeyboardArrowRight, "Sonraki hafta") }
                IconButton(onClick = { pickDate = true }) { Icon(Icons.Filled.DateRange, "Tarih seç") }
            }
            Spacer(Modifier.height(12.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0..6) {
                    val day = weekStart.plusDays(i.toLong())
                    val ed = day.toEpochDay()
                    val isSel = ed == selected
                    val isToday = ed == today
                    val count = Repo.events.count { it.epochDay == ed } +
                        Repo.tasks.count { !it.done && it.dueEpochDay == ed }
                    Surface(
                        onClick = { selected = ed },
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isSel -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(day.format(DateTimeFormatter.ofPattern("EEE", TR)), style = MaterialTheme.typography.labelMedium)
                            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge)
                            Text(if (count > 0) "● $count" else " ", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(fmtDate(selected), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val dayEvents = Repo.events.filter { it.epochDay == selected }
                .sortedWith(compareBy<AgendaEvent>({ it.startMin ?: -1 }, { it.title }))
            val dayTasks = Repo.tasks.filter { it.dueEpochDay == selected }
                .sortedWith(compareBy<Task>({ it.done }, { -it.priority }))

            if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                Text("Bu gün için kayıt yok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                items(dayEvents, key = { "e${it.id}" }) { e -> EventRow(e) { editingEvent = e } }
                if (dayTasks.isNotEmpty()) {
                    item(key = "taskhdr") {
                        Text(
                            "Görevler",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                    }
                }
                items(dayTasks, key = { "t${it.id}" }) { t -> TaskRow(t) { editingTask = t } }
            }
        }

        Row(
            Modifier.align(Alignment.BottomEnd).padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = { editingTask = Task(Repo.newId(), "", dueEpochDay = selected) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Görev") }
            )
            ExtendedFloatingActionButton(
                onClick = { editingEvent = AgendaEvent(Repo.newId(), "", selected) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Etkinlik") }
            )
        }
    }

    if (pickDate) DatePickDialog(selected, { pickDate = false }) { d -> if (d != null) selected = d; pickDate = false }
    editingEvent?.let { e -> EventDialog(e) { editingEvent = null } }
    editingTask?.let { t -> TaskDialog(t) { editingTask = null } }
}

@Composable
fun EventRow(e: AgendaEvent, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            val time = when {
                e.startMin == null -> "Tüm gün"
                e.endMin == null -> fmtTime(e.startMin)
                else -> "${fmtTime(e.startMin)}–${fmtTime(e.endMin)}"
            }
            Text(time, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(110.dp))
            Column(Modifier.weight(1f)) {
                Text(e.title, style = MaterialTheme.typography.bodyLarge)
                if (e.note.isNotBlank()) {
                    Text(e.note, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
        }
    }
}

@Composable
fun EventDialog(ev: AgendaEvent, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(ev.title) }
    var note by remember { mutableStateOf(ev.note) }
    var day by remember { mutableStateOf(ev.epochDay) }
    var start by remember { mutableStateOf(ev.startMin) }
    var end by remember { mutableStateOf(ev.endMin) }
    val exists = Repo.events.any { it.id == ev.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exists) "Etkinliği düzenle" else "Yeni etkinlik") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth())
                DateField("Tarih", day, clearable = false) { if (it != null) day = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeField("Başlangıç", start) { start = it }
                    TimeField("Bitiş", end) { end = it }
                }
                OutlinedTextField(note, { note = it }, label = { Text("Not") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    Repo.saveEvent(ev.copy(title = title.trim(), note = note.trim(), epochDay = day, startMin = start, endMin = end))
                }
                onDismiss()
            }) { Text("Kaydet") }
        },
        dismissButton = {
            Row {
                if (exists) {
                    TextButton(onClick = { Repo.deleteEvent(ev.id); onDismiss() }) {
                        Text("Sil", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("İptal") }
            }
        }
    )
}
