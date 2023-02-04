package ru.yandex.realty.feedprocessor.task

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.capa.CapaClient
import ru.yandex.capa.model.{PartnerEnvironment, PartnerEnvironmentForRequest}
import ru.yandex.partnerdata.feedloader.common.FeedloaderClient
import ru.yandex.realty.feedprocessor.dao.{CleanSchemaBeforeEach, FeedProcessorDAOBase}
import ru.yandex.realty.feedprocessor.model.PartnerFeedGenerator
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.{AsyncSpecBase, CommonConstants}

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class WatchFeedsTaskSpec
  extends AsyncSpecBase
  with FeedProcessorDAOBase
  with CleanSchemaBeforeEach
  with PartnerFeedGenerator {

  private val capaClient = mock[CapaClient]
  private val feedloaderClient = mock[FeedloaderClient]
  private val watchFeedsTask = new WatchFeedsTask(capaClient, feedloaderClient, partnerFeedDAOImpl)
  implicit private val traced: Traced = Traced.empty

  "WatchFeedsTask" should {
    "watch feeds" in {
      val vosPartnerId = CommonConstants.REALTY_VOS_PARTNER_ID
      val existingPartners = Seq(
        partnerFeedGen(1, "feed_1").next,
        partnerFeedGen(2, "feed_2").next,
        partnerFeedGen(3, "feed_3").next,
        partnerFeedGen(vosPartnerId, "feed_vos_1").next
      )
      val premoderationPartners = Seq(
        capaPartnerGen(1, Some(1), PartnerEnvironment.PREMODERATION).next,
        capaPartnerGen(4, Some(4), PartnerEnvironment.PREMODERATION).next, // new feed
        capaPartnerGen(6, None, PartnerEnvironment.PREMODERATION).next // partner without uid
      )
      val productionPartners = Seq(
        capaPartnerGen(2, Some(2), PartnerEnvironment.PRODUCTION).next,
        capaPartnerGen(5, Some(5), PartnerEnvironment.PRODUCTION).next, // new feed
        capaPartnerGen(vosPartnerId, Some(100), PartnerEnvironment.PRODUCTION).next // vos feed
      )
      val feeds = Seq(
        feedloaderFeedGen(1, "feed_1").next,
        feedloaderFeedGen(2, "feed_2_2").next, // feed url changed
        feedloaderFeedGen(4, "feed_4").next,
        feedloaderFeedGen(5, "feed_5").next,
        feedloaderFeedGen(vosPartnerId, "feed_vos_2").next // feed url changed
      )

      val ownPartnerIds =
        (premoderationPartners ++ productionPartners).filter(_.getClusterId != null).map(_.getId).toSet
      toMockFunction1(capaClient.getPartners(_: PartnerEnvironmentForRequest))
        .expects(PartnerEnvironmentForRequest.PREMODERATION)
        .once()
        .returning(premoderationPartners.asJava)

      toMockFunction1(capaClient.getPartners(_: PartnerEnvironmentForRequest))
        .expects(PartnerEnvironmentForRequest.PRODUCTION)
        .once()
        .returning(productionPartners.asJava)

      toMockFunction2(feedloaderClient.getLastValidFeeds(_: java.util.List[java.lang.Long], _: Int))
        .expects(
          where {
            case (partnerIds: java.util.List[java.lang.Long], typeId: Int) =>
              typeId == 1 && partnerIds.asScala.map(_.toLong).toSet == ownPartnerIds
          }
        )
        .once()
        .returning(feeds.asJava)
      val f = for {
        _ <- partnerFeedDAOImpl.upsertPartners(existingPartners)
        _ <- watchFeedsTask.run
      } yield ()
      f.futureValue
      val actual = partnerFeedDAOImpl.getAllFeeds.futureValue
      val expectedPartnerIds = Set(1, 2, 3, 4, 5, vosPartnerId)
      val expectedPartnerFeedUrls =
        Map(1 -> "feed_1", 2 -> "feed_2_2", 3 -> "feed_3", 4 -> "feed_4", 5 -> "feed_5", vosPartnerId -> "feed_vos_2")
      val expectedStatuses = Map(1 -> true, 2 -> true, 3 -> false, 4 -> true, 5 -> true, vosPartnerId -> true)
      val expectedVisited = Set(2L, 3, 4, 5, vosPartnerId)
      actual.map(_.partnerId).toSet shouldEqual expectedPartnerIds
      actual.map(p => p.partnerId -> p.feedState.getUrl).toMap shouldEqual expectedPartnerFeedUrls
      actual.map(p => p.partnerId -> p.status).toMap shouldEqual expectedStatuses
      actual.filter(_.visitTime.isDefined).map(_.partnerId).toSet shouldEqual expectedVisited
      actual.filter(_.visitTime.isEmpty).map(_.partnerId).toSet shouldEqual (expectedPartnerIds -- expectedVisited)
    }
  }
}
