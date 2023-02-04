package vsmoney.auction.scheduler.test.task.scheduler

import billing.common_model.{Money => ProtoMoney, Project => BillingProtoProject}
import billing.howmuch.model.{Matrix, Rule => ProtoBillingRule, RuleContext => ProtoRuleContext, RuleCriteria}
import com.typesafe.config.ConfigFactory
import ru.auto.api.api_offer_model.{Offer, OfferStatus}
import vsmoney.auction.scheduler.model.UserAuctionWithBid
import vsmoney.auction.scheduler.task.scheduler.RemoveBidsOnNotActiveOffersTask
import vsmoney.auction.scheduler.testkit.{AuctionBidsClientMock, S3EdrReaderMock, VosServiceMock}
import zio.{ZIO, ZLayer}
import zio.clock.Clock
import zio.stream.ZStream
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object RemoveBidsOnNotActiveOffersTaskSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("RemoveBidsOnNotActiveOffersTask")(
      testM("success calculate data") {
        val matrixByteArray = ZStream.fromIterable(sourceMatrix.toByteArray)
        val s3edrMock = S3EdrReaderMock.Data(anything, value(matrixByteArray))
        val auctionBidsClientMock = AuctionBidsClientMock.LeaveAuction(
          hasField(
            "offer_id",
            { case (r: UserAuctionWithBid) =>
              r.userAuction.key.auctionObject.get.value.value
            },
            equalTo("345-dfe")
          ),
          unit
        )
        val vosServiceMock = VosServiceMock.GetOffer(
          hasField(
            "id",
            { case (_, id: String, _) =>
              id
            },
            equalTo("345-dfe")
          ),
          value(Some(Offer.defaultInstance.copy(status = OfferStatus.EXPIRED)))
        ) &&
          VosServiceMock.GetOffer(
            hasField(
              "id",
              { case (_, id: String, _) =>
                println(id)
                id
              },
              equalTo("123-ddfd")
            ),
            value(Some(Offer.defaultInstance.copy(status = OfferStatus.ACTIVE)))
          )
        val res = for {
          service <- ZIO.service[RemoveBidsOnNotActiveOffersTask]
          res <- service.program
        } yield res
        assertM(res)(isUnit).provideCustomLayer(
          (s3edrMock ++ vosServiceMock ++ auctionBidsClientMock) >>> RemoveBidsOnNotActiveOffersTask.live ++
            Clock.live ++ ZLayer.succeed(ConfigFactory.defaultApplication())
        )

      }
    )

  lazy val sourceMatrix = Matrix(
    project = BillingProtoProject.AUTORU,
    matrixId = "call:cars:used_auction",
    rules = Seq(
      ProtoBillingRule(
        context = Some(
          ProtoRuleContext(
            Seq(
              RuleCriteria("user_id", RuleCriteria.Value.DefinedValue("4")),
              RuleCriteria("mark", RuleCriteria.Value.DefinedValue("bmw")),
              RuleCriteria("model", RuleCriteria.Value.DefinedValue("x5"))
            )
          )
        ),
        price = Some(
          ProtoMoney(kopecks = 1000)
        )
      ),
      ProtoBillingRule(
        context = Some(
          ProtoRuleContext(
            Seq(
              RuleCriteria("user_id", RuleCriteria.Value.DefinedValue("4")),
              RuleCriteria("mark", RuleCriteria.Value.DefinedValue("bmw")),
              RuleCriteria("model", RuleCriteria.Value.DefinedValue("x5")),
              RuleCriteria("offer_id", RuleCriteria.Value.DefinedValue("345-dfe"))
            )
          )
        ),
        price = Some(
          ProtoMoney(kopecks = 1500)
        )
      ),
      ProtoBillingRule(
        context = Some(
          ProtoRuleContext(
            Seq(
              RuleCriteria("user_id", RuleCriteria.Value.DefinedValue("4")),
              RuleCriteria("mark", RuleCriteria.Value.DefinedValue("bmw")),
              RuleCriteria("model", RuleCriteria.Value.DefinedValue("x5")),
              RuleCriteria("offer_id", RuleCriteria.Value.DefinedValue("123-ddfd"))
            )
          )
        ),
        price = Some(
          ProtoMoney(kopecks = 1700)
        )
      )
    )
  )
}
