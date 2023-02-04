package ru.auto.salesman.service.tskv.dealer.format.impl

object ResultLogConverter {

  def convert(input: String): Map[String, Option[String]] =
    input
      .split("\t")
      .map { element =>
        val elementArray = element.split("=")
        val value =
          if (elementArray.length > 1)
            Some(elementArray(1))
          else None
        elementArray(0) -> value
      }
      .toMap
}
