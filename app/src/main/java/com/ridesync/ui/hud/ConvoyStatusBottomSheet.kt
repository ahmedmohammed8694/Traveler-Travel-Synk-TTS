package com.ridesync.ui.hud

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.data.model.ConvoyMember
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.RiderLocationPing
import com.ridesync.data.model.RiderStatus
import com.ridesync.util.rememberRiderAvatarBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvoyStatusBottomSheet(
    convoyMembers: List<ConvoyMember>,
    riderPings: Map<String, RiderLocationPing>,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = com.ridesync.ui.theme.HudColors.ObsidianSurface,
        contentColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Convoy Roster (${convoyMembers.size} Riders)",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(convoyMembers) { member ->
                    val ping = riderPings[member.userId]
                    ConvoyMemberCard(member = member, ping = ping)
                }
            }
        }
    }
}

@Composable
private fun ConvoyMemberCard(
    member: ConvoyMember,
    ping: RiderLocationPing?
) {
    val statusColor = when (member.status) {
        RiderStatus.RIDING -> com.ridesync.ui.theme.HudColors.StatusRiding
        RiderStatus.STOPPED -> com.ridesync.ui.theme.HudColors.StatusStopped
        RiderStatus.DELAYED -> com.ridesync.ui.theme.HudColors.StatusDelayed
        RiderStatus.SOS -> com.ridesync.ui.theme.HudColors.StatusSos
    }
    val avatarBitmap by rememberRiderAvatarBitmap(member.photoUrl)

    Surface(
        color = com.ridesync.ui.theme.HudColors.ObsidianElevated,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, com.ridesync.ui.theme.HudColors.ObsidianBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Role Icon Badge / Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(2.dp, statusColor, CircleShape)
                        .background(statusColor.copy(alpha = 0.2f))
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap!!.asImageBitmap(),
                            contentDescription = "${member.displayName} Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        when (member.role) {
                            ConvoyRole.LEAD -> Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Lead",
                                tint = com.ridesync.ui.theme.HudColors.CyanPrimary
                            )
                            ConvoyRole.SWEEP -> Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Sweep",
                                tint = com.ridesync.ui.theme.HudColors.CobaltBlue
                            )
                            ConvoyRole.MEMBER -> Text(
                                text = member.displayName.take(1).uppercase(),
                                color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = member.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = com.ridesync.ui.theme.HudColors.TextCrispWhite
                        )
                        if (member.role != ConvoyRole.MEMBER) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = member.role.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (member.role == ConvoyRole.LEAD) com.ridesync.ui.theme.HudColors.CyanPrimary else com.ridesync.ui.theme.HudColors.CobaltBlue,
                                modifier = Modifier
                                    .background(com.ridesync.ui.theme.HudColors.ObsidianBorder.copy(alpha = 0.4f), CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "${member.vehicleModel} • ${member.status.name}",
                        fontSize = 13.sp,
                        color = com.ridesync.ui.theme.HudColors.TextCoolSilver
                    )

                    if (member.isRouteDeviated) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Route Off-Track (>150m)",
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Metrics (Speed & Distance Behind Lead)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("%.0f km/h", ping?.speedKmh ?: 0f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = com.ridesync.ui.theme.HudColors.TextCrispWhite
                )

                val behindText = if (member.distanceBehindLeadMeters > 1000) {
                    String.format("-%.1f km", member.distanceBehindLeadMeters / 1000)
                } else {
                    String.format("-%.0f m", member.distanceBehindLeadMeters)
                }

                Text(
                    text = behindText,
                    fontSize = 13.sp,
                    color = com.ridesync.ui.theme.HudColors.TextCoolSilver
                )
            }
        }
    }
}
