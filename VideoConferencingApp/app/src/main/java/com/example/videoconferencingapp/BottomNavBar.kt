package com.example.videoconferencingapp

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun BottomNavBar(context: Context, _selectedItem: Int)
{
    var selectedItem by remember { mutableIntStateOf(_selectedItem) }
    val items = listOf("home", "history", "profile")

    NavigationBar(modifier = Modifier
        .wrapContentSize(
            align = androidx.compose.ui.Alignment.BottomCenter
        )
        .height(50.dp)) {
        items.forEachIndexed { index, item ->
            val id = when(index) {
                0 -> R.drawable.home_icon
                1 -> R.drawable.history_icon
                2 -> R.drawable.profile_icon
                else -> R.drawable.home_icon
            }
            NavigationBarItem(
                icon = { Icon(painterResource(id = id), contentDescription = item, modifier = Modifier.size(24.dp)) },
                selected = selectedItem == index,
                onClick = {
                    selectedItem = index
                    when(index) {
                        0 -> context.startActivity(Intent(context, MainActivity::class.java))
                        1 -> context.startActivity(Intent(context, HistoryActivity::class.java))
                        2 -> context.startActivity(Intent(context, SettingsActivity::class.java))
                    }
                }
            )
        }
    }
}

