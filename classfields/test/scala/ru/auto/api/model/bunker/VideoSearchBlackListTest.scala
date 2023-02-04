package ru.auto.api.model.bunker

import java.io.ByteArrayInputStream

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.testkit.TestDataEngine

class VideoSearchBlackListTest extends AnyFunSuite {

  test("parse VideoSearchBlacklist from test json") {
    val in = new ByteArrayInputStream("""[{
        |"fullName": "/auto_ru/common/catalog_video_blacklist",
        |"content": ["alpina", "brabus", "ac"]
        |}]""".stripMargin.getBytes("UTF-8"))
    val videoSearchBlacklist: VideoSearchBlackList = VideoSearchBlackList.parse(in)
    assert(videoSearchBlacklist.marks == Set("alpina", "brabus", "ac"))
  }

  test("parse VideoSearchBlacklist from empty test json") {
    val in = new ByteArrayInputStream("""[{
        |"fullName": "/auto_ru/common/catalog_video_blacklist",
        |"content": []
        |}]""".stripMargin.getBytes("UTF-8"))
    val videoSearchBlacklist: VideoSearchBlackList = VideoSearchBlackList.parse(in)
    assert(videoSearchBlacklist.marks == Set())
  }

  test("parse VideoSearchBlacklist from unexpected test json") {
    val in = new ByteArrayInputStream("""[{
        |"fullName": "/auto_ru/common/catalog_video_blacklist",
        |"content": {"title":"test"}
        |}]""".stripMargin.getBytes("UTF-8"))
    val videoSearchBlacklist: VideoSearchBlackList = VideoSearchBlackList.parse(in)
    assert(videoSearchBlacklist.marks == Set())
  }

  test("parse VideoSearchBlacklist from no test json") {
    val in = new ByteArrayInputStream("""[{
        |"fullName": "/auto_ru/common/other_node",
        |"content": {"title":"test"}
        |}]""".stripMargin.getBytes("UTF-8"))
    val videoSearchBlacklist: VideoSearchBlackList = VideoSearchBlackList.parse(in)
    assert(videoSearchBlacklist.marks == Set())
  }

  test("toLowercase") {
    val in = new ByteArrayInputStream("""[{
                                        |"fullName": "/auto_ru/common/catalog_video_blacklist",
                                        |"content": ["Alpina", "Brabus", "AC"]
                                        |}]""".stripMargin.getBytes("UTF-8"))
    val videoSearchBlacklist: VideoSearchBlackList = VideoSearchBlackList.parse(in)
    assert(videoSearchBlacklist.marks == Set("alpina", "brabus", "ac"))
  }

  test("parse VideoSearchBlacklist from test data engine") {
    val videoSearchBlacklist: VideoSearchBlackList = VideoSearchBlackList.from(TestDataEngine)
    assert(videoSearchBlacklist.marks.nonEmpty)
  }
}
