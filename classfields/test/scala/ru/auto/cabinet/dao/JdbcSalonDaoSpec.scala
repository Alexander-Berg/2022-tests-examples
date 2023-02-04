package ru.auto.cabinet.dao

import org.scalatest.flatspec.AsyncFlatSpec
import ru.auto.cabinet.dao.jdbc.{JdbcSalonDao, MarkDao}
import ru.auto.cabinet.model.dealer.{Dealership, Phone, Salon}
import ru.auto.cabinet.model.{ClientPoi, Mark}
import ru.auto.cabinet.service.dealer.SalonBuilderService
import ru.auto.cabinet.test.JdbcSpecTemplate

import java.time.Instant
import java.time.temporal.ChronoUnit

class JdbcSalonDaoSpec extends AsyncFlatSpec with JdbcSpecTemplate {

  private val dao =
    new JdbcSalonDao(
      office7Database,
      office7Database,
      poiDatabaseName = office7Handle.databaseName)

  private val marksDao =
    new MarkDao(office7Database, office7Database)

  val ts = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  val dealershipTs = Instant.parse("2019-06-18T14:27:04.578Z")

  val salonBuilder = new SalonBuilderService(dao, marksDao)

  val clientId = 105

  val salon = Salon(
    title = "",
    description = None,
    url = Some("http://best-test.ru"),
    phones = Set.empty,
    logo = "",
    photo = Set("img6.jpg"),
    rentCertificate = Set.empty,
    lessor = None,
    dealership = Set(
      Dealership(
        Mark(3, Some("TEST3"), "test3"),
        Set("img5.jpg"),
        dealershipTs
      )),
    id = 4,
    address = "address\", \"address\"",
    lat = 55.657055d,
    lng = 37.732887d,
    cityId = Some(1123),
    regionId = Some(87),
    countryId = Some(1),
    yaCityId = 213,
    yaRegionId = 1,
    yaCountryId = 225
  )

  val salonForUpdate = Salon(
    title = "salon1",
    description = Some("descr"),
    url = Some("www.ya.ru"),
    phones = Set(
      Phone(
        title = "manager",
        countryCode = "7",
        cityCode = "921",
        phone = "79211234567",
        localPhone = "1234567",
        phoneMask = "1:3:7",
        callFrom = 7,
        callTill = 22,
        extension = ""
      )),
    logo = "logo.jpg",
    photo = Set("photo.jpg"),
    rentCertificate = Set("rent.jpg"),
    lessor = Some("lessor"),
    dealership = Set(
      Dealership(
        Mark(3, Some("TEST3"), "test3"),
        Set("40553-certificate.jpg"),
        ts
      )),
    id = 4,
    address = "address salon",
    lat = 1.1d,
    lng = 2.2d,
    cityId = Some(123),
    regionId = Some(321),
    countryId = Some(111),
    yaCityId = 1,
    yaRegionId = 2,
    yaCountryId = 3
  )

  "JdbcSalonDao.getSalons" should "get salon" in {
    for {
      receivedSalon <- salonBuilder.getSalons(
        Set(ClientPoi(clientId, salonForUpdate.id)))
    } yield assert(salon == receivedSalon(clientId))
  }

  "JdbcSalonDao.updateSalon" should "update salon" in {
    for {
      _ <- dao.updateSalon(clientId, salonForUpdate)
      salon <- salonBuilder.getSalons(
        Set(ClientPoi(clientId, salonForUpdate.id)))
    } yield {
      salon(clientId) shouldEqual salonForUpdate
      salon(clientId).dealership.head.date shouldEqual dealershipTs
        .truncatedTo(ChronoUnit.SECONDS)
        .plusSeconds(1)
        .minus(3, ChronoUnit.HOURS)
    }
  }

}
