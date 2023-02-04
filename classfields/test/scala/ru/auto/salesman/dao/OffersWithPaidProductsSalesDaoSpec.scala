package ru.auto.salesman.dao

import org.joda.time.DateTime
import ru.auto.salesman.dao.OffersWithPaidProductsSalesDao.{
  OfferWithPaidProduct,
  SalesServices,
  SalesServicesCategories,
  Table
}
import ru.auto.salesman.model.ProductId
import ru.auto.salesman.model.offer.AutoruOfferId
import ru.auto.salesman.test.{BaseSpec, TestException}

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success}

trait OffersWithPaidProductsSalesDaoSpec extends BaseSpec {
  protected def dao: OffersWithPaidProductsSalesDao

  private val from = DateTime.parse("2019-01-01")

  private def allOffers(table: Table) = {
    // as in sql/data/sales.sql
    val hash = table match {
      case SalesServices => "3c82"
      case SalesServicesCategories => "94jfg"
    }
    for (i <- 0 to 24)
      yield
        OfferWithPaidProduct(
          20101,
          AutoruOfferId(4444, hash),
          ProductId.PremiumOffer,
          DateTime.parse("2019-01-01"),
          DateTime.parse("2019-01-01"),
          DateTime.parse("2019-01-02").plusDays(i)
        )
  }

  "OffersWithPaidProductsSalesDao" should {
    "get new records from sales_services" in {
      val salesmanTable =
        ArrayBuffer.empty[Seq[OfferWithPaidProduct]]
      dao
        .scanNewRecords(from, SalesServices) { offers =>
          salesmanTable += offers
          Success(DateTime.now())
        }
        .success

      salesmanTable(0) shouldBe allOffers(SalesServices).take(20)
      salesmanTable(1) shouldBe allOffers(SalesServices).takeRight(5)
    }

    "get new records from sales_services_categories" in {
      val salesmanTable =
        ArrayBuffer.empty[Seq[OfferWithPaidProduct]]
      dao
        .scanNewRecords(from, SalesServicesCategories) { offers =>
          salesmanTable += offers
          Success(DateTime.now())
        }
        .success

      salesmanTable(0) shouldBe allOffers(SalesServicesCategories).take(20)
      salesmanTable(1) shouldBe allOffers(SalesServicesCategories).takeRight(5)
    }

    "return max epoch" in {
      val salesmanTable =
        ArrayBuffer.empty[Seq[OfferWithPaidProduct]]
      val result = dao
        .scanNewRecords(from, SalesServices) { offers =>
          salesmanTable += offers
          Success(DateTime.now())
        }
        .success
        .value

      result shouldBe salesmanTable.flatten.maxBy(_.epoch.getMillis).epoch
    }

    "return `from` epoch if no records found" in {
      val lateFrom = DateTime.now().plusDays(1)
      val result = dao
        .scanNewRecords(lateFrom, SalesServices) { _ =>
          Failure(new TestException())
        }
        .success
        .value

      result shouldBe lateFrom
    }

    "fail if batch handling fails" in {
      dao
        .scanNewRecords(from, SalesServicesCategories) { offers =>
          if (allOffers(SalesServicesCategories).take(20) != offers)
            Failure(new TestException)
          else
            Success(DateTime.now())
        }
        .failed
        .get shouldBe a[TestException]
    }

    "do not invoke handler function after batch processing failed" in {
      val salesmanTable =
        ArrayBuffer.empty[Seq[OfferWithPaidProduct]]
      dao
        .scanNewRecords(from, SalesServicesCategories) { offers =>
          if (allOffers(SalesServicesCategories).take(20) != offers)
            Failure(new TestException)
          else {
            salesmanTable += offers
            Success(DateTime.now())
          }
        }
        .failed
      salesmanTable.size shouldBe 1
      salesmanTable.head shouldBe allOffers(SalesServicesCategories).take(20)
    }
  }
}
