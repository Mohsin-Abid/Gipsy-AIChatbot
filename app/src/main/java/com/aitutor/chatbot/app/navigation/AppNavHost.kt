package com.aitutor.chatbot.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.aitutor.chatbot.app.ui.chat.ChatDetailScreen
import com.aitutor.chatbot.app.ui.language.LanguageSelectRoute
import com.aitutor.chatbot.app.ui.main.MainScreen
import com.aitutor.chatbot.app.ui.onboarding.OnboardingRoute
import com.aitutor.chatbot.app.ui.premium.PremiumScreen
import com.aitutor.chatbot.app.ui.splash.SplashDestination
import com.aitutor.chatbot.app.ui.splash.SplashRoute

@Composable
fun AppNavHost(modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Route.Splash, modifier = modifier) {

        composable<Route.Splash> {
            SplashRoute(onNavigate = { destination ->
                val target = when (destination) {
                    SplashDestination.Onboarding -> Route.Onboarding
                    SplashDestination.LanguageSelect -> Route.LanguageSelect()
                    SplashDestination.Main -> Route.Main
                }
                navController.navigate(target) {
                    popUpTo<Route.Splash> { inclusive = true }
                }
            })
        }

        composable<Route.Onboarding> {
            OnboardingRoute(onDone = {
                navController.navigate(Route.LanguageSelect()) {
                    popUpTo<Route.Onboarding> { inclusive = true }
                }
            })
        }

        composable<Route.LanguageSelect> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.LanguageSelect>()
            LanguageSelectRoute(onDone = {
                if (args.fromSettings) {
                    navController.popBackStack()
                } else {
                    navController.navigate(Route.Main) {
                        popUpTo<Route.LanguageSelect> { inclusive = true }
                    }
                }
            })
        }

        composable<Route.Main> {
            MainScreen(
                onModeClick = { mode -> navController.navigate(Route.ChatDetail(modeId = mode.id.name)) },
                onChatClick = { chat ->
                    navController.navigate(Route.ChatDetail(modeId = chat.modeId.name, chatId = chat.id))
                },
                onLanguageClick = { navController.navigate(Route.LanguageSelect(fromSettings = true)) },
                onPremiumClick = { navController.navigate(Route.Premium) },
                onAccountCleared = {
                    navController.navigate(Route.Splash) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable<Route.ChatDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<Route.ChatDetail>()
            ChatDetailScreen(
                modeId = args.modeId,
                chatId = args.chatId,
                onBack = { navController.popBackStack() },
                onOpenPremium = { navController.navigate(Route.Premium) },
            )
        }

        composable<Route.Premium> {
            PremiumScreen(onBack = { navController.popBackStack() })
        }
    }
}
