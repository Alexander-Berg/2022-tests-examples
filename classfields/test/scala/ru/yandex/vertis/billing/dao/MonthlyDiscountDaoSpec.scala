package ru.yandex.vertis.billing.dao

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.model_core.gens.CustomerIdGen
import ru.yandex.vertis.billing.model_core.gens.Producer
import ru.yandex.vertis.billing.util.DateTimeUtils

import scala.util.Success

/**
  * @author ruslansd
  */
trait MonthlyDiscountDaoSpec extends AnyWordSpec with Matchers {

  protected def dao: MonthlyDiscountDao

  private val record = MonthlyDiscountDao.Record(
    CustomerIdGen.next,
    DateTimeUtils.now()
  )

  "MonthlyDiscountDao" should {

    "store direct customers" in {
      val customer = CustomerIdGen.suchThat(_.agencyId.isEmpty).next
      val directCustomerRecord = record.copy(customer = customer)
      (dao.store(directCustomerRecord) should be).a(Symbol("Success"))

      dao.get(MonthlyDiscountDao.ForCustomer(customer)) shouldBe Success(Iterable(directCustomerRecord))

    }

    "store agency client customers" in {
      val customer = CustomerIdGen.suchThat(_.agencyId.nonEmpty).next
      val agencyClientCustomerRecord = record.copy(customer = customer)
      (dao.store(agencyClientCustomerRecord) should be).a(Symbol("Success"))

      dao.get(MonthlyDiscountDao.ForCustomer(customer)) shouldBe Success(Iterable(agencyClientCustomerRecord))

    }

    "store agency customers" in {
      val customer = record.customer.copy(agencyId = Some(record.customer.clientId))
      val agencyRecord = record.copy(customer = customer)
      (dao.store(agencyRecord) should be).a(Symbol("Success"))

      dao.get(MonthlyDiscountDao.ForCustomer(customer)) shouldBe Success(Iterable(agencyRecord))

    }

  }

}
