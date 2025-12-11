package com.loudon23.acatch.ui.video.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loudon23.acatch.ui.video.SortOrder

@Composable
fun SortDialog(
    currentSortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val sortOptions = listOf(
        SortOrder.NEWEST_FIRST,
        SortOrder.OLDEST_FIRST,
        SortOrder.NAME_ASC,
        SortOrder.NAME_DESC
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Sort by") },
        text = {
            Column {
                sortOptions.forEach { sortOrder ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (sortOrder == currentSortOrder),
                                onClick = { onSortOrderChange(sortOrder) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (sortOrder == currentSortOrder),
                            onClick = { onSortOrderChange(sortOrder) }
                        )
                        Text(
                            text = when (sortOrder) {
                                SortOrder.NEWEST_FIRST -> "Newest first"
                                SortOrder.OLDEST_FIRST -> "Oldest first"
                                SortOrder.NAME_ASC -> "Name (A-Z)"
                                SortOrder.NAME_DESC -> "Name (Z-A)"
                            },
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
