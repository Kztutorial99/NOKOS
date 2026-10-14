package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveNokos
import com.example.model.NokosStatus
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberRose
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardHover
import com.example.ui.theme.DarkCardStroke
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.BrandIcon

@Composable
fun ActiveOrderCard(
    order: ActiveNokos,
    onCancel: () -> Unit,
    onFinish: () -> Unit,
    onSimulateSms: () -> Unit,
    onDismiss: () -> Unit,
    onCopyOtp: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showFullSms by remember { mutableStateOf(false) }
    var copiedOtp by remember { mutableStateOf(false) }
    var copiedPhone by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val borderColor = when (order.status) {
        NokosStatus.RECEIVED_OTP -> CyberEmerald
        NokosStatus.WAITING_SMS -> CyberCyan.copy(alpha = 0.6f)
        NokosStatus.COMPLETED -> CyberEmerald.copy(alpha = 0.4f)
        NokosStatus.CANCELED, NokosStatus.EXPIRED -> CyberRose.copy(alpha = 0.4f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("order_card_${order.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Flag + Service + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandIcon(
                        serviceId = order.serviceId,
                        serviceName = order.serviceName,
                        fallbackEmoji = order.flagEmoji,
                        size = 30.dp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = order.serviceName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = order.flagEmoji,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "${order.countryName} • ID: ${order.id}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                // Status Badge
                StatusPill(status = order.status, pulseAlpha = pulseAlpha)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Phone Number Section with 1-tap Copy
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(order.phoneNumber))
                        copiedPhone = true
                        Toast.makeText(context, "Nomor disalin: ${order.phoneNumber}", Toast.LENGTH_SHORT).show()
                    },
                color = DarkCardHover,
                border = BorderStroke(1.dp, DarkCardStroke)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NOMOR VIRTUAL (NOKOS)",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = order.phoneNumber,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (copiedPhone) {
                            Text(
                                text = "Disalin!",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberEmerald,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Tersalin",
                                tint = CyberEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(order.phoneNumber))
                                    copiedPhone = true
                                    Toast.makeText(context, "Nomor disalin: ${order.phoneNumber}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("copy_phone_${order.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Salin Nomor",
                                    tint = CyberCyan
                                )
                            }
                        }
                    }
                }
            }

            // OTP Received Section
            if (order.status == NokosStatus.RECEIVED_OTP && !order.otpCode.isNullOrEmpty()) {
                val otp = order.otpCode
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberEmerald.copy(alpha = 0.12f))
                        .border(1.5.dp, CyberEmerald, RoundedCornerShape(12.dp))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(otp))
                            copiedOtp = true
                            onCopyOtp?.invoke(otp)
                        }
                        .testTag("tap_to_copy_otp_box_${order.id}"),
                    color = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = CyberEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "KODE OTP VERIFIKASI DITERIMA",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyberEmerald,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = otp,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            letterSpacing = 4.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (copiedOtp) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = CyberEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (copiedOtp) "Kode tersalin! Ketuk untuk salin ulang" else "Ketuk kode untuk langsung menyalin",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = CyberEmerald
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(otp))
                                copiedOtp = true
                                onCopyOtp?.invoke(otp)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberEmerald,
                                contentColor = DarkCard
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("copy_otp_btn_${order.id}")
                        ) {
                            Icon(
                                imageVector = if (copiedOtp) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (copiedOtp) "OTP Berhasil Disalin!" else "Salin Kode OTP",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Waiting Countdown Timer for Active Order
            if (order.status == NokosStatus.WAITING_SMS) {
                Spacer(modifier = Modifier.height(12.dp))
                val minutes = order.remainingSeconds / 60
                val seconds = order.remainingSeconds % 60
                val formattedTime = String.format("%02d:%02d", minutes, seconds)
                val progress = (order.remainingSeconds.toFloat() / (15 * 60)).coerceIn(0f, 1f)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CyberAmber)
                                    .alpha(pulseAlpha)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Menunggu SMS masuk otomatis...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = CyberCyan,
                        trackColor = DarkCardStroke
                    )
                }
            }

            // Expandable SMS Body Preview (if available)
            if (!order.fullSms.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showFullSms = !showFullSms }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lihat Pesan SMS Lengkap",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan
                    )
                    Icon(
                        imageVector = if (showFullSms) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = showFullSms) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = DarkCardHover,
                        border = BorderStroke(1.dp, DarkCardStroke)
                    ) {
                        Text(
                            text = order.fullSms,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If in demo or waiting, allow quick simulation
                if (order.status == NokosStatus.WAITING_SMS) {
                    OutlinedButton(
                        onClick = onSimulateSms,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CyberCyan
                        ),
                        border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("simulate_sms_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Kirim Uji SMS", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CyberRose
                        ),
                        border = BorderStroke(1.dp, CyberRose.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cancel_btn_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Batal & Refund", fontSize = 12.sp)
                    }
                } else if (order.status == NokosStatus.RECEIVED_OTP) {
                    Button(
                        onClick = onFinish,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberEmerald,
                            contentColor = DarkCard
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("finish_btn_${order.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Selesai & Arsip", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Completed, Canceled, Expired -> allow dismiss
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hapus dari Daftar", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(status: NokosStatus, pulseAlpha: Float) {
    val (label, bg, fg, hasPulse) = when (status) {
        NokosStatus.WAITING_SMS -> Quad("Menunggu SMS", CyberAmber.copy(alpha = 0.15f), CyberAmber, true)
        NokosStatus.RECEIVED_OTP -> Quad("OTP Diterima", CyberEmerald.copy(alpha = 0.2f), CyberEmerald, false)
        NokosStatus.COMPLETED -> Quad("Selesai", CyberEmerald.copy(alpha = 0.15f), CyberEmerald, false)
        NokosStatus.CANCELED -> Quad("Dibatalkan", CyberRose.copy(alpha = 0.15f), CyberRose, false)
        NokosStatus.EXPIRED -> Quad("Kedaluwarsa", TextMuted.copy(alpha = 0.2f), TextMuted, false)
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasPulse) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(fg)
                        .alpha(pulseAlpha)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
