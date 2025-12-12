package com.loudon23.acatch.ui.video.list

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loudon23.acatch.data.dao.FolderInfo
import com.loudon23.acatch.ui.video.VideoViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Collections

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderableFolderListScreen(
    videoViewModel: VideoViewModel = viewModel(),
    onNavigateUp: () -> Unit
) {
    val folderInfoList by videoViewModel.folderListState.collectAsState()
    var reorderableFolderList by remember(folderInfoList) { mutableStateOf(folderInfoList) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reorder Folders") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        videoViewModel.updateFolderOrder(reorderableFolderList)
                        onNavigateUp()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm Reorder")
                    }
                }
            )
        }
    ) { paddingValues ->
        ReorderableFolderList(
            list = reorderableFolderList, 
            onListChange = { reorderableFolderList = it },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
private fun ReorderableFolderList(
    list: List<FolderInfo>,
    onListChange: (List<FolderInfo>) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var dragJob by remember { mutableStateOf<Job?>(null) }
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    var internalList by remember { mutableStateOf(list) }

    LaunchedEffect(list, draggedItemIndex) {
        if (draggedItemIndex == null) {
            internalList = list
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        itemsIndexed(internalList, key = { _, item -> item.folder.uri }) { index, item ->
            val isDragging = index == draggedItemIndex
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 8.dp)
                    .graphicsLayer {
                        translationY = if(isDragging) dragOffset else 0f
                        shadowElevation = if(isDragging) 8f else 0f
                    },
                elevation = CardDefaults.cardElevation(if (isDragging) 8.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag Handle",
                        modifier = Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedItemIndex = index
                                },
                                onDragEnd = {
                                    draggedItemIndex = null
                                    dragOffset = 0f
                                    onListChange(internalList) 
                                },
                                onDragCancel = {
                                    draggedItemIndex = null
                                    dragOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y

                                    val currentDraggedIndex = draggedItemIndex ?: return@detectDragGestures
                                    val draggedItemLayout = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentDraggedIndex } ?: return@detectDragGestures

                                    val draggedItemCenter = draggedItemLayout.offset + dragOffset + (draggedItemLayout.size / 2)

                                    val targetItem = listState.layoutInfo.visibleItemsInfo.find {
                                        it.index != currentDraggedIndex &&
                                        draggedItemCenter >= it.offset && draggedItemCenter <= (it.offset + it.size)
                                    }

                                    if (targetItem != null) {
                                        val fromIndex = currentDraggedIndex
                                        val toIndex = targetItem.index

                                        if (fromIndex != toIndex) {
                                            val mutableList = internalList.toMutableList()
                                            Collections.swap(mutableList, fromIndex, toIndex)
                                            internalList = mutableList
                                            onListChange(internalList)

                                            dragOffset += (draggedItemLayout.offset - targetItem.offset)
                                            draggedItemIndex = toIndex
                                        }
                                    }

                                    if (dragJob?.isActive != true) {
                                        dragJob = scope.launch {
                                            listState.scrollBy(dragAmount.y)
                                        }
                                    }
                                }
                            )
                        }
                    )
                    Text(item.folder.name, modifier = Modifier.weight(1f).padding(start = 16.dp))
                }
            }
        }
    }
}