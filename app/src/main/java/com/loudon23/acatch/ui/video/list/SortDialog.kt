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

@Composable
fun SortDialog(
    currentSortOrder: SortOption,
    onSortOrderChange: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sortOptions = listOf(
        SortOption.LATEST,
        SortOption.OLDEST,
        SortOption.NAME_AZ,
        SortOption.NAME_ZA
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
                                SortOption.LATEST -> "Newest first"
                                SortOption.OLDEST -> "Oldest first"
                                SortOption.NAME_AZ -> "Name (A-Z)"
                                SortOption.NAME_ZA -> "Name (Z-A)"
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
