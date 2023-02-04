package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.model.SaleCategories
import ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper.truck.CommonTruckUnificator
import ru.yandex.vertis.feedprocessor.autoru.scheduler.services.unificator.UnificatorClient
import ru.yandex.vertis.feedprocessor.autoru.scheduler.util.AutoruGenerators.{
  truckExternalOfferGen,
  truckInfoGen,
  truckLcvInfoGen
}
import ru.yandex.vertis.feedprocessor.dao.KVClient
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters._
import scala.concurrent.Future

/**
  * @author pnaydenov
  */
class UnificatorCachingDecoratorSpec extends WordSpecBase with MockitoSupport {

  private val unificatorClient = mock[UnificatorClient]
  private val kvClient = mock[KVClient]

  "UnificatorMapperWithCaching" should {
    "consider truck subcategory" in {
      val truckFeedTask = tasksGen(serviceInfoGen = serviceInfoGen(categoryGen = SaleCategories.Truck.id))
      val lcvFeedTask = tasksGen(serviceInfoGen = serviceInfoGen(categoryGen = SaleCategories.Lcv.id))
      val truck = truckExternalOfferGen(truckFeedTask, truckInfoGen()).next.copy(mark = "Ford", model = "Transit")
      val lcv = truckExternalOfferGen(lcvFeedTask, truckLcvInfoGen()).next.copy(mark = "Ford", model = "Transit")
      val unificator = new CommonTruckUnificator(unificatorClient)
      val unificatorCachingDecorator = new UnificatorCachingDecorator(unificator, kvClient)
      when(kvClient.bulkGet(?)).thenReturn(Future.successful(Map.empty[String, String]))

      unificatorCachingDecorator.unify(Seq(truck, lcv))

      val arg: ArgumentCaptor[Seq[String]] = ArgumentCaptor.forClass(classOf[Seq[String]])
      verify(kvClient, atLeastOnce()).bulkGet(arg.capture())
      val key1 :: key2 :: Nil = arg.getAllValues.asScala.flatten.toList
      (key1 should not).equal(key2)
    }
  }
}
