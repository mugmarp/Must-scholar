package com.must.timetable.core.network

import retrofit2.Response

object SafeApiCall {
    suspend fun <T> execute(block: suspend () -> Response<T>): Response<T>? {
        return try {
            block()
        } catch (e: Exception) {
            null
        }
    }
}