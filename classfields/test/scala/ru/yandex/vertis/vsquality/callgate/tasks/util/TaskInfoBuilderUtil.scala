package ru.yandex.vertis.vsquality.callgate.tasks.util

import io.circe.Decoder
import io.circe.parser.decode
import ru.yandex.vertis.vsquality.utils.scalapb_utils.ProtoJson.fromProtoReads
import ru.yandex.vertis.vsquality.utils.scalapb_utils.{ProtoMessage, ProtoMessageReads}

trait TaskInfoBuilderUtil {

  protected def loadRes(name: String): String

  protected def loadEntity[T, M <: ProtoMessage[M]](
      fileName: String
    )(implicit protoReader: ProtoMessageReads[T, M]): T =
    decode(loadRes(fileName))(fromProtoReads(protoReader)).getOrElse(???)

  protected def loadEntityCirce[A: Decoder](fileName: String): A = decode(loadRes(fileName)).getOrElse(???)
}
