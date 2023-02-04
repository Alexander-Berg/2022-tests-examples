package ru.yandex.vertis.chat.components.clients.mds

import org.scalatest.{FunSuite, Matchers}
import play.api.libs.json.Json

/**
  * TODO
  *
  * @author aborunov
  */
class DefaultMdsUploaderSpec extends FunSuite with Matchers {
  test("parseJsonUploadResponse") {
    val str =
      """
        |{
        |    "extra": {
        |        "svg": {
        |            "path": "/get-vertis-chat/3915/6b5c18b5fb330f6767b015f5e99b8cbc/svg"
        |        }
        |    },
        |    "group-id": 3915,
        |    "imagename": "6b5c18b5fb330f6767b015f5e99b8cbc",
        |    "meta": {
        |        "crc64": "308BE881B7C885B1",
        |        "expires-at": "Wed, 04 Jul 2018 14:07:32 GMT",
        |        "md5": "2271c617e08693ceab6084a57cb06c5f",
        |        "orig-animated": false,
        |        "orig-format": "JPEG",
        |        "orig-orientation": "",
        |        "orig-size": {
        |            "x": 640,
        |            "y": 423
        |        },
        |        "processed_by_computer_vision": false,
        |        "processed_by_computer_vision_description": "computer vision is disabled",
        |        "processing": "finished"
        |    },
        |    "sizes": {
        |        "1200x1200": {
        |            "height": 423,
        |            "path": "/get-vertis-chat/3915/6b5c18b5fb330f6767b015f5e99b8cbc/1200x1200",
        |            "width": 640
        |        },
        |        "320x320": {
        |            "height": 212,
        |            "path": "/get-vertis-chat/3915/6b5c18b5fb330f6767b015f5e99b8cbc/320x320",
        |            "width": 320
        |        },
        |        "460x460": {
        |            "height": 304,
        |            "path": "/get-vertis-chat/3915/6b5c18b5fb330f6767b015f5e99b8cbc/460x460",
        |            "width": 460
        |        },
        |        "orig": {
        |            "height": 423,
        |            "path": "/get-vertis-chat/3915/6b5c18b5fb330f6767b015f5e99b8cbc/orig",
        |            "width": 640
        |        }
        |    }
        |}
        |
      """.stripMargin
    val js = Json.parse(str)
    val mdsImage = DefaultMdsUploader.parseJsonUploadResponse(js, "http://example.com", "vertis-chat")
    assert(
      mdsImage == MdsImage(
        3915,
        "6b5c18b5fb330f6767b015f5e99b8cbc",
        "vertis-chat",
        "http://example.com/get-vertis-chat/3915/6b5c18b5fb330f6767b015f5e99b8cbc/1200x1200",
        "http://example.com/get-vertis-chat/3915/6b5c18b5fb330f6767b015f5e99b8cbc/460x460",
        "http://example.com/get-vertis-chat/3915/6b5c18b5fb330f6767b015f5e99b8cbc/320x320",
        "http://example.com/get-vertis-chat/3915/6b5c18b5fb330f6767b015f5e99b8cbc/orig"
      )
    )
  }
}
