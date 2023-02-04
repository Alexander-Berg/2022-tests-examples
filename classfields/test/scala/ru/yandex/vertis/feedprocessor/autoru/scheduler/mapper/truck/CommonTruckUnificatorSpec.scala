package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper.truck

import org.mockito.ArgumentCaptor
import org.mockito.Mockito.verify
import org.scalatest.concurrent.ScalaFutures
import ru.auto.api.unification.Unification.{TrucksUnificationCollection, TrucksUnificationEntry}
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.scheduler.model.TruckExternalOffer
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClient
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.feedprocessor.autoru.model.Messages.OfferMessage
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.feedprocessor.autoru.model.ModelUtils._
import ru.yandex.vertis.feedprocessor.autoru.model.SaleCategories

import scala.concurrent.Future

/**
  * @author pnaydenov
  */
class CommonTruckUnificatorSpec extends WordSpecBase with MockitoSupport with ScalaFutures {
  val taskGenerator = tasksGen(serviceInfoGen = serviceInfoGen(categoryGen = SaleCategories.Lcv.id))

  "CommonTruckUnificator" should {
    "not escape comma drom mark/model" in {
      val task = taskGenerator.next
      val client = mock[UnificatorClient]
      when(client.trucksUnify(?)).thenReturn(
        Future.successful(
          TrucksUnificationCollection
            .newBuilder()
            .addEntries(
              TrucksUnificationEntry
                .newBuilder()
                .setRawMark("Ford, SuperEdition")
                .setRawModel("F-series (F-250, F-350, F-450, F-550)")
                .setMark("FORD")
                .setModel("F_SERIES")
                .setTruckCategory(task.truckCategory.get)
            )
            .build()
        )
      )
      val unificator = new CommonTruckUnificator(client)
      val offer1 = AutoruGenerators
        .truckExternalOfferGen(task)
        .next
        .copy(
          mark = "  Ford,   SuperEdition",
          model = "F-series   (F-250, F-350, F-450,   F-550)  ", // should ignore extra spaces
          unification = None
        )

      val result = unificator.unify(Seq(offer1)).futureValue
      val request: ArgumentCaptor[TrucksUnificationCollection] =
        ArgumentCaptor.forClass(classOf[TrucksUnificationCollection])
      verify(client).trucksUnify(request.capture())
      val entry = request.getValue.getEntries(0)
      entry.getRawMark shouldEqual "Ford, SuperEdition"
      entry.getRawModel shouldEqual "F-series (F-250, F-350, F-450, F-550)"

      val offer = result.head.asInstanceOf[OfferMessage[TruckExternalOffer]].offer
      offer.unification shouldNot be(empty)
    }
  }
}
