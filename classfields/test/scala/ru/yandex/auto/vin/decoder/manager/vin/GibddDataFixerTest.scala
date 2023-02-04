package ru.yandex.auto.vin.decoder.manager.vin

import com.google.protobuf.util.JsonFormat
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.proto.VinHistory
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory

class GibddDataFixerTest extends AnyFunSuite {

  val fixer = new GibddDataFixer

  test("drop duplicate registration periods") {
    val b = VinInfoHistory.newBuilder()
    JsonFormat.parser().ignoringUnknownFields().merge(duplicatePeriodsJson, b)
    val res = fixer.fixRegistration(b.build())

    assert(res.getRegistration.getPeriodsCount === 4)
    assert(res.getStatus === VinInfoHistory.Status.OK)
  }

  test("drop duplicate accidents") {
    val b = VinInfoHistory.newBuilder()
    JsonFormat.parser().ignoringUnknownFields().merge(duplicateAccidentsJson, b)
    val res = fixer.fixAccidents(b.build())

    assert(res.getAccidentsCount === 2)
    assert(res.getStatus === VinInfoHistory.Status.OK)
  }

  test("convert legacy error status") {
    val data = VinInfoHistory.newBuilder().setAutocodeStatus(VinHistory.AutocodeStatus.AUTOCODE_ERROR)
    val res = fixer.fixWanted(data.build())

    assert(res.getStatus === VinInfoHistory.Status.ERROR)
  }

  test("convert legacy ok status") {
    val data = VinInfoHistory.newBuilder().setAutocodeStatus(VinHistory.AutocodeStatus.AUTOCODE_OK)
    val res = fixer.fixConstraints(data.build())

    assert(res.getStatus === VinInfoHistory.Status.OK)
  }

  private val duplicatePeriodsJson =
    """
      |{
      |  "vin": "XTA21074052158574",
      |  "status": "OK",
      |  "event_type": "AUTOCODE_REGISTRATION",
      |  "registration": {
      |    "pts": "63МА292270",
      |    "sts": "9907585889",
      |    "mark": "VAZ",
      |    "year": 2005,
      |    "color": "Синий",
      |    "model": "2107",
      |    "wheel": "LEFT",
      |    "periods": [
      |      {
      |        "from": "1547586000000",
      |        "owner": "PERSON"
      |      },
      |      {
      |        "to": "1547586000000",
      |        "from": "1421614800000",
      |        "owner": "PERSON"
      |      },
      |      {
      |        "to": "1421614800000",
      |        "from": "1412020800000",
      |        "owner": "PERSON"
      |      },
      |      {
      |        "to": "1412020800000",
      |        "from": "1122580800000",
      |        "owner": "PERSON"
      |      },
      |      {
      |        "from": "1547586000000",
      |        "owner": "PERSON"
      |      },
      |      {
      |        "to": "1547586000000",
      |        "from": "1421614800000",
      |        "owner": "PERSON"
      |      },
      |      {
      |        "to": "1421614800000",
      |        "from": "1412020800000",
      |        "owner": "PERSON"
      |      },
      |      {
      |        "to": "1412020800000",
      |        "from": "1122580800000",
      |        "owner": "PERSON"
      |      }
      |    ],
      |    "power_hp": 74,
      |    "gear_type": "Переднеприводной",
      |    "timestamp": "1558623869810",
      |    "displacement": 1568,
      |    "vehicle_type": "Легковые Автомобили Седан",
      |    "license_plate": "Е322ОН42",
      |    "raw_mark_model": "ВАЗ 21074"
      |  },
      |  "autocode_status": "AUTOCODE_OK"
      |}
    """.stripMargin

  private val duplicateAccidentsJson =
    """
      |{
      |  "vin": "WBAUE71090E005594",
      |  "status": "OK",
      |  "accidents": [
      |    {
      |      "date": "1511001600000",
      |      "mark": "Bmw",
      |      "year": 2007,
      |      "model": "Прочие Модели Bmw (Легковые)",
      |      "number": "030103030",
      |      "region": "Краснодарский Край",
      |      "accident_type": "Столкновение",
      |      "damage_points": [
      |        5
      |      ]
      |    },
      |    {
      |      "date": "1511001600000",
      |      "mark": "Bmw",
      |      "year": 2007,
      |      "model": "Прочие Модели Bmw (Легковые)",
      |      "number": "030103030",
      |      "region": "Краснодарский Край",
      |      "accident_type": "Столкновение",
      |      "damage_points": [
      |        5
      |      ]
      |    },
      |    {
      |      "date": "1711001600000",
      |      "mark": "Bmw",
      |      "year": 2007,
      |      "model": "Прочие Модели Bmw (Легковые)",
      |      "number": "130103030",
      |      "region": "Краснодарский Край",
      |      "accident_type": "Столкновение",
      |      "damage_points": [
      |        5, 6
      |      ]
      |    }
      |  ],
      |  "event_type": "AUTOCODE_ACCIDENT",
      |  "autocode_status": "AUTOCODE_OK"
      |}
    """.stripMargin
}
