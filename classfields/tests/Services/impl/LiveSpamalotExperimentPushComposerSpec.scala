package ru.vertistraf.notification_center.mindbox_api.services.impl

import common.zio.logging.Logging
import monocle.syntax.all._
import ru.vertistraf.notification_center.mindbox_api.model.mindbox.PushData.FlagPushData
import ru.vertistraf.notification_center.mindbox_api.model.mindbox.{Action, MindboxBody, PushMediaData}
import ru.vertistraf.notification_center.mindbox_api.services.SpamalotExperimentPushComposer
import ru.vertistraf.notification_center.mindbox_api.services.SpamalotExperimentPushComposer.SpamalotExperimentPushComposer
import ru.vertistraf.notification_center.mindbox_api.{flagNameGen, flagPushDataGen, flagsListGen, mindboxBodyGen}
import ru.vertistraf.notification_center.model.ExpFlag
import ru.yandex.vertis.image.avatars_image.AvatarsImage
import ru.yandex.vertis.spamalot.model.{Notification, Payload}
import zio.magic._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{ZIO, ZLayer}

object LiveSpamalotExperimentPushComposerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("LiveSpamalotExperimentPushComposer")(
      suite("composeSpamalotPush when")(
        suite("there is exactly 1 same flag in the list and request")(
          testM(
            "the fields that are overridden by the flag should be used and missing fields are defaulted to main"
          ) {
            checkM(flagNameGen, flagPushDataGen, mindboxBodyGen, Gen.setOf(flagNameGen)) {
              (matchingFlag, matchingFlagData, mindboxBody, flags) =>
                val updatedMindboxBody = mindboxBody
                  .focus(_.flags)
                  .modify(
                    _.filterNot { case (flagName, _) => flags.contains(flagName) }
                      .updated(matchingFlag, matchingFlagData)
                  )
                val updatedFlags = flags + matchingFlag
                val expectedNotification =
                  expectedNotificationForFlag(updatedMindboxBody, Some(matchingFlagData), Some(matchingFlag))

                for {
                  pushComposer <- ZIO.service[SpamalotExperimentPushComposer.Service]
                  notification <- pushComposer.composeSpamalotPush(updatedMindboxBody, updatedFlags)
                } yield assert(notification)(equalTo(expectedNotification))
            }
          }
        ),
        suite("there are multiple flag matches in the list and the request") {
          testM("the first (sorted alphabetically) matching flag must be used") {
            checkM(flagsListGen, mindboxBodyGen) { (flags, mindboxBody) =>
              val uniqueFlags = flags.distinctBy(_._1)
              val expectedFlag = uniqueFlags.sortBy(_._1).headOption
              val updatedMindboxBody = mindboxBody.focus(_.flags).replace(uniqueFlags.toMap)
              val expectedNotification =
                expectedNotificationForFlag(updatedMindboxBody, expectedFlag.map(_._2), expectedFlag.map(_._1))

              for {
                pushComposer <- ZIO.service[SpamalotExperimentPushComposer.Service]
                notification <- pushComposer.composeSpamalotPush(updatedMindboxBody, uniqueFlags.map(_._1).toSet)
              } yield assert(notification)(equalTo(expectedNotification))
            }
          }
        },
        suite("there are no flag matches") {
          testM("the main body fields must be used") {
            checkM(Gen.setOf(flagNameGen), mindboxBodyGen) { (flags, mindboxBody) =>
              val updatedMindboxBody =
                mindboxBody.focus(_.flags).modify(_.filterNot { case (flagName, _) => flags.contains(flagName) })
              val expectedNotification = expectedNotificationForFlag(updatedMindboxBody, None, None)

              for {
                pushComposer <- ZIO.service[SpamalotExperimentPushComposer.Service]
                notification <- pushComposer.composeSpamalotPush(updatedMindboxBody, flags)
              } yield assert(notification)(equalTo(expectedNotification))
            }
          }
        }
      )
    ).provideSomeLayer[TestEnvironment](layer) @@ TestAspect.sized(10) @@ TestAspect.samples(100)

  private def expectedNotificationForFlag(
      mindboxBody: MindboxBody,
      flagData: Option[FlagPushData],
      flagName: Option[ExpFlag]): Notification =
    Notification(
      topic = mindboxBody.topic,
      name = Some(flagName.map(fn => s"${mindboxBody.pushName}-$fn").getOrElse(mindboxBody.pushName)),
      content = Notification.Content.Payload(
        Payload(
          title = flagData.flatMap(_.title).getOrElse(mindboxBody.main.title),
          body = flagData.flatMap(_.body).getOrElse(mindboxBody.main.body),
          media = flagData
            .flatMap(_.media.map(_.map(mindboxMediaToSpamalotMedia)))
            .getOrElse(mindboxBody.main.media.map(mindboxMediaToSpamalotMedia)),
          userActions = Seq(),
          action = Some(
            flagData
              .flatMap(_.action.map(actionToSpamalotAction))
              .getOrElse(actionToSpamalotAction(mindboxBody.main.action))
          ),
          categoryId = flagData.flatMap(_.categoryId).getOrElse(mindboxBody.main.categoryId)
        )
      ),
      `object` = None
    )

  private def mindboxMediaToSpamalotMedia(media: PushMediaData): Payload.Media =
    media match {
      case PushMediaData.Image(aliases, groupId, imageName, namespace) =>
        Payload.Media(
          media = Payload.Media.Media.Image(
            value = AvatarsImage(
              groupId = groupId,
              name = imageName,
              namespace = namespace,
              aliases = aliases
            )
          )
        )
      case PushMediaData.Video(videoUrl) =>
        Payload.Media(
          media = Payload.Media.Media.Video(value = Payload.Media.Video(url = videoUrl))
        )
      case PushMediaData.SimpleImage(url) =>
        Payload.Media(
          media = Payload.Media.Media.SimpleImage(value = Payload.Media.SimpleImage(url = url))
        )
    }

  private def actionToSpamalotAction(action: Action): Payload.Action =
    Payload.Action(
      url = Some(
        Payload.Action.PlatformUrl(
          android = action.url.android,
          ios = action.url.ios
        )
      ),
      payload = Map.empty
    )

  private val layer = ZLayer.wire[SpamalotExperimentPushComposer](
    Logging.live,
    LiveSpamalotExperimentPushComposer.layer
  )
}
