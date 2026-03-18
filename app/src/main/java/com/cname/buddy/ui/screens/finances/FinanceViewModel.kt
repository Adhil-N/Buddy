package com.cname.buddy.ui.screens.finances

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cname.buddy.data.local.BuddyDatabase
import com.cname.buddy.data.local.FinanceDao
import com.cname.buddy.data.local.FinanceEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(private val dao: FinanceDao) : ViewModel() {

    // This automatically grabs the list from Room and converts it to Compose State!
    val finances: StateFlow<List<FinanceEntity>> = dao.getAllFinances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(finance: FinanceEntity) = viewModelScope.launch { dao.insertFinance(finance) }
    fun update(finance: FinanceEntity) = viewModelScope.launch { dao.updateFinance(finance) }
    fun delete(finance: FinanceEntity) = viewModelScope.launch { dao.deleteFinance(finance) }
}

// We need a Factory to tell Android how to build this ViewModel with our Room Database
class FinanceViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val dao = BuddyDatabase.getDatabase(context).financeDao()
        return FinanceViewModel(dao) as T
    }
}