package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import org.scalatest.concurrent.ScalaFutures
import ru.auto.api.CatalogModel
import ru.auto.api.CatalogModel.{TechInfo, TechInfoList}
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model.Generators.{newTasksGen, _}
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.OfferMessage
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.AutoruExternalOffer.Unification
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer.TechParam
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClient
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future
import scala.language.implicitConversions

/**
  * @author pnaydenov
  */
class CarsByTechParamUnificatorSpec extends WordSpecBase with MockitoSupport with ScalaFutures {
  "CarsByTechParamUnificatorSpec" should {
    "unify offer with already known tech_param_id" in {
      implicit def valToOptVal[R](value: R): Option[R] = Some(value)

      val unificatorResponseByTechParam = TechInfoList
        .newBuilder()
        .addEntities(
          TechInfo
            .newBuilder()
            .setMarkInfo(
              CatalogModel.Mark
                .newBuilder()
                .setCode("Acura")
            )
            .setModelInfo(
              CatalogModel.Model
                .newBuilder()
                .setCode("MDX, III")
            )
            .setSuperGen(
              CatalogModel.SuperGeneration
                .newBuilder()
                .setId(155003)
            )
            .setConfiguration(
              CatalogModel.Configuration
                .newBuilder()
                .setId(111003)
                .setBodyType("ALLROAD_5_DOORS")
            )
            .setTechParam(
              CatalogModel.TechParam
                .newBuilder()
                .setId(103)
                .setPower(290)
                .setTransmissionAutoru("AT")
                .setDisplacement(3500)
                .setEngineType("GASOLINE")
                .setGearTypeAutoru("REAR")
            )
        )
        .addEntities(TechInfo.newBuilder().build())
        .build()

      val client = mock[UnificatorClient]
      when(client.infoByTechParamIds(?)).thenReturn(Future.successful(unificatorResponseByTechParam))
      val unificator = new CarsByTechParamUnificator(client)
      val offer1 = AutoruGenerators.carExternalOfferGen(newTasksGen).next.copy(modification = TechParam(103))
      val response = unificator.unify(Seq(offer1)).futureValue

      val result = response.head.asInstanceOf[OfferMessage[CarExternalOffer]].offer
      result.unification.get shouldEqual Unification(
        "Acura",
        "MDX, III",
        "ALLROAD_5_DOORS",
        "AT",
        155003L,
        111003L,
        103L,
        "GASOLINE",
        "REAR",
        290,
        3500,
        None
      )
    }
  }
}
