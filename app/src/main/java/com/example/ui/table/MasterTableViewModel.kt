package com.example.ui.table

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NutriRepository
import com.example.domain.model.MasterTableIntervalStats
import com.example.domain.model.MasterTableRow
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TableIntervalFilter(val labelEs: String) {
    ALL("Todo el historial"),
    SINCE_LAST_WEIGHT("Desde último pesaje"),
    LAST_7_DAYS("Últimos 7 días"),
    LAST_14_DAYS("Últimos 14 días"),
    LAST_30_DAYS("Últimos 30 días"),
    CUSTOM("Rango personalizado")
}

data class MasterTableUiState(
    val allRows: List<MasterTableRow> = emptyList(),
    val filteredRows: List<MasterTableRow> = emptyList(),
    val intervalStats: MasterTableIntervalStats = MasterTableIntervalStats(0, 0, 0.0, 0, 0, 0.0, 0.0, null),
    val activeFilter: TableIntervalFilter = TableIntervalFilter.ALL,
    val customStartDate: String = DateUtils.shiftLogicalDate(DateUtils.todayLogicalDate(), -14),
    val customEndDate: String = DateUtils.todayLogicalDate(),
    val excelFeedback: String? = null
)

class MasterTableViewModel(private val repository: NutriRepository) : ViewModel() {

    private val activeFilter = MutableStateFlow(TableIntervalFilter.ALL)
    private val customStartDate = MutableStateFlow(DateUtils.shiftLogicalDate(DateUtils.todayLogicalDate(), -14))
    private val customEndDate = MutableStateFlow(DateUtils.todayLogicalDate())
    private val excelFeedback = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MasterTableUiState> = combine(
        repository.masterTableRows,
        activeFilter,
        customStartDate,
        customEndDate,
        excelFeedback
    ) { rows, filter, start, end, msg ->
        val today = DateUtils.todayLogicalDate()
        val filtered = when (filter) {
            TableIntervalFilter.ALL -> rows
            TableIntervalFilter.SINCE_LAST_WEIGHT -> {
                val lastWeightDate = rows.firstOrNull { it.weightKg != null }?.logicalDate
                if (lastWeightDate == null) rows else rows.filter { it.logicalDate >= lastWeightDate }
            }
            TableIntervalFilter.LAST_7_DAYS -> {
                val cutoff = DateUtils.shiftLogicalDate(today, -6)
                rows.filter { it.logicalDate >= cutoff }
            }
            TableIntervalFilter.LAST_14_DAYS -> {
                val cutoff = DateUtils.shiftLogicalDate(today, -13)
                rows.filter { it.logicalDate >= cutoff }
            }
            TableIntervalFilter.LAST_30_DAYS -> {
                val cutoff = DateUtils.shiftLogicalDate(today, -29)
                rows.filter { it.logicalDate >= cutoff }
            }
            TableIntervalFilter.CUSTOM -> {
                val minD = minOf(start, end)
                val maxD = maxOf(start, end)
                rows.filter { it.logicalDate in minD..maxD }
            }
        }
        val stats = repository.masterTableStatsUseCase.calculateIntervalStats(filtered)
        MasterTableUiState(
            allRows = rows,
            filteredRows = filtered,
            intervalStats = stats,
            activeFilter = filter,
            customStartDate = start,
            customEndDate = end,
            excelFeedback = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MasterTableUiState()
    )

    fun selectFilter(filter: TableIntervalFilter) {
        activeFilter.value = filter
    }

    fun updateCustomRange(startDate: String, endDate: String) {
        if (DateUtils.isValidIsoDate(startDate)) customStartDate.value = startDate
        if (DateUtils.isValidIsoDate(endDate)) customEndDate.value = endDate
        activeFilter.value = TableIntervalFilter.CUSTOM
    }

    fun exportExcel(uri: Uri) {
        viewModelScope.launch {
            val rows = uiState.value.filteredRows.ifEmpty { uiState.value.allRows }
            repository.exportExcel(uri, rows)
                .onSuccess { count ->
                    excelFeedback.value = "Exportadas $count jornadas a Excel (.xlsx) correctamente."
                }
                .onFailure { err ->
                    excelFeedback.value = "Error al exportar .xlsx: ${err.localizedMessage}"
                }
        }
    }

    fun importExcel(uri: Uri) {
        viewModelScope.launch {
            repository.importExcel(uri)
                .onSuccess { count ->
                    excelFeedback.value = "Importadas $count filas desde Excel (.xlsx) al historial."
                }
                .onFailure { err ->
                    excelFeedback.value = "Error al importar .xlsx: ${err.localizedMessage}"
                }
        }
    }

    fun clearFeedback() {
        excelFeedback.value = null
    }

    class Factory(private val repository: NutriRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MasterTableViewModel(repository) as T
        }
    }
}
