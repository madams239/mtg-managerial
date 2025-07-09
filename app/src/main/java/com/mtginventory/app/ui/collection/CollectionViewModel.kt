package com.mtginventory.app.ui.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mtginventory.app.data.repository.CardRepository
import com.mtginventory.app.model.ScannedCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionUiState(
    val cards: List<ScannedCard> = emptyList(),
    val totalValue: Double = 0.0,
    val totalCards: Int = 0,
    val rarityDistribution: Map<String, Int> = emptyMap(),
    val topSets: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedSortOption: SortOption = SortOption.NAME,
    val selectedRarityFilter: String? = null
)

enum class SortOption(val displayName: String) {
    NAME("Name"),
    SET("Set"),
    RARITY("Rarity"),
    PRICE("Price"),
    DATE_ADDED("Recently Added")
}

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    init {
        loadCollection()
        loadCollectionStats()
    }

    private fun loadCollection() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                cardRepository.getAllCards()
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load collection: ${e.message}"
                        )
                    }
                    .collect { cards ->
                        val filteredAndSortedCards = applyFiltersAndSorting(cards)
                        _uiState.value = _uiState.value.copy(
                            cards = filteredAndSortedCards,
                            totalCards = cards.size,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load collection: ${e.message}"
                )
            }
        }
    }

    private fun loadCollectionStats() {
        viewModelScope.launch {
            try {
                val totalValue = cardRepository.getCollectionValue()
                val rarityDistribution = cardRepository.getRarityDistribution()
                val topSets = cardRepository.getTopSets()

                _uiState.value = _uiState.value.copy(
                    totalValue = totalValue,
                    rarityDistribution = rarityDistribution,
                    topSets = topSets
                )
            } catch (e: Exception) {
                // Stats loading failed, but don't show error - collection data is more important
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            loadCollection()
        } else {
            searchCards(query)
        }
    }

    private fun searchCards(query: String) {
        viewModelScope.launch {
            try {
                // Debounce search to avoid excessive API calls
                kotlinx.coroutines.delay(300)
                
                // Check if query is still the same after debounce
                if (_uiState.value.searchQuery != query) return@launch
                
                cardRepository.searchCards(query)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Search failed: ${e.message}"
                        )
                    }
                    .collect { cards ->
                        val filteredAndSortedCards = applyFiltersAndSorting(cards)
                        _uiState.value = _uiState.value.copy(
                            cards = filteredAndSortedCards,
                            errorMessage = null
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Search failed: ${e.message}"
                )
            }
        }
    }

    fun updateSortOption(sortOption: SortOption) {
        _uiState.value = _uiState.value.copy(selectedSortOption = sortOption)
        val sortedCards = applyFiltersAndSorting(_uiState.value.cards)
        _uiState.value = _uiState.value.copy(cards = sortedCards)
    }

    fun updateRarityFilter(rarity: String?) {
        _uiState.value = _uiState.value.copy(selectedRarityFilter = rarity)
        if (_uiState.value.searchQuery.isBlank()) {
            loadCollection()
        } else {
            searchCards(_uiState.value.searchQuery)
        }
    }

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            try {
                val result = cardRepository.deleteCard(cardId)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete card: ${result.exceptionOrNull()?.message}"
                    )
                } else {
                    // Card will be automatically removed from the flow
                    loadCollectionStats() // Refresh stats
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete card: ${e.message}"
                )
            }
        }
    }

    fun updateCardQuantity(cardId: String, quantity: Int) {
        viewModelScope.launch {
            try {
                val result = cardRepository.updateCardQuantity(cardId, quantity)
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to update quantity: ${result.exceptionOrNull()?.message}"
                    )
                } else {
                    loadCollectionStats() // Refresh stats
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update quantity: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun refreshCollection() {
        loadCollection()
        loadCollectionStats()
    }

    private fun applyFiltersAndSorting(cards: List<ScannedCard>): List<ScannedCard> {
        var result = cards

        // Apply rarity filter
        _uiState.value.selectedRarityFilter?.let { rarity ->
            result = result.filter { it.rarity.equals(rarity, ignoreCase = true) }
        }

        // Apply sorting
        result = when (_uiState.value.selectedSortOption) {
            SortOption.NAME -> result.sortedBy { it.name }
            SortOption.SET -> result.sortedBy { it.setName }
            SortOption.RARITY -> result.sortedBy { 
                when (it.rarity.lowercase()) {
                    "common" -> 1
                    "uncommon" -> 2
                    "rare" -> 3
                    "mythic" -> 4
                    else -> 5
                }
            }
            SortOption.PRICE -> result.sortedByDescending { it.price }
            SortOption.DATE_ADDED -> result.sortedByDescending { it.id } // Assuming newer cards have larger IDs
        }

        return result
    }
}