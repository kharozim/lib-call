package com.neo.lib_call.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.neo.lib_call.model.CallRequest
import com.neo.lib_call.model.SipCredentials
import com.neo.lib_call.util.IntentKeys
import com.neo.lib_call.util.MetadataConverter
import java.io.Serializable

class CallActivity : ComponentActivity() {
  private val viewModel: CallViewModel by viewModels {
    CallViewModel.Factory(parseRequest(intent))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
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

      LaunchedEffect(state.fatalError) {
        if (state.fatalError != null) {
          Toast.makeText(
            this@CallActivity,
            state.fatalError,
            Toast.LENGTH_LONG
          ).show()
          finish()
        }
      }

      SetSystemBarAppearance(true)

      MaterialTheme {
        Surface {
          CallScreen(
            state = state,
            onEndCall = {
              viewModel.endCall()
              finish()
            }
          )
        }
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
        android.Manifest.permission.RECORD_AUDIO,
      )
    }
  }
}
