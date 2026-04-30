package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.neo.lib_call.api.CallSdk

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        CallScreen()
      }
    }
  }
}

@Composable
fun CallScreen() {
  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    val context = LocalContext.current
    val destination by remember { mutableStateOf("085600431521") }
    val name by remember { mutableStateOf("Arman") }
    val user by remember { mutableStateOf("1012") }
    val pass by remember { mutableStateOf("5678") }
    val domain by remember { mutableStateOf("147.139.193.218:5551") }

    Column(
      Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(24.dp),
      verticalArrangement = Arrangement.Center
    ) {
      Column(
        Modifier
          .fillMaxWidth()
          .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
          .padding(12.dp)
      ) {
        Text("Destination : $destination")
        Text("Name : $name")
        Text("Username : $user")
        Text("Password : $pass")
        Text("Domain : $domain")
      }
      Spacer(Modifier.size(12.dp))
      Button(onClick = {
        CallSdk.makeCall(
          context,
          destinationNumber = destination,
          username = user,
          password = pass,
          domain = domain,
        )
      }) { Text("Call") }

    }
  }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme {
    CallScreen()
  }
}