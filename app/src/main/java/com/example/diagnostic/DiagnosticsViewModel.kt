package com.example.diagnostic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TuningState {
    object Idle : TuningState()
    data class TuningInProgress(val progress: Int, val logMessage: String) : TuningState()
    data class Completed(val message: String) : TuningState()
}

data class DashboardUiState(
    val deviceSummary: DeviceSummary,
    val hardwareSpecs: List<HardwareSpecCard>,
    val thermalProfile: ThermalProfile,
    val diagnosticsList: List<DiagnosticTestItem> = emptyList(),
    val isRunningDiagnostics: Boolean = false,
    val currentSelectedCard: HardwareSpecCard? = null,
    val tuningState: TuningState = TuningState.Idle,
    val overallScore: Int = 98,
    val systemStatusText: String = "All drivers calibrated & running optimally"
)

class DiagnosticsViewModel(application: Application) : AndroidViewModel(application) {

    private val diagnostics = SystemDiagnostics(application.applicationContext)

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            deviceSummary = diagnostics.getDeviceSummary(),
            hardwareSpecs = diagnostics.queryHardwareStats(),
            thermalProfile = diagnostics.getThermalProfile()
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Initial load of diagnostic tasks
        refreshSpecs()
    }

    fun refreshSpecs() {
        viewModelScope.launch {
            val specs = diagnostics.queryHardwareStats()
            val thermals = diagnostics.getThermalProfile()
            
            // Calculate a relative health score based on memory usage, storage used, battery temp etc.
            var baseScore = 100
            
            // Deduct for high ram utilization
            val ramCard = specs.firstOrNull { it.iconName == "ram" }
            if (ramCard?.status == "Warning") baseScore -= 5
            
            // Deduct for battery temperature
            val batteryCard = specs.firstOrNull { it.iconName == "battery" }
            val batTempStr = batteryCard?.metadata?.get("Temperature") ?: ""
            val batTemp = batTempStr.replace(" °C", "").toDoubleOrNull() ?: 30.0
            if (batTemp > 44) baseScore -= 15
            else if (batTemp > 38) baseScore -= 5

            val score = baseScore.coerceIn(40, 100)
            val statusText = when {
                score >= 95 -> "Excellent Performance • Snapdragon Core at Maximum Health"
                score >= 85 -> "Balanced Performance • Device is fully optimal"
                else -> "Moderate Load • Calibration recommended below"
            }

            _uiState.value = _uiState.value.copy(
                hardwareSpecs = specs,
                thermalProfile = thermals,
                overallScore = score,
                systemStatusText = statusText
            )
        }
    }

    fun runAutomatedCheckups() {
        if (_uiState.value.isRunningDiagnostics) return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRunningDiagnostics = true,
                systemStatusText = "Executing automated subsystem diagnostic suite..."
            )

            diagnostics.runAutomatedDiagnostics().collect { updatedTests ->
                // Look for any warnings or failures to adjust the status
                val hasWarning = updatedTests.any { it.status == DiagnosticStatus.WARNING }
                val hasFailed = updatedTests.any { it.status == DiagnosticStatus.FAILED }
                
                val currentStatus = when {
                    hasFailed -> "Subsystem failures detected! Action required."
                    hasWarning -> "subsystem warnings detected. Tune-up recommended."
                    else -> "Subsystem diagnostics completed successfully."
                }

                _uiState.value = _uiState.value.copy(
                    diagnosticsList = updatedTests,
                    systemStatusText = currentStatus
                )
            }

            _uiState.value = _uiState.value.copy(
                isRunningDiagnostics = false
            )
        }
    }

    fun optimizeSystemDrivers() {
        if (_uiState.value.tuningState is TuningState.TuningInProgress) return

        viewModelScope.launch {
            diagnostics.runPerformanceBoostOptimization().collect { (progress, message) ->
                _uiState.value = _uiState.value.copy(
                    tuningState = TuningState.TuningInProgress(progress, message)
                )
                if (progress == 100) {
                    delay(800) // let user absorb the completion state
                    _uiState.value = _uiState.value.copy(
                        tuningState = TuningState.Completed(message)
                    )
                    // Refresh stats to show freed memory
                    refreshSpecs()
                }
            }
        }
    }

    fun dismissTuning() {
        _uiState.value = _uiState.value.copy(
            tuningState = TuningState.Idle
        )
    }

    fun selectHardwareSpecCard(card: HardwareSpecCard?) {
        _uiState.value = _uiState.value.copy(
            currentSelectedCard = card
        )
    }
}
