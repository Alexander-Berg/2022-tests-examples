package ru.yandex.vertis.vsquality.techsupport.service

import java.time.Instant
import cats.{Applicative, Id}
import cats.data.NonEmptyList
import cats.implicits.catsSyntaxEitherId
import cats.kernel.Monoid
import org.scalacheck.{Gen, Prop}
import org.scalatestplus.scalacheck.Checkers
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.techsupport.dao.AppealDao
import ru.yandex.vertis.vsquality.techsupport.dao.AppealDao.{AppealFilter, AppealPatch, ConversationPatch, MessagePatch}
import ru.yandex.vertis.vsquality.techsupport.dao.impl.ydb.{YdbAppealDao, YdbAppealSerialization}
import ru.yandex.vertis.vsquality.techsupport.model.Conversation.MetadataSet
import ru.yandex.vertis.vsquality.techsupport.model.{
  Appeal,
  Conversation,
  Message,
  MessageType,
  TechsupportProvider,
  TechsupportRespondent,
  UserId
}
import ru.yandex.vertis.vsquality.techsupport.service.AppealPatcher.{IllegalPatch, PatcherError, PatcherResult}
import ru.yandex.vertis.vsquality.techsupport.service.AppealPatcherSpec.DbPatcher
import ru.yandex.vertis.vsquality.techsupport.service.impl.{AppealPatcherImpl, ConversationFactoryImpl}
import ru.yandex.vertis.vsquality.techsupport.util.Clearable._
import ru.yandex.vertis.vsquality.techsupport.util.{AppealPatchesCalculator, Clearable}
import ru.yandex.vertis.vsquality.utils.lang_utils.{Ignore, Use}
import ru.yandex.vertis.vsquality.techsupport.model.UserId.Client
import ru.yandex.vertis.vsquality.techsupport.util.ydb.YdbSpecBase

/**
  * @author potseluev
  */
class AppealPatcherSpec extends YdbSpecBase {

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  private val conversationFactory = new ConversationFactoryImpl(
    ConversationFactoryImpl.TechsupportRespondentDispatcher.Default
  )
  private val patcher: AppealPatcher[Id] = new AppealPatcherImpl(conversationFactory)

  private def genConversation(ts: Int): Conversation =
    generate[Conversation]().copy(createTime = Instant.ofEpochMilli(ts))

  private def newConversationPatchGen(appeal: Appeal): Gen[ConversationPatch] = {
    val conversationCreateTimes = appeal.conversations.map(_.createTime)
    gen[ConversationPatch].suchThat(patch => !conversationCreateTimes.exists(_ == patch.createTime))
  }

  private def isNewMessage(conversation: Conversation)(msg: Message): Boolean =
    conversation.messages.forall { m =>
      m.messageId != msg.messageId &&
      m.timestamp != msg.timestamp
    }

  private def updateConversationPatchGen(appeal: Appeal): Gen[AppealPatch] =
    for {
      conversationToPatch <- Gen.oneOf(appeal.conversations.toList)
      conversationPatch <- gen[ConversationPatch]
        .suchThat(_.message.toOption.forall(isNewMessage(conversationToPatch)))
        .map(_.copy(createTime = conversationToPatch.createTime))
      appealPatch <- gen[AppealPatch]
    } yield appealPatch.copy(
      key = appeal.key,
      conversation = Use(conversationPatch)
    )

  private def addNewConversationPatchGen(appeal: Appeal): Gen[AppealPatch] =
    for {
      techsupportRespondent <- gen[Use[TechsupportRespondent]]
      msg                   <- gen[Use[Message]]
      conversationPatch <- newConversationPatchGen(appeal).map(
        _.copy(techsupportRespondent = techsupportRespondent, message = msg)
      )
      appealPatch <- gen[AppealPatch]
    } yield appealPatch.copy(
      key = appeal.key,
      conversation = Use(conversationPatch)
    )

  private val patcherInputGen: Gen[(Appeal, AppealPatch)] = for {
    appeal <- gen[Appeal]
    patch  <- Gen.oneOf(addNewConversationPatchGen(appeal), updateConversationPatchGen(appeal))
  } yield (appeal, patch)

  "AppealPatcher" should {

    "return same result as DbPatcher on correct input data" in {
      val dbPatcher: AppealPatcher[Id] = new DbPatcher(new YdbAppealDao(ydb)(new YdbAppealSerialization))
      Checkers.check(Prop.forAll(patcherInputGen) { case (appeal, patch) =>
        val inMemoryResult = patcher.applyPatch(appeal, patch).map(_.sorted)
        val dbResult = dbPatcher.applyPatch(appeal, patch)
        inMemoryResult == dbResult
      })
    }

    "add new conversation to the end of conversation list" in {
      val appeal = generate[Appeal]().copy(
        conversations = NonEmptyList.of(
          genConversation(1),
          genConversation(5),
          genConversation(2)
        )
      )
      val newMessage = generate[Message]()
      val newConversation = genConversation(3).copy(messages = NonEmptyList.one(newMessage))
      val patch =
        ConversationPatch(
          newConversation.createTime,
          Use(newConversation.techsupportRespondent),
          Use(newMessage),
          Use(newConversation.metadataSet)
        ).asAppealPatch(appeal.key, newConversation.createTime)
      val actualResult = patcher.applyPatch(appeal, patch)
      val expectedAppeal = appeal.copy(
        conversations = appeal.conversations :+ newConversation
      )
      actualResult shouldBe Right(expectedAppeal)
    }

    "use bot techsupportRespondent if techsupportRespondent is not specified in new conversation patch and message not contains images" in {
      val appeal = generate[Appeal]()
      val newConversationCreateTime = generate[Instant](ts => !appeal.conversations.exists(_.createTime == ts))
      val client = generate[UserId.Client]()
      val message = generate[Message](msg => msg.payload.forall(_.images.isEmpty))
        .copy(`type` = MessageType.Common, author = client)
      val newConversationPatch = ConversationPatch(
        createTime = newConversationCreateTime,
        techsupportRespondent = Ignore,
        message = Use(message)
      ).asAppealPatch(appeal.key, newConversationCreateTime)
      val actualResult = patcher.applyPatch(appeal, newConversationPatch)
      val newConversation =
        Conversation(
          TechsupportRespondent.Bot,
          NonEmptyList.one(message),
          newConversationCreateTime,
          Monoid[MetadataSet].empty
        )
      val expectedAppeal = appeal.copy(conversations = appeal.conversations :+ newConversation)
      actualResult shouldBe Right(expectedAppeal)
    }

    "use jivosite techsupportRespondent if techsupportRespondent is not specified in new conversation patch and message contains images" in {
      val appeal = generate[Appeal]()
      val newConversationCreateTime = generate[Instant](ts => !appeal.conversations.exists(_.createTime == ts))
      val client = generate[UserId.Client]()
      val message = generate[Message](_.payload.exists(_.images.nonEmpty))
        .copy(author = client)
      val newConversationPatch = ConversationPatch(
        createTime = newConversationCreateTime,
        techsupportRespondent = Ignore,
        message = Use(message)
      ).asAppealPatch(appeal.key, newConversationCreateTime)
      val actualResult = patcher.applyPatch(appeal, newConversationPatch)
      val newConversation =
        Conversation(
          TechsupportRespondent.ExternalProvider(TechsupportProvider.Jivosite),
          NonEmptyList.one(message),
          newConversationCreateTime,
          Monoid[MetadataSet].empty
        )
      val expectedAppeal = appeal.copy(conversations = appeal.conversations :+ newConversation)
      actualResult shouldBe Right(expectedAppeal)
    }

    "use jivosite techsupportRespondent if techsupportRespondent is not specified in new conversation patch and conversation is started by an operator" in {
      val appeal = generate[Appeal](_.state.isTerminal)
      val newConversationCreateTime = generate[Instant](ts => !appeal.conversations.exists(_.createTime == ts))
      val operator = generate[UserId.Operator.Jivosite]()
      val message = generate[Message](_.payload.forall(_.images.isEmpty))
        .copy(`type` = MessageType.Common, author = operator)
      val newConversationPatch = ConversationPatch(
        createTime = newConversationCreateTime,
        techsupportRespondent = Ignore,
        message = Use(message)
      ).asAppealPatch(appeal.key, newConversationCreateTime)
      val actualResult = patcher.applyPatch(appeal, newConversationPatch)
      val newConversation =
        Conversation(
          TechsupportRespondent.ExternalProvider(TechsupportProvider.Jivosite),
          NonEmptyList.one(message),
          newConversationCreateTime,
          Monoid[MetadataSet].empty
        )
      val expectedAppeal = appeal.copy(conversations = appeal.conversations :+ newConversation)
      actualResult shouldBe Right(expectedAppeal)
    }

    "add new message to existing conversation to the end of the message list" in {
      val conversations = Seq(1, 5, 2).map(genConversation).toList
      val appeal = generate[Appeal]().copy(conversations = NonEmptyList.fromListUnsafe(conversations))
      val conversationsToPatch = conversations(1)
      val newMessage = generate[Message](msg => !conversationsToPatch.messages.exists(_.messageId == msg.messageId))
      val patch = ConversationPatch(
        createTime = conversationsToPatch.createTime,
        message = Use(newMessage)
      ).asAppealPatch(appeal.key, conversationsToPatch.createTime)
      val expectedPatchedConversation =
        conversationsToPatch.copy(messages = conversationsToPatch.messages :+ newMessage)
      val actualResult = patcher.applyPatch(appeal, patch)
      val expectedAppeal = appeal.copy(
        conversations = NonEmptyList.fromListUnsafe(
          conversations.updated(1, expectedPatchedConversation)
        )
      )
      actualResult shouldBe Right(expectedAppeal)
    }

    "fail if appeal key != patch.key" in {
      val appeal = generate[Appeal]()
      val patch = generate[AppealPatch](_.key != appeal.key)
      patcher
        .applyPatch(appeal, patch)
        .shouldFailWith[IllegalPatch]
    }

    "fail if patched conversation contain message with the same id as message in patch" in {
      val appeal = generate[Appeal]()
      val conversationToPatch = Gen.oneOf(appeal.conversations.toList).generate()
      val messageId = Gen.oneOf(conversationToPatch.messages.map(_.messageId).toList).generate()
      val message = generate[Message]().copy(messageId = messageId)
      val patch = MessagePatch(message).asAppealPatch(
        conversationKey = conversationToPatch.key(appeal.key),
        updateTime = conversationToPatch.createTime
      )
      patcher
        .applyPatch(appeal, patch)
        .shouldFailWith[IllegalPatch]
    }

    "fail if patch creates new conversation but it doesn't contain a message" in {
      val appeal = generate[Appeal]()
      val newConversationCreateTime = generate[Instant](ts => !appeal.conversations.exists(_.createTime == ts))
      val illegalNewConversationPatch = ConversationPatch(newConversationCreateTime)
        .asAppealPatch(appeal.key, newConversationCreateTime)
      patcher
        .applyPatch(appeal, illegalNewConversationPatch)
        .shouldFailWith[IllegalPatch]
    }

  }
}

object AppealPatcherSpec {

  private class DbPatcher[F[_]: Awaitable: Applicative](appealDao: AppealDao[F])(implicit cl: Clearable[AppealDao[F]])
    extends AppealPatcher[Id] {

    override def applyPatch(appeal: Appeal, patch: AppealDao.AppealPatch): PatcherResult[Appeal] = {
      assume(appeal.key == patch.key)
      appealDao.clear()
      val patches = AppealPatchesCalculator(appeal) :+ patch
      appealDao.applyPatches(patches.toList).await
      val filter = AppealFilter(Use(appeal.client), Ignore)
      val sort = AppealDao.Sort.ByCreateTime(asc = true)
      Right(appealDao.getAppeals(filter, sort, 1).await.head)
    }

    override def applyPatches(appeal: Appeal, patches: Seq[AppealPatch]): PatcherResult[Appeal] =
      patches.foldLeft(appeal.asRight[PatcherError]) { (appealToPatch, patch) =>
        appealToPatch.flatMap(applyPatch(_, patch))
      }
  }

}
