package com.cname.buddy.ui.screens.vault

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cname.buddy.data.local.BuddyDatabase
import com.cname.buddy.data.local.PasswordDao
import com.cname.buddy.data.local.PasswordEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PasswordViewModel(private val dao: PasswordDao) : ViewModel() {

    val passwords: StateFlow<List<PasswordEntity>> = dao.getAllPasswords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun insert(password: PasswordEntity) = viewModelScope.launch { dao.insertPassword(password) }
    fun update(password: PasswordEntity) = viewModelScope.launch { dao.updatePassword(password) }
    fun delete(password: PasswordEntity) = viewModelScope.launch { dao.deletePassword(password) }
}

class PasswordViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val dao = BuddyDatabase.getDatabase(context).passwordDao()
        return PasswordViewModel(dao) as T
    }
}