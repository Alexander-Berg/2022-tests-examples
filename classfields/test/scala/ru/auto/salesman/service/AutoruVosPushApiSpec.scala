package ru.auto.salesman.service

import org.joda.time.DateTime
import ru.auto.api.{CommonModel, RequestModel}
import ru.auto.salesman.client.VosClient
import ru.auto.salesman.model.user.VosProductSource
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  ProductStatus,
  ProductStatuses
}
import ru.auto.salesman.service.AutoruVosPushApiSpec.buildExpectedRequest
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.addServicesResultGen
import ru.auto.salesman.test.model.gens.user.UserModelGenerators

import scala.util.Random

class AutoruVosPushApiSpec extends BaseSpec with UserModelGenerators {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  private val vosClient = mock[VosClient]

  val pushApi = new AutoruVosPushApi(vosClient)

  private val addServicesMock = toMockFunction4 {
    vosClient.addServices
  }

  "AutoruVosPushApi" should {
    "push paid offer products" in {
      forAll(list(1, 20, vosProductSourceGen), addServicesResultGen) {
        (allProducts, result) =>
          val byUserAndOffer = allProducts.groupBy(p => (p.user, p.offer))
          byUserAndOffer.foreach { case ((user, offer), products) =>
            addServicesMock
              .expects(offer, None, user, buildExpectedRequest(products))
              .returningZ(result)
          }
          pushApi.pushPaidOfferProducts(allProducts).success.value shouldBe unit
      }
    }

    val maxDeadline = DateTime.parse("2020-05-04T18:00:00+03:00")
    val deadlines = Random.shuffle(
      List(
        maxDeadline,
        DateTime.parse("2020-05-04T17:30:00+03:00"),
        DateTime.parse("2020-05-04T12:00:00+03:00"),
        DateTime.parse("2020-04-18T15:00:00+03:00"),
        DateTime.parse("2020-04-07T13:00:00+03:00")
      )
    )

    "push product with max deadline" in {
      forAll(vosProductSourceGen, addServicesResultGen) { (baseProduct, result) =>
        val inputProducts =
          deadlines.map(deadline => baseProduct.copy(deadline = deadline))
        val expectedProduct = baseProduct.copy(deadline = maxDeadline)
        addServicesMock
          .expects(
            baseProduct.offer,
            None,
            baseProduct.user,
            buildExpectedRequest(Iterable(expectedProduct))
          )
          .returningZ(result)

        pushApi
          .pushPaidOfferProducts(inputProducts)
          .success
          .value shouldBe unit
      }
    }

    "push Active product if latest have the same epoch" in {
      forAll(
        productStatusGen.suchThat(_ != ProductStatuses.Active),
        bool,
        vosProductSourceGen,
        addServicesResultGen
      ) { (inactiveStatus, activeFirst, productSource, result) =>
        val activeProduct =
          productSource.copy(status = ProductStatuses.Active)
        val inactiveProduct = productSource.copy(status = inactiveStatus)
        val products =
          if (activeFirst) List(activeProduct, inactiveProduct)
          else List(inactiveProduct, activeProduct)
        addServicesMock
          .expects(*, *, *, buildExpectedRequest(Iterable(activeProduct)))
          .returningZ(result)
        pushApi.pushPaidOfferProducts(products).success.value shouldBe unit
      }
    }
  }
}

object AutoruVosPushApiSpec {

  private def buildExpectedRequest(
      products: Iterable[VosProductSource],
      status: Option[ProductStatus] = None
  ): RequestModel.AddServicesRequest = {
    val builder = RequestModel.AddServicesRequest.newBuilder()
    products.foreach { g =>
      builder.addServices(
        CommonModel.PaidService
          .newBuilder()
          .setService(
            if (g.product == AutoruGoods.Placement) "all_sale_add"
            else g.product.alias
          )
          .setCreateDate(g.activated.getMillis)
          .setExpireDate(g.deadline.getMillis)
          .setIsActive(status.getOrElse(g.status) == ProductStatuses.Active)
      )
    }
    builder.build()
  }
}
