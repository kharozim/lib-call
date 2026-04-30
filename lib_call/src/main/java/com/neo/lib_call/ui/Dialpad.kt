package cc.neo.sdkcall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neo.lib_call.core.DtmfTonePlayer

@Composable
internal fun DialPad(
  modifier: Modifier = Modifier,
  onKeyPress: (String) -> Unit,
) {

  val dtmfPlayer = remember { DtmfTonePlayer() }

  DisposableEffect(Unit) {
    onDispose {
      dtmfPlayer.release()
    }
  }

  val keys = listOf(
    "1", "2", "3",
    "4", "5", "6",
    "7", "8", "9",
    "*", "0", "#"
  )

  var input by remember { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {

    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
    ) {
      Text(
        text = input.ifEmpty { " " },
        fontSize = 32.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.sp,
        modifier = Modifier
          .weight(1f)
          .padding(vertical = 16.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1
      )
      IconButton(
        onClick = {
          if (input.isNotEmpty()) {
            input = input.dropLast(1)
          }
        },
        modifier = Modifier
          .size(56.dp)
          .pointerInput(Unit) {
            detectTapGestures(
              onLongPress = { input = "" } // clear all
            )
          }
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Backspace,
          contentDescription = "Delete"
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    /* ===== DIALPAD GRID ===== */
    LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      userScrollEnabled = false,
      modifier = Modifier.fillMaxWidth()
    ) {
      items(keys) { key ->
        DialPadButton(
          text = key,
          onClick = {
            input += key
            onKeyPress(key)
            dtmfPlayer.play(key)
          }
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    /* ===== DELETE BUTTON ===== */
    IconButton(
      onClick = {
        onKeyPress("close")
      },
      modifier = Modifier
        .size(56.dp)
        .pointerInput(Unit) {
          detectTapGestures(
            onLongPress = { input = "" } // clear all
          )
        }
    ) {
      Icon(
        imageVector = Icons.Outlined.ArrowDownward,
        contentDescription = "Hide"
      )
    }
  }
}

@Composable
private fun DialPadButton(
  text: String,
  onClick: () -> Unit,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
      .padding(10.dp)
      .aspectRatio(1f)
      .clip(CircleShape)
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .clickable { onClick() }
  ) {
    Text(
      text = text,
      fontSize = 26.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Preview
@Composable
private fun Prev() {
  MaterialTheme {
    DialPad() { }
  }
}