package ru.yandex.complaints.ammo

import ru.yandex.complaints.api.handlers.api.complaint.model.UpdateComplaintRequest

import java.io.{BufferedOutputStream, FileOutputStream, OutputStreamWriter}
import java.util.zip.GZIPOutputStream
import ru.yandex.complaints.model.ComplaintGenerator

import scala.collection.mutable
import scala.util.Random

/**
  * Created by s-reznick on 01.08.16.
  */
object AmmoGenerator extends App {
  if (args.length > 0 && args(0) == "-gui") {
    import java.awt

    println("GUI")
    val frame = new awt.Frame
    val layout = new awt.GridLayout(0, 2)
    frame.setLayout(layout)
    frame.setVisible(true)
    frame.add(new awt.Label("# of complaints"))
    val nComplaints = new awt.List()
    nComplaints.add("10")
    frame.add(nComplaints)
    frame.pack
  }

  //System.exit(0)


  private def longPow(base: Int, pow: Int): Long = {
    (1 until pow).foldLeft(base)((a,b) => a*base)
  }

  def req(url: String,
          tag: String,
          method: String = "GET",
          content: String = ""): String = {
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


  def dump(line: String): Unit = {
    fileWriter.write(line)
  }

  val hostName = "complaints-api-01-sas.test.vertis.yandex.net"
  val numberOfComplaints = 10000


  val file: String = s"complaints-realty-c${numberOfComplaints}-o1-mod.gz"

  val start = System.currentTimeMillis()

  val fileWriter =
    new OutputStreamWriter(
      new GZIPOutputStream(
        new BufferedOutputStream(
          new FileOutputStream(file), 10 * 1024 * 1024)), "UTF-8")

  val buffer = mutable.ArrayBuffer[String]()

  import ComplaintGenerator._

  for (a ← 1 to numberOfComplaints) {
//    for (oid ← OfferIdGen.sample) {
        val oid = "3464020066962951609"
        val aid = "yandex_uid_1039236996"
        for (request: UpdateComplaintRequest ← NewUpdateComplaintRequestGen.sample) {
          dump(req(s"/api/1.x/complaint/$aid/$oid",
            "create_offer", "PUT", request.toJson))
        }
  //    }
  }

  fileWriter.write("0\n")
  fileWriter.flush()
  fileWriter.close()

  System.out.format("Ammo generation took %s seconds.",
    ((System.currentTimeMillis() - start)/1000).toString)
  System.out.format(s" Finished. Ammo generated: %s%n", file)
}