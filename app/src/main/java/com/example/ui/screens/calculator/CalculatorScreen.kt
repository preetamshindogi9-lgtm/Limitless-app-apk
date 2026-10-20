package com.example.ui.screens.calculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calculator.AngleMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showHistory by remember { mutableStateOf(false) }

    LaunchedEffect(state.noteCreatedToast) {
        state.noteCreatedToast?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Scientific Calculator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Academic calculations in study context",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHistory = !showHistory },
                        modifier = Modifier.testTag("btn_calc_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = if (showHistory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { viewModel.saveCalculationToStudyNote() },
                        enabled = state.expression.isNotBlank(),
                        modifier = Modifier.testTag("btn_save_to_note")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NoteAdd,
                            contentDescription = "Save to Study Notes",
                            tint = if (state.expression.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Calculator Display Card
            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculator_display_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Mode indicators (DEG/RAD, SHIFT)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { viewModel.toggleAngleMode() }
                        ) {
                            Text(
                                text = if (state.angleMode == AngleMode.DEGREE) "DEG" else "RAD",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (state.errorMessage != null) {
                            Text(
                                text = state.errorMessage ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Expression input line
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState(), reverseScrolling = true),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (state.expression.isNotBlank()) state.expression else "0",
                            fontSize = 28.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Live result
                    val displayResult = state.finalResult.ifBlank { state.liveResult }
                    Text(
                        text = if (displayResult.isNotBlank()) "= $displayResult" else "",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Calculation History Strip (if toggled)
            if (showHistory && state.history.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    items(state.history) { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.clickable { viewModel.loadFromHistory(item) }
                        ) {
                            Text(
                                text = "${item.expression} = ${item.result}",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Keypad Layout (4 rows scientific + standard number grid)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Scientific Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcKey(text = "sin", onClick = { viewModel.onFunction("sin") }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(text = "cos", onClick = { viewModel.onFunction("cos") }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(text = "tan", onClick = { viewModel.onFunction("tan") }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(text = "ln", onClick = { viewModel.onFunction("ln") }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(text = "log", onClick = { viewModel.onFunction("log") }, modifier = Modifier.weight(1f), isSpecial = true)
                }

                // Scientific Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcKey(text = "√", onClick = { viewModel.onFunction("sqrt") }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(text = "^", onClick = { viewModel.onInput("^") }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(text = "(", onClick = { viewModel.onInput("(") }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(text = ")", onClick = { viewModel.onInput(")") }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(text = "π", onClick = { viewModel.onInput("π") }, modifier = Modifier.weight(1f), isSpecial = true)
                }

                // Numbers & Operations Grid
                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcKey(text = "C", onClick = { viewModel.onClear() }, modifier = Modifier.weight(1f), isDanger = true)
                    CalcKey(text = "e", onClick = { viewModel.onInput("e") }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(text = "%", onClick = { viewModel.onInput("%") }, modifier = Modifier.weight(1f), isSpecial = true)
                    CalcKey(text = "÷", onClick = { viewModel.onInput("÷") }, modifier = Modifier.weight(1f), isOperator = true)
                }

                // Row 4
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcKey(text = "7", onClick = { viewModel.onInput("7") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "8", onClick = { viewModel.onInput("8") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "9", onClick = { viewModel.onInput("9") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "×", onClick = { viewModel.onInput("×") }, modifier = Modifier.weight(1f), isOperator = true)
                }

                // Row 5
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcKey(text = "4", onClick = { viewModel.onInput("4") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "5", onClick = { viewModel.onInput("5") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "6", onClick = { viewModel.onInput("6") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "−", onClick = { viewModel.onInput("−") }, modifier = Modifier.weight(1f), isOperator = true)
                }

                // Row 6
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcKey(text = "1", onClick = { viewModel.onInput("1") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "2", onClick = { viewModel.onInput("2") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "3", onClick = { viewModel.onInput("3") }, modifier = Modifier.weight(1f))
                    CalcKey(text = "+", onClick = { viewModel.onInput("+") }, modifier = Modifier.weight(1f), isOperator = true)
                }

                // Row 7
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CalcKey(text = "0", onClick = { viewModel.onInput("0") }, modifier = Modifier.weight(1f))
                    CalcKey(text = ".", onClick = { viewModel.onInput(".") }, modifier = Modifier.weight(1f))
                    // Backspace icon key
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clickable { viewModel.onBackspace() }
                            .testTag("key_backspace"),
                        tonalElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    // Equals Primary Key
                    Button(
                        onClick = { viewModel.onEquals() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("key_equals")
                    ) {
                        Text("=", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    isSpecial: Boolean = false,
    isDanger: Boolean = false
) {
    val containerColor = when {
        isDanger -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        isOperator -> MaterialTheme.colorScheme.primaryContainer
        isSpecial -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isDanger -> MaterialTheme.colorScheme.error
        isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
        isSpecial -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .height(52.dp)
            .clickable(onClick = onClick)
            .testTag("key_$text"),
        tonalElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontSize = if (text.length > 2) 13.sp else 18.sp,
                fontWeight = if (isOperator || isDanger) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
