package vertis.yt.proto.testkit

import com.google.common.collect.Iterables
import com.google.protobuf.DynamicMessage

import java.util
import scala.jdk.CollectionConverters._

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */

// FIXME: move to proto-utils
trait DynamicProtoTestAccessors {

  protected def getTheOnlyFieldValue(dynamicMessage: DynamicMessage): Any =
    Iterables.getOnlyElement(dynamicMessage.getAllFields.values)

  protected def getFieldValues(dynamicMessage: DynamicMessage): Map[String, Any] =
    dynamicMessage.getAllFields.asScala.map { case (field, value) =>
      field.getName -> value
    }.toMap

  protected def getTheOnlyFieldRepeatedValues(dynamicMessage: DynamicMessage): Seq[DynamicMessage] =
    getTheOnlyFieldValue(dynamicMessage)
      .asInstanceOf[util.Collection[DynamicMessage]]
      .asScala
      .toSeq
}
