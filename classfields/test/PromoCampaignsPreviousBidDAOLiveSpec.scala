package vsmoney.auction_auto_strategy.storage.test

import cats.data.NonEmptyList
import common.models.finance.Money.Kopecks
import common.zio.doobie.testkit.TestMySQL
import vsmoney.auction_auto_strategy.model.auction._
import vsmoney.auction_auto_strategy.model.common._
import vsmoney.auction_auto_strategy.model.promo_campaign._
import vsmoney.auction_auto_strategy.storage.PromoCampaignsPreviousBidDAO
import vsmoney.auction_auto_strategy.storage.formats._
import vsmoney.auction_auto_strategy.storage.impl.PromoCampaignsPreviousBidDAOLive
import vsmoney.auction_auto_strategy.storage.table.PromoCampaignsPreviousBidTable
import zio._
import zio.magic._
import zio.test.TestAspect._
import zio.test._
import zio.test.environment.TestEnvironment

object PromoCampaignsPreviousBidDAOLiveSpec extends DefaultRunnableSpec with CommonFormats with JsonFormats {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("PromoCampaignsPreviousBidDAOLive") {
      testM("should insert and read promo companies") {
        for {
          service <- ZIO.service[PromoCampaignsPreviousBidDAO]
          _ <- service.add(List(testPromoCampaignAuctionBid))
          promoCampaignsByAuctionKey <- service.getForAuctionKeys(NonEmptyList.one(testAuctionKey))
          promoCampaignsByAuctionKeyBatch <- service.getForAuctionKeys(NonEmptyList.of(testAuctionKey))
          promoCampaignsById <- service.getForPromoCampaigns(NonEmptyList.one(testPromoCampaignId))
          promoCampaignsByIdBatch <- service.getForPromoCampaigns(NonEmptyList.of(testPromoCampaignId))
          _ <- service.delete(testPromoCampaignId)
          promoCampaignsAfterRemove <- service.getForPromoCampaigns(NonEmptyList.one(testPromoCampaignId))
        } yield {
          assertTrue(
            promoCampaignsByAuctionKeyBatch == promoCampaignsByAuctionKey,
            promoCampaignsByIdBatch == promoCampaignsById,
            promoCampaignsById.head == promoCampaignsByAuctionKey.head,
            promoCampaignsById.size == 1,
            promoCampaignsAfterRemove.isEmpty
          )
        }
      }
    } @@ beforeAll(init) @@ after(truncate(PromoCampaignsPreviousBidTable.table)) @@ sequential
  }.injectCustomShared(
    TestMySQL.managedTransactor,
    PromoCampaignsPreviousBidDAOLive.live
  )

  val testAuctionKey = AuctionKey(
    project = Project.Autoru,
    product = ProductId("call:cars:used"),
    context = CriteriaContext(
      criteria = Seq(
        Criterion(key = CriterionKey("region_id"), value = CriterionValue("1"))
      )
    )
  )

  val testAuctionObject = Criterion(
    key = CriterionKey("offer_id"),
    value = CriterionValue("2223224")
  )

  val testPromoCampaignId = PromoCampaignId(userId = UserId("user1"), userCampaignId = 123)

  val testPromoCampaignAuctionBid =
    PromoCampaignAuctionBid(
      key = testAuctionKey,
      auctionObject = testAuctionObject,
      bid = Kopecks(123),
      promoCampaignId = testPromoCampaignId
    )
}
