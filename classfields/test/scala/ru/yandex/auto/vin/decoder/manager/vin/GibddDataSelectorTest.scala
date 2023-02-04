package ru.yandex.auto.vin.decoder.manager.vin

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.offer.VosOffers
import ru.yandex.auto.vin.decoder.manager.vin.adaperio.AdaperioDataSelector
import ru.yandex.auto.vin.decoder.manager.vin.autocode.AutocodeDataSelector
import ru.yandex.auto.vin.decoder.manager.vin.checkburo.CheckburoDataSelector
import ru.yandex.auto.vin.decoder.manager.vin.megaparser.MegaParserDataSelector
import ru.yandex.auto.vin.decoder.manager.vin.mos.autocode.MosAutocodeDataSelector
import ru.yandex.auto.vin.decoder.manager.vin.scrapinghub.ScrapinghubDataSelector
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.model.gibdd.GibddDataPart
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared

class GibddDataSelectorTest extends AnyFunSuite {

  test("get latest") {
    val autocode = VinInfoHistory.newBuilder().setEventType(EventType.AUTOCODE_ACCIDENT).build()
    val adaperio = VinInfoHistory.newBuilder().setEventType(EventType.ADAPERIO_ACCIDENT).build()
    val megaParser = VinInfoHistory.newBuilder().setEventType(EventType.MEGA_PARSER_GIBDD_ACCIDENTS).build()

    val res = selector.getLatest(
      List(
        Prepared.simulate(megaParser, 3),
        Prepared.simulate(autocode, 1),
        Prepared.simulate(adaperio, 2)
      )
    )

    assert(res.get.data.getEventType === EventType.MEGA_PARSER_GIBDD_ACCIDENTS)
  }

  test("get latest registration if no fresh data") {
    val autocode = buildRegistration(EventType.AUTOCODE_REGISTRATION, 2, 3L)
    val adaperio = buildRegistration(EventType.ADAPERIO_REGISTRATION, 1, 2L)
    val mosAutocode = buildRegistration(EventType.AUTOCODE_MOS_REGISTRATION, 5, 1L)

    val res = selector.getRegistration(
      List(
        Prepared.simulate(autocode, 3),
        Prepared.simulate(adaperio, 2),
        Prepared.simulate(mosAutocode, 1)
      )
    )

    assert(res.get.data === autocode)
  }

  test("get registration with max periods if there is several fresh providers") {
    val now = System.currentTimeMillis()

    val autocode = buildRegistration(EventType.AUTOCODE_REGISTRATION, 2, now - 1)
    val adaperio = buildRegistration(EventType.ADAPERIO_REGISTRATION, 3, now - 2)

    val res = selector.getRegistration(
      List(
        Prepared.simulate(autocode, now - 1),
        Prepared.simulate(adaperio, now - 3)
      )
    )

    assert(res.get.data === adaperio)
  }

  test("get accidents with max accidents then deleted accidents feature is enabled") {
    val autocode = buildAccident(EventType.AUTOCODE_ACCIDENT, 2)
    val adaperio = buildAccident(EventType.ADAPERIO_ACCIDENT, 1)
    val scrapinghub = buildAccident(EventType.MEGA_PARSER_GIBDD_ACCIDENTS, 5)

    val res = selector.getAccidents(
      List(
        Prepared.simulate(autocode, 3),
        Prepared.simulate(adaperio, 2),
        Prepared.simulate(scrapinghub, 1)
      )
    )

    assert(res.get.data === scrapinghub)
  }

  test("get providers data") {
    val scrapinghubAccidents = buildAccident(EventType.MEGA_PARSER_GIBDD_ACCIDENTS, 2)
    val autocodeAccidents = buildAccident(EventType.AUTOCODE_MAIN, 2)
    val adaperioAccidents = buildAccident(EventType.ADAPERIO_ACCIDENT, 1)

    val rawStorageData = List(
      EventType.MEGA_PARSER_GIBDD_ACCIDENTS -> List(Prepared(3, 3, 3, scrapinghubAccidents, "")),
      EventType.AUTOCODE_MAIN -> List(Prepared(1, 1, 1, autocodeAccidents, "")),
      EventType.ADAPERIO_ACCIDENT -> List(Prepared(2, 2, 2, adaperioAccidents, ""))
    ).toMap

    val vin = VinCode("X4XEV18403EE30824")
    val vinData = VinData(vin, Map.empty, rawStorageData, VosOffers.Empty)

    val res = selector.getProvidersData(
      vinData,
      GibddDataPart.Accidents
    )

    assert(res.size === 3)
    assert(res.contains(Prepared.simulate(scrapinghubAccidents, 3)))
    assert(res.contains(Prepared.simulate(autocodeAccidents, 1)))
    assert(res.contains(Prepared.simulate(adaperioAccidents, 2)))
  }

  test("When registration data records are fresh and having same periods count then fresher record will be chosen") {
    val now = System.currentTimeMillis()
    val autocode = buildRegistration(EventType.AUTOCODE_REGISTRATION, 1, now - 2L)
    val adaperio = buildRegistration(EventType.ADAPERIO_REGISTRATION, 1, now - 1L) // Самый свежий
    val mosAutocode = buildRegistration(EventType.AUTOCODE_MOS_REGISTRATION, 1, now - 3L)

    val providers = List(autocode, adaperio, mosAutocode).map { p =>
      Prepared.simulate(p, p.getRegistration.getTimestamp)
    }
    // Проверим все перестановки, чтобы не сломаться от неожиданного порядка провайдеров во входном массиве
    providers.permutations.foreach { p =>
      val res = selector.getRegistration(p)
      assert(res.get.data === adaperio)
    }
  }

  private lazy val fixer = new GibddDataFixer

  private lazy val autocodeDataSelector = new AutocodeDataSelector
  private lazy val adaperioDataSelector = new AdaperioDataSelector
  private lazy val megaParserDataSelector = new MegaParserDataSelector
  private lazy val shDataSelector = new ScrapinghubDataSelector
  private lazy val mosAutocodeDataSelector = new MosAutocodeDataSelector
  private lazy val checkburoDataSelector = new CheckburoDataSelector

  private lazy val selector =
    new GibddDataSelector(
      fixer,
      autocodeDataSelector,
      adaperioDataSelector,
      megaParserDataSelector,
      shDataSelector,
      mosAutocodeDataSelector,
      checkburoDataSelector
    )

  private def buildRegistration(eventType: EventType, count: Int, timestamp: Long) = {
    val builder = VinInfoHistory
      .newBuilder()
      .setEventType(eventType)
    builder.getRegistrationBuilder.setTimestamp(timestamp)

    (0 to count).foreach(p => {
      val period = builder.getRegistrationBuilder.addPeriodsBuilder()
      period.setFrom(p).setTo(p + 1).setOwner("PERSON")
    })

    builder.getStatusesBuilder.setRegistrationStatus(VinInfoHistory.Status.OK)
    builder.build()
  }

  private def buildAccident(eventType: EventType, count: Int) = {
    val builder = VinInfoHistory
      .newBuilder()
      .setEventType(eventType)

    (0 to count).foreach(p => {
      val accident = builder.addAccidentsBuilder()
      accident.setDate(p)
    })

    builder.getStatusesBuilder.setAccidentsStatus(VinInfoHistory.Status.OK)
    builder.build()
  }
}
