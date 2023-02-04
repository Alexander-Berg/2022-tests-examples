package ru.auto.salesman.api.v1

import java.io.{IOException, InputStream}

import com.google.common.base.Charsets
import com.google.protobuf.Message
import org.apache.commons.io.IOUtils
import ru.auto.salesman.util.copypaste.Protobuf

import scala.reflect.ClassTag

/** Copy-paste from public-api */
object Resources {

  def open[T](path: String)(f: InputStream => T): T =
    IO.using(
      Option(
        getClass.getResourceAsStream(path)
      ).getOrElse(throw new IOException(s"Resource $path not found"))
    )(f)

  def toProto[T <: Message: ClassTag](path: String): T =
    Protobuf.fromJson[T] {
      open(path)(IOUtils.toString(_, Charsets.UTF_8))
    }
}
