package ru.yandex.realty.generator

import com.google.protobuf.util.Timestamps
import com.google.protobuf.Timestamp
import org.scalacheck.Gen
import ru.yandex.realty.chat.model.{AppUser, ChatUserRef, PassportUser, RealtyC2cChat, WebUser}
import ru.yandex.realty.clients.chat.ChatApiMessageBuilders
import ru.yandex.vertis.{Identity, MimeType, RawMdsIdentity}
import ru.yandex.vertis.chat.model.api.ApiModel._
import ru.yandex.vertis.generators.{BasicGenerators, DateTimeGenerators}
import ru.yandex.vertis.image.Image
import ru.yandex.vertis.paging.Paging
import ru.yandex.vertis.paging.Slice.Page

import scala.collection.JavaConverters._

/**
  * Generators for objects to interact with Chat Backend.
  *
  * @author dimas
  * @author romkaz
  */
object ChatApiGenerators extends BasicGenerators with DateTimeGenerators {

  val rawMdsPayload: Gen[RawMdsIdentity] = for {
    namespace <- Gen.oneOf("auto", "realty", "rabota", "tours")
    groupId <- Gen.choose(0, 1024)
    name <- readableString
  } yield RawMdsIdentity
    .newBuilder()
    .setNamespace(namespace)
    .setGroupId(groupId)
    .setName(name)
    .build()

  val rawMdsIdentity: Gen[Identity] =
    rawMdsPayload.map { payload =>
      Identity
        .newBuilder()
        .setRawMds(payload)
        .build()
    }

  val urlPayload: Gen[String] = readableString

  val urlIdentity: Gen[Identity] =
    urlPayload.map { payload =>
      Identity
        .newBuilder()
        .setUrl(payload)
        .build()
    }

  val identity: Gen[Identity] =
    Gen.oneOf(rawMdsIdentity, urlIdentity)

  val messagePayload: Gen[MessagePayload] = for {
    contentType <- Gen.oneOf(MimeType.values().filter(mt => mt.toString != "UNRECOGNIZED" && mt.getNumber != 0))
    value <- readableString
  } yield MessagePayload
    .newBuilder()
    .setContentType(contentType)
    .setValue(value)
    .build()

  val textMessagePayload: Gen[MessagePayload] = for {
    value <- readableString
  } yield MessagePayload
    .newBuilder()
    .setContentType(MimeType.TEXT_PLAIN)
    .setValue(value)
    .build()

  val htmlMessagePayload: Gen[MessagePayload] = for {
    value <- readableString
  } yield MessagePayload
    .newBuilder()
    .setContentType(MimeType.TEXT_HTML)
    .setValue(s"<b>$value</b>")
    .build()

  val image: Gen[Image] = for {
    id <- identity
  } yield Image
    .newBuilder()
    .setId(id)
    .build()

  val attachment: Gen[Attachment] =
    image.map { value =>
      Attachment
        .newBuilder()
        .setImage(value)
        .build()
    }

  val attachments: Gen[Seq[Attachment]] =
    list(0, 3, attachment)

  val chatMessage: Gen[Message] = for {
    id <- readableString
    created <- timestampInPast
    payload <- messagePayload
    attachments <- attachments
  } yield Message
    .newBuilder()
    .setId(id)
    .setCreated(created)
    .setPayload(payload)
    .addAllAttachments(attachments.asJava)
    .build

  val textChatMessage: Gen[Message] = for {
    id <- readableString
    created <- timestampInPast
    payload <- textMessagePayload
  } yield Message
    .newBuilder()
    .setId(id)
    .setCreated(created)
    .setPayload(payload)
    .build

  val htmlChatMessage: Gen[Message] = for {
    id <- readableString
    created <- timestampInPast
    payload <- htmlMessagePayload
  } yield Message
    .newBuilder()
    .setId(id)
    .setCreated(created)
    .setPayload(payload)
    .build

  val createRoomParameters: Gen[CreateRoomParameters] = for {
    id <- readableString
    users <- set(0, 10, readableString)
    props <- properties
  } yield {
    CreateRoomParameters
      .newBuilder()
      .setId(id)
      .addAllUserIds(users.asJava)
      .putAllProperties(props.asJava)
      .build()
  }

  val roomLocatorDirect: Gen[RoomLocator] = for {
    roomId <- readableString
  } yield RoomLocator.newBuilder().setRoomId(roomId).build()

  val roomLocatorSource: Gen[RoomLocator] = for {
    params <- createRoomParameters
  } yield RoomLocator.newBuilder().setSource(params).build()

  val roomLocator: Gen[RoomLocator] = {
    Gen.oneOf(roomLocatorDirect, roomLocatorSource)
  }

  val roomIdOrLocator: Gen[Either[String, RoomLocator]] = for {
    roomId <- readableString
    optRoomLocator <- Gen.option(roomLocator)
  } yield {
    optRoomLocator.map(Right(_)).getOrElse(Left(roomId))
  }

  val properties: Gen[Map[String, String]] = for {
    count <- Gen.choose(0, 10)
    keys <- Gen.listOfN(count, readableString)
    values <- Gen.listOfN(count, readableString)
  } yield keys.zip(values).toMap

  val room: Gen[Room] = for {
    id <- readableString
    users <- set(0, 10, chatUserRefGen)
    created <- timestampInPast
    props <- properties
    lastMessage <- Gen.option(chatMessage)
  } yield {
    createRoom(id, users.map(_.toPlain), created, props, lastMessage)
  }

  def roomGen(
    usersGen: Gen[Set[String]] = set(0, 10, chatUserRefGen).map(_.map(_.toPlain)),
    propertiesGen: Gen[Map[String, String]] = properties
  ): Gen[Room] =
    for {
      id <- readableString
      users <- usersGen
      created <- timestampInPast
      props <- propertiesGen
      lastMessage <- Gen.option(chatMessage)
    } yield {
      createRoom(id, users, created, props, lastMessage)
    }

  def roomGenByUserRefs(
    usersGen: Gen[Set[ChatUserRef]] = set(0, 10, chatUserRefGen),
    propertiesGen: Gen[Map[String, String]] = properties
  ): Gen[Room] = {
    roomGen(usersGen.map(_.map(_.toPlain)), propertiesGen)
  }

  private def createRoom(
    id: String,
    users: Set[String],
    created: Timestamp,
    properties: Map[String, String],
    lastMessage: Option[Message]
  ) = {
    val builder = Room
      .newBuilder()
      .setId(id)
      .addAllUserIds(users.asJava)
      .addAllUsers(
        users
          .map(userId => {
            User.newBuilder().setId(userId).setMutedNotifications(false).build()
          })
          .asJava
      )
      .setCreated(created)
      .putAllProperties(properties.asJava)
    lastMessage.foreach { message =>
      builder.setLastMessage(message)
    }
    builder.build()
  }

  private def createPaging(num: Int, pageSize: Int) = {
    val page = Page
      .newBuilder()
      .setNum(num)
      .setSize(pageSize)
      .build()

    Paging
      .newBuilder()
      .setPage(page)
      .setPageCount(num)
      .build()
  }

//  private def createRoomsPage(
//    id: String,
//    users: Set[String],
//    created: Timestamp,
//    properties: Map[String, String],
//    lastMessage: Option[Message],
//    page: Int,
//    pageSize: Int
//  ) = {
////    val room = createRoom(id, users, created, properties, lastMessage)
//    val rooms = set(0, 10, roomGen())
//    val paging = createPaging(page, pageSize)
//
//    RoomsPage
//      .newBuilder()
//      .setPaging(paging)
//      .addAllRooms(rooms)
//      .build()
//  }

  private def timestampInPast = instantInPast.map(instant => Timestamps.fromMillis(instant.getMillis))

  val passportUserGen: Gen[PassportUser] = for {
    uid <- Gen.choose(1, 1000000)
  } yield PassportUser(uid.toString)

  val webUserGen: Gen[WebUser] = for {
    uid <- Gen.choose(1, 1000000)
  } yield WebUser(uid.toString)

  val appUserGen: Gen[AppUser] = for {
    uid <- Gen.choose(1, 1000000)
  } yield AppUser(uid.toString)

  val chatUserRefGen: Gen[ChatUserRef] =
    Gen.oneOf(passportUserGen, webUserGen, appUserGen)

  val roomIdGen: Gen[String] = for {
    prefix <- Gen.oneOf(RealtyC2cChat.toPlain, RealtyC2cChat.toPlain)
    uid1 <- Gen.choose(1, 1000000)
    uid2 <- Gen.choose(1, 1000000)
  } yield s"$prefix-$uid1-$uid2"

  val updateRoom: Gen[UpdateRoomParameters] = for {
    add <- set(0, 10, chatUserRefGen)
    remove <- set(0, 10, chatUserRefGen)
  } yield ChatApiMessageBuilders.buildUpdateRoomParameters(add.toList, remove.toList)
}
