package com.ojhdtapp.parabox.ui.message

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ojhdtapp.parabox.R
import com.ojhdtapp.parabox.core.util.DataStoreKeys
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.core.util.getDataStoreValue
import com.ojhdtapp.parabox.domain.model.ChatWithLatestMessage
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.use_case.GetChat
import com.ojhdtapp.parabox.domain.use_case.GetContact
import com.ojhdtapp.parabox.domain.use_case.UpdateChat
import com.ojhdtapp.parabox.ui.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MessagePageViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val getChat: GetChat,
    val getContact: GetContact,
    val updateChat: UpdateChat,
) : BaseViewModel<MessagePageState, MessagePageEvent, MessagePageEffect>() {

    override fun initialState(): MessagePageState {
        return MessagePageState(
            chatPagingDataFlow = getChat(listOf(GetChatFilter.Normal)).cachedIn(viewModelScope)
        )
    }

    init {
        sendEvent(MessagePageEvent.UpdateDataStore)
    }

    override suspend fun handleEvent(
        event: MessagePageEvent,
        state: MessagePageState
    ): MessagePageState? {
        return when (event) {
            is MessagePageEvent.UpdateDataStore -> {
                state.copy(
                    datastore = state.datastore.copy(
                        enableSwipeToDismiss = context.getDataStoreValue(
                            DataStoreKeys.SETTINGS_ENABLE_SWIPE_TO_DISMISS,
                            true
                        )
                    )
                )
            }

            is MessagePageEvent.OpenEnabledChatFilterDialog -> {
                state.copy(
                    openEnabledChatFilterDialog = event.open
                )
            }

            is MessagePageEvent.UpdateEnabledGetChatFilterList -> {
                val newList = state.selectedGetChatFilterList.toMutableList()
                    .apply {
                        retainAll(event.list)
                        if (isEmpty()) {
                            add(GetChatFilter.Normal)
                        }
                    }
                state.copy(
                    chatPagingDataFlow = getChat(newList),
                    enabledGetChatFilterList = event.list,
                    selectedGetChatFilterList = newList,
                )
            }

            is MessagePageEvent.AddOrRemoveSelectedGetChatFilter -> {
                if (event.filter is GetChatFilter.Normal) return state
                val newList = if (state.selectedGetChatFilterList.contains(event.filter)) {
                    state.selectedGetChatFilterList.toMutableList().apply {
                        remove(event.filter)
                    }
                } else {
                    state.selectedGetChatFilterList.toMutableList().apply {
                        add(event.filter)
                    }
                }.apply {
                    if (isEmpty()) {
                        add(GetChatFilter.Normal)
                    } else {
                        remove(GetChatFilter.Normal)
                    }
                }
                return state.copy(
                    chatPagingDataFlow = getChat(newList),
                    selectedGetChatFilterList = newList
                )
            }

            is MessagePageEvent.GetChatPagingDataFlow -> {
                return state.copy(
                    chatPagingDataFlow = getChat(state.selectedGetChatFilterList)
                )
            }

            is MessagePageEvent.UpdateChatUnreadMessagesNum -> {
                return coroutineScope {
                    val res = withContext(Dispatchers.IO) {
                        updateChat.unreadMessagesNum(event.chatId, event.value)
                    }
                    if (res) {
                        sendEffect(
                            MessagePageEffect.ShowSnackBar(
                                message = "操作成功",
                                label = context.getString(R.string.cancel),
                                callback = {
                                    launch(Dispatchers.IO) {
                                        updateChat.unreadMessagesNum(event.chatId, event.oldValue)
                                    }
                                })
                        )
                    } else {
                        sendEffect(MessagePageEffect.ShowSnackBar(message = "操作失败"))
                    }
                    state
                }
            }

            is MessagePageEvent.UpdateChatPin -> {
                return coroutineScope {
                    val res = withContext(Dispatchers.IO) {
                        updateChat.pin(event.chatId, event.value)
                    }
                    if (res) {
                        sendEffect(
                            MessagePageEffect.ShowSnackBar(
                                message = "操作成功",
                                label = context.getString(R.string.cancel),
                                callback = {
                                    launch(Dispatchers.IO) {
                                        updateChat.pin(event.chatId, event.oldValue)
                                    }
                                })
                        )
                    } else {
                        sendEffect(MessagePageEffect.ShowSnackBar(message = "操作失败"))
                    }
                    state
                }
            }

            is MessagePageEvent.UpdateChatHide -> {
                return coroutineScope {
                    val res = withContext(Dispatchers.IO) {
                        updateChat.hide(event.chatId, event.value)
                    }
                    if (res) {
                        sendEffect(
                            MessagePageEffect.ShowSnackBar(
                                message = "操作成功",
                                label = context.getString(R.string.cancel),
                                callback = {
                                    launch(Dispatchers.IO) {
                                        updateChat.hide(event.chatId, event.oldValue)
                                    }
                                })
                        )
                    } else {
                        sendEffect(MessagePageEffect.ShowSnackBar(message = "操作失败"))
                    }
                    state
                }
            }

            is MessagePageEvent.UpdateChatArchive -> {
                return coroutineScope {
                    val res = withContext(Dispatchers.IO) {
                        updateChat.archive(event.chatId, event.value)
                    }
                    if (res) {
                        sendEffect(
                            MessagePageEffect.ShowSnackBar(
                                message = "操作成功",
                                label = context.getString(R.string.cancel),
                                callback = {
                                    launch(Dispatchers.IO) {
                                        updateChat.archive(event.chatId, event.oldValue)
                                    }
                                })
                        )
                    } else {
                        sendEffect(MessagePageEffect.ShowSnackBar(message = "操作失败"))
                    }
                    state
                }
            }

            is MessagePageEvent.UpdateChatTags -> {
                return coroutineScope {
                    val res = withContext(Dispatchers.IO) {
                        updateChat.tags(event.chatId, event.value)
                    }
                    if (res) {
                        sendEffect(
                            MessagePageEffect.ShowSnackBar(
                                message = "操作成功",
                                label = context.getString(R.string.cancel),
                                callback = {
                                    launch(Dispatchers.IO) {
                                        updateChat.tags(event.chatId, event.oldValue)
                                    }
                                })
                        )
                    } else {
                        sendEffect(MessagePageEffect.ShowSnackBar(message = "操作失败"))
                    }
                    state
                }
            }
        }
    }

    private val chatLatestMessageSenderMap = mutableMapOf<Long, Resource<Contact>>()

    fun getLatestMessageSenderWithCache(senderId: Long?): Flow<Resource<Contact>> {
        return flow {
            if (senderId == null) {
                emit(Resource.Error("no sender"))
            } else {
                if (chatLatestMessageSenderMap[senderId] != null) {
                    emit(chatLatestMessageSenderMap[senderId]!!)
                } else {
                    emitAll(
                        getContact.byId(senderId).onEach {
                            if (it is Resource.Success) {
                                chatLatestMessageSenderMap[senderId] = it
                            }
                        }
                    )
                }
            }
        }
    }
}