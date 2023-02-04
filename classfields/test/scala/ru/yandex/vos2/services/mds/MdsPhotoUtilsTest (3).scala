package ru.yandex.vos2.services.mds

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.BasicsModel.Photo
import ru.yandex.vos2.services.mds.MdsPhotoUtils.parseMDSPhoto

/**
  * Created by andrey on 3/31/17.
  */
@RunWith(classOf[JUnitRunner])
class MdsPhotoUtilsTest extends AnyFunSuite {
  val autoruAllMdsUtils = MdsPhotoUtils("writeUrl", "readUrl", AutoruAllNamespaceSettings)

  test("missing group name is not accepted py parse") {
    val urlParsed =
      parseMDSPhoto("https://avatars.yandex.net/get-autoru-all/3858273751401290741141866327448314666/orig")
    assert(urlParsed.isEmpty)
  }

  test("missing size name is not accepted py parse") {
    val urlParsed =
      parseMDSPhoto("https://avatars.yandex.net/get-autoru-vos/12345/3858273751401290741141866327448314666")
    assert(urlParsed.isEmpty)
    val nameParsed = autoruAllMdsUtils.getMdsInfo("12345-3858273751401290741141866327448314666")
    assert(nameParsed.contains(("12345", "3858273751401290741141866327448314666")))
  }

  test("valid url is parsed") {
    val urlParsed =
      parseMDSPhoto("https://avatars.yandex.net/get-autoru-vos/12345/3858273751401290741141866327448314666/orig")
    assert(urlParsed.contains("12345-3858273751401290741141866327448314666"))
    val nameParsed = autoruAllMdsUtils.getMdsInfo(urlParsed.get)
    assert(nameParsed.contains(("12345", "3858273751401290741141866327448314666")))
  }

  val autoruMdsUtils = new MdsPhotoUtils(
    Map(
      AutoruAllNamespaceSettings.namespace -> AutoruAllNamespaceSettings,
      AutoruVosNamespaceSettings.namespace -> AutoruVosNamespaceSettings,
      AutoruOrigNamespaceSettings.namespace -> AutoruOrigNamespaceSettings
    ),
    AutoruAllNamespaceSettings,
    "https://avatars.yandex.net",
    "https://avatars-int.yandex.net"
  )

  test("Correct mds info, more than one possible namespace") {
    val photoAutoruVos = createPhoto("123-abc", "autoru-vos", "456-def", "autoru-orig")
    val photoAutoruOrig = createPhoto("123-abc", "autoru-orig", "456-def", "autoru-orig")
    val photoAutoruAll = createPhoto("123-abc", "autoru-all", "456-def", "autoru-all")

    assert(autoruMdsUtils.getMdsPhotoInfo(photoAutoruVos).get._1 == "123")
    assert(autoruMdsUtils.getMdsPhotoInfo(photoAutoruVos).get._2 == "abc")
    assert(autoruMdsUtils.getOrigMdsPhotoInfo(photoAutoruVos).get._1 == "456")
    assert(autoruMdsUtils.getOrigMdsPhotoInfo(photoAutoruVos).get._2 == "def")

    assert(autoruMdsUtils.getMdsPhotoInfo(photoAutoruOrig).get._1 == "123")
    assert(autoruMdsUtils.getMdsPhotoInfo(photoAutoruOrig).get._2 == "abc")
    assert(autoruMdsUtils.getOrigMdsPhotoInfo(photoAutoruOrig).get._1 == "456")
    assert(autoruMdsUtils.getOrigMdsPhotoInfo(photoAutoruOrig).get._2 == "def")

    assert(autoruMdsUtils.getMdsPhotoInfo(photoAutoruAll).get._1 == "123")
    assert(autoruMdsUtils.getMdsPhotoInfo(photoAutoruAll).get._2 == "abc")
    assert(autoruMdsUtils.getOrigMdsPhotoInfo(photoAutoruAll).get._1 == "456")
    assert(autoruMdsUtils.getOrigMdsPhotoInfo(photoAutoruAll).get._2 == "def")
  }

  test("Correct main url, more than one possible namespace") {
    val photoAutoruVos = createPhoto("123-abc", "autoru-vos", "456-def", "autoru-orig")
    val photoAutoruOrig = createPhoto("123-abc", "autoru-orig", "456-def", "autoru-orig")
    val photoAutoruAll = createPhoto("123-abc", "autoru-all", "456-def", "autoru-all")

    assert(
      autoruMdsUtils.getMainPhotoUrl(photoAutoruVos).get ==
        "https://avatars.yandex.net/get-autoru-vos/123/abc/1200x900"
    )
    assert(
      autoruMdsUtils.getMainPhotoUrl(photoAutoruVos).get ==
        autoruMdsUtils.getMainUrl("autoru-vos", "123", "abc")
    )

    assert(
      autoruMdsUtils.getMainPhotoUrl(photoAutoruOrig).get ==
        "https://avatars-int.yandex.net/get-autoru-orig/123/abc/orig"
    )
    assert(
      autoruMdsUtils.getMainPhotoUrl(photoAutoruOrig).get ==
        autoruMdsUtils.getMainUrl("autoru-orig", "123", "abc")
    )

    assert(
      autoruMdsUtils.getMainPhotoUrl(photoAutoruAll).get ==
        "https://avatars.yandex.net/get-autoru-all/123/abc/1200x900"
    )
    assert(
      autoruMdsUtils.getMainPhotoUrl(photoAutoruAll).get ==
        autoruMdsUtils.getMainUrl("autoru-all", "123", "abc")
    )
  }

  val autoruPanoramaMdsUtils = MdsPhotoUtils("writeUrl", "readUrl", AutoruPanoramaNamespaceSettings)

  test("autoru-panorama namespace has only orig size") {
    val res =
      autoruPanoramaMdsUtils.getSizes(AutoruPanoramaNamespaceSettings.namespace, "group", "name", isOrig = false)
    assert(res.size == 1)
    assert(res.head._1 == "orig")
  }

  test("correct original photo urls") {
    val photoAutoruVos = createPhoto("123-abc", "autoru-vos", "456-def", "autoru-orig")
    assert(
      autoruMdsUtils.getOrigSizes(photoAutoruVos) ==
        Map(
          "1200x900" -> "https://avatars-int.yandex.net/get-autoru-orig/456/def/1200x900",
          "orig" -> "https://avatars-int.yandex.net/get-autoru-orig/456/def/orig"
        )
    )
  }

  private def createPhoto(name: String, namespace: String, origName: String, orignNamespace: String) = {
    Photo
      .newBuilder()
      .setCreated(123)
      .setName(name)
      .setNamespace(namespace)
      .setOrigName(origName)
      .setOrigNamespace(orignNamespace)
      .setIsMain(false)
      .setOrder(2)
      .build()
  }
}
