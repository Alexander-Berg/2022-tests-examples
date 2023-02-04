package ru.yandex.realty.componenttest.data.utils

import com.google.protobuf.AbstractMessage.Builder
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import play.api.libs.json.{JsArray, Json}
import ru.yandex.realty.componenttest.utils.ResourceUtils
import ru.yandex.realty.model.message.ExtDataSchema.PhoneMessage
import ru.yandex.realty.model.message.Mortgages.{Bank, MortgageProgram}
import ru.yandex.realty.proto.phone.PhoneRedirectMessage

import scala.reflect.{classTag, ClassTag}
import scala.util.matching.Regex

object ComponentTestDataUtils {

  private val ClassNameRegex: Regex = "^\\w+_(\\d+).+".r

  def asPhoneMessage(redirect: PhoneRedirectMessage): PhoneMessage = {
    PhoneMessage
      .newBuilder()
      .setTag(redirect.getTag)
      .setPhone(redirect.getSource)
      .build()
  }

  def extractIdFromClassName(clazz: Class[_]): Long = {
    clazz.getSimpleName match {
      case ClassNameRegex(offerId) =>
        offerId.toLong
      case _ =>
        throw new IllegalStateException(s"Failed to extract ID: className=${clazz.getName}")
    }
  }

  def loadProtoListFromJsonResource[T <: Message: ClassTag](resource: String): Seq[T] = {
    ResourceUtils
      .getResourceAsString(resource)
      .map(Json.parse)
      .map(_.as[JsArray])
      .map(_.value.map(_.toString).map(parseProtoFromJson[T]))
      .get
  }

  def loadProtoFromJsonResource[T <: Message: ClassTag](resource: String): T = {
    ResourceUtils.getResourceAsString(resource).map(parseProtoFromJson[T]).get
  }

  def parseProtoFromJson[T <: Message: ClassTag](json: String): T = {
    val builder: Builder[_] = classTag[T].runtimeClass.getMethod("newBuilder").invoke(null).asInstanceOf[Builder[_]]
    JsonFormat.parser.ignoringUnknownFields.merge(json, builder)
    builder.build().asInstanceOf[T]
  }

  def buildNew[T: ClassTag](initializer: (T => Unit)): T = {
    val obj: T = classTag[T].runtimeClass.asInstanceOf[Class[T]].newInstance()
    initializer(obj)
    obj
  }

  def initialize[T](value: => T)(initializer: (T => Unit)): T = {
    val v = value
    initializer(v)
    v
  }

  def mortgageProgramCanonicalUrl(mortgageProgram: MortgageProgram): String = {
    s"/bank-${mortgageProgram.getBankId}/mortgage-program-${mortgageProgram.getId}"
  }

}
