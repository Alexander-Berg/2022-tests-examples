package ru.yandex.vertis.billing.banker.dao

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.TrustRecurrentDao.Record
import ru.yandex.vertis.billing.banker.dao.TrustRecurrentDaoSpec.{CardId, User}
import ru.yandex.vertis.billing.banker.dao.util.CleanableJdbcTrustRecurrentDao

trait TrustRecurrentDaoSpec
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with JdbcSpecTemplate
  with BeforeAndAfterEach {

  protected def recurrentDao: CleanableJdbcTrustRecurrentDao

  override def afterEach(): Unit = {
    recurrentDao.clean().toTry.get
  }

  "TrustRecurrentDao" should {

    "insert a record" in {
      val record = Record(user = User, cardId = CardId, isPreferred = true)

      recurrentDao.upsert(record).futureValue

      val inserted = recurrentDao.findByUserAndCardId(User, CardId).futureValue
      inserted.map(_.user) shouldBe Some(User)
      inserted.map(_.cardId) shouldBe Some(CardId)
      inserted.map(_.isPreferred) shouldBe Some(true)
      inserted.map(_.epoch) should not be empty
    }

    "update a record" in {
      val record = Record(user = User, cardId = CardId, isPreferred = true)
      recurrentDao.upsert(record).futureValue

      recurrentDao.upsert(record.copy(isPreferred = false))

      val updated = recurrentDao.findByUserAndCardId(User, CardId).futureValue
      updated.map(_.user) shouldBe Some(User)
      updated.map(_.cardId) shouldBe Some(CardId)
      updated.map(_.isPreferred) shouldBe Some(false)
      updated.map(_.epoch) should not be empty
    }
  }
}

object TrustRecurrentDaoSpec {
  val User = "user:4227"
  val CardId = "card-x75ab11a2dbdd17dba2e35443"
}
