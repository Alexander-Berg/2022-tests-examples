package ru.yandex.auto.vin.decoder.manager.vin

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.carsharing.model.MarkModel
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Carsharing, Owner, Registration, RegistrationEvent}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters.IterableHasAsJava

class CarsharingDataBuilderTest extends AnyFunSuite with MockitoSupport {

  private val vin = VinCode("SHHEP33504U202567")

  private val yaDriveCarsharing = (
    EventType.YANDEX_DRIVE_CARS,
    Carsharing
      .newBuilder()
      .setDate(123L)
      .setOperator("Яндекс.Драйв")
      .build()
  )

  private val yaDriveExternalCarsharing = (
    EventType.YANDEX_DRIVE_EXTERNAL_CARSHARING,
    Carsharing
      .newBuilder()
      .setDate(123L)
      .setOperator("Делимобиль")
      .build()
  )

  private val udakovCarsharing = (
    EventType.CARSHARING_INFO,
    Carsharing
      .newBuilder()
      .setRawCompanyName("BelkaCar")
      .build()
  )

  private val popularMarkModel = MarkModel("BMW", "3ER")

  private def buildRegistration(
      mark: String,
      model: String,
      year: Int,
      periods: List[(String, Long, Long)],
      regActions: List[(String, Long, Long, String, String)]): Registration = {
    Registration
      .newBuilder()
      .setMark(mark)
      .setModel(model)
      .setYear(year)
      .addAllPeriods {
        periods
          .map(p => {
            RegistrationEvent.newBuilder().setOwner(p._1).setFrom(p._2).setTo(p._3).build()
          })
          .asJava
      }
      .addAllRegActions(
        regActions.map { r =>
          val ownerInfo = Owner.newBuilder().setName(r._4).setInn(r._5).build()
          RegistrationEvent.newBuilder().setOwner(r._1).setFrom(r._2).setTo(r._3).setOwnerInfo(ownerInfo).build()
        }.asJava
      )
      .build()
  }

  private val RegistrationWithLegalAndPopularMarkModel: Registration = buildRegistration(
    popularMarkModel.mark,
    popularMarkModel.model,
    2014,
    List(("LEGAL", 100, 200)),
    Nil
  )

  private val RegistrationWithOutdatedPopularMarkModel: Registration = buildRegistration(
    popularMarkModel.mark,
    popularMarkModel.model,
    2012,
    List(("LEGAL", 100, 200)),
    Nil
  )

  private val RegistrationWithLegalAndNoPopularMark: Registration = buildRegistration(
    "LADA",
    "LARGUS",
    2015,
    List(("LEGAL", 100, 200)),
    Nil
  )

  private val RegistrationWithoutLegal: Registration = buildRegistration(
    popularMarkModel.mark,
    popularMarkModel.model,
    2015,
    List(("PRIVATE", 100, 200)),
    Nil
  )

  private val RegistrationWithCarsharingCompanyInn: Registration = buildRegistration(
    popularMarkModel.mark,
    popularMarkModel.model,
    2015,
    List(("LEGAL", 100, 200)),
    List(("LEGAL", 100, 200, "ООО \"ЯНДЕКС.ДРАЙВ\"", "7704448440"))
  )

  private val RegistrationWithCarsharingCompanyInnAndNoRealPeriods: Registration = buildRegistration(
    popularMarkModel.mark,
    popularMarkModel.model,
    2015,
    Nil,
    List(("LEGAL", 100, 200, "ООО \"ЯНДЕКС.ДРАЙВ\"", "7704448440"))
  )

  test("no reg data") {
    val res = CarsharingDataBuilder.buildCarsharing(
      None,
      List(udakovCarsharing, yaDriveCarsharing),
      Map.empty,
      vin
    )

    assert(res.isEmpty === true)
  }

  test("reg data without legal") {
    val res = CarsharingDataBuilder.buildCarsharing(
      Some(RegistrationWithoutLegal),
      List(udakovCarsharing, yaDriveCarsharing),
      Map.empty,
      vin
    )

    assert(res.isEmpty === true)
  }

  test("registration without popular mark, no udakov, no drive data") {
    val res = CarsharingDataBuilder.buildCarsharing(
      Some(RegistrationWithLegalAndNoPopularMark),
      List.empty,
      Map.empty,
      vin
    )

    assert(res.isEmpty === true)
  }

  test("registration with popular mark, no udakov, no drive data") {
    val res = CarsharingDataBuilder.buildCarsharing(
      Some(RegistrationWithLegalAndPopularMarkModel),
      List.empty,
      Map.empty,
      vin
    )

    assert(res.isEmpty === true)
  }

  test("registration without popular mark, has drive data") {
    val res = CarsharingDataBuilder.buildCarsharing(
      Some(RegistrationWithLegalAndNoPopularMark),
      List(yaDriveCarsharing),
      Map.empty,
      vin
    )

    assert(res.isEmpty === false)
    assert(res.get.company === Some("Яндекс.Драйв"))
    assert(res.get.from === 100)
    assert(res.get.to === 200)
  }

  test("registration without popular mark, has external drive data") {
    val res = CarsharingDataBuilder.buildCarsharing(
      Some(RegistrationWithLegalAndNoPopularMark),
      List(yaDriveExternalCarsharing),
      Map.empty,
      vin
    )

    assert(res.isEmpty === false)
    assert(res.get.company === Some("Делимобиль"))
    assert(res.get.from === 100)
    assert(res.get.to === 200)
  }

  test("registration with popular mark, has udakov data") {
    val res = CarsharingDataBuilder.buildCarsharing(
      Some(RegistrationWithLegalAndPopularMarkModel),
      List(udakovCarsharing),
      Map.empty,
      vin
    )

    assert(res.isEmpty === false)
    assert(res.get.company === Some("BelkaCar"))
    assert(res.get.from === 100)
    assert(res.get.to === 200)
  }

  test("registration without popular mark, has udakov data") {
    val res = CarsharingDataBuilder.buildCarsharing(
      Some(RegistrationWithLegalAndNoPopularMark),
      List(udakovCarsharing),
      Map.empty,
      vin
    )

    assert(res.isEmpty === true)
  }

  test("outdated popular mark should't be trusted ") {
    val res = CarsharingDataBuilder.buildCarsharing(
      Some(RegistrationWithOutdatedPopularMarkModel),
      List(udakovCarsharing),
      Map.empty,
      vin
    )

    assert(res.isEmpty === true)
  }

  test("registration with carsharing company INN with no carsharing entities") {
    val res = CarsharingDataBuilder.buildCarsharing(
      Some(RegistrationWithCarsharingCompanyInn),
      Nil,
      Map.empty,
      vin
    )

    assert(res.isEmpty === false)
    assert(res.get.company === Some("Яндекс.Драйв"))
    assert(res.get.from === 100)
    assert(res.get.to === 200)
  }

  test("registration with carsharing company INN with empty real periods") {
    val res = CarsharingDataBuilder.buildCarsharing(
      Some(RegistrationWithCarsharingCompanyInnAndNoRealPeriods),
      Nil,
      Map.empty,
      vin
    )

    assert(res.isEmpty === true)
  }
}
