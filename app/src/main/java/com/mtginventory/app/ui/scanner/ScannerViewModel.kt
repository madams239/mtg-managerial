package com.mtginventory.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtginventory.app.data.processing.CardProcessingPipeline
import com.mtginventory.app.data.repository.CardRepository
import com.mtginventory.app.model.ScannedCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannerUiState(
    val scannedCards: List<ScannedCard> = emptyList(),
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val totalValue: Double = 0.0,
    val errorMessage: String? = null
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    val cardProcessingPipeline: CardProcessingPipeline
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun addScannedCards(cards: List<ScannedCard>) {
        val currentCards = _uiState.value.scannedCards.toMutableList()
        currentCards.addAll(cards)
        
        _uiState.value = _uiState.value.copy(
            scannedCards = currentCards,
            totalValue = currentCards.sumOf { it.price }
        )
    }

    fun removeCard(cardId: String) {
        val currentCards = _uiState.value.scannedCards.toMutableList()
        currentCards.removeAll { it.id == cardId }
        
        _uiState.value = _uiState.value.copy(
            scannedCards = currentCards,
            totalValue = currentCards.sumOf { it.price }
        )
    }

    fun clearCards() {
        _uiState.value = _uiState.value.copy(
            scannedCards = emptyList(),
            totalValue = 0.0
        )
    }

    fun saveCards() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                
                // Save cards to repository
                _uiState.value.scannedCards.forEach { scannedCard ->
                    cardRepository.saveCard(scannedCard)
                }
                
                // Clear scanned cards after saving
                _uiState.value = _uiState.value.copy(
                    scannedCards = emptyList(),
                    totalValue = 0.0,
                    isSaving = false
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to save cards: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun setProcessing(isProcessing: Boolean) {
        _uiState.value = _uiState.value.copy(isProcessing = isProcessing)
    }
}