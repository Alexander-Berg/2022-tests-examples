package vsmoney.auction.services.test

import billing.howmuch.model.{Source => ProtoSource}
import common.models.finance.Money.Kopecks
import common.zio.logging.Logging
import common.zio.ops.tracing.{context, RequestId}
import common.zio.ops.tracing.RequestId.RequestId
import infra.feature_toggles.client.FeatureTogglesClient.FeatureTogglesClient
import infra.feature_toggles.client.testkit.TestFeatureToggles
import infra.feature_toggles.client.testkit.TestFeatureToggles.TestFeatureToggles
import vsmoney.auction.clients.PriceRequestCreator._
import vsmoney.auction.clients.testkit.HowMuchMock
import vsmoney.auction.model.FirstStep.BasePricePlusAmount
import vsmoney.auction.model.NextStep.ArithmeticProgression
import vsmoney.auction.model._
import vsmoney.auction.model.howmuch.{ChangePriceRequest, PriceRequest, PriceResponse, PriceResponseEntry}
import vsmoney.auction.services.UserAuctionService
import vsmoney.auction.services.UserAuctionService.AuctionError.{
  BidLessThanFirstStep,
  BidNotMultiplyOfStep,
  NoAuctionParams,
  NoBasePrice
}
import vsmoney.auction.services.impl.UserAuctionServiceLive
import vsmoney.auction.services.testkit.{
  AuctionBlockDaoMock,
  AuctionParamsServiceMock,
  JournalServiceMock,
  UsersBidsServiceMock
}
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.{assertM, DefaultRunnableSpec, ZSpec}
import zio.{ZIO, ZLayer}

import java.time.Instant

object UserAuctionServiceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("UserAuctionService")(
      suite("bidByDateTime")(
        testM("should return None if no bid exists") {
          val res = for {
            auction <- ZIO.service[UserAuctionService]
            resp <- auction.bidByDateTime(testAuction, Instant.now())
          } yield resp

          assertM(res)(isNone).provideCustomLayer(bidByDateTimeDependencies(price = None))
        },
        testM("should return bid from howMuch with auction block") {
          val date = Instant.now()
          val expectedResult = UserAuctionBid(testAuction, Bid(testBid), date, auctionBlocked = true)

          val res = for {
            auction <- ZIO.service[UserAuctionService]
            resp <- auction.bidByDateTime(testAuction, date)
          } yield resp

          assertM(res)(isSome(equalTo(expectedResult)))
            .provideCustomLayer(bidByDateTimeDependencies(Some(testBid)))
        }
      ),
      suite("placeBid")(
        testM("should place bid with previous bid") {
          val bid = Bid(Kopecks(2000))
          val prevBid = Some(Bid(Kopecks(1000)))
          val basePrice = Kopecks(1000)

          val paramsMock = AuctionParamsServiceMock.Get(anything, value(testParams))
          val howMuchMock = HowMuchMock.GetPrices(anything, value(testPriceResponse(Some(basePrice)))) ++
            HowMuchMock.ChangePrices(
              hasField("newPrice", (r: ChangePriceRequest) => r.newPrice, isSome(equalTo(bid.amount))) &&
                hasField(
                  "previousPrice",
                  (r: ChangePriceRequest) => r.previousPrice,
                  isSome(equalTo(prevBid.get.amount))
                ),
              unit
            )
          val usersBidsMock = UsersBidsServiceMock.empty
          val auctionBlockDaoMock = AuctionBlockDaoMock.empty
          val journalMock = JournalServiceMock
            .BidPlaced(anything, unit)
          val res = placeBid(prevBid, bid)

          assertM(res)(isUnit).provideCustomLayer(
            auctionBlockDaoMock ++ usersBidsMock ++ paramsMock ++ howMuchMock ++ journalMock ++ commonMocks >>> UserAuctionServiceLive.live
          )
        },
        testM("should place bid lesser then previous bid") {
          val bid = Bid(Kopecks(2000))
          val prevBid = Some(Bid(Kopecks(5000)))
          val basePrice = Kopecks(1000)

          val paramsMock = AuctionParamsServiceMock.Get(anything, value(testParams))
          val howMuchMock = HowMuchMock.GetPrices(anything, value(testPriceResponse(Some(basePrice)))) ++
            HowMuchMock.ChangePrices(
              hasField("newPrice", (r: ChangePriceRequest) => r.newPrice, isSome(equalTo(bid.amount))) &&
                hasField(
                  "previousPrice",
                  (r: ChangePriceRequest) => r.previousPrice,
                  isSome(equalTo(prevBid.get.amount))
                ),
              unit
            )
          val usersBidsMock = UsersBidsServiceMock.empty
          val auctionBlockDaoMock = AuctionBlockDaoMock.empty
          val journalMock = JournalServiceMock
            .BidPlaced(anything, unit)
          val res = placeBid(prevBid, bid)

          assertM(res)(isUnit).provideCustomLayer(
            auctionBlockDaoMock ++ usersBidsMock ++ paramsMock ++ howMuchMock ++ journalMock ++ commonMocks >>> UserAuctionServiceLive.live
          )
        },
        testM("should place bid without previous bid") {
          val bid = Bid(Kopecks(2000))
          val prevBid = None
          val basePrice = Kopecks(1000)

          val paramsMock = AuctionParamsServiceMock.Get(anything, value(testParams))
          val howMuchMock = HowMuchMock.GetPrices(anything, value(testPriceResponse(Some(basePrice)))) ++
            HowMuchMock.ChangePrices(
              hasField("newPrice", (r: ChangePriceRequest) => r.newPrice, isSome(equalTo(bid.amount))) &&
                hasField(
                  "previousPrice",
                  (r: ChangePriceRequest) => r.previousPrice,
                  isNone
                ),
              unit
            )
          val usersBidsMock = UsersBidsServiceMock.empty
          val auctionBlockDaoMock = AuctionBlockDaoMock.empty
          val journalMock = JournalServiceMock
            .BidPlaced(anything, unit)

          val res = placeBid(prevBid, bid)

          assertM(res)(isUnit).provideCustomLayer(
            auctionBlockDaoMock ++ usersBidsMock ++ paramsMock ++ howMuchMock ++ journalMock ++ commonMocks >>> UserAuctionServiceLive.live
          )
        },
        testM("should place bid and ignore super_gen_id criterion if feature enabled") {
          val bid = Bid(Kopecks(2000))
          val prevBid = Some(Bid(Kopecks(5000)))
          val basePrice = Kopecks(1000)

          val auctionKeyForPalma = testAuction3.key.copy(context =
            testAuction3.key.context
              .copy(criteria = testAuction3.key.context.criteria.filterNot(_.key.value == "super_gen_id"))
          )

          val auctionKeyForHowmuch = testAuction3.key.copy(context =
            testAuction3.key.context.copy(criteria = testAuction3.key.context.criteria.map {
              case c if c.key.value == "super_gen_id" => c.copy(value = CriterionValue("*"))
              case other => other
            })
          )

          val paramsMock =
            AuctionParamsServiceMock.Get(equalTo(auctionKeyForPalma), value(testParams.copy(key = auctionKeyForPalma)))
          val howMuchMock = HowMuchMock.GetPrices(
            hasField("entries", (a: PriceRequest) => a.entries, hasSize(equalTo(1))) &&
              hasField("entries", (a: PriceRequest) => a.entries.head.context, equalTo(auctionKeyForHowmuch.context)),
            value(testPriceResponse(Some(basePrice)))
          ) ++
            HowMuchMock.ChangePrices(
              hasField("newPrice", (r: ChangePriceRequest) => r.newPrice, isSome(equalTo(bid.amount))) &&
                hasField(
                  "previousPrice",
                  (r: ChangePriceRequest) => r.previousPrice,
                  isSome(equalTo(prevBid.get.amount))
                ),
              unit
            )

          val usersBidsMock = UsersBidsServiceMock.empty
          val auctionBlockDaoMock = AuctionBlockDaoMock.empty
          val journalMock = JournalServiceMock
            .BidPlaced(anything, unit)

          val res = for {
            featuresClient <- ZIO.service[TestFeatureToggles.Service]
            _ = featuresClient.set("auction_cars_used_super_gen_id_ignore", true)
            auction <- ZIO.service[UserAuctionService]
            _ <- auction.placeBid(testAuction3, prevBid, bid, Some(testSource))
          } yield ()

          assertM(res)(isUnit).provideCustomLayer(
            auctionBlockDaoMock ++ usersBidsMock ++ paramsMock ++ howMuchMock ++ journalMock ++ commonMocks >+> UserAuctionServiceLive.live
          )
        },
        testM("should validate bid and fails if no base price") {
          val bid = Bid(Kopecks(2000))
          val prevBid = Some(Bid(Kopecks(1000)))

          val res = placeBid(prevBid, bid)

          assertM(res.run)(fails(isSubtype[NoBasePrice](anything)))
            .provideCustomLayer(placeBidFailedDependencies(price = None))
        },
        testM("should validate bid and fails bid less then base price + step") {
          val bid = Bid(Kopecks(2000))
          val prevBid = Some(Bid(Kopecks(1000)))
          val basePrice = Kopecks(2000)

          val res = placeBid(prevBid, bid)

          assertM(res.run)(fails(isSubtype[BidLessThanFirstStep](anything)))
            .provideCustomLayer(placeBidFailedDependencies(Some(basePrice)))
        },
        testM("should validate bid and fails if bid not multiply of step") {
          val bid = Bid(Kopecks(2010))
          val prevBid = Some(Bid(Kopecks(1000)))
          val basePrice = Kopecks(1000)

          val res = placeBid(prevBid, bid)

          assertM(res.run)(fails(isSubtype[BidNotMultiplyOfStep](anything)))
            .provideCustomLayer(placeBidFailedDependencies(Some(basePrice)))
        }
      ),
      suite("stopUserAuction")(
        testM("should ask howmuch to stop userauction") {
          val prevBid = Bid(Kopecks(1000))

          val paramsMock = AuctionParamsServiceMock.empty
          val howMuchMock = HowMuchMock.ChangePrices(
            hasField("newPrice", (r: ChangePriceRequest) => r.newPrice, isNone) &&
              hasField(
                "previousPrice",
                (r: ChangePriceRequest) => r.previousPrice,
                isSome(equalTo(prevBid.amount))
              ),
            unit
          )
          val usersBidsMock = UsersBidsServiceMock.empty
          val auctionBlockDaoMock = AuctionBlockDaoMock.empty
          val journalMock = JournalServiceMock
            .AuctionStopped(anything, unit)
          val res = for {
            auction <- ZIO.service[UserAuctionService]
            _ <- auction.stopUserAuction(testAuction, prevBid, Some(testSource))
          } yield ()

          assertM(res)(isUnit).provideCustomLayer(
            auctionBlockDaoMock ++ usersBidsMock ++ paramsMock ++ howMuchMock ++ journalMock ++ commonMocks >>> UserAuctionServiceLive.live
          )
        }
      ),
      suite("userAuctionsStates")(
        testM("should return empty list on empty auctions") {
          val res = for {
            auction <- ZIO.service[UserAuctionService]
            state <- auction.userAuctionsStates(List.empty)
          } yield state

          val paramsMock = AuctionParamsServiceMock.empty
          val usersBidsMock = UsersBidsServiceMock.empty
          val howMuchMock = HowMuchMock.empty
          val auctionBlockDaoMock = AuctionBlockDaoMock.empty
          val journalMock = JournalServiceMock.empty
          assertM(res)(equalTo(List.empty)).provideCustomLayer(
            auctionBlockDaoMock ++ usersBidsMock ++ paramsMock ++ howMuchMock ++ journalMock ++ commonMocks >>> UserAuctionServiceLive.live
          )
        },
        testM("should return right state without user bid in competitors") {
          def hasPriceRequestFieldsWithPrefix(idPrefix: String, expectedSize: Int) = hasField(
            "entries",
            (a: PriceRequest) => a.entries.filter(_.entryId.startsWith(idPrefix)),
            hasSize(equalTo(expectedSize))
          )
          val testUsersBids = List(
            UserBid(testAuction.user, Bid(testBid)),
            UserBid(testAuction2.user, Bid(testBid2))
          )

          val paramsMock = AuctionParamsServiceMock.Get(equalTo(testAuction.key), value(testParams)) &&
            AuctionParamsServiceMock.Get(equalTo(testAuction2.key), value(testParams2))
          val howMuchMock = HowMuchMock.GetPrices(
            hasField("entries", (a: PriceRequest) => a.entries, hasSize(equalTo(4))) &&
              hasPriceRequestFieldsWithPrefix("bid:", 2) &&
              hasPriceRequestFieldsWithPrefix("price:", 2),
            value(testPriceResponse4Entries)
          )
          val usersBidsMock = UsersBidsServiceMock.Get(equalTo(testAuction.key), value(testUsersBids)) &&
            UsersBidsServiceMock.Get(equalTo(testAuction2.key), value(testUsersBids))
          val auctionBlockDaoMock = AuctionBlockDaoMock.UserHadBlockAtMoment(anything, value(true)).twice
          val journalMock = JournalServiceMock.empty
          val res = for {
            auction <- ZIO.service[UserAuctionService]
            states <- auction.userAuctionsStates(List(testAuction, testAuction2))
          } yield states

          val expectedAuctionState = List(
            UserAuctionState(
              testAuction,
              BasePrice(testBasePrice),
              Some(Bid(testBid)),
              testNextStep,
              testBasePrice + testFirstStep,
              List(CompetitiveBid(user2, Bid(testBid2))),
              isBlockActive = true,
              bidSource = Some(testBidPC)
            ),
            UserAuctionState(
              testAuction2,
              BasePrice(testBasePrice2),
              Some(Bid(testBid2)),
              testNextStep2,
              testBasePrice2 + testFirstStep2,
              List(CompetitiveBid(user, Bid(testBid))),
              isBlockActive = true,
              bidSource = Some(testBid2PC)
            )
          )

          assertM(res)(equalTo(expectedAuctionState)).provideCustomLayer(
            commonMocks ++ auctionBlockDaoMock ++ journalMock ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
          )
        },
        testM("should validate and fail if no base price") {
          val paramsMock = AuctionParamsServiceMock.Get(anything, value(testParams)).atMost(2)
          val howMuchMock = HowMuchMock.GetPrices(
            anything,
            value(PriceResponse(entries = List.empty))
          )
          val usersBidsMock = UsersBidsServiceMock.Get(anything, value(List.empty)).atMost(2)
          val auctionBlockDaoMock = AuctionBlockDaoMock.UserHadBlockAtMoment(anything, value(true)).twice
          val journalMock = JournalServiceMock.empty
          val res = for {
            auction <- ZIO.service[UserAuctionService]
            states <- auction.userAuctionsStates(List(testAuction, testAuction2))
          } yield states

          assertM(res.run)(fails(isSubtype[NoBasePrice](anything))).provideCustomLayer(
            commonMocks ++ auctionBlockDaoMock ++ journalMock ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
          )
        },
        testM("should validate and fail if no params") {
          val paramsMock = AuctionParamsServiceMock.Get(anything, value(testParams)).atMost(2)
          val howMuchMock = HowMuchMock.GetPrices(anything, value(testPriceResponse4Entries))
          val usersBidsMock = UsersBidsServiceMock.Get(anything, value(List.empty)).atMost(2)
          val auctionBlockDaoMock = AuctionBlockDaoMock.UserHadBlockAtMoment(anything, value(true)).atMost(2)
          val journalMock = JournalServiceMock.empty
          val res = for {
            auction <- ZIO.service[UserAuctionService]
            states <- auction.userAuctionsStates(List(testAuction, testAuction2))
          } yield states

          assertM(res.run)(fails(isSubtype[NoAuctionParams](anything))).provideCustomLayer(
            commonMocks ++ auctionBlockDaoMock ++ journalMock ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
          )
        },
        testM("should return state with no current bid") {
          val testUsersBids = List(UserBid(UserId("1"), Bid(Kopecks(500))))

          val paramsMock = AuctionParamsServiceMock.Get(equalTo(testAuction.key), value(testParams)) &&
            AuctionParamsServiceMock.Get(equalTo(testAuction2.key), value(testParams2))
          val testEntries = testPriceResponse4Entries.entries.filterNot(_.entryId == "bid:1")
          val howMuchMock = HowMuchMock.GetPrices(
            hasField("entries", (a: PriceRequest) => a.entries, hasSize(equalTo(4))),
            value(testPriceResponse4Entries.copy(entries = testEntries))
          )
          val usersBidsMock = UsersBidsServiceMock.Get(anything, value(testUsersBids)).atMost(2)
          val auctionBlockDaoMock = AuctionBlockDaoMock.UserHadBlockAtMoment(anything, value(true)).twice
          val journalMock = JournalServiceMock.empty
          val res = for {
            auction <- ZIO.service[UserAuctionService]
            states <- auction.userAuctionsStates(List(testAuction, testAuction2))
          } yield states

          val expectedAuctionState = List(
            UserAuctionState(
              testAuction,
              BasePrice(testBasePrice),
              Some(Bid(testBid)),
              testNextStep,
              testBasePrice + testFirstStep,
              List(CompetitiveBid(UserId("1"), Bid(Kopecks(500)))),
              isBlockActive = true,
              bidSource = Some(testBidPC)
            ),
            UserAuctionState(
              testAuction2,
              BasePrice(testBasePrice2),
              currentBid = None,
              testNextStep2,
              testBasePrice2 + testFirstStep2,
              List(CompetitiveBid(UserId("1"), Bid(Kopecks(500)))),
              isBlockActive = true,
              bidSource = None
            )
          )

          assertM(res)(equalTo(expectedAuctionState)).provideCustomLayer(
            commonMocks ++ auctionBlockDaoMock ++ journalMock ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
          )
        },
        testM("should return state with no competitive bids") {
          val paramsMock = AuctionParamsServiceMock.Get(equalTo(testAuction.key), value(testParams)) &&
            AuctionParamsServiceMock.Get(equalTo(testAuction2.key), value(testParams2))
          val howMuchMock = HowMuchMock.GetPrices(anything, value(testPriceResponse4Entries))
          val usersBidsMock = UsersBidsServiceMock.Get(anything, value(List.empty)).atMost(2)
          val auctionBlockDaoMock = AuctionBlockDaoMock.UserHadBlockAtMoment(anything, value(true)).twice
          val journalMock = JournalServiceMock.empty
          val res = for {
            auction <- ZIO.service[UserAuctionService]
            states <- auction.userAuctionsStates(List(testAuction, testAuction2))
          } yield states

          val expectedAuctionState = List(
            UserAuctionState(
              testAuction,
              BasePrice(testBasePrice),
              Some(Bid(testBid)),
              testNextStep,
              testBasePrice + testFirstStep,
              List.empty,
              isBlockActive = true,
              bidSource = Some(testBidPC)
            ),
            UserAuctionState(
              testAuction2,
              BasePrice(testBasePrice2),
              Some(Bid(testBid2)),
              testNextStep2,
              testBasePrice2 + testFirstStep2,
              List.empty,
              isBlockActive = true,
              bidSource = Some(testBid2PC)
            )
          )
          assertM(res)(equalTo(expectedAuctionState)).provideCustomLayer(
            commonMocks ++ auctionBlockDaoMock ++ journalMock ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
          )
        },
        suite("should fail if either one of dependency service fails")(
          testM("if howmuch service fails") {
            val testUsersBids = List(UserBid(UserId("1"), Bid(Kopecks(500))))
            val paramsMock = AuctionParamsServiceMock.Get(anything, value(testParams)).atMost(2)
            val howMuchMock = HowMuchMock
              .GetPrices(anything, failure(new Throwable))
            val usersBidsMock = UsersBidsServiceMock.Get(anything, value(testUsersBids)).atMost(2)
            val auctionBlockDaoMock = AuctionBlockDaoMock.UserHadBlockAtMoment(anything, value(true))
            val journalMock = JournalServiceMock.empty
            val res = for {
              auction <- ZIO.service[UserAuctionService]
              states <- auction.userAuctionsStates(List(testAuction, testAuction2))
            } yield states

            assertM(res.run)(fails(anything)).provideCustomLayer(
              commonMocks ++ auctionBlockDaoMock ++ journalMock ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
            )
          },
          testM("if usersbids service fails") {
            val paramsMock = AuctionParamsServiceMock.Get(equalTo(testAuction.key), value(testParams)) &&
              AuctionParamsServiceMock.Get(equalTo(testAuction2.key), value(testParams2))
            val howMuchMock = HowMuchMock.GetPrices(anything, value(testPriceResponse4Entries)).optional
            val usersBidsMock = UsersBidsServiceMock.Get(anything, failure(new Throwable)).atMost(2)
            val auctionBlockDaoMock = AuctionBlockDaoMock.UserHadBlockAtMoment(anything, value(true)).twice
            val journalMock = JournalServiceMock.empty
            val res = for {
              auction <- ZIO.service[UserAuctionService]
              states <- auction.userAuctionsStates(List(testAuction, testAuction2))
            } yield states

            assertM(res.run)(fails(anything)).provideCustomLayer(
              commonMocks ++ auctionBlockDaoMock ++ journalMock ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
            )
          }
        ),
        suite("allUsersAuctionsStates")(
          testM("should return right state") {
            val testUsersBids = List(
              UserBid(UserId("1"), Bid(Kopecks(500))),
              UserBid(testAuction.user, Bid(testBid))
            )

            val paramsMock = AuctionParamsServiceMock.Get(equalTo(testAuction.key), value(testParams)) &&
              AuctionParamsServiceMock.Get(equalTo(testAuction2.key), value(testParams2))
            val howMuchMock = HowMuchMock.GetPrices(
              hasField("entries", (a: PriceRequest) => a.entries, hasSize(equalTo(2))),
              value(testPriceResponseOnlyPrices)
            )
            val usersBidsMock = UsersBidsServiceMock.Get(equalTo(testAuction.key), value(testUsersBids)) &&
              UsersBidsServiceMock.Get(equalTo(testAuction2.key), value(testUsersBids))
            val auctionBlockDaoMock = AuctionBlockDaoMock.empty
            val journalMock = JournalServiceMock.empty
            val res = for {
              auction <- ZIO.service[UserAuctionService]
              states <- auction.allUsersAuctionsStates(List(testAuction.key, testAuction2.key))
            } yield states

            val expectedAuctionState = List(
              AllUsersAuctionState(
                testAuction.key,
                BasePrice(testBasePrice),
                testNextStep,
                testBasePrice + testFirstStep,
                testUsersBids,
                source = testBasePricePC
              ),
              AllUsersAuctionState(
                testAuction2.key,
                BasePrice(testBasePrice2),
                testNextStep2,
                testBasePrice2 + testFirstStep2,
                testUsersBids,
                source = testBasePrice2PC
              )
            )

            assertM(res)(equalTo(expectedAuctionState)).provideCustomLayer(
              auctionBlockDaoMock ++ journalMock ++ commonMocks ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
            )
          },
          testM("should validate and fail if no base price") {
            val paramsMock = AuctionParamsServiceMock.Get(anything, value(testParams)).atMost(2)
            val howMuchMock = HowMuchMock.GetPrices(
              anything,
              value(PriceResponse(entries = List.empty))
            )
            val usersBidsMock = UsersBidsServiceMock.Get(anything, value(List.empty)).atMost(2)
            val auctionBlockDaoMock = AuctionBlockDaoMock.empty
            val journalMock = JournalServiceMock.empty
            val res = for {
              auction <- ZIO.service[UserAuctionService]
              states <- auction.allUsersAuctionsStates(List(testAuction.key, testAuction2.key))
            } yield states

            assertM(res.run)(fails(isSubtype[NoBasePrice](anything))).provideCustomLayer(
              auctionBlockDaoMock ++ journalMock ++ commonMocks ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
            )
          },
          testM("should validate and fail if no params") {
            val paramsMock = AuctionParamsServiceMock.Get(anything, value(testParams)).atMost(2)
            val howMuchMock = HowMuchMock.GetPrices(anything, value(testPriceResponse4Entries))
            val usersBidsMock = UsersBidsServiceMock.Get(anything, value(List.empty)).atMost(2)
            val auctionBlockDaoMock = AuctionBlockDaoMock.empty
            val journalMock = JournalServiceMock.empty
            val res = for {
              auction <- ZIO.service[UserAuctionService]
              states <- auction.allUsersAuctionsStates(List(testAuction.key, testAuction2.key))
            } yield states

            assertM(res.run)(fails(isSubtype[NoAuctionParams](anything))).provideCustomLayer(
              auctionBlockDaoMock ++ journalMock ++ commonMocks ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
            )
          },
          suite("should fail if either one of dependency service fails")(
            testM("if howmuch service fails") {
              val testUsersBids = List(UserBid(UserId("1"), Bid(Kopecks(500))))
              val paramsMock = AuctionParamsServiceMock.Get(anything, value(testParams)).atMost(2)
              val howMuchMock = HowMuchMock
                .GetPrices(anything, failure(new Throwable))
              val usersBidsMock = UsersBidsServiceMock.Get(anything, value(testUsersBids)).atMost(2)
              val auctionBlockDaoMock = AuctionBlockDaoMock.empty
              val journalMock = JournalServiceMock.empty
              val res = for {
                auction <- ZIO.service[UserAuctionService]
                states <- auction.allUsersAuctionsStates(List(testAuction.key, testAuction2.key))
              } yield states

              assertM(res.run)(fails(anything)).provideCustomLayer(
                auctionBlockDaoMock ++ journalMock ++ commonMocks ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
              )
            },
            testM("if usersbids service fails") {
              val paramsMock = AuctionParamsServiceMock.Get(equalTo(testAuction.key), value(testParams)) &&
                AuctionParamsServiceMock.Get(equalTo(testAuction2.key), value(testParams2))
              val howMuchMock = HowMuchMock.GetPrices(anything, value(testPriceResponse4Entries)).optional
              val usersBidsMock = UsersBidsServiceMock.Get(anything, failure(new Throwable)).atMost(2)
              val auctionBlockDaoMock = AuctionBlockDaoMock.empty
              val journalMock = JournalServiceMock.empty

              val res = for {
                auction <- ZIO.service[UserAuctionService]
                states <- auction.allUsersAuctionsStates(List(testAuction.key, testAuction2.key))
              } yield states

              assertM(res.run)(fails(anything)).provideCustomLayer(
                auctionBlockDaoMock ++ journalMock ++ commonMocks ++ usersBidsMock ++ paramsMock ++ howMuchMock >>> UserAuctionServiceLive.live
              )
            }
          )
        ),
        suite("auctionSettings")(
          testM("should return auction settings for AuctionKeys") {
            val auctionKeys = Seq(testAuction.key, testAuction2.key)
            val paramsMock = AuctionParamsServiceMock.Get(
              equalTo(testAuction.key),
              value(testParams)
            ) && AuctionParamsServiceMock.Get(equalTo(testAuction2.key), value(testParams2))
            val howMuchMock = HowMuchMock.GetPrices(
              hasField("entries", (a: PriceRequest) => a.entries, hasSize(equalTo(2))),
              value(testPriceResponseOnlyPrices)
            )
            val usersBidsMock = UsersBidsServiceMock.empty
            val auctionBlockDaoMock = AuctionBlockDaoMock.empty
            val journalMock = JournalServiceMock.empty

            val expected = Seq(
              AuctionSettings(
                auctionKey = testAuction.key,
                basePrice = BasePrice(testBasePrice),
                auctionStep = testNextStep,
                minBid = testBasePrice + testFirstStep
              ),
              AuctionSettings(
                auctionKey = testAuction2.key,
                basePrice = BasePrice(testBasePrice2),
                auctionStep = testNextStep2,
                minBid = testBasePrice2 + testFirstStep2
              )
            )
            val res = for {
              auction <- ZIO.service[UserAuctionService]
              results <- auction.auctionSettings(auctionKeys)
            } yield results

            assertM(res)(equalTo(expected)).provideCustomLayer(
              commonMocks ++ paramsMock ++ auctionBlockDaoMock ++ journalMock ++ usersBidsMock ++ howMuchMock >>> UserAuctionServiceLive.live
            )
          }
        )
      )
    )
  }

  private val product = ProductId("call")
  private val project = Project.Autoru
  private val user = UserId("user:1")
  private val user2 = UserId("user:2")
  private val criterion1key = "key"
  private val criterion1value = "value"
  private val criterion2key = "key2"
  private val criterion2value = "value2"
  private val superGenCriterionKey = "super_gen_id"
  private val superGenCriterionValue = "123"

  private val testContext = CriteriaContext(
    List(Criterion(CriterionKey(criterion1key), CriterionValue(criterion1value)))
  )

  private val testContext2 = CriteriaContext(
    List(Criterion(CriterionKey(criterion2key), CriterionValue(criterion2value)))
  )

  private val testContext3 = CriteriaContext(
    List(
      Criterion(CriterionKey(criterion2key), CriterionValue(criterion2value)),
      Criterion(CriterionKey(superGenCriterionKey), CriterionValue(superGenCriterionValue))
    )
  )

  private val testAuction = UserAuction(
    AuctionKey(
      project,
      product,
      testContext,
      auctionObject = None
    ),
    user
  )

  private val testAuction2 = UserAuction(
    AuctionKey(
      project,
      product,
      testContext2,
      auctionObject = None
    ),
    user2
  )

  private val testAuction3 = UserAuction(
    AuctionKey(
      project,
      ProductId("call:cars:used"),
      testContext3,
      auctionObject = None
    ),
    user2
  )
  private val testBid = Kopecks(2000)
  private val testBasePrice = Kopecks(100)
  private val testBid2 = Kopecks(3000)
  private val testBasePrice2 = Kopecks(200)

  private val testFirstStep = Kopecks(200)
  private val testNextStep = Kopecks(100)
  private val testFirstStep2 = Kopecks(300)
  private val testNextStep2 = Kopecks(200)

  private val testParams = AuctionParams(
    AuctionKey(
      project,
      product,
      testContext,
      auctionObject = None
    ),
    BasePricePlusAmount(testFirstStep),
    ArithmeticProgression(testNextStep)
  )

  private val testParams2 = AuctionParams(
    AuctionKey(
      project,
      product,
      testContext2,
      auctionObject = None
    ),
    BasePricePlusAmount(testFirstStep2),
    ArithmeticProgression(testNextStep2)
  )

  private val testBasePricePC = ProtoSource(ProtoSource.Source.ServiceRequest("pc-base-1"))
  private val testBidPC = ProtoSource(ProtoSource.Source.ServiceRequest("pc-bid-1"))
  private val testBasePrice2PC = ProtoSource(ProtoSource.Source.ServiceRequest("pc-base-2"))
  private val testBid2PC = ProtoSource(ProtoSource.Source.ServiceRequest("pc-bid-2"))

  private val testPriceResponse4Entries = PriceResponse(
    List(
      PriceResponseEntry(priceEntryId(0), "test rule", testBasePrice, testBasePricePC),
      PriceResponseEntry(bidEntryId(0), "other rule", testBid, testBidPC),
      PriceResponseEntry(priceEntryId(1), "test rule", testBasePrice2, testBasePrice2PC),
      PriceResponseEntry(bidEntryId(1), "other rule", testBid2, testBid2PC)
    )
  )

  private val testPriceResponseOnlyPrices = PriceResponse(
    List(
      PriceResponseEntry(priceEntryId(0), "test rule", testBasePrice, testBasePricePC),
      PriceResponseEntry(priceEntryId(1), "test rule", testBasePrice2, testBasePrice2PC)
    )
  )

  private val testSource = AuctionChangeSource.UserAction(user)

  private def testPriceResponse(price: Option[Kopecks]) = {
    val defaultSource = ProtoSource(ProtoSource.Source.ServiceRequest("pc-default"))

    PriceResponse(
      price.toList.map(p => PriceResponseEntry(defaultEntryId, "other rule", p, defaultSource))
    )
  }

  private val commonMocks =
    (Logging.live ++ Clock.live) >+> TestFeatureToggles.live ++ RequestId.test(Some("test-request-id"))

  private def placeBidFailedDependencies(price: Option[Kopecks]) = {
    val paramsMock = AuctionParamsServiceMock.Get(anything, value(testParams))
    val howMuchMock = HowMuchMock.GetPrices(anything, value(testPriceResponse(price)))
    val usersBidsMock = UsersBidsServiceMock.empty
    val journalMock = JournalServiceMock.empty
    val auctionBlockDaoMock = AuctionBlockDaoMock.empty
    TestEnvironment.live ++ auctionBlockDaoMock ++ usersBidsMock ++ paramsMock ++ howMuchMock ++ journalMock ++ commonMocks >>> UserAuctionServiceLive.live
  }

  private def bidByDateTimeDependencies(price: Option[Kopecks]) = {
    val paramsMock = AuctionParamsServiceMock.empty
    val howMuchMock = HowMuchMock.GetPrices(anything, value(testPriceResponse(price)))
    val usersBidsMock = UsersBidsServiceMock.empty
    val journalMock = JournalServiceMock.empty
    val auctionBlockDaoMock = price match {
      case None => AuctionBlockDaoMock.empty
      case Some(_) => AuctionBlockDaoMock.UserHadBlockAtMoment(anything, value(true)).toLayer
    }

    auctionBlockDaoMock ++ paramsMock ++ howMuchMock ++ usersBidsMock ++ journalMock ++ commonMocks >>> UserAuctionServiceLive.live
  }

  private def placeBid(prevBid: Option[Bid], bid: Bid) = for {
    auction <- ZIO.service[UserAuctionService]
    _ <- auction.placeBid(testAuction, prevBid, bid, Some(testSource))
  } yield ()

}
