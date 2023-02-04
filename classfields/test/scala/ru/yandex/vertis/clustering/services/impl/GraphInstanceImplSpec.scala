package ru.yandex.vertis.clustering.services.impl

import java.io.File
import java.util.zip.ZipException

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.dao.GraphDao.{FeatureNodes, UserNodes}
import ru.yandex.vertis.clustering.dao.impl.GraphDaoImpl
import ru.yandex.vertis.clustering.model.GraphGeneration._
import ru.yandex.vertis.clustering.model._

import scala.util.Success

/**
  * @author alesavin
  */
@RunWith(classOf[JUnitRunner])
class GraphInstanceImplSpec extends BaseSpec {

  val firstGenId: GraphGenerationId = "1"
  val secondGenId: GraphGenerationId = "2"
  val thirdGenId: GraphGenerationId = "3"

  "GraphInstanceImpl" should {
    val tarGzFile = new File(ClassLoader.getSystemResource("generation.data").toURI)
    val brokenTarGz = new File(ClassLoader.getSystemResource("broken_generation.data").toURI)

    val workDir = new File(tarGzFile.getParentFile, "workdir")
    workDir.mkdir()

    "create new instance" in {
      val neo4jGeneration = PackedGraphGeneration(firstGenId, tarGzFile)
        .unpack(workDir)(Neo4jGraphGeneration.apply)
      val graph = new GraphInstanceImpl(neo4jGeneration, Domains.Autoru)
      graph.generation should be(Some(neo4jGeneration))

      new GraphDaoImpl(graph, GraphDaoImpl.defaultPolicyForDomain(Domains.Autoru))
        .countNodes(UserNodes)
        .get shouldBe 3437815L

      graph.shutdown shouldBe a[Success[_]]
    }

    "pass to create the same generation twice" in {
      val neo4jGeneration = PackedGraphGeneration(firstGenId, tarGzFile)
        .unpack(workDir)(Neo4jGraphGeneration.apply)
      val graph = new GraphInstanceImpl(neo4jGeneration, Domains.Autoru)

      graph.generation should be(Some(neo4jGeneration))

      val factsNumber = FeatureTypes.values.map { factType =>
        new GraphDaoImpl(graph, GraphDaoImpl.defaultPolicyForDomain(Domains.Autoru))
          .countNodes(FeatureNodes(factType))
          .get
      }.sum
      factsNumber shouldBe 14243698L

      new GraphInstanceImpl(neo4jGeneration, Domains.Autoru).shutdown

      graph.shutdown shouldBe a[Success[_]]
    }

    "create second instance" in {
      val neo4jGeneration1 = PackedGraphGeneration(secondGenId, tarGzFile)
        .unpack(workDir)(Neo4jGraphGeneration.apply)
      val graph1 = new GraphInstanceImpl(neo4jGeneration1, Domains.Autoru)

      val neo4jGeneration2 = PackedGraphGeneration(thirdGenId, tarGzFile)
        .unpack(workDir)(Neo4jGraphGeneration.apply)
      val graph2 = new GraphInstanceImpl(neo4jGeneration2, Domains.Autoru)

      graph1.shutdown shouldBe a[Success[_]]

      graph2.shutdown shouldBe a[Success[_]]
    }

    "fail to create instance if file is broken" in {
      intercept[ZipException] {
        PackedGraphGeneration("280028", brokenTarGz)
          .unpack(workDir)(Neo4jGraphGeneration.apply)
      }
    }
  }
}
