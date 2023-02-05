package ru.yandex.disk.trash

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class TestTrashManager : TrashManager {

    enum class NetworkRequestPolicy {
        SIMULATE_IN_PROGRESS, SUCCESS, NETWORK_ERROR, SERVER_ERROR, SIMULATE_IN_PROGRESS_WITH_NETWORK_ERROR
    }

    enum class NetworkCall {
        LOAD, CLEAR, RESTORE, CLEAR_ALL
    }

    private val requestPolicy = mutableMapOf<NetworkCall, NetworkRequestPolicy>()
    private val trashItems = mutableListOf<TrashFile>()
    private var defaultRequestPolicy = NetworkRequestPolicy.SUCCESS
    private var suspendedContinuations = listOf<() -> Unit>()

    fun addFilesToTrash(startId: Int, count: Int, isDir: Boolean = false): List<TrashFile> {
        return List(count) { i -> createTestFile(startId + i, isDir = isDir) }
            .also { trashItems.addAll(it) }
    }

    fun setRequestPolicy(call: NetworkCall, policy: NetworkRequestPolicy) {
        requestPolicy[call] = policy
    }

    fun setDefaultRequestPolicy(policy: NetworkRequestPolicy) {
        defaultRequestPolicy = policy
    }

    fun resumeSuspended() {
        val continuationsToResume = suspendedContinuations
        suspendedContinuations = emptyList()
        continuationsToResume.forEach {
            it.invoke()
        }
    }

    override suspend fun loadTrashData(): TrashInfo {
        return performNetworkAction(NetworkCall.LOAD) {
            TrashInfo(trashItems.toList())
        }
    }

    override suspend fun restoreItems(items: List<TrashFile>): DiskTrashManager.OperationsResult {
        return performNetworkAction(NetworkCall.RESTORE) {
            trashItems.removeAll(items)
            DiskTrashManager.OperationsResult.EMPTY
        }
    }

    override suspend fun clearItems(items: List<TrashFile>): DiskTrashManager.OperationsResult {
        return performNetworkAction(NetworkCall.CLEAR) {
            trashItems.removeAll(items)
            DiskTrashManager.OperationsResult.EMPTY
        }
    }

    override suspend fun clearTrash(): DiskTrashManager.OperationsResult {
        return performNetworkAction(NetworkCall.CLEAR_ALL) {
            trashItems.clear()
            DiskTrashManager.OperationsResult.EMPTY
        }
    }

    private fun createTestFile(index: Int, isDir: Boolean = false): TrashFile {
        return TrashFile(
            index.toString(),
            if (isDir) "Dir: $index" else "File: $index",
            "test/$index",
            isDir,
            if (isDir) 0 else 1L + index,
            0,
            if (isDir) null else "image"
        )
    }

    private suspend inline fun <T> performNetworkAction(call: NetworkCall, crossinline action: () -> T): T {
        return when (requestPolicy[call] ?: defaultRequestPolicy) {
            NetworkRequestPolicy.SUCCESS -> action.invoke()
            NetworkRequestPolicy.SIMULATE_IN_PROGRESS -> suspendCoroutine {
                suspendedContinuations = suspendedContinuations + { it.resume(action.invoke()) }
            }
            NetworkRequestPolicy.SIMULATE_IN_PROGRESS_WITH_NETWORK_ERROR -> suspendCoroutine {
                suspendedContinuations = suspendedContinuations + { it.resumeWithException(TrashManager.NetworkException()) }
            }
            NetworkRequestPolicy.NETWORK_ERROR ->
                throw TrashManager.NetworkException()
            NetworkRequestPolicy.SERVER_ERROR ->
                throw IllegalStateException("Unexpected backend response")
        }
    }
}
