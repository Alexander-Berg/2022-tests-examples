package ru.yandex.vertis.billing.shop.domain.test

import billing.common_model.Project
import billing.log_model.TargetType
import common.time.Interval
import ru.yandex.vertis.billing.shop.domain.impl.in_memory.InMemoryProductTypeRevisionRegistry.InMemoryStorage
import ru.yandex.vertis.billing.shop.domain.impl.in_memory.{InMemoryProductTypeRevisionRegistry, InMemoryStorageCreator}
import ru.yandex.vertis.billing.shop.model.Constants.RaiseFreeVasCode
import ru.yandex.vertis.billing.shop.model.{
  GoodsCode,
  GoodsType,
  ProductCode,
  ProductTimeline,
  ProductType,
  ProductTypeRevision,
  Status,
  WithDuration
}
import zio.test.DefaultRunnableSpec
import zio.test.Assertion._
import zio.test._

import java.time.{Duration, Instant}

object InMemoryProductTypeRevisionRegistrySpec extends DefaultRunnableSpec {

  private val inMemoryStorage = createStorageOfOneVersion(singleVersionProduct())
  private val inMemoryProductRegistry = new InMemoryProductTypeRevisionRegistry(inMemoryStorage)

  def spec = suite("InMemoryProductTypeRevisionRegistry")(
    testM("get product by code") {
      for {
        foundedProduct <- inMemoryProductRegistry
          .getProduct(Project.GENERAL, ProductCode(RaiseFreeVasCode), "2021-04-02".instant)
      } yield assert(foundedProduct)(equalTo(singleVersionProduct()))
    },
    testM("get products by project") {
      for {
        foundedProducts <- inMemoryProductRegistry.getProducts(Project.GENERAL, "2021-04-02".instant)
      } yield assert(foundedProducts)(equalTo(List(singleVersionProduct())))
    },
    testM("get products by tag") {
      for {
        foundedProducts <- inMemoryProductRegistry
          .getProductsByTag(Project.GENERAL, "2021-04-02".instant, "test_tag")
      } yield assert(foundedProducts)(equalTo(List(singleVersionProduct())))
    },
    testM("get product by code, if there are versions in past") {
      val version1 = product(RaiseFreeVasCode, "2021-03-01".instant, Some("2021-04-01".instant), tags = Set())
      val version2 = product(RaiseFreeVasCode, "2021-04-01".instant, None, tags = Set())
      val storage = multipleVersionsOneProduct(
        List(
          version1,
          version2
        )
      )
      val registry = new InMemoryProductTypeRevisionRegistry(storage)
      val now = "2021-04-02".instant
      for {
        foundedProduct <- registry
          .getProduct(Project.GENERAL, ProductCode(RaiseFreeVasCode), now)
      } yield assert(foundedProduct)(equalTo(version2))
    },
    testM("get product by code, if there are versions in future") {
      val version1 = product(RaiseFreeVasCode, "2021-03-01".instant, Some("2021-04-01".instant), tags = Set())
      val version2 = product(RaiseFreeVasCode, "2021-04-01".instant, None, tags = Set())
      val storage = multipleVersionsOneProduct(
        List(
          version1,
          version2
        )
      )
      val registry = new InMemoryProductTypeRevisionRegistry(storage)
      val now = "2021-03-20".instant
      for {
        foundedProduct <- registry
          .getProduct(Project.GENERAL, ProductCode(RaiseFreeVasCode), now)
      } yield assert(foundedProduct)(equalTo(version1))
    },
    testM("get product by code, if there are other products") {
      val product1 = product(RaiseFreeVasCode, "2021-03-01".instant, Some("2021-04-01".instant), tags = Set())
      val product2 = product(RaiseFreeVasCode, "2021-03-01".instant, Some("2021-04-01".instant), tags = Set())
      val storage = multipleProductsStorage(
        List(
          List(product1),
          List(product2)
        )
      )
      val registry = new InMemoryProductTypeRevisionRegistry(storage)
      val now = "2021-03-20".instant
      for {
        foundedProduct <- registry
          .getProduct(Project.GENERAL, ProductCode(RaiseFreeVasCode), now)
      } yield assert(foundedProduct)(equalTo(product1))
    },
    testM("get products by tag, if there are other tags") {
      val product1 = product(RaiseFreeVasCode, "2021-03-01".instant, Some("2021-04-01".instant), tags = Set("tag1"))
      val product2 = product(RaiseFreeVasCode, "2021-03-01".instant, Some("2021-04-01".instant), tags = Set("tag1"))
      val product3 = product(RaiseFreeVasCode, "2021-03-01".instant, Some("2021-04-01".instant), tags = Set("tag2"))
      val product4 = product(RaiseFreeVasCode, "2021-03-01".instant, Some("2021-04-01".instant), tags = Set("tag2"))
      val storage = multipleProductsStorage(
        List(
          List(product1),
          List(product2),
          List(product3),
          List(product4)
        )
      )
      val registry = new InMemoryProductTypeRevisionRegistry(storage)
      val now = "2021-03-20".instant
      for {
        foundedProducts <- registry
          .getProductsByTag(Project.GENERAL, now, "tag1")
      } yield assert(foundedProducts)(hasSameElementsDistinct(List(product1, product2)))
    },
    testM("get products by tag, if there are other tags") {
      val product1v1 = product(RaiseFreeVasCode, "2021-03-01".instant, Some("2021-04-01".instant), tags = Set())
      val product1v2 = product(RaiseFreeVasCode, "2021-04-01".instant, None, tags = Set("tag1"))
      val product2v1 = product(RaiseFreeVasCode, "2021-03-01".instant, Some("2021-04-01".instant), tags = Set("tag1"))
      val product2v2 = product(RaiseFreeVasCode, "2021-04-01".instant, None, tags = Set("tag2"))
      val storage = multipleProductsStorage(
        List(
          List(product1v1),
          List(product1v2),
          List(product2v1),
          List(product2v2)
        )
      )
      val registry = new InMemoryProductTypeRevisionRegistry(storage)
      val now = "2021-04-02".instant
      for {
        foundedProducts <- registry
          .getProductsByTag(Project.GENERAL, now, "tag1")
      } yield assert(foundedProducts)(hasSameElementsDistinct(List(product1v2)))
    }
  )

  private def createStorageOfOneVersion(testProduct: ProductTypeRevision) = {
    val rawProductTypes: List[ProductTypeRevision] = List(testProduct)
    val versions = rawProductTypes.sortBy(_.interval.from)
    val timeline = ProductTimeline(Project.GENERAL, testProduct.productType.code, versions)

    InMemoryStorageCreator.create(List(timeline))
  }

  private def singleVersionProduct(): ProductTypeRevision = {
    product(RaiseFreeVasCode, createdAt = "2021-04-01".instant, to = None, tags = Set("test_tag"))
  }

  private def multipleVersionsOneProduct(testProducts: List[ProductTypeRevision]): InMemoryStorage = {
    val versions = testProducts.sortBy(_.interval.from)
    val timeline = ProductTimeline(Project.GENERAL, testProducts.head.productType.code, versions)

    InMemoryStorageCreator.create(List(timeline))
  }

  private def multipleProductsStorage(products: List[List[ProductTypeRevision]]): InMemoryStorage = {
    val timelines = products.map { versions =>
      ProductTimeline(Project.GENERAL, versions.head.productType.code, versions)
    }
    InMemoryStorageCreator.create(timelines)
  }

  private def product(
      name: String,
      createdAt: Instant,
      to: Option[Instant],
      tags: Set[String]): ProductTypeRevision = {
    val testProductCode = ProductCode(name)
    val duration = Duration.ofDays(1)
    val goodsType = GoodsType(GoodsCode("test_goods_code"), WithDuration(duration), "user_offer", tags = Set())
    val productType =
      ProductType(testProductCode, TargetType.Offer, List(goodsType), duration, tags, "Тестовый продукт]")
    val activeness = Status.Active

    ProductTypeRevision("test_code", Interval(createdAt, to), productType, activeness)
  }

  implicit class RichString(private val str: String) extends AnyVal {

    def instant: Instant = {
      Instant.parse(s"${str}T00:00:00.00Z")
    }
  }

}
