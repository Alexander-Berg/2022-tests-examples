package ru.yandex.vertis.passport.service.user.social.clients

import com.google.common.base.Charsets
import com.google.common.io.Resources
import org.joda.time.LocalDate
import org.scalatest.{FreeSpec, Matchers}
import play.api.libs.json.Json
import ru.yandex.vertis.passport.model.SocialUserSource

/**
  *
  * @author zvez
  */
class VkClientSpec extends FreeSpec with Matchers {

  "parsing VK response" - {

    "real case" in {
      val data = Resources.toString(getClass.getResource("/social/responses/vk.json"), Charsets.UTF_8)

      val json = Json.parse(data)

      val vkUser = VkClient.parseUserResult(json)
      val socialUser = VkClient.toSocialUser(vkUser)

      val expected = SocialUserSource(
        id = "3449080",
        firstName = Some("Андрей"),
        lastName = Some("Зиновьев"),
        birthday = Some(LocalDate.parse("1985-02-12")),
        city = Some("Караганда"),
        country = Some("Казахстан"),
        avatar = Some("https://pp.userap9080/a_edefb50b.jpg")
      )

      socialUser shouldBe expected
    }

    "filtered data case" in {
      val data = Resources.toString(getClass.getResource("/social/responses/vk_bad_cases.json"), Charsets.UTF_8)

      val json = Json.parse(data)

      val vkUser = VkClient.parseUserResult(json)
      val socialUser = VkClient.toSocialUser(vkUser)

      val expected = SocialUserSource(
        id = "123",
        firstName = Some("Вася"),
        lastName = Some("Пупкин"),
        nickname = Some("pupkin")
      )

      socialUser shouldBe expected
    }

  }

}
