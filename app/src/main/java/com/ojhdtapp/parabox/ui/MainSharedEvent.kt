package com.ojhdtapp.parabox.ui

import com.ojhdtapp.parabox.domain.model.Chat
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.QueryMessage
import com.ojhdtapp.parabox.ui.base.UiEvent

sealed interface MainSharedEvent : UiEvent {
    data class QueryInput(val input: String) : MainSharedEvent
    data class SearchConfirm(val input: String) : MainSharedEvent
    data class MessageSearchDone(val res: List<QueryMessage>, val isSuccess: Boolean) :
        MainSharedEvent

    data class ContactSearchDone(val res: List<Contact>, val isSuccess: Boolean) : MainSharedEvent
    data class ChatSearchDone(val res: List<Chat>, val isSuccess: Boolean) : MainSharedEvent
    data class TriggerSearchBar(val isActive: Boolean) : MainSharedEvent
}