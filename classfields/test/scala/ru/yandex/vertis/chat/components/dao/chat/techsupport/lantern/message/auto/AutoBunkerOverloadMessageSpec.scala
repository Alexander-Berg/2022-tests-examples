package ru.yandex.vertis.chat.components.dao.chat.techsupport.lantern.message.auto

import org.scalatest.OptionValues
import play.api.libs.json.Json
import ru.yandex.vertis.chat.SpecBase
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils
import ru.yandex.vertis.chat.common.techsupport.TechSupportUtils.{TechSupportDealerOverloadMessageId, TechSupportOverloadMessageId}
import ru.yandex.vertis.chat.util.test.RequestContextAware

class AutoBunkerOverloadMessageSpec extends SpecBase with RequestContextAware with OptionValues {

  private val jsonStr =
    """{
      |	"show_chat_notify": true,
      |	"text_chat_notify": "Вас приветствует техническая поддержка!️",
      |	"show_chat_notify_dealer": false,
      |	"text_chat_notify_dealer": "⏱Если в вашем отчёте по VIN что-то не так — не переживайте. Это из-за неполадок на сайте ГИБДД. Мы обновим все отчёты, как только работа наладится. Хороших выходных! 👋",
      |	"show_chat_notify_users_from_list": false,
      |	"text_chat_notify_users_from_list": "LOL KEKw",
      |	"users_list": [
      |		"user:69858069",
      |		"user:57211736",
      |		"user:10245464"
      |	],
      |	"show_chat_notify_dealers_from_list": false,
      |	"text_chat_notify_dealers_from_list": "",
      |	"dealers_list": []
      |}""".stripMargin
  "AutoBunkerOverloadMessage" should {
    "return private overload message for private user" in {
      val message: AutoBunkerOverloadMessage = getMessage
      withUserContext("user:10245464") { rc =>
        message.getOverloadMessage(rc).providedId.value should startWith(TechSupportOverloadMessageId)
      }
    }
    "return dealer overload message for dealer" in {
      val message: AutoBunkerOverloadMessage = getMessage.copy(show_chat_notify_dealer = true)
      withUserContext("dealer:10245464") { rc =>
        message.getOverloadMessage(rc).providedId.value should startWith(TechSupportDealerOverloadMessageId)
      }
    }
    "not be applied for private user" when {
      "show_chat_notify=false and show_chat_notify_users_from_list=false" in {
        val message: AutoBunkerOverloadMessage = getMessage.copy(show_chat_notify = false)
        withUserContext("user:10245464") { rc =>
          message.isApplied(rc) shouldBe false
        }
      }
      "show_chat_notify=true but text is empty and show_chat_notify_users_from_list=false" in {
        val message: AutoBunkerOverloadMessage = getMessage.copy(text_chat_notify = "")
        withUserContext("user:10245464") { rc =>
          message.isApplied(rc) shouldBe false
        }
      }
      "show_chat_notify=false and show_chat_notify_users_from_list=true but text for users from list is empty" in {
        val message: AutoBunkerOverloadMessage = getMessage.copy(
          show_chat_notify = false,
          show_chat_notify_users_from_list = true,
          text_chat_notify_users_from_list = ""
        )
        withUserContext("user:10245464") { rc =>
          message.isApplied(rc) shouldBe false
        }
      }
    }
    "be applied for private user" when {
      "show_chat_notify=true and show_chat_notify_users_from_list=false" in {
        val message: AutoBunkerOverloadMessage = getMessage
        withUserContext("user:10245464") { rc =>
          message.isApplied(rc) shouldBe true
        }
      }
      "show_chat_notify=false and show_chat_notify_users_from_list=true and user is from list" in {
        val message: AutoBunkerOverloadMessage =
          getMessage.copy(show_chat_notify = false, show_chat_notify_users_from_list = true)
        withUserContext("user:10245464") { rc =>
          message.isApplied(rc) shouldBe true
        }
      }
    }
    "return basic text for all private users" when {
      "user in list but show_chat_notify_users_from_list=false" in {
        val message: AutoBunkerOverloadMessage = getMessage
        withUserContext("user:10245464") { rc =>
          message.getText(rc) shouldBe "Вас приветствует техническая поддержка!️"
        }
      }
      "user in list and show_chat_notify_users_from_list=true but text_chat_notify_users_from_list is empty" in {
        val message: AutoBunkerOverloadMessage =
          getMessage.copy(show_chat_notify_users_from_list = true, text_chat_notify_users_from_list = "")
        withUserContext("user:10245464") { rc =>
          message.getText(rc) shouldBe "Вас приветствует техническая поддержка!️"
        }
      }
    }
    "return text for users from list" when {
      "user in list and show_chat_notify_users_from_list=true and text for users from list is not empty" in {
        val message: AutoBunkerOverloadMessage = getMessage.copy(show_chat_notify_users_from_list = true)
        withUserContext("user:10245464") { rc =>
          message.getText(rc) shouldBe "LOL KEKw"
        }
      }
    }
    "be parsed" when {
      "received this data from bunker" in {
        val message: AutoBunkerOverloadMessage = getMessage
        message.show_chat_notify shouldBe true
        message.text_chat_notify shouldBe "Вас приветствует техническая поддержка!️"
        message.show_chat_notify_dealer shouldBe false
        message.text_chat_notify_dealer shouldBe "⏱Если в вашем отчёте по VIN что-то не так — не переживайте. Это из-за неполадок на сайте ГИБДД. Мы обновим все отчёты, как только работа наладится. Хороших выходных! 👋"
        message.show_chat_notify_users_from_list shouldBe false
        message.text_chat_notify_users_from_list shouldBe "LOL KEKw"
      }
    }
  }

  private def getMessage = {
    val json = Json.parse(jsonStr)
    val message = json.as[AutoBunkerOverloadMessage]
    message
  }
}
