package vsmoney.auction.scheduler.test.service

import billing.common_model.{Money => CommonMoney, Project => CommonProject}
import billing.howmuch.model._
import common.models.finance.Money.Kopecks
import common.zio.logging.Logging
import infra.feature_toggles.client.testkit.TestFeatureToggles
import ru.yandex.vertis.s3edr.core.storage.DataType
import vsmoney.auction.clients.testkit.HowMuchMock
import vsmoney.auction.model.`export`.ProductAuctions
import vsmoney.auction.model.howmuch.{PriceRequest, PriceResponse, PriceResponseEntry}
import vsmoney.auction.model.{BasePrice, Bid, ProductId}
import vsmoney.auction.scheduler.services.AuctionExporter
import vsmoney.auction.scheduler.services.impl.{AuctionExporterLive, SearcherS3Exporter}
import vsmoney.auction.scheduler.testkit.ProductsAuctionsExporterMock
import vsmoney.auction.storage.testkit.AuctionBlockDaoMock
import zio.ZIO
import zio.clock.Clock
import zio.test.Assertion._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object AuctionExporterSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] = {
    suite("AuctionExporter")(
      testM("should good convert matrix and save to export") {
        val howMuchMock = HowMuchMock.GetPrices(
          anything,
          value(
            PriceResponse(
              entries = Seq(
                PriceResponseEntry(
                  entryId = "0",
                  ruleId = "ddd",
                  price = Kopecks(123),
                  Source(Source.Source.ServiceRequest("promo-campaign-1"))
                )
              )
            )
          )
        )

        val productsAuctionsExporterMock = ProductsAuctionsExporterMock.ExportAuctions(
          hasField[(Seq[ProductAuctions], DataType), ProductId](
            "product",
            { case (r: Seq[ProductAuctions], _) => r.head.product },
            equalTo(ProductId("call"))
          ) &&
            hasField[(Seq[ProductAuctions], DataType), Bid](
              "bid",
              { case (r: Seq[ProductAuctions], _) => r.head.auctions.head.auctions.head.bid },
              equalTo(Bid(Kopecks(1000)))
            ) &&
            hasField[(Seq[ProductAuctions], DataType), BasePrice](
              "basePrice",
              { case (r: Seq[ProductAuctions], _) => r.head.auctions.head.auctions.head.basePrice },
              equalTo(BasePrice(Kopecks(123)))
            ),
          unit
        )

        val auctionBlockDaoMock = AuctionBlockDaoMock.GetAllUsersWithActiveBlock(anything, value(Set.empty))

        val request = Matrix(
          CommonProject.AUTORU,
          "call",
          Seq(
            Rule(
              Some(
                RuleContext(
                  Seq(
                    RuleCriteria("user_id", RuleCriteria.Value.DefinedValue("4")),
                    RuleCriteria("mark", RuleCriteria.Value.DefinedValue("bmw"))
                  )
                )
              ),
              Some(CommonMoney(1000))
            )
          )
        )

        val response = for {
          auctionExporter <- ZIO.service[AuctionExporter]
          response <- auctionExporter.enrichAndExport(request)
        } yield { response }

        assertM(response)(isUnit)
          .provideCustomLayer(
            featureTogglesClientMock ++ howMuchMock ++ auctionBlockDaoMock ++ productsAuctionsExporterMock >>> AuctionExporterLive.live
          )
      },
      testM("should good convert matrix and save to export if Rules is Empty") {
        val request = Matrix(
          project = CommonProject.AUTORU,
          matrixId = "call",
          rules = Seq()
        )
        val productsAuctionsExporterMock = ProductsAuctionsExporterMock.ExportAuctions(
          hasField(
            "product",
            { case (r: Seq[ProductAuctions], _) => r.head.product },
            equalTo(ProductId("call"))
          ),
          unit
        )
        val auctionBlockDaoMock = AuctionBlockDaoMock.GetAllUsersWithActiveBlock(anything, value(Set.empty))
        val response = for {
          auctionExporter <- ZIO.service[AuctionExporter]
          response <- auctionExporter.enrichAndExport(request)
        } yield { response }

        assertM(response)(isUnit)
          .provideCustomLayer(
            featureTogglesClientMock ++ HowMuchMock.empty ++ auctionBlockDaoMock ++ productsAuctionsExporterMock >>> AuctionExporterLive.live
          )
      },
      testM("should select export dataType used if productId in matrix call:cars:used") {
        val howMuchMock = HowMuchMock.GetPrices(
          anything,
          value(
            PriceResponse(
              entries = Seq(
                PriceResponseEntry(
                  entryId = "0",
                  ruleId = "ddd",
                  price = Kopecks(123),
                  Source(Source.Source.ServiceRequest("promo-campaign-1"))
                )
              )
            )
          )
        )

        val productsAuctionsExporterMock = ProductsAuctionsExporterMock.ExportAuctions(
          hasField[(Seq[ProductAuctions], DataType), DataType](
            "product",
            { case (_, d: DataType) => d },
            equalTo(SearcherS3Exporter.indexAuctionCarsUsed)
          ),
          unit
        )

        val auctionBlockDaoMock = AuctionBlockDaoMock.GetAllUsersWithActiveBlock(anything, value(Set.empty))

        val request = Matrix(
          CommonProject.AUTORU,
          "call:cars:used",
          Seq(
            Rule(
              Some(
                RuleContext(
                  Seq(
                    RuleCriteria("user_id", RuleCriteria.Value.DefinedValue("4")),
                    RuleCriteria("mark", RuleCriteria.Value.DefinedValue("bmw"))
                  )
                )
              ),
              Some(CommonMoney(1000))
            )
          )
        )

        val response = for {
          auctionExporter <- ZIO.service[AuctionExporter]
          response <- auctionExporter.enrichAndExport(request)
        } yield { response }

        assertM(response)(isUnit)
          .provideCustomLayer(
            featureTogglesClientMock ++ howMuchMock ++ auctionBlockDaoMock ++ productsAuctionsExporterMock >>> AuctionExporterLive.live
          )
      },
      testM("should update super_gen_id in howmuch base prices request if feature enabled") {
        val howMuchMock = HowMuchMock.GetPrices(
          hasField("entries", (a: PriceRequest) => a.entries, hasSize(equalTo(1))) &&
            hasField(
              "entries",
              (a: PriceRequest) =>
                a.entries.head.context.criteria.find(_.key.value == "super_gen_id").map(_.value.value),
              equalTo(Option("*"))
            ),
          value(
            PriceResponse(
              entries = Seq(
                PriceResponseEntry(
                  entryId = "0",
                  ruleId = "ddd",
                  price = Kopecks(123),
                  Source(Source.Source.ServiceRequest("promo-campaign-1"))
                )
              )
            )
          )
        )

        val productsAuctionsExporterMock = ProductsAuctionsExporterMock.ExportAuctions(
          hasField[(Seq[ProductAuctions], DataType), DataType](
            "product",
            { case (_, d: DataType) => d },
            equalTo(SearcherS3Exporter.indexAuctionCarsUsed)
          ),
          unit
        )

        val auctionBlockDaoMock = AuctionBlockDaoMock.GetAllUsersWithActiveBlock(anything, value(Set.empty))

        val request = Matrix(
          CommonProject.AUTORU,
          "call:cars:used",
          Seq(
            Rule(
              Some(
                RuleContext(
                  Seq(
                    RuleCriteria("user_id", RuleCriteria.Value.DefinedValue("4")),
                    RuleCriteria("mark", RuleCriteria.Value.DefinedValue("bmw")),
                    RuleCriteria("super_gen_id", RuleCriteria.Value.DefinedValue("1432"))
                  )
                )
              ),
              Some(CommonMoney(1000))
            )
          )
        )

        val response = for {
          featuresClient <- ZIO.service[TestFeatureToggles.Service]
          _ = featuresClient.set("auction_cars_used_super_gen_id_ignore", true)
          auctionExporter <- ZIO.service[AuctionExporter]
          response <- auctionExporter.enrichAndExport(request)
        } yield { response }

        assertM(response)(isUnit)
          .provideCustomLayer(
            featureTogglesClientMock ++ howMuchMock ++ auctionBlockDaoMock ++ productsAuctionsExporterMock >+> AuctionExporterLive.live
          )
      }
    )
  }

  private val featureTogglesClientMock = (Logging.live ++ Clock.live) >>> TestFeatureToggles.live
}
