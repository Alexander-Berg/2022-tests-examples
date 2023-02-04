package vsmoney.auction_auto_strategy.converters.test

import common.models.finance.Money.Kopecks
import vsmoney.auction.auction_bids.{
  AllUserAuctionContextState,
  AllUserStateByContextRequest,
  AuctionSettingsRequest,
  AuctionSettingsResponse,
  UserBid => ProtoUserBid
}
import vsmoney.auction.common_model.AuctionContext.Context.{CriteriaContext => ProtoContext}
import vsmoney.auction.common_model.{AuctionContext, CriteriaValue, Money, Project => ProtoProject}
import vsmoney.auction_auto_strategy.converters.AuctionProtoConverter._
import vsmoney.auction_auto_strategy.converters.ConverterError.IllegalFieldValueError
import vsmoney.auction_auto_strategy.model.auction
import vsmoney.auction_auto_strategy.model.auction._
import vsmoney.auction_auto_strategy.model.common.{ProductId, Project, UserId}
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}

object AuctionProtoConverterSpec extends DefaultRunnableSpec {

  private val user = "user1"
  private val product = "call"
  private val criterion1key = "key"
  private val criterion1value = "value"

  private val testCriteriaContext = CriteriaContext(
    List(Criterion(CriterionKey(criterion1key), CriterionValue(criterion1value)))
  )

  private val testProtoContext = AuctionContext(
    ProtoContext(AuctionContext.CriteriaContext(List(CriteriaValue(criterion1key, criterion1value))))
  )

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("AuctionProtoConverter")(
      suite("auctionKeyToAllUserStateByContextRequest")(
        testM("should convert AuctionKey to AllUserStateByContextRequest") {
          val actual = AuctionKey(
            Project.Autoru,
            ProductId(product),
            testCriteriaContext
          )

          val expected = AllUserStateByContextRequest.of(
            ProtoProject.AUTORU,
            product,
            Some(testProtoContext),
            None
          )

          assertM(auctionKeyToAllUserStateByContextRequest.convert(actual))(equalTo(expected))
        }
      ),
      suite("allUserAuctionContextStateToAuctionState")(
        testM("should convert AllUserAuctionContextState to AuctionState") {
          val basePrice = Kopecks(10000)
          val firstStep = Kopecks(2000)
          val stepCoast = Kopecks(1000)
          val bid = Kopecks(15000)

          val actual = AllUserAuctionContextState(
            ProtoProject.AUTORU,
            product,
            Some(testProtoContext),
            Some(Money(basePrice.value)),
            Some(Money(stepCoast.value)),
            Some(Money((basePrice + firstStep).value)),
            List(ProtoUserBid(user, Some(Money(bid.value))))
          )

          val bids = Seq(UserBid(UserId(user), Bid(bid)))
          val auctionKey = auction.AuctionKey(
            Project.Autoru,
            ProductId(product),
            testCriteriaContext
          )
          val auctionState =
            AuctionState(
              auctionKey = auctionKey,
              bids = bids,
              stepCost = stepCoast,
              minBid = Bid(basePrice + firstStep)
            )

          assertM(allUserAuctionContextStateToAuctionState.convert(actual))(equalTo(auctionState))
        }
      ),
      suite("convertUserBid")(
        testM("should convert StopRequest to UserAuction") {
          val bid = 15000L
          val actual = ProtoUserBid(user, Some(Money(bid)))
          val expected = auction.UserBid(UserId(user), Bid(Kopecks(bid)))

          assertM(convertUserBid.convert(actual))(equalTo(expected))
        },
        testM("should fail if Money is None") {
          val actual = ProtoUserBid(user, None)

          assertM(convertUserBid.convert(actual).run)(
            fails(isSubtype[IllegalFieldValueError](anything))
          )
        }
      ),
      suite("auctionKeyToAuctionSettingsRequest") {
        testM("should convert auctionKey to AuctionSettingsRequest") {
          val auctionKey =
            AuctionKey(project = Project.Autoru, product = ProductId("call"), context = testCriteriaContext)
          val auctionSettingsRequest =
            AuctionSettingsRequest(project = ProtoProject.AUTORU, product = "call", context = Some(testProtoContext))
          assertM(auctionKeyToAuctionSettingsRequest.convert(auctionKey))(equalTo(auctionSettingsRequest))
        }
      },
      suite("auctionSettingsResponseToAuctionSettings") {
        testM("should convert auctionSettingsResponse to AuctionSettings") {
          val request = AuctionSettingsResponse(
            project = ProtoProject.AUTORU,
            product = "call",
            context = Some(testProtoContext),
            basePrice = Some(Money(158)),
            oneStep = Some(Money(34)),
            minBid = Some(Money(98))
          )
          val auctionKey = AuctionKey(
            project = Project.Autoru,
            product = ProductId("call"),
            context = testCriteriaContext
          )
          val auctionSettings = AuctionSettings(step = Kopecks(34), minBid = Kopecks(98))
          assertM(auctionSettingsResponseToAuctionSettings.convert(request))(equalTo((auctionKey -> auctionSettings)))
        }
      }
    )
  }
}
