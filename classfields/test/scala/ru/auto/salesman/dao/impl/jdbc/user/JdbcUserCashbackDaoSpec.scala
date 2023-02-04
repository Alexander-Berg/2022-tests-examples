package ru.auto.salesman.dao.impl.jdbc.user

import doobie.util.update.Update
import org.scalacheck.Gen
import ru.auto.salesman.dao.user.UserCashbackDao
import ru.auto.salesman.dao.user.UserCashbackDao.Record
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.template.SalesmanUserJdbcSpecTemplate

class JdbcUserCashbackDaoSpec extends BaseSpec with SalesmanUserJdbcSpecTemplate {

  def dao = new JdbcUserCashbackDao(transactor, transactor)

  "list" should {
    val givenAmount = 1000L

    "return empty list if there are no user cashback records" in {
      clean()

      dao
        .list(Seq.empty)
        .success
        .value shouldBe empty
    }

    "return list of user cashback records" in {
      forAll(Gen.listOfN(3, recordGen(givenAmount))) { generatedRecords =>
        clean()

        val givenRecords = (1 to generatedRecords.size)
          .zip(generatedRecords)
          .map { case (id, generatedRecord) => generatedRecord.copy(id = id) }

        insertRecords(givenRecords)

        dao
          .list(Seq(UserCashbackDao.Filter.ForAmount(givenAmount)))
          .success
          .value shouldBe givenRecords
      }
    }
  }

  import doobie.implicits._
  import ru.auto.salesman.dao.impl.jdbc.database.doobie.Transactor._

  private def clean() = {
    val deleteSql =
      sql"""
         delete from user_cashback;
         """

    deleteSql.update.run
      .transact(transactor)
      .unit
      .success
  }

  private def insertRecords(records: Seq[Record]) = {
    val insertSql =
      """
      insert into user_cashback(id, user, amount, feature_id, spent_amount, epoch)
      values (?, ?, ?, ?, ?, from_unixtime(?))
      """

    Update[Record](insertSql)
      .updateMany(records.toList)
      .transact(transactor)
      .unit
      .success
  }

  private def recordGen(amount: Long): Gen[Record] =
    for {
      user <- Gen.alphaStr
      featureId <- Gen.alphaStr
      spentAmount <- Gen.posNum[Long]
      epoch <- Gen.chooseNum(10000L, 1000000L)
    } yield
      Record(
        0L,
        user.take(15),
        amount,
        featureId.take(63),
        spentAmount,
        epoch
      )
}
