package ru.yandex.realty.seller.backend

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.PrivateMethodTester
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.model.duration.TimeRange
import ru.yandex.realty.model.user.UserRef
import ru.yandex.realty.seller.dao.PurchasedProductDao
import ru.yandex.realty.seller.model.gen.SellerModelGenerators
import ru.yandex.realty.seller.model.product.{PurchaseTarget, PurchasedProduct}
import ru.yandex.realty.seller.stat.{PeriodStat, ServiceStat}
import ru.yandex.realty.stat.AggregationStatLevel

import scala.collection.JavaConverters._
import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class ProductStatManagerSpec
  extends AsyncSpecBase
  with PropertyChecks
  with PrivateMethodTester
  with SellerModelGenerators {

  private val productDao: PurchasedProductDao = mock[PurchasedProductDao]
  private val manager = new ProductStatManager(productDao)

  private val convertProduct: PrivateMethod[Option[StatProduct]] =
    PrivateMethod[Option[StatProduct]]('convertProduct)

  private val statLevelGen: Gen[AggregationStatLevel] = Gen.oneOf(
    AggregationStatLevel.Day,
    AggregationStatLevel.Week,
    AggregationStatLevel.Month,
    AggregationStatLevel.Year
  )

  private val paramGen: Gen[(PurchasedProduct, AggregationStatLevel)] = for {
    level <- statLevelGen
    product <- purchasedProductGen
  } yield (product, level)

  private val statGen: Gen[List[StatProduct]] = for {
    level <- statLevelGen
    products <- Gen.listOf(purchasedProductGen)
  } yield products.flatMap(p => manager.invokePrivate(convertProduct(level, p)))

  private def allPairs(statProducts: Seq[StatProduct]): Seq[(StatProduct, StatProduct)] =
    for {
      (x, i) <- statProducts.zipWithIndex
      (y, j) <- statProducts.zipWithIndex
      if i < j
    } yield (x, y)

  private val reqGen: Gen[(TimeRange, AggregationStatLevel)] = for {
    level <- statLevelGen
    start <- dateTimeInPast().map(Some(_))
    end <- dateTimeInFuture().map(Some(_))
  } yield (TimeRange(start, end), level)

  private def intersects(stat: Iterable[PeriodStat])(sec: Long): Boolean = {
    stat.find(_.getPeriodStart.getSeconds == sec).exists(_.getServicesCount > 0)
  }

  "ProductStatManager" should {

    "convert PurchasedProduct to StatProduct" in {
      forAll(paramGen) {
        case (product, level) =>
          val timeIsSet = product.startTime.nonEmpty && product.endTime.nonEmpty
          val statProducts = manager.invokePrivate(convertProduct(level, product))
          statProducts.nonEmpty shouldBe timeIsSet
          statProducts.foreach(_.duration.nonEmpty shouldBe true)
      }
    }

    "convert StatProducts to ServiceStat" in {
      val toServiceStat = PrivateMethod[ServiceStat]('toServiceStat)
      forAll(statGen) { p =>
        manager.invokePrivate(toServiceStat(p))
      }
    }

    "process activation dates" in {
      val processActivation = PrivateMethod[Seq[StatProduct]]('processActivation)
      forAll(statGen) { statProducts =>
        val processed = manager.invokePrivate(processActivation(Nil, statProducts))
        processed.size shouldBe statProducts.size
        allPairs(processed).foreach(pair => {
          pair._1.duration.intersect(pair._2.duration).nonEmpty shouldBe (pair._1.activation == pair._2.activation)
        })
      }
    }

    "expand dates" in {
      val expandDates = PrivateMethod[Iterable[(DateTime, StatProduct)]]('expandDates)
      forAll(statGen) { statProducts =>
        for (s <- statProducts) {
          val withDate = manager.invokePrivate(expandDates(s))
          whenever(withDate.nonEmpty) {
            withDate.tail.foreach(_._2.price shouldBe 0)
          }
        }
      }
    }

    "get ProductStat" in {
      val products = Gen.listOf(purchasedProductGen).sample.getOrElse(Nil)
      forAll(reqGen) {
        case (range, level) =>
          (productDao
            .getProducts(_: UserRef, _: Option[Iterable[PurchaseTarget]]))
            .expects(*, *)
            .returning(Future.successful(products))
          val statValues = manager.getProductStat(UserRef.empty, None, range, level).futureValue.getValuesList.asScala
          val statProducts: List[StatProduct] = products.flatMap(p => manager.invokePrivate(convertProduct(level, p)))
          val paidDates = statProducts.flatMap(_.duration).distinct.filter(range.inRange)
          paidDates.map(_.getMillis / 1000).forall(intersects(statValues)) shouldBe true
      }
    }
  }
}
