package ru.vertistraf.notification_center.mindbox_api.services.impl

import common.zio.logging.Logging
import monocle.syntax.all._
import org.mockito.{ArgumentMatchersSugar, IdiomaticMockito}
import ru.vertistraf.notification_center.mindbox_api.services.PushSender.PushSender
import ru.vertistraf.notification_center.mindbox_api.services.SpamalotExperimentPushComposer.SpamalotExperimentPushComposer
import ru.vertistraf.notification_center.mindbox_api.services.{PushSender, SpamalotExperimentPushComposer}
import ru.vertistraf.notification_center.mindbox_api.{flagNameGen, flagPushDataGen, mindboxBodyGen}
import ru.vertistraf.notification_center.service.ABFlagsService
import ru.vertistraf.notification_center.service.ABFlagsService.ABFlagsService
import ru.yandex.vertis.spamalot.model.ReceiverId
import ru.yandex.vertis.spamalot.service.SendResponse
import vertistraf.common.pushnoy.client.mocks.TestPushnoyClient
import vertistraf.common.pushnoy.client.service.PushnoyClient
import vertistraf.common.pushnoy.client.service.PushnoyClient.PushnoyClient
import vertistraf.common.spamalot.client.SpamalotClient
import vertistraf.common.spamalot.client.SpamalotClient.SpamalotClient
import zio.magic._
import zio.test.Assertion._
import zio.test._
import zio.{Has, ZIO, ZLayer}

import scala.util.Try

object LivePushSenderSpec extends DefaultRunnableSpec with ArgumentMatchersSugar with IdiomaticMockito {
  type Env = PushSender with SpamalotClient with PushnoyClient with ABFlagsService with SpamalotExperimentPushComposer

  override def spec = suite("LivePushSender")(
    suite("sendToUser")(
      testM("""should retrieve user devices, retrieve corresponding flags,
            | compose push content based on flags and send the push to spamalot""".stripMargin) {
        checkM(mindboxBodyGen, Gen.anyUUID, flagNameGen, flagPushDataGen, Gen.setOf(flagNameGen)) {
          (mindboxBody, transactionId, flag, flagData, otherFlags) =>
            val updateFlags = otherFlags + flag
            val updatedMindboxBody = mindboxBody
              .focus(_.flags)
              .modify(_.filterNot { case (flagName, _) => updateFlags.contains(flagName) }
                .updated(flag, flagData))

            val layer = ZLayer.wire[Env](
              ZLayer.succeed[ABFlagsService.Service] {
                mock[ABFlagsService.Service].getFlags(*).returns(ZIO.succeed(updateFlags.toSeq))
              },
              baseLayer
            )

            val zio =
              for {
                _ <- ZIO.serviceWith[PushSender.Service](
                  _.sendToUser(updatedMindboxBody, transactionId.toString)
                )
                pushnoyClient <- ZIO.service[PushnoyClient.Service]
                pushComposer <- ZIO.service[SpamalotExperimentPushComposer.Service]
                sendingClient <- ZIO.service[SpamalotClient.Service]
                expectedNotification <- pushComposer.composeSpamalotPush(updatedMindboxBody, updateFlags)
                pushnoyDevicesCalledAssertion = assert(
                  Try {
                    pushnoyClient
                      .getUserDevices(mindboxBody.receiver.userId)
                      .was(called)
                  }
                )(isSuccess)
                userDevices <- pushnoyClient.getUserDevices(mindboxBody.receiver.userId)
                sentToAllDevicesAssertions <- ZIO.foldLeft(userDevices)(assert(())(anything)) { (assertion, device) =>
                  ZIO.succeed {
                    assertion &&
                    assert(device.info)(isSome) ==>
                      assert(Try {
                        sendingClient
                          .sendPush(
                            ReceiverId(ReceiverId.Id.DeviceId(device.device.id)),
                            expectedNotification,
                            Some(transactionId.toString)
                          )
                          .was(called)
                      })(isSuccess)
                  }
                }
                result = pushnoyDevicesCalledAssertion && sentToAllDevicesAssertions
              } yield result

            zio.provideSomeLayer(layer)
        }
      }
    )
  ) @@ TestAspect.sized(10) @@ TestAspect.samples(100)

  private val baseLayer = ZLayer.wireSome[ABFlagsService, Env](
    TestPushnoyClient.autoDomainLayer.map(pc => Has(spy(pc.get))),
    LiveSpamalotExperimentPushComposer.layer,
    ZLayer.succeed[SpamalotClient.Service] {
      mock[SpamalotClient.Service].sendPush(*, *, *).returns(ZIO.succeed(SendResponse("notification_id")))
    },
    Logging.live,
    LivePushSender.layer
  )
}
