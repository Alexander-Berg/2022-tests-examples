package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import ru.auto.api.unification.Unification.{CarsUnificationCollection, CarsUnificationEntry}
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model.Generators
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.OfferMessage
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.CarExternalOffer.ModificationString
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClient
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * @author pnaydenov
  */
class CarsUnificatorWithAlternativesSpec extends WordSpecBase with MockitoSupport with ScalaFutures {

  val offerTemplate = AutoruGenerators
    .carExternalOfferGen(Generators.newTasksGen)
    .next
    .copy(
      mark = "Foo",
      model = "Bar",
      year = 100500,
      bodyType = "Baz"
    )

  val entryTemplate = CarsUnificationEntry
    .newBuilder()
    .setRawMark("Foo")
    .setRawModel("Bar")
    .setRawBodyType("Baz")
    .setRawYear("100500")
    .setRawPower("115 л.с.")
    .setRawTransmission("AT")
    .setRawDisplacement("1.6")
    .setRawIs4Wd("false")
    .setMark("FOO")
    .setModel("BAR")
    .setBodyType("BAZ")
    .setHorsePower(115)
    .setTransmission("AUTOMATIC")
    .setDisplacement(1600)
    .setEngineType("GASOLINE")
    .setTechParamId(1)

  "CarsUnificatorWithAlternatives.AutomaticTransmissionFix" should {
    "select most correct by power" in {
      val client = mock[UnificatorClient]
      when(client.carsUnify(?)).thenReturn(
        Future.successful(
          CarsUnificationCollection
            .newBuilder()
            .addEntries(entryTemplate.clone().setHorsePower(113))
            .addEntries(
              entryTemplate
                .clone()
                .setRawTransmission("AMT")
                .setTransmission("ROBOT")
                .setHorsePower(117)
                .setTechParamId(2)
            )
            .addEntries(
              entryTemplate
                .clone()
                .setRawTransmission("CVT")
                .setTransmission("VARIATOR")
                .setHorsePower(116)
                .setTechParamId(3)
            )
            .build()
        )
      )

      val unificator =
        new CarsUnificatorWithAlternatives(client) with CarsUnificatorWithAlternatives.AutomaticTransmissionFix
      val response = unificator
        .unify(
          offerTemplate
            .copy(modification = ModificationString("1.6 AT (115 л.с.)")) :: Nil
        )
        .futureValue
      response should have size (1)
      val offer = response.head.asInstanceOf[OfferMessage[CarExternalOffer]].offer

      offer.unification.get.transmission.get shouldEqual "VARIATOR"
      offer.unification.get.techParamId.get shouldEqual 3

      val arg: ArgumentCaptor[CarsUnificationCollection] = ArgumentCaptor.forClass(classOf[CarsUnificationCollection])
      verify(client).carsUnify(arg.capture())
      arg.getValue.getEntriesList should have size 3
      arg.getValue.getEntriesList.asScala.map(_.getRawTransmission).toSet shouldEqual Set("AT", "AMT", "CVT")
    }

    "select most correct by power and displacement" in {
      val client = mock[UnificatorClient]
      when(client.carsUnify(?)).thenReturn(
        Future.successful(
          CarsUnificationCollection
            .newBuilder()
            .addEntries(entryTemplate.clone().setHorsePower(115).setDisplacement(1500))
            .addEntries(
              entryTemplate
                .clone()
                .setRawTransmission("AMT")
                .setTransmission("ROBOT")
                .setHorsePower(115)
                .setDisplacement(1550)
                .setTechParamId(2)
            )
            .addEntries(
              entryTemplate
                .clone()
                .setRawTransmission("CVT")
                .setTransmission("VARIATOR")
                .setHorsePower(115)
                .setDisplacement(1700)
                .setTechParamId(3)
            )
            .build()
        )
      )

      val unificator =
        new CarsUnificatorWithAlternatives(client) with CarsUnificatorWithAlternatives.AutomaticTransmissionFix
      val response = unificator
        .unify(
          offerTemplate
            .copy(modification = ModificationString("1.6 AT (115 л.с.)")) :: Nil
        )
        .futureValue
      response should have size (1)
      val offer = response.head.asInstanceOf[OfferMessage[CarExternalOffer]].offer

      offer.unification.get.transmission.get shouldEqual "ROBOT"
      offer.unification.get.techParamId.get shouldEqual 2

      val arg: ArgumentCaptor[CarsUnificationCollection] = ArgumentCaptor.forClass(classOf[CarsUnificationCollection])
      verify(client).carsUnify(arg.capture())
      arg.getValue.getEntriesList should have size 3
      arg.getValue.getEntriesList.asScala.map(_.getRawTransmission).toSet shouldEqual Set("AT", "AMT", "CVT")
    }

    "prefer original transmission type" in {
      val client = mock[UnificatorClient]
      when(client.carsUnify(?)).thenReturn(
        Future.successful(
          CarsUnificationCollection
            .newBuilder()
            .addEntries(entryTemplate.clone().setHorsePower(115).setDisplacement(1500))
            .addEntries(
              entryTemplate
                .clone()
                .setRawTransmission("AMT")
                .setTransmission("ROBOT")
                .setHorsePower(115)
                .setDisplacement(1500)
                .setTechParamId(2)
            )
            .addEntries(
              entryTemplate
                .clone()
                .setRawTransmission("CVT")
                .setTransmission("VARIATOR")
                .setHorsePower(115)
                .setDisplacement(1500)
                .setTechParamId(3)
            )
            .build()
        )
      )

      val unificator =
        new CarsUnificatorWithAlternatives(client) with CarsUnificatorWithAlternatives.AutomaticTransmissionFix
      val response = unificator
        .unify(
          offerTemplate
            .copy(modification = ModificationString("1.6 AMT (115 л.с.)")) :: Nil
        )
        .futureValue
      response should have size (1)
      val offer = response.head.asInstanceOf[OfferMessage[CarExternalOffer]].offer

      offer.unification.get.transmission.get shouldEqual "ROBOT"
      offer.unification.get.techParamId.get shouldEqual 2

      val arg: ArgumentCaptor[CarsUnificationCollection] = ArgumentCaptor.forClass(classOf[CarsUnificationCollection])
      verify(client).carsUnify(arg.capture())
      arg.getValue.getEntriesList should have size 3
      arg.getValue.getEntriesList.asScala.map(_.getRawTransmission).toSet shouldEqual Set("AT", "AMT", "CVT")
    }
  }

  "CarsUnificatorWithAlternatives.custom" should {
    "allow custom variants" in {
      val client = mock[UnificatorClient]
      when(client.carsUnify(?)).thenReturn(Future.successful(CarsUnificationCollection.newBuilder().build()))
      val unificator =
        new CarsUnificatorWithAlternatives(client) with CarsUnificatorWithAlternatives.AutomaticTransmissionFix {
          override protected def keyAlternatives(key: CarsUnificator.SearchKey): List[CarsUnificator.SearchKey] = {
            val key2 = key.copy(driveOr4WdFlag = Some(Left(true)))
            key2 :: (super.keyAlternatives(key) ++ super.keyAlternatives(key2))
          }
        }
      unificator.unify(offerTemplate.copy(modification = ModificationString("1.6 AT (115 л.с.)")) :: Nil).futureValue

      val arg: ArgumentCaptor[CarsUnificationCollection] = ArgumentCaptor.forClass(classOf[CarsUnificationCollection])
      verify(client).carsUnify(arg.capture())
      arg.getValue.getEntriesList should have size 6
      arg.getValue.getEntriesList.asScala.map(e => e.getRawIs4Wd -> e.getRawTransmission).toSet shouldEqual
        Set("true" -> "AT", "false" -> "AT", "true" -> "AMT", "false" -> "AMT", "true" -> "CVT", "false" -> "CVT")
    }
  }
}
