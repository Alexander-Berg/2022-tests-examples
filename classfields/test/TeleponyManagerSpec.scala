package auto.dealers.dealer_pony.logic.test

import auto.common.clients.calltracking.CalltrackingClient
import auto.common.clients.calltracking.testkit.CalltrackingClientMock
import auto.common.manager.cabinet.model.Client
import auto.common.manager.cabinet.testkit.CabinetManagerMock
import auto.common.manager.salesman.testkit.{SalesmanManagerEmpty, SalesmanManagerMock}
import auto.common.model.offers.OfferIdentity
import auto.common.model.{ClientId, PoiId}
import auto.dealers.dealer_pony.logic.TeleponyManager._
import auto.dealers.dealer_pony.logic._
import auto.dealers.dealer_pony.model.Tag._
import auto.dealers.dealer_pony.model._
import auto.dealers.dealer_pony.storage.dao.RedirectConfigOverrideForRegions
import auto.dealers.dealer_pony.storage.resources.DefaultRedirectConfigOverrideForRegions
import auto.dealers.dealer_pony.storage.resources.DefaultRedirectConfigOverrideForRegions.SettingsOverrides
import common.geobase.model.RegionIds
import common.geobase.model.RegionIds._
import common.zio.logging.Logging
import ru.auto.api.api_offer_model._
import ru.auto.api.cars_model.CarInfo
import ru.auto.api.common_model.Actions
import ru.auto.api.moto_model.MotoInfo
import ru.auto.calltracking.proto.calls_service.{GetSettingsRequest, SettingsResponse}
import ru.auto.calltracking.proto.model.Settings
import ru.auto.dealer_calls_auction.proto.api_model.AuctionCurrentState
import ru.auto.dealer_pony.palma.proto.palma.{Transport => ProtoTransportType}
import ru.auto.dealer_pony.proto.api_model.TeleponyInfoRequest
import zio._
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._

import scala.concurrent.duration._

object TeleponyManagerSpec extends DefaultRunnableSpec {
  val poiId = PoiId(12345)
  val clientId = ClientId(87)
  val callPlatform = Platform.Autoru
  val category = "MOTO"
  val section = "NEW"

  override val spec: ZSpec[TestEnvironment, Any] = suite("TeleponyManagerSpec")(
    testM("check that Palma override settings work the same way old hardcoded logic did") {
      val testCases = for {
        category <- Gen.elements(Category.CARS, Category.MOTO, Category.TRUCKS)
        section <- Gen.elements(Section.NEW, Section.USED)
        regionId <- Gen.elements(
          RegionId(0),
          MoscowAndMoscowRegion,
          SaintPetersburgAndLeningradOblast,
          RegSverdlovsk,
          RegChelyabinsk
        )
        paidCall <- Gen.boolean
        callTrackingEnabled <- Gen.boolean
        callTrackingByOffer <- Gen.boolean
      } yield {

        val basicOffer = Offer
          .apply(sellerType = SellerType.COMMERCIAL, category = category, section = section)
          .withSalon(Salon(salonId = poiId.value))
          .withUserRef("dealer:87")

        val offer = if (category == Category.CARS) {
          basicOffer.withCarInfo(CarInfo(mark = Some("BMW")))
        } else {
          basicOffer.withMotoInfo(MotoInfo(mark = "HONDA"))
        }

        val settings =
          SettingsResponse.defaultInstance.withSettings(Settings(callTrackingEnabled, false, callTrackingByOffer))

        val client = Client(clientId, regionId)

        (client, settings, offer, paidCall)
      }

      checkAllM(testCases) { case (client, settings, offer, paidCall) =>
        val mocks =
          (
            CabinetManagerMock.GetClient(equalTo(clientId), value(client)) ++
              SalesmanManagerMock.IsPaidCallInRegion(
                equalTo((clientId, client.regionId, offer.category, offer.section)),
                value(paidCall)
              ) ++ CalltrackingClientMock.GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))
          ).toLayer >+>
            overrideConfigForRegions >+>
            Logging.live >+>
            TeleponyManagerLive.layer

        val reference =
          howTeleponyInfoWasPreviouslyConfiguredNew(poiId, client.regionId, paidCall, settings.getSettings, offer)

        TeleponyManager(_.getTeleponyInfo(offer, experiments = Nil))
          .provideCustomLayer(mocks)
          .fold(Left(_), Right(_))
          .map { result =>
            assertTrue(result == reference)
          }

      }
    },
    testM("redirects allowed for offer") {
      val regionId = RegionIds.RegionId(0)
      val CallTrackingEnabled = true
      val CallTrackingByOffer = false

      val offer = Offer
        .apply(sellerType = SellerType.COMMERCIAL, category = Category.CARS, section = Section.NEW)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val settings =
        SettingsResponse.defaultInstance.withSettings(
          Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = CallTrackingByOffer)
        )

      val client = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(client))
          ++ SalesmanManagerMock.IsPaidCallInRegion(
            equalTo((client.id, client.regionId, offer.category, offer.section)),
            value(false)
          )
          ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      TeleponyManager(_.redirectsAllowedForOffer(offer))
        .provideCustomLayer(mocks)
        .map(res => assertTrue(res == true))

    },
    testM("redirects not allowed for offer (calltracking is off)") {
      val regionId = RegionIds.RegionId(0)
      val CallTrackingEnabled = false
      val CallTrackingByOffer = false

      val offer = Offer
        .apply(sellerType = SellerType.COMMERCIAL, category = Category.CARS, section = Section.NEW)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val settings =
        SettingsResponse.defaultInstance.withSettings(
          Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = CallTrackingByOffer)
        )

      val client = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(client))
          ++ SalesmanManagerMock.IsPaidCallInRegion(
            equalTo((client.id, client.regionId, offer.category, offer.section)),
            value(false)
          ) ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      TeleponyManager(_.redirectsAllowedForOffer(offer))
        .provideCustomLayer(mocks)
        .map(res => assertTrue(res == false))

    },
    testM("redirects not allowed for offer (Not a dealer)") {
      val offer = Offer
        .apply(sellerType = SellerType.PRIVATE, category = Category.CARS, section = Section.NEW)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val mocks =
        CabinetManagerMock.empty >+>
          calltrackingService >+>
          SalesmanManagerEmpty.empty >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      assertM(
        TeleponyManager(_.redirectsAllowedForOffer(offer))
          .provideCustomLayer(mocks)
          .run
      )(
        fails(isSubtype[TeleponyManager.NotDealerError](anything))
      )
    },
    testM("dealer's callback config is same as offer redirect config") {
      val regionId = RegionIds.RegionId(0)
      val CallTrackingEnabled = true
      val CallTrackingByOffer = false

      val offer = Offer
        .apply(sellerType = SellerType.COMMERCIAL, category = Category.CARS, section = Section.NEW)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val settings =
        SettingsResponse.defaultInstance.withSettings(
          Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = CallTrackingByOffer)
        )

      val client = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(client))
          ++ SalesmanManagerMock
            .IsPaidCallInRegion(
              equalTo((clientId, client.regionId, offer.category, offer.section)),
              value(true)
            )
          ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      for {
        callbackInfo <- TeleponyManager(_.callbackTeleponyInfo(offer, experiments = Nil)).provideCustomLayer(mocks)
        teleponyInfo <- TeleponyManager(_.getTeleponyInfo(offer, experiments = Nil)).provideCustomLayer(mocks)
      } yield assertTrue(callbackInfo == teleponyInfo)
    },
    testM("dealer's callback config doesn't require calltracking option being set") {
      val regionId = RegionIds.RegionId(0)
      val CallTrackingEnabled = false
      val CallTrackingByOffer = false

      val offer = Offer
        .apply(sellerType = SellerType.COMMERCIAL, category = Category.CARS, section = Section.NEW)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")
      val settings =
        SettingsResponse.defaultInstance.withSettings(
          Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = CallTrackingByOffer)
        )

      val client = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(client))
          ++ SalesmanManagerMock
            .IsPaidCallInRegion(
              equalTo((clientId, client.regionId, offer.category, offer.section)),
              value(false)
            ) ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      assertM(
        TeleponyManager(_.callbackTeleponyInfo(offer, experiments = Nil))
          .provideCustomLayer(mocks)
      )(
        isSubtype[TeleponyInfo](anything)
      )
    },
    testM("getting callInfoResponse with parsed tags") {
      val objectId = s"dealer-$poiId"
      val offerId = 12345
      val tags = s"offer_id=$offerId#category=$category#section=$section#platform=$callPlatform"

      val mocks =
        CabinetManagerMock.GetClientIdByPoi(equalTo(poiId), value(clientId)).toLayer >+>
          SalesmanManagerEmpty.empty >+>
          calltrackingService >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      val callResult = TeleponyManager.CallInfo(
        clientId,
        offerId.toString,
        Category.MOTO,
        Section.NEW,
        Some(callPlatform)
      )

      TeleponyManager(_.callInfo(objectId, tags))
        .provideCustomLayer(mocks)
        .map(callR => assertTrue(callR == callResult))
    },
    testM("Getting error for ObjectId format") {
      val objectId = s"dealerMistake-$poiId"
      val offerId = 12345
      val tags = s"offer_id=$offerId#category=$category#section=$section#platform=$callPlatform"

      val mocks =
        CabinetManagerMock.GetClientIdByPoi(equalTo(poiId), value(clientId)).toLayer >+>
          SalesmanManagerEmpty.empty >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          calltrackingService >+>
          TeleponyManagerLive.layer

      assertM(TeleponyManager(_.callInfo(objectId, tags)).provideCustomLayer(mocks).run)(
        fails(isSubtype[TeleponyManager.TagParsingError](anything))
      )
    },
    testM("Getting empty string for bad offerId") {
      val objectId = s"dealer-$poiId"
      val offerId = "12badId1234"
      val tags = s"offer_id=$offerId#category=$category#section=$section#platform=$callPlatform"

      val mocks =
        CabinetManagerMock.GetClientIdByPoi(equalTo(poiId), value(clientId)).toLayer >+>
          SalesmanManagerEmpty.empty >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          calltrackingService >+>
          TeleponyManagerLive.layer

      val callResult = TeleponyManager.CallInfo(
        clientId,
        "",
        Category.MOTO,
        Section.NEW,
        Some(callPlatform)
      )

      TeleponyManager(_.callInfo(objectId, tags))
        .provideCustomLayer(mocks)
        .map(callR => assertTrue(callR == callResult))
    },
    testM("return full telepony info for dealer offer") {
      val objectId = s"dealer-$poiId"
      val tag = "category=CARS#section=NEW"
      val regionId = RegionIds.RegionId(0)
      val CallTrackingEnabled = true
      val CallTrackingByOffer = false
      val ttl = 2.days.toSeconds

      val offer = Offer
        .apply(sellerType = SellerType.COMMERCIAL, category = Category.CARS, section = Section.NEW)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val settings =
        SettingsResponse.defaultInstance.withSettings(
          Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = CallTrackingByOffer)
        )

      val client = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(client))
          ++ SalesmanManagerMock
            .IsPaidCallInRegion(
              equalTo((clientId, client.regionId, offer.category, offer.section)),
              value(false)
            ) ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      val teleponyInfo = TeleponyInfo(
        domain = "auto-dealers",
        objectId = objectId,
        tag = tag,
        ttl = ttl
      )

      TeleponyManager(_.getTeleponyInfo(offer, experiments = Nil))
        .provideCustomLayer(mocks)
        .map(teleponyR => assertTrue(teleponyR == teleponyInfo))

    },
    testM("return full telepony info for dealer offer on paid tariff") {
      val objectId = s"dealer-$poiId"
      val regionId = MoscowAndMoscowRegion
      val CallTrackingEnabled = true
      val CallTrackingByOffer = false
      val ttl = 2.days.toSeconds
      val offerId = "1111111-11111"

      val offer = Offer
        .apply(id = offerId, sellerType = SellerType.COMMERCIAL, category = Category.CARS, section = Section.USED)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val tag = s"category=CARS#section=USED#offer_id=${offer.id}"

      val settings =
        SettingsResponse.defaultInstance.withSettings(
          Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = CallTrackingByOffer)
        )

      val client = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(client))
          ++ SalesmanManagerMock
            .IsPaidCallInRegion(
              equalTo((clientId, client.regionId, offer.category, offer.section)),
              value(true)
            ) ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      val teleponyInfo = TeleponyInfo(
        domain = "autoru_billing",
        objectId = objectId,
        tag = tag,
        ttl = ttl,
        geoId = client.regionId.id,
        phoneType = PhoneType.Mobile.value
      )

      TeleponyManager(_.getTeleponyInfo(offer, experiments = Nil))
        .provideCustomLayer(mocks)
        .map(teleponyR => assertTrue(teleponyR == teleponyInfo))

    },
    testM("return full telepony info for dealer offer on paid tariff with call tracking off") {
      val objectId = s"dealer-$poiId"
      val regionId = MoscowAndMoscowRegion
      val CallTrackingEnabled = false
      val CallTrackingByOffer = false
      val ttl = 2.days.toSeconds
      val offerId = "1111111-11111"

      val offer = Offer
        .apply(id = offerId, sellerType = SellerType.COMMERCIAL, category = Category.CARS, section = Section.USED)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val tag = s"category=CARS#section=USED#offer_id=${offer.id}"

      val settings =
        SettingsResponse.defaultInstance.withSettings(
          Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = CallTrackingByOffer)
        )

      val client = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(client))
          ++ SalesmanManagerMock
            .IsPaidCallInRegion(
              equalTo((clientId, client.regionId, offer.category, offer.section)),
              value(true)
            )
          ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      val teleponyInfo = TeleponyInfo(
        domain = "autoru_billing",
        objectId = objectId,
        tag = tag,
        ttl = ttl,
        geoId = client.regionId.id,
        phoneType = PhoneType.Mobile.value
      )

      TeleponyManager(_.getTeleponyInfo(offer, experiments = Nil))
        .provideCustomLayer(mocks)
        .map(teleponyR => assertTrue(teleponyR == teleponyInfo))

    },
    testM("return full telepony info for dealer offer per offer") {
      val objectId = s"dealer-$poiId"
      val offerId = "123-abcd"
      val tag = s"category=CARS#section=NEW#offer_id=$offerId"
      val regionId = RegionIds.RegionId(0)
      val CallTrackingEnabled = true
      val CallTrackingByOffer = true
      val ttl = 2.days.toSeconds

      val offer = Offer
        .apply(id = offerId, sellerType = SellerType.COMMERCIAL, category = Category.CARS, section = Section.NEW)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val settings =
        SettingsResponse.defaultInstance.withSettings(
          Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = CallTrackingByOffer)
        )

      val client = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(client))
          ++ SalesmanManagerMock
            .IsPaidCallInRegion(
              equalTo((clientId, client.regionId, offer.category, offer.section)),
              value(false)
            )
          ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      val teleponyInfo = TeleponyInfo(
        domain = "auto-dealers",
        objectId = objectId,
        tag = tag,
        ttl = ttl,
        phoneType = "Mobile"
      )

      TeleponyManager(_.getTeleponyInfo(offer, experiments = Nil))
        .provideCustomLayer(mocks)
        .map(teleponyR => assertTrue(teleponyR == teleponyInfo))
    },
    testM("return full telepony info for dealer offer per offer with experiments but offer is not in auction") {
      val objectId = s"dealer-$poiId"
      val offerId = "123-abcd"
      val tag = s"category=CARS#section=NEW#offer_id=$offerId"
      val regionId = RegionIds.RegionId(0)
      val CallTrackingEnabled = true
      val CallTrackingByOffer = true
      val ttl = 2.days.toSeconds

      val offer = Offer
        .apply(id = offerId, sellerType = SellerType.COMMERCIAL, category = Category.CARS, section = Section.NEW)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val settings =
        SettingsResponse.defaultInstance.withSettings(
          Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = CallTrackingByOffer)
        )

      val client = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(client))
          ++ SalesmanManagerMock
            .IsPaidCallInRegion(
              equalTo((clientId, client.regionId, offer.category, offer.section)),
              value(false)
            )
          ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      val teleponyInfo = TeleponyInfo(
        domain = "auto-dealers",
        objectId = objectId,
        tag = tag,
        ttl = ttl,
        phoneType = "Mobile"
      )

      TeleponyManager(_.getTeleponyInfo(offer, experiments = List("VS_1526_LBU_OFFLINE_CTR")))
        .provideCustomLayer(mocks)
        .map(teleponyR => assertTrue(teleponyR == teleponyInfo))
    },
    testM("return full telepony info for dealer offer per offer with experiments") {
      val objectId = s"dealer-$poiId"
      val offerId = "123-abcd"
      val tag = s"category=CARS#section=USED#offer_id=$offerId#experiments=VS_1526_LBU_OFFLINE_CTR"
      val regionId = RegSverdlovsk
      val CallTrackingEnabled = true
      val CallTrackingByOffer = true
      val ttl = 2.days.toSeconds
      val auctions = AuctionCurrentState.defaultInstance

      val offer = Offer
        .apply(id = offerId, sellerType = SellerType.COMMERCIAL, category = Category.CARS, section = Section.USED)
        .withAuction(auctions)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val settings =
        SettingsResponse.defaultInstance.withSettings(
          Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = CallTrackingByOffer)
        )

      val client = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(client))
          ++ SalesmanManagerMock
            .IsPaidCallInRegion(
              equalTo((clientId, client.regionId, offer.category, offer.section)),
              value(false)
            )
          ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      val teleponyInfo = TeleponyInfo(
        domain = "auto-dealers",
        objectId = objectId,
        tag = tag,
        ttl = ttl,
        phoneType = "Mobile"
      )

      TeleponyManager(_.getTeleponyInfo(offer, experiments = List("VS_1526_LBU_OFFLINE_CTR", "other_experiment")))
        .provideCustomLayer(mocks)
        .map(teleponyR => assertTrue(teleponyR == teleponyInfo))
    },
    testM("return error for private offers") {
      val offer = Offer
        .apply(sellerType = SellerType.PRIVATE, category = Category.MOTO, section = Section.NEW)
        .withSalon(Salon(salonId = poiId.value))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val mocks =
        CabinetManagerMock.empty >+>
          SalesmanManagerEmpty.empty >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          calltrackingService >+>
          TeleponyManagerLive.layer

      assertM(TeleponyManager(_.getTeleponyInfo(offer, experiments = Nil)).provideCustomLayer(mocks).run)(
        fails(isSubtype[TeleponyManager.NotDealerError](anything))
      )
    },
    testM("return error for bad salon id") {
      val badSalonId = 0

      val offer = Offer
        .apply(sellerType = SellerType.COMMERCIAL, category = Category.CARS, section = Section.NEW)
        .withSalon(Salon(salonId = badSalonId))
        .withCarInfo(CarInfo(mark = Some("BMW")))
        .withUserRef("dealer:87")

      val mocks =
        CabinetManagerMock.empty >+>
          SalesmanManagerEmpty.empty >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          calltrackingService >+>
          TeleponyManagerLive.layer

      assertM(TeleponyManager(_.getTeleponyInfo(offer, experiments = Nil)).provideCustomLayer(mocks).run)(
        fails(isSubtype[TeleponyManager.InvalidArgumentError](anything))
      )
    },
    testM("return qualifier with parsed platform") {
      val regionId = RegionIds.RegionId(0)
      val objectId = s"dealer-$poiId"
      val platform = Platform.Avito
      val tag = s"category=CARS#section=NEW#platform=$platform"
      val CallTrackingEnabled = true
      val offerData = TeleponyManager.TeleponyInfoBatchConfig(
        category = Category.CARS,
        section = Section.NEW,
        Some(platform),
        None
      )

      val settings = SettingsResponse.defaultInstance.withSettings(
        Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = false)
      )

      val clientInfo = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetClientIdByPoi(equalTo(poiId), value(clientId))
          ++ CabinetManagerMock.GetClient(equalTo(clientInfo.id), value(clientInfo))
          ++ SalesmanManagerMock
            .IsPaidCallInRegion(
              equalTo((clientId, clientInfo.regionId, offerData.category, offerData.section)),
              value(true)
            )
          ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      val teleponyInfo =
        TeleponyInfo(domain = "auto-dealers", objectId = objectId, tag = tag, ttl = 172800, phoneType = "Mobile")

      TeleponyManager(
        _.getTeleponyInfoBatchPoi(poiId, Seq((TeleponyInfoRequest.defaultInstance, offerData)), experiments = Nil)
      )
        .provideCustomLayer(mocks)
        .map(res => assertTrue(res == Seq((TeleponyInfoRequest.defaultInstance, teleponyInfo))))
    },
    testM("return qualifier with parsed platform and mobile") {
      val regionId = RegionIds.RegionId(0)
      val objectId = s"dealer-$poiId"
      val platform = Platform.Autoru
      val tag = s"category=CARS#section=NEW"
      val CallTrackingEnabled = true
      val offer = Offer.apply(category = Category.CARS, section = Section.NEW).withUserRef("dealer:87")
      val offerData = TeleponyManager.TeleponyInfoBatchConfig(
        category = Category.CARS,
        section = Section.NEW,
        Some(platform),
        Some(offer)
      )
      val settings = SettingsResponse.defaultInstance.withSettings(
        Settings(CallTrackingEnabled, calltrackingClassifiedsEnabled = false, offersStatEnabled = false)
      )
      val clientInfo = Client(clientId, regionId)

      val mocks =
        (CabinetManagerMock.GetPoiByClientId(equalTo(clientId), value(poiId)) ++ CabinetManagerMock
          .GetClient(equalTo(clientInfo.id), value(clientInfo))
          ++ SalesmanManagerMock
            .IsPaidCallInRegion(
              equalTo((clientId, clientInfo.regionId, offerData.category, offerData.section)),
              value(true)
            )
          ++ CalltrackingClientMock
            .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      val teleponyInfo = Seq(
        (
          TeleponyInfoRequest.defaultInstance,
          TeleponyInfo(domain = "autoru_billing", objectId = objectId, tag = tag, phoneType = "Mobile", ttl = 172800)
        )
      )
      TeleponyManager(
        _.getTeleponyInfoBatchClient(clientId, Seq((TeleponyInfoRequest.defaultInstance, offerData)), experiments = Nil)
      )
        .provideCustomLayer(mocks)
        .map(res => assertTrue(res == teleponyInfo))
    },
    testM("get cme telepony info for external platform") {
      val mocks =
        CabinetManagerMock.empty >+>
          SalesmanManagerEmpty.empty >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          calltrackingService >+>
          TeleponyManagerLive.layer

      val expected = TeleponyInfo(
        objectId = "cme-external-id",
        tag = "category=CARS#section=NEW#platform=AVITO#vin=vin",
        phoneType = "",
        ttl = 2.days.toSeconds
      )

      (for {
        teleponyInfo <- TeleponyManager(
          _.getTeleponyInfoCME(
            externalClientId = "external-id",
            clientId = None,
            vin = "vin",
            platform = Some(Platform.Avito),
            category = Category.CARS,
            section = Section.NEW,
            phoneNumber = "",
            phoneType = PhoneType.Default,
            customTags = Map.empty
          )
        )
      } yield assert(teleponyInfo)(equalTo(expected)))
        .provideCustomLayer(mocks)
    },
    testM("get cme telepony info for external platform without vin, category and section") {
      val mocks =
        CabinetManagerMock.empty >+>
          SalesmanManagerEmpty.empty >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          calltrackingService >+>
          TeleponyManagerLive.layer

      val expected = TeleponyInfo(
        objectId = "cme-external-id",
        tag = "platform=AVITO",
        phoneType = "",
        ttl = 2.days.toSeconds
      )

      (for {
        teleponyInfo <- TeleponyManager(
          _.getTeleponyInfoCME(
            externalClientId = "external-id",
            clientId = None,
            vin = "",
            platform = Some(Platform.Avito),
            category = Category.CATEGORY_UNKNOWN,
            section = Section.SECTION_UNKNOWN,
            phoneNumber = "",
            phoneType = PhoneType.Default,
            customTags = Map.empty
          )
        )
      } yield assert(teleponyInfo)(equalTo(expected)))
        .provideCustomLayer(mocks)
    },
    testM("get cme telepony info for external platform with additional tags and custom PhoneType") {
      val mocks =
        CabinetManagerMock.empty >+>
          SalesmanManagerEmpty.empty >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          calltrackingService >+>
          TeleponyManagerLive.layer

      val expected = TeleponyInfo(
        objectId = "cme-external-id",
        tag = "platform=AVITO#a=B#c=D",
        phoneType = "Mobile",
        ttl = 2.days.toSeconds
      )

      (for {
        teleponyInfo <- TeleponyManager(
          _.getTeleponyInfoCME(
            externalClientId = "external-id",
            clientId = None,
            vin = "",
            platform = Some(Platform.Avito),
            category = Category.CATEGORY_UNKNOWN,
            section = Section.SECTION_UNKNOWN,
            phoneNumber = "",
            phoneType = PhoneType.Mobile,
            customTags = Map("A" -> "B", "C" -> "D")
          )
        )
      } yield assert(teleponyInfo)(equalTo(expected)))
        .provideCustomLayer(mocks)
    },
    testM("get cme telepony info for autoru platform") {
      val clientId = ClientId(1)
      val poiId = PoiId(100)

      val mocks =
        CabinetManagerMock.GetPoiByClientId(equalTo(clientId), value(poiId)).toLayer >+>
          SalesmanManagerEmpty.empty >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          calltrackingService >+>
          TeleponyManagerLive.layer

      val expected = TeleponyInfo(
        objectId = s"dealer-100",
        tag = "category=CARS#section=NEW#vin=vin",
        phoneType = "",
        ttl = 2.days.toSeconds
      )

      (for {
        teleponyInfo <- TeleponyManager(
          _.getTeleponyInfoCME(
            externalClientId = "external-id",
            clientId = Some(clientId),
            vin = "vin",
            platform = Some(Platform.Autoru),
            category = Category.CARS,
            section = Section.NEW,
            phoneNumber = "",
            phoneType = PhoneType.Default,
            customTags = Map.empty
          )
        )
      } yield assert(teleponyInfo)(equalTo(expected)))
        .provideCustomLayer(mocks)
    },
    testM("return error if platform = autoru, but clientId = 0") {
      val mocks =
        CabinetManagerMock.empty >+>
          SalesmanManagerEmpty.empty >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          calltrackingService >+>
          TeleponyManagerLive.layer

      assertM {
        TeleponyManager(
          _.getTeleponyInfoCME(
            externalClientId = "external-id",
            clientId = None,
            vin = "vin",
            platform = Some(Platform.Autoru),
            category = Category.CARS,
            section = Section.NEW,
            phoneNumber = "",
            phoneType = PhoneType.Default,
            customTags = Map.empty
          )
        ).provideCustomLayer(mocks).run
      }(
        fails(isSubtype[TeleponyManager.InvalidArgumentError](anything))
      )
    },
    testM("get telepony info by params with paid call") {
      val regionId = RegionIds.RegionId(1)
      val clientInfo = Client(clientId, regionId)
      val category = Category.CARS
      val section = Section.NEW
      val settings = SettingsResponse.defaultInstance.withSettings(
        Settings(calltrackingEnabled = true, calltrackingClassifiedsEnabled = false, offersStatEnabled = true)
      )
      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(clientInfo)) ++
          SalesmanManagerMock.IsPaidCallInRegion(
            equalTo((clientId, regionId, category, section)),
            value(true)
          ) ++ CalltrackingClientMock.GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))
          ++ CabinetManagerMock.GetPoiByClientId(equalTo(clientId), value(poiId))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      val expected = Some(
        TeleponyInfo(
          domain = "autoru_billing",
          objectId = s"dealer-$poiId",
          tag = s"category=$category#section=$section",
          phoneType = "Mobile",
          geoId = regionId.id,
          ttl = 2.days.toSeconds
        )
      )

      (for {
        teleponyInfo <- TeleponyManager(
          _.getTeleponyInfoByParams(
            clientId = clientInfo.id,
            category = Category.CARS,
            section = Section.NEW,
            experiments = Nil
          )
        )
      } yield assert(teleponyInfo)(equalTo(expected)))
        .provideCustomLayer(mocks)
    },
    testM("get telepony info by params not paid call in region") {
      val regionId = RegionIds.RegionId(1)
      val clientInfo = Client(clientId, regionId)
      val category = Category.CARS
      val section = Section.NEW
      val settings = SettingsResponse.defaultInstance.withSettings(
        Settings(calltrackingEnabled = true, calltrackingClassifiedsEnabled = false, offersStatEnabled = true)
      )
      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(clientInfo)) ++
          SalesmanManagerMock.IsPaidCallInRegion(
            equalTo((clientId, regionId, category, section)),
            value(false)
          ) ++ CalltrackingClientMock.GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))
          ++ CabinetManagerMock.GetPoiByClientId(equalTo(clientId), value(poiId))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      val expected = Some(
        TeleponyInfo(
          domain = "auto-dealers",
          objectId = s"dealer-$poiId",
          tag = s"category=$category#section=$section",
          phoneType = "Mobile",
          ttl = 2.days.toSeconds
        )
      )

      (for {
        teleponyInfo <- TeleponyManager(
          _.getTeleponyInfoByParams(
            clientId = clientInfo.id,
            category = Category.CARS,
            section = Section.NEW,
            experiments = Nil
          )
        )
      } yield assert(teleponyInfo)(equalTo(expected)))
        .provideCustomLayer(mocks)
    },
    testM("get telepony info by params without paid calls with redirects not allowed") {
      val regionId = RegionIds.RegionId(1)
      val clientInfo = Client(clientId, regionId)
      val category = Category.CARS
      val section = Section.USED
      val settings = SettingsResponse.defaultInstance.withSettings(
        Settings(calltrackingEnabled = false, calltrackingClassifiedsEnabled = false, offersStatEnabled = true)
      )
      val mocks =
        (CabinetManagerMock.GetClient(equalTo(clientId), value(clientInfo))
          ++
            SalesmanManagerMock.IsPaidCallInRegion(
              equalTo((clientId, regionId, category, section)),
              value(false)
            ) ++ CalltrackingClientMock
              .GetSettings(equalTo(GetSettingsRequest(clientId.value)), value(settings))).toLayer >+>
          overrideConfigForRegions >+>
          Logging.live >+>
          TeleponyManagerLive.layer

      (for {
        teleponyInfo <- TeleponyManager(
          _.getTeleponyInfoByParams(
            clientId = clientInfo.id,
            category = Category.CARS,
            section = Section.USED,
            experiments = Nil
          )
        )
      } yield assert(teleponyInfo)(equalTo(None)))
        .provideCustomLayer(mocks)
    }
  )

  val calltrackingService: ULayer[Has[CalltrackingClient]] =
    CalltrackingClientMock.empty

  val overrideConfigForRegions: ULayer[Has[RedirectConfigOverrideForRegions]] = ZLayer.fromEffect {
    val config = Map(
      MoscowAndMoscowRegion -> createOverrideConfigs(
        List(ProtoTransportType.CARS_USED),
        callTrackingEnabled = None,
        callTrackingByOfferEnabled = None
      ),
      SaintPetersburgAndLeningradOblast -> createOverrideConfigs(
        List(ProtoTransportType.CARS_USED),
        callTrackingEnabled = None,
        callTrackingByOfferEnabled = None
      ),
      RegSverdlovsk -> createOverrideConfigs(
        List(ProtoTransportType.CARS_NEW),
        callTrackingEnabled = Some(true),
        callTrackingByOfferEnabled = Some(true)
      ),
      RegChelyabinsk -> createOverrideConfigs(
        List(ProtoTransportType.CARS_NEW),
        callTrackingEnabled = Some(true),
        callTrackingByOfferEnabled = Some(true)
      )
    )

    Ref
      .make(config)
      .map(new DefaultRedirectConfigOverrideForRegions(_))
  }

  private def createOverrideConfigs(
      transport: List[ProtoTransportType],
      callTrackingEnabled: Option[Boolean] = None,
      callTrackingByOfferEnabled: Option[Boolean] = None): Map[ProtoTransportType, SettingsOverrides] = {

    val settings: SettingsOverrides = SettingsOverrides(
      callTrackingEnabled = callTrackingEnabled,
      callTrackingByOfferEnabled = callTrackingByOfferEnabled
    )

    transport.map(_ -> settings).toMap
  }

  def howTeleponyInfoWasPreviouslyConfiguredNew(
      poiId: PoiId,
      regionId: RegionId,
      paidCalls: Boolean,
      settings: Settings,
      offer: Offer): Either[TeleponyManagerError, TeleponyInfo] = {

    val DealersTtl = 2.days

    val isNotDealerOffer = offer.sellerType != SellerType.COMMERCIAL
    val invalidSalonId = offer.getSalon.salonId == 0
    val isCars = offer.category == Category.CARS

    for {
      _ <- Either.cond(!isNotDealerOffer, (), NotDealerError(offer.id))
      _ <- Either.cond(!invalidSalonId, (), InvalidArgumentError(offer.id, "id"))
      redirectsAllowed = paidCalls || settings.calltrackingEnabled
      _ <- Either.cond(redirectsAllowed, (), RedirectNotAllowedError(offer.id))
    } yield {

      val tags = Seq(
        CategoryTag(offer.category),
        SectionTag(offer.section).filter(_ => isCars),
        OfferIdentity.unapply(offer.id).flatMap(OfferIdTag.apply).filter(_ => settings.offersStatEnabled || paidCalls)
      ).flatten

      val domain = if (paidCalls) {
        TeleponyDomain.AutoDealersAuction
      } else {
        TeleponyDomain.AutoDealers
      }

      val geoId =
        if (paidCalls) regionId.id
        else 0L

      val phoneType =
        if (settings.offersStatEnabled || paidCalls) PhoneType.Mobile
        else PhoneType.Default

      TeleponyInfo(
        domain.value,
        objectId = ObjectId.dealer(poiId),
        ttl = DealersTtl.toSeconds,
        tag = collectTags(tags),
        geoId = geoId,
        phoneType = phoneType.value
      )

    }
  }
}
