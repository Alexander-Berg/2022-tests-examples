package ru.yandex.vos2.autoru.services

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.OptionValues
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vos2.AutoruModel.AutoruOffer.YoutubeVideo.YoutubeVideoStatus

/**
  * Created by andrey on 8/22/16.
  */
@RunWith(classOf[JUnitRunner])
class YoutubeVideoStatusJsonConverterTest extends AnyFunSuite with OptionValues {

  val existsJson =
    """{
      | "kind": "youtube#videoListResponse",
      | "etag": "\"I_8xdZu766_FSaexEaDXTIfEWc0/dxJZ0pXva8NE5YlWnG9oEKc-YY4\"",
      | "pageInfo": {
      |  "totalResults": 1,
      |  "resultsPerPage": 1
      | },
      | "items": [
      |  {
      |   "kind": "youtube#video",
      |   "etag": "\"I_8xdZu766_FSaexEaDXTIfEWc0/mgdvQFYj4H9JzJwI1RK6kzZvjYo\"",
      |   "id": "gQNdu7GDdbc",
      |   "status": {
      |    "uploadStatus": "processed",
      |    "privacyStatus": "public",
      |    "license": "youtube",
      |    "embeddable": true,
      |    "publicStatsViewable": true
      |   }
      |  }
      | ]
      |}""".stripMargin

  val notExistsJson =
    """{
      | "kind": "youtube#videoListResponse",
      | "etag": "\"I_8xdZu766_FSaexEaDXTIfEWc0/fLm1KQ8-3QJyblcpL-fp8y0eIVw\"",
      | "pageInfo": {
      |  "totalResults": 0,
      |  "resultsPerPage": 0
      | },
      | "items": []
      |}
      | """.stripMargin

  test("testGetVideoInfo") {
    val existingVideoStatus = YoutubeVideoStatusJsonConverter.info(existsJson).value
    assert(existingVideoStatus.kind == "youtube#videoListResponse")
    assert(existingVideoStatus.etag == "\"I_8xdZu766_FSaexEaDXTIfEWc0/dxJZ0pXva8NE5YlWnG9oEKc-YY4\"")
    assert(existingVideoStatus.pageInfo.totalResults == 1)
    assert(existingVideoStatus.pageInfo.resultsPerPage == 1)
    assert(existingVideoStatus.items.length == 1)
    assert(existingVideoStatus.items.head.kind == "youtube#video")
    assert(existingVideoStatus.items.head.etag == "\"I_8xdZu766_FSaexEaDXTIfEWc0/mgdvQFYj4H9JzJwI1RK6kzZvjYo\"")
    assert(existingVideoStatus.items.head.id == "gQNdu7GDdbc")
    assert(existingVideoStatus.items.head.status.uploadStatus == "processed")
    assert(existingVideoStatus.items.head.status.privacyStatus == "public")
    assert(existingVideoStatus.items.head.status.license == "youtube")
    assert(existingVideoStatus.items.head.status.embeddable)
    assert(existingVideoStatus.status == YoutubeVideoStatus.AVAILABLE)

    assert(
      existingVideoStatus
        .copy(items = {
          List(existingVideoStatus.items.head.copy(status = {
            existingVideoStatus.items.head.status.copy(uploadStatus = "not processed")
          }))
        })
        .status == YoutubeVideoStatus.UNAVAILABLE
    )
    assert(
      existingVideoStatus
        .copy(items = {
          List(existingVideoStatus.items.head.copy(status = {
            existingVideoStatus.items.head.status.copy(privacyStatus = "private")
          }))
        })
        .status == YoutubeVideoStatus.UNAVAILABLE
    )
    assert(
      existingVideoStatus
        .copy(items = {
          List(existingVideoStatus.items.head.copy(status = {
            existingVideoStatus.items.head.status.copy(embeddable = false)
          }))
        })
        .status == YoutubeVideoStatus.UNAVAILABLE
    )

    val notExistingVideoStatus = YoutubeVideoStatusJsonConverter.info(notExistsJson).value
    assert(notExistingVideoStatus.kind == "youtube#videoListResponse")
    assert(notExistingVideoStatus.etag == "\"I_8xdZu766_FSaexEaDXTIfEWc0/fLm1KQ8-3QJyblcpL-fp8y0eIVw\"")
    assert(notExistingVideoStatus.pageInfo.totalResults == 0)
    assert(notExistingVideoStatus.pageInfo.resultsPerPage == 0)
    assert(notExistingVideoStatus.items.isEmpty)
    assert(notExistingVideoStatus.status == YoutubeVideoStatus.UNAVAILABLE)
  }

  test("testVideoStatus") {
    assert(YoutubeVideoStatusJsonConverter.status(existsJson) == YoutubeVideoStatus.AVAILABLE)
    assert(YoutubeVideoStatusJsonConverter.status(notExistsJson) == YoutubeVideoStatus.UNAVAILABLE)
  }

}
