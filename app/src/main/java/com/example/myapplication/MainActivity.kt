package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.neo.lib_call.api.CallSdk
import com.neo.lib_call.model.RegisterState
import com.neo.lib_call.model.SipCredentials
import kotlinx.coroutines.launch

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
  val coroutineScope = rememberCoroutineScope()
  val registrationState by CallSdk.registrationState.collectAsState()
  var registrationError by remember { mutableStateOf<String?>(null) }
  var lastCallConnected by remember { mutableStateOf<Boolean?>(null) }
  val callLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
      lastCallConnected = it.data?.extras?.getBoolean(CallSdk.IS_CALL_CONNECT) ?: false
    }

  val callState by CallSdk.callState.collectAsState()

  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf("085600431521") }
//    var destination by remember { mutableStateOf("085771518294") }
    var name by remember { mutableStateOf("Jack Sparrow") }
    var image by remember { mutableStateOf("https://akcdn.detik.net.id/api/wm/2026/02/05/suraj-chavan-1770282300425_169.png?w=1200") }
    var user by remember { mutableStateOf("1012") }
    var pass by remember { mutableStateOf("5678") }
    var domain by remember { mutableStateOf("147.139.193.218:5551") }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
              MaterialTheme.colorScheme.surface,
              MaterialTheme.colorScheme.surface,
            )
          )
        )
        .padding(innerPadding),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 24.dp),
      ) {
        AppHeader()
        Spacer(Modifier.height(22.dp))

        RegistrationStatusCard(registrationState)
        Spacer(Modifier.height(16.dp))

        if (registrationState == RegisterState.Ok) {
          FormCard("Cal State", "Current call state") {
            Text(
              callState.name,
              style = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
              modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.15f))
                .padding(vertical = 6.dp)
            )
          }
        }

        Spacer(Modifier.height(16.dp))

        FormCard(
          title = "SIP account",
          subtitle = "Register once before starting a call",
        ) {
          AppTextField(
            value = user,
            onValueChange = { user = it },
            label = "Username",
            icon = Icons.Rounded.AccountCircle,
          )
          Spacer(Modifier.height(12.dp))
          AppTextField(
            value = pass,
            onValueChange = { pass = it },
            label = "Password",
            icon = Icons.Rounded.Lock,
            visualTransformation = if (passwordVisible) {
              VisualTransformation.None
            } else {
              PasswordVisualTransformation()
            },
            trailingIcon = {
              IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Text(
                  text = if (passwordVisible) "Hide" else "Show",
                  style = MaterialTheme.typography.labelSmall,
                )
              }
            },
          )
          Spacer(Modifier.height(12.dp))
          AppTextField(
            value = domain,
            onValueChange = { domain = it },
            label = "Domain / server",
            icon = Icons.Rounded.AccountCircle,
          )

          registrationError?.let { error ->
            Spacer(Modifier.height(12.dp))
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
              )
            }
          }

          Spacer(Modifier.height(16.dp))
          Button(
            enabled = registrationState !in listOf(
              RegisterState.Progress,
              RegisterState.Refreshing,
            ) && user.isNotBlank() && pass.isNotBlank() && domain.isNotBlank(),
            onClick = {
              registrationError = null
              coroutineScope.launch {
                CallSdk.register(
                  SipCredentials(
                    username = user,
                    password = pass,
                    domain = domain,
                  )
                ).onSuccess {
                  passwordVisible = false
                }.onFailure { error ->
                  registrationError = error.message ?: "Registration failed"
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
            shape = RoundedCornerShape(14.dp),
          ) {
            if (registrationState in listOf(
                RegisterState.Progress,
                RegisterState.Refreshing,
              )
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
              Spacer(Modifier.width(10.dp))
              Text("Registering…")
            } else {
              Icon(Icons.Rounded.CheckCircle, contentDescription = null)
              Spacer(Modifier.width(10.dp))
              Text(if (registrationState == RegisterState.Ok) "Re-register" else "Register")
            }
          }

          if (registrationState == RegisterState.Ok) {
            TextButton(
              onClick = {
                registrationError = null
                coroutineScope.launch {
                  CallSdk.unregister().onFailure { error ->
                    registrationError = error.message ?: "Unregister failed"
                  }
                }
              },
              modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
              Text("Unregister account")
            }
          }
        }

        Spacer(Modifier.height(16.dp))
        FormCard(
          title = "Call destination",
          subtitle = "Choose who you want to contact",
        ) {
          AppTextField(
            value = destination,
            onValueChange = { destination = it },
            label = "Phone number",
            icon = Icons.Rounded.Phone,
            keyboardType = KeyboardType.Phone,
          )
          Spacer(Modifier.height(12.dp))
          AppTextField(
            value = name,
            onValueChange = { name = it },
            label = "Display name",
            icon = Icons.Rounded.Person,
          )
          Spacer(Modifier.height(12.dp))
          AppTextField(
            value = image,
            onValueChange = { image = it },
            label = "Avatar URL (optional)",
            icon = Icons.Rounded.Person,
          )
        }

        Spacer(Modifier.height(18.dp))
        Button(
          enabled = registrationState == RegisterState.Ok && destination.isNotBlank(),
          onClick = {
            val intent = CallSdk.makeCallIntent(
              context,
              destinationNumber = destination,
              contactImage = image,
              destinationName = name,
              metadata = mapOf(
                "telephone_id" to "telID",
                "customer_id" to "cusID",
                "customer_name" to name,
              )
            )
            callLauncher.launch(intent)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
          ),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        ) {
          Icon(Icons.Rounded.Call, contentDescription = null)
          Spacer(Modifier.width(10.dp))
          Text(
            text = if (registrationState == RegisterState.Ok) "Start call" else "Register to call",
            fontWeight = FontWeight.SemiBold,
          )
        }

        lastCallConnected?.let { connected ->
          Spacer(Modifier.height(14.dp))
          LastCallCard(connected)
        }
        Spacer(Modifier.height(24.dp))
      }
    }
  }
}

@Composable
private fun AppHeader() {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Surface(
      color = MaterialTheme.colorScheme.primary,
      shape = CircleShape,
      shadowElevation = 6.dp,
      modifier = Modifier.size(52.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Rounded.Call,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.size(25.dp),
        )
      }
    }
    Spacer(Modifier.width(14.dp))
    Column {
      Text(
        text = "Neo Call SDK",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = "SIP registration & call playground",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun RegistrationStatusCard(state: RegisterState) {
  val statusColor = when (state) {
    RegisterState.Ok -> Color(0xFF168567)
    RegisterState.Failed -> MaterialTheme.colorScheme.error
    RegisterState.Progress,
    RegisterState.Refreshing,
      -> Color(0xFFE08A15)

    RegisterState.None,
    RegisterState.Cleared,
      -> MaterialTheme.colorScheme.outline
  }
  val statusTitle = when (state) {
    RegisterState.Ok -> "SIP account is ready"
    RegisterState.Progress -> "Registering SIP account"
    RegisterState.Refreshing -> "Refreshing registration"
    RegisterState.Failed -> "Registration failed"
    RegisterState.Cleared -> "Registration cleared"
    RegisterState.None -> "Not registered"
  }
  val statusDescription = when (state) {
    RegisterState.Ok -> "You can start an outgoing call"
    RegisterState.Progress,
    RegisterState.Refreshing,
      -> "Please wait while the connection is prepared"

    RegisterState.Failed -> "Check the account information and try again"
    RegisterState.Cleared,
    RegisterState.None,
      -> "Complete the SIP account form below"
  }

  Card(
    colors = CardDefaults.cardColors(
      containerColor = statusColor.copy(alpha = 0.12f),
    ),
    shape = RoundedCornerShape(18.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(16.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(44.dp)
          .background(statusColor.copy(alpha = 0.18f), CircleShape),
      ) {
        if (state in listOf(RegisterState.Progress, RegisterState.Refreshing)) {
          CircularProgressIndicator(
            color = statusColor,
            strokeWidth = 2.dp,
            modifier = Modifier.size(22.dp),
          )
        } else {
          Icon(
            imageVector = if (state == RegisterState.Ok) {
              Icons.Rounded.CheckCircle
            } else {
              Icons.Rounded.AccountCircle
            },
            contentDescription = null,
            tint = statusColor,
          )
        }
      }
      Spacer(Modifier.width(13.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = statusTitle,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = statusDescription,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Surface(
        color = statusColor,
        shape = RoundedCornerShape(50),
      ) {
        Text(
          text = state.name.uppercase(),
          color = Color.White,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
      }
    }
  }
}

@Composable
private fun FormCard(
  title: String,
  subtitle: String,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(16.dp))
      content()
    }
  }
}

@Composable
private fun AppTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  icon: ImageVector,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardType: KeyboardType = KeyboardType.Text,
  trailingIcon: (@Composable () -> Unit)? = null,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    leadingIcon = { Icon(icon, contentDescription = null) },
    trailingIcon = trailingIcon,
    visualTransformation = visualTransformation,
    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    singleLine = true,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth(),
  )
}

@Composable
private fun LastCallCard(connected: Boolean) {
  val containerColor = if (connected) {
    Color(0xFF168567).copy(alpha = 0.12f)
  } else {
    MaterialTheme.colorScheme.errorContainer
  }
  val contentColor = if (connected) {
    Color(0xFF126B55)
  } else {
    MaterialTheme.colorScheme.onErrorContainer
  }

  Surface(
    color = containerColor,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
    ) {
      Icon(
        imageVector = if (connected) Icons.Rounded.CheckCircle else Icons.Rounded.Phone,
        contentDescription = null,
        tint = contentColor,
      )
      Spacer(Modifier.width(10.dp))
      Text(
        text = if (connected) {
          "Last call was connected successfully"
        } else {
          "Last call ended before connecting"
        },
        color = contentColor,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
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
