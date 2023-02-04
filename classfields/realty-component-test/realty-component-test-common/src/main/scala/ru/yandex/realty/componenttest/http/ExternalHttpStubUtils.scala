package ru.yandex.realty.componenttest.http

object ExternalHttpStubUtils {

  def getOrMatchAll(regex: Option[String] = None): String =
    regex.getOrElse(".+")

}
