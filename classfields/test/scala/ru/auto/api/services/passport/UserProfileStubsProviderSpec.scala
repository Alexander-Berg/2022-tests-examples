package ru.auto.api.services.passport

import ru.auto.api.BaseSpec
import ru.auto.api.services.passport.util.UserProfileStubsProvider
import ru.auto.api.util.Resources

//scalastyle:off line.size.limit
class UserProfileStubsProviderSpec extends BaseSpec {
  var stubsProvider: UserProfileStubsProvider = null

  "UserProfileStubsProvider" should {
    "be parsed correctly" in {
      stubsProvider = Resources.open("/user_profile_stubs.json")(UserProfileStubsProvider.parse)
    }
    "provide fullName" in {
      stubsProvider.fullName(1) shouldBe "Оранжевый лимузин"
    }
    "provide fullName if original is empty and alias is a stub" in {
      stubsProvider.login(1, "", "id1") shouldBe "Оранжевый лимузин"
    }
    "return original fullName if it's set" in {
      stubsProvider.login(1, "Full Name", "id1") shouldBe "Full Name"
    }
    "return alias if it's not a stub" in {
      stubsProvider.login(1, "", "Alias") shouldBe "Alias"
    }
    "provide userpic" in {
      stubsProvider.userpic(1, "alias") shouldBe "//avatars.mds.yandex.net/get-autoru-users/40165/cfaf99e4ee69f58834ae222ac8f81455/alias"
    }
    "provide userpicMap" in {
      val sizesMap = stubsProvider.userpicSizes(1)
      sizesMap.keys should contain theSameElementsAs Seq("24x24", "100x100", "430x600", "48x48", "200x200")
    }
  }
}
