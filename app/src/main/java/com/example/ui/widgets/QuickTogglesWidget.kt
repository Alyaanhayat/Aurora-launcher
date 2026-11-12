package com.example.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickTogglesWidget(
    modifier: Modifier = Modifier
) {
    var wifiOn by remember { mutableStateOf(true) }
    var bluetoothOn by remember { mutableStateOf(true) }
    var flashlightOn by remember { mutableStateOf(false) }
    var dndOn by remember { mutableStateOf(false) }
    var batterySaverOn by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("productivity_quick_toggles_widget"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        ),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Toggles",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Quick Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                ToggleIconTile(
                    title = "Wi-Fi",
                    icon = Icons.Default.Wifi,
                    isActive = wifiOn,
                    onClick = { wifiOn = !wifiOn },
                    tag = "toggle_wifi"
                )
                ToggleIconTile(
                    title = "Bluetooth",
                    icon = Icons.Default.Bluetooth,
                    isActive = bluetoothOn,
                    onClick = { bluetoothOn = !bluetoothOn },
                    tag = "toggle_bluetooth"
                )
                ToggleIconTile(
                    title = "Torch",
                    icon = Icons.Default.FlashlightOn,
                    isActive = flashlightOn,
                    onClick = { flashlightOn = !flashlightOn },
                    tag = "toggle_torch"
                )
                ToggleIconTile(
                    title = "DND",
                    icon = Icons.Default.DoNotDisturbOn,
                    isActive = dndOn,
                    onClick = { dndOn = !dndOn },
                    tag = "toggle_dnd"
                )
                ToggleIconTile(
                    title = "Eco Saver",
                    icon = Icons.Default.BatterySaver,
                    isActive = batterySaverOn,
                    onClick = { batterySaverOn = !batterySaverOn },
                    tag = "toggle_saver"
                )
            }
        }
    }
}

@Composable
private fun ToggleIconTile(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        label = "toggle_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "toggle_fg"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag(tag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
