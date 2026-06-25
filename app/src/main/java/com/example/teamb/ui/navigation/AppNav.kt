package com.example.teamb.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.teamb.AppContainer
import com.example.teamb.ui.dailypulse.DailyPulseScreen
import com.example.teamb.ui.feedback.FeedbackScreen
import com.example.teamb.ui.report.ReportCategoryScreen
import com.example.teamb.ui.spaces.KitchenDetailScreen
import com.example.teamb.ui.spaces.SpacesScreen
import com.example.teamb.ui.leaderboard.LeaderboardScreen
import com.example.teamb.ui.newsfeed.NewsfeedScreen
import com.example.teamb.ui.onboarding.LoginScreen
import com.example.teamb.ui.onboarding.OnboardingScreen
import com.example.teamb.ui.profile.ProfileScreen
import com.example.teamb.ui.theme.AccentBlue
import com.example.teamb.ui.theme.Canvas
import com.example.teamb.ui.theme.CardSurface
import com.example.teamb.ui.theme.GarminBlue
import com.example.teamb.ui.theme.TextMuted
import com.example.teamb.ui.tickets.TicketsScreen
import kotlinx.coroutines.launch

/**
 * Root composable. Decides between onboarding, password login and the main app, then hosts the
 * bottom-navigation graph.
 */
@Composable
fun AppRoot(container: AppContainer) {
    val profile by container.profileStore.profile.collectAsState(initial = null)
    // Auto-login: if a session was remembered (set on unlock/onboarding), skip the lock screen.
    var unlocked by remember { mutableStateOf(container.credentialStore.isLoggedIn()) }
    val scope = rememberCoroutineScope()

    // Fully sign out: drop the local identity + password so the app returns to onboarding,
    // not just the password lock. Clearing the profile re-routes AppRoot to OnboardingScreen.
    fun signOut() {
        scope.launch {
            unlocked = false
            container.credentialStore.clear()
            container.profileStore.clear()
        }
    }

    val currentProfile = profile
    when {
        currentProfile == null || !currentProfile.isComplete ->
            OnboardingScreen(container, onCompleted = { unlocked = true })

        // Show the lock screen only when there's a password AND no remembered session — a remembered
        // session (set on unlock/onboarding, cleared on sign out) makes the app log in automatically.
        container.credentialStore.hasPassword() && !unlocked && !container.credentialStore.isLoggedIn() ->
            LoginScreen(container, onUnlocked = { unlocked = true })

        else -> MainScaffold(container, onSignOut = ::signOut)
    }
}

@Composable
private fun MainScaffold(container: AppContainer, onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val profile by container.profileStore.profile.collectAsState(initial = null)

    // Land on Community when THIS user's pulse is already done today; otherwise nudge them to check in.
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(profile?.staffId) {
        val uid = profile?.staffId ?: return@LaunchedEffect // wait for the profile to load
        val checkedIn = runCatching { container.dailyPulseRepository.checkedInToday(uid) }.getOrDefault(false)
        startDestination = if (checkedIn) Routes.NEWSFEED else Routes.PULSE
    }
    val start = startDestination ?: return

    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDest = backStackEntry?.destination
            NavigationBar(containerColor = CardSurface) {
                Tab.entries.forEach { tab ->
                    val routes = currentDest?.hierarchy?.mapNotNull { it.route }?.toSet().orEmpty()
                    val selected = when (tab) {
                        // Child flows keep their parent tab highlighted.
                        Tab.SPACES -> routes.any { it == Routes.SPACES || it == Routes.KITCHEN }
                        Tab.REPORT -> routes.any { it == Routes.REPORT || it.startsWith(Routes.FEEDBACK) }
                        else -> tab.route in routes
                    }
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(start) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GarminBlue,
                            selectedTextColor = GarminBlue,
                            indicatorColor = AccentBlue,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.PULSE) { DailyPulseScreen(container) }
            composable(Routes.SPACES) {
                SpacesScreen(
                    container,
                    onOpenKitchen = { navController.navigate(Routes.KITCHEN) },
                )
            }
            composable(Routes.KITCHEN) {
                KitchenDetailScreen(
                    container,
                    onReport = { category, note, community ->
                        navController.navigate(Routes.feedback(category, note, community))
                    },
                )
            }
            composable(Routes.REPORT) {
                ReportCategoryScreen(
                    onContinue = { category, note, community ->
                        navController.navigate(Routes.feedback(category, note, community))
                    },
                )
            }
            composable(
                Routes.FEEDBACK_WITH_ARG,
                arguments = listOf(
                    navArgument(Routes.FEEDBACK_ARG_CATEGORY) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(Routes.FEEDBACK_ARG_NOTE) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(Routes.FEEDBACK_ARG_COMMUNITY) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                FeedbackScreen(
                    container,
                    initialCategoryName = entry.arguments?.getString(Routes.FEEDBACK_ARG_CATEGORY),
                    initialNote = entry.arguments?.getString(Routes.FEEDBACK_ARG_NOTE),
                    initialCommunity = entry.arguments?.getString(Routes.FEEDBACK_ARG_COMMUNITY).toBoolean(),
                    onSubmitted = { navController.popBackStack() },
                )
            }
            composable(Routes.NEWSFEED) { NewsfeedScreen(container) }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    container,
                    onSignOut = onSignOut,
                    onOpenTickets = { navController.navigate(Routes.TICKETS) },
                    onOpenLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                )
            }
            composable(Routes.TICKETS) { TicketsScreen(container) }
            composable(Routes.LEADERBOARD) { LeaderboardScreen(container) }
        }
    }
}
