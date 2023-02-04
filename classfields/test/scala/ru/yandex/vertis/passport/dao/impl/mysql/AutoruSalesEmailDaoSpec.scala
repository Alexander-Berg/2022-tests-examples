package ru.yandex.vertis.passport.dao.impl.mysql

import org.scalatest.WordSpec
import ru.yandex.vertis.passport.model.UserId
import ru.yandex.vertis.passport.test.Producer.generatorAsProducer
import ru.yandex.vertis.passport.test.{ModelGenerators, MySqlSupport, SpecBase}
import slick.jdbc.MySQLProfile.api._

import scala.util.Random

/**
  *
  * @author zvez
  */
class AutoruSalesEmailDaoSpec extends WordSpec with SpecBase with MySqlSupport {

  val dao = new AutoruSalesEmailDao(dbs.legacySalesAll)

  "AutoruSalesEmailDao.getUserEmails" should {

    "return Nil if nothing to return" in {
      val userId = ModelGenerators.userId.next
      dao.getUserEmails(userId).futureValue shouldBe Nil
    }

    "return emails" in {
      val userId = ModelGenerators.userId.next
      val emails = ModelGenerators.emailAddress.next(3)

      emails.foreach(insertSalesEmail(userId, _))

      dao.getUserEmails(userId).futureValue.map(_.email) should contain theSameElementsAs emails
    }
  }

  def insertSalesEmail(userId: UserId, email: String): Unit = {
    val saleId = Random.nextInt(10000)
    dbs.legacySalesAll
      .run(
        sqlu"""
        INSERT INTO emails_sales(email, sale_id, user_id, hash, create_date, update_date)
        VALUES($email, $saleId, $userId, "123", NOW(), NOW())
          """
      )
      .futureValue
  }

}
