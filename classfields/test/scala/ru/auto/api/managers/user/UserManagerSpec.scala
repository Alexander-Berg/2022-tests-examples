package ru.auto.api.managers.user

import org.scalacheck.Gen
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.Inspectors
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.ResponseStatus
import ru.auto.api.auth.Application
import ru.auto.api.extdata.DataService
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.billing.BankerManager
import ru.auto.api.managers.dealer.DealerManager
import ru.auto.api.managers.offers.{EnrichedOfferLoader, PhoneRedirectManager}
import ru.auto.api.managers.passport.PassportManager
import ru.auto.api.managers.promocoder.PromocoderManager
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.bunker.telepony.PhoneRedirectInfo
import ru.auto.api.model.gen.BankerModelGenerators._
import ru.auto.api.model.gen.BasicGenerators
import ru.auto.api.model.gen.CabinetModelGenerators._
import ru.auto.api.model.gen.PromocoderModelGenerators.{CentsGen => PromocoderCentsGen}
import ru.auto.api.model.{DealerUserRoles, RequestParams}
import ru.auto.api.services.billing.{BankerClient, VsBillingClient}
import ru.auto.api.services.cabinet.CabinetApiClient
import ru.auto.api.services.calltracking.CalltrackingClient
import ru.auto.api.services.dealer_pony.DealerPonyClient
import ru.auto.api.services.geobase.GeobaseClient
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.settings.SettingsClient
import ru.auto.api.services.telepony.{TeleponyCallsClient, TeleponyClient}
import ru.auto.api.testkit.TestData.favoriteResellerList
import ru.auto.api.util.{Request, RequestImpl, UrlBuilder}
import ru.yandex.passport.model.api.ApiModel.LoadUserHint
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import java.lang.Character.isDigit
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.services.promocoder.PromocoderClient
import ru.auto.api.services.vos.VosClient

import scala.jdk.CollectionConverters._
import ru.auto.api.model.gen.DateTimeGenerators
import ru.auto.api.exceptions.ActionForbidden
import _root_.com.google.protobuf.BoolValue
import ru.auto.api.model.searcher.SearcherRequest
import ru.auto.api.ResponseModel.OfferCountResponse
import ru.auto.api.ResponseModel
import ru.auto.api.ApiOfferModel.OfferStatus
import _root_.ru.auto.api.model.CategorySelector
import ru.auto.api.util.crypt.TypedCrypto
import ru.auto.api.model.AutoruUser
import ru.yandex.vertis.feature.model.Feature

class UserManagerSpec extends BaseSpec with MockitoSupport {

  private val passportManager = mock[PassportManager]
  private val passportClient = mock[PassportClient]
  private val cabinetApiClient = mock[CabinetApiClient]
  private val salesmanClient = mock[SalesmanClient]
  private val dealerPonyClient = mock[DealerPonyClient]
  private val bankerManager = mock[BankerManager]
  private val bankerClient = mock[BankerClient]
  private val promocoderManager = mock[PromocoderManager]
  private val dataService = mock[DataService]
  private val settingsClient = mock[SettingsClient]
  private val urlBuilder: UrlBuilder = mock[UrlBuilder]
  private val fakeManager: FakeManager = mock[FakeManager]

  when(fakeManager.shouldFakeRequest(?)).thenReturn(false)

  private val redirectManager =
    new PhoneRedirectManager(
      mock[TeleponyClient],
      mock[TeleponyCallsClient],
      mock[GeobaseClient],
      dealerPonyClient,
      settingsClient,
      mock[FeatureManager],
      null,
      null,
      1,
      PhoneRedirectInfo(Map.empty),
      urlBuilder,
      fakeManager
    )

  private val vsBillingClient = mock[VsBillingClient]

  private val searcherClient = mock[SearcherClient]

  private val offerLoader = mock[EnrichedOfferLoader]

  private val calltrackingClient = mock[CalltrackingClient]

  private val promocoderClient = mock[PromocoderClient]

  private val vosClient = mock[VosClient]

  private val cryptoUserId = mock[TypedCrypto[AutoruUser]]
  when(cryptoUserId.encrypt(? : AutoruUser)).thenAnswer(_.getArgument[AutoruUser](0).toString.reverse)

  private val feature: Feature[Boolean] = mock[Feature[Boolean]]

  when(feature.value).thenReturn(false)

  private val dealerManager = new DealerManager(
    cabinetApiClient,
    searcherClient,
    salesmanClient,
    dealerPonyClient,
    redirectManager,
    vsBillingClient,
    passportClient,
    dataService,
    offerLoader,
    calltrackingClient,
    promocoderClient,
    vosClient,
    fakeManager
  )

  private val userManager =
    new UserManager(
      passportManager,
      bankerManager,
      bankerClient,
      promocoderManager,
      dealerManager,
      favoriteResellerList,
      searcherClient,
      vosClient,
      cryptoUserId,
      urlBuilder
    )

  private val user = PrivateUserRefGen.next
  private val dealer = DealerUserRefGen.next
  private val passportUserResult = passportUserResultWithAutoruExpertGen().next
  private val passportUserResultForDealer = passportUserResultWithAutoruExpertGen(true, true).next
  private val passportUserProfile = PassportAutoruProfileGen.next

  private val passportDealerProfile =
    PassportAutoruProfileGen.next.toBuilder
      .setClientId(dealer.clientId.toString)
      .build()

  private val tiedCards = Gen.listOf(TiedCardGen).next
  private val accountInfo = AccountInfoGen.next
  private val dealerAccountResponse = DealerAccountResponseGen.next

  private val bonusBalance = PromocoderCentsGen.next

  private val userRequest = {
    val r = new RequestImpl
    r.setTrace(Traced.empty)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.swagger)
    r.setUser(user)
    r
  }

  private val dealerRequest = {
    val r = new RequestImpl
    r.setTrace(Traced.empty)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.swagger)
    r.setUser(user)
    r.setDealer(dealer)
    r.setDealerRole(DealerUserRoles.Unknown)
    r
  }

  private val anonRequest = {
    val r = new RequestImpl
    r.setTrace(Traced.empty)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.swagger)
    r
  }

  private val moderationRequest = {
    val r = new RequestImpl
    r.setTrace(Traced.empty)
    r.setRequestParams(RequestParams.construct("1.1.1.1"))
    r.setApplication(Application.moderation)
    r
  }

  "UserManager.getCurrentUser() for user" should {

    implicit val r: Request = userRequest

    "get user enriched with cards and balance" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(bankerManager.getTiedCards(user)).thenReturnF(tiedCards)
      when(bankerClient.getAccountInfo(user, user.toPlain)).thenReturnF(accountInfo)
      when(promocoderManager.bonusBalance(user)).thenReturnF(bonusBalance)
      val userResponse = userManager.getCurrentUser().futureValue
      userResponse.getStatus shouldBe ResponseStatus.SUCCESS
      userResponse.getUser shouldBe passportUserResult.getUser
      val resultTiedCards = userResponse.getTiedCardsList.asScala
      resultTiedCards.size shouldBe tiedCards.size
      Inspectors.forEvery(resultTiedCards.zip(tiedCards)) {
        case (resultTiedCard, bankerTiedCard) =>
          val expectedId = bankerTiedCard.getId.filter(isDigit).drop(1).toLong
          val expectedMask = bankerTiedCard.getProperties.getCard.getCddPanMask.filter(isDigit).dropWhile(_ == 0)
          resultTiedCard.getId shouldBe expectedId
          resultTiedCard.getCardMask shouldBe expectedMask
          resultTiedCard.getPreferred shouldBe bankerTiedCard.getPreferred.getValue
          resultTiedCard.getProperties shouldBe bankerTiedCard.getProperties.getCard
      }
      userResponse.getUserBalance shouldBe (accountInfo.getBalance + bonusBalance) / 100
      userResponse.hasClientBalance shouldBe false
    }

    "get non-enriched user if getting tied cards fails" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(bankerManager.getTiedCards(user)).thenThrowF(new Exception)
      when(bankerClient.getAccountInfo(user, user.toPlain)).thenReturnF(accountInfo)
      when(promocoderManager.bonusBalance(user)).thenReturnF(bonusBalance)
      val userResponse = userManager.getCurrentUser().futureValue
      userResponse.getUser shouldBe passportUserResult.getUser
      userResponse.getTiedCardsList shouldBe empty
    }

    "get non-enriched user if getting balance fails" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(bankerManager.getTiedCards(user)).thenReturnF(tiedCards)
      when(bankerClient.getAccountInfo(user, user.toPlain)).thenThrowF(new Exception)
      val userResponse = userManager.getCurrentUser().futureValue
      userResponse.getUser shouldBe passportUserResult.getUser
      userResponse.hasUserBalance shouldBe false
    }

    "fail on passport failure" in {
      when(passportManager.getUser(user)).thenThrowF(new Exception)
      userManager.getCurrentUser().failed.futureValue shouldBe an[Exception]
    }

    "get encrypted user ID" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(bankerManager.getTiedCards(user)).thenThrowF(new Exception)
      when(bankerClient.getAccountInfo(user, user.toPlain)).thenThrowF(new Exception)
      val userResponse = userManager.getCurrentUser().futureValue
      userResponse.getEncryptedUserId() shouldBe cryptoUserId.encrypt(user)
    }
  }

  "UserManager.getUser() for user" should {

    implicit val r: Request = moderationRequest

    "get user enriched with cards and balance" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenReturnF(passportUserProfile)
      when(bankerManager.getTiedCards(user)).thenReturnF(tiedCards)
      when(bankerClient.getAccountInfo(user, user.toPlain)).thenReturnF(accountInfo)
      when(promocoderManager.bonusBalance(user)).thenReturnF(bonusBalance)
      val userResponse = userManager.getUser(user).futureValue
      userResponse.getStatus shouldBe ResponseStatus.SUCCESS
      userResponse.getUser shouldBe passportUserResult.getUser
      val resultTiedCards = userResponse.getTiedCardsList.asScala
      resultTiedCards.size shouldBe tiedCards.size
      Inspectors.forEvery(resultTiedCards.zip(tiedCards)) {
        case (resultTiedCard, bankerTiedCard) =>
          val expectedId = bankerTiedCard.getId.filter(isDigit).drop(1).toLong
          val expectedMask = bankerTiedCard.getProperties.getCard.getCddPanMask.filter(isDigit).dropWhile(_ == 0)
          resultTiedCard.getId shouldBe expectedId
          resultTiedCard.getCardMask shouldBe expectedMask
          resultTiedCard.getPreferred shouldBe bankerTiedCard.getPreferred.getValue
      }
      userResponse.getUserBalance shouldBe (accountInfo.getBalance + bonusBalance) / 100
      userResponse.hasClientBalance shouldBe false
    }

    "get non-enriched user if getting tied cards fails" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenReturnF(passportUserProfile)
      when(bankerManager.getTiedCards(user)).thenThrowF(new Exception)
      when(bankerClient.getAccountInfo(user, user.toPlain)).thenReturnF(accountInfo)
      when(promocoderManager.bonusBalance(user)).thenReturnF(bonusBalance)
      val userResponse = userManager.getUser(user).futureValue
      userResponse.getUser shouldBe passportUserResult.getUser
      userResponse.getTiedCardsList shouldBe empty
    }

    "get non-enriched user if getting balance fails" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenReturnF(passportUserProfile)
      when(bankerManager.getTiedCards(user)).thenReturnF(tiedCards)
      when(bankerClient.getAccountInfo(user, user.toPlain)).thenThrowF(new Exception)
      val userResponse = userManager.getUser(user).futureValue
      userResponse.getUser shouldBe passportUserResult.getUser
      userResponse.hasUserBalance shouldBe false
    }

    "fail on passport failure" in {
      when(passportManager.getUser(user)).thenThrowF(new Exception)
      userManager.getUser(user).failed.futureValue shouldBe an[Exception]
    }

    "fail on passport profile failure" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenThrowF(new Exception)
      userManager.getUser(user).failed.futureValue shouldBe an[Exception]
    }

    "get encrypted user id" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenReturnF(passportUserProfile)
      when(bankerManager.getTiedCards(user)).thenThrowF(new Exception)
      when(bankerClient.getAccountInfo(user, user.toPlain)).thenThrowF(new Exception)
      val userResponse = userManager.getUser(user).futureValue
      userResponse.getEncryptedUserId() shouldBe cryptoUserId.encrypt(user)
    }
  }

  "UserManager.getCurrentUser() for dealer" should {

    implicit val r: Request = dealerRequest
    implicit val traced = dealerRequest.trace

    "get user enriched with balance" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResultForDealer)

      when(cabinetApiClient.getDealerAccount(?, ?)(?)).thenReturnF(dealerAccountResponse)
      when(promocoderClient.getFeatures(?, ?)(?)).thenReturnF(List.empty)

      val userResponse = userManager.getCurrentUser().futureValue
      userResponse.getStatus shouldBe ResponseStatus.SUCCESS
      userResponse.getUser shouldBe passportUserResultForDealer.getUser
      userResponse.getTiedCardsList shouldBe empty
      userResponse.hasUserBalance shouldBe false
      userResponse.getClientBalance shouldBe dealerAccountResponse.getBalance
    }

    "get user enriched with balance when overdraft invoice not found" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResultForDealer)
      when(cabinetApiClient.getDealerAccount(?, ?)(?)).thenReturnF(dealerAccountResponse)
      when(promocoderClient.getFeatures(?, ?)(?)).thenReturnF(List.empty)

      val userResponse = userManager.getCurrentUser().futureValue
      userResponse.getStatus shouldBe ResponseStatus.SUCCESS
      userResponse.getUser shouldBe passportUserResultForDealer.getUser
      userResponse.getTiedCardsList shouldBe empty
      userResponse.hasUserBalance shouldBe false
      userResponse.getClientBalance shouldBe dealerAccountResponse.getBalance
    }

    "get non-enriched user if getting balance fails" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResultForDealer)
      when(cabinetApiClient.getDealerAccount(?, ?)(?)).thenThrowF(new Exception)
      val userResponse = userManager.getCurrentUser().futureValue
      userResponse.getUser shouldBe passportUserResultForDealer.getUser
      userResponse.hasClientBalance shouldBe false
    }

    "get user with LoadUserHint" in {
      val hint = BasicGenerators.protoEnum(LoadUserHint.values()).next
      when(passportManager.getUser(eq(user), eq.apply(Seq(hint)): _*)(?)).thenReturnF(passportUserResultForDealer)
      when(dealerManager.getDealerAccount(dealer.clientId)).thenReturnF(dealerAccountResponse)
      val userResponse = userManager.getCurrentUser(hint).futureValue
      userResponse.getUser shouldBe passportUserResultForDealer.getUser
    }

    "fail on passport failure" in {
      when(passportManager.getUser(user)).thenThrowF(new Exception)
      userManager.getCurrentUser().failed.futureValue shouldBe an[Exception]
    }
  }

  "UserManager.getUser() for moderator" should {
    implicit val r: Request = PrivateModeratorRequestGen.next

    "get user with autoru_expert_status.can_read = true" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenReturnF(passportUserProfile)
      when(bankerManager.getTiedCards(user)).thenReturnF(tiedCards)
      when(bankerClient.getAccountInfo(user, user.toPlain)).thenReturnF(accountInfo)
      when(promocoderManager.bonusBalance(user)).thenReturnF(bonusBalance)
      val userResponse = userManager.getUser(user).futureValue
      userResponse.getUser.getProfile.getAutoru.getAutoruExpertStatus.getCanRead shouldBe true
      userResponse.getUser.getProfile.getAutoru.getAutoruExpertStatus.getCanAdd shouldBe false
    }
  }

  "UserManager.getUser() for dealer" should {

    implicit val r: Request = moderationRequest

    "get user enriched with balance" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenReturnF(passportDealerProfile)
      when(cabinetApiClient.getDealerAccount(?, ?)(?)).thenReturnF(dealerAccountResponse)
      when(promocoderClient.getFeatures(?, ?)(?)).thenReturnF(List.empty)

      val userResponse = userManager.getUser(user).futureValue
      userResponse.getStatus shouldBe ResponseStatus.SUCCESS
      userResponse.getUser shouldBe passportUserResult.getUser
      userResponse.getTiedCardsList shouldBe empty
      userResponse.hasUserBalance shouldBe false
      userResponse.getClientBalance shouldBe dealerAccountResponse.getBalance
    }

    "get user enriched with balance when overdraft invoice not found" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenReturnF(passportDealerProfile)
      when(cabinetApiClient.getDealerAccount(?, ?)(?)).thenReturnF(dealerAccountResponse)
      when(promocoderClient.getFeatures(?, ?)(?)).thenReturnF(List.empty)

      val userResponse = userManager.getUser(user).futureValue
      userResponse.getStatus shouldBe ResponseStatus.SUCCESS
      userResponse.getUser shouldBe passportUserResult.getUser
      userResponse.getTiedCardsList shouldBe empty
      userResponse.hasUserBalance shouldBe false
      userResponse.getClientBalance shouldBe dealerAccountResponse.getBalance
    }

    "get non-enriched user if getting balance fails" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenReturnF(passportDealerProfile)
      when(cabinetApiClient.getDealerAccount(?, ?)(?)).thenThrowF(new Exception)
      val userResponse = userManager.getUser(user).futureValue
      userResponse.getUser shouldBe passportUserResult.getUser
      userResponse.hasClientBalance shouldBe false
    }

    "fail on passport failure" in {
      when(passportManager.getUser(user)).thenThrowF(new Exception)
      userManager.getUser(user).failed.futureValue shouldBe an[Exception]
    }

    "fail on passport profile failure" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenThrowF(new Exception)
      userManager.getUser(user).failed.futureValue shouldBe an[Exception]
    }
  }

  "UserManager.getUser() for anon request" should {

    implicit val r: Request = anonRequest

    "get private not enriched with balance" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenReturnF(passportUserProfile)
      val userResponse = userManager.getUser(user).futureValue
      userResponse.getStatus shouldBe ResponseStatus.SUCCESS
      userResponse.getUser shouldBe passportUserResult.getUser
      userResponse.getTiedCardsList.asScala shouldBe empty
      userResponse.getUserBalance shouldBe 0
      userResponse.hasClientBalance shouldBe false
    }

    "get dealer not enriched with balance" in {
      when(passportManager.getUser(user)).thenReturnF(passportUserResult)
      when(passportManager.getUserProfile(user)).thenReturnF(passportDealerProfile)
      val userResponse = userManager.getUser(user).futureValue
      userResponse.getStatus shouldBe ResponseStatus.SUCCESS
      userResponse.getUser shouldBe passportUserResult.getUser
      userResponse.getTiedCardsList shouldBe empty
      userResponse.hasUserBalance shouldBe false
      userResponse.getClientBalance shouldBe 0
    }
  }

  "UserManager.getUserInfo()" should {

    implicit val r: Request = anonRequest

    "produce expected result when all parameters are specified" in {
      val alias = Gen.identifier.next
      val registrationDate = DateTimeGenerators.localDateInPast().next.toString()
      val activeCount = arbitrary[Int].next
      val inactiveCount = arbitrary[Int].next
      val category = StrictCategoryGen.next
      val shareUrl = Gen.identifier.next

      val userEssentialsBuilder = UserEssentialsGen.next.toBuilder()
      userEssentialsBuilder.getProfileBuilder().setAllowOffersShow(BoolValue.of(true))
      userEssentialsBuilder.getProfileBuilder.setAlias(alias)
      userEssentialsBuilder.setRegistrationDate(registrationDate)

      when(passportManager.getUserEssentials(user, false)).thenReturnF(userEssentialsBuilder.build())
      when(
        searcherClient.offersCount(
          eq(SearcherRequest(category, Map("user_id" -> Set(user.toRaw)))),
          eq(true)
        )(?)
      ).thenReturnF(OfferCountResponse.newBuilder().setCount(activeCount).build())
      when(
        vosClient
          .countOffers(
            eq(category),
            eq(user),
            eq(ResponseModel.Filters.newBuilder().addStatus(OfferStatus.INACTIVE).build)
          )(?)
      ).thenReturnF(OfferCountResponse.newBuilder().setCount(inactiveCount).build())
      when(urlBuilder.resellerUrl(eq(user))).thenReturn(shareUrl)

      val userInfoResponse =
        userManager.getUserInfo(user, Set(category), countInactiveOffers = true).futureValue

      userInfoResponse.getStatus shouldBe ResponseStatus.SUCCESS
      userInfoResponse.getAlias shouldBe alias
      userInfoResponse.getRegistrationDate shouldBe registrationDate
      userInfoResponse.getShareUrl shouldBe shareUrl

      userInfoResponse.getOffersStatsByCategoryMap().asScala.keySet shouldBe Set(category.enum.name)
      val categoryStats = userInfoResponse.getOffersStatsByCategoryOrThrow(category.enum.name)
      categoryStats.getActiveOffersCount() shouldBe activeCount
      categoryStats.getInactiveOffersCount() shouldBe inactiveCount
    }

    "throw ActionForbidden when listing offers is not allowed" in {
      val userEssentialsBuilder = UserEssentialsGen.next.toBuilder()
      userEssentialsBuilder.getProfileBuilder().clearAllowOffersShow

      when(passportManager.getUserEssentials(user, false)).thenReturnF(userEssentialsBuilder.build())
      val exception = userManager.getUserInfo(user, Set.empty, false).failed.futureValue
      exception shouldBe an[ActionForbidden]
    }

    "omit inactive offers count when countInactiveOffer is not set" in {
      val activeCount = arbitrary[Int].next

      val userEssentialsBuilder = UserEssentialsGen.next.toBuilder()
      userEssentialsBuilder.getProfileBuilder().setAllowOffersShow(BoolValue.of(true))

      when(passportManager.getUserEssentials(user, false)).thenReturnF(userEssentialsBuilder.build())
      when(
        searcherClient.offersCount(?, ?)(?)
      ).thenReturnF(OfferCountResponse.newBuilder().setCount(activeCount).build())

      val userInfoResponse =
        userManager.getUserInfo(user, Set(CategorySelector.Cars), countInactiveOffers = false).futureValue

      userInfoResponse.getStatus shouldBe ResponseStatus.SUCCESS

      userInfoResponse.getOffersStatsByCategoryMap().asScala.keySet shouldBe Set("CARS")
      val categoryStats = userInfoResponse.getOffersStatsByCategoryOrThrow("CARS")
      categoryStats.getActiveOffersCount() shouldBe activeCount
      categoryStats.hasInactiveOffersCount() shouldBe false
    }

    "include all categories when the list of selected categories is empty" in {
      val userEssentialsBuilder = UserEssentialsGen.next.toBuilder()
      userEssentialsBuilder.getProfileBuilder().setAllowOffersShow(BoolValue.of(true))

      when(passportManager.getUserEssentials(user, false)).thenReturnF(userEssentialsBuilder.build())
      CategorySelector.categories.foreach { category =>
        when(
          searcherClient.offersCount(
            eq(SearcherRequest(category, Map("user_id" -> Set(user.toRaw)))),
            eq(true)
          )(?)
        ).thenReturnF(OfferCountResponse.newBuilder().build())
      }

      val userInfoResponse =
        userManager.getUserInfo(user, Set.empty, countInactiveOffers = false).futureValue

      userInfoResponse.getStatus shouldBe ResponseStatus.SUCCESS

      val expectedCategories = CategorySelector.categories
        .map(_.enum.name)
        .toSet

      userInfoResponse.getOffersStatsByCategoryMap().asScala.keySet shouldBe expectedCategories
    }

    "skip alias field if the alias was not provided" in {
      val userEssentialsBuilder = UserEssentialsGen.next.toBuilder()
      userEssentialsBuilder.getProfileBuilder().setAllowOffersShow(BoolValue.of(true))
      userEssentialsBuilder.getProfileBuilder().clearAlias()

      when(passportManager.getUserEssentials(user, false)).thenReturnF(userEssentialsBuilder.build())

      val userInfoResponse =
        userManager.getUserInfo(user, Set.empty, countInactiveOffers = false).futureValue

      userInfoResponse.getStatus shouldBe ResponseStatus.SUCCESS
      userInfoResponse.hasAlias shouldBe false
    }
  }
}
