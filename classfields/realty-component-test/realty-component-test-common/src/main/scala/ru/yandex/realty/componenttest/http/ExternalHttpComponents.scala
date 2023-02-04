package ru.yandex.realty.componenttest.http

object ExternalHttpComponents {

  val ExtdataComponentBasePath: String = "extdata"
  val TeleponyComponentBasePath: String = "telepony"

  def toExtdataPath(path: String): String = {
    appendPrefixToPath(ExtdataComponentBasePath, path)
  }

  def toTeleponyPath(path: String): String = {
    appendPrefixToPath(TeleponyComponentBasePath, path)
  }

  private def appendPrefixToPath(prefix: String, path: String): String = {
    if (path.startsWith("/")) {
      s"/$prefix$path"
    } else {
      s"$prefix/$path"
    }
  }

}
