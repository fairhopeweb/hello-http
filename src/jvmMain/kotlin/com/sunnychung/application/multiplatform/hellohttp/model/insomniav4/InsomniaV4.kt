package com.sunnychung.application.multiplatform.hellohttp.model.insomniav4

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

object InsomniaV4 {

    data class HttpRequest(
        @JsonProperty("_id") val id: String,
        val parentId: String,
        val url: String,
        val name: String,
        val description: String,
        val method: String,
        val body: Body,
        val parameters: List<KeyValue>,
        val headers: List<KeyValue>,
        val authentication: Authentication,
    ) {
        data class Body(
            val mimeType: String?,
            val text: String?
        )

        data class KeyValue(
            val id: String?,
            val name: String,
            val value: String,
            val description: String?,
            val disabled: Boolean?,
        )

        data class Authentication(
            val type: String?,
            val token: String?,
            val prefix: String?,
            val disabled: Boolean?,
        )
    }

    data class RequestGroup(
        @JsonProperty("_id") val id: String,
        val parentId: String,
        val name: String,
    )

    data class Environment(
        @JsonProperty("_id") val id: String,
        val parentId: String,
        val name: String,
        val data: JsonNode,
    )

    data class Workspace(
        @JsonProperty("_id") val id: String,
        val parentId: String?, // always null
        val name: String,
        val description: String,
        val scope: String,
    )
}