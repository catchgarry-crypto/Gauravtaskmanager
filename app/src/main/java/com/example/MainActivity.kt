package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.TaskItem
import com.example.ui.AppTab
import com.example.ui.TaskViewModel
import com.example.ui.TaskViewModelFactory
import com.example.ui.components.AddEditTaskBottomSheet
import com.example.ui.components.NotificationPermissionHandler
import com.example.ui.components.TaskListScreen
import com.example.ui.components.TimelineScreen
import com.example.ui.components.VisualDashboardScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels {
        TaskViewModelFactory((application as TaskApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: TaskViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val rawTasks by viewModel.rawTasks.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val overdueTasks by viewModel.overdueTasks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedPriority by viewModel.selectedPriority.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val dashboardStats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val isAddEditSheetOpen by viewModel.isAddEditSheetOpen.collectAsStateWithLifecycle()
    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "TaskFlow",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (currentTab) {
                                    AppTab.TASKS -> "${rawTasks.count { !it.isCompleted }} active assignments"
                                    AppTab.TIMELINE -> "Timeline & Deadlines"
                                    AppTab.DASHBOARD -> "${(dashboardStats.completionRate * 100).toInt()}% completion velocity"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (overdueTasks.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFE4E6)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .badgeColor()
                                )
                                Text(
                                    text = "${overdueTasks.size} Overdue",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFE11D48)
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.TASKS,
                    onClick = { viewModel.selectTab(AppTab.TASKS) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.TASKS) Icons.Default.Checklist else Icons.Outlined.Checklist,
                            contentDescription = "Tasks"
                        )
                    },
                    label = { Text("Tasks") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_tasks")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.TIMELINE,
                    onClick = { viewModel.selectTab(AppTab.TIMELINE) },
                    icon = {
                        if (overdueTasks.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = Color(0xFFE11D48)) {
                                        Text("${overdueTasks.size}")
                                    }
                                }
                            ) {
                                Icon(
                                    if (currentTab == AppTab.TIMELINE) Icons.Default.Schedule else Icons.Outlined.Schedule,
                                    contentDescription = "Timeline"
                                )
                            }
                        } else {
                            Icon(
                                if (currentTab == AppTab.TIMELINE) Icons.Default.Schedule else Icons.Outlined.Schedule,
                                contentDescription = "Timeline"
                            )
                        }
                    },
                    label = { Text("Timeline") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_timeline")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.DASHBOARD,
                    onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                    icon = {
                        Icon(
                            if (currentTab == AppTab.DASHBOARD) Icons.Default.Analytics else Icons.Outlined.Analytics,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateTaskSheet() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                modifier = Modifier.testTag("fab_add_task")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Notification Permission Banner
            NotificationPermissionHandler()

            // Main Body Content
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition",
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    AppTab.TASKS -> {
                        TaskListScreen(
                            tasks = filteredTasks,
                            overdueTasks = overdueTasks,
                            categories = categories,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.searchQuery.value = it },
                            selectedFilter = selectedFilter,
                            onSelectFilter = { viewModel.selectedFilter.value = it },
                            selectedPriority = selectedPriority,
                            onSelectPriority = { viewModel.selectedPriority.value = it },
                            selectedCategory = selectedCategory,
                            onSelectCategory = { viewModel.selectedCategory.value = it },
                            onToggleCompletion = { task ->
                                viewModel.toggleTaskCompletion(task)
                                coroutineScope.launch {
                                    val action = if (!task.isCompleted) "Completed" else "Reopened"
                                    snackbarHostState.showSnackbar(
                                        message = "\"${task.title}\" $action",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onEdit = { task -> viewModel.openEditTaskSheet(task) },
                            onDelete = { task ->
                                viewModel.deleteTask(task)
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Task deleted",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.saveTask(
                                            title = task.title,
                                            description = task.description,
                                            priority = task.priority,
                                            category = task.category,
                                            dueDate = task.dueDate,
                                            startDate = task.startDate,
                                            reminderEnabled = task.reminderEnabled,
                                            subtasks = task.subtasks,
                                            status = task.status
                                        )
                                    }
                                }
                            },
                            onToggleSubtask = { task, subId -> viewModel.toggleSubtask(task, subId) },
                            onAddNewTask = { viewModel.openCreateTaskSheet() }
                        )
                    }

                    AppTab.TIMELINE -> {
                        TimelineScreen(
                            tasks = rawTasks,
                            onToggleCompletion = { task ->
                                viewModel.toggleTaskCompletion(task)
                                coroutineScope.launch {
                                    val action = if (!task.isCompleted) "Completed" else "Reopened"
                                    snackbarHostState.showSnackbar(
                                        message = "\"${task.title}\" $action",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onEdit = { task -> viewModel.openEditTaskSheet(task) },
                            onDelete = { task ->
                                viewModel.deleteTask(task)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Task deleted", duration = SnackbarDuration.Short)
                                }
                            },
                            onToggleSubtask = { task, subId -> viewModel.toggleSubtask(task, subId) }
                        )
                    }

                    AppTab.DASHBOARD -> {
                        VisualDashboardScreen(
                            stats = dashboardStats,
                            completedTasks = rawTasks.filter { it.isCompleted }.sortedByDescending { it.completedAt ?: 0L },
                            onToggleCompletion = { task ->
                                viewModel.toggleTaskCompletion(task)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Reopened \"${task.title}\"",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // Add / Edit Task Modal Bottom Sheet
        if (isAddEditSheetOpen) {
            AddEditTaskBottomSheet(
                taskToEdit = editingTask,
                existingCategories = categories,
                onDismiss = { viewModel.closeAddEditSheet() },
                onSave = { title, desc, priority, category, dueDate, startDate, reminder, subtasks, status ->
                    viewModel.saveTask(title, desc, priority, category, dueDate, startDate, reminder, subtasks, status)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message = if (editingTask == null) "Task created & alert set!" else "Task updated",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            )
        }
    }
}

private fun Modifier.badgeColor() = this.background(Color(0xFFE11D48))
