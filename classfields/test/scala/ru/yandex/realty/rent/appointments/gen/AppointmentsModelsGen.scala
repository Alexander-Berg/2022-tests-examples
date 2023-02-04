package ru.yandex.realty.rent.appointments.gen

import java.time.{Instant, ZonedDateTime}

import org.scalacheck.Gen
import realty.palma.rent_appointments_employee.{
  AppointmentTypeNamespace,
  GeoPoint,
  RentAppointmentsEmployee,
  TransportationModeNamespace
}
import ru.yandex.realty.proto.model.SimpleGeoPoint
import ru.yandex.realty.rent.appointments.model.TimeInterval.ConsecutiveTimeIntervals
import ru.yandex.realty.rent.appointments.model.{
  Appointment,
  AppointmentStatus,
  AppointmentType,
  EmployeeOnDuty,
  TimeInterval
}
import ru.yandex.realty.rent.appointments.proto.inner_model.AppointmentData
import ru.yandex.vertis.generators.BasicGenerators

trait AppointmentsModelsGen extends BasicGenerators {

  def appointmentGen(): Gen[Appointment] =
    for {
      id <- readableString
      flatId <- readableString
      status <- Gen.oneOf(
        List(
          AppointmentStatus.Arranged,
          AppointmentStatus.Conducted,
          AppointmentStatus.Cancelled
        )
      )
      scheduledTime <- posNum[Long].map(Instant.ofEpochMilli)
      employeeLogin <- readableString
      appointmentType <- Gen.oneOf(List(AppointmentType.Photo))
      subjectFederationId <- posNum[Int]
      version <- posNum[Int]
      data <- appointmentDataGen()
    } yield Appointment(
      id = id,
      flatId = flatId,
      status = status,
      scheduledTime = scheduledTime,
      employeeLogin = employeeLogin,
      appointmentType = appointmentType,
      subjectFederationId = subjectFederationId,
      data = data,
      version = version
    )

  def appointmentDataGen(): Gen[AppointmentData] =
    for {
      appointmentAddress <- readableString
      comment <- readableString
      durationMinutes <- posNum[Int]
      latitude <- Gen.chooseNum(0.0f, 80.0f)
      longitude <- Gen.chooseNum(0.0f, 80.0f)
    } yield AppointmentData(
      appointmentAddress,
      Some(SimpleGeoPoint(latitude = latitude, longitude = longitude)),
      comment,
      durationMinutes
    )

  def consecutiveTimeIntervalsGen(): Gen[ConsecutiveTimeIntervals] =
    for {
      startInstant <- posNum[Long].map(Instant.ofEpochMilli)
      shifts <- Gen.listOf(Gen.chooseNum(1, 1000))
      intervals = shifts.grouped(2).foldLeft(Nil: List[TimeInterval]) {
        case (list, shift1 :: shift2 :: Nil) =>
          val start = list.headOption.map(_.to).getOrElse(startInstant)
          TimeInterval(start.plusSeconds(shift1), start.plusSeconds(shift1 + shift2)) :: list
        case (list, _) => list
      }
    } yield ConsecutiveTimeIntervals(intervals)

  def employeeOnDutyGen(): Gen[EmployeeOnDuty] =
    for {
      employeeLogin <- readableString
      timeIntervals <- consecutiveTimeIntervalsGen()
      appointmentTypes <- Gen.atLeastOne(List(AppointmentTypeNamespace.AppointmentType.PHOTO))
      transport <- Gen.oneOf(
        TransportationModeNamespace.TransportationMode.PUBLIC_TRANSPORT,
        TransportationModeNamespace.TransportationMode.CAR
      )
      latitude <- Gen.chooseNum(0, 80.0f)
      longitude <- Gen.chooseNum(0, 80.0f)
    } yield EmployeeOnDuty(
      staffLogin = employeeLogin,
      workingHours = timeIntervals,
      data = RentAppointmentsEmployee(
        login = employeeLogin,
        startingPoint = Some(GeoPoint(latitude, longitude)),
        appointmentTypes = appointmentTypes,
        transportationMode = transport
      )
    )
}
