package com.informatika.sars.viewmodel

import androidx.lifecycle.ViewModel
import com.informatika.sars.service.NotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NotificationViewModel(private val notificationService: NotificationService) : ViewModel() {
    private val _notifications = MutableStateFlow<List<String>>(emptyList())
    val notifications: StateFlow<List<String>> = _notifications

    fun triggerNotification(title: String, message: String) {
        notificationService.showNotification(title, message)
        _notifications.value = _notifications.value + "$title: $message"
    }
}
