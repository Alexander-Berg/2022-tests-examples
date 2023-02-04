package vertistraf.common.pushnoy.client

import ru.yandex.vertis.image.avatars_image.AvatarsImage
import ru.yandex.vertis.spamalot.core.AppVersionLimit
import ru.yandex.vertis.spamalot.core.AppVersionLimit.AppVersionCompare
import ru.yandex.vertis.spamalot.model.Payload.Action.PlatformUrl
import ru.yandex.vertis.spamalot.model.Payload.Media.Media.{Image, Video}
import ru.yandex.vertis.spamalot.model.Payload.{Action, Media, UserAction}
import ru.yandex.vertis.spamalot.model.{NotificationObject, Payload, PushIds}
import vertistraf.common.pushnoy.client.mocks.TestTopicNameResolver
import vertistraf.common.pushnoy.client.model.{ImagesConfig, PushContent}
import vertistraf.common.pushnoy.client.render.PushRendererImpl
import zio._

/** old push example: https://paste.yandex-team.ru/1077954/text
 *
 * @author kusaeva
 */
trait PushRendererSupport {

  private val avatarsImage =
    AvatarsImage(namespace = "autoru-vos", groupId = "2106781", name = "0424288c1373bbe35d317afb069eb842")
  private val aliasForPush = "1200x900"

  private val topicNameResolver = new TestTopicNameResolver

  private val imagesConfig =
    ImagesConfig(
      namespace = avatarsImage.namespace,
      url = "avatars.mdst.yandex.net",
      aliases = Nil,
      aliasForPush = aliasForPush
    )

  private val renderer = new PushRendererImpl(imagesConfig, topicNameResolver)

  protected val pushImageUrl: String =
    s"//${imagesConfig.url}/get-${avatarsImage.namespace}/${avatarsImage.groupId}/${avatarsImage.name}/$aliasForPush"

  val notificationId: Int = -123456

  val appVersionLimit =
    Some(AppVersionLimit(ios = Some("1.0.0"), android = Some("1.1.0"), compare = AppVersionCompare.LT))

  lazy val render: Task[PushContent] =
    renderer.render(
      payload = Payload(
        title = title,
        body = body,
        media = media,
        userActions = actions,
        action = Some(action),
        categoryId = iosCategoryId,
        appVersionLimit = appVersionLimit
      ),
      pushName = Some(pushName),
      topic = topic,
      pushIds = Some(PushIds(android = notificationId, ios = notificationId.toString)),
      notificationObject = notificationObject
    )

  val title = "ГАЗ 21 «Волга», 1969"
  val body = "2 000 000 ₽ / 1 000 км посмотреть объявление"
  val pushName = "offer_add"
  val notificationObject = Some(NotificationObject(id = "1111", `type` = "offer"))
  val topic = "personal_recommendations"
  val topicName = "Персональные рекомендации"
  val iosCategoryId = "ios_category_id"

  val action: Action = Action(
    url = Some(PlatformUrl(android = "android_main_url", ios = "ios_main_url", web = ""))
  )

  val image: Media = Media(
    media = Image(value = avatarsImage)
  )

  val video: Media = Media(
    media = Video(value = Media.Video(url = "http://youtube.com/kitties"))
  )

  val urlAction: UserAction = UserAction(
    text = "Open",
    action =
      Some(Action(url = Some(PlatformUrl(android = "android_specific_url", ios = "ios_specific_url", web = "url"))))
  )

  val payloadAction: UserAction = UserAction(
    text = "Go to",
    action = Some(
      Action(
        url = Some(PlatformUrl(android = "android_specific_url", ios = "ios_specific_url", web = "url")),
        payload = Map("type" -> "action_type", "offer" -> "id1234", "random" -> "random_text")
      )
    )
  )
  val media = Seq(image, video)
  val actions = Seq(urlAction, payloadAction)
}
