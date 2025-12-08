package com.loudon23.acatch.ui.video.list

import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun VideoAppDrawer(
    drawerState: DrawerState,
    scope: CoroutineScope,
    directoryPickerLauncher: ActivityResultLauncher<Uri?>,
    onClearAllData: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    NavigationDrawerItem(
                        label = { Text("Add Folder") },
                        selected = false,
                        onClick = {
                            directoryPickerLauncher.launch(null)
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = "Add Folder") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Export Videos") },
                        selected = false,
                        onClick = {
                            Toast.makeText(context, "Export functionality not yet implemented", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Filled.Upload, contentDescription = "Export Videos") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Import Videos") },
                        selected = false,
                        onClick = {
                            Toast.makeText(context, "Import functionality not yet implemented", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Filled.Download, contentDescription = "Import Videos") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Delete All Data") },
                        selected = false,
                        onClick = {
                            onClearAllData()
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Filled.DeleteForever, contentDescription = "Delete All Data") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        },
        content = content
    )
}
