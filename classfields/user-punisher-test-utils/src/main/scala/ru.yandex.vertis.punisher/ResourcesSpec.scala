package ru.yandex.vertis.punisher

import com.google.common.base.Charsets

trait ResourcesSpec {

  private def read(path: String) =
    scala.io.Source.fromInputStream(getClass.getResourceAsStream(path), Charsets.UTF_8.name())

  protected def resourceString(path: String) = read(path).mkString

  protected def resourceLines(path: String): Iterator[String] =
    read(path)
      .getLines()
      .filterNot(s => s.isEmpty || s.startsWith("#"))
}
