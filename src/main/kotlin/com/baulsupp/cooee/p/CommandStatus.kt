// Code generated by Wire protocol buffer compiler, do not edit.
// Source: com.baulsupp.cooee.p.CommandStatus in api.proto
package com.baulsupp.cooee.p

import com.squareup.wire.EnumAdapter
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.Syntax.PROTO_3
import com.squareup.wire.WireEnum
import kotlin.Int
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

public enum class CommandStatus(
  public override val `value`: Int,
) : WireEnum {
  UNDEFINED(0),
  CLIENT_ACTION(1),
  DONE(2),
  REDIRECT(3),
  REQUEST_ERROR(4),
  SERVER_ERROR(5),
  STREAM(6),
  ;

  public companion object {
    @JvmField
    public val ADAPTER: ProtoAdapter<CommandStatus> = object : EnumAdapter<CommandStatus>(
      CommandStatus::class, 
      PROTO_3, 
      CommandStatus.UNDEFINED
    ) {
      public override fun fromValue(`value`: Int): CommandStatus? = CommandStatus.fromValue(value)
    }

    @JvmStatic
    public fun fromValue(`value`: Int): CommandStatus? = when (value) {
      0 -> UNDEFINED
      1 -> CLIENT_ACTION
      2 -> DONE
      3 -> REDIRECT
      4 -> REQUEST_ERROR
      5 -> SERVER_ERROR
      6 -> STREAM
      else -> null
    }
  }
}
