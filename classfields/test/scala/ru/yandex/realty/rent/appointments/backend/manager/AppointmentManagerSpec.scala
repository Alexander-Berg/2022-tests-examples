package ru.yandex.realty.rent.appointments.backend.manager

import java.time.{Instant, LocalDate, LocalTime, ZonedDateTime}

import org.junit.runner.RunWith
import org.scalatest.time.{Millis, Minutes, Span}
import org.scalatestplus.junit.JUnitRunner
import realty.palma.rent_appointments_employee.RentAppointmentsEmployee
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.palma.client.PalmaClient
import ru.yandex.realty.clients.rent.RentFlatServiceClient
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, TestContainerDatasource}
import ru.yandex.realty.doobie.{DoobieTestDatabase, StubDbMonitorFactory}
import ru.yandex.realty.errors.{InvalidParamsApiException, NotFoundApiException}
import ru.yandex.realty.model.region.Regions.MSK_AND_MOS_OBLAST
import ru.yandex.realty.proto.model.SimpleGeoPoint
import ru.yandex.realty.rent.appointments.backend.manager.AppointmentManager.{AppointmentPatch, NewAppointmentFields}
import ru.yandex.realty.rent.appointments.dao.AppointmentDaoImpl
import ru.yandex.realty.rent.appointments.gen.AppointmentsModelsGen
import ru.yandex.realty.rent.appointments.model.TimeInterval.ConsecutiveTimeIntervals
import ru.yandex.realty.rent.appointments.model._
import ru.yandex.realty.rent.proto.api.flats.Flat
import ru.yandex.realty.rent.proto.api.moderation.{FlatDetailedInfo, FlatRequestFeature}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.time.TimezoneUtils

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class AppointmentManagerSpec
  extends AsyncSpecBase
  with DoobieTestDatabase
  with AppointmentsModelsGen
  with MySQLTestContainer.V8_0
  with TestContainerDatasource {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))

  before {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/schema.sql")).futureValue
  }

  after {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/drop_tables.sql")).futureValue
  }

  val palmaClient = mock[PalmaClient[RentAppointmentsEmployee]]
  val employeesManager = mock[EmployeesManager]
  val availableSlotsManager = mock[AvailableSlotsManager]
  val rentFlatServiceClient = mock[RentFlatServiceClient]

  private val dao = new AppointmentDaoImpl(new StubDbMonitorFactory)

  implicit val traced: Traced = Traced.empty

  val appointmentManager =
    new AppointmentManagerImpl(
      palmaClient,
      employeesManager,
      availableSlotsManager,
      rentFlatServiceClient,
      doobieDatabase,
      dao
    )

  "AppointmentManagerImpl" should {
    "fail if flat does not exist" in {
      val flatId = "test-1"
      (rentFlatServiceClient
        .getModerationFlatByFlatId(_: String, _: Set[FlatRequestFeature])(_: Traced))
        .expects(flatId, Set.empty[FlatRequestFeature], *)
        .returning(Future.successful(None))

      val appointmentFields = NewAppointmentFields(
        flatId = flatId,
        scheduledTime = Instant.now(),
        employeeLogin = "zhukovrv",
        appointmentType = AppointmentType.Photo,
        comment = "hello"
      )
      appointmentManager.createAppointment(appointmentFields).failed.futureValue shouldBe a[NotFoundApiException]
    }

    "fail to create appointment if the time is not available" in {
      val flatId = "test-2"
      val employee = "zhukovrv"

      val date = LocalDate.of(2022, 7, 5)
      val time = LocalTime.of(12, 37)
      val instant =
        ZonedDateTime.of(date, time, TimezoneUtils.findBySubjectFederationId(MSK_AND_MOS_OBLAST).get).toInstant

      val flatAddress =
        Flat.Address.newBuilder().setSubjectFederationId(MSK_AND_MOS_OBLAST).setAddress("address string")
      (rentFlatServiceClient
        .getModerationFlatByFlatId(_: String, _: Set[FlatRequestFeature])(_: Traced))
        .expects(flatId, Set.empty[FlatRequestFeature], *)
        .returning(
          Future.successful(
            Some(FlatDetailedInfo.newBuilder().setFlat(Flat.newBuilder().setAddress(flatAddress)).build())
          )
        )

      val availableIntervals = List(
        TimeInterval(instant.minusSeconds(10), instant.minusSeconds(1)),
        TimeInterval(instant.plusSeconds(1), instant.plusSeconds(10))
      )
      (availableSlotsManager
        .getAvailableSlots(_: String, _: AppointmentType, _: LocalDate, _: List[String], _: String)(_: Traced))
        .expects(flatId, AppointmentType.Photo, date, *, employee, *)
        .returning(doobie.free.connection.pure(Map(employee -> ConsecutiveTimeIntervals(availableIntervals))))

      val appointmentFields = NewAppointmentFields(
        flatId = flatId,
        scheduledTime = instant,
        employeeLogin = employee,
        appointmentType = AppointmentType.Photo,
        comment = "hello"
      )
      appointmentManager.createAppointment(appointmentFields).failed.futureValue shouldBe a[InvalidParamsApiException]
    }

    "create and get appointment" in {
      val flatId = "test-3"
      val employee = "zhukovrv"
      val comment = "this is a comment"
      val address = "Москва, улица Льва Толстого, Красная Роза"
      val scalaPbGeoPoint = SimpleGeoPoint(81, 74)
      val javaGeoPoint = ru.yandex.realty.proto.SimpleGeoPoint.newBuilder().setLatitude(81).setLongitude(74)

      val date = LocalDate.of(2022, 7, 5)
      val time = LocalTime.of(12, 37)
      val instant =
        ZonedDateTime.of(date, time, TimezoneUtils.findBySubjectFederationId(MSK_AND_MOS_OBLAST).get).toInstant

      val flatAddress =
        Flat.Address
          .newBuilder()
          .setSubjectFederationId(MSK_AND_MOS_OBLAST)
          .setAddress(address)
          .setGeoPoint(javaGeoPoint)
      (rentFlatServiceClient
        .getModerationFlatByFlatId(_: String, _: Set[FlatRequestFeature])(_: Traced))
        .expects(flatId, Set.empty[FlatRequestFeature], *)
        .returning(
          Future.successful(
            Some(FlatDetailedInfo.newBuilder().setFlat(Flat.newBuilder().setAddress(flatAddress)).build())
          )
        )

      val availableIntervals = List(
        TimeInterval(instant.minusSeconds(10), instant.plusSeconds(10))
      )
      (availableSlotsManager
        .getAvailableSlots(_: String, _: AppointmentType, _: LocalDate, _: List[String], _: String)(_: Traced))
        .expects(flatId, AppointmentType.Photo, date, *, employee, *)
        .returning(doobie.free.connection.pure(Map(employee -> ConsecutiveTimeIntervals(availableIntervals))))

      val appointmentFields = NewAppointmentFields(
        flatId = flatId,
        scheduledTime = instant,
        employeeLogin = employee,
        appointmentType = AppointmentType.Photo,
        comment = comment
      )
      val createdAppointment = appointmentManager.createAppointment(appointmentFields).futureValue

      createdAppointment.copy(id = "1") shouldBe Appointment(
        id = "1",
        flatId = flatId,
        AppointmentStatus.Arranged,
        scheduledTime = instant,
        employeeLogin = employee,
        appointmentType = AppointmentType.Photo,
        subjectFederationId = MSK_AND_MOS_OBLAST,
        data = ru.yandex.realty.rent.appointments.proto.inner_model.AppointmentData(
          appointmentAddress = address,
          appointmentGeoPoint = Some(scalaPbGeoPoint),
          comment = comment,
          durationMinutes = AppointmentManagerImpl.DefaultAppointmentDuration.toMinutes.toInt
        ),
        version = 0
      )

      appointmentManager.getAppointment(createdAppointment.id).futureValue shouldBe createdAppointment
    }

    "fail to get an appointment if it doesn't exist" in {
      val appointmentId = "test-4"
      appointmentManager.getAppointment(appointmentId).failed.futureValue shouldBe a[NotFoundApiException]
    }

    "update an appointment without checking time" in {
      val date = LocalDate.of(2022, 7, 5)
      val time = LocalTime.of(12, 37)
      val instant =
        ZonedDateTime.of(date, time, TimezoneUtils.findBySubjectFederationId(MSK_AND_MOS_OBLAST).get).toInstant

      val appointment = appointmentGen().next.copy(
        status = AppointmentStatus.Arranged,
        scheduledTime = instant
      )

      doobieDatabase.masterTransaction(dao.createAppointment(appointment)(_)).futureValue

      appointmentManager
        .updateAppointment(appointment.id, AppointmentPatch(scheduledTime = None, Some(AppointmentStatus.Cancelled)))
        .futureValue shouldBe appointment.copy(status = AppointmentStatus.Cancelled, version = appointment.version + 1)
    }

    "update an appointment and validate new time" in {
      val date = LocalDate.of(2022, 7, 5)
      val time = LocalTime.of(12, 37)
      val instant =
        ZonedDateTime.of(date, time, TimezoneUtils.findBySubjectFederationId(MSK_AND_MOS_OBLAST).get).toInstant
      val timeAfterUpdate = instant.plusSeconds(5)

      val appointment = appointmentGen().next.copy(
        status = AppointmentStatus.Arranged,
        scheduledTime = instant,
        subjectFederationId = MSK_AND_MOS_OBLAST
      )

      doobieDatabase.masterTransaction(dao.createAppointment(appointment)(_)).futureValue

      val availableIntervals = List(
        TimeInterval(timeAfterUpdate.minusSeconds(10), timeAfterUpdate.plusSeconds(10))
      )
      (availableSlotsManager
        .getAvailableSlots(_: String, _: AppointmentType, _: LocalDate, _: List[String], _: String)(_: Traced))
        .expects(appointment.flatId, AppointmentType.Photo, date, *, appointment.employeeLogin, *)
        .returning(
          doobie.free.connection.pure(Map(appointment.employeeLogin -> ConsecutiveTimeIntervals(availableIntervals)))
        )

      val patch = AppointmentPatch(scheduledTime = Some(timeAfterUpdate), Some(AppointmentStatus.Cancelled))
      val expectedAppointment = appointment.copy(
        status = AppointmentStatus.Cancelled,
        scheduledTime = timeAfterUpdate,
        version = appointment.version + 1
      )
      appointmentManager.updateAppointment(appointment.id, patch).futureValue shouldBe expectedAppointment
    }

    "fail to update an appointment if the new time is incorrect" in {
      val date = LocalDate.of(2022, 7, 5)
      val time = LocalTime.of(12, 37)
      val instant =
        ZonedDateTime.of(date, time, TimezoneUtils.findBySubjectFederationId(MSK_AND_MOS_OBLAST).get).toInstant

      val appointment = appointmentGen().next.copy(
        status = AppointmentStatus.Arranged,
        subjectFederationId = MSK_AND_MOS_OBLAST
      )

      doobieDatabase.masterTransaction(dao.createAppointment(appointment)(_)).futureValue

      (availableSlotsManager
        .getAvailableSlots(_: String, _: AppointmentType, _: LocalDate, _: List[String], _: String)(_: Traced))
        .expects(appointment.flatId, AppointmentType.Photo, date, *, appointment.employeeLogin, *)
        .returning(
          doobie.free.connection.pure(Map(appointment.employeeLogin -> ConsecutiveTimeIntervals(Nil)))
        )

      val patch = AppointmentPatch(scheduledTime = Some(instant), Some(AppointmentStatus.Cancelled))
      appointmentManager
        .updateAppointment(appointment.id, patch)
        .failed
        .futureValue shouldBe a[InvalidParamsApiException]
    }

    "fail to update an appointment if it doesn't exist" in {
      val appointmentId = "test-6"
      appointmentManager
        .updateAppointment(appointmentId, AppointmentPatch(None, Some(AppointmentStatus.Cancelled)))
        .failed
        .futureValue shouldBe a[NotFoundApiException]
    }

    "list appointments for flat" in {
      val flatId = "abacaba"
      val appointments = appointmentGen().next(4).map(_.copy(flatId = flatId))

      appointments.foreach { appointment =>
        doobieDatabase.masterTransaction(dao.createAppointment(appointment)(_)).futureValue
      }

      appointmentManager
        .listAppointmentsForFlat(flatId)
        .futureValue
        .toSet shouldBe appointments.toSet
    }

    "get workload" in {
      val date = LocalDate.of(2022, 7, 5)
      val zoneId = TimezoneUtils.findBySubjectFederationId(MSK_AND_MOS_OBLAST).get

      val employeeWithMultipleAppointments = employeeOnDutyGen().next
      val employeeWithoutAppointments = employeeOnDutyGen().next
      val employeeNotOnDuty = employeeOnDutyGen().next
      val employeeNotInPalma = employeeOnDutyGen().next

      val appointments =
        List(
          appointmentGen().next.copy(
            employeeLogin = employeeNotOnDuty.data.login,
            status = AppointmentStatus.Arranged,
            scheduledTime = date.atStartOfDay(zoneId).minusHours(2).toInstant
          ), // should be skipped because employee scheduledTime is before the requested date
          appointmentGen().next.copy(
            employeeLogin = employeeNotOnDuty.data.login,
            status = AppointmentStatus.Conducted,
            scheduledTime = date.atStartOfDay(zoneId).plusHours(3).toInstant
          ), // should be present in the results
          appointmentGen().next.copy(
            employeeLogin = employeeNotInPalma.data.login,
            status = AppointmentStatus.Arranged,
            scheduledTime = date.atStartOfDay(zoneId).plusHours(8).toInstant
          ), // should be skipped because employee is not present in Palma
          appointmentGen().next.copy(
            employeeLogin = employeeWithMultipleAppointments.data.login,
            status = AppointmentStatus.Conducted,
            scheduledTime = date.atStartOfDay(zoneId).plusHours(13).toInstant
          ), // should be present in the results
          appointmentGen().next.copy(
            employeeLogin = employeeWithMultipleAppointments.data.login,
            status = AppointmentStatus.Cancelled,
            scheduledTime = date.atStartOfDay(zoneId).plusHours(18).toInstant
          ), // should be skipped because status is CANCELLED
          appointmentGen().next.copy(
            employeeLogin = employeeWithMultipleAppointments.data.login,
            status = AppointmentStatus.Arranged,
            scheduledTime = date.atStartOfDay(zoneId).plusHours(23).toInstant
          ), // should be present in the results
          appointmentGen().next.copy(
            employeeLogin = employeeWithMultipleAppointments.data.login,
            status = AppointmentStatus.Conducted,
            scheduledTime = date.atStartOfDay(zoneId).plusHours(28).toInstant
          ) // should be skipped because employee scheduledTime is after the requested date
        ).map(_.copy(subjectFederationId = MSK_AND_MOS_OBLAST))

      appointments.foreach { appointment =>
        doobieDatabase.masterTransaction(dao.createAppointment(appointment)(_)).futureValue
      }

      (employeesManager
        .getEmployeesOnDuty(_: Int, _: LocalDate, _: Option[AppointmentType])(_: Traced))
        .expects(MSK_AND_MOS_OBLAST, date, None, *)
        .returning(Future.successful(List(employeeWithMultipleAppointments, employeeWithoutAppointments)))

      (employeesManager
        .getEmployees(_: Set[String])(_: Traced))
        .expects(Set(employeeNotOnDuty.data.login, employeeNotInPalma.data.login), *)
        .returning(Future.successful(List(employeeNotOnDuty.data)))

      val expectedWorkload = List(
        EmployeeWorkload(
          employeeWithMultipleAppointments.data,
          employeeWithMultipleAppointments.workingHours,
          appointments = List(appointments(3), appointments(5))
        ),
        EmployeeWorkload(employeeNotOnDuty.data, ConsecutiveTimeIntervals(Nil), List(appointments(1))),
        EmployeeWorkload(employeeWithoutAppointments.data, employeeWithoutAppointments.workingHours, Nil)
      ).sortBy(_.employee.login)

      appointmentManager
        .getWorkload(MSK_AND_MOS_OBLAST, date)
        .futureValue shouldBe expectedWorkload
    }
  }
}
