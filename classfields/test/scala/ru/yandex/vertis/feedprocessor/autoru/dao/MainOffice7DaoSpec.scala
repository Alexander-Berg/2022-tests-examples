package ru.yandex.vertis.feedprocessor.autoru.dao

import java.sql.{Connection, Statement}
import org.scalatest.{BeforeAndAfter, Inside, OptionValues}
import org.springframework.jdbc.core.PreparedStatementCreator
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.utils.OpsJdbc
import ru.yandex.vertis.feedprocessor.util.{DatabaseSpec, DummyOpsSupport}

class MainOffice7DaoSpec
  extends WordSpecBase
  with DatabaseSpec
  with BeforeAndAfter
  with Inside
  with OptionValues
  with DummyOpsSupport {
  implicit val opsJdbcMeters: OpsJdbc.Meters = new OpsJdbc.Meters(operationalSupport.prometheusRegistry)
  val mainOffice7Dao: MainOffice7Dao = new MainOffice7DaoImpl(tasksDb)

  before {
    tasksDb.master.jdbc.update("TRUNCATE clients")
  }

  private def createClient(clientId: Int, multipostingEnabled: Boolean) = {
    val sql =
      """
      INSERT INTO clients (id, multiposting_enabled) VALUES (?, ?)
      """
    tasksDb.master.jdbc.update(
      new PreparedStatementCreator {
        override def createPreparedStatement(con: Connection) = {
          val ps = con.prepareStatement(sql, Statement.NO_GENERATED_KEYS)
          ps.setInt(1, Int.box(clientId))
          ps.setInt(2, Int.box(if (multipostingEnabled) 1 else 0))
          ps
        }
      }
    )
  }

  "MainOfficeDao" should {
    "return multiposting_enabled status" in {
      createClient(1, multipostingEnabled = true)
      createClient(2, multipostingEnabled = false)

      mainOffice7Dao.isMultipostingEnabled(1).value shouldBe true
      mainOffice7Dao.isMultipostingEnabled(2).value shouldBe false
      mainOffice7Dao.isMultipostingEnabled(12345678) shouldBe None
    }
  }
}
