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
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import id.cukup.domain.TxType
import id.cukup.ui.accounts.AccountScreen
import id.cukup.ui.help.HelpScreen
import id.cukup.ui.plan.PlanEditScreen
import id.cukup.ui.plan.PlanScreen
import id.cukup.ui.settings.CategoriesScreen
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
import id.cukup.ui.settings.ProfileScreen
import id.cukup.ui.review.InboxScreen
import id.cukup.ui.review.TxDetailScreen
import id.cukup.ui.plan.ReportScreen
import id.cukup.ui.settings.AutoScreen
import id.cukup.ui.settings.SettingsScreen
import id.cukup.ui.theme.Type
import id.cukup.ui.theme.colors

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val HISTORY = "history"
    const val PLAN = "plan"
    const val SETTINGS = "settings"
    const val ADD = "add?type={type}&account={account}&edit={edit}&category={category}"
    const val ACCOUNT = "account/{id}"
    const val TX = "tx/{id}"
    const val INBOX = "inbox"
    const val PLAN_EDIT = "plan/edit"
    const val PROFILE = "profile"
    const val CATEGORIES = "categories"
    const val HELP = "help"
    const val AUTO = "auto"
    const val REPORT = "report"

    fun add(type: TxType = TxType.EXPENSE, account: Long = 0, edit: Long = 0, category: Long = 0) =
        "add?type=${type.name}&account=$account&edit=$edit&category=$category"
    fun account(id: Long) = "account/$id"
    fun tx(id: Long) = "tx/$id"
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "Catatan", Icons.Rounded.Home),
    TabItem(Routes.HISTORY, "Riwayat", Icons.AutoMirrored.Rounded.ReceiptLong),
    TabItem(Routes.PLAN, "Rencana", Icons.Rounded.Flag),
    TabItem(Routes.SETTINGS, "Setelan", Icons.Rounded.Tune),
)

@Composable
fun CukupNav(
    onboarded: Boolean, openAdd: Boolean, onAddHandled: () -> Unit, addCategory: Long = 0,
    openReport: Boolean = false, onReportHandled: () -> Unit = {},
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in tabs.map { it.route }
    // Layar bergradien memakai ikon status bar terang.
    LightStatusBarIcons(light = route == Routes.HOME || route == Routes.PROFILE)
    val c = colors

    LaunchedEffect(openAdd, onboarded) {
        if (openAdd && onboarded) {
            nav.navigate(Routes.add(category = addCategory))
            onAddHandled()
        }
    }

    LaunchedEffect(openReport, onboarded) {
        if (openReport && onboarded) {
            nav.navigate(Routes.REPORT)
            onReportHandled()
        }
    }

    val density = LocalDensity.current
    val top = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
    val bottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    val tabPadding = PaddingValues(top = top, bottom = bottom + 104.dp)
    val openTx: (Long) -> Unit = { nav.navigate(Routes.tx(it)) }
    val openAccount: (Long) -> Unit = { nav.navigate(Routes.account(it)) }

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
                    onAdd = { nav.navigate(Routes.add(it)) },
                    onQuickAdd = { cat -> nav.navigate(Routes.add(category = cat)) },
                    onOpenAccount = openAccount,
                    onNewAccount = { openAccount(0) },
                    onOpenInbox = { nav.navigate(Routes.INBOX) },
                    onOpenTx = openTx,
                    onOpenHistory = { nav.switchTab(Routes.HISTORY) },
                    onOpenProfile = { nav.navigate(Routes.PROFILE) },
                    onOpenPlan = { nav.switchTab(Routes.PLAN) },
                    onOpenHelp = { nav.navigate(Routes.HELP) },
                    onOpenAuto = { nav.navigate(Routes.AUTO) },
                )
            }
            composable(Routes.HISTORY) { HistoryScreen(tabPadding, onOpenTx = openTx) }
            composable(Routes.PLAN) { PlanScreen(tabPadding, onEditPlan = { nav.navigate(Routes.PLAN_EDIT) }, onOpenReport = { nav.navigate(Routes.REPORT) }) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    tabPadding,
                    onOpenAccount = openAccount,
                    onOpenCategories = { nav.navigate(Routes.CATEGORIES) },
                    onOpenPlan = { nav.switchTab(Routes.PLAN) },
                    onOpenProfile = { nav.navigate(Routes.PROFILE) },
                    onOpenHelp = { nav.navigate(Routes.HELP) },
                    onOpenAuto = { nav.navigate(Routes.AUTO) },
                )
            }
            composable(
                Routes.ADD,
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType; defaultValue = TxType.EXPENSE.name },
                    navArgument("account") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("edit") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("category") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) { AddScreen(onClose = { nav.popBackStack() }, onNewAccount = { openAccount(0) }) }
            composable(Routes.ACCOUNT, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                AccountScreen(
                    id = e.arguments?.getLong("id") ?: 0,
                    onBack = { nav.popBackStack() },
                    onOpenTx = openTx,
                    onAdd = { type, account -> nav.navigate(Routes.add(type, account)) },
                )
            }
            composable(Routes.TX, arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                TxDetailScreen(
                    id = e.arguments?.getLong("id") ?: 0,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.add(edit = it)) { popUpTo(Routes.TX) { inclusive = true } } },
                )
            }
            composable(Routes.INBOX) { InboxScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.PLAN_EDIT) { PlanEditScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.CATEGORIES) { CategoriesScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.HELP) { HelpScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.AUTO) { AutoScreen(onBack = { nav.popBackStack() }, onOpenInbox = { nav.navigate(Routes.INBOX) }) }
            composable(Routes.REPORT) { ReportScreen(onBack = { nav.popBackStack() }, onOpenPlan = { nav.popBackStack(); nav.switchTab(Routes.PLAN) }) }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onBack = { nav.popBackStack() },
                    onOpenSettings = { nav.switchTab(Routes.SETTINGS) },
                    onOpenPlan = { nav.switchTab(Routes.PLAN) },
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
            .height(72.dp)
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
                .semantics { contentDescription = "Catat" },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, null, tint = Color.White) }
        tabs.drop(2).forEach { t -> Tab(t, t.route == current) { onTab(t.route) } }
    }
}

@Composable
private fun Tab(tab: TabItem, selected: Boolean, onClick: () -> Unit) {
    val c = colors
    Column(
        Modifier
            .clip(Pill)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .semantics { contentDescription = tab.label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.height(30.dp).width(46.dp).clip(Pill).background(if (selected) c.ink else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { Icon(tab.icon, null, tint = if (selected) c.card else c.mute, modifier = Modifier.size(20.dp)) }
        Text(tab.label, style = Type.label.copy(fontSize = 10.sp), color = if (selected) c.ink else c.mute)
    }
}
