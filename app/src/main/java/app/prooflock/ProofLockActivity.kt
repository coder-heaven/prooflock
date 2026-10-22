package app.prooflock

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

private val Midnight = Color(0xFF09120F)
private val Ink = Color(0xFFF2F8F4)
private val Muted = Color(0xFFA3B7AD)
private val Panel = Color(0xFF14231D)
private val Raised = Color(0xFF1B3027)
private val Emerald = Color(0xFF22D39E)
private val EmeraldDark = Color(0xFF15483A)
private val Amber = Color(0xFFFFD58A)
private val Red = Color(0xFFFFA59B)

class ProofLockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProofLockApp() }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProofLockApp() {
    val context = LocalContext.current
    val repository = remember { ProofLockRepository(context) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(repository.state()) }
    var tab by remember { mutableStateOf("today") }
    var setup by remember { mutableStateOf(!context.getSharedPreferences("prooflock", Context.MODE_PRIVATE).getBoolean("onboarded", false)) }
    var selectedHabit by remember { mutableStateOf<String?>(null) }
    var proofBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var proofMessage by remember { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val habit = state.habits.firstOrNull { it.id == selectedHabit } ?: return@rememberLauncherForActivityResult
        if (bitmap == null) {
            proofMessage = "No photo captured. Your commitment is still protected."
        } else {
            proofBitmap = bitmap
            proofMessage = "Checking your proof with ProofLock AI…"
            scope.launch {
                val result = withContext(Dispatchers.IO) { ProofLockBackend.verify(habit, bitmap) }
                if (result.status == "pass") {
                    repository.markComplete(habit.id, timeLabel())
                    state = repository.state()
                    proofMessage = "Proof accepted · ${result.reason}"
                } else proofMessage = result.reason
            }
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) camera.launch(null) else proofMessage = "Camera permission is needed for fresh proof." }
    val projection = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val intent = Intent(context, ScreenShareService::class.java).putExtra(ScreenShareService.EXTRA_RESULT_CODE, result.resultCode).putExtra(ScreenShareService.EXTRA_DATA, result.data)
            ContextCompat.startForegroundService(context, intent)
            proofMessage = "Screen sharing is active for this online task."
        }
    }
    fun capture(habitId: String) {
        selectedHabit = habitId
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera.launch(null) else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    MaterialTheme(colorScheme = proofLockColors()) {
        if (setup) {
            SetupScreen(state.habits, onSave = { habits ->
                repository.saveHabits(habits)
                context.getSharedPreferences("prooflock", Context.MODE_PRIVATE).edit().putBoolean("onboarded", true).apply()
                state = repository.state(); setup = false
            })
        } else {
            Scaffold(
                containerColor = Midnight,
                topBar = { AppTopBar() },
                bottomBar = {
                    NavigationBar(containerColor = Panel, modifier = Modifier.navigationBarsPadding()) {
                        NavItem("today", "Today", Icons.Default.Today, tab) { tab = "today" }
                        NavItem("history", "History", Icons.Default.History, tab) { tab = "history" }
                        NavItem("settings", "Settings", Icons.Default.Settings, tab) { tab = "settings" }
                    }
                }
            ) { padding ->
                when (tab) {
                    "history" -> HistoryScreen(state, Modifier.padding(padding))
                    "settings" -> SettingsScreen(state, repository, onRefresh = { state = repository.state() }, onShare = { projection.launch((context.getSystemService(MediaProjectionManager::class.java)).createScreenCaptureIntent()) }, Modifier.padding(padding))
                    else -> TodayScreen(state, proofBitmap, proofMessage, ::capture, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppTopBar() {
    TopAppBar(title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(34.dp), CircleShape, color = Emerald) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, null, tint = Midnight, modifier = Modifier.size(18.dp)) } }
            Spacer(Modifier.width(10.dp)); Text("ProofLock", fontWeight = FontWeight.Bold)
        }
    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Midnight, titleContentColor = Ink))
}

@Composable
private fun RowScope.NavItem(route: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: String, onClick: () -> Unit) {
    Column(Modifier.weight(1f).clickable(onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(icon, label, tint = if (route == selected) Emerald else Muted)
        Text(label, color = if (route == selected) Emerald else Muted, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SetupScreen(current: List<Habit>, onSave: (List<Habit>) -> Unit) {
    var selected by remember { mutableStateOf(current.map { it.id }.toSet()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(Modifier.height(20.dp))
        Surface(Modifier.size(64.dp), RoundedCornerShape(21.dp), color = Emerald) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Spa, null, tint = Midnight, modifier = Modifier.size(35.dp)) } }
        Text("Build your better day.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Choose habits that feel real to you. ProofLock keeps the habit app available and protects distracting apps until your check-ins are done.", color = Muted, style = MaterialTheme.typography.bodyLarge, lineHeight = 25.sp)
        HabitTemplates.all.forEach { habit -> HabitToggle(habit, habit.id in selected) { selected = if (habit.id in selected) selected - habit.id else selected + habit.id } }
        Button(onClick = { onSave(HabitTemplates.all.filter { it.id in selected }.ifEmpty { HabitTemplates.all.take(1) }) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Midnight)) { Text("Start my habit plan", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun HabitToggle(habit: Habit, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = if (selected) EmeraldDark else Panel), border = if (selected) BorderStroke(1.dp, Emerald) else null, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(34.dp), CircleShape, color = if (selected) Emerald else Raised) { Box(contentAlignment = Alignment.Center) { if (selected) Icon(Icons.Default.Check, null, tint = Midnight) } }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(habit.title, fontWeight = FontWeight.SemiBold); Text(habit.subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
            Text(habit.kind.name.lowercase().replaceFirstChar { it.uppercase() }, color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun TodayScreen(state: ProofLockState, proof: Bitmap?, message: String?, onCapture: (String) -> Unit, modifier: Modifier) {
    val complete = state.allComplete
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column { Text("${dateLabel()} · ${state.completedToday}/${state.activeHabits.size} COMPLETE", color = Emerald, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold); Text(if (complete) "You showed up." else "Your next win is here.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            StatusPill(complete)
        }
        Card(shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(containerColor = Raised)) {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF1B4E3D), Raised)))) {
                Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("YOUR DAILY PROMISE", color = Emerald, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(if (complete) "Proof accepted for every habit" else "Do the thing before the scroll", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (complete) "Your next unlock is earned. Let the win land." else "${state.activeHabits.size - state.completedToday} check-in(s) left today.", color = Muted, lineHeight = 22.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { repeat(7) { index -> Surface(Modifier.size(if (index == 0) 24.dp else 20.dp), CircleShape, color = if (index == 0 && complete) Emerald else Panel) { Box(contentAlignment = Alignment.Center) { if (index == 0 && complete) Icon(Icons.Default.Check, null, tint = Midnight, modifier = Modifier.size(14.dp)) } } } }
                }
            }
        }
        state.activeHabits.forEach { habit -> HabitCard(habit, onCapture) }
        if (message != null) Text(message, color = if (message.contains("accepted")) Emerald else Red, style = MaterialTheme.typography.bodySmall)
        if (proof != null) Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) { Image(proof.asImageBitmap(), "Fresh proof", Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(15.dp)), contentScale = ContentScale.Crop) }
        ProtectionCard(state)
        Text("Fresh camera proof is checked against each habit. Gallery uploads are not accepted in the proof flow.", color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
    }
}

@Composable
private fun HabitCard(habit: Habit, onCapture: (String) -> Unit) {
    val done = habit.completedToday()
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(habit.kind.name, color = Emerald, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold); Text(habit.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(habit.subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }; Icon(if (done) Icons.Default.CheckCircle else Icons.Default.Spa, null, tint = if (done) Emerald else Amber, modifier = Modifier.size(28.dp)) }
            Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(8.dp)).background(Midnight)) { Box(Modifier.fillMaxWidth(if (done) 1f else .08f).height(8.dp).background(Emerald)) }; Spacer(Modifier.width(10.dp)); Text(if (done) "100%" else "0%", fontWeight = FontWeight.Bold) }
            Button(onClick = { onCapture(habit.id) }, enabled = !done, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Midnight, disabledContainerColor = EmeraldDark, disabledContentColor = Emerald)) { Icon(if (done) Icons.Default.CheckCircle else Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text(if (done) "Proof complete" else "Capture fresh proof", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun StatusPill(complete: Boolean) { Surface(shape = RoundedCornerShape(30.dp), color = if (complete) EmeraldDark else Raised) { Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (complete) Icons.Default.CheckCircle else Icons.Default.Lock, null, tint = if (complete) Emerald else Amber, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)); Text(if (complete) "EARNED" else "PROTECTED", color = if (complete) Emerald else Amber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) } } }

@Composable
private fun ProtectionCard(state: ProofLockState) { Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(21.dp)) { Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(44.dp), CircleShape, color = if (state.allComplete) EmeraldDark else Raised) { Box(contentAlignment = Alignment.Center) { Icon(if (state.allComplete) Icons.Default.Shield else Icons.Default.Lock, null, tint = if (state.allComplete) Emerald else Amber) } }; Spacer(Modifier.width(13.dp)); Column(Modifier.weight(1f)) { Text("Phone protection", fontWeight = FontWeight.Bold); Text(if (state.allComplete) "Today's access is earned." else "${state.selectedPackages.size} distracting app(s) protected.", color = Muted, style = MaterialTheme.typography.bodySmall) }; Text(if (state.allComplete) "OPEN" else "ON", color = if (state.allComplete) Emerald else Amber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) } } }

@Composable
private fun HistoryScreen(state: ProofLockState, modifier: Modifier) {
    val completed = state.activeHabits.sumOf { it.completedDates.size }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Every honest check-in makes the next one easier.", color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("${state.completedToday}", "today", Modifier.weight(1f)); MetricCard("$completed", "proofs logged", Modifier.weight(1f)) }
        state.activeHabits.forEach { habit ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(habit.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("${habit.streak()} day streak", color = Emerald, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    Text("${habit.completedDates.size} completed day(s)", color = Muted, style = MaterialTheme.typography.bodySmall)
                    (0..6).forEach { offset -> HistoryRow(dayLabel(offset), habit.completedDates.contains(LocalDate.now().minusDays(offset.toLong()).toString())) }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier) { Card(modifier, colors = CardDefaults.cardColors(containerColor = Raised), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Emerald); Text(label, color = Muted, style = MaterialTheme.typography.bodySmall) } } }

@Composable
private fun HistoryRow(day: String, done: Boolean) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(28.dp), CircleShape, color = if (done) Emerald else Raised) { Box(contentAlignment = Alignment.Center) { if (done) Icon(Icons.Default.Check, null, tint = Midnight, modifier = Modifier.size(15.dp)) } }; Spacer(Modifier.width(10.dp)); Text(day, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall); Text(if (done) "Complete" else "Waiting", color = if (done) Emerald else Muted, style = MaterialTheme.typography.labelSmall) } }

@Composable
private fun SettingsScreen(state: ProofLockState, repository: ProofLockRepository, onRefresh: () -> Unit, onShare: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf(AppCatalog.launchable(context)) }
    var showApps by remember { mutableStateOf(false) }
    var reminderEnabled by remember { mutableStateOf(state.reminders.enabled) }
    var escalation by remember { mutableStateOf(state.reminders.escalationMinutes.toFloat()) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Control your environment", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Make the good choice easier to reach and the distracting choice harder to open.", color = Muted)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Apps, null, tint = Emerald); Spacer(Modifier.width(10.dp)); Text("Distracting apps", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("${state.selectedPackages.size} selected", color = Emerald, style = MaterialTheme.typography.labelSmall) }
                Text("Pick social, video, game, chat, shopping, or any other app you want ProofLock to protect.", color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { showApps = !showApps; apps = AppCatalog.launchable(context) }, modifier = Modifier.fillMaxWidth()) { Text(if (showApps) "Hide app list" else "Choose distracting apps") }
                if (showApps) apps.take(80).forEach { app ->
                    Row(Modifier.fillMaxWidth().clickable { val next = if (app.packageName in state.selectedPackages) state.selectedPackages - app.packageName else state.selectedPackages + app.packageName; repository.setSelectedPackages(next); onRefresh() }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Text(app.label, modifier = Modifier.weight(1f)); Text(if (app.packageName in state.selectedPackages) "PROTECTED" else if (app.distracting) "SUGGESTED" else "OFF", color = if (app.packageName in state.selectedPackages) Emerald else Muted, style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Lock, null, tint = Amber); Spacer(Modifier.width(10.dp)); Text("Phone lock", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Switch(checked = state.protectionEnabled, onCheckedChange = { repository.setProtectionEnabled(it); onRefresh() }) }
                Text("When active, the Accessibility service redirects selected apps back to ProofLock until today's habits are complete.", color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("Connect app protection") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Notifications, null, tint = Emerald); Spacer(Modifier.width(10.dp)); Text("Escalating reminders", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it; repository.setReminders(state.reminders.copy(enabled = it)); onRefresh() }) }
                Text("If a reminder is ignored, later reminders become more urgent. The final step turns protection on.", color = Muted, style = MaterialTheme.typography.bodySmall)
                Text("Escalate every ${escalation.toInt()} minutes", style = MaterialTheme.typography.bodySmall)
                Slider(value = escalation, onValueChange = { escalation = it; repository.setReminders(state.reminders.copy(escalationMinutes = it.toInt())); onRefresh() }, valueRange = 5f..60f, steps = 10)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ScreenShare, null, tint = Emerald); Spacer(Modifier.width(10.dp)); Text("Online task sharing", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)) }
                Text("Share your screen during a selected online lesson or focus task. Android will always ask for consent before sharing.", color = Muted, style = MaterialTheme.typography.bodySmall)
                Button(onClick = onShare, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Midnight)) { Text("Start a screen-share task") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("ProofLock AI", fontWeight = FontWeight.Bold); Text(if (state.backendConfigured) "Online verification backend connected. Nine capability slots are available, each with two configured backups." else "Local prototype mode is active. Add PROOFLOCK_API_URL in the GitHub build workflow to enable online AI verification.", color = Muted, style = MaterialTheme.typography.bodySmall) } }
    }
}

private fun dateLabel(): String = SimpleDateFormat("MMM d", Locale.US).format(Date()).uppercase(Locale.US)
private fun timeLabel(): String = SimpleDateFormat("h:mm a", Locale.US).format(Date())
private fun dayLabel(offset: Int): String = SimpleDateFormat("EEE, MMM d", Locale.US).format(Date(System.currentTimeMillis() - offset * 86_400_000L))

@Composable
private fun proofLockColors() = androidx.compose.material3.darkColorScheme(primary = Emerald, onPrimary = Midnight, background = Midnight, surface = Panel, onBackground = Ink, onSurface = Ink, onSurfaceVariant = Muted)
