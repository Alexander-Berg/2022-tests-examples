package ru.auto.salesman.service.feature

import ru.auto.salesman.Task
import ru.auto.salesman.model.user.{
  PaidOfferProduct,
  PaidTransaction,
  ProductRequest,
  VosProductSource
}
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, ProductStatus}
import ru.auto.salesman.service.VosPushApi
import ru.auto.salesman.service.feature.VosNotifyingPaidOfferProductServiceSpec.MockPaidProductService
import ru.auto.salesman.service.user.PaidProductService
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.OfferModelGenerators
import ru.auto.salesman.test.model.gens.user.UserModelGenerators
import ru.auto.salesman.util.CacheControl.NoCache
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import zio.ZIO

class VosNotifyingPaidOfferProductServiceSpec
    extends BaseSpec
    with UserModelGenerators
    with OfferModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  implicit val rc: RequestContext = AutomatedContext("unit-test", NoCache)

  val vosPushApi = mock[VosPushApi]

  val service = new MockPaidProductService
    with VosNotifyingPaidOfferProductService[PaidOfferProduct] {

    def vosPushApi: VosPushApi =
      VosNotifyingPaidOfferProductServiceSpec.this.vosPushApi
  }

  val pushPaidOfferProductsMock = toMockFunction1 {
    vosPushApi.pushPaidOfferProducts
  }

  "VosNotifyingPaidProductService" should {

    "push added paid offer products" in {
      forAll(paidTransactionGen(), ProductRequestGen, PaidOfferProductGen) {
        (transaction, productRequest, product) =>
          pushPaidOfferProductsMock
            .expects(List(VosProductSource(product)))
            .returningZ(())
          service.onAdd(product)
          service
            .add(transaction, productRequest)
            .success
            .value shouldBe product
      }
    }

  }
}

object VosNotifyingPaidOfferProductServiceSpec {

  class MockPaidProductService extends PaidProductService[PaidOfferProduct] {

    private var addResult: Option[PaidOfferProduct] = None

    def onAdd(result: PaidOfferProduct): Unit =
      addResult = Some(result)

    def add(
        transaction: PaidTransaction,
        productRequest: ProductRequest
    ): Task[PaidOfferProduct] =
      ZIO
        .fromOption(addResult)
        .orElseFail(new UnsupportedOperationException)

    def deactivate(
        transaction: PaidTransaction,
        products: List[PaidOfferProduct],
        reason: ProductStatus
    ): Task[Unit] =
      ZIO.fail(new UnsupportedOperationException)
  }
}
