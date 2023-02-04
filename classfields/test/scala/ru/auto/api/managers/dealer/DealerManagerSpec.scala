package ru.auto.api.managers.dealer

import org.apache.commons.io.IOUtils
import org.mockito.Mockito.{verify, verifyNoMoreInteractions}
import org.scalatest.OptionValues
import ru.auto.api.ApiOfferModel.{Category, Phone, Section, TeleponyInfo}
import ru.auto.api.BaseSpec
import ru.auto.api.ResponseModel.{ResponseStatus, SalonResponse}
import auto.common.Pagination.ResponsePagination
import ru.auto.api.auth.Application
import ru.auto.api.exceptions.{AccessGroupNotFound, CustomerAccessForbidden, UserIsAlreadyLinkedToClient, UserNotFoundException}
import ru.auto.api.extdata.DataService
import ru.auto.api.managers.fake.FakeManager
import ru.auto.api.managers.offers.{EnrichedOfferLoader, PhoneRedirectManager}
import ru.auto.api.metro.MetroBase
import ru.auto.api.model.DealerUserRoles.DealerUserRole
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model._
import ru.auto.api.model.gen.NetGenerators
import ru.auto.api.services.billing.VsBillingClient
import ru.auto.api.services.cabinet.{CabinetApiClient, ClientPropertiesView, ClientView}
import ru.auto.api.services.calltracking.CalltrackingClient
import ru.auto.api.services.dealer_pony.DealerPonyClient
import ru.auto.api.services.passport.PassportClient
import ru.auto.api.services.promocoder.PromocoderClient
import ru.auto.api.services.salesman.SalesmanClient
import ru.auto.api.services.searcher.SearcherClient
import ru.auto.api.services.telepony.TeleponyClient.CallRecord
import ru.auto.api.services.vos.VosClient
import ru.auto.api.util.{ManagerUtils, Request, RequestImpl, Resources}
import ru.auto.cabinet.AclResponse._
import ru.auto.cabinet.DealerResponse._
import ru.auto.calltracking.proto.RedirectsServiceOuterClass.{GetRedirectsByConfirmationResponse, RedirectPhone}
import ru.auto.dealer_pony.proto.ApiModel.{CallInfoResponse, TeleponyInfoListResponse, TeleponyInfoRequest, TeleponyInfoResponse}
import ru.yandex.passport.model.api.ApiModel.{AutoruUserProfile, User, UserIdsResult, UserProfile}
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Тестируем [[DealerManager]].
  */
// scalastyle:off number.of.methods
class DealerManagerSpec extends BaseSpec with OptionValues {

  private val dealer = DealerUserRefGen.next
  private val privateUser = PrivateUserRefGen.next

  private def passportUser(user: AutoruUser, clientGroup: ClientGroup) = {
    User
      .newBuilder()
      .setId(user.uid.toString)
      .setProfile {
        UserProfile
          .newBuilder()
          .setAutoru {
            AutoruUserProfile
              .newBuilder()
              .setClientGroup(clientGroup)
          }
      }
      .build()
  }

  private def requestWithDealer(dealer: AutoruDealer, role: DealerUserRole) = {
    val r = new RequestImpl
    r.setApplication(Application.iosApp)
    r.setRequestParams(RequestParams.construct("0.0.0.0"))
    r.setUser(dealer)
    r.setDealer(dealer)
    r.setDealerRole(role)
    r
  }

  private val clientRequest = requestWithDealer(dealer, DealerUserRoles.Client)
  private val managerRequest = requestWithDealer(dealer, DealerUserRoles.Manager)
  private val request = requestWithDealer(dealer, DealerUserRoles.Unknown)

  private val privateUserRequest = {
    val r = new RequestImpl
    r.setApplication(Application.iosApp)
    r.setRequestParams(RequestParams.construct("0.0.0.0"))
    r.setUser(privateUser)
    r
  }

  trait Fixture extends MockitoSupport {
    val passportClient = mock[PassportClient]
    val cabinetApiClient = mock[CabinetApiClient]
    val salesmanClient = mock[SalesmanClient]
    val dealerPonyClient = mock[DealerPonyClient]
    val redirectManager = mock[PhoneRedirectManager]
    val dataService = mock[DataService]

    val vsBillingClient = mock[VsBillingClient]

    val searcherClient = mock[SearcherClient]

    val offerLoader = mock[EnrichedOfferLoader]

    val calltrackingClient = mock[CalltrackingClient]

    val promocoderClient = mock[PromocoderClient]
    val vosClient = mock[VosClient]

    val fakeManager: FakeManager = mock[FakeManager]

    val dealerManager = new DealerManager(
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
  }

  "DealerManager.getSalonByDealerId()" should {
    implicit val r: Request = clientRequest

    "call searcher client" in new Fixture {
      val dealerId = ReadableStringGen.next
      val salon = SalonGen.next
      val salonResponse = SalonResponse.newBuilder().setSalon(salon).setStatus(ResponseStatus.SUCCESS).build()
      when(searcherClient.getSalonByDealerId(eq(dealerId))(?)).thenReturnF(salon)
      when(dataService.metroBase).thenReturn(MetroBase(Map(), Map()))
      val result = dealerManager.getSalonByDealerId(dealerId).futureValue
      result shouldBe salonResponse
      verify(searcherClient).getSalonByDealerId(eq(dealerId))(?)
    }
  }

  "DealerManager.getDealerPhones()" should {
    implicit val r: Request = clientRequest

    "get dealer phones" in new Fixture {
      import scala.jdk.CollectionConverters.{IterableHasAsJava, IterableHasAsScala}
      val dealerCode = "test_dealer_code"
      val salon = SalonGen.next.toBuilder.addAllClientIds(Seq("20101").asJava).setCode(dealerCode).build()
      val dealerPhonesFromCabinet = DealerPhones.newBuilder().addAllPhones(salon.getPhonesList).build()
      val teleponyInfo = TeleponyInfoResponse
        .newBuilder()
        .setResponse(TeleponyInfo.newBuilder().setObjectId("dealer-198").build)
        .build()
      val phones = salon.getPhonesList.asScala
      val phonesParams = PhonesParams(Some(Category.CARS), Some(Section.USED), None)
      val dealerPhonesResponse = DealerPhones.newBuilder().addAllPhones(salon.getPhonesList).build()

      when(fakeManager.shouldTakeFakeDealerPhone(?)).thenReturn(false)
      when(searcherClient.getSalonByCode(eq(salon.getCode))(?)).thenReturnF(salon)
      when(
        cabinetApiClient
          .getClientPhones(eq(AutoruDealer(20101L)), eq(Some(Category.CARS)), eq(Some(Section.USED)), eq(None))(?)
      ).thenReturnF(dealerPhonesFromCabinet)
      when(
        dealerPonyClient
          .getTeleponyInfoForDealerPhonesByParams(eq(20101L), eq(Category.CARS), eq(Section.USED))(?)
      ).thenReturnF(teleponyInfo)
      when(
        redirectManager.getDealerPhones(eq(salon), eq(dealerPhonesFromCabinet), eq(Some(teleponyInfo.getResponse)))(?)
      ).thenReturnF(phones.toSeq)

      val result = dealerManager
        .getDealerPhones(salon.getCode, phonesParams)
        .futureValue

      result shouldBe dealerPhonesResponse
      verify(searcherClient).getSalonByCode(eq(salon.getCode))(?)
    }

    "return fake phones for robot request" in new Fixture {
      val phone = Phone.newBuilder().setPhone(PhoneGen.next).build()
      val dealerCode = "test_dealer_code"
      val phonesParams = PhonesParams(Some(Category.CARS), Some(Section.USED), None)
      val dealerPhonesResponse = DealerPhones.newBuilder().addPhones(phone).build()

      when(fakeManager.shouldTakeFakeDealerPhone(?)).thenReturn(true)
      when(fakeManager.getFakeDealerPhone(dealerCode)).thenReturn(Some(phone))

      val result = dealerManager
        .getDealerPhones(dealerCode, phonesParams)
        .futureValue

      result shouldBe dealerPhonesResponse

      verify(fakeManager).shouldTakeFakeDealerPhone(?)
      verify(fakeManager).getFakeDealerPhone(dealerCode)
      verifyNoMoreInteractions(
        searcherClient,
        cabinetApiClient,
        dealerPonyClient,
        redirectManager
      )
    }
  }

  "DealerManager.limitedOverdraft()" should {
    "get limited overdraft" in new Fixture {
      val funds = 10000L

      dealerManager.limitedOverdraft(funds, 50) shouldBe 5000L
      dealerManager.limitedOverdraft(funds, 10) shouldBe 1000L
    }

    "round to hundreds" in new Fixture {
      val funds = 10440L

      dealerManager.limitedOverdraft(funds, 50) shouldBe 5300L
    }
  }

  "DealerManager.dealerCallRecord()" should {
    implicit val r: Request = request

    "get call record with paid = false" in new Fixture {
      Resources.open("/telepony/call_record.wav") { content =>
        val callRecord = CallRecord("record.wav", IOUtils.toByteArray(content))

        val recordId = "someCallRecord"
        when(redirectManager.getCallRecord("auto-dealers", recordId)).thenReturnF(callRecord)

        dealerManager.dealerCallRecord(dealer, recordId, isPaid = false).futureValue shouldBe callRecord
      }
    }

    "get call record with paid = true" in new Fixture {
      Resources.open("/telepony/call_record.wav") { content =>
        val callRecord = CallRecord("record.wav", IOUtils.toByteArray(content))

        val recordId = "someCallRecord"
        when(redirectManager.getCallRecord("autoru_billing", recordId)).thenReturnF(callRecord)

        dealerManager.dealerCallRecord(dealer, recordId, isPaid = true).futureValue shouldBe callRecord
      }
    }
  }

  "DealerManager.usersList()" should {
    implicit val r: Request = request

    "get dealer users list with access grants" in new Fixture {
      val clientGroup = 1L

      val userRef = PrivateUserRefGen.next
      val user = passportUser(userRef, clientGroup.toString)

      val dealerUsers =
        UserIdsResult
          .newBuilder()
          .addUsers(user)
          .build()

      val expectedGrant =
        DealerAccessResourceGen.next

      val expectedGroup =
        Group
          .newBuilder()
          .setId(clientGroup)
          .addGrants(expectedGrant)
          .build()

      val dealerAccessGroups =
        GroupsList
          .newBuilder()
          .addGroups(expectedGroup)
          .build()

      val expectedResponse =
        DealerUsersList
          .newBuilder()
          .addUsers {
            DealerUser
              .newBuilder()
              .setUser(user)
              .setAccess {
                AccessGrants
                  .newBuilder()
                  .setGroup {
                    Group
                      .newBuilder()
                      .setId(clientGroup)
                  }
                  .addGrants {
                    expectedGrant
                  }
              }
          }
          .build()

      when(cabinetApiClient.getClientAccessGroups(dealer)).thenReturnF(dealerAccessGroups)
      when(passportClient.getDealerUsers(dealer)).thenReturnF(dealerUsers)

      dealerManager.usersList(dealer).futureValue shouldBe expectedResponse
    }
  }

  "DealerManager.linkUser()" should {
    implicit val r: Request = privateUserRequest

    "link user to dealer by email" in new Fixture {
      val dealer = DealerUserRefGen.next
      val user = PrivateUserRefGen.next

      val dealerAccessGroup = DealerAccessGroupGen.next
      val dealerAccessGroupId = dealerAccessGroup.getId.toString

      val passportUserBuilder = PassportUserGen.next.toBuilder
      passportUserBuilder.setId(user.uid.toString)
      passportUserBuilder.getProfileBuilder.getAutoruBuilder.setClientGroup(dealerAccessGroupId)

      val passportUser = passportUserBuilder.build()

      val email = NetGenerators.emailGen.next
      when(passportClient.getUserByEmail(?)(?)).thenReturnF(passportUser)

      val userEssentials = UserEssentialsGen.next.toBuilder.clearClientId().build()
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(userEssentials)

      val dealerAccessGroups =
        GroupsList
          .newBuilder()
          .addGroups(dealerAccessGroup)
          .build()

      when(cabinetApiClient.getClientAccessGroups(dealer)).thenReturnF(dealerAccessGroups)

      when(passportClient.linkDealerUser(dealer, user, dealerAccessGroupId))
        .thenReturnF(())

      val dealerUsers =
        UserIdsResult
          .newBuilder()
          .addUsers(passportUser)
          .build()

      when(passportClient.getDealerUsers(dealer)).thenReturnF(dealerUsers)

      val expected =
        DealerUser
          .newBuilder()
          .setUser(passportUser)
          .setAccess {
            AccessGrants
              .newBuilder()
              .setGroup(dealerAccessGroup.toBuilder.clearGrants())
              .addAllGrants(dealerAccessGroup.getGrantsList)
          }
          .build()

      dealerManager
        .linkUser(dealer, optUser = None, optEmail = Some(email), dealerAccessGroupId)
        .futureValue shouldBe expected

      verify(passportClient).getUserByEmail(eq(email))(?)
      verify(passportClient).getUserEssentials(eq(user), eq(false))(?)
    }

    "link user to dealer by userId" in new Fixture {
      val dealer = DealerUserRefGen.next
      val user = PrivateUserRefGen.next

      val dealerAccessGroup = DealerAccessGroupGen.next
      val dealerAccessGroupId = dealerAccessGroup.getId.toString

      val passportUserBuilder = PassportUserGen.next.toBuilder
      passportUserBuilder.setId(user.uid.toString)
      passportUserBuilder.getProfileBuilder.getAutoruBuilder.setClientGroup(dealerAccessGroupId)

      val passportUser = passportUserBuilder.build()

      val userEssentials = UserEssentialsGen.next.toBuilder.clearClientId().build()
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(userEssentials)

      val dealerAccessGroups =
        GroupsList
          .newBuilder()
          .addGroups(dealerAccessGroup)
          .build()

      when(cabinetApiClient.getClientAccessGroups(dealer)).thenReturnF(dealerAccessGroups)

      when(passportClient.linkDealerUser(dealer, user, dealerAccessGroupId))
        .thenReturnF(())

      val dealerUsers =
        UserIdsResult
          .newBuilder()
          .addUsers(passportUser)
          .build()

      when(passportClient.getDealerUsers(dealer)).thenReturnF(dealerUsers)

      val expected =
        DealerUser
          .newBuilder()
          .setUser(passportUser)
          .setAccess {
            AccessGrants
              .newBuilder()
              .setGroup(dealerAccessGroup.toBuilder.clearGrants())
              .addAllGrants(dealerAccessGroup.getGrantsList)
          }
          .build()

      dealerManager
        .linkUser(dealer, optUser = Some(user), optEmail = None, dealerAccessGroupId)
        .futureValue shouldBe expected

      verify(passportClient).getUserEssentials(eq(user), eq(false))(?)
    }

    "fail on link user to dealer without email or user params" in new Fixture {
      val dealer = DealerUserRefGen.next

      val dealerAccessGroup = DealerAccessGroupGen.next
      val dealerAccessGroupId = dealerAccessGroup.getId.toString

      dealerManager
        .linkUser(dealer, optUser = None, optEmail = None, dealerAccessGroupId)
        .failed
        .futureValue shouldBe an[IllegalArgumentException]
    }

    "fail on link user to dealer and the same user" in new Fixture {
      val dealer = DealerUserRefGen.next

      val dealerAccessGroup = DealerAccessGroupGen.next
      val dealerAccessGroupId = dealerAccessGroup.getId.toString

      dealerManager
        .linkUser(dealer, optUser = Some(privateUser), optEmail = None, dealerAccessGroupId)
        .failed
        .futureValue shouldBe an[IllegalArgumentException]
    }

    "fail on link user to dealer and already linked user" in new Fixture {
      val dealer = DealerUserRefGen.next
      val user = PrivateUserRefGen.next

      val dealerAccessGroup = DealerAccessGroupGen.next
      val dealerAccessGroupId = dealerAccessGroup.getId.toString

      val clientId = dealer.clientId.toString

      val userEssentials = UserEssentialsGen.next.toBuilder.setClientId(clientId).build()
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(userEssentials)

      dealerManager
        .linkUser(dealer, optUser = Some(user), optEmail = None, dealerAccessGroupId)
        .failed
        .futureValue shouldBe an[UserIsAlreadyLinkedToClient]
    }

    "fail on link user to dealer and group does not exists" in new Fixture {
      val dealer = DealerUserRefGen.next
      val user = PrivateUserRefGen.next

      val userEssentials = UserEssentialsGen.next.toBuilder.clearClientId.build()
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(userEssentials)

      val dealerAccessGroup = DealerAccessGroupGen.next

      val dealerAccessGroups =
        GroupsList
          .newBuilder()
          .addGroups(dealerAccessGroup)
          .build()

      when(cabinetApiClient.getClientAccessGroups(dealer)).thenReturnF(dealerAccessGroups)

      val clientGroup = 777L

      dealerManager
        .linkUser(dealer, optUser = Some(user), optEmail = None, clientGroup.toString)
        .failed
        .futureValue shouldBe an[AccessGroupNotFound]
    }

    "fail on link user to dealer and user not found after link" in new Fixture {
      val dealer = DealerUserRefGen.next
      val user = PrivateUserRefGen.next

      val dealerAccessGroup = DealerAccessGroupGen.next
      val dealerAccessGroupId = dealerAccessGroup.getId.toString

      val userEssentials = UserEssentialsGen.next.toBuilder.clearClientId().build()
      when(passportClient.getUserEssentials(?, ?)(?)).thenReturnF(userEssentials)

      val dealerAccessGroups =
        GroupsList
          .newBuilder()
          .addGroups(dealerAccessGroup)
          .build()

      when(cabinetApiClient.getClientAccessGroups(dealer)).thenReturnF(dealerAccessGroups)

      when(passportClient.linkDealerUser(dealer, user, dealerAccessGroupId))
        .thenReturnF(())

      val anotherUserBuilder = PassportUserGen.next.toBuilder
      anotherUserBuilder.getProfileBuilder.getAutoruBuilder.setClientGroup(dealerAccessGroupId)

      val anotherUser = anotherUserBuilder.build()

      val dealerUsers =
        UserIdsResult
          .newBuilder()
          .addUsers(anotherUser)
          .build()

      when(passportClient.getDealerUsers(dealer)).thenReturnF(dealerUsers)

      dealerManager
        .linkUser(dealer, optUser = Some(user), optEmail = None, dealerAccessGroupId)
        .failed
        .futureValue shouldBe an[UserNotFoundException]
    }
  }

  "DealerManager.editUser()" should {
    implicit val r: Request = privateUserRequest

    "edit dealer user" in new Fixture {
      val dealer = DealerUserRefGen.next
      val user = PrivateUserRefGen.next

      val dealerAccessGroup = DealerAccessGroupGen.next
      val dealerAccessGroupId = dealerAccessGroup.getId.toString

      val passportUserBuilder = PassportUserGen.next.toBuilder
      passportUserBuilder.setId(user.uid.toString)
      passportUserBuilder.getProfileBuilder.getAutoruBuilder.setClientGroup(dealerAccessGroupId)

      val passportUser = passportUserBuilder.build()

      val dealerAccessGroups =
        GroupsList
          .newBuilder()
          .addGroups(dealerAccessGroup)
          .build()

      when(cabinetApiClient.getClientAccessGroups(dealer)).thenReturnF(dealerAccessGroups)

      when(passportClient.linkDealerUser(dealer, user, dealerAccessGroupId))
        .thenReturnF(())

      val dealerUsers =
        UserIdsResult
          .newBuilder()
          .addUsers(passportUser)
          .build()

      when(passportClient.getDealerUsers(dealer)).thenReturnF(dealerUsers)

      val expected =
        DealerUser
          .newBuilder()
          .setUser(passportUser)
          .setAccess {
            AccessGrants
              .newBuilder()
              .setGroup(dealerAccessGroup.toBuilder.clearGrants())
              .addAllGrants(dealerAccessGroup.getGrantsList)
          }
          .build()

      dealerManager
        .editUser(dealer, user, dealerAccessGroupId)
        .futureValue shouldBe expected
    }

    "fail on edit dealer user and the same user" in new Fixture {
      val dealer = DealerUserRefGen.next

      val dealerAccessGroup = DealerAccessGroupGen.next
      val dealerAccessGroupId = dealerAccessGroup.getId.toString

      dealerManager
        .editUser(dealer, privateUser, dealerAccessGroupId)
        .failed
        .futureValue shouldBe an[IllegalArgumentException]
    }

    "fail on edit dealer user and group does not exists" in new Fixture {
      val dealer = DealerUserRefGen.next
      val user = PrivateUserRefGen.next

      val dealerAccessGroup = DealerAccessGroupGen.next

      val dealerAccessGroups =
        GroupsList
          .newBuilder()
          .addGroups(dealerAccessGroup)
          .build()

      when(cabinetApiClient.getClientAccessGroups(dealer)).thenReturnF(dealerAccessGroups)

      val clientGroup = 777L

      dealerManager
        .editUser(dealer, user, clientGroup.toString)
        .failed
        .futureValue shouldBe an[AccessGroupNotFound]
    }

    "fail on edit dealer user and user not found after link" in new Fixture {
      val dealer = DealerUserRefGen.next
      val user = PrivateUserRefGen.next

      val dealerAccessGroup = DealerAccessGroupGen.next
      val dealerAccessGroupId = dealerAccessGroup.getId.toString

      val dealerAccessGroups =
        GroupsList
          .newBuilder()
          .addGroups(dealerAccessGroup)
          .build()

      when(cabinetApiClient.getClientAccessGroups(dealer)).thenReturnF(dealerAccessGroups)

      when(passportClient.linkDealerUser(dealer, user, dealerAccessGroupId))
        .thenReturnF(())

      val anotherUserBuilder = PassportUserGen.next.toBuilder
      anotherUserBuilder.getProfileBuilder.getAutoruBuilder.setClientGroup(dealerAccessGroupId)

      val anotherUser = anotherUserBuilder.build()

      val dealerUsers =
        UserIdsResult
          .newBuilder()
          .addUsers(anotherUser)
          .build()

      when(passportClient.getDealerUsers(dealer)).thenReturnF(dealerUsers)

      dealerManager
        .editUser(dealer, user, dealerAccessGroupId)
        .failed
        .futureValue shouldBe an[UserNotFoundException]
    }
  }

  "DealerManager.unlinkUser()" should {
    implicit val r: Request = privateUserRequest

    "unlink dealer user" in new Fixture {
      val dealer = DealerUserRefGen.next
      val user = PrivateUserRefGen.next

      when(passportClient.unlinkDealerUser(dealer, user))
        .thenReturnF(())

      dealerManager
        .unlinkUser(dealer, user)
        .futureValue shouldBe ManagerUtils.SuccessResponse
    }

    "fail on delete dealer user and the same user" in new Fixture {
      val dealer = DealerUserRefGen.next

      dealerManager
        .unlinkUser(dealer, privateUser)
        .failed
        .futureValue shouldBe an[IllegalArgumentException]
    }
  }

  "DealerManager.accessResourcesList()" should {
    implicit val r: Request = request

    "get access resources list" in new Fixture {
      val dealer = DealerUserRefGen.next

      val expected =
        ResourcesList
          .newBuilder()
          .addResources {
            Resource
              .newBuilder()
              .setAlias(ResourceAlias.DASHBOARD)
              .setName("Dashboard")
          }
          .build()

      when(cabinetApiClient.getAccessResources(dealer))
        .thenReturnF(expected)

      dealerManager
        .accessResourcesList(dealer)
        .futureValue shouldBe expected
    }
  }

  "DealerManager.accessGroupsList()" should {
    implicit val r: Request = request

    "get access groups list" in new Fixture {
      val dealer = DealerUserRefGen.next

      val expected =
        GroupsList
          .newBuilder()
          .addGroups {
            Group
              .newBuilder()
              .setId(1L)
          }
          .build()

      when(cabinetApiClient.getClientAccessGroups(dealer))
        .thenReturnF(expected)

      dealerManager
        .accessGroupsList(dealer)
        .futureValue shouldBe expected
    }
  }

  "DealerManager.createAccessGroup()" should {
    implicit val r: Request = request

    "create access group" in new Fixture {
      val dealer = DealerUserRefGen.next
      val group = DealerAccessGroupGen.next

      val dealerAccessGroups =
        GroupsList
          .newBuilder()
          .addGroups {
            Group
              .newBuilder()
              .setName("Another access group")
          }
          .build()

      when(cabinetApiClient.getClientAccessGroups(dealer))
        .thenReturnF(dealerAccessGroups)

      val expected = group.toBuilder.clearId().build()

      when(cabinetApiClient.putClientAccessGroup(dealer, expected))
        .thenReturnF(group)

      dealerManager
        .createAccessGroup(dealer, group)
        .futureValue shouldBe group
    }

    "fail on exists group with the same name" in new Fixture {
      val dealer = DealerUserRefGen.next
      val group = DealerAccessGroupGen.next

      val dealerAccessGroups =
        GroupsList
          .newBuilder()
          .addGroups {
            Group
              .newBuilder()
              .setName(group.getName)
          }
          .build()

      when(cabinetApiClient.getClientAccessGroups(dealer))
        .thenReturnF(dealerAccessGroups)

      dealerManager
        .createAccessGroup(dealer, group)
        .failed
        .futureValue shouldBe an[IllegalArgumentException]
    }
  }

  "DealerManager.editAccessGroup()" should {
    implicit val r: Request = request

    "edit access group" in new Fixture {
      val dealer = DealerUserRefGen.next
      val group = DealerAccessGroupGen.next

      val groupId = 12L
      val expected = group.toBuilder.setId(groupId).build()

      when(cabinetApiClient.putClientAccessGroup(dealer, expected))
        .thenReturnF(expected)

      dealerManager
        .editAccessGroup(dealer, groupId.toString, group)
        .futureValue shouldBe expected
    }
  }

  "DealerManager.deleteAccessGroup()" should {
    implicit val r: Request = request

    "delete access group" in new Fixture {
      val dealer = DealerUserRefGen.next

      val groupId = 1L
      val anotherGroupId = 2L

      val dealerUsers =
        UserIdsResult
          .newBuilder()
          .addUsers {
            passportUser(PrivateUserRefGen.next, anotherGroupId.toString)
          }
          .build()

      when(passportClient.getDealerUsers(dealer))
        .thenReturnF(dealerUsers)

      when(cabinetApiClient.deleteClientAccessGroup(dealer, groupId.toString))
        .thenReturnF(())

      dealerManager
        .deleteAccessGroup(dealer, groupId.toString)
        .futureValue shouldBe ManagerUtils.SuccessResponse
    }

    "fail on delete access group with users" in new Fixture {
      val dealer = DealerUserRefGen.next
      val groupId = 1L

      val dealerUsers =
        UserIdsResult
          .newBuilder()
          .addUsers {
            passportUser(PrivateUserRefGen.next, groupId.toString)
          }
          .addUsers {
            passportUser(PrivateUserRefGen.next, groupId.toString)
          }
          .build()

      when(passportClient.getDealerUsers(dealer))
        .thenReturnF(dealerUsers)

      dealerManager
        .deleteAccessGroup(dealer, groupId.toString)
        .failed
        .futureValue shouldBe an[IllegalArgumentException]
    }
  }

  "DealerManager.dealerPhonesRedirects()" should {
    implicit val r: Request = request

    "get DealerPhones (withOffers=true) with platform info" in new Fixture {
      val paging = Paging(1, 10)
      val offer = OfferGen.next
      val phone = PhoneObjectGen.next
      val phoneExpected = phone.toBuilder.setPlatform("avito")

      val expected = DealerPhones
        .newBuilder()
        .addPhonesWithOffers(
          PhoneWithOffer
            .newBuilder()
            .setCategory(Category.CARS)
            .setSection(Section.NEW)
            .setOffer(offer)
            .setPhone(phoneExpected)
            .build()
        )
        .setPagination(
          ResponsePagination.newBuilder().setPageNum(1).setPageSize(10).setTotalCount(1).setTotalPageCount(1).build()
        )
        .build()

      val originalPhonePrefix = Some("+" + PhoneGen.next)
      val redirectPhonePrefix = Some("+" + PhoneGen.next)

      val dealerPhonesRedirects = TeleponyInfoListResponse
        .newBuilder()
        .addTeleponyInfo(TeleponyInfo.newBuilder().setCategoryTag("cars").build())
        .build()

      val callInfoResponse = CallInfoResponse
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setPlatform("avito")
        .setOfferId(offer.getId)
        .setClientId(dealer.clientId)
        .build()

      when(dealerPonyClient.getTeleponyInfoForDealerPhones(?)(?)).thenReturnF(dealerPhonesRedirects)
      when(redirectManager.getActivePhonesRedirects(?, ?, ?, ?)(?)).thenReturnF(Seq(phone))
      when(dealerPonyClient.callInfo(?, ?, ?)(?)).thenReturnF(callInfoResponse)
      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)

      dealerManager
        .dealerPhonesRedirects(
          dealer.clientId,
          withOffers = true,
          originalPhonePrefix,
          redirectPhonePrefix,
          platform = None,
          ConfirmedStatuses.All,
          paging
        )
        .futureValue shouldBe expected
    }

    "get DealerPhones(withOffers=true) with empty platform (should set to autoru)" in new Fixture {
      val paging = Paging(1, 10)
      val offer = OfferGen.next
      val phone = PhoneObjectGen.next
      val phoneExpected = phone.toBuilder.setPlatform("autoru")

      val expected = DealerPhones
        .newBuilder()
        .addPhonesWithOffers(
          PhoneWithOffer
            .newBuilder()
            .setCategory(Category.CARS)
            .setSection(Section.NEW)
            .setOffer(offer)
            .setPhone(phoneExpected)
            .build()
        )
        .setPagination(
          ResponsePagination.newBuilder().setPageNum(1).setPageSize(10).setTotalCount(1).setTotalPageCount(1).build()
        )
        .build()

      val originalPhonePrefix = Some("+" + PhoneGen.next)
      val redirectPhonePrefix = Some("+" + PhoneGen.next)

      val dealerPhonesRedirects = TeleponyInfoListResponse
        .newBuilder()
        .addTeleponyInfo(TeleponyInfo.newBuilder().setCategoryTag("cars").build())
        .build()

      val callInfoResponse = CallInfoResponse
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setOfferId(offer.getId)
        .setClientId(dealer.clientId)
        .build()

      when(dealerPonyClient.getTeleponyInfoForDealerPhones(?)(?)).thenReturnF(dealerPhonesRedirects)
      when(redirectManager.getActivePhonesRedirects(?, ?, ?, ?)(?)).thenReturnF(Seq(phone))
      when(dealerPonyClient.callInfo(?, ?, ?)(?)).thenReturnF(callInfoResponse)
      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)

      dealerManager
        .dealerPhonesRedirects(
          dealer.clientId,
          withOffers = true,
          originalPhonePrefix,
          redirectPhonePrefix,
          platform = None,
          ConfirmedStatuses.All,
          paging
        )
        .futureValue shouldBe expected
    }

    "get DealerPhones(withOffeers=false) with platform info" in new Fixture {
      val paging = Paging(1, 10)
      val phone = PhoneObjectGen.next
      val phoneExpected = phone.toBuilder.setPlatform("avito")

      val expected = DealerPhones
        .newBuilder()
        .addPhones(phoneExpected)
        .setPagination(
          ResponsePagination.newBuilder().setPageNum(1).setPageSize(10).setTotalCount(1).setTotalPageCount(1).build()
        )
        .build()

      val originalPhonePrefix = Some("+" + PhoneGen.next)
      val redirectPhonePrefix = Some("+" + PhoneGen.next)

      val dealerPhonesRedirects = TeleponyInfoListResponse
        .newBuilder()
        .addTeleponyInfo(TeleponyInfo.newBuilder().setCategoryTag("cars").build())
        .build()

      val callInfoResponse = CallInfoResponse
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setPlatform("avito")
        .setClientId(dealer.clientId)
        .build()

      when(dealerPonyClient.getTeleponyInfoForDealerPhones(?)(?)).thenReturnF(dealerPhonesRedirects)
      when(redirectManager.getActivePhonesRedirects(?, ?, ?, ?)(?)).thenReturnF(Seq(phone))
      when(dealerPonyClient.callInfo(?, ?, ?)(?)).thenReturnF(callInfoResponse)

      dealerManager
        .dealerPhonesRedirects(
          dealer.clientId,
          withOffers = false,
          originalPhonePrefix,
          redirectPhonePrefix,
          platform = None,
          ConfirmedStatuses.All,
          paging
        )
        .futureValue shouldBe expected
    }

    "get DealerPhones(withOffers=false) with empty platform (should set to autoru)" in new Fixture {
      val paging = Paging(1, 10)
      val phone = PhoneObjectGen.next
      val phoneExpected = phone.toBuilder.setPlatform("autoru")

      val expected = DealerPhones
        .newBuilder()
        .addPhones(phoneExpected)
        .setPagination(
          ResponsePagination.newBuilder().setPageNum(1).setPageSize(10).setTotalCount(1).setTotalPageCount(1).build()
        )
        .build()

      val originalPhonePrefix = Some("+" + PhoneGen.next)
      val redirectPhonePrefix = Some("+" + PhoneGen.next)

      val dealerPhonesRedirects = TeleponyInfoListResponse
        .newBuilder()
        .addTeleponyInfo(TeleponyInfo.newBuilder().setCategoryTag("cars").build())
        .build()

      val callInfoResponse = CallInfoResponse
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setClientId(dealer.clientId)
        .build()

      when(dealerPonyClient.getTeleponyInfoForDealerPhones(?)(?)).thenReturnF(dealerPhonesRedirects)
      when(redirectManager.getActivePhonesRedirects(?, ?, ?, ?)(?)).thenReturnF(Seq(phone))
      when(dealerPonyClient.callInfo(?, ?, ?)(?)).thenReturnF(callInfoResponse)

      dealerManager
        .dealerPhonesRedirects(
          dealer.clientId,
          withOffers = false,
          originalPhonePrefix,
          redirectPhonePrefix,
          platform = None,
          ConfirmedStatuses.All,
          paging
        )
        .futureValue shouldBe expected
    }

    "get DealerPhones matched on unconfirmed redirects" in new Fixture {
      val paging = Paging(1, 10)
      val phone1 = Phone
        .newBuilder()
        .setPhone("+7 111 111-11-11")
        .setPlatform("avito")
        .setTeleponyInfo(TeleponyInfo.newBuilder().setTag("category=CARS#platform=AVITO"))
        .build()
      val phone2 = Phone
        .newBuilder()
        .setPhone("+7 (111) 222 33 44")
        .setPlatform("avito")
        .setTeleponyInfo(TeleponyInfo.newBuilder().setTag("category=CARS#platform=AVITO"))
        .build()
      val phoneExpected = phone2

      val expected = DealerPhones
        .newBuilder()
        .addPhones(phoneExpected)
        .setPagination(
          ResponsePagination.newBuilder().setPageNum(1).setPageSize(10).setTotalCount(1).setTotalPageCount(1).build()
        )
        .build()

      val originalPhonePrefix = Some("+" + PhoneGen.next)
      val redirectPhonePrefix = Some("+" + PhoneGen.next)

      val dealerPhonesRedirects = TeleponyInfoListResponse
        .newBuilder()
        .addTeleponyInfo(TeleponyInfo.newBuilder().setCategoryTag("cars").build())
        .build()

      val callInfoResponse = CallInfoResponse
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setPlatform("avito")
        .setClientId(dealer.clientId)
        .build()

      val accepted = GetRedirectsByConfirmationResponse
        .newBuilder()
        .addPhones(RedirectPhone.newBuilder().setPlatform("avito").setRedirectPhone("+71112223344"))
        .build()

      val teleponyInfoResponse = Map(
        TeleponyInfoRequest.newBuilder().setPlatform("avito").build() -> TeleponyInfo
          .newBuilder()
          .setTag("platform=avito")
          .build()
      )

      when(dealerPonyClient.getTeleponyInfoForDealerPhones(?)(?)).thenReturnF(dealerPhonesRedirects)
      when(redirectManager.getActivePhonesRedirects(?, ?, ?, ?)(?)).thenReturnF(Seq(phone1, phone2))
      when(dealerPonyClient.callInfo(?, ?, ?)(?)).thenReturnF(callInfoResponse)
      when(dealerPonyClient.getTeleponyInfoBatch(?, ?)(?)).thenReturnF(teleponyInfoResponse)
      when(calltrackingClient.getRedirectsByConfirmation(?)(?)).thenReturnF(accepted)

      dealerManager
        .dealerPhonesRedirects(
          dealer.clientId,
          withOffers = false,
          originalPhonePrefix,
          redirectPhonePrefix,
          platform = None,
          ConfirmedStatuses.Unconfirmed,
          paging
        )
        .futureValue shouldBe expected
    }

    "get DealerPhones matched on confirmed redirects" in new Fixture {
      val paging = Paging(1, 10)
      val phone1 = Phone
        .newBuilder()
        .setTeleponyInfo(TeleponyInfo.newBuilder().setTag("platform=avito").build())
        .setPhone("+7 111 111-11-11")
        .setPlatform("avito")
        .build()
      val phone2 = Phone
        .newBuilder()
        .setTeleponyInfo(TeleponyInfo.newBuilder().setTag("platform=avito").build())
        .setPhone("+7 (111) 111 11 22")
        .setPlatform("avito")
        .build()
      val phone3 = Phone
        .newBuilder()
        .setTeleponyInfo(TeleponyInfo.newBuilder().setTag("platform=direct").build())
        .setPhone("+71112223344")
        .setPlatform("direct")
        .build()
      val phone4 = Phone
        .newBuilder()
        .setTeleponyInfo(TeleponyInfo.newBuilder().build())
        .setPhone("+71112223355")
        .setPlatform("autoru")
        .build()

      val expected = DealerPhones
        .newBuilder()
        .addPhones(phone2)
        .addPhones(phone3)
        .addPhones(phone4)
        .setPagination(
          ResponsePagination.newBuilder().setPageNum(1).setPageSize(10).setTotalCount(3).setTotalPageCount(1).build()
        )
        .build()

      val originalPhonePrefix = Some("+" + PhoneGen.next)
      val redirectPhonePrefix = Some("+" + PhoneGen.next)

      val dealerPhonesRedirects = TeleponyInfoListResponse
        .newBuilder()
        .addTeleponyInfo(TeleponyInfo.newBuilder().setCategoryTag("cars").build())
        .build()

      val callInfoResponse = CallInfoResponse
        .newBuilder()
        .setCategory(Category.CARS)
        .setSection(Section.NEW)
        .setPlatform("avito")
        .setClientId(dealer.clientId)
        .build()

      val unconfirmed = GetRedirectsByConfirmationResponse
        .newBuilder()
        .addPhones(RedirectPhone.newBuilder().setPlatform("avito").setRedirectPhone("+71111111111"))
        .build()

      val teleponyInfoResponse = Map(
        TeleponyInfoRequest.newBuilder().setPlatform("avito").build() -> TeleponyInfo
          .newBuilder()
          .setTag("platform=avito")
          .build()
      )

      when(dealerPonyClient.getTeleponyInfoForDealerPhones(?)(?)).thenReturnF(dealerPhonesRedirects)
      when(redirectManager.getActivePhonesRedirects(?, ?, ?, ?)(?)).thenReturnF(Seq(phone1, phone2, phone3, phone4))
      when(dealerPonyClient.callInfo(?, eq(""), ?)(?))
        .thenReturnF(callInfoResponse.toBuilder.clearPlatform().build())
      when(dealerPonyClient.callInfo(?, eq("platform=avito"), ?)(?)).thenReturnF(callInfoResponse)
      when(dealerPonyClient.callInfo(?, eq("platform=direct"), ?)(?))
        .thenReturnF(callInfoResponse.toBuilder.setPlatform("direct").build())
      when(dealerPonyClient.getTeleponyInfoBatch(?, ?)(?)).thenReturnF(teleponyInfoResponse)
      when(calltrackingClient.getRedirectsByConfirmation(?)(?)).thenReturnF(unconfirmed)

      dealerManager
        .dealerPhonesRedirects(
          dealer.clientId,
          withOffers = false,
          originalPhonePrefix,
          redirectPhonePrefix,
          platform = None,
          ConfirmedStatuses.Confirmed,
          paging
        )
        .futureValue shouldBe expected
    }
  }
  "DealerManager.updateAgency()" should {

    "assert throws CustomerAccessForbidden on non NEW dealer status" in new Fixture {
      implicit val r: Request = request

      val clientView = ClientView(
        properties = ClientPropertiesView(
          status = "active",
          regionId = 123L
        )
      )

      when(cabinetApiClient.getClient(?)(?)).thenReturnF(clientView)

      assertThrows[CustomerAccessForbidden](
        dealerManager.updateAgency(clientId = 1L, agencyId = 123).await
      )

      verify(cabinetApiClient).getClient(clientId = eq(1L))(?)
      verifyNoMoreInteractions(cabinetApiClient)
    }

    "update agency if user is Manager even if non NEW dealer status" in new Fixture {
      implicit val r: Request = managerRequest

      val clientView = ClientView(
        properties = ClientPropertiesView(
          status = "active",
          regionId = 123L
        )
      )
      when(cabinetApiClient.getClient(?)(?)).thenReturnF(clientView)
      when(cabinetApiClient.updateAgency(?, ?)(?)).thenReturnF(ManagerUtils.SuccessResponse)

      dealerManager
        .updateAgency(clientId = 1L, agencyId = 123)(r)
        .futureValue shouldBe ManagerUtils.SuccessResponse

      verify(cabinetApiClient).getClient(clientId = eq(1L))(?)
      verify(cabinetApiClient).updateAgency(clientId = eq(1L), agencyId = eq(123L))(?)
    }

    "return OK if dealer has NEW status" in new Fixture {
      implicit val r: Request = request

      val clientView = ClientView(
        properties = ClientPropertiesView(
          status = "new",
          regionId = 123L
        )
      )

      when(cabinetApiClient.getClient(?)(?)).thenReturnF(clientView)
      when(cabinetApiClient.updateAgency(?, ?)(?)).thenReturnF(ManagerUtils.SuccessResponse)

      dealerManager.updateAgency(clientId = 1L, agencyId = 123).futureValue shouldBe ManagerUtils.SuccessResponse

      verify(cabinetApiClient).getClient(clientId = eq(1L))(?)
      verify(cabinetApiClient).updateAgency(clientId = eq(1L), agencyId = eq(123L))(?)
      verifyNoMoreInteractions(cabinetApiClient)
    }

  }
}
