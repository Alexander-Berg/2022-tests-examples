package ru.auto.api.managers.offers

import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import com.google.protobuf.util.Timestamps
import org.mockito.Mockito.verify
import ru.auto.api.BaseSpec
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.app2app.App2AppManager
import ru.auto.api.managers.counters.CountersManager
import ru.auto.api.model.ModelUtils._
import ru.auto.api.model._
import ru.auto.api.services.hydra.HydraClient
import ru.auto.api.services.telepony.TeleponyClient
import ru.auto.api.services.telepony.TeleponyClient.{Domains => TeleponyDomains}
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.{Request, RequestImpl}
import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, CompositeFeatureTypes, InMemoryFeatureRegistry}

import java.time.{LocalDate, OffsetDateTime}
import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * Created by mcsim-gr on 11.09.17.
  */
class OfferStatManagerSpec extends BaseSpec with MockitoSupport {
  val vosClient: VosClient = mock[VosClient]
  val teleponyClient: TeleponyClient = mock[TeleponyClient]
  val hydraClient = mock[HydraClient]
  val countersManager = mock[CountersManager]

  lazy val featureRegistry = new InMemoryFeatureRegistry(
    new CompositeFeatureTypes(Seq(BasicFeatureTypes))
  )

  lazy val featureManager = new FeatureManager(featureRegistry)

  lazy val app2AppManager = mock[App2AppManager]

  val statsManager =
    new OfferStatManager(countersManager, vosClient, teleponyClient, hydraClient, featureManager, app2AppManager)
  implicit val trace: Traced = Traced.empty

  implicit private val request: Request = {
    val r = new RequestImpl
    r.setTrace(trace)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r
  }

  "OfferStatsManager" should {
    "return stats for offer" in {
      val offer = ModelGenerators.OfferGen.next
      val stats = ModelGenerators.DailyCounterListGen.next.getCountersList.asScala.toSeq
      val callsStat = ModelGenerators.phoneCallsStatsGen(stats.map(_.getDate).toList).next
      val user = ModelGenerators.RegisteredUserRefGen.next
      when(countersManager.getCountersByDay(?, ?, ?, ?)(?)).thenReturnF(Map(offer.id -> stats))
      when(vosClient.getUserOfferCallsCounters(?, ?, ?, ?, ?, ?)(?)).thenReturnF(callsStat)
      when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)

      val fromDate = LocalDate.now().minusDays(10L)
      val toDate = LocalDate.now().plusDays(10L)

      val result = statsManager
        .getUserOfferStat(CategorySelector.from(offer.getCategory), user, offer.id, fromDate, toDate)
        .futureValue

      val resultStats = result.getItems(0)
      resultStats.getOfferId shouldBe offer.id.toString

      resultStats.getCountersList.asScala
        .map(daily => daily.getDate -> daily.getPhoneCalls)
        .toMap shouldEqual callsStat.getDailyCallsMap.asScala

      resultStats.getCountersList.asScala
        .map(daily => daily.getDate -> daily.getFavorite)
        .toMap should contain theSameElementsAs stats.map(counter => counter.getDate -> counter.getFavorite).toMap

      resultStats.getCountersList.asScala
        .map(daily => daily.getDate -> daily.getFavoriteRemove)
        .toMap should contain theSameElementsAs stats.map(counter => counter.getDate -> counter.getFavoriteRemove).toMap

      resultStats.getCountersList.asScala
        .map(daily => daily.getDate -> daily.getFavoriteTotal)
        .toMap should contain theSameElementsAs stats.map(counter => counter.getDate -> counter.getFavoriteTotal).toMap

      resultStats.getCountersList.asScala
        .map(daily => daily.getDate -> daily.getAvitoViews)
        .toMap should contain theSameElementsAs stats.map(counter => counter.getDate -> counter.getAvitoViews).toMap

      resultStats.getCountersList.asScala
        .map(daily => daily.getDate -> daily.getAvitoPhoneViews)
        .toMap should contain theSameElementsAs stats
        .map(counter => counter.getDate -> counter.getAvitoPhoneViews)
        .toMap

      resultStats.getCountersList.asScala
        .map(daily => daily.getDate -> daily.getDromViews)
        .toMap should contain theSameElementsAs stats.map(counter => counter.getDate -> counter.getDromViews).toMap

      resultStats.getCountersList.asScala
        .map(daily => daily.getDate -> daily.getDromPhoneViews)
        .toMap should contain theSameElementsAs stats.map(counter => counter.getDate -> counter.getDromPhoneViews).toMap

    }

    "return calls stats for offer" in {
      val offer = ModelGenerators.OfferGen.next
      val now = OffsetDateTime.now()
      val nowTs = Timestamps.fromMillis(now.toInstant.toEpochMilli)
      val domain = TeleponyDomains.AutoUsers.toString

      when(teleponyClient.getCallsStats(eq(domain), eq(offer.id.id.toString), ?, ?)(?))
        .thenReturn(Future.successful(TeleponyClient.CallsStats(1, Some(now))))

      val result = statsManager
        .getLastCallsStatResponse(CategorySelector.from(offer.getCategory), offer.id)
        .futureValue

      result.getCount shouldBe 1
      result.getLastCallTimestamp shouldBe nowTs
    }

    "return call history" in {
      val user = ModelGenerators.PrivateUserRefGen.next
      val offer = ModelGenerators.OfferGen.next
      val category = CategorySelector.from(offer.getCategory)
      val offerId = OfferID.parse(offer.getId)
      val tpInfo = offer.getSeller.getTeleponyInfo
      val paging = Paging(1, 10)
      val callHistoryResponse = ModelGenerators.callHistoryResponseGen.next

      when(vosClient.getUserOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)
      when(teleponyClient.calls(?, ?, ?, ?)(?)).thenReturnF(callHistoryResponse)

      statsManager.getCallHistory(user, category, offerId, paging).futureValue
      verify(vosClient).getUserOffer(
        category,
        user,
        offerId,
        includeRemoved = true,
        forceTeleponyInfo = true,
        executeOnMaster = false
      )
      verify(teleponyClient).calls(tpInfo.getDomain, user, tpInfo.getObjectId, paging)
    }
  }
}
