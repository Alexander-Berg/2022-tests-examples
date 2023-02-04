package ru.auto.carfax.eds.data_types

import org.apache.commons.io.FileUtils
import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import ru.auto.carfax.eds.data_types.loaders.VinFetcher
import ru.yandex.auto.vin.decoder.extdata.DataTypes
import ru.yandex.auto.vin.decoder.verba.proto.VinPropertiesSchema.VinNode
import ru.yandex.extdata.core.Data.{FileData, StreamingData}
import ru.yandex.extdata.core.ProduceResult.Produced
import ru.yandex.extdata.core.event.ContainerEventListener
import ru.yandex.extdata.core.service.ExtDataService
import ru.yandex.extdata.core.{Controller, Instance, InstanceHeader, ProduceResult}

import java.io.File
import java.util.zip.GZIPInputStream
import scala.jdk.CollectionConverters.{IteratorHasAsScala, ListHasAsScala}
import scala.util.Try

/**
 * Created by artvl on 07.07.16.
 */
class VinLoaderSpec extends AnyFlatSpec with MockitoSugar with Matchers {

  case class WMI(id: Long, code: Option[String], marks: Seq[String], modelYearPosition: Option[Int], checksum: Boolean)

  case class VinLogic(id: Long, name: Option[String], marks: Seq[String], position: Seq[Int])

  case class MarkVDS(code: String, VDS: Seq[VDS])

  case class VDS(
      code: Option[String],
      destination: Option[String],
      URL: Seq[String],
      VinLogic: Option[Long],
      model: Option[String],
      comments: Option[String],
      superGenId: Option[Long],
      configurationIds: Seq[Long])

  val fetcher = new VinFetcher(prepareController())

  "A VinFetcher" should "fetch a catalog" in {
    val produceResult: Try[ProduceResult] = fetcher.fetch(None)
    produceResult.isFailure should not be true

    val is = new GZIPInputStream(produceResult.get.asInstanceOf[Produced].is.asInstanceOf[StreamingData].is)

    var WMIs: Map[Long, WMI] = Map.empty
    var vinLogics: Map[Long, VinLogic] = Map.empty
    var markVDSs: Map[String, MarkVDS] = Map.empty
    LazyList
      .continually(Option(VinNode.parseDelimitedFrom(is)))
      .takeWhile(_.isDefined)
      .foreach { nodeOpt =>
        val node = nodeOpt.get
        node.getType match {
          case VinNode.Type.MARK_VDS =>
            if (!node.getMarkVDS.hasMarkCode) {} else {
              val mark = MarkVDS(
                node.getMarkVDS.getMarkCode,
                buildVDS(node.getMarkVDS.getVdsList.asScala.toList)
              )
              markVDSs += (mark.code -> mark)
            }
          case VinNode.Type.VIN_LOGIC =>
            val vinLogic = VinLogic(
              node.getVinLogic.getId,
              getOpt(node.getVinLogic.hasName, node.getVinLogic.getName),
              node.getVinLogic.getMarksList.asScala.toList,
              node.getVinLogic.getPositionList.asScala.toList.map(_.toInt)
            )
            vinLogics += (vinLogic.id -> vinLogic)
          case VinNode.Type.WMI_MESSAGE =>
            val wmi = WMI(
              node.getWmi.getId,
              getOpt(node.getWmi.hasCode, node.getWmi.getCode),
              node.getWmi.getMarksList.asScala.toList,
              getOpt(node.getWmi.hasModelYearPosition, node.getWmi.getModelYearPosition),
              node.getWmi.getChecksum
            )
            WMIs += (wmi.id -> wmi)
        }
      }
    WMIs.isEmpty should not be true
    vinLogics.isEmpty should not be true
    markVDSs.isEmpty should not be true
    markVDSs.get("BMW") should not be None
  }

  private def prepareController(): Controller = {
    val file = File.createTempFile("vin", "")
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

  private def getOpt[T](hasField: Boolean, getField: => T) =
    if (hasField) Some(getField) else None

  private def buildVDS(list: Seq[VinNode.VDS]): Seq[VDS] =
    list.map(vds =>
      VDS(
        getOpt(vds.hasCode, vds.getCode),
        getOpt(vds.hasDest, vds.getDest),
        vds.getUrlList.iterator().asScala.toList,
        getOpt(vds.hasVINLogic, vds.getVINLogic),
        getOpt(vds.hasModelCode, vds.getModelCode),
        getOpt(vds.hasComments, vds.getComments),
        getOpt(vds.getSuperGen.hasCode, vds.getSuperGen.getCode.toLong),
        vds.getConfigurationsList.asScala.map(_.getCode.toLong).toList
      )
    )

}
