package com.neo.lib_call.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import cc.neo.sdkcall.ui.DialPad
import coil.compose.AsyncImage
import com.neo.lib_call.R
import com.neo.lib_call.model.CallState
import com.neo.lib_call.model.SpeakerOut

/**
 * Created by Kharozim
 * 30/04/26 - kharozim.wrk@gmail.com
 * Copyright (c) 2026. My Application
 * All Rights Reserved
 */
@Composable
internal fun CallScreen(
  isMicMuted: Boolean,
  speakerOutput: SpeakerOut?,
  state: CallUiState,
  onMuteClick: () -> Unit,
  onSpeakerOutputSelected: (SpeakerOut) -> Unit,
  onNumpadClick: (number: String) -> Unit,
  onEndCallClick: () -> Unit,
) {
  var showDialPad by rememberSaveable { mutableStateOf(false) }
  var showSpeakerDialog by rememberSaveable { mutableStateOf(false) }

  BackHandler {
    when {
      showSpeakerDialog -> showSpeakerDialog = false
      showDialPad -> showDialPad = false
      else -> onEndCallClick()
    }
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .navigationBarsPadding()
  ) { padding ->
    Box {
      MultiLayerGradientBackground()
      Column(
        modifier = Modifier
          .padding(padding)
          .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Spacer(modifier = Modifier.height(20.dp))
          Text(
            text = state.metadata["call_title"] ?: "Operator Call",
            style = MaterialTheme.typography.headlineSmall
          )
          Spacer(modifier = Modifier.height(60.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = state.timeCall, style = MaterialTheme.typography.bodyLarge)
          Spacer(modifier = Modifier.height(55.dp))
          CallAvatar(state.contactImage)
          Spacer(modifier = Modifier.height(35.dp))
          Text(
            text = state.destinationName ?: state.destinationNumber,
            style = MaterialTheme.typography.headlineSmall
          )
          if (state.statusMessage.isNotEmpty()) {
            Text(
              text = state.statusMessage,
              color = Color.Red,
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 15.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RoundIconButton(
            icon = when (speakerOutput) {
              SpeakerOut.Bluethooth -> SpeakerBluetooth
              SpeakerOut.Headphone -> SpeakerHeadphone
              else -> Icons.AutoMirrored.Outlined.VolumeUp
            },
            label = state.metadata["call_btn_speaker"] ?: "Speaker",
            onClick = { showSpeakerDialog = true },
            backgroundColor = if (speakerOutput == SpeakerOut.LoadSpeaker) Color(0xFF00BABD) else
              Color(0xFFE9F8F9),
            iconTint = if (speakerOutput == SpeakerOut.LoadSpeaker) Color.White else
              Color(0xFF17666A),
            enabled = state.callState !in listOf(
              CallState.Ended,
              CallState.RegistrationFailed,
              CallState.Failed
            )
          )

          RoundIconButton(
            icon = Icons.Filled.Dialpad,
            label = state.metadata["call_numpad"] ?: "Numpad",
            onClick = { showDialPad = true },
            enabled = state.callState == CallState.Connected
          )

          RoundIconButton(
            icon = Icons.Default.MicOff,
            label = state.metadata["call_btn_mute"] ?: "Mute",
            onClick = onMuteClick,
            backgroundColor = if (isMicMuted) Color(0xFF00BABD) else Color(0xFFE9F8F9),
            iconTint = if (isMicMuted) Color.White else Color(0xFF17666A),
            enabled = state.callState == CallState.Connected
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 55.dp),
          horizontalArrangement = Arrangement.Center
        ) {
          RoundIconButton(
            icon = Icons.Filled.Close,
            label = "",
            onClick = onEndCallClick,
            backgroundColor = Color.Red,
            iconTint = Color.White
          )
//          if (callStatus == "incoming") {
//            Spacer(modifier = Modifier.width(60.dp))
//            RoundIconButton(
//              icon = Icons.Default.Phone,
//              label = "",
//              onClick = onAnswerCallClick,
//              backgroundColor = Color.Green,
//              iconTint = Color.White
//            )
//          }
        }
      }

      AnimatedVisibility(
        visible = showDialPad,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = Modifier
          .fillMaxWidth()
          .zIndex(2f)
          .align(Alignment.BottomCenter)
      ) {
        Surface(tonalElevation = 8.dp) {
          DialPad(onKeyPress = { if (it == "close") showDialPad = false else onNumpadClick(it) })
        }
      }

      if (showSpeakerDialog) {
        SpeakerOutputDialog(
          currentOutput = speakerOutput,
          availableOutputs = state.availableSpeakerOutputs,
          onDismiss = { showSpeakerDialog = false },
          onSelect = { output ->
            showSpeakerDialog = false
            onSpeakerOutputSelected(output)
          }
        )
      }
    }
  }
}

@Composable
private fun SpeakerOutputDialog(
  currentOutput: SpeakerOut?,
  availableOutputs: List<SpeakerOut>,
  onDismiss: () -> Unit,
  onSelect: (SpeakerOut) -> Unit,
) {
  val orderedOutputs = remember(availableOutputs) {
    listOf(
      SpeakerOut.Earpiece,
      SpeakerOut.LoadSpeaker,
      SpeakerOut.Bluethooth,
      SpeakerOut.Headphone,
    ).filter { availableOutputs.contains(it) }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(shape = RoundedCornerShape(16.dp)) {
      Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
          text = "Audio output",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        if (orderedOutputs.isEmpty()) {
          Text(
            text = "No audio output available",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
          )
        } else {
          orderedOutputs.forEach { output ->
            SpeakerOutputRow(
              output = output,
              selected = output == currentOutput,
              onClick = { onSelect(output) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SpeakerOutputRow(
  output: SpeakerOut,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 20.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = output.icon(),
        contentDescription = null,
        tint = if (selected) Color(0xFF00BABD) else Color(0xFF17666A)
      )
      Text(
        text = output.label(),
        modifier = Modifier.padding(start = 12.dp),
        style = MaterialTheme.typography.bodyLarge
      )
    }
    if (selected) {
      Icon(
        imageVector = Icons.Default.Check,
        contentDescription = null,
        tint = Color(0xFF00BABD)
      )
    }
  }
}

private fun SpeakerOut.label(): String {
  return when (this) {
    SpeakerOut.Earpiece -> "Earpiece"
    SpeakerOut.LoadSpeaker -> "Load speaker"
    SpeakerOut.Bluethooth -> "Bluetooth"
    SpeakerOut.Headphone -> "Headphone"
  }
}

private fun SpeakerOut.icon() = when (this) {
  SpeakerOut.Earpiece -> Icons.AutoMirrored.Outlined.VolumeUp
  SpeakerOut.LoadSpeaker -> Icons.AutoMirrored.Outlined.VolumeUp
  SpeakerOut.Bluethooth -> SpeakerBluetooth
  SpeakerOut.Headphone -> SpeakerHeadphone
}

@Composable
private fun MultiLayerGradientBackground() {
  Box(modifier = Modifier.fillMaxSize()) {
    Box(
      modifier = Modifier
        .matchParentSize()
        .background(
          Brush.horizontalGradient(
            listOf(
              Color(0xFFFFF4DF),
              Color(0xFFFFFFFF),
              Color(0xFFDAFFFF)
            )
          )
        )
    )
    Box(
      modifier = Modifier
        .matchParentSize()
        .background(Brush.verticalGradient(listOf(Color(0x00F6F6F6), Color(0xFFF6F6F6))))
    )
  }
}

@Composable
private fun CallAvatar(imageUrl: String?) {
  Box(
    modifier = Modifier
      .size(160.dp)
      .clip(CircleShape)
      .background(Color.LightGray),
    contentAlignment = Alignment.Center
  ) {
    if (imageUrl.isNullOrBlank()) {
      Icon(
        Icons.Default.Person,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(80.dp)
      )
    } else {
      AsyncImage(
        model = imageUrl,
        contentDescription = null,
        placeholder = painterResource(R.drawable.ic_placeholder_person),
        modifier = Modifier
          .fillMaxSize()
          .clip(CircleShape)
          .border(2.dp, Color.Gray, CircleShape)
      )
    }
  }
}

@Composable
fun RoundIconButton(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  backgroundColor: Color = Color(0xFFE9F8F9),
  iconTint: Color = Color(0xFF17666A),
  enabled: Boolean = true,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier = Modifier
        .size(64.dp)
        .clip(CircleShape)
        .background(if (enabled) backgroundColor else backgroundColor.copy(0.4f))
        .clickable(enabled = enabled, onClick = onClick),
      contentAlignment = Alignment.Center
    ) {
      Icon(icon, contentDescription = label, tint = if (enabled) iconTint else iconTint.copy(0.4f))
    }
    if (label.isNotBlank()) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = if (enabled) Color.Gray else Color.Gray.copy(0.4f)
      )
    }
  }
}

@Preview
@Composable
private fun Prev() {
  MaterialTheme {
    CallScreen(
      isMicMuted = false,
      speakerOutput = SpeakerOut.LoadSpeaker,
      state = CallUiState(
        destinationNumber = "08123123",
        destinationName = "Ira Adi",
        contactImage = "cumi",
        metadata = mapOf(),
        callState = CallState.Connected,
        statusMessage = "call end",
        fatalError = "",
        timeCall = "12:12"
      ),
      onMuteClick = {},
      onSpeakerOutputSelected = {},
      onNumpadClick = {},
      onEndCallClick = {}
    )
  }
}
