package ru.yandex.realty.converters

import org.junit.runner.RunWith
import org.scalacheck.ShrinkLowPriority
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.realty.SpecBase
import ru.yandex.realty.model.gen.ProtobufMessageGenerators
import ru.yandex.vertis.protobuf.ProtoInstanceProvider
import ru.yandex.realty.vos.model.user.{User => VosUser}
import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class VosUserToApiUserConverterSpec
  extends SpecBase
  with ProtobufMessageGenerators
  with ProtoInstanceProvider
  with ShrinkLowPriority
  with PropertyChecks {

  "VosUserToApiUserConverter" should {
    "convert correctly" in {
      val generator = generate[VosUser]()
      forAll(generator) { vosUser =>
        val apiUser = VosUserToApiUserConverter.fromVos(vosUser)

        apiUser.getId shouldBe vosUser.getId
        apiUser.getTimestamps shouldBe vosUser.getTimestamps
        apiUser.getUserStatus shouldBe vosUser.getUserStatus

        val apiUserSettings = apiUser.getUserSettings
        val vosUserSettings = vosUser.getUserSettings
        apiUserSettings.getRedirectPhones shouldBe vosUserSettings.getRedirectPhones

        val apiUserContacts = apiUser.getUserContacts
        val vosUserContacts = vosUser.getUserContacts
        apiUserContacts.getName shouldBe vosUserContacts.getName
        apiUserContacts.getEmail shouldBe vosUserContacts.getEmail
        apiUserContacts.getOrganization shouldBe vosUserContacts.getOrganization
        apiUserContacts.getOgrn shouldBe vosUserContacts.getOgrn
        apiUserContacts.getAgencyId shouldBe vosUserContacts.getAgencyId
        apiUserContacts.getUrl shouldBe vosUserContacts.getUrl
        apiUserContacts.getPhotoUrl shouldBe vosUserContacts.getPhotoUrl

        val apiPhones = apiUserContacts.getPhonesList
        val vosPhones = vosUserContacts.getPhonesList

        apiPhones.size shouldBe vosPhones.size
        apiPhones.asScala.zip(vosPhones.asScala).foreach {
          case (apiPhone, vosPhone) =>
            apiPhone shouldBe vosPhone
        }

        val apiUserInfo = apiUser.getUserInfo
        val vosUserInfo = vosUser.getUserInfo

        apiUserInfo.getUserSource shouldBe vosUserInfo.getUserSource
        apiUserInfo.getPaymentType shouldBe vosUserInfo.getPaymentType
        apiUserInfo.getUserType shouldBe vosUserInfo.getUserType
      }
    }
  }
}
