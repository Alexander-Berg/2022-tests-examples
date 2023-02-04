package ru.yandex.vertis.clustering.services

import java.io.File
import java.sql.DriverManager
import java.time.{Instant, LocalTime, ZonedDateTime}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import ru.yandex.vertis.clustering.BaseSpec
import ru.yandex.vertis.clustering.config.{BuilderConfig, DomainBuilderConfig}
import ru.yandex.vertis.clustering.dao.FactsDao
import ru.yandex.vertis.clustering.dao.GraphDao.UserNodes
import ru.yandex.vertis.clustering.dao.impl.GraphDaoImpl
import ru.yandex.vertis.clustering.derivatives.DerivativeFactsBuilderImpl
import ru.yandex.vertis.clustering.derivatives.impl.BuilderDerivativeFeaturesBuilder
import ru.yandex.vertis.clustering.model.Domains.Domain
import ru.yandex.vertis.clustering.model._
import ru.yandex.vertis.clustering.services.impl.{BuilderServiceImpl, GraphInstanceImpl}
import ru.yandex.vertis.clustering.utils.DateTimeUtils
import ru.yandex.vertis.clustering.utils.DateTimeUtils._
import ru.yandex.vertis.clustering.utils.features.FeatureHelpers
import ru.yandex.yql.YqlDriver

import scala.util.{Success, Try}

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class BuilderServiceImplSpec extends BaseSpec with GeneratorDrivenPropertyChecks {

  import BuilderServiceImplSpec._

  DriverManager.registerDriver(new YqlDriver)

  private val ips = Seq(
    Fact(AutoruUser("1"), FeatureHelpers.parseIp("128.156.179.190"), now),
    Fact(AutoruUser("1"), FeatureHelpers.parseIpNet("128.156.179.190"), now),
    Fact(AutoruUser("2"), FeatureHelpers.parseIp("128.156.179.190"), now),
    Fact(AutoruUser("1"), FeatureHelpers.parseIpNet("128.156.179.190"), now),
    Fact(AutoruUser("3"), FeatureHelpers.parseIp("128.156.179.190"), now),
    Fact(AutoruUser("3"), FeatureHelpers.parseIpNet("128.156.179.190"), now)
  )

  private val features = (1 to 1000).map(_ => factRecordGenerator.sample.get).sortBy(_.feature.value) ++ ips

  val featureLogDao: FactsDao = new FactsDao {
    override def readLogs(): Try[Iterator[Fact]] =
      Success(features.iterator)
  }

  import scala.concurrent.duration._

  val builderConfig: BuilderConfig = new BuilderConfig {

    override def domainConfig(domain: Domain): DomainBuilderConfig = new DomainBuilderConfig {
      override val interval: FiniteDuration = 100.minutes
      override val startTime: LocalTime = LocalTime.now()
    }
    override val actualEpochGap: FiniteDuration = 10.minutes
    override val port = 50
  }

  implicit val system: ActorSystem = ActorSystem("user-clustering-builder")
  implicit val materializer: Materializer = ActorMaterializer()

  private val derivativeFeatures = new BuilderDerivativeFeaturesBuilder
  private val derivativeFactsBuilder = new DerivativeFactsBuilderImpl(derivativeFeatures)

  private val neo4jService = new BuilderServiceImpl(featureLogDao,
                                                    builderConfig,
                                                    Domains.Autoru,
                                                    _ => true,
                                                    Set(FeatureTypes.SuidType, FeatureTypes.IpType),
                                                    derivativeFactsBuilder)

  "BuilderServiceImpl" should {
    "build readable new GraphGeneration" in {

      val graphDir = new File("./test/build")

      if (graphDir.isDirectory) {
        FileUtils.deleteDirectory(graphDir)
      }

      val generated = neo4jService.build(graphDir).get
      val packed = generated.pack(new File("./test/"))

      val unpacked = packed.unpack(new File("./test/"))(Neo4jGraphGeneration.apply)
      val graph = new GraphInstanceImpl(unpacked, Domains.Autoru)

      graph.service shouldBe a[Success[_]]
      val graphDao = new GraphDaoImpl(graph, GraphDaoImpl.defaultPolicyForDomain(Domains.Autoru))
      graphDao.countNodes(UserNodes).get should be > 0L

      graph.shutdown shouldBe a[Success[_]]
    }
  }
}

object BuilderServiceImplSpec {
  private val SuidSalt = "0VskO97ak0ywF5TLbHF^Am@ZojqeNP3bRC#YVH^v.tZTxuSXnC6tQJprLBl!CO&U&z6lHCm@%lVXyU52K"

  private val suidGen: Gen[Suid] = for {
    randomString <- Gen.nonEmptyListOf[Char](Gen.alphaNumChar).map(_.mkString)
    hash = DigestUtils.md5Hex(randomString)
    hashCheck = DigestUtils.md5Hex(s"$hash$SuidSalt")
  } yield Suid(s"$hash.$hashCheck")

  private val phoneGen: Gen[Phone] = for {
    phoneStr <- Gen.listOfN(7, Gen.numChar).map(_.mkString)
  } yield FeatureHelpers.parsePhone("7965" + phoneStr)

  private val factRecordGenerator: Gen[Fact] = for {
    userId <- Gen.chooseNum(0, 100).map(x => s"user$x")
    ts <- Gen.chooseNum(1388534400, 1483228800)
    suidFeature <- Gen.oneOf(suidGen, phoneGen)
    user = AutoruUser(userId)
  } yield Fact(user, suidFeature, ZonedDateTime.ofInstant(Instant.ofEpochSecond(ts), DateTimeUtils.DefaultZoneId))
}
