package ru.yandex.realty.rent.appointments.dao

import java.time.Instant
import org.junit.runner.RunWith
import org.scalatest.time.{Millis, Minutes, Span}
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.db.mysql.DuplicateRecordException
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, TestContainerDatasource}
import ru.yandex.realty.doobie.{DoobieTestDatabase, StubDbMonitorFactory}
import ru.yandex.realty.rent.appointments.gen.AppointmentsModelsGen
import ru.yandex.realty.rent.appointments.model.AppointmentStatus
import ru.yandex.realty.rent.appointments.model.AppointmentStatus.{Arranged, Cancelled, Conducted}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.AsyncSpecBase

@RunWith(classOf[JUnitRunner])
class AppointmentDaoImplSpec
  extends WordSpecLike
  with AsyncSpecBase
  with MySQLTestContainer.V8_0
  with TestContainerDatasource
  with BeforeAndAfter
  with Matchers
  with DoobieTestDatabase
  with AppointmentsModelsGen {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))

  before {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/schema.sql")).futureValue
  }

  after {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/drop_tables.sql")).futureValue
  }

  private val dao = new AppointmentDaoImpl(new StubDbMonitorFactory)

  implicit val trace: Traced = Traced.empty

  "AppointmentDaoImpl" should {
    "create and get appointment" in {
      val appointment = appointmentGen().next
      doobieDatabase.masterTransaction {
        dao.createAppointment(appointment)(_)
      }.futureValue
      val appointmentFromDb = doobieDatabase.masterTransaction {
        dao.getAppointment(appointment.id, forUpdate = false)(_)
      }.futureValue
      appointmentFromDb shouldBe Some(appointment)
    }

    "update appointment" in {
      val appointment = appointmentGen().next
      doobieDatabase.masterTransaction {
        dao.createAppointment(appointment)(_)
      }.futureValue

      val appointmentUpdate = appointmentGen().next
      val appointmentUpdateWithCorrectId = appointmentUpdate.copy(id = appointment.id)

      doobieDatabase.masterTransaction {
        dao.updateAppointment(appointment.id, appointmentUpdate)(_)
      }.futureValue

      val appointmentFromDb = doobieDatabase.masterTransaction {
        dao.getAppointment(appointment.id, forUpdate = true)(_)
      }.futureValue
      appointmentFromDb shouldBe Some(appointmentUpdateWithCorrectId)
    }

    "return NONE if the appointment does not exist" in {
      val appointmentId = appointmentGen().next.id

      val appointmentFromDb = doobieDatabase.masterTransaction {
        dao.getAppointment(appointmentId, forUpdate = false)(_)
      }.futureValue
      appointmentFromDb shouldBe None
    }

    "throw exception if the inserted appointment_id already exists" in {
      val appointment = appointmentGen().next
      doobieDatabase.masterTransaction {
        dao.createAppointment(appointment)(_)
      }.futureValue

      val appointmentWithSameId = appointmentGen().next.copy(id = appointment.id)
      doobieDatabase
        .masterTransaction {
          dao.createAppointment(appointmentWithSameId)(_)
        }
        .failed
        .futureValue shouldBe a[DuplicateRecordException]
    }

    "throw exception if the updated row does not exist" in {
      val appointment = appointmentGen().next
      doobieDatabase
        .masterTransaction {
          dao.updateAppointment(appointment.id, appointment)(_)
        }
        .failed
        .futureValue shouldBe a[NoSuchElementException]
    }

    "list all appointments for flat" in {
      val appointmentForFlat1 = appointmentGen().next
      val appointmentForFlat2 = appointmentGen().next.copy(flatId = appointmentForFlat1.flatId)
      val appointmentForAnotherFlat = appointmentGen().next

      List(appointmentForFlat1, appointmentForFlat2, appointmentForAnotherFlat).foreach { appointment =>
        doobieDatabase.masterTransaction(dao.createAppointment(appointment)(_)).futureValue
      }

      val appointmentsInDb = doobieDatabase.masterTransaction {
        dao.listAppointmentsForFlat(appointmentForFlat1.flatId)(_)
      }.futureValue
      appointmentsInDb.toSet shouldBe Set(appointmentForFlat1, appointmentForFlat2)
    }

    "list appointments in time range" in {
      def appointment(subjectFederationId: Int, scheduledTime: Instant, status: AppointmentStatus) =
        appointmentGen().next
          .copy(subjectFederationId = subjectFederationId, scheduledTime = scheduledTime, status = status)

      val targetRegionId = 2
      val wrongRegionId = 213
      val timeFrom = Instant.ofEpochSecond(100)
      val timeTo = Instant.ofEpochSecond(200)

      val appointmentFrom = appointment(targetRegionId, timeFrom, Arranged)
      val appointmentBetween = appointment(targetRegionId, Instant.ofEpochSecond(150), Conducted)
      val appointmentTo = appointment(targetRegionId, timeTo, Conducted)

      val appointmentBefore = appointment(targetRegionId, timeFrom.minusMillis(1), Arranged)
      val appointmentAfter = appointment(targetRegionId, timeTo.plusMillis(1), Conducted)

      val appointmentWrongRegion = appointment(wrongRegionId, Instant.ofEpochSecond(150), Conducted)

      val appointmentWrongStatus = appointment(targetRegionId, Instant.ofEpochSecond(150), Cancelled)

      List(
        appointmentFrom,
        appointmentBetween,
        appointmentTo,
        appointmentBefore,
        appointmentAfter,
        appointmentWrongRegion,
        appointmentWrongStatus
      ).foreach { appointment =>
        doobieDatabase.masterTransaction(dao.createAppointment(appointment)(_)).futureValue
      }

      val searchWithRegion = doobieDatabase.masterTransaction {
        dao.listScheduledAppointments(Some(targetRegionId), timeFrom, timeTo)(_)
      }.futureValue
      searchWithRegion.toSet shouldBe Set(appointmentFrom, appointmentBetween, appointmentTo)

      val searchWithoutRegion = doobieDatabase.masterTransaction {
        dao.listScheduledAppointments(None, timeFrom, timeTo)(_)
      }.futureValue
      searchWithoutRegion.toSet shouldBe Set(appointmentFrom, appointmentBetween, appointmentTo, appointmentWrongRegion)
    }
  }
}
