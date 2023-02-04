package amogus.model

import io.circe.Decoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredDecoder
import io.circe.parser._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class EmbeddedDecoderSpec extends AnyWordSpecLike with Matchers {

  private val inputData = """
                            |{
                            |    "_links": {
                            |        "self": {
                            |            "href": "https://example.amocrm.ru/api/v4/tasks"
                            |        }
                            |    },
                            |    "_embedded": {
                            |        "tasks": [
                            |            {
                            |                "id": 123,
                            |                "request_id": "example",
                            |                "_links": {
                            |                    "self": {
                            |                        "href": "https://example.amocrm.ru/api/v4/tasks/123"
                            |                    }
                            |                }
                            |            },
                            |            {
                            |                "id": 456,
                            |                "request_id": "example",
                            |                "_links": {
                            |                    "self": {
                            |                        "href": "https://example.amocrm.ru/api/v4/tasks/456"
                            |                    }
                            |                }
                            |            }
                            |        ]
                            |    }
                            |}
                            |""".stripMargin

  private case class DummyTask(id: Long, requestId: String)

  implicit private val testCodecConfig: Configuration = Configuration.default.withSnakeCaseMemberNames
  implicit private val DummyTaskDecoder: Decoder[DummyTask] = deriveConfiguredDecoder[DummyTask]

  "EmbeddedDecoder" should {
    "decode list of IDs for given resource" in {
      val idsOpt = parse(inputData)
        .map(json => EmbeddedDecoder.getIds("tasks").decodeJson(json).toOption)
        .toOption
        .flatten

      idsOpt shouldBe Some(List(123, 456))
    }
    "decode list of elements for given resource" in {
      val tasksOpt = parse(inputData)
        .map(json => EmbeddedDecoder.get[List[DummyTask]]("tasks").decodeJson(json).toOption)
        .toOption
        .flatten

      tasksOpt shouldBe Some(List(DummyTask(123, "example"), DummyTask(456, "example")))
    }
  }
}
