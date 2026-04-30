package com.neo.lib_call.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SpeakerBluetooth: ImageVector
    get() = ImageVector.Builder(
        name = "SpeakerBluetooth",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        group(
            scaleX = 1.5f,
            scaleY = 1.5f,
            pivotX = 0f,
            pivotY = 0f
        ) {

            path(
                fill = SolidColor(Color(0xFF808080)),
                pathFillType = PathFillType.Companion.NonZero
            ) {
                moveTo(7f, 1.334f)
                lineTo(3.5f, 5f)
                lineTo(0.871f, 5f)
                curveTo(0f, 5.894f, 0f, 8.002f, 0f, 8.002f)
                curveTo(0f, 10.11f, 0.871f, 11f, 0.871f, 11f)
                lineTo(3.5f, 11f)
                lineTo(7f, 14.666f)
                close()
            }

            path(
                fill = SolidColor(Color(0xFF808080)),
                pathFillType = PathFillType.Companion.NonZero
            ) {
                moveTo(12f, 1.303f)
                lineTo(15.989f, 5.541f)
                lineTo(13.3f, 8.01f)
                lineTo(15.988f, 10.478f)
                lineTo(12f, 14.717f)
                lineTo(12f, 9.103f)
                lineTo(9.572f, 11.143f)
                lineTo(8.928f, 10.377f)
                lineTo(11.746f, 8.01f)
                lineTo(8.928f, 5.643f)
                lineTo(9.572f, 4.877f)
                lineTo(12f, 6.916f)
                close()
                moveTo(13f, 3.717f)
                lineTo(13f, 6.957f)
                lineTo(14.508f, 5.477f)
                close()
                moveTo(13f, 9.062f)
                lineTo(13f, 12.302f)
                lineTo(14.508f, 10.542f)
                close()
            }

            path(
                fill = SolidColor(Color(0xFF808080)),
                pathFillType = PathFillType.Companion.NonZero
            ) {
                moveTo(7f, 1.334f)
                lineTo(3.5f, 5f)
                lineTo(0.871f, 5f)
                curveTo(0f, 5.894f, 0f, 8.002f, 0f, 8.002f)
                curveTo(0f, 10.11f, 0.871f, 11f, 0.871f, 11f)
                lineTo(3.5f, 11f)
                lineTo(7f, 14.666f)
                close()
                moveTo(6f, 3.828f)
                lineTo(6f, 12.172f)
                lineTo(3.928f, 10f)
                lineTo(1.414f, 10f)
                curveTo(1.382f, 9.941f, 1.394f, 9.984f, 1.348f, 9.883f)
                curveTo(1.185f, 9.52f, 1f, 8.918f, 1f, 8.002f)
                curveTo(1f, 7.085f, 1.185f, 6.482f, 1.348f, 6.117f)
                curveTo(1.393f, 6.016f, 1.382f, 6.059f, 1.414f, 6f)
                lineTo(3.928f, 6f)
                close()
            }
        }
    }.build()

val SpeakerHeadphone: ImageVector
    get() = ImageVector.Builder(
        name = "SpeakerHeadphone",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        group(
            scaleX = 1.5f,
            scaleY = 1.5f,
            pivotX = 0f,
            pivotY = 0f
        ) {

            // Speaker body
            path(
                fill = SolidColor(Color(0xFF808080)),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(7f, 1.334f)
                lineTo(3.5f, 5f)
                lineTo(0.871f, 5f)
                curveTo(0f, 5.894f, 0f, 8.002f, 0f, 8.002f)
                curveTo(0f, 10.11f, 0.871f, 11f, 0.871f, 11f)
                lineTo(3.5f, 11f)
                lineTo(7f, 14.666f)
                close()
            }

            // Headphone symbol
            path(
                fill = SolidColor(Color(0xFF808080)),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 3.2f)
                curveTo(9.68f, 3.2f, 7.8f, 5.08f, 7.8f, 7.4f)
                lineTo(7.8f, 9.6f)
                curveTo(7.8f, 10.18f, 8.27f, 10.65f, 8.85f, 10.65f)
                lineTo(9.4f, 10.65f)
                curveTo(9.98f, 10.65f, 10.45f, 10.18f, 10.45f, 9.6f)
                lineTo(10.45f, 7.85f)
                curveTo(10.45f, 7.27f, 9.98f, 6.8f, 9.4f, 6.8f)
                lineTo(8.9f, 6.8f)
                curveTo(9.15f, 5.45f, 10.37f, 4.2f, 12f, 4.2f)
                curveTo(13.63f, 4.2f, 14.85f, 5.45f, 15.1f, 6.8f)
                lineTo(14.6f, 6.8f)
                curveTo(14.02f, 6.8f, 13.55f, 7.27f, 13.55f, 7.85f)
                lineTo(13.55f, 9.6f)
                curveTo(13.55f, 10.18f, 14.02f, 10.65f, 14.6f, 10.65f)
                lineTo(15.15f, 10.65f)
                curveTo(15.73f, 10.65f, 16.2f, 10.18f, 16.2f, 9.6f)
                lineTo(16.2f, 7.4f)
                curveTo(16.2f, 5.08f, 14.32f, 3.2f, 12f, 3.2f)
                close()

                // left earcup
                moveTo(8.8f, 7.8f)
                lineTo(9.45f, 7.8f)
                lineTo(9.45f, 9.65f)
                lineTo(8.8f, 9.65f)
                close()

                // right earcup
                moveTo(14.55f, 7.8f)
                lineTo(15.2f, 7.8f)
                lineTo(15.2f, 9.65f)
                lineTo(14.55f, 9.65f)
                close()
            }

            // Speaker inner detail
            path(
                fill = SolidColor(Color(0xFF808080)),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(7f, 1.334f)
                lineTo(3.5f, 5f)
                lineTo(0.871f, 5f)
                curveTo(0f, 5.894f, 0f, 8.002f, 0f, 8.002f)
                curveTo(0f, 10.11f, 0.871f, 11f, 0.871f, 11f)
                lineTo(3.5f, 11f)
                lineTo(7f, 14.666f)
                close()

                moveTo(6f, 3.828f)
                lineTo(6f, 12.172f)
                lineTo(3.928f, 10f)
                lineTo(1.414f, 10f)
                curveTo(1.382f, 9.941f, 1.394f, 9.984f, 1.348f, 9.883f)
                curveTo(1.185f, 9.52f, 1f, 8.918f, 1f, 8.002f)
                curveTo(1f, 7.085f, 1.185f, 6.482f, 1.348f, 6.117f)
                curveTo(1.393f, 6.016f, 1.382f, 6.059f, 1.414f, 6f)
                lineTo(3.928f, 6f)
                close()
            }
        }
    }.build()