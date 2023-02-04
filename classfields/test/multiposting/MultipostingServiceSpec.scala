package auto.dealers.multiposting.logic.test.multiposting

import common.zio.context.ContextHolder
import ru.auto.api.api_offer_model.Category
import ru.auto.api.api_offer_model.Multiposting.Classified
import ru.auto.api.api_offer_model.Multiposting.Classified.ClassifiedName
import ru.auto.common.broker.testkit.ClientActionLoggerDummy
import ru.auto.common.context.RequestPayload
import auto.dealers.multiposting.model.{AvitoOfferId, ClientId}
import ru.auto.multiposting.multiposting_model.Products
import ru.auto.multiposting.multiposting_service.{AddProductRequest, RemoveProductRequest}
import auto.dealers.multiposting.logic.multiposting.MultipostingService
import auto.dealers.multiposting.logic.multiposting.MultipostingService.{MultipostingService, MultipostingServiceError}
import auto.dealers.multiposting.logic.testkit.avito.AvitoProductServiceMock
import auto.dealers.multiposting.logic.testkit.vos.VosProductServiceMock
import common.zio.logging.Logging
import zio.ZLayer
import zio.test._
import zio.magic._
import zio.test.Assertion._
import zio.test.mock.Expectation._

object MultipostingServiceSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("MultipostingServiceSpec")(
      testM("adding avito service makes multiposting proxy to avitoProductService and vos") {
        val clientId_1: Long = 42
        val offerId_1 = "12345"
        val className = ClassifiedName.AVITO
        val category = Category.CARS
        val productsStrings = Seq("xl", "x2_1", "x2_7")
        val products_1 = Products.apply(productsStrings)

        val clientId_2 = ClientId(42)
        val offerId_2 = AvitoOfferId("12345")
        val products_2 = productsStrings

        val classified = Classified.apply(name = className, id = offerId_1)
        val request = AddProductRequest.apply(
          clientId = clientId_1,
          hashedOfferId = offerId_1,
          classified = Some(classified),
          products = Some(products_1),
          category = category
        )

        val env = ZLayer.fromSomeMagic[ZTestEnv, MultipostingService](
          VosProductServiceMock
            .ApplyMultipostingService(
              equalTo((clientId_1, offerId_1, Some(category), className, products_1)),
              value(())
            ),
          // forked call -> at most once for tests
          AvitoProductServiceMock.ApplyProduct(equalTo((clientId_2, offerId_2, products_2)), value(())).atMost(1),
          Logging.live,
          ContextHolder.live[RequestPayload],
          ClientActionLoggerDummy.test,
          MultipostingService.live
        )

        MultipostingService
          .addProduct(request)
          .provideLayer(env)
          .map(assert(_)(isUnit))
      },
      testM("adding drom service makes multiposting proxy only to vos") {
        val clientId_1: Long = 42
        val offerId_1 = "123456"
        val className = ClassifiedName.DROM
        val category = Category.CARS
        val productsStrings = Seq("xl", "x2_1", "x2_7")
        val products_1 = Products.apply(productsStrings)

        val classified = Classified.apply(name = className, id = offerId_1)
        val request = AddProductRequest.apply(
          clientId = clientId_1,
          hashedOfferId = offerId_1,
          classified = Some(classified),
          products = Some(products_1),
          category = category
        )

        val env = ZLayer.fromSomeMagic[ZTestEnv, MultipostingService](
          VosProductServiceMock
            .ApplyMultipostingService(
              equalTo((clientId_1, offerId_1, Some(category), className, products_1)),
              value(())
            ),
          AvitoProductServiceMock.empty,
          Logging.live,
          ContextHolder.live[RequestPayload],
          ClientActionLoggerDummy.test,
          MultipostingService.live
        )

        MultipostingService
          .addProduct(request)
          .provideLayer(env)
          .map(assert(_)(isUnit))
      },
      testM("deleting avito service makes multiposting proxy to vos") {
        val clientId_1: Long = 42
        val offerId_1 = "123456"
        val className = ClassifiedName.AVITO
        val category = Category.CARS
        val productsStrings = Seq("xl", "x2_1", "x2_7")
        val products_1 = Products.apply(productsStrings)

        val request = RemoveProductRequest.apply(
          clientId = clientId_1,
          hashedOfferId = offerId_1,
          classified = className,
          products = Some(products_1),
          category = category
        )

        val env = ZLayer.fromSomeMagic[ZTestEnv, MultipostingService](
          VosProductServiceMock
            .UnapplyMultipostingService(
              equalTo((clientId_1, offerId_1, Some(category), className, products_1)),
              value(())
            ),
          AvitoProductServiceMock.empty,
          Logging.live,
          ContextHolder.live[RequestPayload],
          ClientActionLoggerDummy.test,
          MultipostingService.live
        )

        MultipostingService
          .removeProduct(request)
          .provideLayer(env)
          .map(assert(_)(isUnit))
      },
      testM("adding unknown service gives error") {
        val clientId_1: Long = 42
        val offerId_1 = "123456"
        val className = ClassifiedName.UNKNOWN
        val category = Category.CARS
        val productsStrings = Seq("xl", "x2_1", "x2_7")
        val products_1 = Products.apply(productsStrings)

        val classified = Classified.apply(name = className, id = offerId_1)
        val request = AddProductRequest.apply(
          clientId = clientId_1,
          hashedOfferId = offerId_1,
          classified = Some(classified),
          products = Some(products_1),
          category = category
        )

        val env = ZLayer.fromSomeMagic[ZTestEnv, MultipostingService](
          VosProductServiceMock.empty,
          AvitoProductServiceMock.empty,
          Logging.live,
          ContextHolder.live[RequestPayload],
          ClientActionLoggerDummy.test,
          MultipostingService.live
        )

        assertM(MultipostingService.addProduct(request).provideLayer(env).run)(
          fails(isSubtype[MultipostingServiceError](anything))
        )
      }
    )
}
