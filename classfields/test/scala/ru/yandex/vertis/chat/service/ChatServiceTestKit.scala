package ru.yandex.vertis.chat.service

import ru.yandex.vertis.chat.model.ModelGenerators.{passportPrivateUserId, roomId}
import ru.yandex.vertis.chat.model.Room
import ru.yandex.vertis.chat.service.ServiceGenerators._
import ru.yandex.vertis.chat.util.test.RequestContextAware
import ru.yandex.vertis.chat.{RequestContext, SpecBase}

trait ChatServiceTestKit {

  self: SpecBase with RequestContextAware =>

  def service: ChatService

  def createAndCheckRoom(rc: RequestContext): Room = {
    val parameters = createRoomParameters(Some(roomId.next)).next
    createAndCheckRoom(parameters)(rc)
  }

  def createAndCheckRoom(correct: CreateRoomParameters => CreateRoomParameters = identity): Room = {
    val id = roomId.next
    val parameters = correct(createRoomParameters(Some(id)).next)
    withUserContext(parameters.participants.userIds.headOption.getOrElse(passportPrivateUserId.next)) { rc =>
      createAndCheckRoom(parameters)(rc)
    }
  }

  def createAndCheckRoomInService(
      service: ChatService
  )(correct: CreateRoomParameters => CreateRoomParameters = identity): Room = {
    val id = roomId.next
    val parameters = correct(createRoomParameters(Some(id)).next)
    withUserContext(parameters.participants.userIds.headOption.getOrElse(passportPrivateUserId.next)) { rc =>
      createAndCheckRoom(service, parameters)(rc)
    }
  }

  def createAndCheckRoom(parameters: CreateRoomParameters)(rc: RequestContext): Room = {
    val id = parameters.id.getOrElse(roomId.next)
    service.createRoom(parameters.copy(id = Some(id)))(rc).futureValue.id should be(id)
    val created = service.getRoom(id)(rc).futureValue
    created.users should be(parameters.participants.userIds)
    created.participants should be(parameters.participants)
    created.lastMessage shouldBe empty
    created.properties shouldBe parameters.properties
    created
  }

  def createAndCheckRoom(service: ChatService, parameters: CreateRoomParameters)(rc: RequestContext): Room = {
    val id = parameters.id.getOrElse(roomId.next)
    service.createRoom(parameters.copy(id = Some(id)))(rc).futureValue.id should be(id)
    val created = service.getRoom(id)(rc).futureValue
    created.users should be(parameters.participants.userIds)
    created.participants should be(parameters.participants)
    created.lastMessage shouldBe empty
    created.properties shouldBe parameters.properties
    created
  }
}
