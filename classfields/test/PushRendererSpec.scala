package vertistraf.common.pushnoy.client

import io.circe.syntax._
import vertis.zio.test.ZioSpecBase
import vertistraf.common.pushnoy.client.model.{PushContent, TopicName}
import vertistraf.common.pushnoy.client.render.PayloadOps._
import vertistraf.common.pushnoy.client.render.Platforms._
import vertistraf.common.pushnoy.client.render.{ContentPayload, Repack}
import vertistraf.common.pushnoy.client.render.apns.{Alert, Apns, Aps}
import vertistraf.common.pushnoy.client.render.gcm.Gcm

/** @author kusaeva
  */
class PushRendererSpec extends ZioSpecBase with PushRendererSupport {

  "PushRenderer" should {
    "render payload to push" in ioTest {

      val expected = PushContent(
        payload = ContentPayload(
          title = title,
          body = body,
          androidChannel = TopicName(topic, topicName),
          topic = topic,
          imageUrl = Some(pushImageUrl),
          androidPushId = notificationId,
          pushName = Some(pushName),
          notificationObject = notificationObject,
          iosAction = Some(action.forPlatform(Ios)),
          androidAction = Some(action.forPlatform(Android)),
          iosActions = actions.map(_.forPlatform(Ios)),
          androidActions = actions.map(_.forPlatform(Android))
        ),
        event = pushName,
        repack = Some(
          Repack(
            apns = Some(
              Apns(
                aps = Aps(
                  alert = Alert.Compound(title = Some(title), body = body),
                  badge = Some(1),
                  sound = Some("default"),
                  category = Some(iosCategoryId)
                ),
                collapseId = notificationId.toString
              )
            ),
            gcm = Some(Gcm())
          )
        ),
        appVersion = appVersionLimit
      )

      for {
        rendered <- render
        _ <- logger.info(s"${rendered.asJson}")
        _ <- check(rendered shouldBe expected)
      } yield ()
    }
  }
}
