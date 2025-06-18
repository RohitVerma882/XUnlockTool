package com.xunlocktool.ui.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.xunlocktool.Constants
import com.xunlocktool.R

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockScreen(viewModel: UnlockViewModel, onLoggedOut: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val regions = Constants.regions.map { it.name }
    val hosts = Constants.regions.map { it.host }
    var expandedRegion by remember { mutableStateOf(false) }
    var selectedRegion by rememberSaveable { mutableStateOf(regions[0]) }
    var host by rememberSaveable { mutableStateOf(hosts[0]) }
    var product by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }

    val currentOnLoggedOut by rememberUpdatedState(onLoggedOut)
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val unlockState by viewModel.unlockState.collectAsStateWithLifecycle()

    LaunchedEffect(loginState) {
        if (!loginState.isLoggedIn) {
            currentOnLoggedOut()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal
                        )
                    )
            ) {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.SemiBold
                        )
                    },

                    navigationIcon = {
                        IconButton(
                            enabled = !unlockState.isRunning,
                            onClick = viewModel::clearAuth
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_logout),
                                contentDescription = null
                            )
                        }
                    },

                    actions = {
                        IconButton(
                            enabled = product.isNotEmpty() && token.isNotEmpty(),
                            onClick = {
                                if (unlockState.isRunning) {
                                    viewModel.stopUnlock()
                                } else {
                                    viewModel.startUnlock(
                                        loginState.auth,
                                        host,
                                        product,
                                        token
                                    )
                                }
                            }) {
                            Icon(
                                imageVector = if (unlockState.isRunning) Icons.Outlined.Clear else Icons.Outlined.PlayArrow,
                                contentDescription = null
                            )
                        }

                        IconButton(enabled = unlockState.token != null, onClick = {
                            coroutineScope.launch {
                                clipboardManager.setText(AnnotatedString(unlockState.token!!))
                                snackbarHostState.showSnackbar("Copied unlock token to clipboard")
                            }
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_content_copy),
                                contentDescription = null
                            )
                        }

                        IconButton(onClick = {
                            coroutineScope.launch {
                                uriHandler.openUri(Constants.GITHUB_SOURCE_CODE_URL)
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null
                            )
                        }
                    })

                if (unlockState.isRunning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExposedDropdownMenuBox(
                    modifier = Modifier.fillMaxWidth(),
                    expanded = expandedRegion,
                    onExpandedChange = { expandedRegion = it }
                ) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        value = selectedRegion,
                        onValueChange = { },
                        readOnly = true,
                        singleLine = true,
                        label = { Text("Region") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRegion) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedRegion,
                        onDismissRequest = { expandedRegion = false }) {
                        regions.forEachIndexed { index, region ->
                            DropdownMenuItem(
                                enabled = !unlockState.isRunning,
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                text = { Text(region, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    selectedRegion = region
                                    host = hosts[index]
                                    expandedRegion = false
                                }
                            )
                        }
                    }
                }

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !unlockState.isRunning,
                    value = product,
                    onValueChange = { product = it },
                    singleLine = true,
                    label = { Text("Product") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = product.isEmpty(),
                    colors = TextFieldDefaults.colors()
                )

                TextField(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !unlockState.isRunning,
                    value = token,
                    onValueChange = { token = it },
                    maxLines = 4,
                    label = { Text("Token") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    isError = token.isEmpty(),
                    colors = TextFieldDefaults.colors()
                )

                unlockState.output?.let { output ->
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        text = output,
                    )
                }
            }
        }
    }
}