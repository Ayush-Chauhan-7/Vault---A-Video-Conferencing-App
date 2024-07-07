package com.example.videoconferencingapp.presentation.sign_in

import android.accounts.Account
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun AccountListDialog(
    accounts: Array<Account>,
    onSignOut: () -> Unit,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "Accounts") },
        text = {
            LazyColumn {
                items(accounts) { account ->
                    Text(text = account.name)
                }
            }
        },
        confirmButton = {
            Button(onClick = onSignOut) {
                Text("Sign Out")
            }
        }
    )
}