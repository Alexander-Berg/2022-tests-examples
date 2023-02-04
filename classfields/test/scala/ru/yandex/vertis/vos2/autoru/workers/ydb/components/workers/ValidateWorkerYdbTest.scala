package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.auto.message.CatalogSchema._
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.catalog.cars.CarsCatalog
import ru.yandex.vos2.autoru.catalog.cars.model.CarCard
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ValidateWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport {

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val offer: Offer = {
      val b = TestUtils.createOffer()
      val carInfo = b.getOfferAutoruBuilder.getCarInfoBuilder
        .setMark("Mark")
        .setModel("Model")
        .setTechParamId(100)
        .setConfigurationId(100)
        .setSuperGenId(100)
        .setComplectationId(100)
        .setSuperGenId(100)
        .setHorsePower(2000)
        .setDisplacement(1000)
        .setEngineType("Type")
        .setTransmission("MECHANIC")
        .build()

      val essentials = b.getOfferAutoruBuilder.getEssentialsBuilder
        .setYear(2000)

      b.getOfferAutoruBuilder
        .setCarInfo(carInfo)
        .setEssentials(essentials)
        .build()
      b.build()
    }

    val mockedFeatureManager = mock[FeaturesManager]
    val mockedFeature = mock[Feature[Boolean]]
    when(mockedFeature.value).thenReturn(false)
    when(mockedFeatureManager.DisableOfferInfoCorrection).thenReturn(mockedFeature)

    val carsCatalog: CarsCatalog = mock[CarsCatalog]

    val worker = new ValidateWorkerYdb(
      carsCatalog
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = mockedFeatureManager
    }
  }

  "do nothing with valid offer with correct info" in new Fixture {
    when(carsCatalog.getCardByTechParamId(?)).thenReturn(
      Some(
        CarCard(
          CatalogCardMessage
            .newBuilder()
            .setVersion(1)
            .setModel(ModelMessage.newBuilder().setVersion(1).setCode("Model"))
            .setSuperGeneration(SuperGenerationMessage.newBuilder().setVersion(1).setId(100))
            .setConfiguration(ConfigurationMessage.newBuilder().setVersion(1).setId(100))
            .setTechparameter(TechparameterMessage.newBuilder().setVersion(1).setDisplacement(1000).setId(100))
            .setMark(MarkMessage.newBuilder().setVersion(1).setCode("Mark"))
            .build()
        )
      )
    )

    val res = worker.process(offer, None)
    val processedOffer = res.updateOfferFunc.get(offer)

    assertResult(false)(processedOffer.getOfferAutoru.getIsTechParamIdInvalid)
    assertResult(1000)(processedOffer.getOfferAutoru.getCarInfo.getDisplacement)
  }

  "set correct values to valid offer with incorrect info" in new Fixture {
    when(carsCatalog.getCardByTechParamId(?)).thenReturn(
      Some(
        CarCard(
          CatalogCardMessage
            .newBuilder()
            .setVersion(1)
            .setModel(ModelMessage.newBuilder().setVersion(1).setCode("DifferentModel"))
            .setSuperGeneration(SuperGenerationMessage.newBuilder().setVersion(1).setId(100))
            .setConfiguration(ConfigurationMessage.newBuilder().setVersion(1).setId(100))
            .setTechparameter(TechparameterMessage.newBuilder().setVersion(1).setDisplacement(40000).setId(100))
            .build()
        )
      )
    )

    val res = worker.process(offer, None)
    val processedOffer = res.updateOfferFunc.get(offer)

    assertResult(false)(processedOffer.getOfferAutoru.getIsTechParamIdInvalid)
    assertResult(40000)(processedOffer.getOfferAutoru.getCarInfo.getDisplacement)
  }

  "mark invalid offer and set possible cards" in new Fixture {
    when(carsCatalog.getCardByTechParamId(?)).thenReturn(None)

    when(carsCatalog.visitCardsWithMarkModel(?, ?)(?)).thenAnswer(new Answer[Unit] {

      override def answer(invocationOnMock: InvocationOnMock): Unit = {
        val func = invocationOnMock.getArgument(2).asInstanceOf[CarCard => Unit]

        Seq(
          CarCard(
            CatalogCardMessage
              .newBuilder()
              .setVersion(1)
              .setTechparameter(
                TechparameterMessage
                  .newBuilder()
                  .setEngineStart(2000)
                  .setVersion(1)
                  .setEngineType("Type")
                  .setDisplacement(1000)
                  .setPower(1000)
                  .setTransmission("MECHANIC")
                  .setId(20)
              )
              .build()
          )
        ).foreach(func)
      }
    })

    val res = worker.process(offer, None)
    val processedOffer = res.updateOfferFunc.get(offer)

    assertResult(true)(processedOffer.getOfferAutoru.getIsTechParamIdInvalid)
    assertResult(1)(processedOffer.getOfferAutoru.getPossibleTechParamIdCount)
  }
}
