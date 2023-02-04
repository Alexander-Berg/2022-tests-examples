package ru.auto.salesman.test

import scala.io.Source

trait ReadResource {

  def readResource(path: String): String =
    Source
      .fromInputStream(getClass.getResourceAsStream(path))
      .mkString
}
