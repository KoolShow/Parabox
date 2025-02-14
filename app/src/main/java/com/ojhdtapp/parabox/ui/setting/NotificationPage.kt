package com.ojhdtapp.parabox.ui.setting

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.launchNotificationSetting
import com.ojhdtapp.parabox.ui.MainSharedViewModel
import com.ojhdtapp.parabox.ui.util.ActivityEvent
import com.ojhdtapp.parabox.ui.util.NormalPreference
import com.ojhdtapp.parabox.ui.util.PreferencesCategory
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@RootNavGraph(start = false)
@Composable
fun NotificationPage(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    mainNavController: NavController,
    mainSharedViewModel: MainSharedViewModel,
    sizeClass: WindowSizeClass,
    onEvent: (ActivityEvent) -> Unit
) {
    val viewModel = hiltViewModel<SettingPageViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val notificationPermissionGranted =
        viewModel.notificationPermissionGrantedStateFlow.collectAsState().value
    val permissionRequester = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            viewModel.onNotificationPermissionResult(it)
        })
    var showDialog by remember {
        mutableStateOf(false)
    }
    ContactListDialog(
        modifier = Modifier,
        showDialog = showDialog,
        contactList = viewModel.contactStateFlow.collectAsState().value,
        contactCheck = { it.enableNotifications },
        onValueChange = { target, value ->
            viewModel.onContactNotificationChange(target, value)
        },
        loading = viewModel.contactLoadingState.value,
        sizeClass = sizeClass,
        onDismiss = {
            showDialog = false
        }
    )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val colorTransitionFraction = scrollBehavior.state.collapsedFraction
            val appBarContainerColor by rememberUpdatedState(
                lerp(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    FastOutLinearInEasing.transform(colorTransitionFraction)
                )
            )
            LargeTopAppBar(
                modifier = Modifier
                    .background(appBarContainerColor)
                    .statusBarsPadding(),
                title = { Text(stringResource(R.string.notification)) },
                navigationIcon = {
                    if (sizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
                        IconButton(onClick = {
                            mainNavController.navigateUp()
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "back"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
    ) {
        // Plugin List State
        val pluginList by mainSharedViewModel.pluginListStateFlow.collectAsState()
        LazyColumn(
            contentPadding = it
        ) {
            item {
                NormalPreference(
                    title = stringResource(R.string.request_notification_permission),
                    subtitle = if (notificationPermissionGranted) stringResource(R.string.notification_permission_granted) else stringResource(
                                            R.string.notification_permission_not_granted),
                    enabled = !notificationPermissionGranted,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AdminPanelSettings,
                            contentDescription = "channel",
                            tint = if (notificationPermissionGranted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionRequester.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }
            item {
                NormalPreference(
                    title = stringResource(R.string.system_notification_settings_title),
                    subtitle = stringResource(R.string.system_notification_settings_subtitle),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = "channel"
                        )
                    },
                    onClick = {
                        context.launchNotificationSetting()
                    }
                )
            }
            item {
                PreferencesCategory(text = stringResource(R.string.contact_notification_settings))
            }
            item {
                NormalPreference(title = stringResource(R.string.contact_notification_settings_title), subtitle = stringResource(
                                    R.string.contact_notification_settings_subtitle)
                                ) {
                    showDialog = true
                }
            }
        }
    }
}