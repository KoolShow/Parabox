package com.ojhdtapp.paraboxdevelopmentkit.model

data class ParaboxResult(
    val code: Int,
    val message: String,
){
    companion object{
        const val SUCCESS = 10000
        const val ERROR_UNINITIALIZED = 20001

        const val ERROR_UNINITIALIZED_MSG = "Uninitialized"
    }
}
