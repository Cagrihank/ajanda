@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ajanda.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

const val DAY_MS = 86_400_000L
val TR: Locale = Locale.forLanguageTag("tr-TR")

val PRIORITY_LABELS = listOf("Düşük", "Normal", "Yüksek")
val PRIORITY_COLORS = listOf(Color(0xFF9E9E9E), Color(0xFF42A5F5), Color(0xFFE53935))
val STATUS_COLORS = listOf(
    Color(0xFF1E88E5), Color(0xFFFB8C00), Color(0xFF8E24AA),
    Color(0xFF43A047), Color(0xFF00897B), Color(0xFF6D4C41)
)

fun todayEpoch(): Long = LocalDate.now().toEpochDay()

fun fmtDate(day: Long): String =
    LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMM yyyy, EEE", TR))

fun fmtShort(day: Long): String {
    val t = todayEpoch()
    return when (day) {
        t -> "Bugün"
        t + 1 -> "Yarın"
        t - 1 -> "Dün"
        else -> LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMM", TR))
    }
}

fun fmtTime(m: Int): String = String.format(Locale.US, "%02d:%02d", m / 60, m % 60)

@Composable
fun DatePickDialog(initial: Long?, onDismiss: () -> Unit, onPick: (Long?) -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = (initial ?: todayEpoch()) * DAY_MS)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(state.selectedDateMillis?.div(DAY_MS)) }) { Text("Tamam") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    ) {
        DatePicker(state = state)
    }
}

@Composable
fun TimePickDialog(initialMin: Int?, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val m = initialMin ?: (9 * 60)
    val state = rememberTimePickerState(initialHour = m / 60, initialMinute = m % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text("Tamam") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
        text = { TimePicker(state = state) }
    )
}

@Composable
fun DateField(label: String, day: Long?, clearable: Boolean = true, onChange: (Long?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { open = true }) { Text(if (day == null) label else fmtDate(day)) }
        if (day != null && clearable) {
            IconButton(onClick = { onChange(null) }) { Icon(Icons.Filled.Close, "Temizle") }
        }
    }
    if (open) DatePickDialog(day, { open = false }) { onChange(it); open = false }
}

@Composable
fun TimeField(label: String, min: Int?, onChange: (Int?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { open = true }) { Text(if (min == null) label else fmtTime(min)) }
        if (min != null) {
            IconButton(onClick = { onChange(null) }) { Icon(Icons.Filled.Close, "Temizle") }
        }
    }
    if (open) TimePickDialog(min, { open = false }) { onChange(it); open = false }
}
