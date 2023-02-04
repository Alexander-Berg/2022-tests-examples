package ru.auto.api.managers.chat

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.BaseSpec
import ru.auto.api.model.ModelGenerators.chatUserGen

class RoomableSpec extends BaseSpec with ScalaCheckPropertyChecks {
  "defaultTitle" when {
    "has one fullName" should {
      "return it" in {
        forAll(chatUserGen.suchThat(_.getProfile.getFullName.nonEmpty)) { user =>
          Roomable.defaultTitle(CleanChatUsers(Set(user), None)) shouldBe user.getProfile.getFullName
        }
      }
    }

    "has at least two fullNames" should {
      "return joined and sorted names" in {
        forAll(Gen.listOfN(2, chatUserGen.suchThat(_.getProfile.getFullName.nonEmpty))) { users =>
          Roomable.defaultTitle(CleanChatUsers(users.toSet, None)) shouldBe
            users.map(_.getProfile.getFullName).sorted.mkString(", ")
        }
      }
    }

    "has zero users with fullName" should {
      "return default chat name" in {
        Roomable.defaultTitle(CleanChatUsers(Set.empty, None)) shouldBe Roomable.defaultTitleNoFullNames
      }
    }
  }
}
