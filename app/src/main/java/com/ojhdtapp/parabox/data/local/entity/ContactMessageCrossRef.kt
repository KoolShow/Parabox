package com.ojhdtapp.parabox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "contact_message_cross_ref",primaryKeys = ["contactId", "messageId"])
data class ContactMessageCrossRef(
    @ColumnInfo(index = true)
    val contactId: Long,
    @ColumnInfo(index = true)
    val messageId: Long,
)
