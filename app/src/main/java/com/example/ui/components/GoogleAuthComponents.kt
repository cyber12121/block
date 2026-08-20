package com.example.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthUser
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.IndigoPrimary

/**
 * Clean vector-drawn Google "G" logo
 */
@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w / 2f

        val blue = Color(0xFF4285F4)
        val green = Color(0xFF34A853)
        val yellow = Color(0xFFFBBC05)
        val red = Color(0xFFEA4335)

        drawArc(
            color = red,
            startAngle = 180f,
            sweepAngle = 100f,
            useCenter = true
        )
        drawArc(
            color = yellow,
            startAngle = 120f,
            sweepAngle = 60f,
            useCenter = true
        )
        drawArc(
            color = green,
            startAngle = 0f,
            sweepAngle = 120f,
            useCenter = true
        )
        drawArc(
            color = blue,
            startAngle = 280f,
            sweepAngle = 80f,
            useCenter = true
        )
        drawCircle(
            color = Color(0xFF1E293B),
            radius = radius * 0.55f,
            center = Offset(cx, cy)
        )
        drawRect(
            color = blue,
            topLeft = Offset(cx, cy - (radius * 0.22f)),
            size = androidx.compose.ui.geometry.Size(radius * 0.95f, radius * 0.44f)
        )
        drawCircle(
            color = Color(0xFF0F172A),
            radius = radius * 0.45f,
            center = Offset(cx, cy)
        )
    }
}

/**
 * Mandatory Login Gate Screen:
 * Displays when user is not logged in and not in Developer Mode.
 * Features mandatory Google Login (10 exits/day) AND prominent Developer Mode button (no login, unlimited exits).
 */
@Composable
fun MandatoryLoginGateScreen(
    authManager: AuthManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isLoading by authManager.isLoading.collectAsState()
    val errorMessage by authManager.errorMessage.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Shield Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(IndigoPrimary, CyanAccent)
                        ),
                        CircleShape
                    )
                    .border(2.dp, IndigoPrimary.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "FocusGuard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Mandatory Authentication",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanAccent
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please sign in with your Google account to access focus tools, distraction blocks, and flora gardens.",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF7F1D1D).copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, Color(0xFFDC2626)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Google Login Primary Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Google Account Login",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Text(
                                text = "10 EXITS / DAY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Standard Google sign-in provides cloud backup, focus streaks, and allows up to 10 emergency exits per day.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = {
                            if (activity != null) {
                                authManager.signInWithGoogle(activity)
                            } else {
                                authManager.quickSignIn("Google User", "user@gmail.com")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("mandatory_google_signin_btn")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.Black,
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GoogleLogoIcon(modifier = Modifier.size(20.dp))
                                Text(
                                    text = "Sign in with Google",
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // 1-Tap Google Quick Demo
                    OutlinedButton(
                        onClick = {
                            authManager.quickSignIn("Vinay", "pandagre.vinay@gmail.com")
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkCardBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mandatory_quick_demo_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            GoogleLogoIcon(modifier = Modifier.size(14.dp))
                            Text(
                                text = "1-Tap Google Demo (10 Exits/Day)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider OR DEVELOPER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DarkCardBorder)
                Text(
                    text = "OR BYPASS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = DarkCardBorder)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Prominent Developer Button Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF06281E)
                ),
                border = BorderStroke(1.5.dp, EmeraldSuccess.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { authManager.enableDeveloperMode() }
                    .testTag("mandatory_developer_mode_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(EmeraldSuccess.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = "Developer",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Developer Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "No Login Required",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldSuccess
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = EmeraldSuccess.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AllInclusive,
                                    contentDescription = "Unlimited",
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "UNLIMITED EXITS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }

                    Text(
                        text = "Click below to activate Developer Mode immediately. Bypasses mandatory login and gives unlimited session unlocks and emergency exits.",
                        fontSize = 12.sp,
                        color = Color(0xFFA7F3D0),
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = { authManager.enableDeveloperMode() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Color(0xFF022C22)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("developer_mode_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Enter Developer Mode (Unlimited Exits)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Exit Quota Chip for Top Bars & Headers
 */
@Composable
fun ExitQuotaChip(
    authManager: AuthManager,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isDev by authManager.isDeveloperMode.collectAsState()
    val remaining by authManager.dailyExitsRemaining.collectAsState()
    val user by authManager.currentUser.collectAsState()

    Surface(
        color = if (isDev) EmeraldSuccess.copy(alpha = 0.15f) else Color(0xFF131D33),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(
            1.dp,
            if (isDev) EmeraldSuccess.copy(alpha = 0.5f) else if (remaining <= 2) Color(0xFFEF4444).copy(alpha = 0.5f) else IndigoPrimary.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .clickable { onClick() }
            .testTag("exit_quota_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isDev) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Dev",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "DEV (∞ EXITS)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldSuccess
                )
            } else {
                GoogleLogoIcon(modifier = Modifier.size(12.dp))
                Text(
                    text = "$remaining/10 EXITS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (remaining <= 2) Color(0xFFF87171) else CyanAccent
                )
            }
        }
    }
}

/**
 * Account & Google Login Card for Settings/Security screen
 */
@Composable
fun GoogleAuthCard(
    authManager: AuthManager,
    modifier: Modifier = Modifier,
    onOpenDialog: () -> Unit = {}
) {
    val user by authManager.currentUser.collectAsState()
    val isDev by authManager.isDeveloperMode.collectAsState()
    val remaining by authManager.dailyExitsRemaining.collectAsState()
    val used by authManager.dailyExitsUsed.collectAsState()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(
            1.dp,
            if (isDev) EmeraldSuccess.copy(alpha = 0.4f) else IndigoPrimary.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isDev) EmeraldSuccess.copy(alpha = 0.15f)
                                else IndigoPrimary.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDev) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            GoogleLogoIcon(modifier = Modifier.size(18.dp))
                        }
                    }

                    Column {
                        Text(
                            text = if (isDev) "Developer Mode Active" else "Google Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isDev) "Unlimited exits • No restrictions" else "10 exits/day quota",
                            fontSize = 12.sp,
                            color = if (isDev) EmeraldSuccess else Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (isDev) EmeraldSuccess.copy(alpha = 0.15f) else Color(0xFF1E293B),
                    border = BorderStroke(
                        1.dp,
                        if (isDev) EmeraldSuccess.copy(alpha = 0.4f) else DarkCardBorder
                    )
                ) {
                    Text(
                        text = if (isDev) "DEV MODE" else "GOOGLE SYNC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDev) EmeraldSuccess else CyanAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            if (isDev) {
                // Developer Mode Active Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF06281E), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unlimited Emergency Exits (∞)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "No login required • No daily 10-exit cap",
                            fontSize = 11.sp,
                            color = Color(0xFFA7F3D0)
                        )
                    }

                    Button(
                        onClick = onOpenDialog,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF134E4A)),
                        modifier = Modifier.testTag("switch_to_google_btn")
                    ) {
                        Text("Switch", fontSize = 11.sp, color = Color.White)
                    }
                }
            } else if (user != null) {
                // Logged In Google Info
                val currentUser = user!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131D33), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!currentUser.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentUser.photoUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(IndigoPrimary, CyanAccent)
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser.displayName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = currentUser.displayName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = currentUser.email,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { authManager.signOut() },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DarkCardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF87171)
                            ),
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Text("Sign Out", fontSize = 11.sp)
                        }
                    }

                    // Daily Exits Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Today's Emergency Exits",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = "$remaining / 10 remaining ($used used)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remaining <= 2) Color(0xFFF87171) else CyanAccent
                            )
                        }
                    }
                }

                // Switch to Developer Mode button
                Button(
                    onClick = { authManager.enableDeveloperMode() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06281E)),
                    border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enable_dev_from_settings_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Switch to Developer Mode (Unlimited Exits)",
                            color = EmeraldSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Google Sign-in & Developer Switch Dialog
 */
@Composable
fun GoogleSignInDialog(
    authManager: AuthManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val user by authManager.currentUser.collectAsState()
    val isDev by authManager.isDeveloperMode.collectAsState()
    val isLoading by authManager.isLoading.collectAsState()
    val errorMessage by authManager.errorMessage.collectAsState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurface),
            color = DarkSurface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close button row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (isDev) EmeraldSuccess.copy(alpha = 0.15f) else IndigoPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isDev) EmeraldSuccess.copy(alpha = 0.3f) else IndigoPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = if (isDev) "DEV MODE (UNLIMITED)" else "GOOGLE LOGIN (10 EXITS/DAY)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDev) EmeraldSuccess else CyanAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                        .border(1.dp, DarkCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDev) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        GoogleLogoIcon(modifier = Modifier.size(28.dp))
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isDev) "Developer Mode" else if (user != null) "Google Account" else "Sign In with Google",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isDev)
                            "Developer Mode gives unlimited emergency exits with no daily limit."
                        else
                            "Google login limits emergency exits to 10 per day (resets at midnight).",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }

                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        if (activity != null) {
                            authManager.signInWithGoogle(activity) { success, _ ->
                                if (success) onDismiss()
                            }
                        } else {
                            authManager.quickSignIn("Google User", "user@gmail.com")
                            onDismiss()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoogleLogoIcon(modifier = Modifier.size(18.dp))
                        Text(
                            text = "Sign in with Google (10 Exits/Day)",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        authManager.enableDeveloperMode()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = Color(0xFF022C22)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dialog_developer_mode_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Developer Mode (Unlimited Exits)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
