package com.example

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.AudioManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkoutPhase {
    IDLE,
    EXERCISE,
    REST,
    FINISHED
}

enum class PresetType {
    CARDIO,
    POWER,
    ENDURANCE
}

data class WorkoutState(
    val totalSets: Int = 3,
    val targetCount: Int = 15,
    val restSeconds: Int = 10,
    val currentSet: Int = 1,
    val currentCount: Int = 1,
    val phase: WorkoutPhase = WorkoutPhase.IDLE,
    val isPaused: Boolean = false,
    val progress: Float = 0f,
    val presetType: PresetType = PresetType.POWER,
    val countDurationSeconds: Float = 1.0f
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(WorkoutState())
    val state: StateFlow<WorkoutState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun selectPreset(preset: PresetType) {
        if (_state.value.phase == WorkoutPhase.IDLE) {
            val (sets, target, rest) = when (preset) {
                PresetType.CARDIO -> Triple(4, 30, 15)
                PresetType.POWER -> Triple(3, 15, 10)
                PresetType.ENDURANCE -> Triple(5, 20, 20)
            }
            _state.update {
                it.copy(
                    presetType = preset,
                    totalSets = sets,
                    targetCount = target,
                    restSeconds = rest
                )
            }
        }
    }

    fun cyclePreset(forward: Boolean) {
        if (_state.value.phase == WorkoutPhase.IDLE) {
            val presets = PresetType.values()
            val currentIndex = presets.indexOf(_state.value.presetType)
            val nextIndex = if (forward) {
                (currentIndex + 1) % presets.size
            } else {
                (currentIndex - 1 + presets.size) % presets.size
            }
            selectPreset(presets[nextIndex])
        }
    }

    enum class BeepType {
        TICK,
        REST_ALERT,
        WARNING,
        COMPLETED
    }

    private fun playSynthesizedTone(frequency: Double, durationMs: Int, volume: Float = 0.8f) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                val sampleRate = 8000
                val numSamples = (durationMs * sampleRate / 1000)
                val generatedSnd = ShortArray(numSamples)
                
                for (i in 0 until numSamples) {
                    val angle = 2.0 * Math.PI * i / (sampleRate / frequency)
                    generatedSnd[i] = (Math.sin(angle) * Short.MAX_VALUE * volume).toInt().toShort()
                }
                
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                
                audioTrack.write(generatedSnd, 0, numSamples)
                audioTrack.play()
                
                delay(durationMs + 100L)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {
                    // Ignore release exceptions
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playBeep(type: BeepType) {
        when (type) {
            BeepType.TICK -> {
                // High short sports click/tick
                playSynthesizedTone(frequency = 1200.0, durationMs = 80, volume = 0.8f)
            }
            BeepType.REST_ALERT -> {
                // Coordinated double audio alert
                viewModelScope.launch {
                    playSynthesizedTone(frequency = 600.0, durationMs = 120, volume = 0.8f)
                    delay(180)
                    playSynthesizedTone(frequency = 900.0, durationMs = 150, volume = 0.8f)
                }
            }
            BeepType.WARNING -> {
                // Warning pulses
                playSynthesizedTone(frequency = 550.0, durationMs = 100, volume = 0.7f)
            }
            BeepType.COMPLETED -> {
                // Fanfare celebration sound structure
                viewModelScope.launch {
                    playSynthesizedTone(frequency = 700.0, durationMs = 100, volume = 0.8f)
                    delay(150)
                    playSynthesizedTone(frequency = 900.0, durationMs = 100, volume = 0.8f)
                    delay(150)
                    playSynthesizedTone(frequency = 1300.0, durationMs = 350, volume = 0.9f)
                }
            }
        }
    }

    fun setWorkoutParameters(sets: Int, target: Int, rest: Int) {
        if (_state.value.phase == WorkoutPhase.IDLE) {
            _state.update {
                it.copy(
                    totalSets = sets.coerceIn(1, 99),
                    targetCount = target.coerceIn(1, 999),
                    restSeconds = rest.coerceIn(1, 300)
                )
            }
        }
    }

    fun setCountDuration(duration: Float) {
        if (_state.value.phase == WorkoutPhase.IDLE) {
            _state.update {
                it.copy(
                    countDurationSeconds = duration.coerceIn(0.1f, 10.0f)
                )
            }
        }
    }

    fun startWorkout() {
        timerJob?.cancel()
        _state.update {
            it.copy(
                phase = WorkoutPhase.EXERCISE,
                currentSet = 1,
                currentCount = 1,
                isPaused = false,
                progress = 0f
            )
        }
        playBeep(BeepType.TICK)
        runWorkoutLoop()
    }

    fun togglePause() {
        _state.update {
            it.copy(isPaused = !it.isPaused)
        }
    }

    fun skipPhase() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.phase == WorkoutPhase.EXERCISE) {
                if (currentState.currentSet < currentState.totalSets) {
                    playBeep(BeepType.REST_ALERT)
                    _state.update {
                        it.copy(
                            phase = WorkoutPhase.REST,
                            currentCount = currentState.restSeconds,
                            progress = 1.0f
                        )
                    }
                } else {
                    finishWorkout()
                }
            } else if (currentState.phase == WorkoutPhase.REST) {
                val nextSet = currentState.currentSet + 1
                playBeep(BeepType.TICK)
                _state.update {
                    it.copy(
                        phase = WorkoutPhase.EXERCISE,
                        currentSet = nextSet,
                        currentCount = 1,
                        progress = 0f
                    )
                }
            }
        }
    }

    fun resetWorkout() {
        timerJob?.cancel()
        _state.update {
            it.copy(
                phase = WorkoutPhase.IDLE,
                currentSet = 1,
                currentCount = 1,
                isPaused = false,
                progress = 0f
            )
        }
    }

    private fun finishWorkout() {
        timerJob?.cancel()
        playBeep(BeepType.COMPLETED)
        _state.update {
            it.copy(
                phase = WorkoutPhase.FINISHED,
                progress = 1.0f
            )
        }
    }

    private fun runWorkoutLoop() {
        timerJob = viewModelScope.launch {
            while (true) {
                val currentState = _state.value
                val phase = currentState.phase
                val isPaused = currentState.isPaused

                if (phase == WorkoutPhase.IDLE || phase == WorkoutPhase.FINISHED) {
                    break
                }

                if (isPaused) {
                    delay(100)
                    continue
                }

                if (phase == WorkoutPhase.EXERCISE) {
                    // Count interval is dynamic
                    val totalDuration = (currentState.countDurationSeconds * 1000).toInt()
                    val tickInterval = 30
                    var progressMs = 0
                    
                    while (progressMs < totalDuration) {
                        if (_state.value.isPaused) {
                            delay(100)
                            continue
                        }
                        if (_state.value.phase != WorkoutPhase.EXERCISE) {
                            break
                        }
                        delay(tickInterval.toLong())
                        progressMs += tickInterval
                        _state.update {
                            it.copy(
                                progress = (progressMs.toFloat() / totalDuration).coerceIn(0f, 1f)
                            )
                        }
                    }

                    if (_state.value.phase != WorkoutPhase.EXERCISE) continue
                    if (_state.value.isPaused) continue

                    val nextCount = _state.value.currentCount + 1
                    if (nextCount <= _state.value.targetCount) {
                        _state.update {
                            it.copy(
                                currentCount = nextCount,
                                progress = 0f
                            )
                        }
                        playBeep(BeepType.TICK)
                    } else {
                        // Completed active count target for this set
                        if (_state.value.currentSet < _state.value.totalSets) {
                            playBeep(BeepType.REST_ALERT)
                            _state.update {
                                it.copy(
                                    phase = WorkoutPhase.REST,
                                    currentCount = _state.value.restSeconds,
                                    progress = 1.0f
                                )
                            }
                        } else {
                            finishWorkout()
                        }
                    }
                } else if (phase == WorkoutPhase.REST) {
                    // Rest countdown tick is 1.0 seconds (1000ms) per second
                    val totalDuration = 1000
                    val tickInterval = 30
                    var progressMs = 0
                    
                    while (progressMs < totalDuration) {
                        if (_state.value.isPaused) {
                            delay(100)
                            continue
                        }
                        if (_state.value.phase != WorkoutPhase.REST) {
                            break
                        }
                        delay(tickInterval.toLong())
                        progressMs += tickInterval
                        _state.update {
                            it.copy(
                                progress = (1.0f - (progressMs.toFloat() / totalDuration)).coerceIn(0f, 1f)
                            )
                        }
                    }

                    if (_state.value.phase != WorkoutPhase.REST) continue
                    if (_state.value.isPaused) continue

                    val nextRest = _state.value.currentCount - 1
                    if (nextRest > 0) {
                        // If rest is in last 3 seconds of countdown, sound warning pulses
                        if (nextRest <= 3) {
                            playBeep(BeepType.WARNING)
                        }
                        _state.update {
                            it.copy(
                                currentCount = nextRest,
                                progress = 1.0f
                            )
                        }
                    } else {
                        // Rest completes! Transition to next active set count 1
                        val nextSet = _state.value.currentSet + 1
                        playBeep(BeepType.TICK)
                        _state.update {
                            it.copy(
                                phase = WorkoutPhase.EXERCISE,
                                currentSet = nextSet,
                                currentCount = 1,
                                progress = 0f
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
