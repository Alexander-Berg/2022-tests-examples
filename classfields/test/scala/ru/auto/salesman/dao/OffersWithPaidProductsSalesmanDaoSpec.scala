package ru.auto.salesman.dao

import org.joda.time.{DateTime, LocalDate}
import ru.auto.salesman.dao.OffersWithPaidProductsSalesmanDao.{
  ActivatedForClientInDateInterval,
  ActivatedForClientInDateIntervalWithLimit,
  ClientProduct,
  OfferWithPaidProduct
}
import ru.auto.salesman.dao.OffersWithPaidProductsSalesmanDaoSpec.TestOffersWithPaidProductsSalesmanDao
import ru.auto.salesman.model.{ClientId, OfferId}
import ru.auto.salesman.test.BaseSpec

import scala.util.Try

trait OffersWithPaidProductsSalesmanDaoSpec extends BaseSpec {

  protected def dao: TestOffersWithPaidProductsSalesmanDao

  val clientId0 = 1L
  val clientId1 = 2L
  val boostProduct = "boost"
  val placementProduct = "placement"
  val offerId0 = 1L
  val offerId1 = 2L
  val offerId2 = 3L

  private val records = List(
    OfferWithPaidProduct(
      clientId0,
      offerId0,
      boostProduct,
      DateTime.parse("2019-01-01")
    ),
    OfferWithPaidProduct(
      clientId0,
      offerId1,
      placementProduct,
      DateTime.parse("2019-01-03")
    ),
    OfferWithPaidProduct(
      clientId1,
      offerId2,
      boostProduct,
      DateTime.parse("2019-01-02")
    ),
    OfferWithPaidProduct(
      clientId1,
      offerId2,
      placementProduct,
      DateTime.parse("2019-01-02")
    ),
    OfferWithPaidProduct(
      clientId1,
      offerId0,
      boostProduct,
      DateTime.parse("2019-01-01")
    ),
    OfferWithPaidProduct(
      clientId1,
      offerId0,
      boostProduct,
      DateTime.parse("2019-01-02T00:00:00")
    )
  )

  "OffersWithPaidProductsDao.setActivateDates()" should {

    "set one new activate date" in {
      val dt = DateTime.parse("2019-01-01")
      set(activateDates = List(dt))
      getActivateDates() should contain only dt
    }

    "set two new activate dates" in {
      val dt1 = DateTime.parse("2019-01-01")
      val dt2 = DateTime.parse("2019-01-02")
      set(activateDates = List(dt1, dt2))
      getActivateDates() should contain theSameElementsAs List(dt1, dt2)
    }

    def set(
        activateDates: List[DateTime],
        clientId: ClientId = clientId0,
        offerId: OfferId = offerId0,
        product: String = boostProduct
    ): Unit =
      dao
        .setActivateDates(ClientProduct(clientId, offerId, product))(
          activateDates
        )
        .success

    def getActivateDates(
        clientId: ClientId = clientId0,
        product: String = boostProduct
    ): List[DateTime] =
      dao
        .get(
          ActivatedForClientInDateInterval(
            clientId0,
            List(product),
            from = LocalDate.parse("2019-01-01"),
            to = LocalDate.parse("2019-02-01")
          )
        )
        .success
        .value
        .map(_.activateDate)
  }

  "OffersWithPaidProductsDao" should {

    "insert and then select filtered FROM TO records " in {
      dao.insert(records).success

      val result = dao
        .get(
          ActivatedForClientInDateInterval(
            clientId1,
            Iterable("boost"),
            LocalDate.parse("2019-01-01"),
            LocalDate.parse("2019-01-02")
          )
        )
        .success
        .value

      result should have size 3
      result should contain theSameElementsAs Iterable(
        records(2),
        records(4),
        records(5)
      )
    }

    "do not insert duplicate records" in {
      dao.insert(Iterable(records(1), records(1))).success

      val result = dao
        .get(
          ActivatedForClientInDateInterval(
            clientId0,
            Iterable.empty,
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2020-01-02")
          )
        )
        .success
        .value

      result should have size 1
      result should contain theSameElementsAs Iterable(records(1))
    }

    "select unique reports" in {
      dao.insert(Iterable(records(2), records(3), records(4))).success

      val result = dao
        .getUniqueOfferIds(
          ActivatedForClientInDateIntervalWithLimit(
            clientId1,
            Iterable.empty,
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2020-01-02"),
            2,
            0
          )
        )
        .success
        .value

      result shouldBe List(offerId0, offerId2)
    }

    "select unique reports with filter by product" in {
      dao.insert(Iterable(records(1), records(2), records(3))).success

      val result = dao
        .getUniqueOfferIds(
          ActivatedForClientInDateIntervalWithLimit(
            clientId1,
            List("boost"),
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2019-01-05"),
            1,
            0
          )
        )
        .success
        .value

      result shouldBe List(offerId2)
    }

    "count unique reports" in {
      dao.insert(Iterable(records(2), records(3))).success

      val result = dao
        .countUniqueOffers(
          ActivatedForClientInDateInterval(
            clientId1,
            Iterable.empty,
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2020-01-02")
          )
        )
        .success
        .value

      result shouldBe 1
    }

    "count unique reports with filters" in {
      dao.insert(Iterable(records(2), records(3))).success

      val result = dao
        .countUniqueOffers(
          ActivatedForClientInDateInterval(
            clientId1,
            List("boost"),
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2019-01-05")
          )
        )
        .success
        .value

      result shouldBe 1
    }

    "select records with date interval include whole 'to' day" in {
      dao.insert(Iterable(records(4), records(5))).success

      val result = dao
        .countUniqueOffers(
          ActivatedForClientInDateInterval(
            clientId1,
            Nil,
            LocalDate.parse("2018-01-01"),
            LocalDate.parse("2019-01-02")
          )
        )
        .success
        .value

      result shouldBe 1
    }

    "select latest activation" in {
      dao.insert(Iterable(records(4), records(5))).success

      dao
        .latestActivation(
          clientId1,
          offerId0,
          boostProduct
        )
        .success
        .value shouldBe Some(records(5))
    }

    "select count rows between day" in {
      val clientId = 454
      val offerId = 2222
      val product = boostProduct
      val inputRows = Iterable(
        OfferWithPaidProduct(
          clientId,
          offerId,
          product,
          DateTime.parse("2021-11-05T23:59:58")
        ),
        OfferWithPaidProduct(
          clientId,
          offerId,
          product,
          DateTime.parse("2021-11-06T17:18:37")
        ),
        OfferWithPaidProduct(
          clientId,
          offerId,
          product,
          DateTime.parse("2021-11-07T00:00:00")
        )
      )
      dao.insert(inputRows).success

      dao
        .countActivationWithinDay(
          clientId,
          offerId,
          product,
          DateTime.parse("2021-11-06T15:18:37").toLocalDate
        )
        .success
        .value shouldBe 1L

      dao
        .countActivationWithinDay(
          clientId,
          offerId,
          product,
          DateTime.parse("2021-12-06T17:18:37").toLocalDate
        )
        .success
        .value shouldBe 0L
    }
  }

}

object OffersWithPaidProductsSalesmanDaoSpec {

  trait TestOffersWithPaidProductsSalesmanDao extends OffersWithPaidProductsSalesmanDao {

    def insert(items: Iterable[OfferWithPaidProduct]): Try[Unit]
  }
}
