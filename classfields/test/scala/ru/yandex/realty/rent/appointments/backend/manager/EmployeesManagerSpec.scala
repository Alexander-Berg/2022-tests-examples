package ru.yandex.realty.rent.appointments.backend.manager

import java.time.{LocalDate, LocalTime, OffsetDateTime, ZoneOffset}

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import realty.palma.rent_appointments_employee.RentAppointmentsEmployee
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.application.ng.palma.client.PalmaClient
import ru.yandex.realty.clients.abcduty2.AbcDuty2Client
import ru.yandex.realty.clients.abcduty2.model.ListShiftsResponse.{ShiftInfo, StaffInfo}
import ru.yandex.realty.clients.abcduty2.model.{ListShiftsRequest, ListShiftsResponse}
import ru.yandex.realty.model.region.Regions.MSK_AND_MOS_OBLAST
import ru.yandex.realty.rent.appointments.gen.AppointmentsModelsGen
import ru.yandex.realty.rent.appointments.model.TimeInterval.ConsecutiveTimeIntervals
import ru.yandex.realty.rent.appointments.model.{AppointmentType, EmployeeOnDuty, TimeInterval}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.util.TimeUtils

import scala.concurrent.Future
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class EmployeesManagerSpec extends AsyncSpecBase with AppointmentsModelsGen {

  val palmaClient = mock[PalmaClient[RentAppointmentsEmployee]]
  val abcDuty2Client = mock[AbcDuty2Client]

  implicit val traced: Traced = Traced.empty

  val employeesManager = new EmployeesManagerImpl(palmaClient, abcDuty2Client)

  private val MskOffset = ZoneOffset.ofHours(3)

  private val requestDate = LocalDate.of(2022, 6, 30)

  private val managerIdToShifts: Map[String, List[(OffsetDateTime, OffsetDateTime)]] = Map(
    "manager with every shift outside of the requested interval" ->
      List(
        (
          OffsetDateTime.of(requestDate.plusDays(1), LocalTime.of(1, 0), MskOffset),
          OffsetDateTime.of(requestDate.plusDays(1), LocalTime.of(12, 23), MskOffset)
        ),
        (
          OffsetDateTime.of(requestDate.minusDays(1), LocalTime.of(9, 12), MskOffset),
          OffsetDateTime.of(requestDate.minusDays(1), LocalTime.of(23, 59), MskOffset)
        )
      ),
    "manager with a big shift" ->
      List(
        (
          OffsetDateTime.of(requestDate.minusDays(10), LocalTime.of(12, 12), MskOffset),
          OffsetDateTime.of(requestDate.plusDays(1), LocalTime.of(21, 12), MskOffset)
        )
      ),
    "manager with overlapping shifts 1" ->
      List(
        (
          OffsetDateTime.of(requestDate, LocalTime.of(12, 0), MskOffset),
          OffsetDateTime.of(requestDate, LocalTime.of(15, 0), MskOffset)
        ),
        (
          OffsetDateTime.of(requestDate, LocalTime.of(13, 0), MskOffset),
          OffsetDateTime.of(requestDate, LocalTime.of(16, 0), MskOffset)
        )
      ),
    "manager with overlapping shifts 2" ->
      List(
        (
          OffsetDateTime.of(requestDate, LocalTime.of(12, 0), MskOffset),
          OffsetDateTime.of(requestDate, LocalTime.of(15, 0), MskOffset)
        ),
        (
          OffsetDateTime.of(requestDate, LocalTime.of(11, 0), MskOffset),
          OffsetDateTime.of(requestDate, LocalTime.of(17, 0), MskOffset)
        )
      ),
    "manager with empty shifts" ->
      List(
        (
          OffsetDateTime.of(requestDate, LocalTime.of(11, 0), MskOffset),
          OffsetDateTime.of(requestDate, LocalTime.of(11, 0), MskOffset)
        ),
        (
          OffsetDateTime.of(requestDate, LocalTime.of(12, 0), MskOffset),
          OffsetDateTime.of(requestDate, LocalTime.of(11, 55), MskOffset)
        ),
        (
          OffsetDateTime.of(requestDate, LocalTime.of(13, 0), MskOffset),
          OffsetDateTime.of(requestDate, LocalTime.of(14, 0), MskOffset)
        )
      ),
    "manager with multiple shifts" ->
      List(
        (
          OffsetDateTime.of(requestDate, LocalTime.of(11, 0), MskOffset),
          OffsetDateTime.of(requestDate, LocalTime.of(12, 0), MskOffset)
        ),
        (
          OffsetDateTime.of(requestDate, LocalTime.of(11, 30), MskOffset),
          OffsetDateTime.of(requestDate, LocalTime.of(14, 0), MskOffset)
        ),
        (
          OffsetDateTime.of(requestDate, LocalTime.of(15, 0), MskOffset),
          OffsetDateTime.of(requestDate.plusDays(1), LocalTime.of(15, 0), MskOffset)
        ),
        (
          OffsetDateTime.of(requestDate, LocalTime.of(8, 0), MskOffset),
          OffsetDateTime.of(requestDate, LocalTime.of(10, 20), MskOffset)
        )
      ),
    "employee without palma profile" ->
      List(
        (
          OffsetDateTime.of(requestDate, LocalTime.of(11, 0), MskOffset),
          OffsetDateTime.of(requestDate, LocalTime.of(12, 0), MskOffset)
        )
      )
  )
  private val abcResponse =
    ListShiftsResponse(
      for {
        (managerLogin, list) <- managerIdToShifts.toList
        (from, to) <- list
      } yield ShiftInfo(
        slotId = Random.nextInt(),
        start = from,
        end = to,
        scheduleId = 0,
        staff = StaffInfo(managerLogin, isRobot = false)
      )
    )

  val expectedResult = Set(
    expectedEmployeeOnDuty(
      "manager with a big shift",
      (requestDate.atStartOfDay().atOffset(MskOffset), requestDate.plusDays(1).atStartOfDay().atOffset(MskOffset))
    ),
    expectedEmployeeOnDuty(
      "manager with overlapping shifts 1",
      (
        OffsetDateTime.of(requestDate, LocalTime.of(12, 0), MskOffset),
        OffsetDateTime.of(requestDate, LocalTime.of(16, 0), MskOffset)
      )
    ),
    expectedEmployeeOnDuty(
      "manager with overlapping shifts 2",
      (
        OffsetDateTime.of(requestDate, LocalTime.of(11, 0), MskOffset),
        OffsetDateTime.of(requestDate, LocalTime.of(17, 0), MskOffset)
      )
    ),
    expectedEmployeeOnDuty(
      "manager with empty shifts",
      (
        OffsetDateTime.of(requestDate, LocalTime.of(13, 0), MskOffset),
        OffsetDateTime.of(requestDate, LocalTime.of(14, 0), MskOffset)
      )
    ),
    expectedEmployeeOnDuty(
      "manager with multiple shifts",
      (
        OffsetDateTime.of(requestDate, LocalTime.of(8, 0), MskOffset),
        OffsetDateTime.of(requestDate, LocalTime.of(10, 20), MskOffset)
      ),
      (
        OffsetDateTime.of(requestDate, LocalTime.of(11, 0), MskOffset),
        OffsetDateTime.of(requestDate, LocalTime.of(14, 0), MskOffset)
      ),
      (
        OffsetDateTime.of(requestDate, LocalTime.of(15, 0), MskOffset),
        OffsetDateTime.of(requestDate.plusDays(1), LocalTime.of(0, 0), MskOffset)
      )
    )
  )

  private def expectedEmployeeOnDuty(
    managerLogin: String,
    workingHours: (OffsetDateTime, OffsetDateTime)*
  ): EmployeeOnDuty =
    EmployeeOnDuty(
      staffLogin = managerLogin,
      workingHours = ConsecutiveTimeIntervals(
        workingHours.toList.map { case (from, to) => TimeInterval(from.toInstant, to.toInstant) }
      ),
      data = RentAppointmentsEmployee(managerLogin)
    )

  "EmployeesManagerImpl" should {
    "get current employees on duty and convert shifts to working hours correctly" in {
      (abcDuty2Client
        .listShifts(_: ListShiftsRequest)(_: Traced))
        .expects(where {
          case (request: ListShiftsRequest, _) =>
            request.startLess.forall(!_.isBefore(requestDate.plusDays(1).atStartOfDay(TimeUtils.MSK).toInstant)) &&
              request.endGreater.forall(!_.isAfter(requestDate.atStartOfDay(TimeUtils.MSK).toInstant))
        })
        .returning(Future.successful(abcResponse))

      (palmaClient
        .all(_: Traced))
        .expects(*)
        .returning(
          Future.successful(
            managerIdToShifts.keys.toList
              .filterNot(_ == "employee without palma profile")
              .map(login => RentAppointmentsEmployee(login = login))
          )
        )

      val result = employeesManager
        .getEmployeesOnDuty(MSK_AND_MOS_OBLAST, requestDate, None)
        .futureValue

      result.toSet shouldBe expectedResult
    }

    "return requested employees" in {

      val notRequestedEmployee = employeeOnDutyGen().next.data
      val requestedEmployee1 = employeeOnDutyGen().next.data
      val requestedEmployee2 = employeeOnDutyGen().next.data
      val notReturnedEmployee = employeeOnDutyGen().next.data

      (palmaClient
        .all(_: Traced))
        .expects(*)
        .returning(
          Future.successful(List(notRequestedEmployee, requestedEmployee1, requestedEmployee2))
        )

      val result = employeesManager
        .getEmployees(Set(requestedEmployee1.login, requestedEmployee2.login, notReturnedEmployee.login))
        .futureValue

      result.toSet shouldBe Set(requestedEmployee1, requestedEmployee2)
    }
  }
}
