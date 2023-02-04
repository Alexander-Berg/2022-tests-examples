package ru.yandex.vertis.clustering.services.impl

import java.io.File

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.extdata.core.Data.FileData
import ru.yandex.extdata.core.{Controller, DataType, Instance, InstanceHeader}
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.dao.GraphDao.FeatureNodes
import ru.yandex.vertis.clustering.dao.impl.GraphDaoImpl
import ru.yandex.vertis.clustering.edl.GraphGenerationFetcher
import ru.yandex.vertis.clustering.model.Domains.Domain
import ru.yandex.vertis.clustering.model.GraphGeneration._
import ru.yandex.vertis.clustering.model.{Domains, FeatureTypes}
import ru.yandex.vertis.clustering.utils.DateTimeUtils

import scala.language.reflectiveCalls
import scala.util.Success

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class ExtDataGraphSpec extends BaseSpec {

  private val firstGenProduced = DateTime.now().minusDays(30)
  private val secondGenProduced = DateTime.now()

  private val firstGenId: GraphGenerationId = DateTimeUtils
    .fromJoda(firstGenProduced)
    .toInstant
    .toEpochMilli
    .toString
  private val secondGenId: GraphGenerationId = DateTimeUtils
    .fromJoda(secondGenProduced)
    .toInstant
    .toEpochMilli
    .toString

  private val tarGzFile = new File(ClassLoader.getSystemResource("generation.data").toURI)
  private val tarGzFile2 = new File(ClassLoader.getSystemResource("generation_2.data").toURI)

  private val testWorkDir = new File(tarGzFile.getParentFile, "workdir")
  testWorkDir.mkdir()

  private val extDataGraph = new ExtDataGraph {
    override protected val extDataType: DataType = GraphGenerationFetcher.AutoruGraphGenerationDataType
    override def domain: Domain = Domains.Autoru
    override protected def edlClientController: Controller = None.orNull
    override protected def workDir: File = testWorkDir
    def push(instance: Instance): Unit = processExtDataInstance(instance)
  }

  val graphExtDataInstance =
    Instance(InstanceHeader(firstGenId, 1, firstGenProduced), FileData(tarGzFile))

  val graphExtDataInstance2 =
    Instance(InstanceHeader(secondGenId, 2, secondGenProduced), FileData(tarGzFile2))

  extDataGraph.push(graphExtDataInstance)

  "ExtDataGraph" should {

    "be empty at start" in {
      extDataGraph.generation shouldBe None
    }

    "rotates for ext instance" in {
      extDataGraph.rotate()
      extDataGraph.service shouldBe a[Success[_]]
      extDataGraph.generation.get.id shouldBe firstGenId
      FeatureTypes.values.map { featureType =>
        new GraphDaoImpl(extDataGraph, GraphDaoImpl.defaultPolicyForDomain(Domains.Autoru))
          .countNodes(FeatureNodes(featureType))
          .get
      }.sum shouldBe 14243698L
    }

    "success after second rotate" in {
      extDataGraph.push(graphExtDataInstance2)
      extDataGraph.rotate().get
      extDataGraph.service shouldBe a[Success[_]]
      extDataGraph.generation.get.id shouldBe secondGenId

      FeatureTypes.values.map { featureType =>
        new GraphDaoImpl(extDataGraph, GraphDaoImpl.defaultPolicyForDomain(Domains.Autoru))
          .countNodes(FeatureNodes(featureType))
          .get
      }.sum shouldBe 14243699L
    }
  }

}
