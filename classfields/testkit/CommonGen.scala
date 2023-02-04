package ru.auto.comeback.model.testkit

import java.time.Instant
import java.time.temporal.ChronoUnit

import zio.random.Random
import ru.auto.api.api_offer_model.{Category, Section}
import ru.auto.api.comeback_model.Comeback.EventType
import ru.auto.api.comeback_model.ComebackListingRequest.Sorting
import ru.auto.api.request_model.RequestPagination
import zio.test.{Gen, Sized}

import scala.concurrent.duration._

/** Author: Vladislav Dolbilov (darl@yandex-team.ru)
  * Created: 22/01/2020
  */
object CommonGen {

  val anyFiniteDuration: Gen[Random, FiniteDuration] =
    Gen
      .long(0, Long.MaxValue)
      .map(_.nanos.toSeconds.seconds)

  val anyInstant: Gen[Random, Instant] =
    Gen
      .instant(
        Instant.parse("1990-01-01T00:00:00Z"),
        Instant.parse("2100-01-01T00:00:00Z")
      )
      .map(_.truncatedTo(ChronoUnit.MILLIS))

  val anyCategory: Gen[Random, Category] =
    Gen.elements(Category.CARS, Category.MOTO, Category.TRUCKS)

  val anySection: Gen[Random, Section] =
    Gen.elements(Section.NEW, Section.USED)

  val anyEventType: Gen[Random, EventType] =
    Gen.fromIterable(EventType.values)

  val anySorting: Gen[Random, Sorting] =
    Gen.elements(Sorting.COMEBACK_DURATION, Sorting.CREATION_DATE)

  val anyPagination: Gen[Random, RequestPagination] =
    Gen.int(1, 1000).zipWith(Gen.int(1, 1000))((page, pageSize) => RequestPagination.of(page, pageSize))

  val anyVinCode: Gen[Random with Sized, String] = Gen.alphaNumericString.map(_.take(17)).filter(_.nonEmpty)

  val anyYandexEmail: Gen[Random with Sized, String] = Gen.alphaNumericString.map(s => s"${s.take(6)}@yandex.ru")
}
