package common.zio.testkit.protogen

import com.google.protobuf.{Descriptors, Message}
import common.zio.testkit.protogen.ProtoGen.GenSettings
import zio.random.Random
import zio.test.{Gen, Sized}

import scala.reflect.ClassTag

/**
 * @author Ratskevich Natalia reimai@yandex-team.ru
 */
object JavaProtoGen {

  def apply[T <: Message: ClassTag]: Gen[Random with Sized, T] = gen[T]()

  def gen[T <: Message: ClassTag](genSettings: GenSettings = GenSettings()): Gen[Random with Sized, T] = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
      .asInstanceOf[Class[T]]

    val descriptor = clazz
      .getMethod("getDescriptor")
      .invoke(null)
      .asInstanceOf[Descriptors.Descriptor]
    val defaultInstance = clazz.getMethod("getDefaultInstance").invoke(null).asInstanceOf[T]
    ProtoGen
      .generate(descriptor, genSettings)
      .map(bytes => defaultInstance.toBuilder.mergeFrom(bytes).build().asInstanceOf[T])
  }
}
