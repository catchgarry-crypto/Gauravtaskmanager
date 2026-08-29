package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class TaskPriority(
    val label: String,
    val rank: Int,
    val colorHex: Long,
    val containerColorHex: Long
) {
    URGENT("Urgent", 1, 0xFFE11D48, 0xFFFFE4E6), // Rose 600
    HIGH("High", 2, 0xFFEA580C, 0xFFFFEDD5),   // Orange 600
    MEDIUM("Medium", 3, 0xFF0284C7, 0xFFE0F2FE), // Light Blue 600
    LOW("Low", 4, 0xFF059669, 0xFFD1FAE5);    // Emerald 600

    val color: Color
        get() = Color(colorHex)

    val containerColor: Color
        get() = Color(containerColorHex)
}
