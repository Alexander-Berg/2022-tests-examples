package ru.yandex.realty.searcher.response.builders.inexact

import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import ru.yandex.vertis.protobuf.ProtoInstance
import ru.yandex.vertis.protobuf.ProtoInstanceProvider.protoInstance

object ProtoHelper {
  val parser = JsonFormat.parser()

  implicit class ProtoConverter(jsonStr: String) {

    def toProto[T <: Message: ProtoInstance]: T = {
      val builder = protoInstance[T].newBuilderForType()
      JsonFormat.parser().ignoringUnknownFields().merge(jsonStr, builder)
      builder.build().asInstanceOf[T]
    }
  }

}
