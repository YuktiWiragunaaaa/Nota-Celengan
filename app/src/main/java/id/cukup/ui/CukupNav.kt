package id.cukup.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Arrangement
import id.cukup.ui.components.Pill
import id.cukup.ui.components.LightStatusBarIcons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.cukup.ui.add.AddScreen
import id.cukup.ui.components.Hairline
import id.cukup.ui.components.Id
import id.cukup.ui.history.HistoryScreen
import id.cukup.ui.home.HomeScreen
import id.cukup.ui.onboarding.OnboardingScreen
import id.cukup.ui.pockets.PocketDetailScreen
import id.cukup.ui.pockets.PocketsScreen
import id.cukup.ui.pockets.SplitScreen
import id.cukup.ui.settings.ProfileScreen
import id.cukup.ui.goals.GoalsScreen
import id.cukup.ui.review.InboxScreen
import id.cukup.ui.review.TxDetailScreen
import id.cukup.ui.settings.SettingsScreen
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val HISTORY = "history"
    const val POCKETS = "pockets"
    const val SETTINGS = "settings"
    const val ADD = "add?type={type}&pocket={pocket}&to={to}"
    const val PROFILE = "profile"
    const val GOALS = "goals"
    const val POCKET = "pocket/{id}"
    const val TX = "tx/{id}"
    const val INBOX = "inbox"
    const val SPLIT = "split"

    fun add(type: String = "EXPENSE", pocket: Long = 0, to: Long = 0) = "add?type=$type&pocket=$pocket&to=$to"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "Beranda", Icons.Rounded.Home),
    TabItem(Routes.HISTORY, "Riwayat", Icons.AutoMirrored.Rounded.ReceiptLong),
    TabItem(Routes.POCKETS, "Kantong", Icons.Rounded.DonutLarge),
    TabItem(Routes.SETTINGS, "Setelan", Icons.Rounded.Tune),
)

@Composable
fun CukupNav(onboarded: Boolean, openAdd: Boolean, onAddHandled: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in tabs.map { it.route }
    // Layar bergradien memakai ikon status bar terang.
    LightStatusBarIcons(light = route in setOf(Routes.HOME, Routes.POCKETS, Routes.POCKET, Routes.PROFILE))
    val c = colors

    LaunchedEffect(openAdd, onboarded) {
        if (openAdd && onboarded) {
            nav.navigate(Routes.add())
            onAddHandled()
        }
    }

    val density = LocalDensity.current
    val top = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val bottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val tabPadding = PaddingValues(top = top, bottom = bottom + 96.dp)

    Box(Modifier.fillMaxSize().background(c.paper)) {
        NavHost(
            navController = nav,
            startDestination = if (onboarded) Routes.HOME else Routes.ONBOARDING,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    contentPadding = tabPadding,
                    onIncome = { nav.navigate(Routes.add("INCOME")) },
                    onExpense = { nav.navigate(Routes.add()) },
                    onMove = { nav.navigate(Routes.add("MOVE")) },
                    onOpenPocket = { nav.navigate("pocket/$it") },
                    onEditSplit = { nav.navigate(Routes.SPLIT) },
                    onOpenInbox = { nav.navigate(Routes.INBOX) },
                    onOpenTx = { nav.navigate("tx/$it") },
                    onOpenHistory = { nav.switchTab(Routes.HISTORY) },
                    onOpenProfile = { nav.navigate(Routes.PROFILE) },
                )
            }
            composable(Routes.HISTORY) { HistoryScreen(tabPadding, onOpenTx = { nav.navigate("tx/$it") }) }
            composable(Routes.POCKETS) {
                PocketsScreen(
                    tabPadding,
                    onOpenPocket = { nav.navigate("pocket/$it") },
                    onEditSplit = { nav.navigate(Routes.SPLIT) },
                    onMove = { nav.navigate(Routes.add("MOVE")) },
                    onOpenGoals = { nav.navigate(Routes.GOALS) },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen(tabPadding, onEditSplit = { nav.navigate(Routes.SPLIT) }) }
            composable(
                Routes.ADD,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; defaultValue = "EXPENSE" },
                    navArgument("pocket") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("to") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) { AddScreen(onClose = { nav.popBackStack() }) }
            composable(Routes.POCKET, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                PocketDetailScreen(
                    onBack = { nav.popBackStack() },
                    onOpenTx = { nav.navigate("tx/$it") },
                    onAdd = { nav.navigate(Routes.add(pocket = it)) },
                )
            }
            composable(Routes.TX, arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                TxDetailScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.INBOX) { InboxScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SPLIT) { SplitScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSettings = { nav.switchTab(Routes.SETTINGS) },
                    onOpenGoals = { nav.navigate(Routes.GOALS) },
                )
            }
            composable(Routes.GOALS) {
                GoalsScreen(
                    onBack = { nav.popBackStack() },
                    onFill = { nav.navigate(Routes.add("MOVE", to = it)) },
                    onEditSplit = { nav.navigate(Routes.SPLIT) },
                )
            }
        }

        if (showBar) {
            BottomBar(
                current = route,
                onTab = { nav.switchTab(it) },
                onAdd = { nav.navigate(Routes.add()) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(current: String?, onTab: (String) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val c = colors
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(64.dp)
            .shadow(16.dp, Pill, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(Pill)
            .background(c.card)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        tabs.take(2).forEach { t -> Tab(t, t.route == current) { onTab(t.route) } }
        Box(
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(c.brandBrush)
                .clickable(role = Role.Button, onClick = onAdd)
                .semantics { contentDescription = "Catat uang keluar" },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, null, tint = Color.White) }
        tabs.drop(2).forEach { t -> Tab(t, t.route == current) { onTab(t.route) } }
    }
}

@Composable
private fun Tab(tab: TabItem, selected: Boolean, onClick: () -> Unit) {
    val c = colors
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) c.ink else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = tab.label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(tab.icon, null, tint = if (selected) c.card else c.mute, modifier = Modifier.size(22.dp))
    }
}
