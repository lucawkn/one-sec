package com.onesec.interceptor.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onesec.interceptor.data.local.dao.AppStatSummary
import com.onesec.interceptor.data.repository.StatsRange
import com.onesec.interceptor.di.ServiceLocator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel : ViewModel() {

    private val repository = ServiceLocator.statsRepository

    private val _range = MutableStateFlow(StatsRange.TODAY)
    val range: StateFlow<StatsRange> = _range

    val stats: StateFlow<List<AppStatSummary>> = _range
        .flatMapLatest { repository.observeStats(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setRange(range: StatsRange) {
        _range.value = range
    }
}
