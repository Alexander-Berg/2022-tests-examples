package vertis.pushnoy.dao

import ru.yandex.vertis.generators.ProducerProvider
import ru.yandex.vertis.ydb.zio.TxUIO
import vertis.pushnoy.model.request.enums.{ClientOS, Platform}
import vertis.pushnoy.model.{Device, DeviceFullInfo, PushHistory, User}
import vertis.pushnoy.model.request.{DeviceInfo, RequestInfo, TokenInfo}
import vertis.pushnoy.model.response.PushHistoryResponse
import vertis.pushnoy.dao.TestDao._
import vertis.pushnoy.model.ClientType
import vertis.pushnoy.services.xiva.TestXivaClient.InvalidId

import scala.concurrent.Future

/** @author kusaeva
  */
class TestDao extends Dao with ProducerProvider {

  override def getPushHistory(device: Device)(implicit ctx: RequestInfo): Future[PushHistory] =
    Future.successful(PushHistory(Map(pushHistory.name -> pushHistory.history)))

  override def savePushHistory(device: Device, name: String)(implicit ctx: RequestInfo): Future[Unit] =
    if (device.id == InvalidId) {
      Future.failed(TestException())
    } else {
      success
    }

  override def getDeviceInfo(device: Device)(implicit ctx: RequestInfo): Future[DeviceInfo] =
    Future.successful(deviceInfo)

  override def deletePushHistory(device: Device, pushName: String)(implicit ctx: RequestInfo): Future[Unit] =
    success

  override def addTokenInfo(device: Device, tokenInfo: TokenInfo)(implicit ctx: RequestInfo): Future[Unit] =
    if (device.id == InvalidId) {
      Future.failed(TestException())
    } else {
      success
    }

  override def addDeviceInfo(device: Device, tokenInfo: DeviceInfo)(implicit ctx: RequestInfo): Future[Unit] =
    success

  override def attachDevice(user: User, device: Device)(implicit ctx: RequestInfo): Future[Unit] =
    success

  override def detachDevice(user: User, device: Device)(implicit ctx: RequestInfo): Future[Unit] =
    success

  override def getTokenInfo(device: Device)(implicit ctx: RequestInfo): Future[TokenInfo] =
    Future.successful(tokenInfo)

  override def getTokenAndDeviceInfo(
      device: Device
    )(implicit ctx: RequestInfo): Future[(Option[TokenInfo], Option[DeviceInfo])] = Future.successful {
    (Some(tokenInfo), Some(deviceInfo))
  }

  override def getUserDevicesFullInfo(user: User)(implicit ctx: RequestInfo): Future[Seq[DeviceFullInfo]] =
    if (user.id == InvalidId) {
      Future.failed(TestException())
    } else {
      Future.successful(
        Seq(deviceFullInfo)
      )
    }

  override def getDeviceFullInfo(device: Device)(implicit ctx: RequestInfo): Future[DeviceFullInfo] =
    Future.successful(deviceFullInfo.copy(device = device))

  override def getUserDevicesId(
      user: User,
      subscription: Option[String]
    )(implicit ctx: RequestInfo): Future[Seq[Device]] =
    if (user.id == InvalidId) {
      Future.failed(TestException())
    } else {
      Future.successful(
        Seq(Device(client = user.client, id = "id"))
      )
    }

  override def getDeviceOwner(device: Device)(implicit ctx: RequestInfo): Future[Option[User]] = ???

  override def updateDeviceSubscription(
      device: Device,
      subscription: String,
      disable: Boolean
    )(implicit ctx: RequestInfo): Future[Unit] = success

  override def deleteUser(user: User, device: Device): TxUIO[Unit] = ???

  override def deleteDevice(device: Device): TxUIO[Unit] = ???
}

object TestDao {
  val success = Future.successful(())
  case class TestException() extends RuntimeException("test", null)

  val pushHistory = PushHistoryResponse("testPush", List(12132132L))
  val deviceInfo = DeviceInfo("test", "test", "test", "test", "test", "test", ClientOS.ANDROID, "test", Some("6.0.0"))
  val tokenInfo = TokenInfo("testToken", Platform.APNS, "pushToken")
  val deviceFullInfo = DeviceFullInfo(Device(ClientType.Auto, "id"), Some(deviceInfo), Some(tokenInfo))
}
