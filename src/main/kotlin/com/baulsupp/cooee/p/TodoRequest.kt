// Code generated by Wire protocol buffer compiler, do not edit.
// Source: com.baulsupp.cooee.p.TodoRequest in api.proto
package com.baulsupp.cooee.p

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.Syntax.PROTO_3
import com.squareup.wire.WireField
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.hashCode
import kotlin.jvm.JvmField
import okio.ByteString

class TodoRequest(
  @field:WireField(
    tag = 1,
    adapter = "com.squareup.wire.ProtoAdapter#UINT32_VALUE",
    label = WireField.Label.OMIT_IDENTITY
  )
  @JvmField
  val limit: Int? = null,
  unknownFields: ByteString = ByteString.EMPTY
) : Message<TodoRequest, TodoRequest.Builder>(ADAPTER, unknownFields) {
  override fun newBuilder(): Builder {
    val builder = Builder()
    builder.limit = limit
    builder.addUnknownFields(unknownFields)
    return builder
  }

  override fun equals(other: Any?): Boolean {
    if (other === this) return true
    if (other !is TodoRequest) return false
    if (unknownFields != other.unknownFields) return false
    if (limit != other.limit) return false
    return true
  }

  override fun hashCode(): Int {
    var result = super.hashCode
    if (result == 0) {
      result = unknownFields.hashCode()
      result = result * 37 + limit.hashCode()
      super.hashCode = result
    }
    return result
  }

  override fun toString(): String {
    val result = mutableListOf<String>()
    if (limit != null) result += """limit=$limit"""
    return result.joinToString(prefix = "TodoRequest{", separator = ", ", postfix = "}")
  }

  fun copy(limit: Int? = this.limit, unknownFields: ByteString = this.unknownFields): TodoRequest =
      TodoRequest(limit, unknownFields)

  class Builder : Message.Builder<TodoRequest, Builder>() {
    @JvmField
    var limit: Int? = null

    fun limit(limit: Int?): Builder {
      this.limit = limit
      return this
    }

    override fun build(): TodoRequest = TodoRequest(
      limit = limit,
      unknownFields = buildUnknownFields()
    )
  }

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<TodoRequest> = object : ProtoAdapter<TodoRequest>(
      FieldEncoding.LENGTH_DELIMITED, 
      TodoRequest::class, 
      "type.googleapis.com/com.baulsupp.cooee.p.TodoRequest", 
      PROTO_3, 
      null
    ) {
      override fun encodedSize(value: TodoRequest): Int {
        var size = value.unknownFields.size
        if (value.limit != null) size += ProtoAdapter.UINT32_VALUE.encodedSizeWithTag(1,
            value.limit)
        return size
      }

      override fun encode(writer: ProtoWriter, value: TodoRequest) {
        if (value.limit != null) ProtoAdapter.UINT32_VALUE.encodeWithTag(writer, 1, value.limit)
        writer.writeBytes(value.unknownFields)
      }

      override fun decode(reader: ProtoReader): TodoRequest {
        var limit: Int? = null
        val unknownFields = reader.forEachTag { tag ->
          when (tag) {
            1 -> limit = ProtoAdapter.UINT32_VALUE.decode(reader)
            else -> reader.readUnknownField(tag)
          }
        }
        return TodoRequest(
          limit = limit,
          unknownFields = unknownFields
        )
      }

      override fun redact(value: TodoRequest): TodoRequest = value.copy(
        limit = value.limit?.let(ProtoAdapter.UINT32_VALUE::redact),
        unknownFields = ByteString.EMPTY
      )
    }

    private const val serialVersionUID: Long = 0L
  }
}
