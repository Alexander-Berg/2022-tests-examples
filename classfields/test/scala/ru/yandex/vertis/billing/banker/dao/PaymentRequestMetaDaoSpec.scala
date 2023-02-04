package ru.yandex.vertis.billing.banker.dao

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.dao.util.CleanableDao
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds

/**
  * Specs on [[PaymentRequestMetaDao]]
  *
  * @author alex-kovalenko
  */
trait PaymentRequestMetaDaoSpec extends AnyWordSpec with Matchers with AsyncSpecBase with BeforeAndAfterEach {

  def dao: PaymentRequestMetaDao with CleanableDao

  override def beforeEach(): Unit = {
    dao.clean().futureValue
    super.beforeEach()
  }

  "PaymentRequestMetaDao" should {
    val psYk = PaymentSystemIds.YandexKassa
    val psRk = PaymentSystemIds.Robokassa
    val pr1 = "rq_1"
    val pr2 = "rq_2"

    "upsert meta" in {
      dao.setMeta(psYk, pr1, "k", "v").futureValue
      dao.getMeta(psYk, pr1).futureValue shouldBe Map("k" -> "v")

      dao.setMeta(psYk, pr1, "k", "w").futureValue
      dao.getMeta(psYk, pr1).futureValue shouldBe Map("k" -> "w")
    }

    "store meta for different requests" in {
      dao.setMeta(psYk, pr1, "k1", "v1").futureValue

      dao.getMeta(psYk, pr1).futureValue shouldBe Map("k1" -> "v1")
      dao.getMeta(psYk, pr2).futureValue shouldBe empty

      dao.setMeta(psYk, pr2, "k1", "v2").futureValue

      dao.getMeta(psYk, pr1).futureValue shouldBe Map("k1" -> "v1")
      dao.getMeta(psYk, pr2).futureValue shouldBe Map("k1" -> "v2")

      dao.setMeta(psYk, pr1, "k1", "v3").futureValue

      dao.getMeta(psYk, pr1).futureValue shouldBe Map("k1" -> "v3")
      dao.getMeta(psYk, pr2).futureValue shouldBe Map("k1" -> "v2")

      dao.setMeta(psYk, pr2, "k1", "v3").futureValue

      dao.getMeta(psYk, pr1).futureValue shouldBe Map("k1" -> "v3")
      dao.getMeta(psYk, pr2).futureValue shouldBe Map("k1" -> "v3")
    }

    "store meta for different payment systems" in {
      dao.setMeta(psYk, pr1, "k1", "v1").futureValue

      dao.getMeta(psYk, pr1).futureValue shouldBe Map("k1" -> "v1")
      dao.getMeta(psRk, pr1).futureValue shouldBe empty

      dao.setMeta(psRk, pr1, "k1", "v2").futureValue

      dao.getMeta(psYk, pr1).futureValue shouldBe Map("k1" -> "v1")
      dao.getMeta(psRk, pr1).futureValue shouldBe Map("k1" -> "v2")

      dao.setMeta(psYk, pr1, "k1", "v3").futureValue

      dao.getMeta(psYk, pr1).futureValue shouldBe Map("k1" -> "v3")
      dao.getMeta(psRk, pr1).futureValue shouldBe Map("k1" -> "v2")

      dao.setMeta(psRk, pr1, "k1", "v3").futureValue

      dao.getMeta(psYk, pr1).futureValue shouldBe Map("k1" -> "v3")
      dao.getMeta(psRk, pr1).futureValue shouldBe Map("k1" -> "v3")
    }
  }
}
