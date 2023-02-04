package vsmoney.auction.clients.test

import billing.howmuch.model.Source
import common.models.finance.Money.Kopecks
import vsmoney.auction.clients.PriceRequestCreator.defaultEntryId
import vsmoney.auction.clients.PriceResponseUtils
import vsmoney.auction.model.{MatrixId, ProductId}
import vsmoney.auction.model.howmuch.{PriceResponse, PriceResponseEntry}
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.{assert, DefaultRunnableSpec, ZSpec}

object PriceResponseUtilsSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("PriceResponseSpec")(
      testM("bidFromPriceResponse should take price with entryId = defaultBidEntryId") {

        val testResponse = PriceResponse(
          List(
            PriceResponseEntry("other entry", "other rule", Kopecks(2000), testSource),
            PriceResponseEntry(defaultEntryId, rule_id, testPrice, testSource)
          )
        )

        for {
          res <- PriceResponseUtils.extractPrice(testResponse)
        } yield assert(res)(isSome(equalTo(testPrice)))
      },
      testM("bidFromPriceResponse should return None if no such entryId") {

        val testResponse = PriceResponse(
          List(
            PriceResponseEntry("other entry", "other rule", Kopecks(2000), testSource)
          )
        )

        for {
          res <- PriceResponseUtils.extractPrice(testResponse)
        } yield assert(res)(isNone)
      }
    )
  }

  private val product = ProductId("call")
  private val rule_id = "test rule 1"
  private val testPrice = Kopecks(1000)
  private val testSource = Source(Source.Source.ServiceRequest("promo-campaign-1"))

}
