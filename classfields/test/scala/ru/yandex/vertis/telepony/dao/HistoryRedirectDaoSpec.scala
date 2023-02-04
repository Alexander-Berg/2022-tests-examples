package ru.yandex.vertis.telepony.dao

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import ru.yandex.vertis.telepony.dao.jdbc.JdbcHistoryRedirectDaoV2
import ru.yandex.vertis.telepony.generator.Generator
import ru.yandex.vertis.telepony.generator.Generator.PhoneGen
import ru.yandex.vertis.telepony.generator.Producer._
import ru.yandex.vertis.telepony.model._
import ru.yandex.vertis.telepony.{DatabaseSpec, SpecBase}

/**
  * @author evans
  */
trait HistoryRedirectDaoSpec extends SpecBase with DatabaseSpec with BeforeAndAfterEach {

  def historyRedirectDao: HistoryRedirectDaoV2

  override protected def beforeEach(): Unit = {
    historyRedirectDao.clear().databaseValue.futureValue
    super.beforeEach()
  }

  "HistoryRedirectDao" should {
    val operatorNumber =
      OperatorNumber(
        PhoneGen.next,
        OperatorAccounts.MtsShared,
        Operators.Mts,
        2,
        PhoneTypes.Local,
        Status.Ready(None),
        None
      )
    val actualRedirect =
      Generator
        .generateRedirectV2(operatorNumber, PhoneGen.next)
        .next
        .copy(
          createTime = DateTime.now.minusMinutes(1)
        )
    val historyRedirect = JdbcHistoryRedirectDaoV2.asHistoryRedirect(actualRedirect)

    "create actual redirect" in {
      historyRedirectDao.start(actualRedirect).databaseValue.futureValue
    }

    "end redirect" in {
      //      todo date time should be provided outside
      historyRedirectDao.start(actualRedirect).databaseValue.futureValue
      historyRedirectDao.end(actualRedirect.id).databaseValue.futureValue
    }

    "get actual on time for ended redirect" in {
      historyRedirectDao.start(actualRedirect).databaseValue.futureValue
      historyRedirectDao.end(actualRedirect.id).databaseValue.futureValue

      val found =
        historyRedirectDao
          .getActual(
            actualRedirect.source.number,
            actualRedirect.createTime.plusSeconds(1)
          )
          .databaseValue
          .futureValue

      found.get.copy(endTime = None) shouldEqual historyRedirect
    }

    "get actual on time for actual redirect" in {
      historyRedirectDao.start(actualRedirect).databaseValue.futureValue

      val found =
        historyRedirectDao
          .getActual(
            actualRedirect.source.number,
            actualRedirect.createTime.plusHours(1)
          )
          .databaseValue
          .futureValue

      found shouldEqual Some(historyRedirect)
    }

    "fail if redirect ended" in {
      historyRedirectDao.start(actualRedirect).databaseValue.futureValue
      historyRedirectDao.end(actualRedirect.id).databaseValue.futureValue

      val found =
        historyRedirectDao
          .getActual(
            actualRedirect.source.number,
            actualRedirect.createTime.plusHours(1)
          )
          .databaseValue
          .futureValue

      found shouldEqual None
    }
  }

}
