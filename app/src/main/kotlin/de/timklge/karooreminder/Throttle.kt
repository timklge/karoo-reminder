package de.timklge.karooreminder

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun<T> Flow<T>.throttle(timeout: Long): Flow<T> = flow {
    var lastEmissionTime = 0L

    collect { value ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEmissionTime >= timeout) {
            emit(value)
            lastEmissionTime = currentTime
        }
    }
}

fun<T> Flow<T>.onlyIfNValuesReceivedWithinTimeframe(n: Int, timeframe: Long): Flow<T> = flow {
    val lastValuesReceivedAt = mutableListOf<Long>()

    collect { value ->
        val currentTime = System.currentTimeMillis()

        lastValuesReceivedAt.removeAll { it + timeframe < currentTime }
        lastValuesReceivedAt.add(currentTime)

        if (lastValuesReceivedAt.size >= n) {
            emit(value)
        }
    }
}