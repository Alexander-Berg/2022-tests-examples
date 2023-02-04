package ru.yandex.vos2.services.mds

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.BasicsModel.Photo
import ru.yandex.vos2.services.mds.MdsPhotoUtils.parseMDSPhoto

/**
  * Created by andrey on 3/31/17.
  */
@RunWith(classOf[JUnitRunner])
class MdsPhotoUtilsTest extends FunSuite {
  val realtyMdsUtils = MdsPhotoUtils("writeUrl", "readUrl", RealtyMdsSettings)

  test("missing group name is not accepted py parse") {
    val urlParsed =
      parseMDSPhoto("https://avatars.yandex.net/get-realty/add.3858273751401290741141866327448314666/orig")
    assert(urlParsed.isEmpty)
    val nameParsed = realtyMdsUtils.getMdsInfo("add.3858273751401290741141866327448314666-orig")
    assert(nameParsed.isEmpty)
    val nameParsedAgain = realtyMdsUtils.getMdsInfo("3858273751401290741141866327448314666-orig")
    assert(nameParsedAgain.isEmpty)
  }

  test("missing size name is not accepted py parse") {
    val urlParsed =
      parseMDSPhoto("https://avatars.yandex.net/get-realty/12345/add.3858273751401290741141866327448314666")
    assert(urlParsed.isEmpty)
    val nameParsed = realtyMdsUtils.getMdsInfo("12345-add.3858273751401290741141866327448314666")
    assert(nameParsed.contains(("12345", "add.3858273751401290741141866327448314666")))
  }

  test("valid url is parsed") {
    val urlParsed =
      parseMDSPhoto("https://avatars.yandex.net/get-realty/12345/add.3858273751401290741141866327448314666/orig")
    assert(urlParsed.contains("12345-add.3858273751401290741141866327448314666"))
    val nameParsed = realtyMdsUtils.getMdsInfo(urlParsed.get)
    assert(nameParsed.contains(("12345", "add.3858273751401290741141866327448314666")))
  }

  val autoruPanoramaMdsUtils = MdsPhotoUtils("writeUrl", "readUrl", AutoruPanoramaMdsSettings)

  test("autoru-panorama namespace has only orig size") {
    val res = autoruPanoramaMdsUtils.getUrls("group", "name")
    assert(res.size == 1)
    assert(res.head._1 == "orig")
  }
}
