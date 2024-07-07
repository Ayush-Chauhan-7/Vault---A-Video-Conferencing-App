package com.example.videoconferencingapp
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.videoconferencingapp.presentation.profile.ProfileScreen
import com.example.videoconferencingapp.presentation.sign_in.GoogleAuthUiClient
import com.example.videoconferencingapp.ui.theme.VideoConferencingAppTheme
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

class SettingsActivity: ComponentActivity() {
    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
        setContent {
            VideoConferencingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val accountManager = AccountManager.get(applicationContext)
                    val accounts = accountManager.accounts
                    val context = LocalContext.current
                    print(accounts)
                    NavHost(navController = navController, startDestination = "profile") {
                        composable("profile") {
                            ProfileScreen(
                                userData = googleAuthUiClient.getSignedInUser(),
                                onSignOut = {
                                    lifecycleScope.launch {
                                        googleAuthUiClient.signOut()
                                        Toast.makeText(
                                            applicationContext,
                                            "Signed out",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        context.startActivity(
                                            Intent(
                                                context,
                                                SignInActivity::class.java
                                            ).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                            }
                                        )
                                    }
                                },
                                accounts = accounts,
                                context = applicationContext
                            )
                        }
                    }
                    BottomNavBar(context = context, _selectedItem = 2)
                }
            }
        }

    }
}