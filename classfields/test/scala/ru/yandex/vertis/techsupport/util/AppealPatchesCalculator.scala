package ru.yandex.vertis.vsquality.techsupport.util

import java.time.Instant

import cats.data.NonEmptyList
import ru.yandex.vertis.vsquality.techsupport.dao.AppealDao.{AppealPatch, ConversationPatch, MessagePatch}
import ru.yandex.vertis.vsquality.techsupport.model.Appeal
import ru.yandex.vertis.vsquality.techsupport.util.Utils.RichIterable
import ru.yandex.vertis.vsquality.utils.lang_utils.{Ignore, Use}

/**
  * @author potseluev
  */
object AppealPatchesCalculator {

  def apply(appeal: Appeal): NonEmptyList[AppealPatch] = {
    assume(appeal.conversations.toList.isUniqueBy(_.key(appeal.key)))
    val updateTime = Instant.now
    val appealUpsertPatch = AppealPatch(
      key = appeal.key,
      updateTime = updateTime,
      chatId = Use(appeal.chat.id),
      state = Use(appeal.state),
      conversation = Ignore,
      tags = Use(appeal.tags),
      marks = Use(appeal.marks)
    )
    val conversationUpsertPatches = appeal.conversations
      .map { conversation =>
        assume(conversation.messages.toList.isUniqueBy(_.messageId))
        ConversationPatch(
          createTime = conversation.createTime,
          techsupportRespondent = Use(conversation.techsupportRespondent),
          message = Ignore,
          metadata = Use(conversation.metadataSet)
        )
      }
      .map(_.asAppealPatch(appeal.key, updateTime))
    val messageUpsertPatches =
      for {
        conversation <- appeal.conversations
        message      <- conversation.messages
      } yield MessagePatch(
        message = message
      ).asAppealPatch(conversation.key(appeal.key), updateTime)
    appealUpsertPatch :: conversationUpsertPatches ::: messageUpsertPatches
  }
}
