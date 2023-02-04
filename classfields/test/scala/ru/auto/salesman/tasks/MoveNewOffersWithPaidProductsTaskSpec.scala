package ru.auto.salesman.tasks

import org.joda.time.DateTime
import ru.auto.salesman.Task
import ru.auto.salesman.dao.OffersWithPaidProductsSalesDao.{
  OfferWithPaidProduct,
  SalesServices,
  Table
}
import ru.auto.salesman.dao.OffersWithPaidProductsSalesmanDao.ClientProduct
import ru.auto.salesman.dao.{
  OffersWithPaidProductsSalesDao,
  OffersWithPaidProductsSalesmanDao
}
import ru.auto.salesman.service.{EpochService, UnfoldActivateDates}
import ru.auto.salesman.test.{BaseSpec, TestException}

class MoveNewOffersWithPaidProductsTaskSpec extends BaseSpec {

  private val newDao = mock[OffersWithPaidProductsSalesmanDao]
  private val oldDao = mock[OffersWithPaidProductsSalesDao]
  private val epochService = mock[EpochService]
  private val unfoldActivateDates = mock[UnfoldActivateDates]

  private val task =
    new MoveNewOffersWithPaidProductsTask(
      SalesServices,
      newDao,
      oldDao,
      unfoldActivateDates,
      epochService
    )

  "MoveNewOffersWithPaidProductsTask" should {
    "work successfully if epoch is present" in {
      val latestEpoch = DateTime.now()
      (epochService.getZ _)
        .expects(*)
        .returningZ(1L)
      (oldDao
        .scanNewRecordsZ(_: DateTime, _: Table)(
          _: Seq[OfferWithPaidProduct] => Task[Unit]
        ))
        .expects(new DateTime(1), SalesServices, *)
        .returningZ(latestEpoch)

      (epochService.setZ _)
        .expects("OffersWithPaidProductsSalesServices", latestEpoch.getMillis)
        .returningZ(())

      task.execute().success
    }

    "throw exception if unable to update epoch" in {
      val latestEpoch = DateTime.now()
      (epochService.getZ _)
        .expects(*)
        .returningZ(1L)
      (oldDao
        .scanNewRecordsZ(_: DateTime, _: Table)(
          _: Seq[OfferWithPaidProduct] => Task[Unit]
        ))
        .expects(new DateTime(1), SalesServices, *)
        .returningZ(latestEpoch)

      (epochService.setZ _)
        .expects("OffersWithPaidProductsSalesServices", latestEpoch.getMillis)
        .throwingZ(new TestException)

      task.execute().failed.get shouldBe a[TestException]
    }

    "throw exception if epoch is not present" in {
      (epochService.getZ _)
        .expects(*)
        .throwingZ(new NoSuchElementException())

      task
        .execute()
        .failure
        .exception shouldBe a[NoSuchElementException]
    }

    "fail if unable to receive epoch" in {
      (epochService.getZ _)
        .expects(*)
        .throwingZ(new TestException)

      task
        .execute()
        .failure
        .exception shouldBe a[TestException]
    }

    "fail if reading from table fails" in {
      (epochService.getZ _)
        .expects(*)
        .returningZ(1L)
      (oldDao
        .scanNewRecordsZ(_: DateTime, _: Table)(
          _: Seq[OfferWithPaidProduct] => Task[Unit]
        ))
        .expects(new DateTime(1L), SalesServices, *)
        .throwingZ(new TestException)
      (newDao
        .setActivateDates(_: ClientProduct)(_: List[DateTime]))
        .expects(*, *)
        .never()
      (epochService.setZ _).expects(*, *).never()

      task
        .execute()
        .failure
        .exception shouldBe a[TestException]
    }
  }
}
