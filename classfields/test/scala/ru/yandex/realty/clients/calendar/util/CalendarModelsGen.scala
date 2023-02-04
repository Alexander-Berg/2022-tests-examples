package ru.yandex.realty.clients.calendar.util

import org.scalacheck.Gen
import ru.yandex.realty.clients.calendar.model.{
  Action,
  CalendarNotification,
  Event,
  EventOrganizer,
  EventType,
  Person,
  Repetition,
  RepetitionType,
  Resource
}
import ru.yandex.vertis.generators.{BasicGenerators, DateTimeGenerators}

import java.time.{LocalDate, LocalDateTime}

trait CalendarModelsGen extends BasicGenerators with DateTimeGenerators {

  def personGen: Gen[Person] = {
    for {
      name <- readableString
      email <- readableString
      decision <- readableString
    } yield Person(name, email, decision)
  }

  def resourceGen: Gen[Resource] = {
    for {
      officeId <- posNum[Int]
      name <- readableString
      email <- readableString
    } yield Resource(officeId, name, email)
  }

  def calendarNotificationGen: Gen[CalendarNotification] = {
    for {
      channel <- readableString
      offset <- readableString
    } yield CalendarNotification(channel, offset)
  }

  def actionGen: Gen[Action] = {
    for {
      accept <- bool
      reject <- bool
      delete <- bool
      attach <- bool
      detach <- bool
      edit <- bool
      invite <- bool
      move <- bool
      changeOrganizer <- bool
    } yield Action(
      accept = accept,
      reject = reject,
      delete = delete,
      attach = attach,
      detach = detach,
      edit = edit,
      invite = invite,
      move = move,
      changeOrganizer = changeOrganizer
    )
  }

  //scalastyle:off
  def eventGen: Gen[Event] =
    for {
      id <- posNum[Long]
      externalId <- readableString
      sequence <- posNum[Int]
      name <- readableString
      description <- readableString
      location <- readableString
      descriptionHtml <- readableString
      locationHtml <- readableString
      startTs = LocalDateTime.now()
      endTs = startTs.plusMinutes(30)
      instanceStartTs = startTs
      person = EventOrganizer(
        uid = 10015L,
        name = "some-name",
        email = "some@email.com",
        login = "some-login",
        officeId = Some(100),
        decision = Some("some decision")
      )
      attendees = personGen.next(5).toSeq
      resources = resourceGen.next(3).toSeq
      notifications = calendarNotificationGen.next(5).toSeq
      repetition = Repetition(RepetitionType.Daily, each = 1, dueDate = Some(LocalDate.now()))
      layerId <- posNum[Int]
      isOnPrimaryLayer <- readableString
      primaryLayerClosed <- bool
      organizerLetToEditAnyMeeting <- bool
      canAdminAllResources <- bool
      repetitionNeedsConfirmation <- bool
      actions = actionGen.next(5).toSeq
      conferenceUrl = "localhost"
    } yield Event(
      id = id,
      externalId = externalId,
      sequence = sequence,
      `type` = EventType.Learning,
      name = name,
      description = description,
      location = location,
      descriptionHtml = descriptionHtml,
      locationHtml = locationHtml,
      startTs = startTs,
      endTs = endTs,
      instanceStartTs = instanceStartTs,
      isAllDay = false,
      isRecurrence = false,
      organizer = Some(person), // TODO
      actions = Action(),
      layerId = layerId,
      conferenceUrl = conferenceUrl
    )
  //scalastyle:on
}
