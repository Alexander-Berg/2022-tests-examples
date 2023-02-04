package ru.yandex.vertis.moderation.flink.bureau.geoinfo

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.TestUtils._
import ru.yandex.vertis.moderation.flink.bureau.geoinfo.GeoInfoStatisticsExtractorSpec.TestCase
import ru.yandex.vertis.moderation.model.context.Context
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{
  AutoruEssentials,
  Diff,
  Essentials,
  InstanceIdImpl,
  RealtyEssentials,
  UpdateJournalRecord,
  User
}
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.DateTimeUtil

/**
  * @author mpoplavkov
  */
@RunWith(classOf[JUnitRunner])
class GeoInfoStatisticsExtractorSpec extends SpecBase {

  private val autoruService = Service.AUTORU
  private val autoruOwnerService = Gen.oneOf(Service.USERS_AUTORU, Service.DEALERS_AUTORU).next
  private val realtyService = Service.REALTY
  private val realtyOwnerService = Service.USERS_REALTY

  private val autoruGeoInfoExtractor: UpdateJournalRecord => Iterable[UserGeoInfoStatistics] =
    new GeoInfoStatisticsExtractor(autoruOwnerService)
  private val realtyGeoInfoExtractor: UpdateJournalRecord => Iterable[UserGeoInfoStatistics] =
    new GeoInfoStatisticsExtractor(realtyOwnerService)

  val geoId1 = IntGen.next
  val geoId2 = IntGen.next
  val geoId3 = IntGen.next

  val cases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "extract geo info from realty offer",
        service = realtyService,
        geoIds = Seq(geoId1),
        isNewInstance = true,
        expectedCount = 1,
        expectedGeoId = geoId1
      ),
      TestCase(
        description = "do not count geoId if the offer is old for realty",
        service = realtyService,
        geoIds = Seq(geoId1),
        isNewInstance = false,
        expectedCount = 0,
        expectedGeoId = geoId1
      ),
      TestCase(
        description = "extract geo info from autoru offer",
        service = autoruService,
        geoIds = Seq(geoId1),
        isNewInstance = true,
        expectedCount = 1,
        expectedGeoId = geoId1
      ),
      TestCase(
        description = "do not count geoId if the offer is old for autoru",
        service = autoruService,
        geoIds = Seq(geoId1),
        isNewInstance = false,
        expectedCount = 0,
        expectedGeoId = geoId1
      ),
      TestCase(
        description = "extract first geo id from autoru offer",
        service = autoruService,
        geoIds = Seq(geoId1, geoId2, geoId3),
        isNewInstance = true,
        expectedCount = 1,
        expectedGeoId = geoId1
      )
    )

  "GeoInfoStatisticsExtractor" should {
    cases.foreach { case TestCase(description, service, geoIds, isNewInstance, expectedCount, expectedGeoId, ts) =>
      description in {
        val record = getUpdateJournalRecord(service, geoIds, ts, isNewInstance, Diff.all(service))
        val actual = getExtractor(service).apply(record)
        actual.size shouldBe 1
        val statistics = actual.head.statistics
        statistics.offersGeoInfoSummary.size shouldBe 1
        val (geoId, counter) = statistics.offersGeoInfoSummary.head
        geoId shouldBe expectedGeoId
        counter.timeInterval shouldBe 'defined
        val timeInterval = counter.timeInterval.get
        timeInterval.from shouldBe record.instance.createTime
        timeInterval.from shouldBe record.instance.essentialsUpdateTime
        counter.count shouldBe expectedCount
      }
    }

    Seq(realtyService, autoruService).foreach { service =>
      s"do not extract statistics if no geo ids present for ${service.toString.toLowerCase}" in {
        val record = getUpdateJournalRecord(service, Seq.empty, DateTime.now(), isNewInstance = true, Diff.all(service))
        val actual = getExtractor(service).apply(record)
        actual.size shouldBe 0
      }
    }
  }

  private def getUpdateJournalRecord(service: Service,
                                     geoIds: Seq[Int],
                                     timestamp: DateTime,
                                     isNewInstance: Boolean,
                                     diff: Diff
                                    ): UpdateJournalRecord = {
    val generated = InstanceGen.next
    val externalId = generated.externalId.copy(user = getUser(getOwnerService(service)))
    val instance =
      generated
        .copy(id = InstanceIdImpl(externalId).toId)
        .copy(essentials = essentialsWithGeoIds(service, geoIds))
        .copy(context = Context.Default)
        .copy(signals = SignalSet.Empty)
        .copy(essentialsUpdateTime = if (isNewInstance) generated.createTime else generated.essentialsUpdateTime)
    UpdateJournalRecord.withInitialDepth(
      prev = if (isNewInstance) None else Some(InstanceGen.next),
      instance = instance,
      timestamp = timestamp,
      diff = diff
    )
  }

  private def essentialsWithGeoIds(service: Service, geoIds: Seq[Int]): Essentials = {
    val essentials = essentialsGen(service).next
    essentials match {
      case realty: RealtyEssentials =>
        require(geoIds.size <= 1)
        val geoInfo = GeoInfoGen.next.copy(subjectFederationId = geoIds.headOption)
        realty.copy(geoInfo = Some(geoInfo))
      case autoru: AutoruEssentials =>
        autoru.copy(geobaseId = geoIds)
      case _ =>
        ???
    }
  }

  private def getOwnerService(service: Service): Service =
    service match {
      case Service.REALTY => realtyOwnerService
      case Service.AUTORU => autoruOwnerService
      case _              => ???
    }

  private def getUser(service: Service): User =
    service match {
      case Service.USERS_REALTY   => UserYandexGen.next
      case Service.USERS_AUTORU   => AutoruUserGen.next
      case Service.DEALERS_AUTORU => DealerUserGen.next
      case _                      => ???
    }

  private def getExtractor(service: Service): UpdateJournalRecord => Iterable[UserGeoInfoStatistics] =
    service match {
      case Service.REALTY => realtyGeoInfoExtractor
      case Service.AUTORU => autoruGeoInfoExtractor
      case _              => ???
    }

}

object GeoInfoStatisticsExtractorSpec {

  case class TestCase(description: String,
                      service: Service,
                      geoIds: Seq[Int],
                      isNewInstance: Boolean,
                      expectedCount: Int,
                      expectedGeoId: Int,
                      timestamp: DateTime = DateTimeUtil.now()
                     )

}
