package com.example.ui.screens.filemanager

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.crypto.VaultCrypto
import com.example.data.local.DownloadEntity
import com.example.data.local.FolderEntity
import com.example.data.local.PreferencesManager
import com.example.data.model.DownloadStatus
import com.example.ui.i18n.StringsProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    downloads: List<DownloadEntity>,
    vaultFiles: List<DownloadEntity>,
    folders: List<FolderEntity>,
    prefs: PreferencesManager,
    strings: StringsProvider.AppStrings,
    onCreateFolder: (name: String, isProtected: Boolean, password: String?) -> Unit,
    onMoveToFolder: (downloadId: Long, folderId: Long?) -> Unit,
    onMoveToVault: (downloadId: Long) -> Unit,
    onRestoreFromVault: (downloadId: Long) -> Unit,
    onDeleteFile: (downloadId: Long) -> Unit,
    onRenameFile: (downloadId: Long, newTitle: String) -> Unit,
    onPlayMedia: (filePath: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Videos, 2: Music, 3: Folders, 4: Vault
    var activeFolderId by remember { mutableStateOf<Long?>(null) }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var folderToUnlock by remember { mutableStateOf<FolderEntity?>(null) }
    var fileForActionMenu by remember { mutableStateOf<DownloadEntity?>(null) }
    var fileForMoveDialog by remember { mutableStateOf<DownloadEntity?>(null) }
    var fileForRenameDialog by remember { mutableStateOf<DownloadEntity?>(null) }
    var fileForDetailsDialog by remember { mutableStateOf<DownloadEntity?>(null) }

    // Vault in files tab state
    var isVaultUnlocked by remember { mutableStateOf(false) }
    var currentPinInput by remember { mutableStateOf("") }
    var setupStep by remember { mutableStateOf(0) }
    var tempNewPin by remember { mutableStateOf("") }
    var vaultErrorMessage by remember { mutableStateOf<String?>(null) }
    val storedPinHash = remember { prefs.getVaultPinHash() }
    val isFirstTimePin = storedPinHash == null

    // Filter downloaded completed files
    val completedFiles = remember(downloads, selectedTab, activeFolderId) {
        val completed = downloads.filter { it.status == DownloadStatus.COMPLETED && !it.isPrivate }
        if (activeFolderId != null) {
            completed.filter { it.folderId == activeFolderId }
        } else {
            when (selectedTab) {
                1 -> completed.filter { !it.format.isAudio }
                2 -> completed.filter { it.format.isAudio }
                else -> completed
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (activeFolderId != null) {
                        val folder = folders.find { it.id == activeFolderId }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { activeFolderId = null }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Text(folder?.name ?: "Folder", fontWeight = FontWeight.Bold)
                        }
                    } else if (selectedTab == 4) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.privateVaultTitle, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(strings.navFiles, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (selectedTab == 3 && activeFolderId == null) {
                        IconButton(onClick = { showNewFolderDialog = true }) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder")
                        }
                    } else if (selectedTab == 4 && isVaultUnlocked) {
                        IconButton(onClick = {
                            isVaultUnlocked = false
                            currentPinInput = ""
                        }) {
                            Icon(Icons.Default.LockReset, contentDescription = "Lock Vault")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeFolderId == null) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(strings.tabAll) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(strings.tabVideos) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(strings.tabMusic) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("${strings.tabFolders} (${folders.size})") }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Vault", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                selectedTab == 4 -> {
                    // Integrated Private Vault View
                    if (!isVaultUnlocked) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            val promptText = if (isFirstTimePin) {
                                if (setupStep == 0) strings.setPin else strings.confirmPin
                            } else {
                                strings.enterPin
                            }

                            Text(
                                text = promptText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 4) {
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

                            vaultErrorMessage?.let { err ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            val keypad = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("", "0", "DEL")
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                for (row in keypad) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (key in row) {
                                            if (key.isEmpty()) {
                                                Spacer(modifier = Modifier.size(60.dp))
                                            } else {
                                                Surface(
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            vaultErrorMessage = null
                                                            if (key == "DEL") {
                                                                if (currentPinInput.isNotEmpty()) {
                                                                    currentPinInput = currentPinInput.dropLast(1)
                                                                }
                                                            } else if (currentPinInput.length < 4) {
                                                                currentPinInput += key
                                                                if (currentPinInput.length == 4) {
                                                                    if (isFirstTimePin) {
                                                                        if (setupStep == 0) {
                                                                            tempNewPin = currentPinInput
                                                                            currentPinInput = ""
                                                                            setupStep = 1
                                                                        } else {
                                                                            if (currentPinInput == tempNewPin) {
                                                                                prefs.setVaultPinHash(VaultCrypto.hashPin(currentPinInput))
                                                                                isVaultUnlocked = true
                                                                            } else {
                                                                                vaultErrorMessage = "PINs do not match. Try again."
                                                                                currentPinInput = ""
                                                                                setupStep = 0
                                                                            }
                                                                        }
                                                                    } else {
                                                                        if (storedPinHash != null && VaultCrypto.verifyPin(currentPinInput, storedPinHash)) {
                                                                            isVaultUnlocked = true
                                                                        } else {
                                                                            vaultErrorMessage = strings.pinIncorrect
                                                                            currentPinInput = ""
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        },
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        if (key == "DEL") {
                                                            Icon(Icons.Default.Backspace, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                                                        } else {
                                                            Text(text = key, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Unlocked Vault Files
                        Column(modifier = Modifier.fillMaxSize()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Private Vault: Files are encrypted with AES-256 and isolated from regular device galleries.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            if (vaultFiles.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        strings.vaultEmpty,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(vaultFiles) { file ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onPlayMedia(file.filePath, file.title) },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(32.dp)
                                                )
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

                selectedTab == 3 && activeFolderId == null -> {
                    // Folders list
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folders) { folder ->
                            val count = downloads.count { it.folderId == folder.id && it.status == DownloadStatus.COMPLETED }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (folder.isProtected) {
                                            folderToUnlock = folder
                                        } else {
                                            activeFolderId = folder.id
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (folder.isProtected) Icons.Default.FolderSpecial else Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (folder.isProtected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(folder.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text("$count files", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    if (folder.isProtected) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "Password Protected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Media files list
                    if (completedFiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "No media files found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(completedFiles) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onPlayMedia(item.filePath, item.title) },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (item.format.isAudio) Icons.Default.Audiotrack else Icons.Default.Videocam,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${formatBytes(item.fileSize)} • ${item.format.extension.uppercase()} • ${item.quality}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(onClick = { fileForActionMenu = item }) {
                                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
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

    // New Folder Dialog
    if (showNewFolderDialog) {
        var folderName by remember { mutableStateOf("") }
        var isProtected by remember { mutableStateOf(false) }
        var folderPin by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text(strings.newFolder) },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text(strings.folderName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.protectFolderWithPin, fontSize = 13.sp)
                        Switch(checked = isProtected, onCheckedChange = { isProtected = it })
                    }
                    if (isProtected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = folderPin,
                            onValueChange = { folderPin = it.take(4) },
                            label = { Text("4-digit PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = folderName.isNotBlank() && (!isProtected || folderPin.length == 4),
                    onClick = {
                        val hash = if (isProtected) VaultCrypto.hashPin(folderPin) else null
                        onCreateFolder(folderName.trim(), isProtected, hash)
                        showNewFolderDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Unlock Protected Folder Dialog
    folderToUnlock?.let { folder ->
        var enteredPin by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { folderToUnlock = null },
            title = { Text("Protected Folder: ${folder.name}") },
            text = {
                Column {
                    Text("Enter PIN to unlock this folder:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it.take(4)
                            isError = false
                        },
                        isError = isError,
                        label = { Text("PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text(strings.pinIncorrect, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (folder.passwordHash != null && VaultCrypto.verifyPin(enteredPin, folder.passwordHash)) {
                        activeFolderId = folder.id
                        folderToUnlock = null
                    } else {
                        isError = true
                    }
                }) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToUnlock = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // File Action Bottom Sheet
    fileForActionMenu?.let { file ->
        ModalBottomSheet(
            onDismissRequest = { fileForActionMenu = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Text(file.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(16.dp))

                ListItem(
                    headlineContent = { Text(strings.play) },
                    leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    modifier = Modifier.clickable {
                        fileForActionMenu = null
                        onPlayMedia(file.filePath, file.title)
                    }
                )

                ListItem(
                    headlineContent = { Text("Move to Folder") },
                    leadingContent = { Icon(Icons.Default.DriveFileMove, contentDescription = null) },
                    modifier = Modifier.clickable {
                        fileForActionMenu = null
                        fileForMoveDialog = file
                    }
                )

                ListItem(
                    headlineContent = { Text(strings.moveToVault) },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.clickable {
                        fileForActionMenu = null
                        onMoveToVault(file.id)
                    }
                )

                ListItem(
                    headlineContent = { Text(strings.rename) },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.clickable {
                        fileForActionMenu = null
                        fileForRenameDialog = file
                    }
                )

                ListItem(
                    headlineContent = { Text(strings.share) },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        fileForActionMenu = null
                        shareMediaFile(context, File(file.filePath), file.title)
                    }
                )

                ListItem(
                    headlineContent = { Text(strings.details) },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                    modifier = Modifier.clickable {
                        fileForActionMenu = null
                        fileForDetailsDialog = file
                    }
                )

                ListItem(
                    headlineContent = { Text(strings.delete, color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        fileForActionMenu = null
                        onDeleteFile(file.id)
                    }
                )
            }
        }
    }

    // Move to folder dialog
    fileForMoveDialog?.let { file ->
        AlertDialog(
            onDismissRequest = { fileForMoveDialog = null },
            title = { Text("Move to Folder") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("None (Root Downloads)") },
                        modifier = Modifier.clickable {
                            onMoveToFolder(file.id, null)
                            fileForMoveDialog = null
                        }
                    )
                    folders.forEach { f ->
                        ListItem(
                            headlineContent = { Text(f.name) },
                            leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                            modifier = Modifier.clickable {
                                onMoveToFolder(file.id, f.id)
                                fileForMoveDialog = null
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { fileForMoveDialog = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Rename dialog
    fileForRenameDialog?.let { file ->
        var newTitle by remember { mutableStateOf(file.title) }
        AlertDialog(
            onDismissRequest = { fileForRenameDialog = null },
            title = { Text(strings.rename) },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    enabled = newTitle.isNotBlank(),
                    onClick = {
                        onRenameFile(file.id, newTitle.trim())
                        fileForRenameDialog = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileForRenameDialog = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Details dialog
    fileForDetailsDialog?.let { file ->
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.createdAt))
        AlertDialog(
            onDismissRequest = { fileForDetailsDialog = null },
            title = { Text(strings.details) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name: ${file.title}", fontWeight = FontWeight.SemiBold)
                    Text("Format: ${file.format.extension.uppercase()} (${file.format.mimeType})")
                    Text("Quality: ${file.quality}")
                    Text("Size: ${formatBytes(file.fileSize)}")
                    Text("Date: $dateStr")
                    Text("Path: ${file.filePath}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { fileForDetailsDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun shareMediaFile(context: Context, file: File, title: String) {
    try {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.name.endsWith(".mp3") || file.name.endsWith(".m4a")) "audio/*" else "video/*"
            putExtra(Intent.EXTRA_TEXT, "Shared from SnapLoad: $title")
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Media"))
    } catch (_: Exception) {}
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups >= units.size) digitGroups = units.size - 1
    return "%.1f %s".format(bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
