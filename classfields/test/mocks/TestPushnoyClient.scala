package vertistraf.common.pushnoy.client.mocks

import ru.yandex.vertis.common.Domain
import vertistraf.common.pushnoy.client.model.{Device, DeviceFullInfo, PushContent, PushSendResponse}
import vertistraf.common.pushnoy.client.service.PushnoyClient
import vertistraf.common.pushnoy.client.service.PushnoyClient.PushnoyClient
import zio._

import java.util.concurrent.ConcurrentHashMap

/** @author Ratskevich Natalia reimai@yandex-team.ru
 */
class TestPushnoyClient(override val domain: Domain) extends PushnoyClient.Service {
  private val pushCounts = new ConcurrentHashMap[String, Int]()

  override def send(
      deviceId: String,
      pushContent: PushContent,
      pushName: Option[String],
      subscription: Option[String]): Task[PushSendResponse] =
    UIO {
      pushCounts.compute(deviceId, (_, c) => Option(c).getOrElse(0) + 1)
      PushSendResponse(1)
    }

  override def getUserDevices(userId: String): Task[Seq[DeviceFullInfo]] =
    Task.succeed(Seq(DeviceFullInfo(Device(deviceForUserId(userId)), None)))

  override def getDeviceFullInfo(deviceId: String): Task[DeviceFullInfo] =
    Task.succeed(DeviceFullInfo(Device(deviceId), None))

  private def deviceForUserId(userId: String): String = s"${userId}_device"

  def totalPushes(receiverId: String): UIO[Int] = UIO(pushCounts.getOrDefault(receiverId, 0))
}

object TestPushnoyClient {
  val layer: URLayer[Has[Domain], PushnoyClient] = (new TestPushnoyClient(_)).toLayer

  def layerForDomain(domain: Domain): ULayer[PushnoyClient] = ZLayer.succeed(new TestPushnoyClient(domain))

  val autoDomainLayer: ULayer[PushnoyClient] = ZLayer.succeed(new TestPushnoyClient(Domain.DOMAIN_AUTO))
}
