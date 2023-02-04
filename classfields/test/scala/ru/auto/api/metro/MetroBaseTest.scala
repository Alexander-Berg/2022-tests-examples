package ru.auto.api.metro

import scala.jdk.CollectionConverters._
import org.scalatest.OptionValues
import ru.auto.api.util.Resources
import org.scalatest.matchers.should.Matchers._
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ApiOfferModel.MetroStation

/**
  * Created by mcsim-gr on 05.09.17.
  */
class MetroBaseTest extends AnyFunSuite with OptionValues {

  private val metroBase: MetroBase = Resources.open("/metros.json") {
    MetroBaseParser.parse
  }

  test("Get station") {
    val station = metroBase.station(20394).get

    station.names.ru shouldBe "Первомайская"
  }

  test("Get non exist station") {
    val station = metroBase.station(-1)

    station.isEmpty shouldBe true
  }

  test("Get line") {
    val line = metroBase.line("213_-64747829").get

    line.stationIds.size shouldBe 7
    line.names.ru shouldBe "Бутовская линия"
  }

  test("Get station lines") {
    val tretyakovskaya = metroBase.station(20468).get
    val lines = metroBase.stationLines(tretyakovskaya).toList

    lines.size shouldBe 2

    lines.map(_.id).toSet shouldBe Set("213_-2090624173", "213_-1236031499")

    val kalininskaya = lines.find(_.id == "213_-2090624173").head
    val kaluzhskoRizhskaya = lines.find(_.id == "213_-1236031499").head

    kalininskaya.names.ru shouldBe "Калининская линия"
    kalininskaya.color shouldBe "#ffdd03"

    kaluzhskoRizhskaya.names.ru shouldBe "Калужско-Рижская линия"
    kaluzhskoRizhskaya.color shouldBe "#ff7f00"
  }

  test("Enrich metro with lines") {

    val vodniyStadion = MetroStation.newBuilder().setRid(20370)
    val tretyakovskaya = MetroStation.newBuilder().setRid(20468)

    metroBase.enrichMetroWithLines(vodniyStadion).getLinesList.size() shouldBe 1
    metroBase.enrichMetroWithLines(vodniyStadion).getLinesList.asScala.head.getName shouldBe "Замоскворецкая линия"
    metroBase.enrichMetroWithLines(vodniyStadion).getLinesList.asScala.head.getColor shouldBe "#0a6f20"

    metroBase.enrichMetroWithLines(tretyakovskaya).getLinesList.size() shouldBe 2
    metroBase.enrichMetroWithLines(tretyakovskaya).getLinesList.asScala.head.getName shouldBe "Калининская линия"
    metroBase.enrichMetroWithLines(tretyakovskaya).getLinesList.asScala.head.getColor shouldBe "#ffdd03"
    metroBase.enrichMetroWithLines(tretyakovskaya).getLinesList.asScala.last.getName shouldBe "Калужско-Рижская линия"
    metroBase.enrichMetroWithLines(tretyakovskaya).getLinesList.asScala.last.getColor shouldBe "#ff7f00"
  }
}
