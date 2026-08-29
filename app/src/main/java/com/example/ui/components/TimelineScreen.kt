package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TaskItem
import java.util.Calendar

data class TimelineGroup(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val tasks: List<TaskItem>
)

@Composable
fun TimelineScreen(
    tasks: List<TaskItem>,
    onToggleCompletion: (TaskItem) -> Unit,
    onEdit: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    onToggleSubtask: (TaskItem, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val timelineGroups = remember(tasks) {
        val now = System.currentTimeMillis()
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val todayDayOfYear = calNow.get(Calendar.DAY_OF_YEAR)
        val todayYear = calNow.get(Calendar.YEAR)

        val overdue = mutableListOf<TaskItem>()
        val today = mutableListOf<TaskItem>()
        val tomorrow = mutableListOf<TaskItem>()
        val thisWeek = mutableListOf<TaskItem>()
        val later = mutableListOf<TaskItem>()
        val noDate = mutableListOf<TaskItem>()

        tasks.filter { !it.isCompleted }.forEach { task ->
            val due = task.dueDate
            if (due == null) {
                noDate.add(task)
            } else if (due < now) {
                overdue.add(task)
            } else {
                val calDue = Calendar.getInstance().apply { timeInMillis = due }
                val dueDayOfYear = calDue.get(Calendar.DAY_OF_YEAR)
                val dueYear = calDue.get(Calendar.YEAR)

                if (dueYear == todayYear && dueDayOfYear == todayDayOfYear) {
                    today.add(task)
                } else if (dueYear == todayYear && dueDayOfYear == todayDayOfYear + 1) {
                    tomorrow.add(task)
                } else if (dueYear == todayYear && dueDayOfYear <= todayDayOfYear + 7) {
                    thisWeek.add(task)
                } else {
                    later.add(task)
                }
            }
        }

        listOf(
            TimelineGroup(
                title = "Overdue Timeline",
                subtitle = "Past due date — prioritize immediate completion",
                icon = Icons.Default.Warning,
                color = Color(0xFFE11D48),
                tasks = overdue.sortedBy { it.dueDate }
            ),
            TimelineGroup(
                title = "Due Today",
                subtitle = "Timeline milestones due before midnight",
                icon = Icons.Default.Schedule,
                color = Color(0xFF4F46E5),
                tasks = today.sortedBy { it.dueDate }
            ),
            TimelineGroup(
                title = "Due Tomorrow",
                subtitle = "Next in queue",
                icon = Icons.Default.CalendarMonth,
                color = Color(0xFF0284C7),
                tasks = tomorrow.sortedBy { it.dueDate }
            ),
            TimelineGroup(
                title = "This Week",
                subtitle = "Within the next 7 days",
                icon = Icons.Default.EventNote,
                color = Color(0xFF059669),
                tasks = thisWeek.sortedBy { it.dueDate }
            ),
            TimelineGroup(
                title = "Upcoming & Later",
                subtitle = "Future milestones & long-range timelines",
                icon = Icons.Default.EventNote,
                color = Color(0xFF7C3AED),
                tasks = later.sortedBy { it.dueDate }
            ),
            TimelineGroup(
                title = "No Deadline",
                subtitle = "Backlog tasks without specific timeframes",
                icon = Icons.Default.EventNote,
                color = Color(0xFF64748B),
                tasks = noDate
            )
        ).filter { it.tasks.isNotEmpty() }
    }

    if (timelineGroups.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("timeline_empty_state"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No Active Timelines",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "All active assignments are completed or none have been added yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("timeline_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            timelineGroups.forEach { group ->
                item(key = group.title) {
                    // Group Header
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = group.color.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(group.color),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = group.icon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${group.title} (${group.tasks.size})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = group.color
                                )
                                Text(
                                    text = group.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(group.tasks, key = { it.id }) { task ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Vertical Timeline Node & Line
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(28.dp)
                                .padding(top = 18.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(group.color)
                            )
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(90.dp)
                                    .background(group.color.copy(alpha = 0.3f))
                            )
                        }

                        // Task Card
                        TaskCard(
                            task = task,
                            onToggleCompletion = { onToggleCompletion(task) },
                            onEdit = { onEdit(task) },
                            onDelete = { onDelete(task) },
                            onToggleSubtask = { subId -> onToggleSubtask(task, subId) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
