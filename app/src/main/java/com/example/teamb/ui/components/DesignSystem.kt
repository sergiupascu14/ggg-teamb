package com.example.teamb.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.teamb.R
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.LocalAppColors
import com.example.teamb.ui.theme.BrandSky
import com.example.teamb.ui.theme.CardBorder
import com.example.teamb.ui.theme.CardSurface
import com.example.teamb.ui.theme.DisabledFill
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.GarminBlueMid
import com.example.teamb.ui.theme.Navy
import com.example.teamb.ui.theme.OnBrand
import com.example.teamb.ui.theme.TextDisabled
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.theme.TextPrimary
import com.example.teamb.ui.theme.TextSecondary

/** Brand hero gradient (navy → classic blue → sky), leading with the primary brand color. */
val BrandGradient: Brush
    get() = Brush.linearGradient(listOf(Navy, GarminBlueMid, BrandSky))

/**
 * The official Garmin wordmark + triangle used in every header.
 * On dark/branded surfaces (`onDark`, or whenever the dark theme is active) the wordmark is
 * tinted white so it stays legible; on light surfaces the original black + blue logo is used.
 */
@Composable
fun GarminLogo(onDark: Boolean = false) {
    val renderLight = onDark || LocalAppColors.current.isDark
    Image(
        painter = painterResource(R.drawable.garmin_logo),
        contentDescription = "Garmin",
        contentScale = ContentScale.Fit,
        colorFilter = if (renderLight) ColorFilter.tint(OnBrand) else null,
        modifier = Modifier.height(24.dp),
    )
}

/** White rounded header bar with the Garmin logo (canvas background screens). */
@Composable
fun GarminHeader(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardSurface,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        shadowElevation = 1.dp,
    ) {
        Box(Modifier.padding(start = 24.dp, top = 16.dp, bottom = 16.dp)) {
            GarminLogo()
        }
    }
}

/** Large screen title + optional streak line and/or muted subtitle. */
@Composable
fun ScreenTitle(
    title: String,
    streak: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        if (streak != null) {
            Text(
                streak,
                style = MaterialTheme.typography.titleSmall,
                color = GarminBlue,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** White rounded content card with the standard soft border + shadow. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    padding: Int = 20,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 3.dp,
    ) {
        Box(Modifier.padding(padding.dp)) { content() }
    }
}

/** Solid blue primary action button. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Navy,
            contentColor = OnBrand,
            disabledContainerColor = DisabledFill,
            disabledContentColor = TextDisabled,
        ),
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

/** White pill with a blue outline + blue label (secondary action). */
@Composable
fun OutlinedPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, GarminBlue),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = GarminBlue, containerColor = CardSurface),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 0.dp))
        }
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.padding(start = if (leadingIcon != null) 8.dp else 0.dp))
    }
}

/** Soft blue info banner with an "i" badge (e.g. the Daily Pulse tip). */
@Composable
fun InfoBanner(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AccentBlue,
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = CardSurface, modifier = Modifier.size(28.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = GarminBlue, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

/** Small rounded tag (e.g. a feedback category) with custom colors. */
@Composable
fun Tag(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Surface(color = bg, shape = RoundedCornerShape(13.dp)) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = fg,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
    }
}

/** Section label (e.g. "Category", "Rewards"). */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
}
