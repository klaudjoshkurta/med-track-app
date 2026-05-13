package com.shkurta.medtrack.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shkurta.medtrack.ui.theme.MedTrackTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddMedication: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Schedule", 
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp
                        )
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            ScheduleBottomBar(
                instantLogName = uiState.instantLogName,
                dateLabel = uiState.currentDateLabel,
                onNameChange = viewModel::onInstantNameChange,
                onLog = viewModel::instantLog,
                onAddClick = onAddMedication
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Black),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                uiState.scheduleItems.forEach { (dateHeader, items) ->
                    item {
                        ScheduleDateHeader(dateHeader)
                    }
                    items(items) { item ->
                        ScheduleTimelineItem(
                            item = item,
                            time = timeFormatter.format(Date(item.timestamp)),
                            onItemClick = { 
                                if (!item.isTaken) {
                                    viewModel.markAsTaken(item.medication.id)
                                }
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            thickness = 0.5.dp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleDateHeader(header: String) {
    val isToday = header.startsWith("Today")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isToday) {
            val parts = header.split(" ", limit = 2)
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = parts[0],
                    color = Color.Black,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = parts.getOrNull(1) ?: "",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        } else {
            Text(
                text = header,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
fun ScheduleTimelineItem(
    item: ScheduleItem,
    time: String,
    onItemClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .alpha(if (item.isTaken) 0.4f else 1f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time
        Text(
            text = time,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(56.dp)
        )
        
        // Vertical Accent Bar
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .width(3.dp)
                .height(24.dp)
                .background(if (item.isTaken) Color.Gray else Color.White, RoundedCornerShape(2.dp))
        )
        
        // Content
        Column {
            Text(
                text = item.medication.name,
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (item.isTaken) TextDecoration.LineThrough else null
                )
            )
            Text(
                text = "${item.medication.dosage} • ${if (item.isTaken) "Taken" else "Scheduled"}",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = if (item.isTaken) TextDecoration.LineThrough else null
                )
            )
        }
    }
}

@Composable
fun ScheduleBottomBar(
    instantLogName: String,
    dateLabel: String,
    onNameChange: (String) -> Unit,
    onLog: () -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick Entry Field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(Color(0xFF222222), CircleShape)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (instantLogName.isEmpty()) {
                    Text(
                        text = "Add medication on $dateLabel",
                        color = Color.Gray,
                        style = TextStyle(fontSize = 16.sp)
                    )
                }
                BasicTextField(
                    value = instantLogName,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(Color.White),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onLog() }),
                    singleLine = true
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Large FAB
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = Color(0xFF333333),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
            }
        }
    }
}
