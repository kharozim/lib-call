package com.neo.lib_call.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.neo.lib_call.model.CallState

@Composable
internal fun CallScreen(
  state: CallUiState,
  onEndCall: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      if (state.contactImage != null) {
        AsyncImage(
          model = state.contactImage,
          contentDescription = "Contact image",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .size(140.dp)
            .clip(CircleShape)
        )
      } else {
        Box(
          modifier = Modifier
            .size(140.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = state.destinationNumber.take(2).uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
          )
        }
      }

      Text(
        text = state.destinationNumber,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold
      )

      Text(
        text = state.statusMessage,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
      )

      if (state.metadata.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Metadata",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
            )
            state.metadata.forEach { (key, value) ->
              Text(
                text = "$key: $value",
                style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }
      }

      Button(onClick = onEndCall) {
        Text("End call")
      }
    }
  }
}

@Preview()
@Composable
private fun Prev() {
  MaterialTheme {
    CallScreen(
      state = CallUiState(
        destinationNumber = "08123123",
//        contactImage = "https://akcdn.detik.net.id/api/wm/2026/02/05/suraj-chavan-1770282300425_169.png?w=1200",
        metadata = mapOf("phone_id" to "123123"),
        callState = CallState.Connected,
//        statusMessage = "Status"
      )
    ) { }
  }
}