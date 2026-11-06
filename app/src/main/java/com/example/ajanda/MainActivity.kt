package com.example.ajanda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.ajanda.data.Repo
import com.example.ajanda.ui.AgendaScreen
import com.example.ajanda.ui.B
import com.example.ajanda.ui.BIcons
import com.example.ajanda.ui.BTheme
import com.example.ajanda.ui.CalcScreen
import com.example.ajanda.ui.HomeScreen
import com.example.ajanda.ui.NotesScreen
import com.example.ajanda.ui.RailItem
import com.example.ajanda.ui.TodoScreen
import com.example.ajanda.ui.TrackScreen
import com.example.ajanda.ui.createNote

enum class Section(val label: String, val icon: ImageVector) {
    HOME("Ana sayfa", BIcons.Home),
    TASKS("Görevler", BIcons.Tasks),
    AGENDA("Ajanda", BIcons.Calendar),
    NOTES("Notlar", BIcons.Pen),
    TRACK("Takip", BIcons.Track),
    CALC("Hesap", BIcons.Calc)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Repo.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            BTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    var section by rememberSaveable { mutableStateOf(Section.HOME) }
    var openNote by rememberSaveable { mutableStateOf<Long?>(null) }

    Surface(Modifier.fillMaxSize(), color = B.Bg, contentColor = B.Text) {
        Row(Modifier.fillMaxSize().safeDrawingPadding()) {
            Column(
                Modifier.width(64.dp).fillMaxHeight().padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ana sayfa düğmesi
                Surface(
                    onClick = { section = Section.HOME; openNote = null },
                    shape = RoundedCornerShape(12.dp),
                    color = if (section == Section.HOME) B.AccentSoft else Color(0x0FFFFFFF),
                    contentColor = if (section == Section.HOME) B.Accent else B.Text2,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(BIcons.Home, "Ana sayfa", Modifier.size(20.dp)) }
                }
                Spacer(Modifier.height(14.dp))
                Section.entries.drop(1).forEach { s ->
                    RailItem(s.label, s.icon, section == s) {
                        if (s == Section.NOTES && section == Section.NOTES) openNote = null
                        section = s
                    }
                }
            }
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .padding(vertical = 0.dp)
                    .then(Modifier)
            ) {
                Surface(Modifier.fillMaxSize(), color = Color(0x0FFFFFFF)) {}
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (section) {
                    Section.HOME -> HomeScreen(
                        onGo = { i -> section = Section.entries[i] },
                        onOpenNote = { id -> openNote = id; section = Section.NOTES },
                        onNewNote = { openNote = createNote(); section = Section.NOTES }
                    )
                    Section.TASKS -> TodoScreen()
                    Section.AGENDA -> AgendaScreen()
                    Section.NOTES -> NotesScreen(openNote) { openNote = it }
                    Section.TRACK -> TrackScreen()
                    Section.CALC -> CalcScreen()
                }
            }
        }
    }
}
