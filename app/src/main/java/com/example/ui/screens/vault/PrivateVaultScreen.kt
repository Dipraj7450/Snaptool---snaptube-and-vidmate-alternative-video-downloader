package com.example.ui.screens.vault

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.crypto.VaultCrypto
import com.example.data.local.DownloadEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.VaultLockType
import com.example.ui.i18n.StringsProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateVaultScreen(
    vaultFiles: List<DownloadEntity>,
    prefs: PreferencesManager,
    strings: StringsProvider.AppStrings,
    onPlayMedia: (filePath: String, title: String) -> Unit,
    onRestoreFromVault: (downloadId: Long) -> Unit,
    onDeleteFile: (downloadId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isUnlocked by remember { mutableStateOf(false) }
    val vaultLockType by prefs.vaultLockType.collectAsState()

    var activeLockType by remember(vaultLockType) { mutableStateOf(vaultLockType) }
    var currentPinInput by remember { mutableStateOf("") }
    var currentPatternInput by remember { mutableStateOf<List<Int>>(emptyList()) }

    var setupStep by remember { mutableStateOf(0) } // 0: enter/draw, 1: confirm
    var tempNewPin by remember { mutableStateOf("") }
    var tempNewPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successNotice by remember { mutableStateOf<String?>(null) }

    // Dialogs
    var showForgotDialog by remember { mutableStateOf(false) }
    var recoveryCodeInput by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }
    var showSecuritySettingsDialog by remember { mutableStateOf(false) }

    val storedPinHash = prefs.getVaultPinHash()
    val storedPatternHash = prefs.getVaultPatternHash()
    val isVaultConfigured = storedPinHash != null || storedPatternHash != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(strings.privateVaultTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                if (isUnlocked) "Unlocked (${activeLockType.label})" else "Protected Vault",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (isUnlocked) {
                        IconButton(onClick = { showSecuritySettingsDialog = true }) {
                            Icon(Icons.Default.SettingsSuggest, contentDescription = "Security Options")
                        }
                        IconButton(onClick = {
                            isUnlocked = false
                            currentPinInput = ""
                            currentPatternInput = emptyList()
                            setupStep = 0
                        }) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock Vault", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isUnlocked) {
                // Locked State UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Shield Icon
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isVaultConfigured) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Setup Mode Selector (if configuring for the first time)
                        if (!isVaultConfigured) {
                            Text(
                                text = "Choose Security Lock Type",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                listOf(VaultLockType.PIN_4, VaultLockType.PIN_6, VaultLockType.PATTERN).forEach { type ->
                                    FilterChip(
                                        selected = activeLockType == type,
                                        onClick = {
                                            activeLockType = type
                                            currentPinInput = ""
                                            currentPatternInput = emptyList()
                                            setupStep = 0
                                            errorMessage = null
                                        },
                                        label = { Text(type.label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        // Instructional prompt
                        val promptText = if (!isVaultConfigured) {
                            if (activeLockType == VaultLockType.PATTERN) {
                                if (setupStep == 0) "Draw your new unlock pattern" else "Draw pattern again to confirm"
                            } else {
                                if (setupStep == 0) "Set your ${activeLockType.pinLength}-digit PIN" else "Confirm your ${activeLockType.pinLength}-digit PIN"
                            }
                        } else {
                            if (activeLockType == VaultLockType.PATTERN) "Draw Pattern to Unlock"
                            else "Enter ${activeLockType.pinLength}-Digit PIN"
                        }

                        Text(
                            text = promptText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        errorMessage?.let { err ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }

                        successNotice?.let { msg ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(msg, color = Color(0xFF00E676), fontSize = 12.sp)
                        }
                    }

                    // Lock Mechanism Area: Pattern Grid OR PIN Dots & Keypad
                    if (activeLockType == VaultLockType.PATTERN) {
                        PatternLockView(
                            pattern = currentPatternInput,
                            onPatternChange = { updated ->
                                currentPatternInput = updated
                            },
                            onPatternComplete = { finalPattern ->
                                if (finalPattern.size < 4) {
                                    errorMessage = "Pattern must connect at least 4 dots"
                                    currentPatternInput = emptyList()
                                    return@PatternLockView
                                }

                                val patternString = finalPattern.joinToString(",")
                                if (!isVaultConfigured) {
                                    if (setupStep == 0) {
                                        tempNewPattern = finalPattern
                                        currentPatternInput = emptyList()
                                        setupStep = 1
                                        errorMessage = null
                                    } else {
                                        if (finalPattern == tempNewPattern) {
                                            prefs.setVaultPatternHash(VaultCrypto.hashPin(patternString))
                                            prefs.setVaultLockType(VaultLockType.PATTERN)
                                            isUnlocked = true
                                            currentPatternInput = emptyList()
                                            setupStep = 0
                                        } else {
                                            errorMessage = "Patterns did not match. Try again."
                                            currentPatternInput = emptyList()
                                            setupStep = 0
                                        }
                                    }
                                } else {
                                    if (storedPatternHash != null && VaultCrypto.verifyPin(patternString, storedPatternHash)) {
                                        isUnlocked = true
                                        currentPatternInput = emptyList()
                                        errorMessage = null
                                    } else {
                                        errorMessage = "Incorrect pattern. Try again."
                                        currentPatternInput = emptyList()
                                    }
                                }
                            }
                        )
                    } else {
                        // Numeric PIN view (4 or 6 digits)
                        val expectedLen = activeLockType.pinLength
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // PIN Dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 12.dp)
                            ) {
                                for (i in 0 until expectedLen) {
                                    val isFilled = i < currentPinInput.length
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isFilled) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Keypad
                            val keypad = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("", "0", "DEL")
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                for (row in keypad) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (key in row) {
                                            if (key.isEmpty()) {
                                                Spacer(modifier = Modifier.size(62.dp))
                                            } else {
                                                Surface(
                                                    modifier = Modifier
                                                        .size(62.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            errorMessage = null
                                                            if (key == "DEL") {
                                                                if (currentPinInput.isNotEmpty()) {
                                                                    currentPinInput = currentPinInput.dropLast(1)
                                                                }
                                                            } else if (currentPinInput.length < expectedLen) {
                                                                currentPinInput += key
                                                                if (currentPinInput.length == expectedLen) {
                                                                    if (!isVaultConfigured) {
                                                                        if (setupStep == 0) {
                                                                            tempNewPin = currentPinInput
                                                                            currentPinInput = ""
                                                                            setupStep = 1
                                                                        } else {
                                                                            if (currentPinInput == tempNewPin) {
                                                                                prefs.setVaultPinHash(VaultCrypto.hashPin(currentPinInput))
                                                                                prefs.setVaultLockType(activeLockType)
                                                                                isUnlocked = true
                                                                                setupStep = 0
                                                                            } else {
                                                                                errorMessage = "PINs do not match. Try again."
                                                                                currentPinInput = ""
                                                                                setupStep = 0
                                                                            }
                                                                        }
                                                                    } else {
                                                                        if (storedPinHash != null && VaultCrypto.verifyPin(currentPinInput, storedPinHash)) {
                                                                            isUnlocked = true
                                                                            currentPinInput = ""
                                                                        } else {
                                                                            errorMessage = strings.pinIncorrect
                                                                            currentPinInput = ""
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        },
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                                    tonalElevation = 2.dp
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        if (key == "DEL") {
                                                            Icon(Icons.Default.Backspace, contentDescription = "Delete")
                                                        } else {
                                                            Text(
                                                                text = key,
                                                                fontSize = 22.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Recovery Master Code Prompt
                    TextButton(
                        onClick = {
                            recoveryCodeInput = ""
                            recoveryError = null
                            showForgotDialog = true
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Forgot Password / Reset Vault?",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // Vault Unlocked Content
                Column(modifier = Modifier.fillMaxSize()) {
                    // Security Info Ribbon
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "AES-256 Encrypted Private Sandbox",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "All saved files are completely hidden from gallery & public explorer.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (vaultFiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    strings.vaultEmpty,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Lock videos or files from the Downloads tab by selecting 'Move to Vault'",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(vaultFiles) { file ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onPlayMedia(file.filePath, file.title) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                file.title,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${file.quality} • ${file.format.extension.uppercase()} (Encrypted)",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(onClick = { onRestoreFromVault(file.id) }) {
                                            Icon(
                                                Icons.Default.LockOpen,
                                                contentDescription = "Restore to Downloads",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        IconButton(onClick = { onDeleteFile(file.id) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Forgot Password / Master Recovery Dialog (Requiring strictly "7450")
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            icon = {
                Icon(
                    Icons.Default.LockReset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("Reset Vault Password", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Enter the master recovery code to immediately unlock and reset vault credentials:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = recoveryCodeInput,
                        onValueChange = {
                            recoveryCodeInput = it
                            recoveryError = null
                        },
                        label = { Text("Master Recovery Code") },
                        placeholder = { Text("Enter 4-digit code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    recoveryError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val entered = recoveryCodeInput.trim()
                        if (entered == "7450") {
                            prefs.resetVaultWithRecoveryCode("7450")
                            isUnlocked = true
                            showForgotDialog = false
                            currentPinInput = ""
                            currentPatternInput = emptyList()
                            successNotice = "Vault unlocked and reset with code 7450! You may now configure a new PIN or pattern."
                        } else {
                            recoveryError = "Invalid recovery code. Please enter the correct code."
                        }
                    }
                ) {
                    Text("Unlock & Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Security Options Dialog when Unlocked
    if (showSecuritySettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSecuritySettingsDialog = false },
            title = { Text("Vault Security Options", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select your preferred vault lock type:", fontSize = 13.sp)
                    listOf(
                        VaultLockType.PIN_4,
                        VaultLockType.PIN_6,
                        VaultLockType.PATTERN
                    ).forEach { type ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    prefs.setVaultLockType(type)
                                    activeLockType = type
                                    showSecuritySettingsDialog = false
                                    isUnlocked = false
                                    setupStep = 0
                                    currentPinInput = ""
                                    currentPatternInput = emptyList()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (activeLockType == type) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (type == VaultLockType.PATTERN) Icons.Default.Gesture else Icons.Default.Pin,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(type.label, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (type == VaultLockType.PATTERN) "Connect dots on 3x3 grid" else "${type.pinLength} digit security code",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecuritySettingsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Interactive 3x3 Pattern Lock View
 */
@Composable
fun PatternLockView(
    pattern: List<Int>,
    onPatternChange: (List<Int>) -> Unit,
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridNodeCount = 9
    val nodePositions = remember { mutableStateMapOf<Int, Offset>() }
    var currentDragPoint by remember { mutableStateOf<Offset?>(null) }
    var containerOffset by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(280.dp)
                .onGloballyPositioned { coordinates ->
                    containerOffset = coordinates.positionInParent()
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentDragPoint = offset
                            val hitNode = findHitNode(offset, nodePositions)
                            if (hitNode != null) {
                                onPatternChange(listOf(hitNode))
                            } else {
                                onPatternChange(emptyList())
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val pos = change.position
                            currentDragPoint = pos
                            val hit = findHitNode(pos, nodePositions)
                            if (hit != null && !pattern.contains(hit)) {
                                val next = pattern + hit
                                onPatternChange(next)
                            }
                        },
                        onDragEnd = {
                            currentDragPoint = null
                            if (pattern.isNotEmpty()) {
                                onPatternComplete(pattern)
                            }
                        },
                        onDragCancel = {
                            currentDragPoint = null
                            onPatternChange(emptyList())
                        }
                    )
                }
        ) {
            // Draw connecting lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (pattern.isNotEmpty()) {
                    for (i in 0 until pattern.size - 1) {
                        val start = nodePositions[pattern[i]]
                        val end = nodePositions[pattern[i + 1]]
                        if (start != null && end != null) {
                            drawLine(
                                color = primaryColor,
                                start = start,
                                end = end,
                                strokeWidth = 10f,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                    val lastNode = nodePositions[pattern.last()]
                    val drag = currentDragPoint
                    if (lastNode != null && drag != null) {
                        drawLine(
                            color = primaryColor.copy(alpha = 0.6f),
                            start = lastNode,
                            end = drag,
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 3x3 Grid of Circular Nodes
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (col in 0..2) {
                            val index = row * 3 + col
                            val isSelected = pattern.contains(index)
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .onGloballyPositioned { coords ->
                                        val parentPos = coords.positionInParent()
                                        val center = Offset(
                                            parentPos.x + coords.size.width / 2f,
                                            parentPos.y + coords.size.height / 2f
                                        )
                                        nodePositions[index] = center
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) primaryColor.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 20.dp else 14.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun findHitNode(touch: Offset, nodePositions: Map<Int, Offset>, hitRadius: Float = 60f): Int? {
    for ((index, pos) in nodePositions) {
        val dx = touch.x - pos.x
        val dy = touch.y - pos.y
        if ((dx * dx + dy * dy) <= hitRadius * hitRadius) {
            return index
        }
    }
    return null
}
