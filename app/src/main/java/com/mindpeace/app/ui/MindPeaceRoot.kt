package com.mindpeace.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mindpeace.app.MindPeaceApp
import com.mindpeace.app.R
import com.mindpeace.app.ui.theme.PeaceBottomBar
import com.mindpeace.app.ui.theme.PeaceTab
import com.mindpeace.app.ui.theme.peaceContainerColor
import com.mindpeace.app.ui.home.AppDetailScreen
import com.mindpeace.app.ui.home.HomeScreen
import com.mindpeace.app.ui.home.HomeViewModel
import com.mindpeace.app.ui.onboarding.AccessibilityRequiredScreen
import com.mindpeace.app.ui.onboarding.OnboardingScreen
import com.mindpeace.app.ui.picker.AppPickerScreen
import com.mindpeace.app.ui.settings.AboutScreen
import com.mindpeace.app.ui.settings.NotificationLabScreen
import com.mindpeace.app.ui.settings.SettingsScreen
import com.mindpeace.app.ui.settings.ThemeSettingsScreen
import com.mindpeace.app.ui.stats.StatsScreen
import com.mindpeace.app.util.Permissions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAB_BUDGET = "budget"
private const val TAB_PICKER = "picker"
private const val TAB_STATS = "stats"
private const val TAB_SETTINGS = "settings"

private val TAB_KEYS = listOf(TAB_BUDGET, TAB_PICKER, TAB_STATS, TAB_SETTINGS)

@Composable
fun MindPeaceRoot(
    pendingDestination: String? = null,
    onPendingConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as MindPeaceApp
    val settings = app.container.settings
    val onboarded by settings.onboarded.collectAsStateWithLifecycle()
    var a11y by remember { mutableStateOf(Permissions.isAccessibilityEnabled(context)) }
    var showSetup by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                a11y = Permissions.isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            a11y = Permissions.isAccessibilityEnabled(context)
            delay(1000)
        }
    }

    val gate = when {
        !onboarded || showSetup -> "setup"
        !a11y -> "a11y"
        else -> "app"
    }

    Surface(modifier = Modifier.fillMaxSize(), color = peaceContainerColor()) {
        AnimatedContent(
            targetState = gate,
            transitionSpec = {
                fadeIn(tween(320)) togetherWith fadeOut(tween(240))
            },
            label = "root-gate",
        ) { current ->
            when (current) {
                "setup" -> OnboardingScreen(
                    onFinished = { showSetup = false },
                    fromSettings = onboarded,
                    onCancelSetup = { showSetup = false },
                )
                "a11y" -> AccessibilityRequiredScreen(
                    onOpenSettings = { Permissions.openAccessibilitySettings(context) },
                )
                else -> AppNav(
                    onReopenSetup = { showSetup = true },
                    pendingDestination = pendingDestination,
                    onPendingConsumed = onPendingConsumed,
                )
            }
        }
    }
}

@Composable
private fun AppNav(
    onReopenSetup: () -> Unit,
    pendingDestination: String?,
    onPendingConsumed: () -> Unit,
) {
    val nav = rememberNavController()
    val homeVm: HomeViewModel = viewModel()
    var tab by rememberSaveable { mutableStateOf(TAB_BUDGET) }
    val pagerState = rememberPagerState(
        initialPage = TAB_KEYS.indexOf(tab).coerceAtLeast(0),
        pageCount = { TAB_KEYS.size },
    )
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState.currentPage) {
        val key = TAB_KEYS.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (tab != key) tab = key
    }
    LaunchedEffect(pendingDestination) {
        if (pendingDestination == "stats") {
            tab = TAB_STATS
            pagerState.scrollToPage(TAB_KEYS.indexOf(TAB_STATS).coerceAtLeast(0))
            nav.popBackStack("main", inclusive = false)
            onPendingConsumed()
        }
    }
    val tabs = listOf(
        PeaceTab(TAB_BUDGET, stringResource(R.string.tab_budget), Icons.Outlined.HourglassEmpty),
        PeaceTab(TAB_PICKER, stringResource(R.string.tab_add), Icons.Outlined.Apps),
        PeaceTab(TAB_STATS, stringResource(R.string.tab_stats), Icons.Outlined.Insights),
        PeaceTab(TAB_SETTINGS, stringResource(R.string.tab_settings), Icons.Outlined.Settings),
    )
    NavHost(
        navController = nav,
        startDestination = "main",
        enterTransition = { Motion.navEnter() },
        exitTransition = { Motion.navExit() },
        popEnterTransition = { Motion.navPopEnter() },
        popExitTransition = { Motion.navPopExit() },
    ) {
        composable("main") {
            Scaffold(
                containerColor = peaceContainerColor(),
                bottomBar = {
                    PeaceBottomBar(
                        tabs = tabs,
                        selectedKey = TAB_KEYS.getOrElse(pagerState.currentPage) { tab },
                        onSelect = { key ->
                            val index = TAB_KEYS.indexOf(key)
                            if (index >= 0) {
                                tab = key
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        },
                    )
                },
            ) { padding ->
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    beyondViewportPageCount = TAB_KEYS.size - 1,
                ) { page ->
                    when (page) {
                        1 -> AppPickerScreen(onBack = null)
                        2 -> StatsScreen(onBack = null)
                        3 -> SettingsScreen(
                            onBack = null,
                            onReopenSetup = onReopenSetup,
                            onAbout = { nav.navigate("about") },
                            onTheme = { nav.navigate("theme") },
                        )
                        else -> HomeScreen(
                            viewModel = homeVm,
                            onAdd = {
                                tab = TAB_PICKER
                                scope.launch { pagerState.animateScrollToPage(TAB_KEYS.indexOf(TAB_PICKER)) }
                            },
                            onApp = { pkg -> nav.navigate("detail/$pkg") },
                        )
                    }
                }
            }
        }
        composable(
            route = "detail/{pkg}",
            arguments = listOf(navArgument("pkg") { type = NavType.StringType }),
        ) { entry ->
            val pkg = entry.arguments?.getString("pkg").orEmpty()
            AppDetailScreen(
                packageName = pkg,
                onBack = { nav.popBackStack() },
            )
        }
        composable("theme") {
            ThemeSettingsScreen(onBack = { nav.popBackStack() })
        }
        composable("about") {
            AboutScreen(
                onBack = { nav.popBackStack() },
                onOpenLab = { nav.navigate("lab") },
            )
        }
        composable("lab") {
            NotificationLabScreen(onBack = { nav.popBackStack() })
        }
    }
}
