package com.example.teamb.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

/** All navigable routes in the signed-in app. */
object Routes {
    const val PULSE = "pulse"
    const val FREEZER = "freezer"
    const val FEEDBACK = "feedback"
    const val NEWSFEED = "newsfeed"
    const val PROFILE = "profile"
    const val TICKETS = "tickets"
    const val LEADERBOARD = "leaderboard"
}

/** Bottom navigation tabs. */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    PULSE(Routes.PULSE, "Pulse", Icons.Filled.Home),
    FREEZER(Routes.FREEZER, "Freezer", Icons.Filled.Kitchen),
    FEEDBACK(Routes.FEEDBACK, "Feedback", Icons.Filled.Feedback),
    NEWSFEED(Routes.NEWSFEED, "Community", Icons.Filled.Forum),
    PROFILE(Routes.PROFILE, "Profile", Icons.Filled.Person),
}
