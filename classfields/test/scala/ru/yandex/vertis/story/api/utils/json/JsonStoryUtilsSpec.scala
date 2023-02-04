package ru.yandex.vertis.story.api.utils.json

import org.scalatest.PrivateMethodTester
import play.api.libs.json.Json
import ru.yandex.vertis.baker.util.test.http.BaseSpec
import ru.yandex.vertis.story.api.model.StoriesContainer
import ru.yandex.vertis.story.api.utils.json.JsonStoryUtils.prepareJson

class JsonStoryUtilsSpec extends BaseSpec with PrivateMethodTester {

  val jsons =
    """
    {
        "storiesBasePath": "https://autoru-stories.s3.yandex.net/export/test/default/",
        "stories": [
        {
            "id": 2231,
            "version": 1,
            "revision": 14,
            "pages": 1,
            "image": "https://autoru-stories.s3.yandex.net/test/materials/images/65adc4e8-b18a-493b-8cff-75f8bee16e8f/image/default_1.jpg",
            "image_sizes": {},
            "imageFull": "https://autoru-stories.s3.yandex.net/test/materials/images/65adc4e8-b18a-493b-8cff-75f8bee16e8f/image/1_3.jpg",
            "image_full_sizes": {
                "preview": "/9j/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAAoABQDASIAAhEBAxEB/8QAGQABAAMBAQAAAAAAAAAAAAAAAAUHCAMG/8QAJhAAAgICAgICAQUBAAAAAAAAAwQBAgUGAAcREggTFQkUISIxQf/EABkBAAIDAQAAAAAAAAAAAAAAAAADAQQHCP/EACERAAICAQUBAAMAAAAAAAAAAAECAwQFAAYREhMhBxQj/9oADAMBAAIRAxEAPwCF6d+OHd3fb8pdU9dbHtQRlgLeZAndXXEL+3i1XM83AkKlr4nysuRlyZia1Wm0eOaB3L4I7p19FMPuG0Y7F7tcVWia4LHtPlUWmtpsZjHIfudjopT1te2SNjFV7Upa4glrEzFR6D+qnvPxOQPpW/Kdk9iYJ/EYpLqguM7Rzupa515i8OmbH5bEj03Cwhi9gdO0THsgyOQcGZBbxMhbKQppxZs/ye37vzbth2HQ9Byes6/sBCTmHcntG2ZNXMMn+mW4ydsls4wPFY9JMdm8sezHn2kt/cNqGG2ziquFjzWYFG1Bfr9qpsWLqRqSQsgT9aWiqTwMGjkSWaUequA0fUd9t3f+SN+2d12dsYCOxirOKvNG1XG4+pk7t1YyrwPM16te71bMLR2UNalV6wSr6NKOSLc2bAm1fJfjWnsdkPdYLi7uKI2VNlVj2kBqS4kizSSVpN/rKtS1azXz5mf4c8ri1Px+PUUgCKtxBr94cYNkKENX/uzdajjTrfoQ9iEmzLbBr2ta9yTNvEOZnY8vebwH8fR/LnkHz7HqeCXI5X7wXcj4CzHk66VxBunF485bocm1Os1/xUJGLbRI06ooJACSFk5XhSQSoVSAIvYdS1nbKY8ey4TH5seKcl/HjyAfvGu1YciuSBzMUJW4/FSBNUgCetJIO00pNZ4IhLhEuuIQFwUqIC4B0CAI6x4qMIRVoIVIj/KDrWsf8jnTjktYneGKu88zwQFzDA0jtDCZGLSGKIsUjLsSzlFBZiS3JPOnJUqxWJ7cVavHatCNbNmOGNLFhYVCQrPOqiWZYkAWMSMwjUBUAHzTjjjidWNOOOOGjTjjjho1/9k=",
                "x1": "https://autoru-stories.s3.yandex.net/test/stories/65adc4e8-b18a-493b-8cff-75f8bee16e8f/images/1_3@1x.jpg",
                "x2": "https://autoru-stories.s3.yandex.net/test/stories/65adc4e8-b18a-493b-8cff-75f8bee16e8f/images/1_3@2x.jpg",
                "x3": "https://autoru-stories.s3.yandex.net/test/stories/65adc4e8-b18a-493b-8cff-75f8bee16e8f/images/1_3@3x.jpg"
            },
            "imagePreview": "/9j/2wBDAAEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/2wBDAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQH/wAARCAAoACMDASIAAhEBAxEB/8QAGwABAAEFAQAAAAAAAAAAAAAAAAcBAwYICQT/xAAnEAACAgICAgICAQUAAAAAAAADBAIFAQYABxESCBMVQSEJFiMxUf/EABkBAAIDAQAAAAAAAAAAAAAAAAAHBQYIBP/EACQRAAICAgICAgIDAAAAAAAAAAIDAQQFBgAREhMHIRQxIiNB/9oADAMBAAIRAxEAPwCJ9K6/3fse2jR6Jq11tVnnOMEBTpEYGrHOceSPuZ9Ea8MMZxKZXmV4Rj/Oc86Z9U/0wNkaWUtu6NjJSyeAQtfpGmYjYbI7P1NAUSuEAxPA8MikueVZVNRASBMZZngc45zzSPnV0zrl7Q9Vddi1vpHQGXLIN73rfaquKq1WthWGKEqmpLFWPlQx1sqGvrllc4IWEjNqnV+4c7nyg+a3WhtCtNb1D5TkXrWqzJXNl0rX9RUtt7YmaMJmuL3Wtqorx6lNXDZHCpSshiaKxDJmspghXGRetaxv/wAhql9cq+k4fzFb325CzmVwYgyYJETBrOVzMeuBx7Eu/V1yxKZ138g/JqNHcmiVB1m7ZTNiqI/SnJhpp8vyJAkKmGBMyHjZZK+oJSSYE81U3D4labqFteUbW9Gy0k696GZKAh6ZJeRSDFfIUadtJNtcEPDmLGCZpZhJjCqovOIan7z18tpoVXI7nq9srYyjirDAlpV2llgkclj+NStq1MVpmIIzZJGuZMQawjMSDgQpyxD7vy4tG6ix0nWux+w2tWbLBNkmn0dVqf1LZGwEyp4N7vahJWtYaCWddJVQdu/WpqGdysWygfBdD3vV+z44co7Luaz/ALIcyoyz2aakLUWFvYV7a82aMda2yUZq0BWxi/xLQGjZBGfH3YiPDhz+LxNDEoqlTpqdj6AVwyNddx1u/aVAqh1uwplembbbYJlhrKprVJQuuIBAiKk0Hdd2y25V0DnbuQq5jJS+3jsgilNOpRGPfaCkArOzSVUqAY1gRcCGMgCsQ1jCk5S4444suaw5SUYTjKBIRIOcZQIOWPMSDnHMCDljP+4khKUJY/cZZx++c8bnorsPO/WGuVdXaH1YljMqWxycEtVr0Ds8kH9xjTxAz9bEkgZREObRTBlP65DLCWOh/KeMefPjHn/v75YcFsdvAjfWhFa0nIIBTU2odKwaopJNkJQ5DBaqDaI9M8CFpCYlHXVI3TQsPvAYscm65VZirRvTYoGhbzS8BGzTMrCLIeixKkGUwv2CaAlZh2feu+sfHfXUAq42ZrNmMGRG/AUk36nWyNj9vZp6JWj2dqUuZ5zKB2FV8QiIMlzQACUJ9r66vqVAoVaKlaivH1AmiuJVYWP36iDCEPbPjHtPOMznnHmcpZ/nns45FW79u8fnaebPuZEJnpYdzM/wXHQD++u4juf9meTWD1nBa4n04bG16fYwDHCMstPiOp/vttk7DY8o8oA2SsZmfABj644445x8neOOOOHDjjjjhw44444cOf/Z",
            "nativeStory": "https:/autoru-stories.s3.yandex.net/export/test/default/xml/2231/story.xml",
            "background": "#000000",
            "text": "#ffffff",
            "title": "Новая Лада уже на АвтоВАЗе и другое",
            "geo": null,
            "tags": [],
            "rotationProbability": 0,
            "x-ios-app-version": null,
            "x-ios-app-version-to": null,
            "x-android-app-version": null,
            "x-android-app-version-to": null
        },
        {
          "id": 123,
          "version": 1,
          "pages": 5,
          "image": "https://autoru-stories.s3.yandex.net/test/stories/5fa7422b-1936-4fda-8c55-76b0936afd5a/default@",
          "image_sizes": {
            "preview": "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",
            "x1": "https://autoru-stories.s3.yandex.net/test/stories/e3715b11-6388-4e00-b4cb-67b4e10fe0be/default_1@1x.jpg",
            "x2": "https://autoru-stories.s3.yandex.net/test/stories/e3715b11-6388-4e00-b4cb-67b4e10fe0be/default_1@2x.jpg",
            "x3": "https://autoru-stories.s3.yandex.net/test/stories/e3715b11-6388-4e00-b4cb-67b4e10fe0be/default_1@3x.jpg"
          },
          "imageFull": "https://autoru-stories.s3.yandex.net/test/stories/5fa7422b-1936-4fda-8c55-76b0936afd5a/images/5@",
          "imagePreview": "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",
          "nativeStory": "https://autoru-stories.s3.yandex.net/test/stories/5fa7422b-1936-4fda-8c55-76b0936afd5a/story.xml",
          "background": "#000000",
          "text": "#ffffff",
          "title": "Соперники кроссовера Maybach",
          "tags": [{
                "id": 1,
                "name": "whatsnew",
                "createdAt": "2022-04-13T14:09:35.000Z"
          }]
        }]
    }
  """
  "prepareJson should correctly parse jsons" in {
    val json = Json.parse(jsons)
    val parsedJson = prepareJson(json)
    val storiesContainer = parsedJson.as[StoriesContainer].enriched

    storiesContainer.stories.head.id shouldBe "2231"
    storiesContainer.stories.head.tags shouldBe None
    storiesContainer.stories.head.image_sizes shouldBe None

    storiesContainer.stories(1).id shouldBe "123"
    storiesContainer.stories(1).tags.get.head.id shouldBe 1
    storiesContainer.stories(1).tags.get.head.name shouldBe "whatsnew"
    storiesContainer.stories(1).tags.get.head.createdAt shouldBe "2022-04-13T14:09:35.000Z"
    storiesContainer.stories(1).image_sizes.get.preview shouldBe Some(
      "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"
    )
    storiesContainer.stories(1).image_sizes.get.x1 shouldBe Some(
      "https://autoru-stories.s3.yandex.net/test/stories/e3715b11-6388-4e00-b4cb-67b4e10fe0be/default_1@1x.jpg"
    )
    storiesContainer.stories(1).image_sizes.get.x2 shouldBe Some(
      "https://autoru-stories.s3.yandex.net/test/stories/e3715b11-6388-4e00-b4cb-67b4e10fe0be/default_1@2x.jpg"
    )
    storiesContainer.stories(1).image_sizes.get.x3 shouldBe Some(
      "https://autoru-stories.s3.yandex.net/test/stories/e3715b11-6388-4e00-b4cb-67b4e10fe0be/default_1@3x.jpg"
    )
  }
}
