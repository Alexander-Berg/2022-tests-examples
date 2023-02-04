package ru.auto.salesman.api.v1.service.products

import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import ru.auto.salesman.api.RoutingSpec
import ru.auto.salesman.dao.ProductDao.ProductAlreadyExists
import ru.auto.salesman.model
import ru.auto.salesman.model.Product.RawKey
import ru.auto.salesman.products.ProductsOuterClass.{
  ActiveProductNaturalKey,
  Product,
  ProductRequest
}
import ru.auto.salesman.service.ProductsService
import ru.auto.salesman.test.model.gens.ProductGenerators

class ProductsHandlerSpec extends RoutingSpec with ProductGenerators {

  val productsService: ProductsService = mock[ProductsService]

  private val route = new ProductsHandler(productsService).route

  "ProductsHandler" should {
    "response 400 if payer validation fails for application-credit:access" in {
      val uri0 = "/create"
      val uri1 = "/prolongable/set"
      val uri2 = "/prolongable/remove"

      val request = ProductRequest
        .newBuilder()
        .setKey(
          ActiveProductNaturalKey
            .newBuilder()
            .setTarget("CARS:NEW")
            .setDomain("application-credit")
            .setProductType("access")
            .setPayer("user:123")
        )
        .build()

      Post(uri0, HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[
          String
        ] shouldBe "Invalid payer [user:123]. Expected dealer:dealerId format."
      }

      Put(uri1, HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[
          String
        ] shouldBe "Invalid payer [user:123]. Expected dealer:dealerId format."
      }

      Put(uri2, HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.BadRequest
        responseAs[
          String
        ] shouldBe "Invalid payer [user:123]. Expected dealer:dealerId format."
      }
    }

    "return 200 on create" in {
      val uri = "/create"

      val request = ProductRequest
        .newBuilder()
        .setKey(
          ActiveProductNaturalKey
            .newBuilder()
            .setTarget("cars:new")
            .setDomain("application-credit")
            .setProductType("access")
            .setPayer("dealer:123")
        )
        .build()

      (productsService.create _)
        .expects(request)
        .returningZ(Right(()))

      Post(uri, HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return 409 on create if product already exists" in {
      val uri = "/create"

      val key = ActiveProductNaturalKey
        .newBuilder()
        .setTarget("cars:new")
        .setDomain("application-credit")
        .setProductType("access")
        .setPayer("dealer:123")
        .build()
      val request = ProductRequest.newBuilder().setKey(key).build()

      (productsService.create _)
        .expects(request)
        .returningZ(
          Left(
            ProductAlreadyExists(model.ActiveProductNaturalKey(key).right.value)
          )
        )

      Post(uri, HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.Conflict
      }
    }

    "return 200 on prolongable set" in {
      val uri = "/prolongable/set"

      val request = ProductRequest
        .newBuilder()
        .setKey(
          ActiveProductNaturalKey
            .newBuilder()
            .setTarget("cars:new")
            .setDomain("application-credit")
            .setProductType("access")
            .setPayer("dealer:123")
        )
        .build()

      (productsService.updateProlongable _)
        .expects(request, true)
        .returningZ(())

      Put(uri, HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return 200 on prolongable remove" in {
      val uri = "/prolongable/remove"

      val request = ProductRequest
        .newBuilder()
        .setKey(
          ActiveProductNaturalKey
            .newBuilder()
            .setTarget("cars:new")
            .setDomain("application-credit")
            .setProductType("access")
            .setPayer("dealer:123")
        )
        .build()

      (productsService.updateProlongable _)
        .expects(request, false)
        .returningZ(())

      Put(uri, HttpEntity(request.toByteArray)) ~> seal(route) ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return 200 on last created product" in {
      val uri =
        "/domain/application-credit/payer/dealer:123/target/cars:new/product-type/access"

      val request = RawKey(
        domain = "application-credit",
        payer = "dealer:123",
        target = "cars:new",
        productType = "access"
      )

      val key = model.ActiveProductNaturalKey(request).right.value
      val productGen = activeProductGen.map(_.copy(key = key))

      forAll(productGen) { product =>
        val response = ProductsService.domainProductToProto(product)

        (productsService.getLastCreatedProduct _)
          .expects(request)
          .returningZ(Some(product))

        Get(uri) ~> seal(route) ~> check {
          status shouldBe StatusCodes.OK
          responseAs[Product] shouldBe response
        }
      }
    }

    "return 404 if there is no product" in {
      val uri =
        "/domain/application-credit/payer/dealer:123/target/cars:new/product-type/access"

      val request = RawKey(
        domain = "application-credit",
        payer = "dealer:123",
        target = "cars:new",
        productType = "access"
      )

      (productsService.getLastCreatedProduct _)
        .expects(request)
        .returningZ(None)

      Get(uri) ~> seal(route) ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }
  }

}
