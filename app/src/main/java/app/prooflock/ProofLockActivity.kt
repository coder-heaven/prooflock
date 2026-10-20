package app.prooflock

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Midnight = Color(0xFF09120F)
private val Ink = Color(0xFFF2F8F4)
private val Muted = Color(0xFFA3B7AD)
private val Panel = Color(0xFF14231D)
private val PanelRaised = Color(0xFF1B3027)
private val Emerald = Color(0xFF22D39E)
private val EmeraldDark = Color(0xFF15483A)
private val Amber = Color(0xFFFFD58A)
private val Red = Color(0xFFFFA59B)

private data class Commitment(val title: String, val subtitle: String)
private data class Proof(val time: String, val bitmap: Bitmap? = null)

private val templates = listOf(
    Commitment("Move for 20 minutes", "Take care of your body"),
    Commitment("Read 10 pages", "Give your attention a place to land"),
    Commitment("Practice a skill", "Make progress before scrolling"),
    Commitment("Reset my space", "Leave one corner better than you found it")
)

class ProofLockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ProofLock() }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProofLock() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("prooflock", Context.MODE_PRIVATE) }
    var commitment by remember { mutableStateOf(loadCommitment(prefs)) }
    var proof by remember { mutableStateOf(loadProof(prefs)) }
    var editing by remember { mutableStateOf(commitment == null) }
    var tab by remember { mutableStateOf("today") }
    var cameraMessage by remember { mutableStateOf<String?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap == null) {
            cameraMessage = "No photo captured. Your commitment is still protected."
        } else {
            val captured = Proof(timeLabel(), bitmap)
            proof = captured
            prefs.edit().putString("proof_day", todayKey()).putString("proof_time", captured.time).apply()
            cameraMessage = null
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) camera.launch(null) else cameraMessage = "Camera permission is needed for fresh proof."
    }
    fun capture() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera.launch(null)
        else permission.launch(Manifest.permission.CAMERA)
    }

    MaterialTheme(colorScheme = proofLockColors()) {
        if (editing) {
            Onboarding(
                current = commitment,
                onCancel = { if (commitment != null) editing = false },
                onSave = { selected ->
                    commitment = selected
                    prefs.edit().putString("commitment_title", selected.title).putString("commitment_subtitle", selected.subtitle).apply()
                    editing = false
                    tab = "today"
                }
            )
        } else {
            Scaffold(
                containerColor = Midnight,
                topBar = { AppTopBar(tab) },
                bottomBar = {
                    NavigationBar(containerColor = Panel, modifier = Modifier.navigationBarsPadding()) {
                        NavItem("today", "Today", Icons.Default.Today, tab) { tab = "today" }
                        NavItem("history", "History", Icons.Default.History, tab) { tab = "history" }
                        NavItem("settings", "Settings", Icons.Default.Settings, tab) { tab = "settings" }
                    }
                }
            ) { padding ->
                when (tab) {
                    "history" -> HistoryScreen(commitment!!, proof, Modifier.padding(padding))
                    "settings" -> SettingsScreen(commitment!!, onEdit = { editing = true }, Modifier.padding(padding))
                    else -> TodayScreen(commitment!!, proof, cameraMessage, ::capture, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AppTopBar(tab: String) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(34.dp), CircleShape, color = Emerald) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, null, tint = Midnight, modifier = Modifier.size(18.dp)) }
                }
                Spacer(Modifier.width(10.dp))
                Text("ProofLock", fontWeight = FontWeight.Bold)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Midnight, titleContentColor = Ink)
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun NavItem(route: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: String, onClick: () -> Unit) {
    NavigationBarItem(
        selected = route == selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) }
    )
}

@Composable
private fun Onboarding(current: Commitment?, onCancel: () -> Unit, onSave: (Commitment) -> Unit) {
    var selected by remember { mutableStateOf(current ?: templates.first()) }
    var custom by remember { mutableStateOf(if (current != null && current !in templates) current.title else "") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (current != null) TextButton(onClick = onCancel) { Text("Cancel") }
        Spacer(Modifier.height(if (current == null) 28.dp else 0.dp))
        Surface(Modifier.size(64.dp), RoundedCornerShape(21.dp), color = Emerald) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Spa, null, tint = Midnight, modifier = Modifier.size(35.dp)) }
        }
        Text(if (current == null) "Make today count." else "Tune your commitment.", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Choose one real-world action. Complete it first. Your distracting apps come after.", color = Muted, style = MaterialTheme.typography.bodyLarge, lineHeight = 25.sp)
        Text("Pick a starting point", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        templates.forEach { option ->
            val active = option == selected && custom.isBlank()
            CommitmentOption(option, active) { selected = option; custom = "" }
        }
        OutlinedTextField(
            value = custom,
            onValueChange = { custom = it.take(80) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Or create your own") },
            placeholder = { Text("e.g. Journal for 5 minutes") },
            singleLine = true
        )
        Button(
            onClick = {
                val title = custom.trim().ifBlank { selected.title }
                val subtitle = if (custom.isBlank()) selected.subtitle else "Your personal daily promise"
                onSave(Commitment(title, subtitle))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Midnight)
        ) { Text(if (current == null) "Create my commitment" else "Save commitment", fontWeight = FontWeight.Bold) }
        Text("Start small. Consistency is the feature.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
private fun CommitmentOption(option: Commitment, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) EmeraldDark else Panel),
        border = if (selected) BorderStroke(1.dp, Emerald) else null,
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(34.dp), CircleShape, color = if (selected) Emerald else PanelRaised) {
                Box(contentAlignment = Alignment.Center) { if (selected) Icon(Icons.Default.Check, null, tint = Midnight) }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(option.title, fontWeight = FontWeight.SemiBold)
                Text(option.subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Default.ArrowForward, null, tint = if (selected) Emerald else Muted, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TodayScreen(commitment: Commitment, proof: Proof?, message: String?, onCapture: () -> Unit, modifier: Modifier) {
    val complete = proof != null
    val capturedProof = proof?.takeIf { it.bitmap != null }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("${dateLabel()} · DAY 01", color = Emerald, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(if (complete) "You showed up." else "Your next win is here.", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            StatusPill(complete)
        }
        HeroCard(complete)
        CommitmentCard(commitment, complete, onCapture)
        if (message != null) Text(message, color = Red, style = MaterialTheme.typography.bodySmall)
        if (capturedProof != null) ProofPreview(capturedProof)
        ProtectionCard(complete)
        Text("No old photos. No excuses disguised as uploads. ProofLock only opens the proof camera for today's check-in.", color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
    }
}

@Composable
private fun HeroCard(complete: Boolean) {
    Card(shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(containerColor = PanelRaised)) {
        Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF1B4E3D), PanelRaised)))) {
            Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("YOUR DAILY PROMISE", color = Emerald, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(if (complete) "Proof accepted for today" else "Do the thing before the scroll", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(if (complete) "Your next unlock is earned. Let the win land." else "One check-in. One honest photo. One better day.", color = Muted, lineHeight = 22.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(7) { index ->
                        Surface(Modifier.size(if (index == 0) 24.dp else 20.dp), CircleShape, color = if (index == 0 && complete) Emerald else Panel) {
                            Box(contentAlignment = Alignment.Center) { if (index == 0 && complete) Icon(Icons.Default.Check, null, tint = Midnight, modifier = Modifier.size(14.dp)) }
                        }
                        if (index < 6) Spacer(Modifier.width(5.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(if (complete) "1 day streak" else "Start your streak", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(complete: Boolean) {
    Surface(shape = RoundedCornerShape(30.dp), color = if (complete) EmeraldDark else PanelRaised) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (complete) Icons.Default.CheckCircle else Icons.Default.Lock, null, tint = if (complete) Emerald else Amber, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (complete) "EARNED" else "PROTECTED", color = if (complete) Emerald else Amber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CommitmentCard(commitment: Commitment, complete: Boolean, onCapture: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("TODAY'S COMMITMENT", color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(commitment.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                }
                Icon(Icons.Default.Spa, null, tint = Emerald, modifier = Modifier.size(28.dp))
            }
            Text(commitment.subtitle, color = Muted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(8.dp)).background(Midnight)) {
                    Box(Modifier.fillMaxWidth(if (complete) 1f else .08f).height(8.dp).background(Emerald))
                }
                Spacer(Modifier.width(10.dp))
                Text(if (complete) "100%" else "0%", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onCapture,
                enabled = !complete,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Midnight, disabledContainerColor = EmeraldDark, disabledContentColor = Emerald)
            ) {
                Icon(if (complete) Icons.Default.CheckCircle else Icons.Default.CameraAlt, null)
                Spacer(Modifier.width(8.dp))
                Text(if (complete) "Proof complete" else "Capture today's proof", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProofPreview(proof: Proof) {
    val photo = proof.bitmap ?: return
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(photo.asImageBitmap(), "Today's proof", Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(15.dp)), contentScale = ContentScale.Crop)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Emerald)
                Spacer(Modifier.width(8.dp))
                Text("Captured today at ${proof.time}", fontWeight = FontWeight.SemiBold)
            }
            Text("This image came directly from the camera. Gallery uploads are not part of the proof flow.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProtectionCard(complete: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(21.dp)) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), CircleShape, color = if (complete) EmeraldDark else PanelRaised) {
                Box(contentAlignment = Alignment.Center) { Icon(if (complete) Icons.Default.Shield else Icons.Default.Lock, null, tint = if (complete) Emerald else Amber) }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Phone protection", fontWeight = FontWeight.Bold)
                Text(if (complete) "Today's access is earned." else "Distracting apps stay protected until proof passes.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            Text(if (complete) "OPEN" else "ON", color = if (complete) Emerald else Amber, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HistoryScreen(commitment: Commitment, proof: Proof?, modifier: Modifier) {
    val complete = proof != null
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Your history", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Small proof, repeated often, becomes a life you can trust.", color = Muted)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("1", "current streak", Modifier.weight(1f))
            MetricCard(if (complete) "1" else "0", "days completed", Modifier.weight(1f))
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("LAST 7 DAYS", color = Emerald, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                (0..6).forEach { offset ->
                    val isToday = offset == 0
                    val done = isToday && complete
                    HistoryRow(day = dayLabel(offset), done = done, current = isToday, commitment = commitment.title)
                }
            }
        }
        Text("ProofLock keeps history on this phone in this first release. Nothing is uploaded.", color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = PanelRaised), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Emerald)
            Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HistoryRow(day: String, done: Boolean, current: Boolean, commitment: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(Modifier.size(34.dp), CircleShape, color = if (done) Emerald else PanelRaised) {
            Box(contentAlignment = Alignment.Center) {
                if (done) Icon(Icons.Default.Check, null, tint = Midnight, modifier = Modifier.size(17.dp))
                else Text("·", color = Muted, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(if (current) "Today" else day, fontWeight = FontWeight.SemiBold)
            Text(if (done) commitment else "No proof submitted", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Text(if (done) "Complete" else "Waiting", color = if (done) Emerald else Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsScreen(commitment: Commitment, onEdit: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Make the rules simple enough to keep.", color = Muted)
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Daily commitment", color = Emerald, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(commitment.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.Edit, null, tint = Muted)
                }
                Text(commitment.subtitle, color = Muted)
                OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text("Change commitment") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Fresh proof", fontWeight = FontWeight.Bold)
                Text("ProofLock uses the camera instead of the gallery so a previous photo cannot be submitted as today's check-in.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("What's next", fontWeight = FontWeight.Bold)
                Text("The next release will add Android Accessibility enforcement, reminders, and optional AI image verification. This first release keeps the promise flow local and transparent.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun loadCommitment(prefs: android.content.SharedPreferences): Commitment? {
    val title = prefs.getString("commitment_title", null) ?: return null
    return Commitment(title, prefs.getString("commitment_subtitle", "Your personal daily promise") ?: "Your personal daily promise")
}

private fun loadProof(prefs: android.content.SharedPreferences): Proof? {
    if (prefs.getString("proof_day", null) != todayKey()) return null
    return Proof(prefs.getString("proof_time", "") ?: "")
}

private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
private fun dateLabel(): String = SimpleDateFormat("MMM d", Locale.US).format(Date()).uppercase(Locale.US)
private fun timeLabel(): String = SimpleDateFormat("h:mm a", Locale.US).format(Date())
private fun dayLabel(offset: Int): String {
    val millis = System.currentTimeMillis() - offset * 86_400_000L
    return SimpleDateFormat("EEE, MMM d", Locale.US).format(Date(millis))
}

@Composable
private fun proofLockColors() = androidx.compose.material3.darkColorScheme(
    primary = Emerald,
    onPrimary = Midnight,
    background = Midnight,
    surface = Panel,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = Muted
)
