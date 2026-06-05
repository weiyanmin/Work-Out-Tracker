package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme

// Premium, modern design system color palettes matching the screenshot's exact vibe
val BlueGradStart = Color(0xFF2E59F8)      // Intense royal blue
val BlueGradEnd = Color(0xFF5E7DF3)        // Soft azure blue
val HighlightCoral = Color(0xFFFF6A55)     // Radiant coral/orange-red focal color
val CustomWhite = Color(0xFFFFFFFF)        // Pristine white basecard surface
val SoftGreyBlue = Color(0xFFF1F4FA)       // High-quality light slate/grey for inactive components
val DarkSlateText = Color(0xFF1E293B)      // Intense charcoal/black for primary typography
val LowContrastText = Color(0xFF94A3B8)    // Soft low contrast grey for details and labels
val ChartBlueBg = Color(0xFF4C6EF5)        // Solid indigo support container for weekly progress charts

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FitTimerApp()
            }
        }
    }
}

@Composable
fun FitTimerApp(viewModel: WorkoutViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(BlueGradStart, BlueGradEnd)
                    )
                )
        ) {
            Crossfade(
                targetState = state.phase,
                animationSpec = tween(durationMillis = 400),
                label = "workoutPhaseTransition"
            ) { phase ->
                when (phase) {
                    WorkoutPhase.IDLE -> {
                        SetupConfiguratorScreen(
                            state = state,
                            onSetsChange = { viewModel.setWorkoutParameters(it, state.targetCount, state.restSeconds) },
                            onTargetChange = { viewModel.setWorkoutParameters(state.totalSets, it, state.restSeconds) },
                            onRestChange = { viewModel.setWorkoutParameters(state.totalSets, state.targetCount, it) },
                            onCountDurationChange = { viewModel.setCountDuration(it) },
                            onStart = { viewModel.startWorkout() }
                        )
                    }
                    WorkoutPhase.EXERCISE, WorkoutPhase.REST -> {
                        ActiveWorkoutScreen(
                            state = state,
                            onTogglePause = { viewModel.togglePause() },
                            onSkip = { viewModel.skipPhase() },
                            onStop = { viewModel.resetWorkout() }
                        )
                    }
                    WorkoutPhase.FINISHED -> {
                        CelebrationScreen(
                            totalSets = state.totalSets,
                            targetCount = state.targetCount,
                            onReset = { viewModel.resetWorkout() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SetupConfiguratorScreen(
    state: WorkoutState,
    onSetsChange: (Int) -> Unit,
    onTargetChange: (Int) -> Unit,
    onRestChange: (Int) -> Unit,
    onCountDurationChange: (Float) -> Unit,
    onStart: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // TOP HALF: Beautiful Dial & Progress Overview with Gradient Base
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.44f)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Title
                Text(
                    text = "INTERVAL CONFIGURATION",
                    color = CustomWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                // HUD Circular Dial
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    // Background ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        drawCircle(
                            color = CustomWhite.copy(alpha = 0.2f),
                            radius = size.minDimension / 2 - strokeWidth / 2,
                            style = Stroke(width = strokeWidth)
                        )
                    }

                    // Dynamic sweep angle based on set count - scales from 0.15 to 1.0
                    val sweepProgress = (state.totalSets.toFloat() / 15f).coerceIn(0.15f, 1.0f)
                    val sweepAngle = sweepProgress * 360f

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val sizeDim = size.minDimension - strokeWidth
                        val topLeftOffset = Offset(strokeWidth / 2, strokeWidth / 2)
                        val arcSize = Size(sizeDim, sizeDim)

                        drawArc(
                            color = HighlightCoral,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeftOffset,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Inside Text content: dynamic summary of the configured session
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(CustomWhite)
                    ) {
                        Text(
                            text = "${state.totalSets * state.targetCount}",
                            color = HighlightCoral,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            text = "TOTAL REPS",
                            color = LowContrastText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${state.totalSets} Set${if (state.totalSets > 1) "s" else ""} • ${state.restSeconds}s Rest",
                            color = DarkSlateText.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // BOTTOM HALF: Pristine Rounded Bottom Sheet Container Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.56f),
            colors = CardDefaults.cardColors(containerColor = CustomWhite),
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Focus indicator Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(HighlightCoral)
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Workout Settings",
                        color = CustomWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Stepper Adjusters (Sets Count, Rep Counts, Rest Delay, Count Duration)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CompactStepperAdjuster(
                        label = "SETS COUNT",
                        currentVal = state.totalSets,
                        unitValue = " sets",
                        onValueChange = onSetsChange,
                        icon = Icons.Default.Repeat,
                        range = 1..99
                    )

                    CompactStepperAdjuster(
                        label = "REP COUNTS",
                        currentVal = state.targetCount,
                        unitValue = " reps",
                        onValueChange = onTargetChange,
                        icon = Icons.Default.Timer,
                        range = 1..999
                    )

                    CompactStepperAdjuster(
                        label = "REST DELAY",
                        currentVal = state.restSeconds,
                        unitValue = "s rest",
                        onValueChange = onRestChange,
                        icon = Icons.Default.Refresh,
                        range = 1..300
                    )

                    CountDurationAdjuster(
                        currentVal = state.countDurationSeconds,
                        onValueChange = onCountDurationChange
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // HIGH TACTILE START BUTTON CAPSULE
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onStart()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighlightCoral),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .shadow(4.dp, RoundedCornerShape(32.dp))
                        .testTag("start_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CustomWhite,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "START WORKOUT",
                            color = CustomWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun CompactStepperAdjuster(
    label: String,
    currentVal: Int,
    unitValue: String,
    onValueChange: (Int) -> Unit,
    icon: ImageVector,
    range: IntRange
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SoftGreyBlue)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HighlightCoral,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = DarkSlateText.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = {
                    if (currentVal > range.first) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onValueChange(currentVal - 1)
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(CustomWhite, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrement",
                    tint = DarkSlateText,
                    modifier = Modifier.size(14.dp)
                )
            }

            Text(
                text = "$currentVal$unitValue",
                color = DarkSlateText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.widthIn(min = 60.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = {
                    if (currentVal < range.last) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onValueChange(currentVal + 1)
                    }
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(CustomWhite, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increment",
                    tint = DarkSlateText,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun CountDurationAdjuster(
    currentVal: Float,
    onValueChange: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val options = listOf(0.5f, 1.0f, 1.5f, 2.0f, 3.0f)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SoftGreyBlue)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = HighlightCoral,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "COUNT DURATION",
                    color = DarkSlateText.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = {
                        if (currentVal > 0.15f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onValueChange((currentVal - 0.1f).coerceIn(0.1f, 10f))
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(CustomWhite, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrement count duration",
                        tint = DarkSlateText,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = String.format(java.util.Locale.US, "%.1fs", currentVal),
                    color = DarkSlateText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.widthIn(min = 60.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = {
                        if (currentVal < 9.9f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onValueChange((currentVal + 0.1f).coerceIn(0.1f, 10f))
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(CustomWhite, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increment count duration",
                        tint = DarkSlateText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Fast-select choices pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { opt ->
                val isSelected = Math.abs(currentVal - opt) < 0.05f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) HighlightCoral else CustomWhite)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onValueChange(opt)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1fs", opt),
                        color = if (isSelected) CustomWhite else DarkSlateText.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveWorkoutScreen(
    state: WorkoutState,
    onTogglePause: () -> Unit,
    onSkip: () -> Unit,
    onStop: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // TOP HEADER: Active State & Big Circular dial count tracker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.44f)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = if (state.phase == WorkoutPhase.EXERCISE) "SET ${state.currentSet} OF ${state.totalSets}" else "RECOVERY ACTIVE",
                    color = CustomWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Large Animated circle ring tracking countdown time
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(210.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        drawCircle(
                            color = CustomWhite.copy(alpha = 0.2f),
                            radius = size.minDimension / 2 - strokeWidth / 2,
                            style = Stroke(width = strokeWidth)
                        )
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val sizeDim = size.minDimension - strokeWidth
                        val topLeftOffset = Offset(strokeWidth / 2, strokeWidth / 2)
                        val arcSize = Size(sizeDim, sizeDim)

                        drawArc(
                            color = HighlightCoral,
                            startAngle = -90f,
                            sweepAngle = state.progress * 360f,
                            useCenter = false,
                            topLeft = topLeftOffset,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Large circular overlay for high readability numbers
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .size(170.dp)
                            .clip(CircleShape)
                            .background(CustomWhite)
                    ) {
                        Text(
                            text = if (state.phase == WorkoutPhase.EXERCISE) "${state.currentCount}" else "${state.currentCount}s",
                            color = HighlightCoral,
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (state.phase == WorkoutPhase.EXERCISE) "EXERCISING" else "RECOVERY REST",
                            color = LowContrastText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }
        }

        // BOTTOM PANEL: Pinned workout status controller actions
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.56f),
            colors = CardDefaults.cardColors(containerColor = CustomWhite),
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Focus Program Pill banner
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(HighlightCoral)
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Training HUD",
                        color = CustomWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                }

                // Active summary specs
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Text(
                        text = if (state.isPaused) "SESSION PAUSED" else "ACTIVE TRAINING UNDERWAY",
                        color = if (state.isPaused) HighlightCoral else DarkSlateText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (state.phase == WorkoutPhase.EXERCISE) {
                            "Push hard! Count target: ${state.targetCount} reps"
                        } else {
                            "Nice job. Take short breathers"
                        },
                        color = LowContrastText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Control actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Stop Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStop()
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .border(1.dp, LowContrastText.copy(alpha = 0.4f), CircleShape)
                            .background(CustomWhite, CircleShape)
                            .testTag("stop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Stop workout",
                            tint = DarkSlateText,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Play/Pause Center Trigger
                    Box(
                        modifier = Modifier
                            .size(78.dp)
                            .clip(CircleShape)
                            .background(HighlightCoral)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTogglePause()
                            }
                            .shadow(3.dp, CircleShape)
                            .testTag("pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (state.isPaused) "Resume" else "Pause",
                            tint = CustomWhite,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    // Skip current phase button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSkip()
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .border(1.dp, LowContrastText.copy(alpha = 0.4f), CircleShape)
                            .background(CustomWhite, CircleShape)
                            .testTag("skip_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Skip current phase",
                            tint = DarkSlateText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Text(
                    text = "TAP RED BUTTON TO PAUSE/RESUME ACCURATELY",
                    color = LowContrastText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun CelebrationScreen(
    totalSets: Int,
    targetCount: Int,
    onReset: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Star Success Indicator
        Box(
            modifier = Modifier
                .size(130.dp)
                .drawBehind {
                    drawCircle(
                        color = CustomWhite.copy(alpha = 0.15f),
                        radius = size.minDimension / 1.3f
                    )
                }
                .clip(CircleShape)
                .background(CustomWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Success Star",
                tint = Color(0xFFF1C40F),
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "WORKOUT COMPLETE!",
            color = CustomWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.8.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Awesome determination! High intensity intervals completed perfectly.",
            color = CustomWhite.copy(alpha = 0.85f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Spec Metrics Card Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CustomWhite),
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL SETS",
                        color = LowContrastText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "$totalSets",
                        color = DarkSlateText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(LowContrastText.copy(alpha = 0.3f))
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TOTAL REPS",
                        color = LowContrastText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "${totalSets * targetCount}",
                        color = DarkSlateText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(44.dp))

        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onReset()
            },
            colors = ButtonDefaults.buttonColors(containerColor = HighlightCoral),
            shape = RoundedCornerShape(32.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .shadow(4.dp, RoundedCornerShape(32.dp))
                .testTag("reset_button")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = CustomWhite,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "START NEW PROGRAM",
                color = CustomWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
