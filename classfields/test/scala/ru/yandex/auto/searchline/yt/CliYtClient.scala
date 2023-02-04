package ru.yandex.auto.searchline.yt

import java.io.{File, InputStream, OutputStream}

import YtClient.NodeType.NodeType
import YtClient._
import ru.yandex.yt.yson.YsonUtil
import ru.yandex.yt.yson.dom.YsonBoolean

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.sys.process.{Process, ProcessIO}

class CliYtClient(proxy: String, token: String) extends YtClient {

  override def writeTable(path: String, fileName: String, optParams: Option[WriteParams]): Unit = {
    require(path.nonEmpty)

    val append = optParams.fold(false)(_.append)

    val cmd = Seq(
      "write-table", s"<append=$append>$path", "--format", "yson"
    ) ++ (if (optParams.exists(_.tableWriter.isDefined)) Seq("--table-writer", optParams.get.tableWriter.get) else Nil)

    val file = new File(fileName)

    val pb = baseProcessBuilder(cmd) #< file

    doRequest(s"write-table $path", pb)
  }

  override def create(nodeType: NodeType,
                      path: String,
                      optAttributes: Option[Seq[Attribute]] = None,
                      optParams: Option[CreateParams]
                     ): Unit = {

    require(path.nonEmpty)

    val cmd = ArrayBuffer("create", nodeType.toString, path)

    optParams.foreach { params =>
      if (params.recursive) cmd += "--recursive"
      if (params.ignoreExisting) cmd += "--ignore-existing"
      if (params.force) cmd += "--force"
    }

    optAttributes.foreach { attributes =>
      cmd ++= Seq("--attributes", attributes.map { case (key, value) => s"$key=$value" }.mkString("{", ";", "}"))
    }

    doRequest(s"create ${nodeType.toString}", cmd)
  }

  override def link(targetPath: String, linkPath: String, optParams: Option[LinkParams]): Unit = {
    require(targetPath.nonEmpty)
    require(linkPath.nonEmpty)

    val cmd = ArrayBuffer("link", targetPath, linkPath)

    optParams.foreach { params =>
      if (params.recursive) cmd += "--recursive"
      if (params.ignoreExisting) cmd += "--ignore-existing"
      if (params.force) cmd += "--force"
    }

    doRequest(s"link", cmd)
  }

  override def exists(path: String): Boolean = {
    require(path.nonEmpty)

    val res = doRequest("exists", Seq("exists", path))

    YsonUtil.fromJsonString(res) match {
      case r: YsonBoolean => r.getBoolean
      case _ => throw new Exception("unknown content")
    }
  }

  override def get(path: String): String = {
    require(path.nonEmpty)

    val cmd = Seq("get", path)

    doRequest("get", cmd)
  }

  override def remove(path: String, optParams: Option[RemovedParams]): Unit = {
    require(path.nonEmpty)

    val cmd = ArrayBuffer("remove", path)

    optParams.foreach { params =>
      if (params.recursive) cmd += "--recursive"
      if (params.force) cmd += "--force"
    }

    doRequest("removed", cmd)
  }

  private def doRequest(comment: String, cmd: Seq[String]): String = {
    doRequest(comment, baseProcessBuilder(cmd))
  }

  private def doRequest(comment: String, pb: scala.sys.process.ProcessBuilder): String = {
    doRequest(comment, pb, IOProcess())
  }

  private def doRequest(comment: String, pb: scala.sys.process.ProcessBuilder, iop: IOProcess): String = {

    val process = pb.run(iop.process)
    println(s"command: $pb")

    try {
      val res = process.exitValue()

      val content = Option(if (res == 0) {
        iop.getOut
      } else {
        iop.getErr
      }).filter(_.nonEmpty).map(c => s" Content: $c").getOrElse("")

      if (res == 0) {
        println(s"YT $comment success. Response code $res.$content")
        iop.getOut
      } else {
        throw new Exception(s"YT $comment failed. Response code $res.$content")
      }
    } finally {
      process.destroy()
    }
  }

  private def baseProcessBuilder(cmd: Seq[String]): scala.sys.process.ProcessBuilder = {
    Process(prepareCommand(cmd), None, "YT_TOKEN" -> token, "YT_STRUCTURED_DATA_FORMAT" -> "json")
  }

  private def prepareCommand(cmd: Seq[String]): Seq[String] = {
    Seq("yt", "--proxy", proxy) ++ cmd.filter(_.nonEmpty)
  }

  private class IOProcess {

    private val out = new StringBuilder
    private val err = new StringBuilder

    private def writeInput(os: OutputStream): Unit = {
      os.close()
    }

    private def processOutput(is: InputStream): Unit = {
      append("out", is, out.append)
    }

    private def processError(is: InputStream): Unit = {
      append("err", is, err.append, endLine = true)
    }

    private def append[T](comment: String, is: InputStream, f: String => T, endLine: Boolean = false): Unit = {

      val s = Source.fromInputStream(is, "UTF-8")

      for (line <- s.getLines()) {

        if (line.nonEmpty) {

          f(line)

          if (endLine) {
            f("\n")
          }

        }
      }
    }

    private val processIO = new ProcessIO(writeInput, processOutput, processError)

    val process: ProcessIO = processIO

    def getOut: String = out.toString()

    def getErr: String = err.toString()
  }

  private object IOProcess {

    def apply(): IOProcess = new IOProcess()

    def apply(writeInput: OutputStream => Unit): IOProcess = new IOProcess {
      override val process: ProcessIO = new ProcessIO({ os =>
        writeInput(os)
        os.close()
      }, super.processOutput, super.processError)
    }
  }

}
