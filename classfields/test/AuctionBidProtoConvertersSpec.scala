package vsmoney.auction.converters.test

import billing.howmuch.model.{Source => ProtoSource}
import common.models.finance.Money.Kopecks
import vsmoney.auction.auction_bids.{
  AllUserAuctionContextState,
  AllUserStateByContextRequest,
  AuctionSettingsRequest,
  AuctionSettingsResponse,
  BidByDateTimeRequest,
  BidRequest,
  BidRequestBatch,
  BidRequestEntity,
  CompetitiveBid => ProtoCompetitiveBid,
  StopRequest,
  StopRequestBatch,
  StopRequestEntity,
  UserBid => ProtoBid
}
import vsmoney.auction.common_model.AuctionContext.Context
import vsmoney.auction.common_model.AuctionContext.Context.{CriteriaContext => ProtoContext}
import vsmoney.auction.common_model.{AuctionContext, ChangeSource, CriteriaValue, Money, Project => ProtoProject}
import vsmoney.auction.converters.AuctionBidProtoConverters
import vsmoney.auction.converters.ProtoConverterError.IllegalFieldValueError
import vsmoney.auction.model.request.{LeaveAuctionEntityRequest, PlaceBidBEntityRequest}
import vsmoney.auction.model._
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.{assert => zioAssert, assertM, DefaultRunnableSpec, ZSpec}

object AuctionBidProtoConvertersSpec extends DefaultRunnableSpec {
  import AuctionBidProtoConverters._

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("AuctionBidProtoConverters")(
      suite("bidRequestToUserAuction")(
        testM("should convert BidByDateTimeRequest to UserAuction") {
          val testRequest = BidByDateTimeRequest(
            ProtoProject.AUTORU,
            product,
            user,
            Some(testProtoContext),
            datetime = None
          )

          assertM(bidRequestToUserAuction.convert(testRequest))(equalTo(testAuction))
        },
        testM("should fail on unknown project") {
          val testRequest = BidByDateTimeRequest(
            ProtoProject.UNKNOWN_PROJECT,
            product,
            user,
            Some(testProtoContext),
            datetime = None
          )

          assertM(bidRequestToUserAuction.convert(testRequest).run)(fails(isSubtype[IllegalFieldValueError](anything)))
        },
        testM("should fail without context") {
          val testRequest = BidByDateTimeRequest(
            ProtoProject.AUTORU,
            product,
            user,
            context = None,
            datetime = None
          )

          assertM(bidRequestToUserAuction.convert(testRequest).run)(fails(isSubtype[IllegalFieldValueError](anything)))
        },
        testM("should fail on empty context") {
          val testRequest = BidByDateTimeRequest(
            ProtoProject.AUTORU,
            product,
            user,
            Some(
              AuctionContext(Context.Empty)
            ),
            datetime = None
          )

          assertM(bidRequestToUserAuction.convert(testRequest).run)(fails(isSubtype[IllegalFieldValueError](anything)))
        }
      ),
      suite("placeBidRequestToUserAuction")(
        testM("should convert BidRequest to UserAuction") {
          val bid = 1000L
          val prevBid = 500L
          val testRequest = BidRequest(
            ProtoProject.AUTORU,
            product,
            user,
            Some(testProtoContext),
            Some(Money(prevBid)),
            Some(Money(bid))
          )

          assertM(placeBidRequestToUserAuction.convert(testRequest))(equalTo(testAuction))
        }
      ),
      suite("stopBidRequestToUserAuction")(
        testM("should convert StopRequest to UserAuction") {
          val prevBid = 500L
          val testRequest = StopRequest(
            ProtoProject.AUTORU,
            product,
            user,
            Some(testProtoContext),
            Some(Money(prevBid))
          )

          assertM(stopBidRequestToUserAuction.convert(testRequest))(equalTo(testAuction))
        }
      ),
      suite("someMoneyToBidOrFail")(
        testM("should fail on None") {
          assertM(someMoneyToBidOrFail.convert(None).run)(
            fails(isSubtype[IllegalFieldValueError](anything))
          )
        },
        testM("should convert Money to Bid") {
          val bid = 1000L
          assertM(someMoneyToBidOrFail.convert(Some(Money(bid))))(equalTo(Bid(Kopecks(bid))))
        }
      ),
      suite("someMoneyToBid")(
        testM("should convert None to None") {
          assertM(someMoneyToBid.convert(None))(isNone)
        },
        testM("should convert money to bid") {
          val bid = 1000L
          assertM(someMoneyToBid.convert(Some(Money(bid))))(isSome(equalTo(Bid(Kopecks(bid)))))
        }
      ),
      suite("createUserAuction")(
        testM("should fail on unknown project") {
          val testRequest = BidByDateTimeRequest(
            ProtoProject.UNKNOWN_PROJECT,
            product,
            user,
            Some(testProtoContext),
            datetime = None
          )
          val res =
            createUserAuction(
              testRequest.project,
              testRequest.context,
              testRequest.product,
              testRequest.userId,
              auctionObject = None
            )

          assertM(res.run)(fails(isSubtype[IllegalFieldValueError](anything)))
        },
        testM("should fail without context") {
          val testRequest = BidByDateTimeRequest(
            ProtoProject.AUTORU,
            product,
            user,
            context = None,
            datetime = None
          )
          val res =
            createUserAuction(
              testRequest.project,
              testRequest.context,
              testRequest.product,
              testRequest.userId,
              auctionObject = None
            )

          assertM(res.run)(fails(isSubtype[IllegalFieldValueError](anything)))
        },
        testM("should fail on empty context") {
          val testRequest = BidByDateTimeRequest(
            ProtoProject.AUTORU,
            product,
            user,
            Some(
              AuctionContext(Context.Empty)
            ),
            datetime = None
          )
          val res =
            createUserAuction(
              testRequest.project,
              testRequest.context,
              testRequest.product,
              testRequest.userId,
              auctionObject = None
            )

          assertM(res.run)(fails(isSubtype[IllegalFieldValueError](anything)))
        }
      ),
      suite("convertCompetitiveBids") {
        test("should group users with same bid") {
          val usersBids = Seq(
            createCompetitiveBid("id1", 10),
            createCompetitiveBid("id2", 20),
            createCompetitiveBid("id3", 10)
          )

          val expected = Seq(
            ProtoCompetitiveBid(Some(Money(10)), Seq("id1", "id3")),
            ProtoCompetitiveBid(Some(Money(20)), Seq("id2"))
          )
          val res = AuctionBidProtoConverters.convertCompetitiveBids(usersBids)

          zioAssert(res)(hasSameElements(expected))
        }
      },
      suite("sourceFromOptionProto")(
        testM("should convert none to none")(
          assertM(AuctionBidProtoConverters.sourceFromOptionProto.convert(None))(isNone)
        ),
        testM("should convert some proto user source to some user source") {
          val source = Some(ChangeSource(ChangeSource.Source.User(ChangeSource.User(user))))
          val expected = Some(AuctionChangeSource.UserAction(UserId(user)))
          val res = AuctionBidProtoConverters.sourceFromOptionProto.convert(source)
          assertM(res)(equalTo(expected))
        },
        testM("should convert some proto autostrategy source to some autostrategy source") {
          val source = Some(ChangeSource(ChangeSource.Source.User(ChangeSource.User(user))))
          val expected = Some(AuctionChangeSource.UserAction(UserId(user)))
          val res = AuctionBidProtoConverters.sourceFromOptionProto.convert(source)
          assertM(res)(equalTo(expected))
        },
        testM("should fail on empty source") {
          val source = Some(ChangeSource(ChangeSource.Source.Empty))
          val res = AuctionBidProtoConverters.sourceFromOptionProto.convert(source)

          assertM(res.run)(fails(isSubtype[IllegalFieldValueError](anything)))
        }
      ),
      suite("sourceToOptionProto")(
        testM("should convert none to none")(
          assertM(AuctionBidProtoConverters.sourceToOptionProto.convert(None))(isNone)
        ),
        testM("should convert some user source to some proto user source") {
          val expected = Some(ChangeSource(ChangeSource.Source.User(ChangeSource.User(user))))
          val source = Some(AuctionChangeSource.UserAction(UserId(user)))
          val res = AuctionBidProtoConverters.sourceToOptionProto.convert(source)
          assertM(res)(equalTo(expected))
        },
        testM("should convert some autostrategy source to some proto autostrategy source") {
          val expected = Some(ChangeSource(ChangeSource.Source.User(ChangeSource.User(user))))
          val source = Some(AuctionChangeSource.UserAction(UserId(user)))
          val res = AuctionBidProtoConverters.sourceToOptionProto.convert(source)
          assertM(res)(equalTo(expected))
        }
      ),
      suite("AllUserAuctionState")(
        testM("allUserStateRequestToAuctionKey should convert request from proto to AuctionKey") {
          val source = AllUserStateByContextRequest(ProtoProject.AUTORU, product, Some(testProtoContext))
          val res = AuctionBidProtoConverters.allUserStateRequestToAuctionKey.convert(source)
          assertM(res)(equalTo(testAuctionKey))
        },
        testM("allUserAuctionStateToProto should convert AllUsersAuctionState to proto") {
          val basePrice = Kopecks(10000)
          val firstStep = Kopecks(2000)
          val nextStep = Kopecks(1000)
          val bid = Kopecks(15000)

          val promoCampaignId = 1234L
          val priceSource = ProtoSource(ProtoSource.Source.ServiceRequest(s"promo_campaign:${user}_$promoCampaignId"))

          val source = AllUsersAuctionState(
            testAuctionKey,
            BasePrice(basePrice),
            nextStep,
            basePrice + firstStep,
            List(UserBid(UserId(user), Bid(bid))),
            priceSource
          )
          val expected = AllUserAuctionContextState(
            ProtoProject.AUTORU,
            product,
            Some(testProtoContext),
            Some(Money(basePrice.value)),
            Some(Money(nextStep.value)),
            Some(Money((basePrice + firstStep).value)),
            List(ProtoBid(user, Some(Money(bid.value)))),
            source = Some(
              ChangeSource(ChangeSource.Source.PromoCampaign(ChangeSource.PromoCampaign(user, promoCampaignId)))
            )
          )
          val res = AuctionBidProtoConverters.allUserAuctionStateToProto.convert(source)
          assertM(res)(equalTo(expected))
        }
      ),
      suite("auctionObjectFromProto") {
        testM("should convert auction object in proto to model") {
          val req = CriteriaValue("offer_id", "3222-sdds")
          val resp = Criterion(CriterionKey("offer_id"), CriterionValue("3222-sdds"))
          val res = AuctionBidProtoConverters.auctionObjectFromProto.convert(Some(req))
          assertM(res)(equalTo(Some(resp)))
        }
        testM("should didn't convert auction object if auction object is None") {
          val res = AuctionBidProtoConverters.auctionObjectFromProto.convert(None)
          assertM(res)(equalTo(None))
        }
      },
      suite("auctionObjectToProto") {
        testM("should convert auction object to proto") {
          val req = Criterion(CriterionKey("offer_id"), CriterionValue("3222-sdds"))
          val response = CriteriaValue("offer_id", "3222-sdds")
          val res = AuctionBidProtoConverters.auctionObjectToProto.convert(Some(req))
          assertM(res)(equalTo(Some(response)))
        }
        testM("should didn't convert None") {
          val res = AuctionBidProtoConverters.auctionObjectToProto.convert(None)
          assertM(res)(equalTo(None))
        }
      },
      suite("placeBidBatchRequestToPlaceBidBEntityRequest") {
        testM("should convert BidBatchRequest to PlaceBidBEntityRequest") {
          val testPrevBid = Bid(Kopecks(32123))
          val testBid = Bid(Kopecks(2972))

          val batchRequestEntity = BidRequestEntity(
            context = Some(testProtoContext),
            previousBid = Some(Money(testPrevBid.amount.value)),
            bid = Some(Money(testBid.amount.value)),
            `object` = None
          )
          val request = BidRequestBatch(
            project = ProtoProject.AUTORU,
            product = testProductId.id,
            userId = testUser.id,
            entities = Seq(
              batchRequestEntity
            ),
            source = None
          )

          val response = PlaceBidBEntityRequest(
            userAuction = UserAuction(
              key = testAuctionKey,
              user = testUser
            ),
            bid = testBid,
            prevBid = Some(testPrevBid)
          )
          val res = AuctionBidProtoConverters.placeBidBatchRequestToPlaceBidBEntityRequest.convert(request)
          assertM(res)(equalTo(List(response)))
        }

      },
      suite("stopRequestToLeaveAuctionEntityRequest") {
        testM("should convert StopBatchRequest to LeaveAuctionEntityRequest") {
          val prevBid = Bid(Kopecks(148))
          val requestEntity = StopRequestEntity(
            project = ProtoProject.AUTORU,
            context = Some(testProtoContext),
            previousBid = Some(Money(prevBid.amount.value)),
            `object` = None
          )
          val request = StopRequestBatch(
            project = ProtoProject.AUTORU,
            product = testProductId.id,
            userId = testUser.id,
            entities = Seq(requestEntity),
            source = None
          )

          val result = LeaveAuctionEntityRequest(
            userAuction = testAuction,
            prevBid = prevBid
          )
          val res = AuctionBidProtoConverters.stopRequestToLeaveAuctionEntityRequest.convert(request)
          assertM(res)(equalTo(List(result)))
        }
      },
      suite("auctionSettingsRequestToAuctionKey") {
        testM("should convert auctionSettingsRequest to AuctionKey") {
          val auctionSettingsRequest =
            AuctionSettingsRequest(project = ProtoProject.AUTORU, product = product, context = Some(testProtoContext))
          val auctionKey = AuctionKey(
            project = Project.Autoru,
            product = testProductId,
            context = testContext,
            auctionObject = None
          )
          val res = AuctionBidProtoConverters.auctionSettingsRequestToAuctionKey.convert(auctionSettingsRequest)
          assertM(res)(equalTo(auctionKey))
        }
      },
      suite("auctionSettingsToAuctionSettingsResponse") {
        testM("should convert auctionSettings to AuctionSettingsResponse") {
          val basePrice = BasePrice(Kopecks(190))
          val auctionStep = Kopecks(45)
          val minBid = Kopecks(235)
          val auctionSettings = AuctionSettings(
            auctionKey = testAuctionKey,
            basePrice = basePrice,
            auctionStep = auctionStep,
            minBid = minBid
          )
          val auctionSettingsResponse = AuctionSettingsResponse(
            project = ProtoProject.AUTORU,
            product = testAuctionKey.product.id,
            context = Some(testProtoContext),
            basePrice = Some(Money(basePrice.amount.value)),
            oneStep = Some(Money(auctionStep.value)),
            minBid = Some(Money(minBid.value))
          )

          val res = AuctionBidProtoConverters.auctionSettingsToAuctionSettingsResponse.convert(auctionSettings)
          assertM(res)(equalTo(auctionSettingsResponse))
        }
      }
    )
  }

  private val user = "user1"
  private val product = "call"
  private val criterion1key = "key"
  private val criterion1value = "value"
  val testUser = UserId(user)
  val testProductId = ProductId(product)

  private val testProtoContext = AuctionContext(
    ProtoContext(AuctionContext.CriteriaContext(List(CriteriaValue(criterion1key, criterion1value))))
  )

  private val testContext = CriteriaContext(
    List(Criterion(CriterionKey(criterion1key), CriterionValue(criterion1value)))
  )

  private val testAuctionKey = AuctionKey(
    Project.Autoru,
    testProductId,
    testContext,
    auctionObject = None
  )

  private val testAuction = UserAuction(testAuctionKey, testUser)

  private def createCompetitiveBid(user: String, bidInKopecks: Long): CompetitiveBid = {
    CompetitiveBid(UserId(user), Bid(Kopecks(bidInKopecks)))
  }
}
