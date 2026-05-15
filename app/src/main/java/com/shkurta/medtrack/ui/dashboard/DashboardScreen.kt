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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shkurta.medtrack.data.entity.Medication
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val focusRequester = remember { FocusRequester() }
    var showMedicationSheet by remember { mutableStateOf(false) }

    val background = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground

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
                    containerColor = background,
                    titleContentColor = onBackground,
                    actionIconContentColor = onBackground,
                    navigationIconContentColor = onBackground
                )
            )
        },
        bottomBar = {
            ScheduleBottomBar(
                instantLogName = uiState.instantLogName,
                dateLabel = uiState.currentDateLabel,
                onNameChange = viewModel::onInstantNameChange,
                onLog = viewModel::instantLog,
                focusRequester = focusRequester,
                onAddClick = { showMedicationSheet = true }
            )
        },
        containerColor = background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().background(background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = onBackground)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(background),
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
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }

    if (showMedicationSheet) {
        MedicationPickerSheet(
            medications = uiState.medications,
            onPick = { med ->
                viewModel.markAsTaken(med.id)
                showMedicationSheet = false
            },
            onDismiss = { showMedicationSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationPickerSheet(
    medications: List<Medication>,
    onPick: (Medication) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(outline, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Text(
            text = "Quick Add",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            color = outline,
            thickness = 0.5.dp
        )

        if (medications.isEmpty()) {
            Text(
                text = "No medications saved yet.\nType a name in the field below to add one.",
                style = MaterialTheme.typography.bodyMedium,
                color = onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
        } else {
            medications.forEach { med ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(med) }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = med.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = onBackground
                        )
                        Text(
                            text = med.dosage,
                            style = MaterialTheme.typography.bodySmall,
                            color = onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Log ${med.name}",
                        tint = onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(start = 20.dp),
                    color = outline,
                    thickness = 0.5.dp
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun ScheduleDateHeader(header: String) {
    val isToday = header.startsWith("Today")
    val onBackground = MaterialTheme.colorScheme.onBackground
    val background = MaterialTheme.colorScheme.background

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isToday) {
            val parts = header.split(" ", limit = 2)
            Surface(
                color = onBackground,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = parts[0],
                    color = background,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Text(
                text = parts.getOrNull(1) ?: "",
                color = onBackground,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        } else {
            Text(
                text = header,
                color = onBackground,
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
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            color = onBackground,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(56.dp)
        )

        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .width(3.dp)
                .height(24.dp)
                .background(onBackground, RoundedCornerShape(2.dp))
        )

        Column {
            Text(
                text = item.medication.name,
                color = onBackground,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "${item.medication.dosage} • ${if (item.isTaken) "Taken" else "Scheduled"}",
                color = onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
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
    focusRequester: FocusRequester,
    onAddClick: () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(surface, CircleShape)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (instantLogName.isEmpty()) {
                    Text(
                        text = "Add medication on $dateLabel",
                        color = onSurfaceVariant,
                        style = TextStyle(fontSize = 16.sp)
                    )
                }
                BasicTextField(
                    value = instantLogName,
                    onValueChange = onNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = TextStyle(color = onBackground, fontSize = 16.sp),
                    cursorBrush = SolidColor(onBackground),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onLog() }),
                    singleLine = true
                )
            }

            Spacer(Modifier.width(16.dp))

            FloatingActionButton(
                onClick = onAddClick,
                containerColor = surfaceVariant,
                contentColor = onBackground,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(32.dp))
            }
        }
    }
}
