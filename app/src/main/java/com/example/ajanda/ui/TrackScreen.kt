@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ajanda.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ajanda.data.DEFAULT_STATUSES
import com.example.ajanda.data.Repo
import com.example.ajanda.data.TrackItem
import com.example.ajanda.data.TrackList

fun statusColor(list: TrackList, status: String): Color {
    val i = list.statuses.indexOf(status)
    return if (i < 0) Color(0xFF757575) else STATUS_COLORS[i % STATUS_COLORS.size]
}

@Composable
fun TrackScreen() {
    var listId by rememberSaveable { mutableStateOf(Repo.trackLists.firstOrNull()?.id ?: 0L) }
    var statusFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var newTitle by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<TrackItem?>(null) }
    var listDialog by remember { mutableStateOf<TrackList?>(null) }
    var confirmDelete by remember { mutableStateOf<TrackList?>(null) }

    val list = Repo.trackLists.firstOrNull { it.id == listId } ?: Repo.trackLists.firstOrNull()

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Takip", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            if (list != null) {
                IconButton(onClick = { listDialog = list }) { Icon(Icons.Filled.Edit, "Listeyi düzenle") }
                IconButton(onClick = { confirmDelete = list }) { Icon(Icons.Filled.Delete, "Listeyi sil") }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Listeler
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Repo.trackLists.forEach { l ->
                FilterChip(
                    selected = l.id == list?.id,
                    onClick = { listId = l.id; statusFilter = null },
                    label = { Text(l.name) }
                )
            }
            AssistChip(
                onClick = { listDialog = TrackList(Repo.newId(), "", DEFAULT_STATUSES) },
                label = { Text("Yeni liste") },
                leadingIcon = { Icon(Icons.Filled.Add, null) }
            )
        }

        if (list == null) {
            Text("Henüz liste yok.", modifier = Modifier.padding(top = 24.dp))
        } else {
            val all = Repo.trackItems.filter { it.listId == list.id }

            // Durum filtresi
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("Tümü (${all.size})") }
                )
                list.statuses.forEach { s ->
                    FilterChip(
                        selected = statusFilter == s,
                        onClick = { statusFilter = s },
                        label = { Text("$s (${all.count { it.status == s }})") }
                    )
                }
            }

            fun add() {
                val t = newTitle.trim()
                if (t.isEmpty()) return
                Repo.saveTrackItem(TrackItem(Repo.newId(), list.id, t, statusFilter ?: list.statuses.first()))
                newTitle = ""
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    placeholder = { Text("Yeni kayıt ekle…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { add() }) { Icon(Icons.Filled.Add, "Ekle") }
            }
            Spacer(Modifier.height(8.dp))

            val shown = all.filter { statusFilter == null || it.status == statusFilter }
                .sortedWith(
                    compareBy<TrackItem>(
                        { list.statuses.indexOf(it.status).let { i -> if (i < 0) 99 else i } },
                        { it.dueEpochDay ?: Long.MAX_VALUE },
                        { -it.updatedAt }
                    )
                )
            LazyColumn(Modifier.fillMaxSize()) {
                items(shown, key = { it.id }) { item -> TrackRow(list, item) { editing = item } }
            }
        }
    }

    val editItem = editing
    val editList = editItem?.let { item -> Repo.trackLists.firstOrNull { it.id == item.listId } }
    if (editItem != null && editList != null) {
        TrackItemDialog(editList, editItem) { editing = null }
    }
    listDialog?.let { l ->
        TrackListDialog(l, onDismiss = { listDialog = null }) { saved -> listId = saved.id; listDialog = null }
    }
    confirmDelete?.let { l ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("\"${l.name}\" silinsin mi?") },
            text = { Text("Listedeki tüm kayıtlar da silinecek.") },
            confirmButton = {
                TextButton(onClick = { Repo.deleteTrackList(l.id); confirmDelete = null }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("İptal") } }
        )
    }
}

@Composable
fun TrackRow(list: TrackList, item: TrackItem, onClick: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val today = todayEpoch()
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge)
                if (item.note.isNotBlank()) {
                    Text(
                        item.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            item.dueEpochDay?.let { d ->
                Text(
                    fmtShort(d),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (d < today) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }
            Box {
                Surface(
                    onClick = { menu = true },
                    shape = RoundedCornerShape(50),
                    color = statusColor(list, item.status),
                    contentColor = Color.White
                ) {
                    Text(
                        item.status,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    list.statuses.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s) },
                            onClick = { Repo.saveTrackItem(item.copy(status = s)); menu = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrackItemDialog(list: TrackList, item: TrackItem, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(item.title) }
    var note by remember { mutableStateOf(item.note) }
    var status by remember { mutableStateOf(item.status) }
    var due by remember { mutableStateOf(item.dueEpochDay) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kaydı düzenle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Başlık") }, modifier = Modifier.fillMaxWidth())
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    list.statuses.forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s) })
                    }
                }
                DateField("Termin ekle", due) { due = it }
                OutlinedTextField(note, { note = it }, label = { Text("Not") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isNotBlank()) {
                    Repo.saveTrackItem(item.copy(title = title.trim(), note = note.trim(), status = status, dueEpochDay = due))
                }
                onDismiss()
            }) { Text("Kaydet") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { Repo.deleteTrackItem(item.id); onDismiss() }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text("İptal") }
            }
        }
    )
}

@Composable
fun TrackListDialog(list: TrackList, onDismiss: () -> Unit, onSaved: (TrackList) -> Unit) {
    var name by remember { mutableStateOf(list.name) }
    var statuses by remember { mutableStateOf(list.statuses.joinToString(", ")) }
    val exists = Repo.trackLists.any { it.id == list.id }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exists) "Listeyi düzenle" else "Yeni takip listesi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    name, { name = it },
                    label = { Text("Liste adı (ör. RFI, Siparişler)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    statuses, { statuses = it },
                    label = { Text("Durumlar (virgülle ayır)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val st = statuses.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
                if (name.isNotBlank()) {
                    val saved = list.copy(name = name.trim(), statuses = st.ifEmpty { DEFAULT_STATUSES })
                    Repo.saveTrackList(saved)
                    onSaved(saved)
                } else onDismiss()
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
