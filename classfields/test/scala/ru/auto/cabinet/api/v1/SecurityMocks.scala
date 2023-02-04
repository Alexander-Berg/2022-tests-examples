package ru.auto.cabinet.api.v1

import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.client.RequestBuilding.RequestTransformer
import akka.http.scaladsl.model.headers.RawHeader

import scala.concurrent.Future.successful
import ru.auto.cabinet.model.{CustomerId, Role, Uid, User}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar.mock
import ru.auto.cabinet.dao.jdbc.RolesDao
import ru.auto.cabinet.security.SecurityProvider
import ru.auto.cabinet.trace.Context

/** Just an example set to initialize
  */
class SecurityMocks {
  implicit private val rc = Context.unknown
  val rolesDao = mock[RolesDao]
  implicit val securityProvider = mock[SecurityProvider]

  def user1Id = 1L
  def user2Id = 2L
  def adminId = 7L
  def client1Id = 1L
  def client2Id = 2L
  def balanceId1 = 1L
  def agencyClient1Id = 101
  def agencyClient2Id = 102
  def client1 = CustomerId(client1Id)
  def client2 = CustomerId(client2Id)
  def agent1 = CustomerId(agencyClient1Id, Option(client1Id), None)
  def agent2 = CustomerId(agencyClient2Id, Option(client2Id), None)
  def user1 = User(user1Id, Seq(Role.Manager))
  def user2 = User(user2Id, Seq(Role.Manager))
  def admin = User(adminId, Seq(Role.Root))
  val agencyClientId = 3L
  val agencyUserId = 4L

  when(rolesDao.userRoles(user1Id)).thenReturn(successful(user1))
  when(rolesDao.userRoles(user2Id)).thenReturn(successful(user2))
  when(rolesDao.userRoles(adminId)).thenReturn(successful(admin))

  def requestHeader = RequestBuilding.addHeader(hdrRequest)
  def headers1: RequestTransformer = addHeaders(user1Id)
  def headers2: RequestTransformer = addHeaders(user2Id)
  def adminHeaders: RequestTransformer = addHeaders(adminId)
  def agentHeaders: RequestTransformer = addHeaders(agencyUserId)

  def addHeaders(userId: Uid): RequestTransformer =
    RequestBuilding.addHeaders(hdrOperator(userId), hdrRequest)

  def hdrOperator(userId: Uid) =
    RawHeader("X-Autoru-Operator-Uid", userId.toString)
  def hdrRequest = RawHeader("X-Autoru-Request-ID", "0")

}
