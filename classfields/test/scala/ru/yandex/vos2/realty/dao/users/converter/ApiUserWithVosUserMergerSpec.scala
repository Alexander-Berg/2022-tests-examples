package ru.yandex.vos2.realty.dao.users.converter

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.proto.Phone
import ru.yandex.realty.proto.offer.PaymentType
import ru.yandex.realty.vos.model.agency.AgencyProfile
import ru.yandex.realty.vos.model.user.{
  UserInfo,
  UserSettings,
  UserSource,
  UserType,
  UserUpdate,
  UserContacts => ExternalUserContacts
}
import ru.yandex.vos2.BasicsModel.TrustLevel
import ru.yandex.vos2.UserModel
import ru.yandex.vos2.UserModel.{User, UserPhone, UserContacts => InnerUserContacts}
import ru.yandex.vos2.model.UserRefUID

import scala.collection.JavaConverters._

@RunWith(classOf[JUnitRunner])
class ApiUserWithVosUserMergerSpec extends FlatSpec with Matchers {

  val oldPhones = List(
    UserPhone.newBuilder().setNumber("OLD_NUMBER_1").setTimestampAdded(111111L).setLegacyId(123).build(),
    UserPhone.newBuilder().setNumber("OLD_NUMBER_2").setTimestampAdded(222222L).build()
  )

  val oldUserContacts: InnerUserContacts = InnerUserContacts
    .newBuilder()
    .setName("old_name")
    .setOrganization("old_organization")
    .setAgencyId("old_agency_id")
    .setOgrn("old_ogrn")
    .addAllPhones(oldPhones.asJava)
    .setEmail("old_email")
    .setUrl("old_url")
    .build()

  val oldUser: User = User
    .newBuilder()
    .setCallCenter(false)
    .setCapaUser(false)
    .setUserRef("uid_1")
    .setPhotoUrl("old_photo_url")
    .setTimestampCreate(0)
    .setTimestampUpdate(1)
    .setTrustLevel(TrustLevel.TL_HIGH)
    .setUserContacts(oldUserContacts)
    .setPaymentType(UserModel.PaymentType.NATURAL_PERSON)
    .build()

  "ApiUserWithVosUserMerger" should "merge users correctly, when new user has new fields" in {
    val userInfo = UserInfo
      .newBuilder()
      .setPaymentType(PaymentType.PT_NATURAL_PERSON)
      .setUserType(UserType.OWNER)
      .setUserSource(UserSource.DEFAULT)
      .build()

    val userContacts = ExternalUserContacts
      .newBuilder()
      .setAgencyId("new_agency_id")
      .setEmail("new_email")
      .setUrl("new_url")
      .setPhotoUrl("new_photo_url")
      .setName("old_name")
      .setOrganization("old_organization")
      .setOgrn("old_ogrn")
      .build()

    val userSettings = UserSettings
      .newBuilder()
      .setRedirectPhones(false)
      .build()

    val newUpdate = UserUpdate
      .newBuilder()
      .setUserInfo(userInfo)
      .setUserContacts(userContacts)
      .setUserSettings(userSettings)
      .build()

    val mergeResult = ApiUserWithVosUserMerger.mergeUsers(UserRefUID(42), newUpdate, Some(oldUser))

    require(mergeResult.getTimestampUpdate > 1)
    require(mergeResult.getTimestampCreate == 0)
    require(mergeResult.getTrustLevel == TrustLevel.TL_HIGH)
    val mergedContacts = mergeResult.getUserContacts
    require(mergedContacts.getPhonesList.isEmpty)
    require(mergeResult.getPhotoUrl == "new_photo_url")
    require(mergedContacts.getName == "old_name")
    require(mergedContacts.getOrganization == "old_organization")
    require(mergedContacts.getAgencyId == "new_agency_id")
    require(mergedContacts.getOgrn == "old_ogrn")
    require(mergedContacts.getEmail == "new_email")
    require(mergedContacts.getUrl == "new_url")
  }

  it should "merge correctly, when no new information is given" in {
    val userInfo = UserInfo
      .newBuilder()
      .setPaymentType(PaymentType.PT_NATURAL_PERSON)
      .build()

    val newPhones = oldPhones.map { phone =>
      Phone
        .newBuilder()
        .setWholePhoneNumber(phone.getNumber)
        .build()
    }

    val userContacts = ExternalUserContacts
      .newBuilder()
      .addAllPhones(newPhones.asJava)
      .setAgencyId("old_agency_id")
      .setEmail("old_email")
      .setUrl("old_url")
      .setPhotoUrl("old_photo_url")
      .setName("old_name")
      .setOrganization("old_organization")
      .setOgrn("old_ogrn")
      .build()

    val userSettings = UserSettings
      .newBuilder()
      .build()

    val newUpdate = UserUpdate
      .newBuilder()
      .setUserInfo(userInfo)
      .setUserContacts(userContacts)
      .setUserSettings(userSettings)
      .build()

    val mergeResult = ApiUserWithVosUserMerger.mergeUsers(UserRefUID(42), newUpdate, Some(oldUser))

    require(mergeResult.getTimestampUpdate == 1)
    require(mergeResult.getTimestampCreate == 0)
    require(mergeResult.getTrustLevel == TrustLevel.TL_HIGH)
    val mergedContacts = mergeResult.getUserContacts
    require(mergedContacts.getPhonesList.asScala.toList == oldPhones)
    require(mergeResult.getPhotoUrl == "old_photo_url")
    require(mergedContacts.getName == "old_name")
    require(mergedContacts.getOrganization == "old_organization")
    require(mergedContacts.getAgencyId == "old_agency_id")
    require(mergedContacts.getOgrn == "old_ogrn")
    require(mergedContacts.getEmail == "old_email")
    require(mergedContacts.getUrl == "old_url")
  }

  it should "apply any first profile update" in {
    val newUpdate = UserUpdate
      .newBuilder()
      .setUserInfo(UserInfo.newBuilder().setPaymentType(PaymentType.PT_JURIDICAL_PERSON))
      .setAgencyProfile(AgencyProfile.newBuilder().setOgrn("123"))
      .build()

    val mergeResult = ApiUserWithVosUserMerger.mergeUsers(UserRefUID(42), newUpdate, Some(oldUser))
    mergeResult.getCurrentAgencyProfile.getOgrn shouldBe "123"
  }

  it should "apply changed profile update" in {
    val newUpdate = UserUpdate
      .newBuilder()
      .setUserInfo(UserInfo.newBuilder().setPaymentType(PaymentType.PT_JURIDICAL_PERSON))
      .setAgencyProfile(AgencyProfile.newBuilder().setOgrn("123"))
      .build()
    val userBuilder = oldUser.toBuilder
    userBuilder.getApprovedAgencyProfileBuilder.setOgrn("1234567")

    val mergeResult = ApiUserWithVosUserMerger.mergeUsers(UserRefUID(42), newUpdate, Some(oldUser))
    mergeResult.getCurrentAgencyProfile.getOgrn shouldBe "123"
  }

  it should "ignore unchanged profile update" in {
    val newUpdate = UserUpdate
      .newBuilder()
      .setUserInfo(UserInfo.newBuilder().setPaymentType(PaymentType.PT_JURIDICAL_PERSON))
      .setAgencyProfile(
        AgencyProfile.newBuilder().setOgrn("123").addPhones(Phone.newBuilder().setWholePhoneNumber("2128506"))
      )
      .build()

    val userWithAppliedProfile =
      ApiUserWithVosUserMerger.mergeUsers(UserRefUID(42), newUpdate, Some(oldUser))

    val approvedUserBuilder = userWithAppliedProfile.toBuilder
    approvedUserBuilder.setApprovedAgencyProfile(userWithAppliedProfile.getCurrentAgencyProfile)
    approvedUserBuilder.getApprovedAgencyProfileBuilder.getPhonesBuilder(0).setTimestampAdded(549939600000L)
    approvedUserBuilder.clearCurrentAgencyProfile()

    val mergeResult = ApiUserWithVosUserMerger.mergeUsers(UserRefUID(42), newUpdate, Some(approvedUserBuilder.build()))
    mergeResult.hasCurrentAgencyProfile shouldBe false
  }

  it should "add user type from update to profile" in {
    val newUpdate = UserUpdate
      .newBuilder()
      .setUserInfo(UserInfo.newBuilder().setPaymentType(PaymentType.PT_JURIDICAL_PERSON).setUserType(UserType.AGENT))
      .setAgencyProfile(AgencyProfile.newBuilder().setOgrn("123"))
      .build()

    val agencyUserType = oldUser.toBuilder
    agencyUserType.setUserType(UserModel.UserType.UT_AGENCY)

    val mergeResult = ApiUserWithVosUserMerger.mergeUsers(UserRefUID(42), newUpdate, Some(agencyUserType.build()))
    mergeResult.getCurrentAgencyProfile.getUserType shouldBe UserModel.UserType.UT_AGENT
  }
}
