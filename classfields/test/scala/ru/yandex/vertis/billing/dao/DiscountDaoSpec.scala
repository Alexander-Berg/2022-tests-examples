package ru.yandex.vertis.billing.dao

import org.joda.time.DateTime
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.DiscountDaoSpec._
import ru.yandex.vertis.billing.model_core._
import ru.yandex.vertis.billing.model_core.gens._
import ru.yandex.vertis.billing.util.DateTimeUtils.DateTimeAreOrdered

import scala.util.{Failure, Random, Success}

/**
  * Spec on [[DiscountDao]]
  *
  * @author ruslansd
  */
trait DiscountDaoSpec extends AnyWordSpec with Matchers {

  protected def dao: DiscountDao
  protected def customerDao: CustomerDao

  "DiscountDao" should {

    "successfully insert and get discounts" in {
      ownerHeaders.foreach(customerDao.create)
      dao.upsert(discounts) match {
        case Success(_) => info("Done")
        case Failure(e) =>
          e.printStackTrace()
          fail(s"Unexpected $e")
      }
      val time = discounts.head.effectiveSince.plusDays(10)

      owners.foreach { owner =>
        val result = dao.get(Iterable(owner), time).get
        result should contain theSameElementsAs expected(owner, discounts, time)
      }
    }
  }
}

object DiscountDaoSpec {
  val RowsNumber = 1000
  val CustomersNumber = 5
  val TargetsNumber = 5
  val random = new Random()

  val owners = CustomerIdGen.next(CustomersNumber)
  val ownerHeaders = owners.map(id => CustomerHeaderGen.next.copy(id = id))

  val targets = DiscountTargetGen.next(TargetsNumber)

  val discounts =
    deleteDuplicateKeys(DiscountGen.next(RowsNumber).map(withOwnerAndTarget))

  def withOwnerAndTarget(discount: Discount) =
    discount.copy(
      owner = getRandom(owners),
      target = getRandom(targets)
    )

  def deleteDuplicateKeys(discounts: Iterable[Discount]) =
    discounts.groupBy(d => (d.owner, d.source, d.target, d.effectiveSince)).map { case (tuple, group) =>
      group.head
    }

  def expected(owner: CustomerId, discounts: Iterable[Discount], time: DateTime) =
    discounts.filter(_.owner == owner).groupBy(d => (d.owner, d.source, d.target, DiscountType(d.value))).map {
      case (tuple, group) =>
        group
          .filter(d1 =>
            d1.effectiveSince.getMillis <= time.getMillis &&
              d1.owner == owner
          )
          .maxBy(_.effectiveSince)
    }

  def getRandom[T](collection: Iterable[T]): T =
    collection.toSeq(random.nextInt(collection.size))
}
