package ru.yandex.vertis.moderation.flink.bureau.techsupport

import com.google.protobuf.{Int64Value, StringValue}
import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalacheck.Gen
import org.scalatest.Assertion
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.TestUtils._
import ru.yandex.vertis.moderation.converters.Protobuf
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.generators.Producer._
import ru.yandex.vertis.moderation.model.generators.RichGen
import ru.yandex.vertis.moderation.model.instance.User
import ru.yandex.vertis.moderation.model.meta.Metadata
import ru.yandex.vertis.moderation.proto.Model
import ru.yandex.vertis.moderation.proto.Model.Service
import ru.yandex.vertis.moderation.util.DateTimeUtil.OrderedDateTime
import ru.yandex.vertis.techsupport.proto.Model.ConversationMetadata.Bot
import ru.yandex.vertis.techsupport.proto.Model.{
  Appeal,
  Conversation,
  ConversationMetadata,
  Event,
  Image,
  InternalScenarioId,
  Message,
  MessagePayload,
  RequestContext,
  ScenarioId,
  UserId => TechsupportUserId
}

import scala.collection.JavaConverters._

/**
  * @author potseluev
  */
@RunWith(classOf[JUnitRunner])
class UserTechsupportPhotosExtractorSpec extends SpecBase {

  import UserTechsupportPhotosExtractorSpec._

  private val service: Model.Service = Service.USERS_AUTORU

  private val photosExtractor: Event => Iterable[UserTechsupportPhotosStatistics] =
    new UserTechsupportPhotosExtractor(
      userService = service,
      filter = _ => true
    )

  "UserTechsupportPhotosExtractor" should {
    "extract photos correctly if prevAppeal is empty" in {
      val timestamp = DateTimeGen.next
      val techsupportUser = UserIdGen.suchThat(_.getClient.getAutoru.hasPrivatePerson).next
      val moderationUser = User.Autoru(techsupportUser.getClient.getAutoru.getPrivatePerson.getValue.toString)
      val clientMsg1 = ClientMessageGen.next
      val clientMsg2 = ClientMessageGen.next
      val operatorMsg = OperatorMessageGen.next
      val conversation1 =
        conversationGen(scenarioId = None).next.toBuilder.clearMessages
          .addMessages(clientMsg1)
          .addMessages(operatorMsg)
      val conversation2 =
        conversationGen(scenarioId = None).next.toBuilder.clearMessages
          .addMessages(clientMsg2)
      val appeal =
        AppealGen.next.toBuilder.clearConversations
          .addConversations(conversation1)
          .addConversations(conversation2)
          .setClient(techsupportUser.getClient)
          .build
      val event = toEvent(appeal, prevAppeal = None, timestamp)
      val expectedPhotosSummary =
        Seq(clientMsg1, clientMsg2)
          .flatMap(getPhotosInfos(scenarioId = None))
          .sortBy(_.createTime)
      val expectedPhotosStatistics =
        UserTechsupportPhotosStatistics(
          moderationUser,
          timestamp,
          expectedPhotosSummary
        )
      val actualPhotosStatistics = photosExtractor(event)
      actualPhotosStatistics.size shouldBe 1
      checkIsEqual(actualPhotosStatistics.head, expectedPhotosStatistics)
    }

    "extract photos correctly if prevAppeal isn't empty" in {
      val timestamp = DateTimeGen.next
      val scenarioId = ScenarioIdGen.next
      val techsupportUser = UserIdGen.suchThat(_.getClient.getAutoru.hasPrivatePerson).next
      val moderationUser = User.Autoru(techsupportUser.getClient.getAutoru.getPrivatePerson.getValue.toString)
      val clientMsg1 = ClientMessageGen.next
      val clientMsg2 = ClientMessageGen.next
      val conversation1 =
        conversationGen(Some(scenarioId)).next.toBuilder.clearMessages
          .addMessages(clientMsg1)
      val conversation2 =
        conversationGen(Some(scenarioId)).next.toBuilder.clearMessages
          .addMessages(clientMsg2)
      val appeal =
        AppealGen.next.toBuilder.clearConversations
          .addConversations(conversation1)
          .addConversations(conversation2)
          .setClient(techsupportUser.getClient)
          .build
      val prevAppeal =
        appeal.toBuilder
          .removeConversations(1)
          .build
      val event = toEvent(appeal, Some(prevAppeal), timestamp)
      val expectedPhotosSummary = getPhotosInfos(Some(scenarioId))(clientMsg2).toSeq
      val expectedPhotosStatistics =
        UserTechsupportPhotosStatistics(
          moderationUser,
          timestamp,
          expectedPhotosSummary
        )
      val actualPhotosStatistics = photosExtractor(event)
      actualPhotosStatistics.size shouldBe 1
      checkIsEqual(actualPhotosStatistics.head, expectedPhotosStatistics)
    }

    "return empty output if event doesn't contain new photos" in {
      val appeal = AppealGen.next
      val event = toEvent(appeal, Some(appeal), DateTimeGen.next)
      photosExtractor(event) shouldBe Iterable.empty
    }

  }

  private def checkIsEqual(actual: UserTechsupportPhotosStatistics,
                           expected: UserTechsupportPhotosStatistics
                          ): Assertion = {
    actual.user shouldBe expected.user
    actual.timestamp shouldBe expected.timestamp
    actual.statistics.toSet shouldBe expected.statistics.toSet
  }
}

object UserTechsupportPhotosExtractorSpec {
  private def getPhotosInfos(scenarioId: Option[ScenarioId])(msg: Message): Set[Metadata.TechsupportPhotos.Info] =
    msg.getPayload.getImagesList.asScala.map { image =>
      Metadata.TechsupportPhotos.Info(
        url = image.getUrl.getValue,
        createTime = Protobuf.fromMessage(msg.getTimestamp),
        scenarioId = scenarioId
      )
    }.toSet

  private def toEvent(appeal: Appeal, prevAppeal: Option[Appeal], timestamp: DateTime): Event =
    Event.newBuilder.setAppealUpdate {
      val builder =
        Event.AppealUpdate.newBuilder
          .setAppeal(appeal)
          .setRequestContext(RequestContext.newBuilder.setTimestamp(Protobuf.toMessage(timestamp)))
      prevAppeal.foreach(builder.setPrevAppeal)
      builder
    }.build

  private lazy val AppealGen: Gen[Appeal] =
    for {
      conversations <- ConversationGen.seq(minSize = 0, maxSize = 10)
      user          <- UserIdGen.suchThat(_.hasClient)
    } yield Appeal.newBuilder
      .addAllConversations(conversations.asJava)
      .setClient(user.getClient)
      .build

  private lazy val InternalScenarioIdGen: Gen[ScenarioId] =
    for {
      id <- Gen.oneOf(InternalScenarioId.values.filter(_ != InternalScenarioId.UNRECOGNIZED))
    } yield ScenarioId.newBuilder
      .setInternal(id)
      .build

  private lazy val ExternalScenarioIdGen: Gen[ScenarioId] =
    for {
      id <- StringGen
    } yield ScenarioId.newBuilder
      .setExternal(id)
      .build

  private lazy val ScenarioIdGen: Gen[ScenarioId] = Gen.oneOf(InternalScenarioIdGen, ExternalScenarioIdGen)

  private def conversationGen(scenarioId: Option[ScenarioId]): Gen[Conversation] =
    for {
      messages   <- MessageGen.seq(minSize = 0, maxSize = 10)
      createTime <- DateTimeGen
    } yield {
      val builder =
        Conversation.newBuilder
          .addAllMessages(messages.asJava)
          .setCreateTime(Protobuf.toMessage(createTime))
      scenarioId.foreach { scenario =>
        builder.setMetadata(
          ConversationMetadata.newBuilder
            .setBot(Bot.newBuilder.setScenario(scenario))
        )
      }
      builder.build
    }

  private lazy val ConversationGen: Gen[Conversation] =
    for {
      scenarioId   <- ScenarioIdGen
      conversation <- conversationGen(Some(scenarioId))
    } yield conversation

  private lazy val MessageGen: Gen[Message] =
    for {
      payload   <- MessagePayloadGen
      authorId  <- UserIdGen
      timestamp <- DateTimeGen
    } yield Message.newBuilder
      .setPayload(payload)
      .setAuthor(authorId)
      .setTimestamp(Protobuf.toMessage(timestamp))
      .build

  private val ClientMessageGen: Gen[Message] = MessageGen.suchThat(_.getAuthor.hasClient)

  private val OperatorMessageGen: Gen[Message] = MessageGen.suchThat(_.getAuthor.hasOperator)

  private lazy val MessagePayloadGen: Gen[MessagePayload] =
    for {
      images <- ImageGen.seq(minSize = 1, maxSize = 10)
    } yield MessagePayload.newBuilder
      .addAllImages(images.asJava)
      .build

  private lazy val ImageGen: Gen[Image] =
    for {
      url <- StringGen
    } yield Image.newBuilder
      .setUrl(url)
      .build

  private lazy val AutoruClientGen: Gen[TechsupportUserId] =
    for {
      id <- LongGen
      protoId = Int64Value.newBuilder.setValue(id).build
      isPrivatePerson <- BooleanGen
    } yield {
      val autoruClient = TechsupportUserId.Client.Autoru.newBuilder
      if (isPrivatePerson)
        autoruClient.setPrivatePerson(protoId)
      else
        autoruClient.setDealer(protoId)
      TechsupportUserId.newBuilder
        .setClient(TechsupportUserId.Client.newBuilder.setAutoru(autoruClient))
        .build
    }

  private val OperatorIdGen: Gen[TechsupportUserId] =
    for {
      id <- StringGen
    } yield {
      val jivositeId =
        TechsupportUserId.Operator.Jivosite.newBuilder
          .setId(id)
      TechsupportUserId.newBuilder
        .setOperator(TechsupportUserId.Operator.newBuilder.setJivosite(jivositeId))
        .build
    }

  private lazy val UserIdGen: Gen[TechsupportUserId] =
    Gen.oneOf(
      AutoruClientGen,
      OperatorIdGen
    )

  implicit private def wrap(string: String): StringValue = StringValue.newBuilder.setValue(string).build
}
