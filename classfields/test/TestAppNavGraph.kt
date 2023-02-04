package ru.auto.test

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import ru.auto.core_ui.compose.animation.animatedComposable
import ru.auto.test.experiments.router.ExperimentsCoordinator
import ru.auto.test.experiments.ui.ExperimentsScreen
import ru.auto.test.settings.ui.SettingsScreen
import ru.auto.test.testid.ui.AddTestIdScreen

object Routes {
    const val Settings = "settings"
    const val Experiments = "experiments"
    const val AddTestId = "add_test_id"
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TestAppNavGraph() {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(navController = navController, startDestination = Routes.Settings) {
        animatedComposable(Routes.Settings) {
            SettingsScreen(openExperiments = { navController.navigate(Routes.Experiments) })
        }
        animatedComposable(Routes.Experiments) {
            ExperimentsScreen(navController.experimentsCoordinator())
        }
        animatedComposable(Routes.AddTestId) {
            AddTestIdScreen(close = navController::popBackStack)
        }
    }
}

fun NavHostController.experimentsCoordinator() = object : ExperimentsCoordinator {

    override fun close() {
        popBackStack()
    }

    override fun openAddTestId() {
        navigate(Routes.AddTestId)
    }
}
