// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: api.proto
package com.baulsupp.cooee.p

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.WireField
import com.squareup.wire.internal.checkElementsNotNull
import com.squareup.wire.internal.redactElements
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlin.jvm.JvmField
import okio.ByteString

class CompletionResponse(
  @field:WireField(
    tag = 1,
    adapter = "com.baulsupp.cooee.p.CompletionSuggestion#ADAPTER",
    label = WireField.Label.REPEATED
  )
  @JvmField
  val completions: List<CompletionSuggestion> = emptyList(),
  unknownFields: ByteString = ByteString.EMPTY
) : Message<CompletionResponse, CompletionResponse.Builder>(ADAPTER, unknownFields) {
  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.completions = completions
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is CompletionResponse) return false
    return unknownFields == other.unknownFields
        && completions == other.completions
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + completions.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (completions.isNotEmpty()) result += """completions=$completions"""
    return result.joinToString(prefix = "CompletionResponse{", separator = ", ", postfix = "}")
  }

  fun copy(completions: List<CompletionSuggestion> = this.completions, unknownFields: ByteString =
      this.unknownFields): CompletionResponse = CompletionResponse(completions, unknownFields)

  class Builder : Message.Builder<CompletionResponse, Builder>() {
    @JvmField
    var completions: List<CompletionSuggestion> = emptyList()

    fun completions(completions: List<CompletionSuggestion>): Builder {
      checkElementsNotNull(completions)
      this.completions = completions
      return this
    }

    override fun build(): CompletionResponse = CompletionResponse(
      completions = completions,
      unknownFields = buildUnknownFields()
    )
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<CompletionResponse> = object : ProtoAdapter<CompletionResponse>(
      FieldEncoding.LENGTH_DELIMITED, 
      CompletionResponse::class, 
      "type.googleapis.com/com.baulsupp.cooee.p.CompletionResponse"
    ) {
      override fun encodedSize(value: CompletionResponse): Int = 
        CompletionSuggestion.ADAPTER.asRepeated().encodedSizeWithTag(1, value.completions) +
        value.unknownFields.size

      override fun encode(writer: ProtoWriter, value: CompletionResponse) {
        CompletionSuggestion.ADAPTER.asRepeated().encodeWithTag(writer, 1, value.completions)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): CompletionResponse {
        val completions = mutableListOf<CompletionSuggestion>()
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> completions.add(CompletionSuggestion.ADAPTER.decode(reader))
            else -> reader.readUnknownField(tag)
          }
        }
        return CompletionResponse(
          completions = completions,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: CompletionResponse): CompletionResponse = value.copy(
        completions = value.completions.redactElements(CompletionSuggestion.ADAPTER),
        unknownFields = ByteString.EMPTY
      )
    }
  }
}