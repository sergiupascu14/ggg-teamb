package com.example.teamb.ui.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.teamb.data.model.FeedbackCategory
import com.example.teamb.ui.components.GarminHeader
import com.example.teamb.ui.components.PrimaryButton
import com.example.teamb.ui.components.ScreenTitle
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.CardBorder
import com.example.teamb.ui.theme.CardSurface
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.InputBorder
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.theme.TextSecondary

/** Emoji glyph shown for each category card. */
private fun FeedbackCategory.glyph(): String = when (this) {
    FeedbackCategory.ELEVATORS -> "🛗"
    FeedbackCategory.KITCHEN -> "🍽️"
    FeedbackCategory.DESK_AREA -> "🪑"
    FeedbackCategory.MEETING_ROOMS -> "📅"
    FeedbackCategory.BATHROOMS -> "🚻"
    FeedbackCategory.TEMPERATURE -> "🌡️"
    FeedbackCategory.PARKING -> "🅿️"
    FeedbackCategory.OTHER -> "💬"
}

/**
 * Quick-pick chips that jump straight into the form with a category preselected and a starter
 * message prefilled, so the user only needs to adjust the details.
 */
private data class QuickChipSpec(val label: String, val category: FeedbackCategory, val note: String)

private val QUICK_CHIPS = listOf(
    QuickChipSpec("Too noisy", FeedbackCategory.OTHER, "It's too noisy to focus in my area."),
    QuickChipSpec("Too hot", FeedbackCategory.TEMPERATURE, "It's too hot in my area — could the temperature be adjusted?"),
    QuickChipSpec("No rooms", FeedbackCategory.MEETING_ROOMS, "There are no meeting rooms available when I need one."),
)

/**
 * Step one of the report flow: pick a category (Screen 5 of the design). Continuing opens the
 * detailed [com.example.teamb.ui.feedback.FeedbackScreen] with the category preselected.
 */
@Composable
fun ReportCategoryScreen(onContinue: (categoryName: String, note: String?, community: Boolean) -> Unit) {
    var selected by remember { mutableStateOf<FeedbackCategory?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        GarminHeader()
        Column(Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 20.dp)) {
            ScreenTitle("Report an issue", subtitle = "Pick a category to get started.")

            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FeedbackCategory.entries.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowItems.forEach { category ->
                            CategoryCard(
                                category = category,
                                selected = selected == category,
                                modifier = Modifier.weight(1f),
                                onClick = { selected = category },
                            )
                        }
                        if (rowItems.size == 1) Box(Modifier.weight(1f))
                    }
                }
            }

            Text(
                "Quick chips",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(top = 24.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QUICK_CHIPS.forEach { chip ->
                    // Quick chips post straight to the community.
                    QuickChip(label = chip.label, onClick = { onContinue(chip.category.name, chip.note, true) })
                }
            }

            Box(Modifier.padding(top = 24.dp)) {
                PrimaryButton(
                    text = "Continue",
                    onClick = { selected?.let { onContinue(it.name, null, false) } },
                    enabled = selected != null,
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: FeedbackCategory,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier.height(116.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = if (selected) AccentBlue else CardSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) GarminBlue else CardBorder),
        shadowElevation = if (selected) 0.dp else 3.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(category.glyph(), style = MaterialTheme.typography.headlineSmall)
            Text(
                category.label,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AccentBlue,
        border = BorderStroke(1.dp, InputBorder),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = GarminBlue,
        )
    }
}
