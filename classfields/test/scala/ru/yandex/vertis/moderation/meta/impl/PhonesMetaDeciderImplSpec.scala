package ru.yandex.vertis.moderation.meta.impl

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.moderation.{Globals, SpecBase}
import ru.yandex.vertis.moderation.dao.{InstanceDao, SearchInstanceDao}
import ru.yandex.vertis.moderation.meta.PhonesMetaDecider
import ru.yandex.vertis.moderation.model.InstanceId
import ru.yandex.vertis.moderation.model.context.Context
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, ExternalId, Instance, UpdateJournalRecord}
import ru.yandex.vertis.moderation.model.meta.MetadataFetchRequest.{Phones, YandexMoneyPhones}
import ru.yandex.vertis.moderation.model.meta.{MetadataFetchRequest, MetadataSet}
import ru.yandex.vertis.moderation.model.signal.SignalSet
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials._
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.{Service, Visibility}
import ru.yandex.vertis.moderation.util.{Page, SlicedResult}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class PhonesMetaDeciderImplSpec extends SpecBase {

  import PhonesMetaDeciderImplSpec._

  private val ownerInstanceDao: InstanceDao[Future] = mock[InstanceDao[Future]]
  private val searchInstanceDao: SearchInstanceDao = mock[SearchInstanceDao]

  implicit private val featureRegistry: FeatureRegistry = new InMemoryFeatureRegistry(BasicFeatureTypes)

  private def decider(service: Service): PhonesMetaDecider =
    new PhonesMetaDeciderImpl(
      searchInstanceDao = searchInstanceDao,
      ownerInstanceDao = ownerInstanceDao,
      opinionCalculator = Globals.opinionCalculator(service)
    )

  private val createDateTime: DateTime = DateTimeGen.next
  private val ofValidEssentials: AutoruEssentials =
    AutoruEssentialsGen.next
      .copy(
        geobaseId = Seq(2),
        category = Some(Category.CARS),
        section = Some(Section.USED),
        sellerType = Some(SellerType.PRIVATE),
        condition = Some(Condition.EXCELLENT),
        isCallCenter = false,
        isPlacedForFree = Some(true),
        timestampCreate = Some(createDateTime),
        phones = AutoruPhoneGen.next(1).toMap
      )

  private val visibleContext: Context =
    ContextGen.next.copy(visibility = Model.Visibility.VISIBLE, updateTime = Some(createDateTime))

  private val ofValidInstance: Instance =
    InstanceGen.next.copy(
      essentials = ofValidEssentials,
      metadata = MetadataSet.Empty,
      context = visibleContext.copy(updateTime = Some(createDateTime)),
      createTime = createDateTime,
      essentialsUpdateTime = createDateTime,
      signals = SignalSet.Empty
    )

  private val yandexMoneyValidEssentials: AutoruEssentials =
    ofValidEssentials.copy(
      phones = AutoruPhoneGen.next(10).toMap
    )
  private val yandexMoneyValidInstance: Instance = ofValidInstance.copy(essentials = yandexMoneyValidEssentials)

  private val testCases: Seq[TestCase] =
    Seq(
      TestCase(
        description = "OF match by phone amount",
        instance = ofValidInstance,
        prepare =
          () => {
            setEnableOfFeature(true)
            setOnlyMoscowFeature(false)
            setCheckOfferUserCreateTimeFeature(false)
            setupDaoForOfRequest()
          },
        check =
          (r: Seq[MetadataFetchRequest]) =>
            r.exists {
              case Phones(phones) => phones == ofValidInstance.essentials.getPhones.toSet
              case _              => false
            },
        decider = decider(ofValidInstance.service)
      ),
      TestCase(
        description = "OF mismatch by phone amount",
        instance = ofValidInstance.copy(essentials = ofValidEssentials.copy(phones = AutoruPhoneGen.next(10).toMap)),
        prepare =
          () => {
            setEnableOfFeature(true)
            setOnlyMoscowFeature(false)
            setCheckOfferUserCreateTimeFeature(false)
            setupDaoForOfRequest()
          },
        check = (r: Seq[MetadataFetchRequest]) => !r.exists(_.isInstanceOf[Phones]),
        decider = decider(ofValidInstance.service)
      ),
      TestCase(
        description = "OF match with onlyMoscow feature",
        instance = ofValidInstance.copy(essentials = ofValidEssentials.copy(geobaseId = Seq(1))),
        prepare =
          () => {
            setEnableOfFeature(true)
            setOnlyMoscowFeature(true)
            setCheckOfferUserCreateTimeFeature(false)
            setupDaoForOfRequest()
          },
        check =
          (r: Seq[MetadataFetchRequest]) =>
            r.exists {
              case Phones(phones) => phones == ofValidInstance.essentials.getPhones.toSet
              case _              => false
            },
        decider = decider(ofValidInstance.service)
      ),
      TestCase(
        description = "OF mismatch with onlyMoscow feature",
        instance = ofValidInstance,
        prepare =
          () => {
            setEnableOfFeature(true)
            setOnlyMoscowFeature(true)
            setCheckOfferUserCreateTimeFeature(false)
            setupDaoForOfRequest()
          },
        check = (r: Seq[MetadataFetchRequest]) => !r.exists(_.isInstanceOf[Phones]),
        decider = decider(ofValidInstance.service)
      ),
      TestCase(
        description = "OF match with ensureOfferAndUserHaveSimilarCreateTime feature",
        instance = ofValidInstance,
        prepare =
          () => {
            setEnableOfFeature(true)
            setOnlyMoscowFeature(false)
            setCheckOfferUserCreateTimeFeature(true)
            setupDaoForOfRequest(changeInstance = _.copy(createTime = createDateTime.minusHours(5)))
          },
        check =
          (r: Seq[MetadataFetchRequest]) =>
            r.exists {
              case Phones(phones) => phones == ofValidInstance.essentials.getPhones.toSet
              case _              => false
            },
        decider = decider(ofValidInstance.service)
      ),
      TestCase(
        description = "OF mismatch with ensureOfferAndUserHaveSimilarCreateTime feature",
        instance = ofValidInstance,
        prepare =
          () => {
            setEnableOfFeature(true)
            setOnlyMoscowFeature(false)
            setCheckOfferUserCreateTimeFeature(true)
            setupDaoForOfRequest(changeInstance = _.copy(createTime = createDateTime.minusHours(7)))
          },
        check = (r: Seq[MetadataFetchRequest]) => !r.exists(_.isInstanceOf[Phones]),
        decider = decider(ofValidInstance.service)
      ),
      TestCase(
        description = "OF match with ensureOfferIsFirst feature if user has no offers in statistics",
        instance = ofValidInstance,
        prepare =
          () => {
            setEnableOfFeature(true)
            setOnlyMoscowFeature(false)
            setCheckOfferUserCreateTimeFeature(false)
            setEnsureOfferIsFirstFeature(true)
            val offersMeta =
              OffersStatisticsMetadataGen.suchThat(_.statistics.offersVisibilitySummary.values.sum == 0).next
            setupDaoForOfRequest(changeInstance = _.copy(metadata = MetadataSet(offersMeta)))
          },
        check =
          (r: Seq[MetadataFetchRequest]) =>
            r.exists {
              case Phones(phones) => phones == ofValidInstance.essentials.getPhones.toSet
              case _              => false
            },
        decider = decider(ofValidInstance.service)
      ),
      TestCase(
        description = "OF mismatch with ensureOfferIsFirst feature if user has > 1 offers in statistics",
        instance = ofValidInstance,
        prepare =
          () => {
            setEnableOfFeature(true)
            setOnlyMoscowFeature(false)
            setCheckOfferUserCreateTimeFeature(false)
            val offersMeta =
              OffersStatisticsMetadataGen.suchThat(_.statistics.offersVisibilitySummary.values.sum > 1).next
            setupDaoForOfRequest(changeInstance = _.copy(metadata = MetadataSet(offersMeta)))
          },
        check = (r: Seq[MetadataFetchRequest]) => !r.exists(_.isInstanceOf[Phones]),
        decider = decider(ofValidInstance.service)
      ), {
        val offerRecord = UpdateJournalRecordGen.next.copy(instance = ofValidInstance)
        TestCase(
          description =
            "OF match with ensureOfferIsFirst feature if user has one offer in statistics and metadata was recently updated",
          offerRecord = offerRecord,
          prepare =
            () => {
              setEnableOfFeature(true)
              setOnlyMoscowFeature(false)
              setCheckOfferUserCreateTimeFeature(false)
              setEnsureOfferIsFirstFeature(true)

              val offersMeta =
                OffersStatisticsMetadataGen
                  .suchThat(_.statistics.offersVisibilitySummary.values.sum == 1)
                  .next
                  .copy(timestamp = offerRecord.timestamp)
              setupDaoForOfRequest(changeInstance = _.copy(metadata = MetadataSet(offersMeta)))
            },
          check =
            (r: Seq[MetadataFetchRequest]) =>
              r.exists {
                case Phones(phones) => phones == ofValidInstance.essentials.getPhones.toSet
                case _              => false
              },
          decider = decider(offerRecord.instance.service)
        )
      }, {
        val offerRecord = UpdateJournalRecordGen.next.copy(instance = ofValidInstance)
        TestCase(
          description = "OF mismatch with ensureOfferIsFirst feature if user has 1 old offer in statistics",
          offerRecord = offerRecord,
          prepare =
            () => {
              setEnableOfFeature(true)
              setOnlyMoscowFeature(false)
              setCheckOfferUserCreateTimeFeature(false)
              setEnsureOfferIsFirstFeature(true)
              val offersMeta =
                OffersStatisticsMetadataGen
                  .suchThat(_.statistics.offersVisibilitySummary.values.sum == 1)
                  .next
                  .copy(timestamp = offerRecord.timestamp.minusMillis(1))
              setupDaoForOfRequest(changeInstance = _.copy(metadata = MetadataSet(offersMeta)))
            },
          check = (r: Seq[MetadataFetchRequest]) => !r.exists(_.isInstanceOf[Phones]),
          decider = decider(offerRecord.instance.service)
        )
      },
      TestCase(
        description = "OF mismatch by phone search",
        instance = ofValidInstance,
        prepare =
          () => {
            setEnableOfFeature(true)
            setOnlyMoscowFeature(false)
            setCheckOfferUserCreateTimeFeature(false)
            setupDaoForOfRequest(values = InstanceIdGen.next(2).toSeq)
          },
        check = (r: Seq[MetadataFetchRequest]) => !r.exists(_.isInstanceOf[Phones]),
        decider = decider(ofValidInstance.service)
      ),
      TestCase(
        description = "YandexMoney match by phone amount",
        instance = yandexMoneyValidInstance,
        prepare =
          () => {
            setEnableYandexMoneyFeature(true)
            setOnlyMoscowFeature(false)
            setCheckOfferUserCreateTimeFeature(false)
            setupDaoForOfRequest()
          },
        check =
          (r: Seq[MetadataFetchRequest]) =>
            r.exists {
              case YandexMoneyPhones(phones) => phones.size == 2
              case _                         => false
            },
        decider = decider(yandexMoneyValidInstance.service)
      ),
      TestCase(
        description = "Of if checkOnlyFirstVisible disabled and offer is not new",
        instance = ofValidInstance,
        prevInstance =
          Some(
            ofValidInstance.copy(context =
              ofValidInstance.context.copy(visibility = VisibilityGen.filter(_ != Visibility.VISIBLE).next)
            )
          ),
        prepare =
          () => {
            setCheckOnlyFirstVisible(false)
            setEnableYandexMoneyFeature(true)
            setOnlyMoscowFeature(false)
            setCheckOfferUserCreateTimeFeature(false)
            setEnsureOfferIsFirstFeature(false)
            setupDaoForOfRequest()
          },
        check =
          (r: Seq[MetadataFetchRequest]) =>
            r.exists {
              case Phones(phones) => phones == ofValidInstance.essentials.getPhones.toSet
              case _              => false
            },
        decider = decider(ofValidInstance.service)
      ),
      TestCase(
        description = "Of if checkOnlyFirstVisible enabled and offer is new, but now visible",
        instance =
          ofValidInstance.copy(context =
            ofValidInstance.context.copy(visibility = VisibilityGen.filter(_ != Visibility.VISIBLE).next)
          ),
        prevInstance = None,
        prepare =
          () => {
            setCheckOnlyFirstVisible(true)
            setEnableYandexMoneyFeature(true)
            setOnlyMoscowFeature(false)
            setCheckOfferUserCreateTimeFeature(false)
            setupDaoForOfRequest()
          },
        check = (r: Seq[MetadataFetchRequest]) => !r.exists(_.isInstanceOf[Phones]),
        decider = decider(ofValidInstance.service)
      )
    )

  private def setupDaoForOfRequest(values: Seq[InstanceId] = Seq.empty,
                                   changeInstance: Instance => Instance = identity
                                  ): Unit = {
    doReturn(
      Future.successful(
        SlicedResult(values = values, total = 0L, slice = Page(0, 10))
      )
    )
      .when(searchInstanceDao)
      .searchIds(any(), any(), any())
    doReturn(
      Future.successful(
        Some(changeInstance(InstanceGen.next))
      )
    )
      .when(ownerInstanceDao)
      .getOpt(any[ExternalId](), any())
  }

  private def setOnlyMoscowFeature(value: Boolean): Unit = setBooleanFeature("phone-meta-decider-only-moscow", value)

  private def setCheckOfferUserCreateTimeFeature(value: Boolean): Unit =
    setBooleanFeature("phone-meta-decider-check-offer-user-create-time", value)

  private def setEnableOfFeature(value: Boolean): Unit = setBooleanFeature("phone-meta-enable-of", value)

  private def setEnableYandexMoneyFeature(value: Boolean): Unit =
    setBooleanFeature("phone-meta-enable-yandex-money", value)

  private def setEnsureOfferIsFirstFeature(value: Boolean): Unit =
    setBooleanFeature("phone-meta-decider-check-only-first-offer", value)

  private def setCheckOnlyFirstVisible(value: Boolean): Unit =
    setBooleanFeature("phone-meta-check-only-first-visible", value)

  private def setBooleanFeature(name: String, value: Boolean): Unit =
    featureRegistry.updateFeature(name, value).futureValue

  "PhonesMetaDeciderImpl" should {
    testCases.foreach { case TestCase(description, record, prepare, check, decider) =>
      description in {
        prepare.apply()
        check(decider.decide(record).futureValue) shouldBe true
      }
    }
  }
}

object PhonesMetaDeciderImplSpec {

  private case class TestCase(description: String,
                              offerRecord: UpdateJournalRecord,
                              prepare: () => Unit,
                              check: Seq[MetadataFetchRequest] => Boolean,
                              decider: PhonesMetaDecider
                             )

  private object TestCase {
    def apply(description: String,
              instance: Instance,
              prevInstance: Option[Instance] = None,
              prepare: () => Unit,
              check: Seq[MetadataFetchRequest] => Boolean,
              decider: PhonesMetaDecider
             ): TestCase = {
      val record = UpdateJournalRecordGen.next.copy(instance = instance, prev = prevInstance)
      TestCase(description, record, prepare, check, decider)
    }
  }

}
