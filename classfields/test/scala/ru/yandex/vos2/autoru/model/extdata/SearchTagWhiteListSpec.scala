package ru.yandex.vos2.autoru.model.extdata

import java.io.ByteArrayInputStream

import com.google.common.base.Charsets
import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vos2.autoru.utils.docker.DockerAutoruCoreComponents

@RunWith(classOf[JUnitRunner])
class SearchTagWhiteListSpec extends AnyFunSuite {
  lazy val components = DockerAutoruCoreComponents

  test("should parse from cached eds data") {
    val wl = SearchTagWhiteList.from(components.extDataEngine)
    assert(!wl.allowed.isEmpty)
  }

  val json =
    """[
      |{
      |      "version" : 4,
      |      "name" : "search_tag_white_list",
      |      "flushDate" : "2019-03-01T10:59:39.205Z",
      |      "fullName" : "/auto_ru/common/search_tag_white_list",
      |      "content" : {
      |         "allowed" : [
      |            "test1",
      |            "test2",
      |            "pivo"
      |         ]
      |      },
      |      "mime" : "application/json; charset=us-ascii"
      |   }
      |]
    """.stripMargin

  test("parse from custom input") {
    val is = new ByteArrayInputStream(json.getBytes(Charsets.UTF_8))
    val wl = SearchTagWhiteList.parse(is)
    assert(wl.allowed.size == 3)
    assert(wl.allowed("pivo"))
    assert(wl.allowed("test1"))
    assert(wl.allowed("test2"))
    assert(!wl.allowed("test3"))
  }

}
