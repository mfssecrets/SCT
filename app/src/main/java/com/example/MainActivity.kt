package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.AuthRepository
import com.example.data.supabase.SupabaseClient
import com.example.ui.components.CleanShieldTab
import com.example.ui.screens.BlockedUsersScreen
import com.example.ui.screens.CallScreen
import com.example.ui.screens.CleanShieldSplashScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.CreateAccessPinScreen
import com.example.ui.screens.EmailVerificationScreen
import com.example.ui.screens.EnterAccessPinScreen
import com.example.ui.screens.FriendsScreen
import com.example.ui.screens.InboxScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OptimizationCategory
import com.example.ui.screens.OptimiseCompleteScreen
import com.example.ui.screens.OptimiseScanningScreen
import com.example.ui.screens.PrivateVaultScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SecurityDashboardScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SignUpScreen
import com.example.ui.theme.MyApplicationTheme

sealed interface AppScreen {
    data object Splash : AppScreen
    data object Scanning : AppScreen
    data object Complete : AppScreen
    data class CreatePin(val category: OptimizationCategory?) : AppScreen
    data class EnterPin(val category: OptimizationCategory?) : AppScreen
    data class SignUp(val category: OptimizationCategory?) : AppScreen
    data class SignIn(val category: OptimizationCategory?) : AppScreen
    data class EmailVerification(
        val email: String,
        val username: String,
        val category: OptimizationCategory?
    ) : AppScreen
    data class Dashboard(val category: OptimizationCategory?) : AppScreen
    data object Profile : AppScreen
    data object Friends : AppScreen
    data object BlockedUsers : AppScreen
    data object PrivateVault : AppScreen
    data object Search : AppScreen
    data object Notifications : AppScreen
    data object Inbox : AppScreen
    data class Chat(val partnerUsername: String) : AppScreen
    data class AudioCall(val partnerUsername: String) : AppScreen
    data class VideoCall(val partnerUsername: String) : AppScreen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Initialize Supabase client before any UI
        SupabaseClient.initialize(applicationContext)
        setContent {
            MyApplicationTheme {
                CleanShieldApp(
                    onCloseApp = { finishAffinity() }
                )
            }
        }
    }
}

@Composable
fun CleanShieldApp(
    onCloseApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository.getInstance(context) }

    // Every app launch starts with Splash -> Scanning -> Optimise Complete
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }

    fun navigateToTab(tab: CleanShieldTab) {
        currentScreen = when (tab) {
            CleanShieldTab.PROFILE -> AppScreen.Profile
            CleanShieldTab.FRIENDS -> AppScreen.Friends
            CleanShieldTab.PRIVATE_VAULT -> AppScreen.PrivateVault
            CleanShieldTab.SEARCH -> AppScreen.Search
        }
    }

    // Handle Android system back button
    BackHandler {
        when (currentScreen) {
            is AppScreen.Splash -> { /* no back during splash */ }
            is AppScreen.Scanning -> onCloseApp()
            is AppScreen.Complete -> onCloseApp()
            is AppScreen.CreatePin -> currentScreen = AppScreen.Complete
            is AppScreen.EnterPin -> currentScreen = AppScreen.Complete
            is AppScreen.SignUp -> currentScreen = AppScreen.Complete
            is AppScreen.SignIn -> currentScreen = AppScreen.Complete
            is AppScreen.EmailVerification -> {
                val cat = (currentScreen as AppScreen.EmailVerification).category
                currentScreen = AppScreen.SignUp(cat)
            }
            is AppScreen.Dashboard -> currentScreen = AppScreen.Complete
            is AppScreen.Profile -> currentScreen = AppScreen.Dashboard(null)
            is AppScreen.Friends -> currentScreen = AppScreen.Profile
            is AppScreen.BlockedUsers -> currentScreen = AppScreen.Friends
            is AppScreen.PrivateVault -> currentScreen = AppScreen.Profile
            is AppScreen.Search -> currentScreen = AppScreen.Profile
            is AppScreen.Notifications -> currentScreen = AppScreen.Profile
            is AppScreen.Inbox -> currentScreen = AppScreen.Profile
            is AppScreen.Chat -> currentScreen = AppScreen.Inbox
            is AppScreen.AudioCall -> {
                val partner = (currentScreen as AppScreen.AudioCall).partnerUsername
                currentScreen = AppScreen.Chat(partner)
            }
            is AppScreen.VideoCall -> {
                val partner = (currentScreen as AppScreen.VideoCall).partnerUsername
                currentScreen = AppScreen.Chat(partner)
            }
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val direction = if (initialState.ordinal < targetState.ordinal) 1 else -1
            slideInHorizontally(
                animationSpec = tween(350)
            ) { (it * direction * 0.15f).toInt() } togetherWith
            slideOutHorizontally(
                animationSpec = tween(350)
            ) { (-it * direction * 0.15f).toInt() }
        },
        label = "clean_shield_flow",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            is AppScreen.Splash -> {
                CleanShieldSplashScreen(
                    onComplete = { currentScreen = AppScreen.Scanning }
                )
            }

            is AppScreen.Scanning -> {
                OptimiseScanningScreen(
                    onScanComplete = {
                        currentScreen = AppScreen.Complete
                    }
                )
            }

            is AppScreen.Complete -> {
                OptimiseCompleteScreen(
                    onNavigateBack = {
                        // Re-run system scan
                        currentScreen = AppScreen.Scanning
                    },
                    onOpenSecurityAccess = { category ->
                        // Step 1: Go button -> Access Check
                        if (!authRepository.hasAccessPin()) {
                            // First time user: Create PIN
                            currentScreen = AppScreen.CreatePin(category)
                        } else {
                            // Returning user: Enter PIN
                            currentScreen = AppScreen.EnterPin(category)
                        }
                    }
                )
            }

            is AppScreen.CreatePin -> {
                CreateAccessPinScreen(
                    onPinCreated = {
                        // Step 3: After PIN creation -> Create Account
                        currentScreen = AppScreen.SignUp(screen.category)
                    },
                    onNavigateBack = {
                        currentScreen = AppScreen.Complete
                    }
                )
            }

            is AppScreen.EnterPin -> {
                EnterAccessPinScreen(
                    onPinSuccess = {
                        // Step 6: Correct PIN -> session check
                        val session = authRepository.currentSession.value
                        if (session != null && session.isVerified) {
                            currentScreen = AppScreen.Dashboard(screen.category)
                        } else {
                            currentScreen = AppScreen.SignIn(screen.category)
                        }
                    },
                    onNavigateBack = {
                        currentScreen = AppScreen.Complete
                    }
                )
            }

            is AppScreen.SignUp -> {
                SignUpScreen(
                    onSignUpSuccess = { email, username ->
                        // Step 4: Account creation -> Email OTP
                        currentScreen = AppScreen.EmailVerification(
                            email = email,
                            username = username,
                            category = screen.category
                        )
                    },
                    onNavigateToSignIn = {
                        currentScreen = AppScreen.SignIn(screen.category)
                    },
                    onNavigateBack = {
                        currentScreen = AppScreen.Complete
                    }
                )
            }

            is AppScreen.SignIn -> {
                SignInScreen(
                    onSignInSuccess = {
                        currentScreen = AppScreen.Dashboard(screen.category)
                    },
                    onRequiresVerification = { email, username ->
                        currentScreen = AppScreen.EmailVerification(
                            email = email,
                            username = username,
                            category = screen.category
                        )
                    },
                    onNavigateToSignUp = {
                        currentScreen = AppScreen.SignUp(screen.category)
                    },
                    onNavigateBack = {
                        currentScreen = AppScreen.Complete
                    }
                )
            }

            is AppScreen.EmailVerification -> {
                EmailVerificationScreen(
                    email = screen.email,
                    username = screen.username,
                    onVerificationSuccess = {
                        // Step 5: Successful OTP -> Clean Shield Dashboard
                        currentScreen = AppScreen.Dashboard(screen.category)
                    },
                    onNavigateBack = {
                        currentScreen = AppScreen.SignUp(screen.category)
                    }
                )
            }

            is AppScreen.Dashboard -> {
                SecurityDashboardScreen(
                    initialCategory = screen.category,
                    onLogout = {
                        authRepository.signOut()
                        currentScreen = AppScreen.SignIn(null)
                    },
                    onNavigateToInbox = {
                        currentScreen = AppScreen.Inbox
                    },
                    onNavigateToNotifications = {
                        currentScreen = AppScreen.Notifications
                    },
                    onNavigateToProfile = {
                        currentScreen = AppScreen.Profile
                    },
                    onNavigateToFriends = {
                        currentScreen = AppScreen.Friends
                    },
                    onNavigateToVault = {
                        currentScreen = AppScreen.PrivateVault
                    },
                    onNavigateToSearch = {
                        currentScreen = AppScreen.Search
                    },
                    onRescanRequested = {
                        currentScreen = AppScreen.Scanning
                    }
                )
            }

            is AppScreen.Profile -> {
                ProfileScreen(
                    onLogoutClicked = {
                        authRepository.signOut()
                        currentScreen = AppScreen.SignIn(null)
                    },
                    onMessengerClicked = {
                        currentScreen = AppScreen.Inbox
                    },
                    onNotificationClicked = {
                        currentScreen = AppScreen.Notifications
                    },
                    onNavigateToFriends = {
                        currentScreen = AppScreen.Friends
                    },
                    onNavigateToVault = {
                        currentScreen = AppScreen.PrivateVault
                    },
                    onNavigateToSearch = {
                        currentScreen = AppScreen.Search
                    }
                )
            }

            is AppScreen.Friends -> {
                FriendsScreen(
                    onNavigateToTab = ::navigateToTab,
                    onLogoutClicked = {
                        authRepository.signOut()
                        currentScreen = AppScreen.SignIn(null)
                    },
                    onMessengerClicked = {
                        currentScreen = AppScreen.Inbox
                    },
                    onNotificationClicked = {
                        currentScreen = AppScreen.Notifications
                    },
                    onNavigateToChat = { friendUsername ->
                        currentScreen = AppScreen.Chat(friendUsername)
                    },
                    onNavigateToBlockedUsers = {
                        currentScreen = AppScreen.BlockedUsers
                    }
                )
            }

            is AppScreen.BlockedUsers -> {
                BlockedUsersScreen(
                    onNavigateBack = {
                        currentScreen = AppScreen.Friends
                    }
                )
            }

            is AppScreen.PrivateVault -> {
                PrivateVaultScreen(
                    onNavigateToTab = ::navigateToTab,
                    onLogoutClicked = {
                        authRepository.signOut()
                        currentScreen = AppScreen.SignIn(null)
                    },
                    onMessengerClicked = {
                        currentScreen = AppScreen.Inbox
                    },
                    onNotificationClicked = {
                        currentScreen = AppScreen.Notifications
                    }
                )
            }

            is AppScreen.Search -> {
                SearchScreen(
                    onNavigateToTab = ::navigateToTab,
                    onLogoutClicked = {
                        authRepository.signOut()
                        currentScreen = AppScreen.SignIn(null)
                    },
                    onMessengerClicked = {
                        currentScreen = AppScreen.Inbox
                    },
                    onNotificationClicked = {
                        currentScreen = AppScreen.Notifications
                    }
                )
            }

            is AppScreen.Notifications -> {
                NotificationsScreen(
                    onNavigateBack = {
                        currentScreen = AppScreen.Profile
                    },
                    onNavigateToChat = { senderUsername ->
                        currentScreen = AppScreen.Chat(senderUsername)
                    }
                )
            }

            is AppScreen.Inbox -> {
                InboxScreen(
                    onNavigateBack = {
                        currentScreen = AppScreen.Profile
                    },
                    onNavigateToChat = { partner ->
                        currentScreen = AppScreen.Chat(partner)
                    }
                )
            }

            is AppScreen.Chat -> {
                ChatScreen(
                    partnerUsername = screen.partnerUsername,
                    onNavigateBack = {
                        currentScreen = AppScreen.Inbox
                    },
                    onStartAudioCall = { partner ->
                        currentScreen = AppScreen.AudioCall(partner)
                    },
                    onStartVideoCall = { partner ->
                        currentScreen = AppScreen.VideoCall(partner)
                    }
                )
            }

            is AppScreen.AudioCall -> {
                CallScreen(
                    partnerUsername = screen.partnerUsername,
                    isVideoCall = false,
                    onEndCall = {
                        currentScreen = AppScreen.Chat(screen.partnerUsername)
                    }
                )
            }

            is AppScreen.VideoCall -> {
                CallScreen(
                    partnerUsername = screen.partnerUsername,
                    isVideoCall = true,
                    onEndCall = {
                        currentScreen = AppScreen.Chat(screen.partnerUsername)
                    }
                )
            }
        }
    }
}
