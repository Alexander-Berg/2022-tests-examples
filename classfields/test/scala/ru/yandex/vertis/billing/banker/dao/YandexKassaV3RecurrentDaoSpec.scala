package ru.yandex.vertis.billing.banker.dao

import org.joda.time.LocalDate
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.YandexKassaRecurrentDao.{All, ForUser, Record}
import ru.yandex.vertis.billing.banker.dao.util.CleanableJdbcYandexKassaV3RecurrentDao
import ru.yandex.vertis.billing.banker.model.User
import ru.yandex.vertis.billing.banker.model.gens.{readableString, CardGen, Producer}
import ru.yandex.vertis.billing.banker.util.DateTimeUtils

/**
  * Spec on [[YandexKassaV3RecurrentDao]]
  *
  * @author ruslansd
  */
trait YandexKassaV3RecurrentDaoSpec
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with JdbcSpecTemplate
  with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    recurrents.clean().toTry.get
    super.beforeEach()
  }

  protected def recurrents: CleanableJdbcYandexKassaV3RecurrentDao

  val invoiceIdGen: Gen[Long] = Gen.posNum[Long]
  val customer = "1"
  val otherCustomer = "2"
  val cddPanMask = "123|456"
  val otherCddPanMask = "456|789"

  def recordGen(
      user: User = customer,
      expireAt: LocalDate = DateTimeUtils.now().plusMonths(1).toLocalDate,
      isEnabled: Boolean = true,
      isPreferred: Boolean = false): Gen[Record] =
    for {
      invoice <- readableString()
      mask <- CardGen
    } yield Record(
      user,
      invoice,
      mask,
      Some(expireAt),
      isEnabled,
      isPreferred
    )

  "YandexKassaV3RecurrentDao" should {
    "start with empty" in {
      recurrents.get(All).futureValue.isEmpty shouldBe true
      recurrents.get(ForUser(customer)).futureValue.isEmpty shouldBe true
    }
    "do not upsert same invoice" in {
      val recordFirst = recordGen().next
      val first = recurrents.upsert(recordFirst).futureValue
      first.copy(epoch = None) shouldBe recordFirst
      first.epoch should not be empty
      recurrents.get(All).futureValue should not be empty
      recurrents.get(ForUser(customer)).futureValue should (have size 1 and contain(first))

      val recordSecond = recordGen().next
        .copy(cddPanMask = recordFirst.cddPanMask, baseInvoiceId = first.baseInvoiceId)
      recurrents.upsert(recordSecond).futureValue
      recurrents.get(ForUser(customer)).futureValue should contain theSameElementsAs Iterable(first)
    }

    "update if invoice changed" in {
      val recordFirst = recordGen().next
      val recordSecond = recordGen().next.copy(cddPanMask = recordFirst.cddPanMask)
      val second = recurrents.upsert(recordSecond).futureValue

      recurrents.get(ForUser(customer)).futureValue should contain theSameElementsAs Iterable(second)
    }

    "insert different masks" in {
      val records = recordGen().next(5)
      records.foreach { rr =>
        recurrents.upsert(rr).futureValue.copy(epoch = None) shouldBe rr
      }

      recurrents.get(All).futureValue.map(_.copy(epoch = None)) should contain theSameElementsAs records
    }

    "return recurrent methods in correct order" in {
      val first = recordGen().next
      val second = recordGen().next

      recurrents.upsert(first).futureValue
      recurrents.upsert(second).futureValue

      recurrents.get(ForUser(customer)).futureValue.map(_.copy(epoch = None)) shouldBe Seq(second, first)

    }

    "update if changed expire date" in {
      val first = recordGen().next
      val second = first.copy(
        baseInvoiceId = "new_one_invoice",
        expireAt = Some(DateTimeUtils.now().toLocalDate.plusYears(1))
      )

      recurrents.upsert(first).futureValue
      recurrents.upsert(second).futureValue

      recurrents.get(ForUser(customer)).futureValue.map(_.copy(epoch = None)) shouldBe Seq(second)

    }

  }

}
