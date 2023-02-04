package vertis.pushnoy.model.response

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import spray.json.JsonParser
import vertis.pushnoy.model.response.XivaBatchResponse
import vertis.pushnoy.model.response.XivaBatchResponse.BatchResponse

class XivaBatchResponseSpec extends AnyWordSpec with Matchers with TypeCheckedTripleEquals {

  "PushXivaBatchFormat" should {
    "parse the valid json successfully" in {
      val expected = XivaBatchResponse(
        Seq(
          BatchResponse(Some(200), None),
          BatchResponse(Some(200), None),
          BatchResponse(Some(200), None),
          BatchResponse(Some(204), None),
          BatchResponse(Some(200), None)
        )
      )

      val jsonString =
        """{
         |  "results": [
         |    {
         |      "body": {
         |        "apns_message_id": "1"
         |      },
         |      "code": 200,
         |      "id": "mob:1"
         |    },
         |    {
         |      "body": {
         |        "apns_message_id": "2"
         |      },
         |      "code": 200,
         |      "id": "mob:2"
         |    },
         |    {
         |      "body": {
         |        "message_id": "3"
         |      },
         |      "code": 200,
         |      "id": "3"
         |    },
         |    {
         |      "body": "no subscriptions",
         |      "code": 204
         |    },
         |    {
         |      "body": {
         |        "apns_message_id": "4"
         |      },
         |      "code": 200,
         |      "id": "4"
         |    }
         |  ]
         |}
         |""".stripMargin
      val jsonToParse = JsonParser(jsonString).asJsObject
      val converted = jsonToParse.convertTo[XivaBatchResponse]

      converted should ===(expected)
    }
  }
}
