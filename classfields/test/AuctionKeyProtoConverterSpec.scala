package vsmoney.auction_auto_strategy.api.test

import vsmoney.auction.common_model.{
  AuctionContext => ProtoAuctionContext,
  CriteriaValue => ProtoCriteriaValue,
  Money => MoneyProto,
  Project => ProtoProject
}
import vsmoney.auction_auto_strategy.api.convertes.AuctionKeyProtoConverter
import vsmoney.auction_auto_strategy.model.auction.{
  AuctionKey,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue
}
import vsmoney.auction_auto_strategy.model.common.{ProductId, Project}
import vsmoney.auction_auto_strategy.settings.{
  AutoStrategy => AutoStrategyProto,
  CreateRequest,
  DeleteRequest,
  MaxPositionForPrice => MaxPositionForPriceProto,
  SettingsForContextRequest
}
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object AuctionKeyProtoConverterSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("AuctionKeyProtoConverter")(
      testM("should convert CreateRequest to AuctionKey") {
        val createRequest = CreateRequest(
          project = ProtoProject.AUTORU,
          product = "call",
          userId = "user:1",
          context = Some(testProtoAuctionContext),
          autoStrategy = Some(testAutoStrategy)
        )
        assertM(AuctionKeyProtoConverter.fromCreateRequest.convert(createRequest).run)(
          succeeds(equalTo(testAuctionKey))
        )
      },
      testM("should convert DeleteRequest to AuctionKey") {
        val deleteRequest = DeleteRequest(
          project = ProtoProject.AUTORU,
          product = "call",
          userId = "user:1",
          context = Some(testProtoAuctionContext)
        )
        assertM(AuctionKeyProtoConverter.fromDeleteRequest.convert(deleteRequest).run)(
          succeeds(equalTo(testAuctionKey))
        )
      },
      testM("should convert SettingsForContextRequest to AuctionKey") {
        val settingsForContextRequest = SettingsForContextRequest(
          context = Some(testProtoAuctionContext),
          project = ProtoProject.AUTORU,
          product = "call"
        )
        assertM(AuctionKeyProtoConverter.fromSettingForContextRequest.convert(settingsForContextRequest).run)(
          succeeds(equalTo(testAuctionKey))
        )
      }
    )
  }

  private val testCriteriaContext = CriteriaContext(criteria =
    Seq(
      Criterion(key = CriterionKey("region_id"), value = CriterionValue("42")),
      Criterion(key = CriterionKey("mark"), value = CriterionValue("bmw")),
      Criterion(key = CriterionKey("model"), value = CriterionValue("x5"))
    )
  )

  private val testAuctionKey = AuctionKey(
    project = Project.Autoru,
    product = ProductId("call"),
    context = testCriteriaContext
  )

  private val testProtoAuctionContext = ProtoAuctionContext(context =
    ProtoAuctionContext.Context.CriteriaContext(
      ProtoAuctionContext.CriteriaContext(criteriaValues =
        Seq(
          ProtoCriteriaValue(key = "region_id", value = "42"),
          ProtoCriteriaValue(key = "mark", value = "bmw"),
          ProtoCriteriaValue(key = "model", value = "x5")
        )
      )
    )
  )

  private val testAutoStrategy = AutoStrategyProto(
    maxBid = Some(MoneyProto(1L)),
    algorithm = AutoStrategyProto.Algorithm.MaxPositionForPrice(value = MaxPositionForPriceProto())
  )

}
