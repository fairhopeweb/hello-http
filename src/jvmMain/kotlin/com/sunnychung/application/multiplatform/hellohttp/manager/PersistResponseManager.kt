package com.sunnychung.application.multiplatform.hellohttp.manager

import com.sunnychung.application.multiplatform.hellohttp.AppContext
import com.sunnychung.application.multiplatform.hellohttp.document.RequestsDI
import com.sunnychung.application.multiplatform.hellohttp.document.ResponseCollection
import com.sunnychung.application.multiplatform.hellohttp.document.ResponsesDI
import com.sunnychung.application.multiplatform.hellohttp.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.ConcurrentHashMap

class PersistResponseManager {

//    private val flows: MutableSet<Flow<NetworkManager.NetworkEvent>> = mutableSetOf()
//    private val networkManager by lazy { AppContext.NetworkManager }
    private val responseCollectionRepository by lazy { AppContext.ResponseCollectionRepository }
    private val requestCollectionRepository by lazy { AppContext.RequestCollectionRepository }

    fun registerCall(networkManager: NetworkManager, callId: String) {
        val callData = networkManager.getCallData(callId)!!
        callData.events.onEach {
            log.d { "PersistResponseManager receives call $callId event ${it.event}" }
            val documentId = ResponsesDI(subprojectId = callData.subprojectId)
            val record = loadResponseCollection(documentId)
            record.responsesByRequestExampleId[callData.response.requestExampleId] = callData.response
            responseCollectionRepository.notifyUpdated(documentId)
        }.launchIn(CoroutineScope(Dispatchers.IO))
    }

    /**
     * This method assumes `requestCollectionRepository.read` has already been called for the same subproject.
     */
    suspend fun loadResponseCollection(documentId: ResponsesDI): ResponseCollection {
        val result = responseCollectionRepository.readOrCreate(documentId) { id ->
            ResponseCollection(id, ConcurrentHashMap())
        }

        // cleanup responses that is not linked to an active request
        requestCollectionRepository.read(RequestsDI(subprojectId = documentId.subprojectId))?.let { requestCollection ->
            val requestExampleIds = requestCollection.requests.flatMap { it.examples }.map { it.id }.toSet()
            val keysOfDetachedResponses = result.responsesByRequestExampleId.keys - requestExampleIds
            if (keysOfDetachedResponses.isNotEmpty()) {
                keysOfDetachedResponses.forEach {
                    result.responsesByRequestExampleId.remove(key = it)
                }
                responseCollectionRepository.notifyUpdated(documentId)
            }
        }

        return result
    }
}
