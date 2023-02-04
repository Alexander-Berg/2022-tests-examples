package ru.yandex.vertis.billing.balance.xmlrpc

import com.google.common.base.Charsets
import com.google.common.io.Files
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import ru.yandex.vertis.billing.balance.xmlrpc.model.MethodCallValue
import ru.yandex.vertis.billing.balance.xmlrpc.parse.Parsers

import scala.xml.{Elem, XML}

/**
  * Created by IntelliJ IDEA.
  * User: alesavin
  * Date: 24.09.14
  * Time: 20:25
  */
class Recorder(path: String) extends XmlRpcExecutor {

  if ("true".equals(System.getProperty("scenario.recorder.cleanup"))) {
    println("scenario.recorder.cleanup=true, so cleanup scenario dir")
    cleanup()
  }

  var count = new AtomicInteger(0)
  var responses = read()

  override def execute(xml: Elem): Elem = {
    if (count.get() < responses.length) {
      val result = responses(count.get())
      count.incrementAndGet()
      result
    } else {
      dump(xml)
    }
  }

  def dump(xml: Elem): Elem = {
    val method = Parsers.asMethodCall(xml).asInstanceOf[MethodCallValue].method
    dump(xml, s"_rq_$method.xml")
    // slow balance =/
    Thread.sleep(100L)
    val result = execute(xml)
    dump(result, s"_rs_$method.xml")
    count.incrementAndGet()
    result
  }

  def dump(xml: Elem, suffix: String) = {
    val file = new File(path, "%03d".format(count.get()) + suffix)
    Files.write(xml.toString().getBytes(Charsets.UTF_8), file)
  }

  def read(): Array[Elem] = {
    val dir = new File(path)
    if (dir.isDirectory) {
      dir
        .listFiles()
        .filter(f => f.getName.endsWith(".xml") && f.getName.contains("_rs_"))
        .sortBy(f => f.getName)
        .map(f => {
          XML.loadString(scala.io.Source.fromFile(f, Charsets.UTF_8.name()).mkString)
        })
    } else
      throw new IllegalArgumentException(s"getPath should return directory. Returned: ${dir.getAbsolutePath}")
  }

  def cleanup() = {
    val dir = new File(path)
    if (dir.isDirectory)
      dir.listFiles().map(_.delete())
    else
      throw new IllegalArgumentException("getPath should return directory")
  }

  override protected def executeRequest(request: HttpUriRequest): HttpResponse =
    throw new NotImplementedError

}

object Recorder {

  def apply(path: String): Recorder = new Recorder(path)

}
