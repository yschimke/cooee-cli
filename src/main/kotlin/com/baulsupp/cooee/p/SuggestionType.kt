// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: api.proto
package com.baulsupp.cooee.p

import com.squareup.wire.EnumAdapter
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.WireEnum
import kotlin.Int
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

enum class SuggestionType(
  override val value: Int
) : WireEnum {
  UNKNOWN(0),

  /**
   * Returns a link to redirect (message is secondary to link)
   */
  LINK(1),

  /**
   * Returns a command that can be executed via a POST, with preview etc
   */
  COMMAND(2),

  /**
   * Returns a command prefix that allows further comment
   */
  PREFIX(3),

  /**
   * Returns subcommands
   */
  LIST(4),

  /**
   * Shows a preview or information (link is secondary to message)
   */
  INFO(5);

  companion object {
    @JvmField
    val ADAPTER: ProtoAdapter<SuggestionType> = object : EnumAdapter<SuggestionType>(
      SuggestionType::class
    ) {
      override fun fromValue(value: Int): SuggestionType? = SuggestionType.fromValue(value)
    }

    @JvmStatic
    fun fromValue(value: Int): SuggestionType? = when (value) {
      0 -> UNKNOWN
      1 -> LINK
      2 -> COMMAND
      3 -> PREFIX
      4 -> LIST
      5 -> INFO
      else -> null
    }
  }
}