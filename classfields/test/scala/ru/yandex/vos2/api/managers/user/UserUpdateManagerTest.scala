package ru.yandex.vos2.api.managers.user

import com.google.protobuf.BoolValue
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.geocoder.LocationUnifierService
import ru.yandex.realty.model.location.Location
import ru.yandex.realty.model.region.NodeRgid
import ru.yandex.realty.proto.offer.PaymentType
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.vos.model.agency.AgencyProfile
import ru.yandex.realty.vos.model.user.{UserInfo, UserType, UserUpdate}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vos2.UserModel
import ru.yandex.vos2.dao.offers.OfferDao
import ru.yandex.vos2.dao.users.DuplicateOgrnException
import ru.yandex.vos2.model.UserRef
import ru.yandex.vos2.realty.dao.users.RealtyUserDao

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * @author pnaydenov
  */
@RunWith(classOf[JUnitRunner])
class UserUpdateManagerTest extends WordSpec with MockitoSupport with ScalaFutures with Matchers {
  import org.mockito.ArgumentMatchers.{eq => Eq}

  private val DefaultPatienceConfig =
    PatienceConfig(Span(1, Seconds), Span(100, Millis))

  implicit override def patienceConfig: PatienceConfig =
    DefaultPatienceConfig

  abstract class Fixture {
    val realtyUserDao = mock[RealtyUserDao]
    val offerDao = mock[OfferDao]
    val locationUnifierService = mock[LocationUnifierService]
    val userRef = UserRef(123)

    when(offerDao.revisitUsersOffers(?, ?)(?)).thenReturn(0)

    val userUpdate = UserUpdate
      .newBuilder()
      .setAgencyProfile(AgencyProfile.newBuilder().setOgrn("12345").setName("Foo Bar"))
      .setAgencyProfileEnabled(BoolValue.newBuilder().setValue(true))
      .setUserInfo(UserInfo.newBuilder().setUserType(UserType.AGENCY).setPaymentType(PaymentType.PT_JURIDICAL_PERSON))
      .build()

    val userUpdateWithAgencyAddress = UserUpdate
      .newBuilder()
      .setAgencyProfile(
        AgencyProfile
          .newBuilder()
          .setOgrn("12345")
          .setName("Foo Bar")
          .setAddress(AgencyProfile.Address.newBuilder().setUnifiedAddress("update address"))
      )
      .setAgencyProfileEnabled(BoolValue.newBuilder().setValue(true))
      .setUserInfo(UserInfo.newBuilder().setUserType(UserType.AGENCY).setPaymentType(PaymentType.PT_JURIDICAL_PERSON))
      .build()

    implicit val trace = Traced.empty

    def manager: UserUpdateManager = new UserUpdateManager(realtyUserDao, offerDao, locationUnifierService)

    lazy val existingUser = UserModel.User
      .newBuilder()
      .setUserRef("uid_123")
      .setCurrentAgencyProfile(
        UserModel.AgencyProfile
          .newBuilder()
          .setName("Foo Bar")
          .setOgrn("12345")
          .addSparkDocUrls("http://ogrn12345")
      )
      .build()
  }

  lazy val existingUserWithRgid = UserModel.User
    .newBuilder()
    .setUserRef("uid_123")
    .setApprovedAgencyProfile(
      UserModel.AgencyProfile
        .newBuilder()
        .setName("Foo Bar with rgid")
        .setOgrn("12345")
        .addSparkDocUrls("http://ogrn12345")
        .setAddress(
          UserModel.AgencyProfile.Address
            .newBuilder()
            .setUnifiedAddress("kazan address")
            .setRgid(NodeRgid.KAZAN)
            .build()
        )
        .build()
    )
    .setCurrentAgencyProfile(
      UserModel.AgencyProfile
        .newBuilder()
        .setName("Foo Bar")
        .setOgrn("12345")
        .addSparkDocUrls("http://ogrn12345")
    )
    .build()

  "UserUpdateManager" should {

    "check OGRN uniqueness" in new Fixture {
      when(realtyUserDao.isApprovedAgencyOgrnExists(Eq("12345"), ?)).thenReturn(false)
      when(realtyUserDao.isApprovedAgencyNameExists(?, ?)).thenReturn(false)
      when(realtyUserDao.find(?, ?)).thenReturn(None)
      doNothing.when(realtyUserDao).update(?, ?)

      val resp = manager.updateUser(userRef, userUpdate).futureValue
      verify(realtyUserDao).isApprovedAgencyOgrnExists(?, ?)
    }

    "check OGRN uniqueness and fall" in new Fixture {
      when(realtyUserDao.isApprovedAgencyOgrnExists(Eq("12345"), ?)).thenReturn(true)
      when(realtyUserDao.isApprovedAgencyNameExists(?, ?)).thenReturn(false)

      manager.updateUser(userRef, userUpdate).failed.futureValue shouldBe a[DuplicateOgrnException]
    }

    "not check Agent name uniquiness" in new Fixture {
      when(realtyUserDao.isApprovedAgencyOgrnExists(?, ?)).thenReturn(false)
      when(realtyUserDao.isApprovedAgencyNameExists(?, ?)).thenReturn(false)
      when(realtyUserDao.find(?, ?)).thenReturn(None)
      doNothing.when(realtyUserDao).update(?, ?)

      val agentUserUpdate = userUpdate.toBuilder
      agentUserUpdate.getUserInfoBuilder.setUserType(UserType.AGENT)
      val resp = manager.updateUser(userRef, agentUserUpdate.build()).futureValue
      verify(realtyUserDao).isApprovedAgencyOgrnExists(?, ?)
      verify(realtyUserDao, never()).isApprovedAgencyNameExists(?, ?)
    }

    "not check name uniquiness if trademark docs provided" in new Fixture {
      when(realtyUserDao.isApprovedAgencyOgrnExists(?, ?)).thenReturn(false)
      when(realtyUserDao.isApprovedAgencyNameExists(?, ?)).thenReturn(false)
      when(realtyUserDao.find(?, ?)).thenReturn(None)
      doNothing.when(realtyUserDao).update(?, ?)

      val agentUserUpdate = userUpdate.toBuilder
      agentUserUpdate.addAgencyProfileTrademarkDocUrls("http://trademark.com")
      val resp = manager.updateUser(userRef, agentUserUpdate.build()).futureValue
      verify(realtyUserDao).isApprovedAgencyOgrnExists(?, ?)
      verify(realtyUserDao, never()).isApprovedAgencyNameExists(?, ?)
    }

    "ignore non Agent or Agency public profile" in new Fixture {
      when(realtyUserDao.find(?, ?)).thenReturn(None)
      doNothing.when(realtyUserDao).update(?, ?)

      val nonAgencyUserUpdate = userUpdate.toBuilder
      nonAgencyUserUpdate.getUserInfoBuilder.setUserType(UserType.OWNER)
      val resp = manager.updateUser(userRef, nonAgencyUserUpdate.build()).futureValue
    }

    "not check uniquiness for disabled profiles" in new Fixture {
      when(realtyUserDao.find(?, ?)).thenReturn(None)
      doNothing.when(realtyUserDao).update(?, ?)

      val disableProfileUpdate = userUpdate.toBuilder
      disableProfileUpdate.clearAgencyProfileEnabled()
      val resp = manager.updateUser(userRef, disableProfileUpdate.build()).futureValue

      verify(realtyUserDao, never()).isApprovedAgencyOgrnExists(?, ?)
      verify(realtyUserDao, never()).isApprovedAgencyNameExists(?, ?)
    }

    "clear preceding OGRN-related docs" in new Fixture {
      when(realtyUserDao.find(?, ?)).thenReturn(Some(existingUser))
      when(realtyUserDao.isApprovedAgencyOgrnExists(?, ?)).thenReturn(false)
      when(realtyUserDao.isApprovedAgencyNameExists(?, ?)).thenReturn(false)
      val savedUser = ArgumentCaptor.forClass[UserModel.User, UserModel.User](classOf[UserModel.User])
      doNothing.when(realtyUserDao).update(savedUser.capture(), ?)

      val newOgrnProfileUpdate = userUpdate.toBuilder
      newOgrnProfileUpdate.getAgencyProfileBuilder.setOgrn("22222")

      val resp = manager.updateUser(userRef, newOgrnProfileUpdate.build()).futureValue
      savedUser.getValue.getCurrentAgencyProfile.getSparkDocUrlsList.asScala shouldBe empty
    }

    "keep relevant OGRN-related docs" in new Fixture {
      when(realtyUserDao.find(?, ?)).thenReturn(Some(existingUser))
      when(realtyUserDao.isApprovedAgencyOgrnExists(?, ?)).thenReturn(false)
      when(realtyUserDao.isApprovedAgencyNameExists(?, ?)).thenReturn(false)
      val savedUser = ArgumentCaptor.forClass[UserModel.User, UserModel.User](classOf[UserModel.User])
      doNothing.when(realtyUserDao).update(savedUser.capture(), ?)

      val resp = manager.updateUser(userRef, userUpdate).futureValue
      savedUser.getValue.getCurrentAgencyProfile.getSparkDocUrlsList.asScala.toList shouldEqual List("http://ogrn12345")
    }

    "update agency with rgid " in new Fixture {
      when(realtyUserDao.find(?, ?)).thenReturn(Some(existingUserWithRgid))
      when(realtyUserDao.isApprovedAgencyOgrnExists(?, ?)).thenReturn(false)
      when(realtyUserDao.isApprovedAgencyNameExists(?, ?)).thenReturn(false)

      private val savedUser = ArgumentCaptor.forClass[UserModel.User, UserModel.User](classOf[UserModel.User])
      doNothing.when(realtyUserDao).update(savedUser.capture(), ?)

      manager.updateUser(userRef, userUpdateWithAgencyAddress).futureValue
      savedUser.getValue.getApprovedAgencyProfile.getAddress.getRgid shouldBe NodeRgid.KAZAN
      savedUser.getValue.getCurrentAgencyProfile.getAddress.getRgid shouldBe NodeRgid.KAZAN
    }

    "update agency without rgid " in new Fixture {
      val location = new Location()
      location.setRegionGraphId(NodeRgid.SPB)
      when(locationUnifierService.resolveLocation(?, ?, ?)(?)).thenReturn(location)

      when(realtyUserDao.find(?, ?)).thenReturn(Some(existingUser))
      when(realtyUserDao.isApprovedAgencyOgrnExists(?, ?)).thenReturn(false)
      when(realtyUserDao.isApprovedAgencyNameExists(?, ?)).thenReturn(false)

      private val savedUser = ArgumentCaptor.forClass[UserModel.User, UserModel.User](classOf[UserModel.User])
      doNothing.when(realtyUserDao).update(savedUser.capture(), ?)

      manager.updateUser(userRef, userUpdateWithAgencyAddress).futureValue
      savedUser.getValue.getCurrentAgencyProfile.getAddress.getRgid shouldBe NodeRgid.SPB
    }
  }
}
