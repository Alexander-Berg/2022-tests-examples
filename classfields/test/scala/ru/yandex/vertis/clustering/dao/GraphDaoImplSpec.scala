package ru.yandex.vertis.clustering.dao

import java.io.File
import java.time.ZonedDateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.dao.GraphDao.{FeatureNodes, UserNodes}
import ru.yandex.vertis.clustering.dao.impl.GraphDaoImpl
import ru.yandex.vertis.clustering.model._
import ru.yandex.vertis.clustering.proto.Model.ClusteringFormula
import ru.yandex.vertis.clustering.services.impl.GraphInstanceImpl
import ru.yandex.vertis.clustering.utils.DateTimeUtils
import ru.yandex.vertis.clustering.utils.features.FeatureHelpers

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class GraphDaoImplSpec extends BaseSpec {

  "GraphDaoImpl" should {

    val tarGzFile = new File(ClassLoader.getSystemResource("generation.data").toURI)
    val workDir = new File(tarGzFile.getParentFile, "workdir")
    workDir.mkdir()
    val neo4jGeneration = PackedGraphGeneration(DateTimeUtils.now, tarGzFile)
      .unpack(workDir)(Neo4jGraphGeneration.apply)
    val graph = new GraphInstanceImpl(neo4jGeneration, Domains.Autoru)
    graph.generation should be(Some(neo4jGeneration))

    val graphDao = new GraphDaoImpl(graph, GraphDaoImpl.defaultPolicyForDomain(Domains.Autoru))
    val baseUsersNumber = 3437815L
    val baseFeaturesNumber = 14243698L

    "return true numbers apply test graph" in {
      graphDao.countNodes(UserNodes).get shouldBe baseUsersNumber

      FeatureTypes.values.map { featureType =>
        graphDao.countNodes(FeatureNodes(featureType)).get
      }.sum shouldBe baseFeaturesNumber
    }

    case class FactInsertTest(fact: Fact, usersNumber: Long, featuresNumber: Long, lastFactEpoch: ZonedDateTime)

    val fresh = ZonedDateTime.now
    val alice = AutoruUser("alice")
    val bob = AutoruUser("bob")
    val phone1 = FeatureHelpers.parsePhoneNet("+79099752718")
    val phone2 = FeatureHelpers.parsePhoneNet("+79652861016")

    val tests = Seq(
      FactInsertTest(Fact(alice, phone1, fresh),
                     usersNumber = baseUsersNumber + 1,
                     featuresNumber = baseFeaturesNumber + 1,
                     lastFactEpoch = fresh),
      FactInsertTest(Fact(bob, phone1, fresh.minusDays(1)),
                     usersNumber = baseUsersNumber + 2,
                     featuresNumber = baseFeaturesNumber + 1,
                     lastFactEpoch = fresh),
      FactInsertTest(Fact(bob, phone2, fresh.plusDays(1)),
                     usersNumber = baseUsersNumber + 2,
                     featuresNumber = baseFeaturesNumber + 2,
                     lastFactEpoch = fresh.plusDays(1))
    )

    tests.foreach { test =>
      s"correctly adds feature in $test" in {
        graphDao.upsertFact(test.fact)
        graphDao.countNodes(UserNodes).get shouldBe test.usersNumber
        graphDao.version.get.lastFactEpoch.get shouldBe test.lastFactEpoch

        FeatureTypes.values.map { featureType =>
          graphDao.countNodes(FeatureNodes(featureType)).get
        }.sum shouldBe test.featuresNumber
      }
    }

    "iterate over all facts" in {
      graphDao.getAllFacts(_.size).get shouldBe 3
    }

    "raise exception IllegalArgumentException if arguments are not correct" in {
      intercept[IllegalArgumentException] {
        graphDao.getAllClusters(ClusteringFormula.L2_STRICT)(_ => ()).get
      }
    }

    case object SpecificException extends Exception

    "raise exception if exception was thrown in iterator handle function" in {
      intercept[SpecificException.type] {
        graphDao.getAllClusters(ClusteringFormula.L1_STRICT)(_ => throw SpecificException).get
      }
    }
  }

}
