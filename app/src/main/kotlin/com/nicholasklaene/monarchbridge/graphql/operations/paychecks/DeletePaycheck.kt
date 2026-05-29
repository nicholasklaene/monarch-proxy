package com.nicholasklaene.monarchbridge.graphql.operations.paychecks

import com.fasterxml.jackson.databind.JsonNode
import com.nicholasklaene.monarchbridge.generated.model.DeleteAccountResponseData
import com.nicholasklaene.monarchbridge.graphql.GraphQLLoader
import com.nicholasklaene.monarchbridge.graphql.MonarchOperation

/**
 * Delete one paycheck. Destructive. Monarch's mutation returns `success: Boolean`;
 * the controller surfaces it as `{deleted: Boolean}` via the spec-deduped
 * `DeleteAccountResponseData` envelope (same pattern as other delete endpoints).
 */
object DeletePaycheck : MonarchOperation<DeletePaycheck.Input, DeleteAccountResponseData> {
    override val operationName: String = "Common_DeletePaycheck"

    // The .graphql inlines the full `errors { fieldErrors { field messages } message code }`
    // selection, so it is self-contained. Declaring fragments here would append an unused
    // `fragment PayloadErrorFields` definition to the document, which Monarch rejects with a
    // "no fragment used" parse error (HTTP 400). Load it with no fragments.
    override val query: String by lazy { GraphQLLoader.load(operationName) }

    data class Input(
        val id: String,
    )

    override fun variables(input: Input): Map<String, Any?> = mapOf("input" to mapOf("id" to input.id))

    override fun parseOutput(data: JsonNode): DeleteAccountResponseData =
        DeleteAccountResponseData(
            deleted = data.path("deletePaycheck").path("success").asBoolean(false),
        )
}
