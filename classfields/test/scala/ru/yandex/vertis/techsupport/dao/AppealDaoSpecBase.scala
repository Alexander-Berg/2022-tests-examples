package ru.yandex.vertis.vsquality.techsupport.dao

import java.time.Instant
import org.scalacheck.Prop
import org.scalatestplus.scalacheck.Checkers
import ru.yandex.vertis.vsquality.techsupport.dao.AppealDao._
import ru.yandex.vertis.vsquality.techsupport.model.ChatProvider.{Telegram, VertisChats}
import ru.yandex.vertis.vsquality.techsupport.model._
import ru.yandex.vertis.vsquality.techsupport.util.Utils._
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.lang_utils.{Ignore, Use}
import ru.yandex.vertis.vsquality.techsupport.util.{
  AppealPatchesCalculator,
  Clearable,
  ClearableAppealDaoProvider,
  SpecBase
}
import Clearable._
import cats.data.NonEmptyList
import ru.yandex.vertis.vsquality.utils.lang_utils.interval.OptInterval

import java.time.temporal.ChronoUnit
import scala.concurrent.duration._

/**
  * @author potseluev
  */
trait AppealDaoSpecBase extends SpecBase with ClearableAppealDaoProvider {
  def appealDao: AppealDao[F]

  import ru.yandex.vertis.vsquality.techsupport.Arbitraries._
  import ru.yandex.vertis.vsquality.techsupport.CoreArbitraries._

  before {
    appealDao.clear()
  }

  private def generateAppeal(client: UserId.Client): Appeal =
    generate[Appeal]().copy(client = client)

  "AppealDao" should {
    "create appeal and get it correctly" in {
      Checkers.check(Prop.forAll { appeal: Appeal =>
        appealDao.clear()
        upsertAppeal(appeal)
        val filter = AppealFilter(
          client = Use(appeal.client),
          chatProvider = Use(appeal.chat.provider)
        )
        val appeals = appealDao.getAppeals(filter, generate[Sort](), Int.MaxValue).await
        appeals == Seq(appeal.sorted)
      })
    }

    "get several appeals from one client sorted and limited correctly" in {
      val client = generate[UserId.Client]()
      val appeal1 = generateAppeal(client)
      val appeal2 = generateAppeal(client).copy(createTime = appeal1.createTime.plusMillis(1))
      val otherAppeals = generateSeq[Appeal](3, _.client != client)
      (Seq(appeal1, appeal2) ++ otherAppeals).foreach(upsertAppeal)
      val filterByClient = AppealFilter(
        client = Use(client),
        chatProvider = Ignore
      )
      val sort = Sort.ByCreateTime(asc = false)
      val resultAppeals = appealDao.getAppeals(filterByClient, sort, limit = 1).await
      resultAppeals shouldBe Seq(appeal2.sorted)
    }

    "filter appeals by chatProvider correctly" in {
      val client = generate[UserId.Client]()
      val appeal1 = generateAppeal(client).copy(
        chat = generate[ChatDescriptor]().copy(provider = Telegram)
      )
      val appeal2 = generateAppeal(client).copy(
        chat = generate[ChatDescriptor]().copy(provider = VertisChats)
      )
      Seq(appeal1, appeal2).foreach(upsertAppeal)
      val filter = AppealFilter(
        client = Use(client),
        chatProvider = Use(ChatProvider.Telegram)
      )
      val resultAppeals = appealDao.getAppeals(filter, generate[Sort](), Int.MaxValue).await
      resultAppeals shouldBe Seq(appeal1.sorted)
    }

    "patch existed appeal correctly" in {
      appealDao.clear()
      val appeal = generate[Appeal]().sorted
      val lastConversation = appeal.conversations.last
      upsertAppeal(appeal)
      val state = generate[AppealState]()
      val message = generate[Message]().copy(messageId = lastConversation.messages.last.messageId)
      val techsupportRespondent = generate[TechsupportRespondent]()
      val chatId = generate[ChatId]()
      val tags = generate[Appeal.Tags]()
      val marks = generate[Appeal.Marks]()
      val lastConversationPatch = ConversationPatch(
        createTime = appeal.conversations.last.createTime,
        techsupportRespondent = Use(techsupportRespondent),
        message = Use(message)
      )
      val appealPatch = AppealPatch(
        key = appeal.key,
        updateTime = now(),
        state = Use(state),
        conversation = Use(lastConversationPatch),
        chatId = Use(chatId),
        tags = Use(tags),
        marks = Use(marks)
      )
      appealDao.createOrPatchAppeal(appealPatch).await
      val expectedConversations = appeal.conversations.mapLast(conversation =>
        conversation.copy(
          messages = conversation.messages.mapLast(_ => message),
          techsupportRespondent = techsupportRespondent
        )
      )
      val expectedAppeal = appeal.copy(
        state = state,
        conversations = expectedConversations,
        chat = appeal.chat.copy(id = chatId),
        tags = tags,
        marks = marks
      )
      val filter = AppealFilter(
        client = Use(appeal.client),
        chatProvider = Ignore
      )
      val actualAppeals = appealDao.getAppeals(filter, generate[Sort](), Int.MaxValue).await
      actualAppeals shouldBe Seq(expectedAppeal.sorted)
    }

    "not change existed appeal if patch is empty" in {
      val appeal = generate[Appeal]().sorted
      upsertAppeal(appeal)
      val emptyAppealPatch = AppealPatch(key = appeal.key, updateTime = now())
      appealDao.createOrPatchAppeal(emptyAppealPatch).await
      val filter = AppealFilter(
        client = Use(appeal.client),
        chatProvider = Ignore
      )
      val actualAppeals = appealDao.getAppeals(filter, generate[Sort](), Int.MaxValue).await
      actualAppeals shouldBe Seq(appeal)
    }

    "fail to read appeal with no conversations" in {
      val appealKey = generate[Appeal.Key]()
      val emptyAppealPatch = AppealPatch(key = appealKey, updateTime = now())
      appealDao.createOrPatchAppeal(emptyAppealPatch).await
      val filter = AppealFilter(
        client = Use(appealKey.client),
        chatProvider = Ignore
      )
      appealDao
        .getAppeals(filter, generate[Sort](), Int.MaxValue)
        .shouldFailWith[RuntimeException]
    }

    "fail to read appeal with conversation with no messages" in {
      val appealKey = generate[Appeal.Key]()
      val conversationPatch = generate[ConversationPatch]().copy(message = Ignore)
      appealDao.createOrPatchConversation(appealKey, conversationPatch, now()).await
      val filter = AppealFilter(
        client = Use(appealKey.client),
        chatProvider = Ignore
      )
      appealDao
        .getAppeals(filter, generate[Sort](), Int.MaxValue)
        .shouldFailWith[RuntimeException]
    }

    "successfully apply 0 patches" in {
      appealDao.applyPatches(Seq.empty).await shouldBe (())
    }

    "return correct expired appeals with bot" in {

      val ttl = 1.days
      val bot = UserId.Operator.VertisTechsupportBot

      val interval = OptInterval(None, Some(now().minusMillis(ttl.toMillis)))

      val filter = AppealFilter(Ignore, Ignore, Use(interval))

      def getMessages(messageMeta: Seq[(UserId, Instant)]): NonEmptyList[Message] = {
        NonEmptyList.fromListUnsafe(
          messageMeta.map { case (author, createTime) =>
            generate[Message]().copy(author = author, timestamp = createTime)
          }.toList
        )
      }

      def nowMinus(duration: FiniteDuration): Instant = now().minusMillis(duration.toMillis)

      appealDao.clear()
      val client1 = generate[UserId.Client]()
      val client2 = generate[UserId.Client]()

      val metaNotExpired = Seq(
        (client1, nowMinus(1.minutes)),
        (bot, nowMinus(1.seconds))
      )
      val metaExpired = Seq(
        (client2, nowMinus(ttl + 1.minutes)),
        (bot, nowMinus(ttl + 1.seconds))
      )

      val activeNotExpiredConversation = generate[Conversation]().copy(
        techsupportRespondent = TechsupportRespondent.Bot,
        messages = getMessages(metaNotExpired)
      )
      val activeExpiredConversation = generate[Conversation]().copy(
        techsupportRespondent = TechsupportRespondent.Bot,
        messages = getMessages(metaExpired)
      )

      val appeal1 = generate[Appeal]().copy(
        client = client1,
        state = AppealState.Created,
        conversations = NonEmptyList.one(activeNotExpiredConversation)
      )
      val appeal2 = generate[Appeal]().copy(
        client = client2,
        state = AppealState.Created,
        conversations = NonEmptyList.one(activeExpiredConversation)
      )

      def upsert(appeal: Appeal) = {
        val patches = AppealPatchesCalculator(appeal)
        val ts = appeal.conversations.last.messages.last.timestamp
        appealDao.applyPatches(patches.map(_.copy(updateTime = ts)).toList).await
      }

      upsert(appeal1)
      upsert(appeal2)

      val actualAppeals = appealDao.getAppeals(filter, generate[Sort](), Int.MaxValue).await
      val expected = appeal2

      actualAppeals.size shouldBe 1

      val actual = actualAppeals.head

      actual.client shouldBe expected.client
      actual.conversations.last.messages.last.author shouldBe bot
    }
  }

  private def now() = Instant.now().truncatedTo(ChronoUnit.MICROS)

  private def upsertAppeal(appeal: Appeal): Unit =
    appealDao.applyPatches(AppealPatchesCalculator(appeal).toList).await
}
