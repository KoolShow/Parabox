package com.ojhdtapp.parabox.domain.use_case

import androidx.paging.*
import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.data.local.entity.ContactWithMessagesEntity
import com.ojhdtapp.parabox.domain.model.Contact
import com.ojhdtapp.parabox.domain.model.ContactWithMessages
import com.ojhdtapp.parabox.domain.model.Message
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMessages @Inject constructor(
    val repository: MainRepository
) {
    operator fun invoke(contact: Contact): Flow<Resource<ContactWithMessages>> {
        repository.getPluginConnectionObjectIdListByContactId(contactId = contact.contactId).also {
            return repository.getSpecifiedListOfContactWithMessages(it)
                .map<Resource<List<ContactWithMessages>>, Resource<ContactWithMessages>> { contactWithMessagesListResource ->
                    return@map when (contactWithMessagesListResource) {
                        is Resource.Error -> Resource.Error(contactWithMessagesListResource.message!!)
                        is Resource.Loading -> Resource.Loading()
                        is Resource.Success -> Resource.Success(
                            ContactWithMessages(
                                contact = contact,
                                messages = contactWithMessagesListResource.data?.fold(initial = mutableListOf<Message>()) { acc, contactWithMessages ->
                                    acc.addAll(contactWithMessages.messages)
                                    acc
                                }?.sortedBy { it.timestamp }?.toList() ?: emptyList<Message>()
                            )
                        )
                    }
                }
        }
    }
    // Must called within coroutine
    fun pluginConnectionObjectIdList(contact: Contact) : List<Long>{
        return repository.getPluginConnectionObjectIdListByContactId(contactId = contact.contactId)
    }

    fun pagingFlow(pluginConnectionObjectIdList: List<Long>): Flow<PagingData<Message>> {
            return Pager(
                PagingConfig(
                    pageSize = 20,
                    enablePlaceholders = true,
                    initialLoadSize = 20
                )
            ) { repository.getMessagesPagingSource(pluginConnectionObjectIdList) }.flow
                .map { pagingData ->
                    pagingData.map {
                        it.toMessage()
                    }
                }
        }
}