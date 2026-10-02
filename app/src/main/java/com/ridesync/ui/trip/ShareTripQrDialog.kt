package com.ridesync.ui.trip

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.data.model.SavedTrip
import com.ridesync.ui.theme.HudColors
import com.ridesync.util.QrCodeGenerator

const val RIDERsYNK_DRIVE_DOWNLOAD_URL = "https://drive.google.com/drive/folders/10SYP7-K7K8ibPgcA1PM5SKu2X17MNON2?usp=sharing"

/**
 * Share Trip QR Code & Deep Link Invite Dialog.
 * Generates an instant scannable QR Code and formatted share invitation.
 * Supports:
 * 1. Automatic deep linking: opens the trip in Traveler Travel Synk (TTS) if installed.
 * 2. Download redirect link to Google Drive if the app is not yet installed.
 */
@Composable
fun ShareTripQrDialog(
    tripTitle: String,
    lobbyCode: String,
    startDate: String = "Upcoming",
    routeDescription: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val effectiveCode = lobbyCode.ifBlank { "TTSP9421" }
    val deepLinkUrl = "ridesync://trip/join?code=$effectiveCode"
    val webJoinUrl = "https://tts.app/join/$effectiveCode"

    // Generate QR Code bitmap with the deep link content
    val qrBitmap = remember(effectiveCode) {
        QrCodeGenerator.generateQrBitmap(
            content = deepLinkUrl,
            width = 512,
            height = 512
        )
    }

    val shareText = "🏍️ Join my travel trip on Traveler Travel Synk (TTS)!\n\n" +
            "📌 Trip: '$tripTitle'\n" +
            (if (startDate.isNotBlank()) "📅 Date: $startDate\n" else "") +
            (if (routeDescription.isNotBlank()) "📍 Route: $routeDescription\n" else "") +
            "🔑 Trip Code: $effectiveCode\n\n" +
            "📲 If you already have Traveler Travel Synk (TTS) installed, open to join:\n" +
            "$deepLinkUrl\n\n" +
            "📥 If you haven't installed Traveler Travel Synk (TTS) yet, download it here:\n" +
            "$RIDERsYNK_DRIVE_DOWNLOAD_URL\n" +
            "(After installing, open the link or enter the Trip Code '$effectiveCode' to join our convoy!)"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFFFF),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF0052CC), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Trip Invite & QR Code",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tripTitle,
                    color = Color(0xFF0052CC),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // High-Contrast QR Code Container
                Box(
                    modifier = Modifier
                        .size(210.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Convoy QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = "Scan with Traveler Travel Synk (TTS) app to join this convoy instantly",
                    color = Color(0xFF475569),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                // Trip Code Banner
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text("Trip Join Code", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = effectiveCode,
                                color = Color(0xFF0052CC),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(effectiveCode))
                                Toast.makeText(context, "Trip Code copied: $effectiveCode", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF0052CC)),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0052CC)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Code", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Download App Fallback Link Card
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF0052CC), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("App Download Link (For new travelers):", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = RIDERsYNK_DRIVE_DOWNLOAD_URL,
                            color = Color(0xFF0052CC),
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(RIDERsYNK_DRIVE_DOWNLOAD_URL))
                                    Toast.makeText(context, "Download link copied!", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share Trip Invitation Link & QR")
                    context.startActivity(shareIntent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0052CC), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Trip Link & App Invite", fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = Color(0xFF64748B), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}
