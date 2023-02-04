package ru.yandex.vos2.autoru.services

import ru.yandex.vos2.util.http.MockHttpClientHelper

/**
  * Created by andrey on 8/25/16.
  */
trait MockYoutubeClientHelper extends MockHttpClientHelper {

  val videoExistAnswer =
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

  val videoNotExistAnswer =
    """{
      | "kind": "youtube#videoListResponse",
      | "etag": "\"I_8xdZu766_FSaexEaDXTIfEWc0/fLm1KQ8-3QJyblcpL-fp8y0eIVw\"",
      | "pageInfo": {
      |  "totalResults": 0,
      |  "resultsPerPage": 0
      | },
      | "items": []
      |}""".stripMargin
}
