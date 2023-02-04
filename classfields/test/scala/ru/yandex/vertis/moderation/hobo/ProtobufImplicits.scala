package ru.yandex.vertis.moderation.hobo

import com.google.protobuf

object ProtobufImplicits {

  implicit def stringToStringValue(s: String): protobuf.StringValue =
    protobuf.StringValue.newBuilder().setValue(s).build()

}
