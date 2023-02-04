package ru.yandex.vos2.autoru.utils

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vos2.BasicsModel.Photo.RecognizedNumber
import ru.yandex.vos2.autoru.InitTestDbs
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RecognizedLpUtilsTest extends AnyFunSuite with BeforeAndAfterAll with InitTestDbs {

  private val lpUtils = new RecognizedLpUtils(components.featuresManager.RecognizedLicensePlate)
  private val version = components.featuresManager.RecognizedLicensePlate.value.generation

  test("all lp is empty") {
    val plates = Seq(createRecognizedLp(), createRecognizedLp(), createRecognizedLp())
    assert(lpUtils.chooseLp(plates).getLicensePlate.isEmpty)
  }

  test("one non empty lp") {
    val plates = Seq(createRecognizedLp(), createRecognizedLp(), createRecognizedLp("А777МР777"))
    val result = lpUtils.chooseLp(plates)
    assert(result.getLicensePlate === "А777МР777")
    assert(result.getSourceHash === lpUtils.getHash(plates))
    assert(result.getVersion === version)
  }

  test("choose most frequent") {
    val mostFrequent = "В022ТХ777"
    val plates = Seq(
      createRecognizedLp(),
      createRecognizedLp("А777МР777"),
      createRecognizedLp("А777МР777"),
      createRecognizedLp("А123БВ45"),
      createRecognizedLp("Я987ЮЭ65"),
      createRecognizedLp(mostFrequent),
      createRecognizedLp(mostFrequent),
      createRecognizedLp(mostFrequent)
    )

    val result = lpUtils.chooseLp(plates)
    assert(result.getLicensePlate === mostFrequent)
    assert(result.getSourceHash === lpUtils.getHash(plates))
    assert(result.getVersion === version)
  }

  private def createRecognizedLp(lp: String = ""): RecognizedNumber = {
    RecognizedNumber
      .newBuilder()
      .setNumber(lp)
      .setConfidence(.8)
      .setWidthPercent(.1)
      .build()
  }

}
