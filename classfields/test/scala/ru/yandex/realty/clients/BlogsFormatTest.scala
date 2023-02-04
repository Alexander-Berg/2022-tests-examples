package ru.yandex.realty.clients

import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.{JsValue, Json}
import ru.yandex.realty.clients.blogs.{BlogsClient}

/**
  * author: rmuzhikov
  */
class BlogsFormatTest extends FlatSpec with Matchers {
  import ru.yandex.realty.clients.blogs.BlogsFormat._

  val postJson: JsValue = Json.parse(s"""{
       |        "_id": "54dae44eb6637a226fba4124",
       |        "created_at": "2015-02-08T11:33:27.074Z",
       |        "updated_at": "2015-02-09T11:33:27.074Z",
       |        "publishDate": "2015-02-10T11:33:27.074Z",
       |        "approvedTitle": "Заголовок второго поста",
       |        "approvedBody": {
       |            "source": "Текст поста **с разметкой** ...",
       |            "html": "Текст поста с разметкой ...",
       |            "contentType": "WIKIv0.71"
       |        },
       |        "approvedPreview": {
       |            "source": "Сегодня мы запустили бета-версию новой ((http://metrika.yandex.ru Яндекс.Метрики)",
       |            "html": "Сегодня мы запустили бета-версию новой <a href='http://metrika.yandex.ru'>Яндекс.Метрики.</a>",
       |            "contentType": "WIKIv0.71"
       |        },
       |        "titleImage": {
       |            "orig" : {
       |               "height" : 640,
       |               "path" : "/get-yablogs/603/imagename/orig",
       |               "width" : 1024,
       |               "fullPath": "http://avatars.mdst.yandex.net/get-yablogs/603/imagename/orig"
       |            },
       |            "200x200" : {
       |               "path" : "/get-yablogs/603/imagename/sizename",
       |               "fullPath": "http://avatars.mdst.yandex.net/get-yablogs/603/imagename/size"
       |            }
       |        },
       |        "tags":[{"displayName":"1","slug":"1"}],
       |        "authorId": "8600685941399260644",
       |        "slug": "second_test_post",
       |        "viewType": "minor",
       |        "commentsCount": 5,
       |        "hasNext": true
       |    }""".stripMargin)

  "Post parser" should "parse test post without errors" in {
    postJson.as[BlogsClient.Post]
  }
}
