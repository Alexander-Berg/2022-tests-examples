package ru.yandex.vos2.model

import java.io._
import java.util.zip.GZIPOutputStream

import play.api.libs.json.Json

import scala.collection.mutable
import scala.util.Random


/**
  * A base for generating lunapark ammo
  */
abstract class AmmoGenBase(file: String, hostName: String) {

  private var fileWriter = initWriter()
  private var writerClosed = false

  protected val request = Json.obj(
    "request" → Json.obj(
      "userIp" → "192.168.0.1",
      "proxyIp" → "127.0.0.1",
      "userAgent" → "tank",
      "flashCookie" → "tymzsviqZvcPvNdSzktgekexbg"
    ))


  private def initWriter(): OutputStreamWriter = {
    new File(file).getParentFile.mkdirs()
    new OutputStreamWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(file), 10 * 1024 * 1024)), "UTF-8")
  }

  def generate(): Unit = {
    val start = System.currentTimeMillis()
    if (writerClosed) fileWriter = initWriter()

    doGenerate()

    fileWriter.write("0\n")
    fileWriter.flush()
    fileWriter.close()
    writerClosed = true

    println(s"Ammo generation took ${(System.currentTimeMillis() - start) / 1000} s")
    println(s"Finished. Ammo generated: $file")
  }

  protected def doGenerate(): Unit

  protected def write(line: String): Unit = {
    fileWriter.write(line)
  }

  protected def dump(lines: mutable.Iterable[String]): Unit = {
    Random.shuffle(lines).foreach(line ⇒ fileWriter.write(line))
  }

  protected def req(url: String, tag: String, method: String = "GET", content: String = ""): String = {
    val res = if (content.nonEmpty) {
      s"$method $url HTTP/1.1\r\n" +
        "User-Agent: tank\r\n" +
        s"Host: $hostName\r\n" +
        "Content-Type: application/json\r\n" +
        s"Content-Length: ${content.length}\r\n" +
        "\r\n" +
        content
    } else {
      s"$method $url HTTP/1.1\r\n" +
        "User-Agent: tank\r\n" +
        s"Host: $hostName\r\n" +
        "\r\n"
    }

    s"${res.length} $tag \n$res"
  }

}