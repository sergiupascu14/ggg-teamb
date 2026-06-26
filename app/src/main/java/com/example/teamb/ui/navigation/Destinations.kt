package com.example.teamb.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.ui.graphics.vector.ImageVector

/** All navigable routes in the signed-in app. */
object Routes {
    const val PULSE = "pulse"
    const val SPACES = "spaces"
    const val KITCHEN = "kitchen"
    const val REPORT = "report"
    const val FEEDBACK = "feedback"
    const val NEWSFEED = "newsfeed"
    const val PROFILE = "profile"
    const val TICKETS = "tickets"
    const val LEADERBOARD = "leaderboard"

    /**
     * Feedback form deep link carrying an optional preselected category, a prefilled message
     * ([FEEDBACK_ARG_NOTE]) and whether to default "Share with the community" on
     * ([FEEDBACK_ARG_COMMUNITY]). All are optional query params.
     */
    const val FEEDBACK_ARG_CATEGORY = "category"
    const val FEEDBACK_ARG_NOTE = "note"
    const val FEEDBACK_ARG_COMMUNITY = "community"
    const val FEEDBACK_WITH_ARG =
        "$FEEDBACK?$FEEDBACK_ARG_CATEGORY={$FEEDBACK_ARG_CATEGORY}" +
            "&$FEEDBACK_ARG_NOTE={$FEEDBACK_ARG_NOTE}" +
            "&$FEEDBACK_ARG_COMMUNITY={$FEEDBACK_ARG_COMMUNITY}"

    fun feedback(
        categoryName: String? = null,
        note: String? = null,
        community: Boolean = false,
    ): String {
        val params = buildList {
            categoryName?.let { add("$FEEDBACK_ARG_CATEGORY=${Uri.encode(it)}") }
            note?.let { add("$FEEDBACK_ARG_NOTE=${Uri.encode(it)}") }
            if (community) add("$FEEDBACK_ARG_COMMUNITY=true")
        }
        return if (params.isEmpty()) FEEDBACK else "$FEEDBACK?${params.joinToString("&")}"
    }
}

/** Bottom navigation tabs. Hub sits in the middle as the primary shared surface. */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    PULSE(Routes.PULSE, "Pulse", Icons.Filled.Favorite),
    SPACES(Routes.SPACES, "Spaces", Icons.Filled.Apartment),
    NEWSFEED(Routes.NEWSFEED, "Hub", Icons.Filled.Public),
    REPORT(Routes.REPORT, "Report", Icons.AutoMirrored.Filled.Chat),
    PROFILE(Routes.PROFILE, "Profile", Icons.Filled.Person),
}
