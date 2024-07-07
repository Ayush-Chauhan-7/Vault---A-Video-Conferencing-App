package com.example.videoconferencingapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.videoconferencingapp.presentation.sign_in.GoogleAuthUiClient
import com.example.videoconferencingapp.ui.theme.VideoConferencingAppTheme
import com.facebook.react.modules.core.PermissionListener
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import org.jitsi.meet.sdk.BroadcastEvent
import org.jitsi.meet.sdk.BroadcastIntentHelper
import org.jitsi.meet.sdk.JitsiMeet
import org.jitsi.meet.sdk.JitsiMeetActivity
import org.jitsi.meet.sdk.JitsiMeetActivityInterface
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URL

class MainActivity :ComponentActivity(), JitsiMeetActivityInterface {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude by mutableDoubleStateOf(-100.0)
    private var longitude by mutableDoubleStateOf(-100.0)
    val db = Firebase.firestore
    companion object {
        private const val REQUEST_CHECK_SETTINGS = 0x1
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onBroadcastReceived(intent)
        }
    }
    private var isModerator by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            getLocation()
        }
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            setContent {
                VideoConferencingAppTheme{
                    // A surface container using the 'background' color from the theme
                    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                        MainScreen(latitude, longitude)
                    }
                }
            }
            val serverURL: URL = try {
                // When using JaaS, replace "https://meet.jit.si" with the proper serverURL
                URL("https://meet.ffmuc.net/")
            } catch (e: MalformedURLException) {
                e.printStackTrace()
                throw RuntimeException("Invalid server URL!")
            }
            val defaultOptions = JitsiMeetConferenceOptions.Builder()
                .setServerURL(serverURL)
                .build()
            JitsiMeet.setDefaultConferenceOptions(defaultOptions)
            registerForBroadcastMessages()
            Toast.makeText(this, "Location permissions are not granted", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.requestLocationUpdates(locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)
                    val location = p0.lastLocation
                    if (location != null) {
                        latitude = location.latitude
                    }
                    if (location != null) {
                        longitude = location.longitude
                    }
                    setContent {
                        VideoConferencingAppTheme{
                            // A surface container using the 'background' color from the theme
                            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                                MainScreen(latitude, longitude)
                            }
                        }
                    }
                    val serverURL: URL = try {
                        // When using JaaS, replace "https://meet.jit.si" with the proper serverURL
                        URL("https://meet.ffmuc.net/")
                    } catch (e: MalformedURLException) {
                        e.printStackTrace()
                        throw RuntimeException("Invalid server URL!")
                    }
                    val defaultOptions = JitsiMeetConferenceOptions.Builder()
                        .setServerURL(serverURL)
                        .build()
                    JitsiMeet.setDefaultConferenceOptions(defaultOptions)
                    registerForBroadcastMessages()
                }
            },
            Looper.getMainLooper())
    }
    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    private fun generateRandomString(): String {
        val characters = ('a'..'z')
        return (1..9)
            .map { characters.random() }
            .joinToString("") { it.toString() }
    }

    @SuppressLint("LogNotTimber")
    fun onButtonClick(v: View?, meetingName: String?, meetingSubject: String?, user: MutableMap<String, String>) {
        db.collection("users")
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
        val options = JitsiMeetConferenceOptions.Builder()
            .setRoom(if (meetingName == "") generateRandomString() else meetingName)
            .setSubject(if (meetingSubject == "") "Meeting" else meetingSubject)
            .build()
        JitsiMeetActivity.launch(this, options)
    }


    private fun registerForBroadcastMessages() {
        val intentFilter = IntentFilter()
        for (type in BroadcastEvent.Type.values()) {
            intentFilter.addAction(type.action)
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter)
    }

    // Example for handling different JitsiMeetSDK events
    private fun onBroadcastReceived(intent: Intent?) {
        if (intent != null) {
            val event = BroadcastEvent(intent)
            when (event.type) {
                BroadcastEvent.Type.CONFERENCE_JOINED -> {
                    Timber.i("Conference Joined with url%s", event.data["url"])
                    // Check if the current user is the first participant
                    if (!isModerator) {
                        val participantCount = event.data["participantCount"] as? Int
                        if (participantCount == 1) {
                            isModerator = true
                        }
                    }
                }
                BroadcastEvent.Type.PARTICIPANT_JOINED -> {
                    Timber.i("Participant joined%s", event.data["name"])
                    // Update moderator status if the current user is the first participant
                    if (!isModerator) {
                        val participantCount = event.data["participantCount"] as? Int
                        if (participantCount == 1) {
                            isModerator = true
                        }
                    }
                }
                else -> Timber.i("Received event: %s", event.type)
            }
        }
    }

    // Example for sending actions to JitsiMeetSDK
    private fun hangUp() {
        val hangupBroadcastIntent: Intent = BroadcastIntentHelper.buildHangUpIntent()
        LocalBroadcastManager.getInstance(this.applicationContext).sendBroadcast(hangupBroadcastIntent)
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return ;
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    latitude = location.latitude
                    longitude = location.longitude
                    setContent {
                        VideoConferencingAppTheme{
                            // A surface container using the 'background' color from the theme
                            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                                MainScreen(latitude, longitude)
                            }
                        }
                    }
                    val serverURL: URL = try {
                        // When using JaaS, replace "https://meet.jit.si" with the proper serverURL
                        URL("https://meet.ffmuc.net/")
                    } catch (e: MalformedURLException) {
                        e.printStackTrace()
                        throw RuntimeException("Invalid server URL!")
                    }
                    val defaultOptions = JitsiMeetConferenceOptions.Builder()
                        .setServerURL(serverURL)
                        .build()
                    JitsiMeet.setDefaultConferenceOptions(defaultOptions)
                    registerForBroadcastMessages()
                    // Do something with latitude and longitude
                } ?: run {
                    setContent {
                        VideoConferencingAppTheme{
                            // A surface container using the 'background' color from the theme
                            Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                                MainScreen(latitude, longitude)
                            }
                        }
                    }
                    val serverURL: URL = try {
                        // When using JaaS, replace "https://meet.jit.si" with the proper serverURL
                        URL("https://meet.ffmuc.net/")
                    } catch (e: MalformedURLException) {
                        e.printStackTrace()
                        throw RuntimeException("Invalid server URL!")
                    }
                    val defaultOptions = JitsiMeetConferenceOptions.Builder()
                        .setServerURL(serverURL)
                        .build()
                    JitsiMeet.setDefaultConferenceOptions(defaultOptions)
                    registerForBroadcastMessages()
                    Toast.makeText(this, "Unable to retrieve location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                setContent {
                    VideoConferencingAppTheme{
                        // A surface container using the 'background' color from the theme
                        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                            MainScreen(latitude, longitude)
                        }
                    }
                }
                val serverURL: URL = try {
                    // When using JaaS, replace "https://meet.jit.si" with the proper serverURL
                    URL("https://meet.ffmuc.net/")
                } catch (e: MalformedURLException) {
                    e.printStackTrace()
                    throw RuntimeException("Invalid server URL!")
                }
                val defaultOptions = JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverURL)
                    .build()
                JitsiMeet.setDefaultConferenceOptions(defaultOptions)
                registerForBroadcastMessages()
                Toast.makeText(this, "Unable to retrieve location", Toast.LENGTH_SHORT).show()
            }
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(this)
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // All location settings are satisfied. The client can initialize
            // location requests here.

        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@MainActivity, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            } else {
                // If the exception is not ResolvableApiException, handle it as needed.
                // For example, you can show a message to the user.
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    // The user changed the location settings to satisfy your app's needs
                    // You can reinitialize location requests or perform actions that require the location to be enabled here.
                    // For example, you can call the getLocation() function again.
//                    getLocation()
                    requestLocationUpdates()
                } else {
                    // The user didn't change the location settings. Handle this scenario as needed.
                    // For example, you can show a message to the user.
                    Toast.makeText(this, "Location settings are not enabled", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    fun ChangingTextScreen() {
        var currentIndex by remember { mutableStateOf(0) }
        val texts = listOf("Business Meetings", "Client Review", "Project Showcase", "Family Time", "Study Group", "Interviews", "Webinars", "Online Classes", "Team Standup", "Brainstorming", "Product Demos", "Virtual Events", "Remote Work", "Social Gatherings", "Conferences", "Workshops", "Training Sessions", "Happy Hours", "Coffee Breaks", "Networking", "Hackathons", "Game Nights", "Birthday Parties", "Weddings", "Baby Showers", "Celebrations", "Reunions", "Hangouts", "Meetups", "Concerts", "Live Shows", "Fitness Classes", "Yoga Sessions", "Meditation", "Cooking Classes", "Dance Workshops", "Music Lessons", "Art Classes", "Crafting Sessions", "Book Clubs", "Movie Nights", "Trivia Nights", "Karaoke", "Open Mic", "Comedy Shows", "Talent Shows", "Debates", "Discussions", "Q&A Sessions")

        LaunchedEffect(Unit) {
            while (true) {
                delay(2000) // Update text every 3 seconds
                currentIndex = (currentIndex + 1) % texts.size
            }
        }
        ChangingText(text = texts[currentIndex])

    }

    @Composable
    fun ChangingText(text: String) {
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
            modifier= Modifier
                .wrapContentSize()
                .padding(bottom = 50.dp)
        ) {
            Text(
                text = text,
                fontSize = 36.sp,
                textAlign = TextAlign.Start,
                fontFamily = MaterialTheme.typography.headlineLarge.fontFamily,
                fontStyle = MaterialTheme.typography.headlineLarge.fontStyle,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
    @Composable
    fun MainScreen(latitude: Double, longitude: Double) {
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        var meetingLink by remember { mutableStateOf("") }
        var meetingName by remember { mutableStateOf("") }
        var meetingSubject by remember { mutableStateOf("") }


        val context = LocalContext.current

//        println("Latitude: $latitude, Longitude: $longitude")
        val googleAuthUiClient by lazy {
            GoogleAuthUiClient(
                context = applicationContext,
                oneTapClient = Identity.getSignInClient(applicationContext)
            )
        }
        var userData = googleAuthUiClient.getSignedInUser()

        val user = mutableMapOf(
            "name" to userData?.username.toString(),
            "email" to userData?.email.toString(),
            "latitude" to latitude.toString(),
            "longitude" to longitude.toString(),
            "timestamp" to System.currentTimeMillis().toString(),
            "meetingSubject" to meetingSubject,
            "meetingLinkJoin" to meetingLink,
            "meetingNameCreate" to meetingName
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            }


            ChangingTextScreen()

            Text(
                text = "SteamScape",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 20.dp)

            )
            TextField(value = meetingName, onValueChange ={meetingName=it},label = { Text("Enter Room Name") },modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp) )

            TextField(value = meetingSubject, onValueChange ={meetingSubject=it},label = { Text("Enter Meeting Subject") },modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp) )
            ElevatedButton(
                onClick = {
                    isLoading = true
                    onButtonClick(null, meetingName, meetingSubject,user)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text("Create Room")
            }

            Spacer(modifier = Modifier.padding(25.dp))

            TextField(value = meetingLink, onValueChange ={meetingLink=it},label = { Text("Enter Meeting Link") },modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp) )
            ElevatedButton(
                onClick = {
                    isLoading = true
                    try {
//                        val options = JitsiMeetConferenceOptions.Builder()
//                            .setRoom(meetingLink)
//                            .setSubject(meetingSubject)
//                            .build()
//                        JitsiMeetActivity.launch(this@MainActivity, options)

                        onButtonClick(null, meetingLink, meetingSubject,user)
                        isLoading = false
                    } catch (e: Exception) {
                        isLoading = false
                        errorMessage = "Failed to start the meeting: ${e.localizedMessage}"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text("Join Room")
            }


            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 20.dp)
                )
            }
        }
        BottomNavBar(context = context, _selectedItem = 0)
    }

    override fun requestPermissions(p0: Array<out String>?, p1: Int, p2: PermissionListener?) {
    }
}




