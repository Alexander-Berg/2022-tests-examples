package vertis.spamalot

import vertistraf.common.pushnoy.client.model.ImagesConfig

/** @author kusaeva
  */
trait TestImagesConfig {

  protected lazy val imagesConfig =
    ImagesConfig(
      namespace = "test",
      url = "avatars.mdst.yandex.net",
      aliases = Seq("alias1", "alias2"),
      aliasForPush = ""
    )
}
