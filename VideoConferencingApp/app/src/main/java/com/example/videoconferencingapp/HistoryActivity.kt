
package com.example.videoconferencingapp
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.BoxScopeInstance.align
import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.FlowRowScopeInstance.align
//import androidx.compose.foundation.layout.FlowRowScopeInstance.weight
//import androidx.compose.foundation.layout.RowScopeInstance.weight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.motionEventSpy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.videoconferencingapp.presentation.profile.ProfileScreen
import com.example.videoconferencingapp.presentation.sign_in.GoogleAuthUiClient
import com.example.videoconferencingapp.ui.theme.VideoConferencingAppTheme
import com.google.android.gms.auth.api.identity.Identity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class HistoryActivity: ComponentActivity() {
    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoConferencingAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val context = LocalContext.current
                    val db = Firebase.firestore
                    val dataList = mutableListOf<String>()
                    var userData = googleAuthUiClient.getSignedInUser()
                    val email= userData?.email

                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            // Fetch documents from the "users" collection
                            val result = db.collection("users").whereEqualTo("email", email).get().await()
                            for (document in result) {
                                // Add data of each document to dataList
                                dataList.add(document.data.toString())
                                println(document.data.toString())
                            }
                        } catch (e: Exception) {
                            // Handle exceptions
                            println("Error getting documents: $e")
                        }

                        // Display the UI using the fetched data
                        setContent {
                            VideoConferencingAppTheme {
                                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                                    HistoryScreen(dataList)
                                }

                            }
                        }
                    }
                }
            }
        }

    }
}
fun parseDataString(dataString: String): Map<String, String> {
    val keyValuePairs = dataString
        .substring(1, dataString.length - 1) // Remove curly braces at the start and end
        .split(", ") // Split the string into key-value pairs

    val dataMap = mutableMapOf<String, String>()
    for (pair in keyValuePairs) {
        val (key, value) = pair.split("=") // Split each pair into key and value
        dataMap[key.trim()] = value.trim() // Trim whitespace and add to the map
    }
    return dataMap
}

@Composable
fun HistoryScreen(dataList: List<String>) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        content = {
            Text(
                text = "History",
                fontSize = 36.sp,
                modifier = Modifier.padding(16.dp)
            )
            LazyColumn {
                items(dataList) { item ->
                    // Parse the item string into a map
                    val dataMap = parseDataString(item)

                    // Display parsed data within a Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Display each key-value pair in the parsed data
                            dataMap.forEach { (key, value) ->
                                Text(
                                    text = "$key: $value",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }



        }
    )

    Column (modifier = Modifier
        .fillMaxSize()
        .padding(top = 24.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally){
        BottomNavBar(context = context, _selectedItem = 1)

    }

}