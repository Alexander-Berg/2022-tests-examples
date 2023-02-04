package ru.yandex.vos2.realty.dao.users.converter

import com.google.protobuf.util.Timestamps
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.vos2.UserModel.AgencyProfile.Address
import ru.yandex.vos2.UserModel.User.AlternativeIds
import ru.yandex.vos2.UserModel.{AgencyProfile, PaymentType, User, UserContacts, UserPhone, UserType}
import ru.yandex.realty.proto.CommunicationChannels

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class OldApiUserWithVosUserMergerSpec extends FlatSpec with Matchers {

  val existingUser = User
    .newBuilder()
    .setUserRef("1")
    .setUserType(UserType.UT_OWNER)
    .setAlternativeIds(AlternativeIds.newBuilder().setExternal(1234))
    .setPhotoUrl("url1")
    .setTimestampUpdate(1234)
    .setLicenseAccept(false)
    .setCallCenter(true)
    .setRedirectPhones(false)
    .setPaymentType(PaymentType.NATURAL_PERSON)
    .setUserContacts(
      UserContacts
        .newBuilder()
        .setName("name1")
        .setUrl("url1")
        .setAgencyId("agency1")
        .setOgrn("ogrn1")
        .setEmail("email1")
        .setOrganization("organization1")
        .addPhones(UserPhone.newBuilder().setNumber("+79999999999"))
        .addAllowedCommunicationChannels(CommunicationChannels.COM_CALLS)
    )
    .setAgencyProfileEnabled(true)
    .setApprovedAgencyProfile(
      AgencyProfile
        .newBuilder()
        .setUserType(UserType.UT_AGENT)
        .setOgrn("123131")
        .setLogoUrl("logo")
        .setFoundationDate(Timestamps.fromMillis(1568384205911L))
        .setDescription("description")
        .setAddress(Address.newBuilder().setUnifiedAddress("address"))
        .addAllPhones(Seq(UserPhone.newBuilder().setNumber("+79999999999").build()).asJava)
    )
    .build()

  val userUpdate = User
    .newBuilder()
    .setUserRef("1")
    .setUserType(UserType.UT_AGENT)
    .setAlternativeIds(AlternativeIds.newBuilder().setExternal(123))
    .setPhotoUrl("url2")
    .setTimestampUpdate(123456)
    .setLicenseAccept(true)
    .setCallCenter(false)
    .setRedirectPhones(true)
    .setPaymentType(PaymentType.JURIDICAL_PERSON)
    .setUserContacts(
      UserContacts
        .newBuilder()
        .setName("name2")
        .setUrl("url2")
        .setAgencyId("agency2")
        .setOgrn("ogrn2")
        .setEmail("email2")
        .setOrganization("organization2")
        .addPhones(UserPhone.newBuilder().setNumber("+78888888888"))
        .addAllowedCommunicationChannels(CommunicationChannels.COM_CHATS)
    )
    .build()

  "OldApiUserWithVosUserMerger" should "merge users correctly" in {
    val result = OldApiUserWithVosUserMerger.mergeUsers(userUpdate, existingUser)
    result.getUserType shouldBe userUpdate.getUserType
    result.getCapaUser shouldBe userUpdate.getCapaUser
    result.getAlternativeIds shouldBe userUpdate.getAlternativeIds
    result.getPhotoUrl shouldBe userUpdate.getPhotoUrl
    result.getLicenseAccept shouldBe userUpdate.getLicenseAccept
    result.getCallCenter shouldBe userUpdate.getCallCenter
    result.getRedirectPhones shouldBe userUpdate.getRedirectPhones
    result.getPaymentType shouldBe userUpdate.getPaymentType
    result.getUserContacts shouldBe userUpdate.getUserContacts
    result.getAgencyProfileEnabled shouldBe existingUser.getAgencyProfileEnabled
    result.getApprovedAgencyProfile shouldBe existingUser.getApprovedAgencyProfile
  }
}
