package ru.auto.api.managers.stats

import ru.auto.api.BreadcrumbsModel.Entity
import ru.auto.api.ResponseModel.PredictResponse
import ru.auto.api.StatsModel.{PredictPrice, PriceRange, PriceTagRequest}
import ru.auto.api.{BaseSpec, StatsModel}
import ru.auto.api.auth.Application
import ru.auto.api.model.{ModelGenerators, RequestParams}
import ru.auto.api.search.SearchModel
import ru.auto.api.services.catalog.CatalogClient
import ru.auto.api.services.predict.PredictClient
import ru.auto.api.services.stats.StatsClient
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.RequestImpl
import ru.auto.catalog.model.api.ApiModel.{ConfigurationCard, RawCatalog, SuperGenCard, TechParamCard}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

class StatsManagerSpec extends BaseSpec with MockitoSupport {

  trait mocks {
    val statsClient = mock[StatsClient]
    val vosClient = mock[VosClient]
    val predictClient = mock[PredictClient]
    val catalogClient = mock[CatalogClient]
    implicit val trace: Traced = Traced.empty
    implicit val request: RequestImpl = new RequestImpl
    request.setApplication(Application.desktop)
    request.setTrace(trace)
    request.setRequestParams(RequestParams.empty.copy(deviceUid = Some("uid")))
    val manager = new StatsManager(statsClient, vosClient, predictClient, catalogClient)
  }

  val catalogRespone = {
    val catalog = RawCatalog.newBuilder()
    val techParam = List("1337").map { tpId =>
      tpId -> TechParamCard
        .newBuilder()
        .setEntity(Entity.newBuilder.setTechParamId(tpId))
        .setParentConfiguration("1338")
        .build()
    }.toMap
    val configuration = List("1338").map { confId =>
      confId -> ConfigurationCard
        .newBuilder()
        .setEntity(Entity.newBuilder.setTechParamId(confId))
        .setParentSuperGen("1339")
        .build()
    }.toMap
    val superGen = List("1338").map { superGenId =>
      superGenId -> SuperGenCard
        .newBuilder()
        .setEntity(Entity.newBuilder.setId(superGenId))
        .setParentModel("X1")
        .build()
    }.toMap
    catalog.putAllTechParam(techParam.asJava)
    catalog.putAllConfiguration(configuration.asJava)
    catalog.putAllSuperGen(superGen.asJava)
    catalog.build()
  }

  "StatsManager" should {
    "should return predicted prices with tag prices for private offer" in new mocks {
      val predictRequest = StatsModel.PredictRequest.newBuilder
        .setTechParamId(1337L)
        .build()

      val user = ModelGenerators.PrivateUserRefGen.next
      request.setUser(user)

      when(catalogClient.filter(?, ?)(?))
        .thenReturnF(catalogRespone)
      when(predictClient.getPredict(?, ?)(?))
        .thenAnswer { args =>
          val f = args.getArguments.apply(1)
          val result = f.asInstanceOf[Option[PricePredictModel]]
          result match {
            case None =>
              Future.successful(
                PredictPrice.newBuilder
                  .setAutoru(buildRange(400000, 500000))
                  .setTradein(buildRange(350000, 450000))
                  .setTradeinDealerMatrixNew(buildRange(500000, 550000))
                  .setTradeinDealerMatrixUsed(buildRange(600000, 650000))
                  .setTradeinDealerMatrixBuyout(buildRange(700000, 750000))
                  .build
              )
            case Some(Q10Q90PredictModel) =>
              Future.successful(
                PredictPrice.newBuilder.setAutoru(buildRange(410000, 490000)).build
              )
            case Some(Q25Q75PredictModel) =>
              Future.successful(
                PredictPrice.newBuilder.setAutoru(buildRange(425000, 475000)).build
              )
            case _ => throw new IllegalArgumentException
          }
        }
      when(vosClient.getTagPrices(?)(?))
        .thenAnswer { args =>
          val result = args.getArguments.apply(0).asInstanceOf[PriceTagRequest]
          result.getPredicted.getAutoru.getFrom shouldBe 400000
          result.getPredicted.getQ10Q90.getFrom shouldBe 410000
          result.getPredicted.getQ10Q90.getTo shouldBe 490000
          result.getPredicted.getQ25Q75.getFrom shouldBe 425000
          Future(
            PredictResponse.newBuilder
              .setTagRanges(
                StatsModel.PriceTagRanges.newBuilder
                  .setExcellent(buildRange(410000, 430000))
                  .setGood(buildRange(430000, 450000))
                  .build
              )
              .build
          )
        }
      val result = manager
        .getPredict(predictRequest)
        .await

      result.getPrices.getAutoru.getFrom shouldBe 400000
      result.getPrices.getAutoru.getTo shouldBe 500000
      result.getPrices.getTradein.getFrom shouldBe 350000
      result.getPrices.getTradein.getTo shouldBe 450000
      result.getTagRanges.getExcellent.getFrom shouldBe 410000
      result.getTagRanges.getExcellent.getTo shouldBe 430000
      result.getTagRanges.getGood.getFrom shouldBe 430000
      result.getTagRanges.getGood.getTo shouldBe 450000
      result.getPrices.getTradeinDealerMatrixNew.getFrom shouldBe 500000
      result.getPrices.getTradeinDealerMatrixNew.getTo shouldBe 550000
      result.getPrices.getTradeinDealerMatrixUsed.getFrom shouldBe 600000
      result.getPrices.getTradeinDealerMatrixUsed.getTo shouldBe 650000
      result.getPrices.getTradeinDealerMatrixBuyout.getFrom shouldBe 700000
      result.getPrices.getTradeinDealerMatrixBuyout.getTo shouldBe 750000
    }
  }

  "StatsManager" should {
    "should return predicted prices with tag prices for commercial offer" in new mocks {
      val predictRequest = StatsModel.PredictRequest.newBuilder
        .setTechParamId(1337L)
        .build()

      val dealer = ModelGenerators.DealerUserRefGen.next
      request.setUser(dealer)
      request.setDealer(dealer)

      when(catalogClient.filter(?, ?)(?))
        .thenReturnF(catalogRespone)
      when(predictClient.getPredict(?, ?)(?))
        .thenAnswer { args =>
          val f = args.getArguments.apply(1)
          val result = f.asInstanceOf[Option[PricePredictModel]]
          result match {
            case Some(DealersPredictModel) =>
              Future.successful(
                PredictPrice.newBuilder
                  .setAutoru(buildRange(430000, 530000))
                  .setCatalog(buildRange(450000, 550000))
                  .setTradein(buildRange(350000, 450000))
                  .setTradeinDealerMatrixNew(buildRange(500000, 550000))
                  .setTradeinDealerMatrixUsed(buildRange(600000, 650000))
                  .setTradeinDealerMatrixBuyout(buildRange(700000, 750000))
                  .build
              )
            case _ => throw new IllegalArgumentException
          }
        }
      when(vosClient.getTagPrices(?)(?))
        .thenAnswer { args =>
          val result = args.getArguments.apply(0).asInstanceOf[PriceTagRequest]
          result.getPredicted.getAutoru.getFrom shouldBe 430000
          result.getPredicted.getAutoru.getTo shouldBe 530000
          result.getPredicted.getQ10Q90.getFrom shouldBe 0
          result.getPredicted.getQ25Q75.getFrom shouldBe 0
          result.getPredicted.getCatalog.getFrom shouldBe 450000
          result.getPredicted.getCatalog.getTo shouldBe 550000
          result.getPredicted.getMarket.getPrice shouldBe 480000
          Future(
            PredictResponse.newBuilder
              .setTagRanges(
                StatsModel.PriceTagRanges.newBuilder
                  .setExcellent(buildRange(440000, 460000))
                  .setGood(buildRange(460000, 480000))
                  .build
              )
              .build
          )
        }
      val result = manager
        .getPredict(predictRequest)
        .await

      result.getPrices.getAutoru.getFrom shouldBe 430000
      result.getPrices.getAutoru.getTo shouldBe 530000
      result.getPrices.getTradein.getFrom shouldBe 350000
      result.getTagRanges.getExcellent.getFrom shouldBe 440000
      result.getTagRanges.getExcellent.getTo shouldBe 460000
      result.getTagRanges.getGood.getFrom shouldBe 460000
      result.getTagRanges.getGood.getTo shouldBe 480000
      result.getPrices.getTradeinDealerMatrixNew.getFrom shouldBe 500000
      result.getPrices.getTradeinDealerMatrixNew.getTo shouldBe 550000
      result.getPrices.getTradeinDealerMatrixUsed.getFrom shouldBe 600000
      result.getPrices.getTradeinDealerMatrixUsed.getTo shouldBe 650000
      result.getPrices.getTradeinDealerMatrixBuyout.getFrom shouldBe 700000
      result.getPrices.getTradeinDealerMatrixBuyout.getTo shouldBe 750000
    }
  }

  private def buildRange(from: Int, to: Int) =
    PriceRange.newBuilder
      .setCurrency(SearchModel.Currency.RUR)
      .setFrom(from)
      .setTo(to)
      .build

}
