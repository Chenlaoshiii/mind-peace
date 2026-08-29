package com.mindpeace.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    LaunchedEffect(pendingDestination) {
        if (pendingDestination == "stats") {
            nav.navigate("stats")
            onPendingConsumed()
        }
    }
    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = { Motion.navEnter() },
        exitTransition = { Motion.navExit() },
        popEnterTransition = { Motion.navPopEnter() },
        popExitTransition = { Motion.navPopExit() },
    ) {
        composable("home") {
            HomeScreen(
                viewModel = homeVm,
                onAdd = { nav.navigate("picker") },
                onSettings = { nav.navigate("settings") },
                onApp = { pkg -> nav.navigate("detail/$pkg") },
                onStats = { nav.navigate("stats") },
            )
        }
        composable("picker") {
            AppPickerScreen(onBack = { nav.popBackStack() })
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
        composable("settings") {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onReopenSetup = onReopenSetup,
                onAbout = { nav.navigate("about") },
                onTheme = { nav.navigate("theme") },
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
        composable("stats") {
            StatsScreen(onBack = { nav.popBackStack() })
        }
    }
}
