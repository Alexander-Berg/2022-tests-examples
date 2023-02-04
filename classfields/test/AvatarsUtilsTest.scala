package auto.carfax.common.clients.avatars

import auto.carfax.common.clients.avatars.AvatarsUtils
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.internal.Mds.MdsPhotoInfo

class AvatarsUtilsTest extends AnyWordSpec with Matchers {
  import AvatarsUtilsTest._

  "convertUriToMdsPhotoInfo" should {
    "handle good link" in {
      AvatarsUtils.convertUriToMdsPhotoInfo(goodLink) should be(correctMdsPhotoInfo)
    }

    "handle good link with slash" in {
      AvatarsUtils.convertUriToMdsPhotoInfo(goodLinkWithSlash) should be(correctMdsPhotoInfo)
    }

    "handle bad link" in {
      AvatarsUtils.convertUriToMdsPhotoInfo(badLinkWithSize) should be(None)
    }

    "handle bad link with incomplete data" in {
      AvatarsUtils.convertUriToMdsPhotoInfo(badLinkWithIncompleteData) should be(None)
    }
  }
}

object AvatarsUtilsTest {
  val namespace = "testnamespace"
  val groupId = 12345
  val imageName = "testimagename"

  val goodLink = s"//avatars.mds.yandex.net/get-$namespace/$groupId/$imageName"
  val goodLinkWithSlash: String = goodLink + "/"
  val badLinkWithSize = s"//avatars.mds.yandex.net/get-$namespace/$groupId/$imageName/300x300"
  val badLinkWithIncompleteData = s"//avatars.mds.yandex.net/get-$namespace/$groupId"

  val correctMdsPhotoInfo: Option[MdsPhotoInfo] = Some(
    MdsPhotoInfo
      .newBuilder()
      .setNamespace(namespace)
      .setGroupId(groupId)
      .setName(imageName)
      .build()
  )
}
