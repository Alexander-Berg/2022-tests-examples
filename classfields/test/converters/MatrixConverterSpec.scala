package vsmoney.auction.scheduler.test.converters

import billing.common_model.Money
import billing.howmuch.model.{Rule, RuleContext, RuleCriteria}
import common.models.finance.Money.Kopecks
import vsmoney.auction.model.{
  AuctionKey,
  Bid,
  CriteriaContext,
  Criterion,
  CriterionKey,
  CriterionValue,
  ProductId,
  Project,
  UserAuction,
  UserId
}
import vsmoney.auction.scheduler.convertes.MatrixConverter
import vsmoney.auction.scheduler.model.UserAuctionWithBid
import zio.test.{DefaultRunnableSpec, ZSpec}
import billing.howmuch.model.Matrix
import billing.common_model.{Project => CommonProject}
import vsmoney.auction.converters.ProtoConverterError
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

object MatrixConverterSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("MatrixConverter")(
      testM("should return user auction without user creds") {
        val productId = ProductId("call")
        val matrix = Matrix(
          CommonProject.AUTORU,
          "call_auction",
          Seq(
            Rule(
              Some(
                RuleContext(
                  Seq(
                    RuleCriteria("user_id", RuleCriteria.Value.DefinedValue("4")),
                    RuleCriteria("mark", RuleCriteria.Value.DefinedValue("bmw")),
                    RuleCriteria("model", RuleCriteria.Value.DefinedValue("x5"))
                  )
                )
              ),
              Some(Money(1000))
            )
          )
        )

        val expectedResponse = Seq(
          UserAuctionWithBid(
            userAuction = UserAuction(
              key = AuctionKey(
                project = Project.Autoru,
                product = productId,
                context = CriteriaContext(
                  criteria = Seq(
                    Criterion(key = CriterionKey("mark"), value = CriterionValue("bmw")),
                    Criterion(key = CriterionKey("model"), value = CriterionValue("x5"))
                  )
                ),
                auctionObject = None
              ),
              user = UserId("4")
            ),
            bid = Bid(
              Kopecks(
                1000L
              )
            ),
            entityId = "0"
          )
        )

        assertM(MatrixConverter.parseMatrix(matrix, Project.Autoru, productId))(equalTo(expectedResponse))
      },
      testM("should return exception ProtoConverterError.PriceNotFoundInMatrix") {
        val productId = ProductId("call")
        val matrix = Matrix(
          CommonProject.AUTORU,
          "call_auction",
          Seq(
            Rule(
              Some(
                RuleContext(
                  Seq(
                    RuleCriteria("user_id", RuleCriteria.Value.DefinedValue("4")),
                    RuleCriteria("mark", RuleCriteria.Value.DefinedValue("bmw")),
                    RuleCriteria("model", RuleCriteria.Value.DefinedValue("x5"))
                  )
                )
              ),
              None
            )
          )
        )
        val res = MatrixConverter.parseMatrix(matrix, Project.Autoru, productId).run

        println(res)
        assertM(res)(
          fails(isSubtype[ProtoConverterError.PriceNotFoundInMatrix](anything))
        )

      },
      testM("should return ProtoConverterError.NotFoundUserInMatrix exception ") {
        val productId = ProductId("call")
        val matrix = Matrix(
          CommonProject.AUTORU,
          "call_auction",
          Seq(
            Rule(
              Some(
                RuleContext(
                  Seq(
                    RuleCriteria("mark", RuleCriteria.Value.DefinedValue("bmw")),
                    RuleCriteria("model", RuleCriteria.Value.DefinedValue("x5"))
                  )
                )
              ),
              Some(Money(1000))
            )
          )
        )

        assertM(MatrixConverter.parseMatrix(matrix, Project.Autoru, productId).run)(
          fails(isSubtype[ProtoConverterError.NotFoundUserInMatrix](anything))
        )
      },
      testM("should return ProtoConverterError.ContextNotFoundInMatrix exception ") {
        val productId = ProductId("call")
        val matrix = Matrix(
          CommonProject.AUTORU,
          "call_auction",
          Seq(
            Rule(
              None,
              Some(Money(1000))
            )
          )
        )

        assertM(MatrixConverter.parseMatrix(matrix, Project.Autoru, productId).run)(
          fails(isSubtype[ProtoConverterError.ContextNotFoundInMatrix](anything))
        )
      }
    )

  }
}
