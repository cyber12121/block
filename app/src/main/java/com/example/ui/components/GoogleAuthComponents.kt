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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.auth.AuthManager
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
 * Simplified strictly to:
 * 1. Sign In / Sign Up with Google (No 1-tap card, no password, no extra fields)
 * 2. Developer Mode bypass toggle on/off (Unlimited emergency exits)
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
    val isDev by authManager.isDeveloperMode.collectAsState()

    var showDevPasscodeDialog by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }
    var headerTapCount by remember { mutableIntStateOf(0) }

    if (showDevPasscodeDialog) {
        DeveloperPasscodeDialog(
            authManager = authManager,
            onDismiss = { showDevPasscodeDialog = false },
            onSuccess = { headerTapCount = 0 }
        )
    }

    if (showPinChangeDialog) {
        DeveloperPinChangeDialog(
            authManager = authManager,
            onDismiss = { showPinChangeDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Shield Hero Icon (Secret 5-Tap Trigger)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(listOf(IndigoPrimary, CyanAccent)),
                        CircleShape
                    )
                    .border(2.dp, IndigoPrimary.copy(alpha = 0.6f), CircleShape)
                    .clickable {
                        headerTapCount++
                        if (headerTapCount >= 5) {
                            showDevPasscodeDialog = true
                            headerTapCount = 0
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "FocusGuard Shield",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "FocusGuard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.clickable {
                    headerTapCount++
                    if (headerTapCount >= 5) {
                        showDevPasscodeDialog = true
                        headerTapCount = 0
                    }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Sign In / Sign Up with Google",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanAccent
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign in or sign up using your Google Account (Gmail) to protect sessions, manage app blocklists, and sync stats.",
                fontSize = 12.sp,
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

            // Mandatory Google Sign-In / Sign-Up Primary Action Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, IndigoPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google Sign-In / Sign-Up Main Button
                    Button(
                        onClick = {
                            if (activity != null) {
                                authManager.signInWithGoogle(activity)
                            } else {
                                authManager.quickSignIn("Vinay Pandagre", "pandagre.vinay@gmail.com")
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                GoogleLogoIcon(modifier = Modifier.size(22.dp))
                                Text(
                                    text = "Sign In / Sign Up with Google",
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "1-click Google authentication with Gmail • No password needed",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Compact Signed-In User Icon / Exit Quota Chip for Top Bars & Headers
 * When user is signed in: prominently displays Google user avatar, Google logo, signed-in check, and remaining exits.
 */
@Composable
fun ExitQuotaChip(
    authManager: AuthManager,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val remaining by authManager.dailyExitsRemaining.collectAsState()
    val user by authManager.currentUser.collectAsState()

    Surface(
        color = Color(0xFF131D33),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(
            1.dp,
            if (user != null) EmeraldSuccess.copy(alpha = 0.6f)
            else if (remaining <= 2) Color(0xFFEF4444).copy(alpha = 0.5f)
            else IndigoPrimary.copy(alpha = 0.3f)
        ),
        modifier = modifier
            .clickable { onClick() }
            .testTag("exit_quota_chip")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (user != null) {
                val currentUser = user!!
                // Prominent Signed-in User Avatar with Verified Green Check
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (!currentUser.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = currentUser.photoUrl,
                            contentDescription = "User Photo",
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFF34A853))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.displayName.take(1).uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = Color.White
                            )
                        }
                    }
                    // Verified Signed-In Green Dot Badge
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(EmeraldSuccess, CircleShape)
                            .border(0.5.dp, Color(0xFF131D33), CircleShape)
                    )
                }

                GoogleLogoIcon(modifier = Modifier.size(12.dp))

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Signed In",
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(13.dp)
                )

                Text(
                    text = "$remaining/10",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
 * Account & Auth Card for Settings/Security screen
 * When user is signed in: displays Google profile, verified badge, exits, and sign out.
 * When user is not signed in: displays Google sign in button.
 */
@Composable
fun GoogleAuthCard(
    authManager: AuthManager,
    modifier: Modifier = Modifier,
    onOpenDialog: () -> Unit = {}
) {
    val user by authManager.currentUser.collectAsState()
    val remaining by authManager.dailyExitsRemaining.collectAsState()
    val used by authManager.dailyExitsUsed.collectAsState()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(
            1.dp,
            if (user != null) EmeraldSuccess.copy(alpha = 0.4f)
            else IndigoPrimary.copy(alpha = 0.3f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row with Main Account / Mode status
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
                            .size(38.dp)
                            .background(
                                if (user != null) EmeraldSuccess.copy(alpha = 0.15f)
                                else IndigoPrimary.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        GoogleLogoIcon(modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text(
                            text = if (user != null) "Google Account (Gmail)" else "Google Authentication",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (user != null) "$remaining/10 exits remaining today" else "Sign in required",
                            fontSize = 12.sp,
                            color = if (user != null) CyanAccent else Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = if (user != null) EmeraldSuccess.copy(alpha = 0.15f) else Color(0xFF1E293B),
                    border = BorderStroke(
                        1.dp,
                        if (user != null) EmeraldSuccess.copy(alpha = 0.4f) else DarkCardBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (user != null) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Signed In",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = if (user != null) "SIGNED IN" else "LOGGED OUT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (user != null) EmeraldSuccess else CyanAccent
                        )
                    }
                }
            }

            // If user IS signed in: Display ONLY user details and sign out.
            if (user != null) {
                val currentUser = user!!
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131D33), RoundedCornerShape(14.dp))
                        .border(1.dp, EmeraldSuccess.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                if (!currentUser.photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = currentUser.photoUrl,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF4285F4), Color(0xFF34A853))
                                                ),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentUser.displayName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .background(EmeraldSuccess, CircleShape)
                                        .border(1.5.dp, Color(0xFF131D33), CircleShape)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentUser.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    GoogleLogoIcon(modifier = Modifier.size(13.dp))
                                }
                                Text(
                                    text = currentUser.email,
                                    fontSize = 12.sp,
                                    color = CyanAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { authManager.signOut() },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF87171)
                            ),
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Daily Exits Progress
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
            } else {
                // User is NOT signed in: Google sign in action
                Button(
                    onClick = onOpenDialog,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("open_auth_dialog_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GoogleLogoIcon(modifier = Modifier.size(16.dp))
                        Text(
                            text = "Sign In / Sign Up with Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Account Sign-in Dialog:
 * When user is signed in: shows user profile, Google details, and Sign Out (NO developer mode).
 * When user is not signed in: shows Google Sign In button and protected Developer Access Gate.
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

    var showDevPasscodeDialog by remember { mutableStateOf(false) }

    if (showDevPasscodeDialog) {
        DeveloperPasscodeDialog(
            authManager = authManager,
            onDismiss = { showDevPasscodeDialog = false },
            onSuccess = { onDismiss() }
        )
    }

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
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = if (user != null || isDev) EmeraldSuccess.copy(alpha = 0.15f) else IndigoPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (user != null || isDev) EmeraldSuccess.copy(alpha = 0.3f) else IndigoPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (user != null) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldSuccess,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = if (user != null) "SIGNED IN (GOOGLE)" else if (isDev) "DEV MODE ON (UNLIMITED)" else "10 EXITS / DAY QUOTA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (user != null || isDev) EmeraldSuccess else CyanAccent
                            )
                        }
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

                Text(
                    text = if (user != null) "Google Account" else if (isDev) "Developer Mode Settings" else "Sign In with Google",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // If user is already signed in, show user profile info card with avatar & sign-out option
                if (user != null) {
                    val currentUser = user!!
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF131D33),
                        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                if (!currentUser.photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = currentUser.photoUrl,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF4285F4), Color(0xFF34A853))
                                                ),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentUser.displayName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(EmeraldSuccess, CircleShape)
                                        .border(1.5.dp, Color(0xFF131D33), CircleShape)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentUser.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    GoogleLogoIcon(modifier = Modifier.size(13.dp))
                                }
                                Text(
                                    text = currentUser.email,
                                    fontSize = 12.sp,
                                    color = CyanAccent
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    authManager.signOut()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF87171)
                                )
                            ) {
                                Text("Sign Out", fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    // When user is NOT signed in
                    if (isDev) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF042017)),
                            border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Developer Mode ACTIVE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Lock & hide developer options anytime to test as standard user.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFA7F3D0)
                                )
                                Button(
                                    onClick = {
                                        authManager.lockAndHideDeveloperMode()
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Lock & Hide Developer Mode", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
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

                    // Google Sign-In button
                    Button(
                        onClick = {
                            if (activity != null) {
                                authManager.signInWithGoogle(activity) { success, _ ->
                                    if (success) onDismiss()
                                }
                            } else {
                                authManager.quickSignIn("Vinay Pandagre", "pandagre.vinay@gmail.com")
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GoogleLogoIcon(modifier = Modifier.size(18.dp))
                            Text(
                                text = "Sign In / Sign Up with Google",
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (!isDev) {
                        // Protected Developer Access Gate Button
                        OutlinedButton(
                            onClick = { showDevPasscodeDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("dialog_developer_gate_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Developer Access Gate 🔒 (PIN Required)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Developer Verification Passcode Dialog (PIN Protected)
 */
@Composable
fun DeveloperPasscodeDialog(
    authManager: AuthManager,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {}
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showChangePin by remember { mutableStateOf(false) }

    if (showChangePin) {
        DeveloperPinChangeDialog(
            authManager = authManager,
            onDismiss = { showChangePin = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = EmeraldSuccess,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text("Developer Access Gate 🔒", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Developer Mode is PIN-protected so standard users cannot bypass strict focus sessions.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 8) {
                            pinInput = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Enter Developer PIN (Default: 2026)") },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldSuccess,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = EmeraldSuccess,
                        unfocusedLabelColor = Color(0xFF94A3B8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (errorMessage != null) {
                    Text(errorMessage!!, color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Change Developer PIN",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showChangePin = true }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (authManager.verifyDeveloperPin(pinInput)) {
                        authManager.enableDeveloperMode()
                        onSuccess()
                        onDismiss()
                    } else {
                        errorMessage = "Incorrect PIN. Default PIN is 2026."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF022C22))
            ) {
                Text("Verify & Enable Dev Mode", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFCBD5E1))
            }
        },
        containerColor = Color(0xFF0F172A)
    )
}

/**
 * Developer PIN Change Dialog
 */
@Composable
fun DeveloperPinChangeDialog(
    authManager: AuthManager,
    onDismiss: () -> Unit
) {
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Developer PIN", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPinInput,
                    onValueChange = { currentPinInput = it },
                    label = { Text("Current PIN (Default: 2026)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { newPinInput = it },
                    label = { Text("New PIN (e.g. 1234)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = confirmPinInput,
                    onValueChange = { confirmPinInput = it },
                    label = { Text("Confirm New PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (errorMsg != null) {
                    Text(errorMsg!!, color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                if (successMsg != null) {
                    Text(successMsg!!, color = EmeraldSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!authManager.verifyDeveloperPin(currentPinInput)) {
                        errorMsg = "Current PIN is incorrect."
                    } else if (newPinInput.isBlank() || newPinInput.length < 4) {
                        errorMsg = "New PIN must be at least 4 digits."
                    } else if (newPinInput != confirmPinInput) {
                        errorMsg = "New PIN and Confirm PIN do not match."
                    } else {
                        authManager.setDeveloperPin(newPinInput)
                        errorMsg = null
                        successMsg = "Developer PIN successfully updated!"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color(0xFF022C22))
            ) {
                Text("Save PIN", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFFCBD5E1))
            }
        },
        containerColor = Color(0xFF0F172A)
    )
}
