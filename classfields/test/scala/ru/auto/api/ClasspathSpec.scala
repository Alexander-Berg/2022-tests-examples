package ru.auto.api

import com.google.common.reflect.ClassPath

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 27.08.18
  */
class ClasspathSpec extends BaseSpec {

  private val ignoredPrefixes = Seq(
    "org.scalatools.",
    "sbt."
  )

  "App" should {
    "not have duplicates in classpath" in {
      val visited = mutable.Set.empty[String]
      val duplicates = ArrayBuffer.empty[String]
      ClassPath.from(getClass.getClassLoader).getAllClasses.forEach { clazz =>
        val name = clazz.getName
        if (!visited.add(name) && !ignoredPrefixes.exists(name.startsWith)) {
          duplicates += name
        }
      }

      if (duplicates.nonEmpty) {
        fail(s"Some classes have duplicates in classpath:\n${duplicates.mkString("\n")}")
      }
    }
  }
}
