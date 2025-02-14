package com.ojhdtapp.parabox.domain.use_case

import com.ojhdtapp.parabox.core.util.Resource
import com.ojhdtapp.parabox.domain.model.PluginConnection
import com.ojhdtapp.parabox.domain.repository.MainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GroupNewContact @Inject constructor(
    val repository: MainRepository
) {
    operator fun invoke(
        name: String,
        pluginConnections: List<PluginConnection>,
        senderId: Long,
        avatar: String? = null,
        avatarUri: String? = null,
        tags: List<String>,
        contactId: Long? = null
    ): Flow<Resource<Boolean>> {
        return flow {
            emit(Resource.Loading())
            repository.groupNewContact(name, pluginConnections, senderId, avatar, avatarUri, tags, contactId).let {
                if (it) {
                    emit(Resource.Success(true))
                } else {
                    emit(Resource.Error(message = "修改数据时发生错误"))
                }
            }
        }
    }
}