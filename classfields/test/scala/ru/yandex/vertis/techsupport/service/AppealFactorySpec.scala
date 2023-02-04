package ru.yandex.vertis.vsquality.techsupport.service

import cats.Id

import java.time.Instant
import cats.data.NonEmptyList
import cats.kernel.Monoid
import ru.yandex.vertis.vsquality.techsupport.dao.AppealDao.{AppealPatch, ConversationPatch}
import ru.yandex.vertis.vsquality.techsupport.model.Conversation.MetadataSet
import ru.yandex.vertis.vsquality.techsupport.model.{Appeal, AppealState, Conversation, TechsupportRespondent}
import ru.yandex.vertis.vsquality.techsupport.service.AppealFactory.AppealSource
import ru.yandex.vertis.vsquality.techsupport.service.impl.{AppealFactoryImpl, ConversationFactoryImpl}
import ru.yandex.vertis.vsquality.utils.lang_utils.Use
import ru.yandex.vertis.vsquality.techsupport.util.SpecBase

/**
  * @author potseluev
  */
class AppealFactorySpec extends SpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  private def getFactory(techsupportRespondent: TechsupportRespondent): AppealFactory[Id] =
    new AppealFactoryImpl(
      new ConversationFactoryImpl(ConversationFactoryImpl.TechsupportRespondentDispatcher.Direct(techsupportRespondent))
    )

  "AppealFactory" should {
    "create appeal correctly" in {
      val techsupportRespondent = generate[TechsupportRespondent]()
      val appealFactory = getFactory(techsupportRespondent)
      val appealSource = generate[AppealSource]()
      val expectedConversation = Conversation(
        techsupportRespondent = techsupportRespondent,
        createTime = appealSource.createTime,
        messages = NonEmptyList.one(appealSource.message),
        metadataSet = Monoid[MetadataSet].empty
      )
      val expectedAppeal = Appeal(
        client = appealSource.client,
        chat = appealSource.chatDescriptor,
        conversations = NonEmptyList.one(expectedConversation),
        state = AppealState.Created,
        tags = Monoid[Appeal.Tags].empty,
        createTime = appealSource.createTime,
        marks = Monoid[Appeal.Marks].empty
      )
      val expectedPatch = AppealPatch(
        key = expectedAppeal.key,
        updateTime = Instant.now,
        chatId = Use(appealSource.chatDescriptor.id),
        state = Use(expectedAppeal.state),
        conversation = Use(
          ConversationPatch(
            createTime = appealSource.createTime,
            techsupportRespondent = Use(techsupportRespondent),
            message = Use(appealSource.message)
          )
        ),
        tags = Use(Monoid[Appeal.Tags].empty)
      )
      val actual = appealFactory.createAppeal(appealSource)
      actual shouldBe AppealFactory.Result(expectedAppeal, expectedPatch.copy(updateTime = actual.patch.updateTime))
    }
  }
}
