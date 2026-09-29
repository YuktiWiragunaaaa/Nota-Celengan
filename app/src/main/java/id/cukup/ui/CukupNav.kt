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
    const val ADD = "add?type={type}&pocket={pocket}"
    const val POCKET = "pocket/{id}"
    const val TX = "tx/{id}"
    const val INBOX = "inbox"
    const val SPLIT = "split"

    fun add(type: String = "EXPENSE", pocket: Long = 0) = "add?type=$type&pocket=$pocket"
}

private val tabs = listOf(Routes.HOME to "Beranda", Routes.HISTORY to "Riwayat", Routes.POCKETS to "Kantong", Routes.SETTINGS to "Setelan")

@Composable
fun CukupNav(onboarded: Boolean, openAdd: Boolean, onAddHandled: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in tabs.map { it.first }
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
    val tabPadding = PaddingValues(top = top, bottom = bottom + 84.dp)

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
                    onOpenPocket = { nav.navigate("pocket/$it") },
                    onEditSplit = { nav.navigate(Routes.SPLIT) },
                    onOpenInbox = { nav.navigate(Routes.INBOX) },
                    onOpenTx = { nav.navigate("tx/$it") },
                    onOpenHistory = { nav.switchTab(Routes.HISTORY) },
                )
            }
            composable(Routes.HISTORY) { HistoryScreen(tabPadding, onOpenTx = { nav.navigate("tx/$it") }) }
            composable(Routes.POCKETS) {
                PocketsScreen(
                    tabPadding,
                    onOpenPocket = { nav.navigate("pocket/$it") },
                    onEditSplit = { nav.navigate(Routes.SPLIT) },
                    onMove = { nav.navigate(Routes.add("MOVE")) },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen(tabPadding, onEditSplit = { nav.navigate(Routes.SPLIT) }) }
            composable(
                Routes.ADD,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; defaultValue = "EXPENSE" },
                    navArgument("pocket") { type = NavType.LongType; defaultValue = 0L },
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
    Column(modifier.fillMaxWidth().background(c.paper.copy(alpha = 0.96f)).windowInsetsPadding(WindowInsets.navigationBars)) {
        Hairline()
        Row(Modifier.fillMaxWidth().height(72.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.take(2).forEach { (r, label) -> Tab(label, r == current, { onTab(r) }, Modifier.weight(1f)) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(54.dp)
                        .shadow(6.dp, CircleShape, ambientColor = c.ink, spotColor = c.ink)
                        .clip(CircleShape)
                        .background(c.ink)
                        .clickable(role = Role.Button, onClick = onAdd)
                        .semantics { contentDescription = "Catat transaksi" },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", style = Type.display, color = c.paper)
                }
            }
            tabs.drop(2).forEach { (r, label) -> Tab(label, r == current, { onTab(r) }, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun Tab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val c = colors
    Column(
        modifier.clickable(role = Role.Tab, onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(Id), style = Type.label, color = if (selected) c.ink else c.faint)
        Box(
            Modifier.padding(top = 6.dp).size(width = 16.dp, height = 1.5.dp)
                .background(if (selected) c.ink else c.paper),
        )
    }
}
