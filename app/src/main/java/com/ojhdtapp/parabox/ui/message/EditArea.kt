package com.ojhdtapp.parabox.ui.message

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.ojhdtapp.messagedto.message_content.MessageContent
import com.ojhdtapp.messagedto.message_content.PlainText
import com.ojhdtapp.parabox.BuildConfig
import com.ojhdtapp.parabox.core.util.toDateAndTimeString
import com.ojhdtapp.parabox.ui.util.clearFocusOnKeyboardDismiss
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(
    ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalAnimationApi::class, ExperimentalPermissionsApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun EditArea(
    modifier: Modifier = Modifier,
    isBottomSheetExpand: Boolean,
    onBottomSheetExpand: () -> Unit,
    onBottomSheetCollapse: () -> Unit,
    onSend: (contents: List<MessageContent>) -> Unit,
    onTextFieldHeightChange: (height: Int) -> Unit
) {
    val context = LocalContext.current

    // Meme
    val memeList = remember {
        mutableStateListOf<File>()
    }
    LaunchedEffect(key1 = true) {
        context.getExternalFilesDir("meme")?.listFiles()
            ?.filter { it.path.endsWith(".jpg") || it.path.endsWith(".jpeg") || it.path.endsWith(".png") }
            ?.forEach {
                memeList.add(it)
            }
    }

    // Image Send
    val gallerySelected = remember {
        mutableStateListOf<Uri>()
    }
    val cameraSelected = remember {
        mutableStateListOf<Uri>()
    }

    var audioState by remember {
        mutableStateOf(false)
    }
    var emojiState by remember {
        mutableStateOf(false)
    }
    var inputText by remember {
        mutableStateOf("")
    }
    var shouldToolbarShrink by remember {
        mutableStateOf(false)
    }
    val audioPermissionState =
        rememberPermissionState(permission = android.Manifest.permission.RECORD_AUDIO)
    val imagePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.let {
                    var i = 0
                    while (i < it.clipData!!.itemCount) {
                        it.clipData!!.getItemAt(i).also {
                            if (!gallerySelected.contains(it.uri)) {
                                gallerySelected.add(it.uri)
                            }
                            Log.d("parabox", it.uri.toString())
                        }
                        i++
                    }
                }
            }
        }
    val imagePickerSLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents()) {
            it.forEach {
                if (!gallerySelected.contains(it)) {
                    gallerySelected.add(it)
                }
                Log.d("parabox", it.toString())
            }
        }
    val targetCameraShotUri = remember(cameraSelected.size) {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val outPutFile = File(File(path, "Parabox/Camera").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }, "CHAT_${System.currentTimeMillis().toDateAndTimeString()}.jpg")
        FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".provider", outPutFile
        )
    }
    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) {
            if (it) {
                cameraSelected.add(targetCameraShotUri)
                Log.d("parabox", targetCameraShotUri.toString())
            }
        }
    val filePickerLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) {
            Log.d("parabox", it.toString())
        }

    val cameraAccessible by remember {
        mutableStateOf<Boolean>(context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
    }

    val permissionDeniedDialog = remember { mutableStateOf(false) }
    if (permissionDeniedDialog.value) {
        AlertDialog(
            onDismissRequest = {
                permissionDeniedDialog.value = false
            },
            icon = { Icon(Icons.Outlined.KeyboardVoice, contentDescription = null) },
            title = {
                Text(text = "权限申请")
            },
            text = {
                Text(
                    "要发送语音消息，您需要授权本应用使用设备麦克风。\n您亦可前往设置页面手动授权。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionDeniedDialog.value = false
                        audioPermissionState.launchPermissionRequest()
                    }
                ) {
                    Text("尝试授权")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        permissionDeniedDialog.value = false
                    }
                ) {
                    Text("转到设置")
                }
            }
        )
    }
    Surface(
        modifier = modifier
//            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(MaterialTheme.colorScheme.surface),
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            val relocation = remember { BringIntoViewRequester() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 88.dp)
                    .bringIntoViewRequester(relocation)
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                var isEditing by remember {
                    mutableStateOf(false)
                }
                val keyboardController = LocalSoftwareKeyboardController.current
                Crossfade(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
                        .animateContentSize(),
                    targetState = shouldToolbarShrink
                ) {
                    if (it) {
                        IconButton(onClick = { shouldToolbarShrink = false }) {
                            Icon(
                                imageVector = Icons.Outlined.NavigateNext,
                                contentDescription = "expand"
                            )
                        }
                    } else {
                        Row() {
                            IconButton(onClick = {
                                keyboardController?.hide()
                                if (!isBottomSheetExpand) {
                                    onBottomSheetExpand()
                                    emojiState = false
                                } else {
                                    if (emojiState) {
                                        emojiState = false
                                    } else {
                                        onBottomSheetCollapse()
                                    }
                                }

                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.AddCircleOutline,
                                    contentDescription = "more"
                                )
                            }
                            IconButton(onClick = {
                                keyboardController?.hide()
                                if (!isBottomSheetExpand) {
                                    onBottomSheetExpand()
                                    emojiState = true
                                } else {
                                    if (!emojiState) {
                                        emojiState = true
                                    } else {
                                        onBottomSheetCollapse()
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.EmojiEmotions,
                                    contentDescription = "emoji"
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp, top = 4.dp, bottom = 4.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        modifier = Modifier.zIndex(2f),
                        visible = audioState,
                        enter = expandHorizontally { 0 },
                        exit = shrinkHorizontally { 0 }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            onClick = {}
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "长按录制", color = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { audioState = false }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Keyboard,
                                        contentDescription = "keyboard",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = {
                            isEditing = true
                        }
                    ) {
                        val originalBoxHeight = with(LocalDensity.current) {
                            24.dp.toPx().toInt()
                        }
                        //temp
//                    val originalBoxHeight = 56
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .weight(1f)
                                    .padding(12.dp)
                                    .onSizeChanged { onTextFieldHeightChange(it.height - originalBoxHeight) },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val scope = rememberCoroutineScope()
                                val focusRequester = remember { FocusRequester() }
                                LaunchedEffect(isEditing) {
                                    if (isEditing) focusRequester.requestFocus()
                                }
                                BasicTextField(
                                    modifier = Modifier
                                        .onFocusEvent {
                                            if (it.isFocused) scope.launch { delay(200); relocation.bringIntoView() }
                                        }
                                        .focusRequester(focusRequester)
                                        .clearFocusOnKeyboardDismiss() {
                                            isEditing = false
                                        },
                                    value = inputText,
                                    onValueChange = {
                                        if (it.length > 6) shouldToolbarShrink = true
                                        else if (it.isEmpty()) shouldToolbarShrink = false
                                        inputText = it
                                    },
                                    enabled = isEditing && !audioState,
                                    textStyle = MaterialTheme.typography.bodyLarge.merge(
                                        TextStyle(color = MaterialTheme.colorScheme.onSurface)
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (inputText.isEmpty()) {
                                            Text(
                                                text = "输入内容",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    },
                                    cursorBrush = SolidColor(value = MaterialTheme.colorScheme.primary)
                                )
                            }
                            AnimatedVisibility(
                                visible = inputText.isEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                IconButton(onClick = {
                                    if (audioPermissionState.status.isGranted) {
                                        audioState = true
                                        isEditing = false
                                    } else {
                                        permissionDeniedDialog.value = true
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.KeyboardVoice,
                                        contentDescription = "voice"
                                    )
                                }
                            }
                        }
                    }

                }
                AnimatedVisibility(visible = (inputText.isNotEmpty() || false) && !audioState,
//                    enter = slideInHorizontally { width -> width },
//                    exit = slideOutHorizontally { width -> width }
                    enter = expandHorizontally() { width -> 0 },
                    exit = shrinkHorizontally() { width -> 0 }
                ) {
                    FloatingActionButton(
                        onClick = {
                            val content = mutableListOf<MessageContent>()
                            if (inputText.isNotEmpty()) {
                                content.add(PlainText(inputText))
                            }
                            onSend(content)
                            inputText = ""
                        },
                        modifier = Modifier.padding(end = 16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Icon(imageVector = Icons.Outlined.Send, contentDescription = "send")
                    }
                }
            }
            AnimatedContent(targetState = emojiState,
                transitionSpec = {
                    if (targetState) {
                        (slideIntoContainer(AnimatedContentScope.SlideDirection.Start) with slideOutOfContainer(
                            AnimatedContentScope.SlideDirection.Start
                        ))
                    } else {
                        (slideIntoContainer(AnimatedContentScope.SlideDirection.End) with slideOutOfContainer(
                            AnimatedContentScope.SlideDirection.End
                        ))
                    }
                }) {
                if (it) {
                    Row(
                        modifier = Modifier
                            .height(160.dp)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 4.dp)
                    ) {
                        var memeState by remember {
                            mutableStateOf(true)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(0.5f)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 2.dp, bottom = 2.dp),
                                color = if (memeState) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            memeState = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.FavoriteBorder,
                                        contentDescription = "favorite",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 2.dp, top = 2.dp),
                                color = if (memeState) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer,
                                tonalElevation = 3.dp,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable {
                                            memeState = false
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Face,
                                        contentDescription = "face",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        AnimatedContent(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f), targetState = memeState
                        ) {
                            if (it) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (memeList.isEmpty()) {
                                        item {
                                            Column(
                                                modifier = Modifier.fillParentMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "暂无自定义表情",
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                                TextButton(onClick = { /*TODO*/ }) {
                                                    Text(text = "手动添加")
                                                }
                                            }
                                        }
                                    } else {
                                        items(items = memeList, key = { it.name }) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                tonalElevation = 3.dp,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(3.dp)
                                                        .clickable { }) {
                                                    val imageLoader = ImageLoader.Builder(context)
                                                        .components {
                                                            if (Build.VERSION.SDK_INT >= 28) {
                                                                add(ImageDecoderDecoder.Factory())
                                                            } else {
                                                                add(GifDecoder.Factory())
                                                            }
                                                        }
                                                        .build()
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(it)
                                                            .crossfade(true)
                                                            .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                                            .build(),
                                                        imageLoader = imageLoader,
                                                        contentDescription = "meme",
                                                        contentScale = ContentScale.FillHeight,
                                                        modifier = Modifier
                                                            .widthIn(0.dp, 144.dp)
                                                            .clip(
                                                                RoundedCornerShape(13.dp)
                                                            )
                                                    )

                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyHorizontalGrid(
                                    rows = GridCells.Fixed(2),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(
                                        items = listOf(
                                            "\uD83E\uDD24",
                                            "\uD83E\uDD13",
                                            "\uD83D\uDE05",
                                            "\uD83D\uDE02",
                                            "\uD83E\uDD23",
                                            "\uD83D\uDE2D",
                                            "\uD83E\uDD70",
                                            "\uD83D\uDE0B",
                                            "\uD83E\uDD17",
                                            "\uD83D\uDE13",
                                            "\uD83E\uDD7A",
                                            "\uD83E\uDD14",
                                            "\uD83D\uDE09",
                                            "\uD83D\uDE19",
                                            "\uD83D\uDE0D",
                                            "\uD83D\uDE1D",
                                            "\uD83E\uDEE3",
                                            "\uD83D\uDE31",
                                            "\uD83D\uDE21",
                                            "\uD83D\uDE0E",
                                            "\uD83D\uDC2E",
                                            "\uD83D\uDC34",
                                            "\uD83D\uDC2D",
                                            "\uD83D\uDC30",
                                            "\uD83D\uDC38",
                                            "\uD83D\uDC22",
                                            "\uD83D\uDC12",
                                            "\uD83D\uDC37",
                                            "\uD83D\uDCA7",
                                            "☔"
                                        )
                                    ) {
                                        Surface(
                                            modifier = Modifier.width(48.dp),
                                            color = Color.Transparent,
                                            tonalElevation = 0.dp,
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable {
                                                        inputText += it
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = it)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    AnimatedContent(targetState = gallerySelected.isNotEmpty() || cameraSelected.isNotEmpty()) {
                        if (it) {
                            Row(
                                modifier = Modifier
                                    .height(160.dp)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(bottom = 4.dp)
                            ) {
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .aspectRatio(0.5f)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(
                                                start = if (cameraAccessible) 0.dp else 2.dp,
                                                bottom = if (cameraAccessible) 0.dp else 2.dp
                                            ),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 3.dp,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                        val maxNumPhotosAndVideos = 10
                                                        Intent(MediaStore.ACTION_PICK_IMAGES)
                                                            .apply {
                                                                type = "image/*"
                                                                putExtra(
                                                                    MediaStore.EXTRA_PICK_IMAGES_MAX,
                                                                    maxNumPhotosAndVideos
                                                                )
                                                            }
                                                            .also {
                                                                imagePickerLauncher.launch(it)
                                                            }
                                                    } else {
                                                        imagePickerSLauncher.launch("image/*")
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            BadgedBox(badge = {
                                                gallerySelected.size.let {
                                                    if (it > 0) {
                                                        Badge {
                                                            Text(
                                                                text = "$it",
                                                                modifier = Modifier.semantics {
                                                                    contentDescription =
                                                                        "$it selected gallery images"
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AddPhotoAlternate,
                                                    contentDescription = "gallery add",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    if (cameraAccessible) {
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 2.dp, top = 2.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 3.dp,
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable {
                                                        cameraLauncher.launch(targetCameraShotUri)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                BadgedBox(badge = {
                                                    cameraSelected.size.let {
                                                        if (it > 0) {
                                                            Badge {
                                                                Text(
                                                                    text = "$it",
                                                                    modifier = Modifier.semantics {
                                                                        contentDescription =
                                                                            "$it selected camera images"
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.AddAPhoto,
                                                        contentDescription = "camera add",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(1f),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(items = gallerySelected, key = { it.toString() }) {
                                        Surface(
                                            modifier = Modifier.animateItemPlacement(),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            tonalElevation = 3.dp,
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(3.dp)
                                                    .clickable { }
                                            ) {
                                                val imageLoader = ImageLoader.Builder(context)
                                                    .components {
                                                        if (Build.VERSION.SDK_INT >= 28) {
                                                            add(ImageDecoderDecoder.Factory())
                                                        } else {
                                                            add(GifDecoder.Factory())
                                                        }
                                                    }
                                                    .build()
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(it)
                                                        .crossfade(true)
                                                        .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                                        .build(),
                                                    imageLoader = imageLoader,
                                                    contentDescription = "gallery",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .width(144.dp)
                                                        .clip(
                                                            RoundedCornerShape(13.dp)
                                                        )
                                                )
                                                IconButton(
                                                    modifier = Modifier.align(Alignment.TopEnd),
                                                    onClick = { gallerySelected.remove(it) },
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Cancel,
                                                        contentDescription = "delete"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    items(items = cameraSelected, key = { it.toString() }) {
                                        Surface(
                                            modifier = Modifier.animateItemPlacement(),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            tonalElevation = 3.dp,
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(3.dp)
                                                    .clickable { }) {
                                                val imageLoader = ImageLoader.Builder(context)
                                                    .components {
                                                        if (Build.VERSION.SDK_INT >= 28) {
                                                            add(ImageDecoderDecoder.Factory())
                                                        } else {
                                                            add(GifDecoder.Factory())
                                                        }
                                                    }
                                                    .build()
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(it)
                                                        .crossfade(true)
                                                        .diskCachePolicy(CachePolicy.ENABLED)// it's the same even removing comments
                                                        .build(),
                                                    imageLoader = imageLoader,
                                                    contentDescription = "camera",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .width(144.dp)
                                                        .clip(
                                                            RoundedCornerShape(13.dp)
                                                        )
                                                )
                                                IconButton(
                                                    modifier = Modifier.align(Alignment.TopEnd),
                                                    onClick = { cameraSelected.remove(it) },
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Cancel,
                                                        contentDescription = "delete"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .height(160.dp)
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 4.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(horizontal = 2.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 3.dp,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                    val maxNumPhotosAndVideos = 10
                                                    Intent(MediaStore.ACTION_PICK_IMAGES)
                                                        .apply {
                                                            type = "image/*"
                                                            putExtra(
                                                                MediaStore.EXTRA_PICK_IMAGES_MAX,
                                                                maxNumPhotosAndVideos
                                                            )
                                                        }
                                                        .also {
                                                            imagePickerLauncher.launch(it)
                                                        }
                                                } else {
                                                    imagePickerSLauncher.launch("image/*")
                                                }
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            imageVector = Icons.Outlined.Image,
                                            contentDescription = "image",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "相册",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                                if (cameraAccessible) {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(horizontal = 2.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 3.dp,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable {
                                                    cameraLauncher.launch(targetCameraShotUri)
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                modifier = Modifier.padding(bottom = 8.dp),
                                                imageVector = Icons.Outlined.PhotoCamera,
                                                contentDescription = "camera",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "拍摄",
                                                style = MaterialTheme.typography.labelLarge
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(horizontal = 2.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 3.dp,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable {
                                                filePickerLauncher.launch("*/*")
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = "file",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "文件",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(horizontal = 2.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 3.dp,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(bottom = 8.dp),
                                            imageVector = Icons.Outlined.Videocam,
                                            contentDescription = "video",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "视频",
                                            style = MaterialTheme.typography.labelLarge
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