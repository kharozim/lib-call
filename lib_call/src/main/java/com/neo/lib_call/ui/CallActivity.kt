package com.neo.lib_call.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neo.lib_call.core.TimerManager
import com.neo.lib_call.model.CallRequest
import com.neo.lib_call.model.CallState
import com.neo.lib_call.model.SipCredentials
import com.neo.lib_call.util.IntentKeys
import com.neo.lib_call.util.MetadataConverter
import java.io.Serializable

class CallActivity : ComponentActivity() {
  private val viewModel: CallViewModel by viewModels {
    CallViewModel.Factory(parseRequest(intent), TimerManager())
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      val showLoading = state.callState in listOf(
        CallState.Initializing,
        CallState.Registering,
      )
      var loadingMessage by remember { mutableStateOf("") }
      val permissions = remember { requiredPermissions() }
      val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
      ) { grantResults ->
        val allGranted = permissions.all { grantResults[it] == true }
        if (allGranted) {
          viewModel.beginCall()
        } else {
          viewModel.setFatalError("Permission audio tidak lengkap. Aplikasi akan ditutup.")
          finish()
        }
      }

      LaunchedEffect(Unit) {
        val alreadyGranted = permissions.all { permission ->
          ContextCompat.checkSelfPermission(
            this@CallActivity,
            permission
          ) == PackageManager.PERMISSION_GRANTED
        }
        if (alreadyGranted) {
          viewModel.beginCall()
        } else {
          permissionLauncher.launch(permissions)
        }
      }

      LaunchedEffect(state.callState) {
        when (val callState = state.callState) {
          CallState.Initializing -> loadingMessage = "Initializing..."
          CallState.Registering -> loadingMessage = "Registering.."
          else -> loadingMessage = ""
        }
      }

      if (showLoading) {
        Loading(loadingMessage) { }
      }

      if (!state.fatalError.isNullOrEmpty()) {
        ErrorDialog(state.fatalError.orEmpty()) {
          viewModel.setFatalError("")
          finish()
        }
      }

      SetSystemBarAppearance(true)

      BackHandler() {
        viewModel.endCall()
        finish()
      }

      MaterialTheme {
        CallScreen(
          state = state,
          onEndCallClick = {
            viewModel.endCall()
            finish()
          },
          isMicMuted = state.isMicMuted,
          speakerOutput = state.speakerOutput,
          onMuteClick = { viewModel.toggleMute() },
          onSpeakerClick = { viewModel.cycleSpeakerOutput() },
          onNumpadClick = {},
        )

      }
    }
  }

  companion object {
    fun createIntent(context: Context, request: CallRequest): Intent {
      return Intent(context, CallActivity::class.java).apply {
        putExtra(IntentKeys.EXTRA_DESTINATION_NUMBER, request.destinationNumber)
        putExtra(IntentKeys.EXTRA_DESTINATION_NAME, request.destinationName)
        putExtra(IntentKeys.EXTRA_CONTACT_IMAGE, request.contactImage)
        putExtra(IntentKeys.EXTRA_METADATA, MetadataConverter.toHashMap(request.metadata))
        putExtra(IntentKeys.EXTRA_USERNAME, request.credentials.username)
        putExtra(IntentKeys.EXTRA_PASSWORD, request.credentials.password)
        putExtra(IntentKeys.EXTRA_DOMAIN, request.credentials.domain)
      }
    }

    private fun parseRequest(intent: Intent): CallRequest {
      val destinationNumber =
        requireNotNull(intent.getStringExtra(IntentKeys.EXTRA_DESTINATION_NUMBER)) {
          "Missing destination number for CallActivity."
        }
      val destinationName = intent.getStringExtra(IntentKeys.EXTRA_DESTINATION_NAME)
      val username = requireNotNull(intent.getStringExtra(IntentKeys.EXTRA_USERNAME)) {
        "Missing SIP username for CallActivity."
      }
      val password = requireNotNull(intent.getStringExtra(IntentKeys.EXTRA_PASSWORD)) {
        "Missing SIP password for CallActivity."
      }
      val domain = requireNotNull(intent.getStringExtra(IntentKeys.EXTRA_DOMAIN)) {
        "Missing SIP domain for CallActivity."
      }

      return CallRequest(
        destinationNumber = destinationNumber,
        destinationName = destinationName,
        contactImage = intent.getStringExtra(IntentKeys.EXTRA_CONTACT_IMAGE),
        metadata = MetadataConverter.fromSerializable(
          serializableExtra(
            intent,
            IntentKeys.EXTRA_METADATA
          )
        ),
        credentials = SipCredentials(
          username = username,
          password = password,
          domain = domain,
        ),
      )
    }

    private fun serializableExtra(intent: Intent, key: String): Serializable? {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getSerializableExtra(key, HashMap::class.java)
      } else {
        @Suppress("DEPRECATION")
        intent.getSerializableExtra(key)
      }
    }

    private fun requiredPermissions(): Array<String> {
      // Linphone voice call only needs microphone access for the outgoing audio stream.
      return arrayOf(
        Manifest.permission.RECORD_AUDIO,
      )
    }
  }
}

@Composable
private fun ErrorDialog(message: String, onDismiss: () -> Unit) {
  Dialog(onDismissRequest = onDismiss) {
    Text(
      message, Modifier
        .background(Color.White, RoundedCornerShape(12.dp))
        .padding(vertical = 12.dp, horizontal = 18.dp)
    )
  }
}


@Composable
private fun Loading(message: String, onDismissRequest: () -> Unit) {
  Dialog(onDismissRequest = onDismissRequest) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .background(Color.White, RoundedCornerShape(12.dp))
        .padding(24.dp)
    ) {
      CircularProgressIndicator(modifier = Modifier.size(32.dp))
      Text(message, Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
    }
  }
}