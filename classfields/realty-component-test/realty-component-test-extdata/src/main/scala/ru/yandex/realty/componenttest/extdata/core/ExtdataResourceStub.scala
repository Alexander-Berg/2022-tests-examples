package ru.yandex.realty.componenttest.extdata.core

import java.io.{FileInputStream, InputStream}
import java.util.zip.GZIPOutputStream

import akka.http.scaladsl.model.StatusCodes
import com.google.protobuf.Message
import org.apache.commons.io.IOUtils
import org.joda.time.DateTime
import ru.yandex.extdata.core.ProduceResult.Produced
import ru.yandex.extdata.core.{DataType, Fetcher, ProduceResult}
import ru.yandex.extdata.server.http.view.{InstanceHeaderView, MetaView}
import ru.yandex.realty.clients.resource.S3LinkResponseView
import ru.yandex.realty.componenttest.http.ExternalHttpComponents.toExtdataPath
import ru.yandex.realty.componenttest.http.ExternalHttpStub
import ru.yandex.realty.componenttest.utils.ResourceUtils.getResourceAsStream
import ru.yandex.realty.logging.Logging
import ru.yandex.realty.util.extdata.{result, ExtdataUtils}

import scala.util.{Failure, Success, Try}

trait ExtdataResourceStub extends ExternalHttpStub with Logging {

  def stubFromFetcher(dataType: DataType, fetcher: Fetcher): Unit = {
    fetcher.fetch(None) match {
      case Success(data) =>
        stubResource(dataType, data)
      case Failure(e) =>
        throw new RuntimeException(s"Failed to build resource from fetcher: dataType=$dataType", e)
    }
  }

  def stubGzipFromResources(dataType: DataType, resource: String): Unit = {
    getResourceAsStream(resource)
      .map(streamGzip)
      .map(stubResource(dataType, _))
      .getOrElse(throw new IllegalStateException(s"Could not find resource: dataType=$dataType"))
  }

  private def streamGzip(in: InputStream): ProduceResult = {
    result { os =>
      val gzipos = new GZIPOutputStream(os)
      IOUtils.copy(in, gzipos)
      gzipos.close()
    }
  }

  def stubGzipped(dataType: DataType, data: Iterable[Message]): Unit = {
    stubResource(dataType, result(data))
  }

  protected def stubResource(dataType: DataType, data: ProduceResult): Unit = {
    data match {
      case Produced(is, _) =>
        ExtdataUtils.withFileData(is) { fd =>
          Try {
            stub(dataType, new FileInputStream(fd.file))
          }
        } match {
          case Success(_) => ()
          case Failure(e) =>
            throw new IllegalStateException(s"Failed to produce ext-data: dataType=$dataType", e)
        }
      case _ =>
        throw new IllegalArgumentException(s"Failed to stub ext-data: dataType=$dataType")
    }
  }

  def stubLines(dataType: DataType, lines: Seq[String]): Unit = {
    stub(dataType, IOUtils.toInputStream(lines.mkString("\n")))
  }

  def stub(dataType: DataType, data: InputStream): Unit = {
    val link = dataTypeLinkResponse(dataType)
    stubGetJsonResponse(
      toExtdataPath(s"/api/1.x/ext-data/${dataType.name}/${dataType.format}/link"),
      status = StatusCodes.OK.intValue,
      response = link
    )
    stubGetResponse(
      dataRelativeLink(dataType),
      status = StatusCodes.OK.intValue,
      response = data
    )
  }

  private def dataTypeLinkResponse(dataType: DataType): S3LinkResponseView = {
    S3LinkResponseView(
      meta = MetaView(
        dataType = dataType,
        instanceHeader = new InstanceHeaderView(
          version = 1,
          instanceId = "1",
          produceTime = DateTime.parse("2020-01-01")
        )
      ),
      link = s"${toAbsoluteUrl(dataRelativeLink(dataType))}"
    )
  }

  private def dataRelativeLink(dataType: DataType): String = {
    toExtdataPath(s"/${dataType.name}/${dataType.format}/data")
  }

}
