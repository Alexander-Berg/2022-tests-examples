package ru.yandex.realty.componenttest.utils

import java.io.{BufferedReader, InputStream, InputStreamReader}

import ru.yandex.realty.util.IOUtil

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.Try

object ResourceUtils {

  def listResources(path: String): Seq[String] = {
    Try {
      val filenames = new ArrayBuffer[String]()
      getResourceAsStream(path).map { in =>
        IOUtil.using(in) { in =>
          IOUtil.using(new BufferedReader(new InputStreamReader(in))) { reader =>
            var continueScan = true
            do {
              val resource = reader.readLine()
              if (resource != null) {
                filenames += resource
              } else {
                continueScan = false
              }
            } while (continueScan)
          }
        }
      }
      filenames
    }.get
  }

  def getResourceAsString(resource: String): Option[String] = {
    getResourceAsStream(resource).map(Source.fromInputStream).map(_.mkString)
  }

  def getResourceAsStream(resource: String): Option[InputStream] = {
    Option(contextClassLoader.getResourceAsStream(resource))
      .orElse(Option(ResourceUtils.getClass.getResourceAsStream(resource)))
  }

  def contextClassLoader: ClassLoader =
    Thread.currentThread().getContextClassLoader

}
