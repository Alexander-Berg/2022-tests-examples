package ru.auto.api.model.bunker

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.model.bunker.chatbot.ChatBotInfo
import ru.auto.api.testkit.TestDataEngine

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-15.
  */
class ChatBotProfilesTest extends AnyFunSuite {

  test("parse ChatBotProfiles from RAW_BUNKER data type") {
    val profiles: ChatBotInfo = ChatBotInfo.from(TestDataEngine)

    assert(profiles.profiles.nonEmpty)
  }

}
