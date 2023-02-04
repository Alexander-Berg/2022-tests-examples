package ru.yandex.vertis.general.users.testkit

import com.google.protobuf.empty.Empty
import common.zio.grpc.client.{GrpcClient, GrpcClientLive}
import general.users.api.UserServiceGrpc.UserService
import general.users.api._
import general.users.model.{LimitedUserView, UserView}
import io.grpc.Status
import zio._

import scala.concurrent.Future

object TestUserService {
  type UpdateUsers = (Map[Long, UserView] => Map[Long, UserView]) => IO[Nothing, Unit]

  val layer: ULayer[Has[GrpcClient.Service[UserService]] with Has[UpdateUsers]] = {
    val creation = for {
      state <- ZRef.make[Map[Long, UserView]](Map.empty)
      rt <- ZIO.runtime[Any]
      getUsers = (id: Long) => state.get.map(_.get(id))
      getUserList = (ids: Seq[Long]) => state.get.map(_.filter { case (id, _) => ids.contains(id) })
      updateUsers = state.update(_)
      grpcClient: GrpcClient.Service[UserService] =
        new GrpcClientLive[UserService](new UserServiceImpl(rt, getUsers, getUserList), null)
    } yield Has.allOf(
      grpcClient,
      updateUsers
    )
    creation.toLayerMany
  }

  def withUsers(data: Seq[UserView]): ULayer[Has[GrpcClient.Service[UserService]]] =
    layer >>>
      (for {
        usersService <- ZIO.service[GrpcClient.Service[UserService]]
        update <- ZIO.service[UpdateUsers]
        _ <- update { usersMap =>
          data.foldLeft(usersMap) { case (map, view) =>
            map + (view.id -> view)
          }
        }
      } yield usersService).toLayer

  def addUser(user: UserView): URIO[Has[UpdateUsers], Unit] = addUsers(user :: Nil)

  def addUsers(users: List[UserView]): URIO[Has[UpdateUsers], Unit] =
    ZIO.accessM[Has[UpdateUsers]](_.get.apply { oldUserMap =>
      val newMap = users.map(user => user.id -> user).toMap
      oldUserMap ++ newMap
    })

  private class UserServiceImpl(
      runitme: Runtime[Any],
      getUser: Long => UIO[Option[UserView]],
      getUserList: Seq[Long] => UIO[Map[Long, UserView]],
      publicIdToId: Map[String, Long] = Map.empty)
    extends UserService {

    override def getLimitedUser(request: GetUserRequest): Future[GetLimitedUserResponse] = {
      runitme.unsafeRunToFuture {
        getUser(request.id)
          .someOrFail(Status.NOT_FOUND.asRuntimeException())
          .map(userViewToLimited)
          .map(view => GetLimitedUserResponse(Some(view)))
      }
    }

    override def getUser(request: GetUserRequest): Future[GetUserResponse] =
      runitme.unsafeRunToFuture {
        getUser(request.id)
          .someOrFail(Status.NOT_FOUND.asRuntimeException())
          .map(uv => GetUserResponse(Some(uv)))
      }

    override def getLimitedUserList(request: GetUserListRequest): Future[GetLimitedUserListResponse] =
      runitme.unsafeRunToFuture {
        getUserList(request.ids)
          .map(uv => GetLimitedUserListResponse(users = uv.values.map(userViewToLimited).toSeq))
      }

    override def getUserByPublicId(request: GetUserByPublicIdRequest): Future[GetLimitedUserResponse] = {
      runitme.unsafeRunToFuture {
        getUser(publicIdToId(request.publicId))
          .someOrFail(Status.NOT_FOUND.asRuntimeException())
          .map(uv => GetLimitedUserResponse(Some(userViewToLimited(uv))))
      }
    }

    override def updateUser(request: UpdateUserRequest): Future[UpdateUserResponse] =
      Future.successful(UpdateUserResponse.defaultInstance)

    override def banUserChats(request: BanUserChatsRequest): Future[Empty] = Future.successful(Empty.defaultInstance)

    override def unbanUserChats(request: UnbanUserChatsRequest): Future[Empty] =
      Future.successful(Empty.defaultInstance)

    override def getAddresses(request: GetAddressesRequest): Future[GetAddressesResponse] =
      Future.successful(GetAddressesResponse(None, None))

    private def userViewToLimited(uv: UserView): LimitedUserView = {
      LimitedUserView(
        id = uv.id,
        user = uv.user,
        avatar = uv.avatar,
        email = uv.email,
        phones = uv.phones,
        moderationInfo = uv.moderationInfo,
        isYandexEmployee = uv.isYandexEmployee,
        staffLogin = uv.staffLogin,
        karma = uv.karma,
        publicId = uv.publicId
      )
    }

    override def enableYmlPhones(request: EnableYmlPhoneRequest): Future[EnableYmlPhoneResponse] =
      Future.successful(EnableYmlPhoneResponse.defaultInstance)

    override def updateAddress(request: UpdateAddressRequest): Future[Empty] = {
      Future.successful(Empty.defaultInstance)
    }

    override def updateYmlPhone(request: UpdateYmlPhoneRequest): Future[UpdateUserResponse] = {
      Future.successful(UpdateUserResponse.defaultInstance)
    }
  }
}
