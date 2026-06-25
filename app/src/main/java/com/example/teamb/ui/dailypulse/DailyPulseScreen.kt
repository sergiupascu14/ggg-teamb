package com.example.teamb.ui.dailypulse

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamb.AppContainer
import com.example.teamb.data.model.WeeklyPulse
import com.example.teamb.data.util.Dates
import com.example.teamb.ui.components.AppTextField
import com.example.teamb.ui.components.GarminHeader
import com.example.teamb.ui.components.InfoBanner
import com.example.teamb.ui.components.PrimaryButton
import com.example.teamb.ui.components.ScreenTitle
import com.example.teamb.ui.components.SurfaceCard
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.BrandCyan
import com.example.teamb.ui.theme.BrandSky
import com.example.teamb.ui.theme.CardBorder
import com.example.teamb.ui.theme.CardSurface
import com.example.teamb.ui.theme.FlameOrange
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.InputBorder
import com.example.teamb.ui.theme.Navy
import com.example.teamb.ui.theme.OnBrand
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.theme.TextSecondary

private val MOODS = listOf("😞", "🙁", "😐", "🙂", "😄")

// Distinct, theme-stable series colors for the weekly graph.
private val YOU_COLOR = FlameOrange
private val FLOOR_COLOR = BrandCyan
private val COMPANY_COLOR = BrandSky

@Composable
fun DailyPulseScreen(container: AppContainer) {
    val vm: DailyPulseViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DailyPulseViewModel(
                    container.dailyPulseRepository,
                    container.pulseSyncRepository,
                    container.clock,
                ) as T
        },
    )
    val state by vm.state.collectAsState()
    val profile by container.profileStore.profile.collectAsState(initial = null)

    LaunchedEffect(profile) {
        vm.configure(profile?.staffId, profile?.building?.ifBlank { null }, profile?.floor?.takeIf { it > 0 })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { },
    )
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val streakLabel = "🔥 ${state.streak} ${if (state.streak == 1) "day" else "days"}"

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        GarminHeader()
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp)) {
            ScreenTitle("Daily Pulse", streakLabel)

            Box(Modifier.padding(top = 16.dp)) {
                when {
                    state.loading -> SurfaceCard {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = GarminBlue)
                        }
                    }
                    state.checkedInToday -> CheckedInCard()
                    else -> PulseForm(submitting = state.submitting, onSubmit = vm::submit)
                }
            }

            Box(Modifier.padding(top = 16.dp)) {
                WeeklyPulseCard(weekly = state.weekly)
            }

            Box(Modifier.padding(top = 16.dp)) {
                InfoBanner(
                    if (state.checkedInToday) "Today's entry is saved. Keep the streak going."
                    else "Daily Pulse helps your workplace team spot trends. Your note is always optional."
                )
            }
        }
    }
}

@Composable
private fun PulseForm(submitting: Boolean, onSubmit: (Int, String?) -> Unit) {
    var mood by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    SurfaceCard {
        Column {
            Text("How are you feeling today?", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(
                "Choose the mood that best matches your day.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MOODS.forEachIndexed { index, emoji ->
                    val value = index + 1
                    val selected = mood == value
                    Surface(
                        modifier = Modifier.size(48.dp).selectable(selected) { mood = value },
                        shape = CircleShape,
                        color = if (selected) AccentBlue else CardSurface,
                        border = BorderStroke(if (selected) 2.dp else 1.5.dp, if (selected) GarminBlue else InputBorder),
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(emoji, fontSize = MaterialTheme.typography.titleLarge.fontSize) }
                    }
                }
            }
            Box(Modifier.padding(top = 18.dp)) {
                AppTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "Add a note (optional)",
                    singleLine = false,
                    minLines = 3,
                )
            }
            Box(Modifier.padding(top = 16.dp)) {
                PrimaryButton(
                    text = if (submitting) "Submitting…" else "Submit",
                    onClick = { onSubmit(mood, note.ifBlank { null }) },
                    enabled = mood in 1..5 && !submitting,
                )
            }
        }
    }
}

@Composable
private fun CheckedInCard() {
    SurfaceCard(padding = 28) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Success badge: white check inside navy circle inside accent square.
            Surface(
                modifier = Modifier.size(96.dp).padding(top = 12.dp),
                shape = RoundedCornerShape(28.dp),
                color = AccentBlue,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(shape = CircleShape, color = Navy, modifier = Modifier.size(56.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = OnBrand, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
            Text(
                "You checked in today",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                "Thanks for sharing your pulse.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** "This week" pulse: company & floor averages + a 7-day line graph of you/floor/company. */
@Composable
private fun WeeklyPulseCard(weekly: WeeklyPulse) {
    SurfaceCard {
        Column(Modifier.fillMaxWidth()) {
            Text("This week", style = MaterialTheme.typography.titleMedium, color = TextPrimary)

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AverageChip("Company", weekly.companyAverage, COMPANY_COLOR, Modifier.weight(1f))
                AverageChip("Floor", weekly.floorAverage, FLOOR_COLOR, Modifier.weight(1f))
                AverageChip("You", weekly.youAverage, YOU_COLOR, Modifier.weight(1f))
            }

            if (!weekly.hasData) {
                Text(
                    "No pulse data yet this week — check in to start the trend.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else {
                WeeklyPulseChart(weekly, modifier = Modifier.padding(top = 16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LegendDot("You", YOU_COLOR)
                    LegendDot("Floor", FLOOR_COLOR)
                    LegendDot("Company", COMPANY_COLOR)
                }
            }
        }
    }
}

@Composable
private fun AverageChip(label: String, value: Double?, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = AccentBlue,
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(
                value?.let { String.format("%.1f", it) } ?: "—",
                style = MaterialTheme.typography.titleLarge,
                color = color,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun WeeklyPulseChart(weekly: WeeklyPulse, modifier: Modifier = Modifier) {
    val gridColor = CardBorder
    val you = weekly.days.map { it.youMood }
    val floor = weekly.days.map { it.floorAverage }
    val company = weekly.days.map { it.companyAverage }

    Column(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(150.dp),
        ) {
            val leftPad = 6f
            val rightPad = 6f
            val topPad = 8f
            val bottomPad = 8f
            val chartW = size.width - leftPad - rightPad
            val chartH = size.height - topPad - bottomPad
            val n = weekly.days.size.coerceAtLeast(1)

            fun xFor(i: Int) = leftPad + if (n == 1) chartW / 2 else chartW * i / (n - 1)
            // Mood 1..5 mapped bottom..top.
            fun yFor(v: Double) = topPad + chartH * (1f - ((v.toFloat() - 1f) / 4f))

            // Horizontal gridlines for moods 1..5.
            for (mood in 1..5) {
                val y = yFor(mood.toDouble())
                drawLine(gridColor, Offset(leftPad, y), Offset(size.width - rightPad, y), strokeWidth = 1f)
            }

            fun drawSeries(values: List<Double?>, color: Color) {
                val pts = values.mapIndexedNotNull { i, v -> v?.let { Offset(xFor(i), yFor(it)) } }
                for (k in 1 until pts.size) {
                    drawLine(color, pts[k - 1], pts[k], strokeWidth = 6f, cap = StrokeCap.Round)
                }
                pts.forEach { p -> drawCircle(color, radius = 6f, center = p) }
            }

            // Company first (back), then floor, then you (front, emphasized).
            drawSeries(company, COMPANY_COLOR)
            drawSeries(floor, FLOOR_COLOR)
            drawSeries(you, YOU_COLOR)
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            // Labels reflect each day's actual weekday across the rolling window.
            weekly.days.forEach { point ->
                Text(
                    Dates.weekdayInitial(point.date),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }
        }
    }
}
