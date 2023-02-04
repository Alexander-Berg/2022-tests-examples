package ru.yandex.vos2.api.managers.user

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.api.ProtoResponse
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.vos.model.user.User.Feature
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.UserModel.{User, UserType}
import ru.yandex.vos2.api.managers.user.UserInfoManager.{FiltersForRent, FiltersForSale}
import ru.yandex.vos2.api.model.Paging
import ru.yandex.vos2.dao.users.UserDao
import ru.yandex.vos2.model.{UserRef, UserRefRaw}

import scala.concurrent.Future

@RunWith(classOf[JUnitRunner])
class UserInfoManagerSpec extends AsyncSpecBase {

  implicit private val traced = Traced.empty
  implicit private val features = Set.empty[Feature]

  "UserInfoManagerSpec" should {
    "set mosRu available flag if skipMosRuAvailabilityCheck = false and userOffersManager returns offers" in
      new UserInfoManagerFixture {
        val offer: Offer = Offer.getDefaultInstance
        val result: UserOffersFoundResult = UserOffersFoundResult(UserRef(user), 1, Paging(), Seq(offer))

        (userOffersManager
          .getUserOffers(_: UserOffersRequest)(_: Traced))
          .expects(UserOffersRequest(UserRef(user), FiltersForRent, Paging(), None), *)
          .returning(Future.successful(result))
        (userOffersManager
          .getUserOffers(_: UserOffersRequest)(_: Traced))
          .expects(UserOffersRequest(UserRef(user), FiltersForSale, Paging(), None), *)
          .returning(Future.successful(result))

        val userResponse: ProtoResponse.UserResponse =
          manager
            .getUserByRef(userRef, skipMosRuAvailabilityCheck = false)
            .futureValue
        userResponse.getResponse.getTrustedUserInfo.getMosRuAvailable shouldBe true
      }

    "not set mosRu available flag if skipMosRuAvailabilityCheck = false and userOffersManager returns no offers" in
      new UserInfoManagerFixture {
        val offer: Offer = Offer.getDefaultInstance
        val result: UserOffersFoundResult = UserOffersFoundResult(UserRef(user), 0, Paging(), Seq(offer))

        (userOffersManager
          .getUserOffers(_: UserOffersRequest)(_: Traced))
          .expects(UserOffersRequest(UserRef(user), FiltersForRent, Paging(), None), *)
          .returning(Future.successful(result))
        (userOffersManager
          .getUserOffers(_: UserOffersRequest)(_: Traced))
          .expects(UserOffersRequest(UserRef(user), FiltersForSale, Paging(), None), *)
          .returning(Future.successful(result))

        val userResponse: ProtoResponse.UserResponse =
          manager
            .getUserByRef(userRef, skipMosRuAvailabilityCheck = false)
            .futureValue
        userResponse.getResponse.getTrustedUserInfo.getMosRuAvailable shouldBe false
      }

    "not set mosRu available flag if skipMosRuAvailabilityCheck = false but user is missing" in
      new UserInfoManagerFixture {
        val result: UserNotFoundUserOffersResult = UserNotFoundUserOffersResult(UserRef(user))

        (userOffersManager
          .getUserOffers(_: UserOffersRequest)(_: Traced))
          .expects(UserOffersRequest(UserRef(user), FiltersForRent, Paging(), None), *)
          .returning(Future.successful(result))
        (userOffersManager
          .getUserOffers(_: UserOffersRequest)(_: Traced))
          .expects(UserOffersRequest(UserRef(user), FiltersForSale, Paging(), None), *)
          .returning(Future.successful(result))

        val userResponse: ProtoResponse.UserResponse =
          manager
            .getUserByRef(userRef, skipMosRuAvailabilityCheck = false)
            .futureValue
        userResponse.getResponse.getTrustedUserInfo.getMosRuAvailable shouldBe false
      }

    "set has_offers=true if feature USER_HAS_OFFERS_FLAG is set and user has some offers" in new UserInfoManagerFixture {
      val offer: Offer = Offer.getDefaultInstance
      val result: UserOffersFoundResult = UserOffersFoundResult(UserRef(user), 1, Paging(1), Seq(offer))
      implicit val features = Set(Feature.USER_HAS_OFFERS_FLAG)

      (userOffersManager
        .getUserOffers(_: UserOffersRequest)(_: Traced))
        .expects(UserOffersRequest(UserRef(user), Seq.empty, Paging(1), None), *)
        .returning(Future.successful(result))

      val userResponse: ProtoResponse.UserResponse = manager
        .getUserByRef(userRef, skipMosRuAvailabilityCheck = true)
        .futureValue
      userResponse.getResponse.getHasOffers shouldBe true
    }

    "set has_offers=false if feature USER_HAS_OFFERS_FLAG is set but user has no offers" in new UserInfoManagerFixture {
      val offer: Offer = Offer.getDefaultInstance
      val result: UserOffersFoundResult = UserOffersFoundResult(UserRef(user), 0, Paging(1), Seq(offer))
      implicit val features = Set(Feature.USER_HAS_OFFERS_FLAG)

      (userOffersManager
        .getUserOffers(_: UserOffersRequest)(_: Traced))
        .expects(UserOffersRequest(UserRef(user), Seq.empty, Paging(1), None), *)
        .returning(Future.successful(result))

      val userResponse: ProtoResponse.UserResponse = manager
        .getUserByRef(userRef, skipMosRuAvailabilityCheck = true)
        .futureValue
      userResponse.getResponse.getHasOffers shouldBe false
    }

    "not set any flags when no features are requested" in new UserInfoManagerFixture {
      val userResponse: ProtoResponse.UserResponse =
        manager.getUserByRef(userRef, skipMosRuAvailabilityCheck = true).futureValue
      userResponse.getResponse.getTrustedUserInfo.getMosRuAvailable shouldBe false
      userResponse.getResponse.getHasOffers shouldBe false
    }
  }

  trait UserInfoManagerFixture {
    val userDao: UserDao = mock[UserDao]
    val userOffersManager: UserOffersManager = mock[UserOffersManager]

    val manager = new UserInfoManager(
      userDao,
      userOffersManager
    )

    val userRef: UserRef = UserRefRaw("userId")
    val user: User = User.newBuilder().setUserType(UserType.UT_OWNER).setUserRef(userRef.toApiRefString).build()

    (userDao
      .find(_: UserRef, _: Boolean))
      .expects(userRef, false)
      .returning(Some(user))
  }
}
