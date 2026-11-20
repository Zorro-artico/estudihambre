package com.example.ui.containers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NutriRepository
import com.example.domain.model.ContainerItem
import com.example.domain.model.ContainerShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContainerEditorState(
    val editingId: Long = 0,
    val name: String = "",
    val shape: ContainerShape = ContainerShape.CYLINDRICAL,
    val radiusText: String = "",
    val heightText: String = "",
    val lengthText: String = "",
    val widthText: String = "",
    val previewFillPercent: Int = 100,
    val isFavorite: Boolean = false
)

data class ContainersUiState(
    val containers: List<ContainerItem> = emptyList(),
    val editor: ContainerEditorState = ContainerEditorState(),
    val liveTotalVolumeMl: Double = 0.0,
    val liveOccupiedVolumeMl: Double = 0.0,
    val isEditorVisible: Boolean = false
)

class ContainersViewModel(private val repository: NutriRepository) : ViewModel() {

    private val editor = MutableStateFlow(ContainerEditorState())
    private val isEditorVisible = MutableStateFlow(false)

    val uiState: StateFlow<ContainersUiState> = combine(
        repository.containers,
        editor,
        isEditorVisible
    ) { list, ed, visible ->
        val r = ed.radiusText.replace(',', '.').toDoubleOrNull() ?: 0.0
        val h = ed.heightText.replace(',', '.').toDoubleOrNull() ?: 0.0
        val l = ed.lengthText.replace(',', '.').toDoubleOrNull() ?: 0.0
        val w = ed.widthText.replace(',', '.').toDoubleOrNull() ?: 0.0

        val totalMl = repository.geometryUseCase.calculateTotalVolumeMl(ed.shape, r, h, l, w)
        val occupiedMl = repository.geometryUseCase.calculateOccupiedVolumeMl(totalMl, ed.previewFillPercent)

        ContainersUiState(
            containers = list,
            editor = ed,
            liveTotalVolumeMl = totalMl,
            liveOccupiedVolumeMl = occupiedMl,
            isEditorVisible = visible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ContainersUiState()
    )

    fun openNewEditor() {
        editor.value = ContainerEditorState()
        isEditorVisible.value = true
    }

    fun openEditEditor(item: ContainerItem) {
        editor.value = ContainerEditorState(
            editingId = item.id,
            name = item.name,
            shape = item.shape,
            radiusText = if (item.radiusCm > 0) item.radiusCm.toString() else "",
            heightText = if (item.heightCm > 0) item.heightCm.toString() else "",
            lengthText = if (item.lengthCm > 0) item.lengthCm.toString() else "",
            widthText = if (item.widthCm > 0) item.widthCm.toString() else "",
            isFavorite = item.isFavorite
        )
        isEditorVisible.value = true
    }

    fun closeEditor() {
        isEditorVisible.value = false
    }

    fun updateName(v: String) = editor.update { it.copy(name = v) }
    fun updateShape(s: ContainerShape) = editor.update { it.copy(shape = s) }
    fun updateRadius(v: String) = editor.update { it.copy(radiusText = v) }
    fun updateHeight(v: String) = editor.update { it.copy(heightText = v) }
    fun updateLength(v: String) = editor.update { it.copy(lengthText = v) }
    fun updateWidth(v: String) = editor.update { it.copy(widthText = v) }
    fun updatePreviewFill(pct: Int) = editor.update { it.copy(previewFillPercent = pct) }
    fun updateFavorite(fav: Boolean) = editor.update { it.copy(isFavorite = fav) }

    fun saveEditor() {
        val ed = editor.value
        if (ed.name.isBlank()) return
        val r = ed.radiusText.replace(',', '.').toDoubleOrNull() ?: 0.0
        val h = ed.heightText.replace(',', '.').toDoubleOrNull() ?: 0.0
        val l = ed.lengthText.replace(',', '.').toDoubleOrNull() ?: 0.0
        val w = ed.widthText.replace(',', '.').toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            repository.saveContainer(
                ContainerItem(
                    id = ed.editingId,
                    name = ed.name.trim(),
                    shape = ed.shape,
                    radiusCm = r,
                    heightCm = h,
                    lengthCm = l,
                    widthCm = w,
                    isFavorite = ed.isFavorite
                )
            )
            isEditorVisible.value = false
        }
    }

    fun toggleFavorite(item: ContainerItem) {
        viewModelScope.launch { repository.toggleContainerFavorite(item) }
    }

    fun deleteContainer(id: Long) {
        viewModelScope.launch { repository.deleteContainer(id) }
    }

    fun addStudentPresets() {
        viewModelScope.launch { repository.addStudentContainerPresets() }
    }

    class Factory(private val repository: NutriRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ContainersViewModel(repository) as T
        }
    }
}
