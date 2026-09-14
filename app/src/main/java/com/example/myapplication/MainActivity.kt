package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
  var destination by rememberSaveable { mutableStateOf("085600431521") }
  var name by rememberSaveable { mutableStateOf("Jack Sparrow") }
  var image by rememberSaveable {
    mutableStateOf("https://akcdn.detik.net.id/api/wm/2026/02/05/suraj-chavan-1770282300425_169.png?w=1200")
  }
  var user by rememberSaveable { mutableStateOf("1012") }
  var pass by rememberSaveable { mutableStateOf("5678") }
  var domain by rememberSaveable { mutableStateOf("149.129.218.243:5551") }

  val callLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
      val callIsConnected = it.data?.extras?.getBoolean(CallSdk.IS_CALL_CONNECT) ?: false
      val callResult = it.data?.extras?.getString(CallSdk.CALL_DETAIL_RESULT)

      Log.d("TAG", "cekCallScreen callIsConnected: $callIsConnected")
      Log.d("TAG", "cekCallScreen callResult: $callResult")
    }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = Color.Transparent
  ) { innerPadding ->
    val context = LocalContext.current
    val isFormValid = destination.isNotBlank() &&
      name.isNotBlank() &&
      user.isNotBlank() &&
      pass.isNotBlank() &&
      domain.isNotBlank()

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
              MaterialTheme.colorScheme.surface,
              MaterialTheme.colorScheme.surface
            )
          )
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 24.dp)
      ) {
        CallHeader()
        Spacer(Modifier.height(24.dp))

        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(28.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
          ),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            FormSectionTitle(
              number = "01",
              title = "Data tujuan",
              description = "Informasi kontak yang akan dihubungi"
            )
            Spacer(Modifier.height(16.dp))
            CallFormField(
              value = destination,
              onValueChange = { destination = it },
              label = "Nomor tujuan",
              keyboardType = KeyboardType.Phone
            )
            CallFormField(
              value = name,
              onValueChange = { name = it },
              label = "Nama tujuan"
            )
            CallFormField(
              value = image,
              onValueChange = { image = it },
              label = "URL foto kontak",
              keyboardType = KeyboardType.Uri,
              supportingText = "Opsional"
            )

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 12.dp),
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            )

            FormSectionTitle(
              number = "02",
              title = "Akun SIP",
              description = "Kredensial untuk terhubung ke server"
            )
            Spacer(Modifier.height(16.dp))
            CallFormField(
              value = user,
              onValueChange = { user = it },
              label = "Username"
            )
            CallFormField(
              value = pass,
              onValueChange = { pass = it },
              label = "Password",
              keyboardType = KeyboardType.Password,
              isPassword = true
            )
            CallFormField(
              value = domain,
              onValueChange = { domain = it },
              label = "Domain / server",
              keyboardType = KeyboardType.Uri,
              supportingText = "Contoh: server.com:5060"
            )

            Spacer(Modifier.height(8.dp))
            Button(
              onClick = {
                val intent = CallSdk.makeCallIntent(
                  context,
                  destinationNumber = destination.trim(),
                  destinationName = name.trim(),
                  contactImage = image.trim(),
                  username = user.trim(),
                  password = pass,
                  domain = domain.trim(),
                  metadata = mapOf(
                    "telephone_id" to "telID",
                    "customer_id" to "cusID",
                    "customer_name" to name.trim(),
                  )
                )
                callLauncher.launch(intent)
              },
              enabled = isFormValid,
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
              shape = RoundedCornerShape(16.dp),
              elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
              Text(
                text = "Mulai Panggilan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
              )
            }
          }
        }

        Text(
          text = "Pastikan data akun dan server sudah benar sebelum menelepon.",
          modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(horizontal = 24.dp, vertical = 18.dp),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun CallHeader() {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Surface(
      modifier = Modifier.size(58.dp),
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primary,
      shadowElevation = 4.dp
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = "N",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimary
        )
      }
    }
    Spacer(Modifier.width(14.dp))
    Column {
      Text(
        text = "Neo Call",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = "Siapkan panggilan baru",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun FormSectionTitle(
  number: String,
  title: String,
  description: String
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.secondaryContainer
    ) {
      Text(
        text = number,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSecondaryContainer
      )
    }
    Spacer(Modifier.width(12.dp))
    Column {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@Composable
private fun CallFormField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  keyboardType: KeyboardType = KeyboardType.Text,
  isPassword: Boolean = false,
  supportingText: String? = null
) {
  var passwordVisible by rememberSaveable { mutableStateOf(false) }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    visualTransformation = if (isPassword && !passwordVisible) {
      PasswordVisualTransformation()
    } else {
      VisualTransformation.None
    },
    trailingIcon = if (isPassword) {
      {
        TextButton(onClick = { passwordVisible = !passwordVisible }) {
          Text(if (passwordVisible) "Sembunyikan" else "Lihat")
        }
      }
    } else {
      null
    },
    supportingText = supportingText?.let { text ->
      { Text(text) }
    },
    shape = RoundedCornerShape(16.dp),
    colors = OutlinedTextFieldDefaults.colors(
      unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
      focusedContainerColor = MaterialTheme.colorScheme.surface
    ),
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 10.dp)
  )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme {
    CallScreen()
  }
}
