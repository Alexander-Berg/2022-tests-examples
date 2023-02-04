package ru.auto.carfax.eds.data_types

import org.apache.commons.io.FileUtils
import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import ru.auto.carfax.eds.data_types.loaders.CatalogFetcher
import ru.yandex.auto.vin.decoder.extdata.DataTypes
import ru.yandex.auto.vin.decoder.proto.CatalogSchema
import ru.yandex.extdata.core.Data.{FileData, StreamingData}
import ru.yandex.extdata.core.ProduceResult.Produced
import ru.yandex.extdata.core.event.ContainerEventListener
import ru.yandex.extdata.core.service.ExtDataService
import ru.yandex.extdata.core.{Controller, Instance, InstanceHeader, ProduceResult}

import java.io.File
import java.util.zip.GZIPInputStream
import scala.collection.mutable.HashMap
import scala.util.Try

/**
 * Created by artvl on 07.07.16.
 */
class CatalogLoaderSpec extends AnyFlatSpec with MockitoSugar with Matchers {

  val fetcher = new CatalogFetcher(prepareController())

  "A catalogFetcher" should "fetch a catalog" in {
    val produceResult: Try[ProduceResult] = fetcher.fetch(None)
    produceResult.isFailure should not be true

    val is = new GZIPInputStream(produceResult.get.asInstanceOf[Produced].is.asInstanceOf[StreamingData].is)

    val catalogCards = new HashMap[String, HashMap[String, (String, String)]]
    LazyList
      .continually(Option(CatalogSchema.CatalogCard.parseDelimitedFrom(is)))
      .takeWhile(_.isDefined)
      .foreach { card =>
        val mark = card.get.getMark.getMarkCode
        val model = card.get.getModel.getModelCode
        catalogCards
          .getOrElseUpdate(mark, new HashMap[String, (String, String)])
          .put(model, (mark, model))

      }

    catalogCards.get("BMW") should not be None
    catalogCards("BMW")("M3") should not be None
    a[NoSuchElementException] should be thrownBy {
      catalogCards("BMW")("M4")
    }

    catalogCards.get("AUDI") should be(None)
  }

  private def prepareController(): Controller = {
    val file = File.createTempFile("catalog", "")
    FileUtils.copyInputStreamToFile(getClass.getResourceAsStream("/auto.xml.gz"), file)

    val streamingData = FileData(file)
    val instanceHeader = InstanceHeader("1", 0, null)
    val instance = Instance(instanceHeader, streamingData)

    val controller = mock[Controller]

    val extDataService = mock[ExtDataService]

    val containerEventListener = mock[ContainerEventListener]

    when(extDataService.getLast(DataTypes.Verba)).thenReturn(Try(instance))

    when(controller.extDataService).thenReturn(extDataService)

    when(controller.listenerContainer).thenReturn(containerEventListener)
    controller
  }

}
