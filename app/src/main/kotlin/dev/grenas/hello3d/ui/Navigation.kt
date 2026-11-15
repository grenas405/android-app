package dev.grenas.hello3d.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.grenas.hello3d.data.ProfileViewModel
import dev.grenas.hello3d.ui.theme.Hello3DTheme
import dev.grenas.hello3d.ui.theme.ThemePacks
import io.github.sceneview.rememberEngine

private const val ROUTE_PROFILE = "profile"
private const val ROUTE_SETTINGS = "settings"

/** Root of the app: one Filament engine, one view model, two destinations. */
@Composable
fun AppNavigation() {
    val viewModel: ProfileViewModel = viewModel()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val pack = ThemePacks.byId(profile?.themeId)
    // Created once here so navigating to Settings and back does not re-initialise Filament.
    val engine = rememberEngine()

    Hello3DTheme(pack = pack) {
        val current = profile
        if (current == null) {
            // DataStore has not produced the first value yet; the veil hides the empty frame.
            Box(Modifier.fillMaxSize().background(pack.backdrop.last())) {
                WarmUpVeil(visible = true, pack = pack)
            }
            return@Hello3DTheme
        }

        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = ROUTE_PROFILE,
            enterTransition = { slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(320)) },
            exitTransition = { fadeOut(tween(220)) },
            popEnterTransition = { fadeIn(tween(320)) },
            popExitTransition = { slideOutHorizontally(tween(260)) { it / 4 } + fadeOut(tween(260)) },
        ) {
            composable(ROUTE_PROFILE) {
                ProfileScreen(
                    engine = engine,
                    profile = current,
                    pack = pack,
                    onThemeSelected = viewModel::setTheme,
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) { launchSingleTop = true } },
                )
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(
                    profile = current,
                    pack = pack,
                    onBack = { navController.popBackStack() },
                    onNameChange = viewModel::setName,
                    onLocationChange = viewModel::setLocation,
                    onToggleLanguage = viewModel::toggleLanguage,
                    onLanguagesChange = viewModel::setLanguages,
                    onThemeSelected = viewModel::setTheme,
                    onQualitySelected = viewModel::setQuality,
                    onRainSelected = viewModel::setRainIntensity,
                )
            }
        }
    }
}
