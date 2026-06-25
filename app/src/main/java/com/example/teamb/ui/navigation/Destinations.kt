package com.example.teamb.ui.navigation

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

    /** Feedback form deep link carrying an optional preselected category name. */
    const val FEEDBACK_ARG_CATEGORY = "category"
    const val FEEDBACK_WITH_ARG = "$FEEDBACK?$FEEDBACK_ARG_CATEGORY={$FEEDBACK_ARG_CATEGORY}"
    fun feedback(categoryName: String? = null): String =
        if (categoryName == null) FEEDBACK else "$FEEDBACK?$FEEDBACK_ARG_CATEGORY=$categoryName"
}

/** Bottom navigation tabs. Community sits in the middle as the primary shared surface. */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    PULSE(Routes.PULSE, "Pulse", Icons.Filled.Favorite),
    SPACES(Routes.SPACES, "Spaces", Icons.Filled.Apartment),
    NEWSFEED(Routes.NEWSFEED, "Community", Icons.Filled.Public),
    REPORT(Routes.REPORT, "Report", Icons.AutoMirrored.Filled.Chat),
    PROFILE(Routes.PROFILE, "Profile", Icons.Filled.Person),
}
