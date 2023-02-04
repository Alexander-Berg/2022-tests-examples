package ru.auto.cabinet.dao.jdbc

import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.time.{Seconds, Span}
import ru.auto.cabinet.test.JdbcSpecTemplate
import ru.auto.cabinet.ApiModel.CustomerDiscount.Product
import ru.auto.cabinet.ApiModel.CustomerType
import ru.auto.cabinet.dao.entities._
import ru.auto.cabinet.service.instr.{EmptyInstr, Instr}

import scala.concurrent.ExecutionContext

class CustomerDiscountDaoSpec extends AsyncWordSpec with JdbcSpecTemplate {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    Span(10, Seconds))
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  implicit val instr: Instr = new EmptyInstr("test")

  val customerDiscountDao =
    new CustomerDiscountDao(office7Database, office7Database)

  val clientId = 200L
  val companyId = 190
  val defaultPercent: Percent = Percent.unsafeFromInt(10)

  "CustomerDiscountDao" when {
    "discounts exist" should {
      "find discount for customer" in {
        customerDiscountDao
          .find(CustomerType.CLIENT, clientId, None)
          .futureValue
          .size shouldBe 2
      }

      "find discount for customer by product" in {
        customerDiscountDao
          .find(CustomerType.CLIENT, clientId, Some(Product.VAS))
          .futureValue
          .headOption
          .get
          .percent shouldBe defaultPercent
      }

      "update discount by customer and product" in {
        val percent: Percent = Percent.unsafeFromInt(20)

        customerDiscountDao
          .upsert(
            CustomerType.COMPANY_GROUP,
            companyId,
            Product.VAS,
            percent,
            10)
          .futureValue

        customerDiscountDao
          .find(CustomerType.COMPANY_GROUP, companyId, Some(Product.VAS))
          .futureValue
          .headOption
          .get
          .percent shouldBe percent

        customerDiscountDao
          .find(CustomerType.CLIENT, clientId, Some(Product.VAS))
          .futureValue
          .headOption
          .get
          .percent shouldBe percent

        customerDiscountDao
          .find(CustomerType.CLIENT, clientId, Some(Product.PLACEMENT))
          .futureValue
          .headOption
          .get
          .percent shouldBe defaultPercent

      }
    }

    "no discounts exist" when {
      "inserting client discounts" should {
        "insert discount for client only" in {
          val percent: Percent = Percent.unsafeFromInt(42)

          val newClientId = 202L

          customerDiscountDao
            .upsert(CustomerType.CLIENT, newClientId, Product.VAS, percent, 10)
            .futureValue

          customerDiscountDao
            .find(CustomerType.CLIENT, newClientId, None)
            .futureValue
            .headOption
            .get
            .percent shouldBe percent

          customerDiscountDao
            .find(CustomerType.COMPANY_GROUP, newClientId, None)
            .futureValue
            .length shouldBe 0
        }
      }

      "inserting company discounts" should {
        "insert discount for company and client" in {
          val percent: Percent = Percent.unsafeFromInt(43)

          val newCompanyId = 777L
          val newCompanyClientId = 333L

          customerDiscountDao
            .upsert(
              CustomerType.COMPANY_GROUP,
              newCompanyId,
              Product.VAS,
              percent,
              10)
            .futureValue

          customerDiscountDao
            .find(CustomerType.COMPANY_GROUP, newCompanyId, Some(Product.VAS))
            .futureValue
            .headOption
            .get
            .percent shouldBe percent

          customerDiscountDao
            .find(
              CustomerType.COMPANY_GROUP,
              newCompanyId,
              Some(Product.PLACEMENT))
            .futureValue
            .length shouldBe 0

          customerDiscountDao
            .find(CustomerType.CLIENT, newCompanyClientId, Some(Product.VAS))
            .futureValue
            .headOption
            .get
            .percent shouldBe percent

          customerDiscountDao
            .find(
              CustomerType.CLIENT,
              newCompanyClientId,
              Some(Product.PLACEMENT))
            .futureValue
            .length shouldBe 0
        }
      }

      "update company discounts without discount for a client" should {
        "update discount for company and create discounts for a client" in {
          val percent: Percent = Percent.unsafeFromInt(30)

          val companyWithoutClientDiscountId = 191L
          val clientWithoutDiscountId = 201L

          customerDiscountDao
            .upsert(
              CustomerType.COMPANY_GROUP,
              companyWithoutClientDiscountId,
              Product.VAS,
              percent,
              10)
            .futureValue

          customerDiscountDao
            .find(
              CustomerType.COMPANY_GROUP,
              companyWithoutClientDiscountId,
              Some(Product.VAS))
            .futureValue
            .headOption
            .get
            .percent shouldBe percent

          customerDiscountDao
            .find(
              CustomerType.CLIENT,
              clientWithoutDiscountId,
              Some(Product.VAS))
            .futureValue
            .headOption
            .get
            .percent shouldBe percent
        }
      }
    }

    "loyalty_update_pushed_to_kafka" should {
      "be FALSE on insert new record" in {
        val companyId = 999L
        val clientId = 555L

        customerDiscountDao
          .find(CustomerType.COMPANY_GROUP, companyId, None)
          .futureValue
          .length shouldBe 0

        customerDiscountDao
          .find(CustomerType.CLIENT, clientId, None)
          .futureValue
          .length shouldBe 0

        val percent: Percent = Percent.unsafeFromInt(50)

        customerDiscountDao
          .upsert(
            CustomerType.COMPANY_GROUP,
            companyId,
            Product.VAS,
            percent,
            10)
          .futureValue

        val companyRecord = customerDiscountDao
          .find(CustomerType.COMPANY_GROUP, companyId, None)
          .futureValue
          .headOption
          .get

        companyRecord.percent shouldBe Percent(50)
        companyRecord.loyaltyUpdatePushedToKafka shouldBe false

        val clientRecord = customerDiscountDao
          .find(CustomerType.CLIENT, clientId, None)
          .futureValue
          .headOption
          .get

        clientRecord.percent shouldBe Percent(50)
        companyRecord.loyaltyUpdatePushedToKafka shouldBe false
      }

      "be FALSE on update old records with flag value = 1" in {
        val companyId = 888L
        val clientId = 444L

        val oldCompanyRecord = customerDiscountDao
          .find(CustomerType.COMPANY_GROUP, companyId, None)
          .futureValue
          .headOption
          .get

        oldCompanyRecord.percent shouldBe defaultPercent
        oldCompanyRecord.loyaltyUpdatePushedToKafka shouldBe true

        val oldClientRecord = customerDiscountDao
          .find(CustomerType.CLIENT, clientId, None)
          .futureValue
          .headOption
          .get

        oldClientRecord.percent shouldBe defaultPercent
        oldClientRecord.loyaltyUpdatePushedToKafka shouldBe true

        val percent: Percent = Percent.unsafeFromInt(50)

        customerDiscountDao
          .upsert(
            CustomerType.COMPANY_GROUP,
            companyId,
            Product.VAS,
            percent,
            10)
          .futureValue

        val companyRecord = customerDiscountDao
          .find(CustomerType.COMPANY_GROUP, companyId, None)
          .futureValue
          .headOption
          .get

        companyRecord.percent shouldBe Percent(50)
        companyRecord.loyaltyUpdatePushedToKafka shouldBe false

        val clientRecord = customerDiscountDao
          .find(CustomerType.CLIENT, clientId, None)
          .futureValue
          .headOption
          .get

        clientRecord.percent shouldBe Percent(50)
        companyRecord.loyaltyUpdatePushedToKafka shouldBe false
      }

      "be FALSE on update old records with flag value = 0" in {
        val oldCompanyRecord = customerDiscountDao
          .find(CustomerType.COMPANY_GROUP, companyId, Some(Product.VAS))
          .futureValue
          .headOption
          .get

        oldCompanyRecord.percent shouldBe Percent(20)
        oldCompanyRecord.loyaltyUpdatePushedToKafka shouldBe false

        val oldClientRecord = customerDiscountDao
          .find(CustomerType.CLIENT, clientId, Some(Product.VAS))
          .futureValue
          .headOption
          .get

        oldClientRecord.percent shouldBe Percent(20)
        oldClientRecord.loyaltyUpdatePushedToKafka shouldBe false

        val percent: Percent = Percent.unsafeFromInt(50)

        customerDiscountDao
          .upsert(
            CustomerType.COMPANY_GROUP,
            companyId,
            Product.VAS,
            percent,
            10)
          .futureValue

        val companyRecord = customerDiscountDao
          .find(CustomerType.COMPANY_GROUP, companyId, Some(Product.VAS))
          .futureValue
          .headOption
          .get

        companyRecord.percent shouldBe Percent(50)
        companyRecord.loyaltyUpdatePushedToKafka shouldBe false

        val clientRecord = customerDiscountDao
          .find(CustomerType.CLIENT, clientId, Some(Product.VAS))
          .futureValue
          .headOption
          .get

        clientRecord.percent shouldBe Percent(50)
        companyRecord.loyaltyUpdatePushedToKafka shouldBe false
      }

      "be TRUE on mark record as pushed" in {
        val oldCompanyRecord = customerDiscountDao
          .find(CustomerType.COMPANY_GROUP, companyId, Some(Product.VAS))
          .futureValue
          .headOption
          .get

        oldCompanyRecord.loyaltyUpdatePushedToKafka shouldBe false

        val oldCompanyRecordWithId = customerDiscountDao
          .findActualDiscountWithId(
            CustomerType.COMPANY_GROUP,
            companyId,
            Product.VAS)
          .futureValue
          .get

        oldCompanyRecordWithId.discount.loyaltyUpdatePushedToKafka shouldBe false

        customerDiscountDao
          .markDiscountAsPushedToKafka(oldCompanyRecordWithId.recordId)
          .futureValue

        val updatedCompanyRecord = customerDiscountDao
          .find(CustomerType.COMPANY_GROUP, companyId, Some(Product.VAS))
          .futureValue
          .headOption
          .get

        updatedCompanyRecord.loyaltyUpdatePushedToKafka shouldBe true
      }
    }
  }

}
