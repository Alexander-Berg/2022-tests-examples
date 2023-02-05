import MarketModels
import MarketProtocols
import OHHTTPStubs
import PromiseKit
import SwiftyJSON
import XCTest

@testable import BeruServices

final class LoadInitialOrderOptionsTests: NetworkingTestCase {

    // MARK: - Properties

    private lazy var defaultParams = CheckoutLoadOptionsParameters(
        shops: makeSeveralShops(),
        paymentOptions: [],
        isCheckoutSpreadAlgorithmV2: false
    )

    private var checkoutService: CheckoutServiceImpl!
    private var addressChooser: ServicesAddressChooser!
    private var stubRegionService: StubRegionService!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        let outletsService = OutletsServiceImpl(
            client: DependencyProvider().apiClient,
            storage: OutletsStorageStub()
        )
        stubRegionService = StubRegionService()
        addressChooser = AddressChooserStub()
        checkoutService = CheckoutServiceImpl(
            addressChooser: addressChooser,
            apiClient: DependencyProvider().apiClient,
            outletsService: outletsService,
            regionService: stubRegionService,
            favoritePickupManager: FavoritePickupManagerStub()
        )
    }

    override func tearDown() {
        checkoutService = nil
        addressChooser = nil
        stubRegionService = nil
        super.tearDown()
    }

    // MARK: - Address tests

    func test_shouldUsePreferredAddress_whenPreferredAddressProvided() {
        // given
        let preferredAddress = makeAddress()
        checkoutService.preferredAddress = preferredAddress

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let shop_1 = json["shops"].array?[0]
            let shop_2 = json["shops"].array?[1]

            let checkShop: (SwiftyJSON.JSON?) -> Bool = { shop in
                let deliveryPoint = shop?["deliveryPoint"]
                return deliveryPoint?["city"].string == preferredAddress.city &&
                    deliveryPoint?["regionId"].stringValue == preferredAddress.regionId?.stringValue &&
                    deliveryPoint?["street"].stringValue == preferredAddress.street
            }
            return checkShop(shop_1) && checkShop(shop_2)
        }

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = checkoutService.loadOrderOptionsWithFavoritesOutlets(defaultParams).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            XCTAssertNotNil(optionsResult.options)
        default:
            XCTFail("Should succeed")
        }
    }

    func test_shouldUseAddressChooserAddress_whenPreferredAddressNotProvided() {
        // given
        let preferredAddress = makeAddress()
        checkoutService.preferredAddress = preferredAddress

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let shop_1 = json["shops"].array?[0]
            let shop_2 = json["shops"].array?[1]

            let checkShop: (SwiftyJSON.JSON?) -> Bool = { shop in
                let deliveryPoint = shop?["deliveryPoint"]
                return deliveryPoint?["city"].string == preferredAddress.city &&
                    deliveryPoint?["regionId"].stringValue == preferredAddress.regionId?.stringValue &&
                    deliveryPoint?["street"].stringValue == preferredAddress.street
            }
            return checkShop(shop_1) && checkShop(shop_2)
        }

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = checkoutService.loadOrderOptionsWithOutlets(defaultParams).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            XCTAssertNotNil(optionsResult.options)
        default:
            XCTFail("Should succeed")
        }
    }

    func test_shouldNotSendDeliveryPoint_whenProvidedAddressWithoutHouse() {
        // given
        let preferredAddress = Address()
        preferredAddress.house = nil
        checkoutService.preferredAddress = preferredAddress

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            guard let shops = JSON(body)["shops"].array,
                  shops.count >= 2 else {
                XCTFail("Shops count is less than 2. Should be more.")
                return false
            }
            let shop_1 = shops[0]
            let shop_2 = shops[1]

            let checkShop: (SwiftyJSON.JSON?) -> Bool = { shop in
                let deliveryPoint = shop?["deliveryPoint"]
                return (deliveryPoint?.count ?? 0) == 0
            }
            return checkShop(shop_1) && checkShop(shop_2)
        }

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = checkoutService.loadOrderOptionsWithOutlets(defaultParams).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            XCTAssertNotNil(optionsResult.options)
        default:
            XCTFail("Should succeed")
        }
    }

    // MARK: - Region tests

    func test_shouldUsePreferredRegion_whenPreferredRegionIsSpecified() {
        // given
        let preferredRegion = YMTRegion(id: 999, name: "TestRegion", type: .city)
        checkoutService.preferredRegion = preferredRegion

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            return json["regionId"].number == preferredRegion.id
        }

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = checkoutService.loadOrderOptionsWithOutlets(defaultParams).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            XCTAssertNotNil(optionsResult.options)
        default:
            XCTFail("Should succeed")
        }
    }

    func test_shouldUseRegionFromRegionService_whenPreferredRegionIsNotSpecified() {
        // given
        let fakeRegion = YMTRegion(id: 998, name: "StubRegionServiceRegion", type: .city)
        stubRegionService.stubbedRegion = fakeRegion

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            return json["regionId"].number == fakeRegion.id
        }

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = checkoutService.loadOrderOptionsWithOutlets(defaultParams).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            XCTAssertNotNil(optionsResult.options)
        default:
            XCTFail("Should succeed")
        }
    }

    func test_shouldTriggerRegionAutoDetect_whenRegionNotSpecified() {
        // given

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options"
        )

        // when
        checkoutService.loadOrderOptionsWithOutlets(defaultParams).expect(in: self)

        // then
        XCTAssertTrue(stubRegionService.autodetectCalled)
    }

    func test_shouldFallbackToMoscow_whenFailedToDetectRegion() {
        // given
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            return json["regionId"].intValue == 213
        }

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = checkoutService.loadOrderOptionsWithOutlets(defaultParams).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            XCTAssertNotNil(optionsResult.options)
        default:
            XCTFail("Should succeed")
        }
    }

    // MARK: - Outlets tests

    func test_shouldTriggerOutletsFromFAPI_whenNoExperiments() {
        // given
        var outletsResolverTriggered = false

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: containsQueryParams(["outlets": "false"])
        )

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_outlets",
            testBlock: containsQueryParams(["name": "resolveOutlets"]) &&
                dummyTestBlock { outletsResolverTriggered = true }
        )

        // when
        checkoutService.loadOrderOptionsWithOutlets(defaultParams).expect(in: self)

        // then
        XCTAssertTrue(outletsResolverTriggered)
    }

    func test_shouldFallbackToCAPIOutlets_whenFAPIOutletsFailed() {
        // given
        var capiTriggered = false

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: containsQueryParams(["outlets": "false"])
        )

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: containsQueryParams(["outlets": "true"]) &&
                dummyTestBlock { capiTriggered = true }
        )

        stubError(requestPartName: "api/v1", code: 500)

        // when
        checkoutService.loadOrderOptionsWithOutlets(defaultParams).expect(in: self)

        // then
        XCTAssertTrue(capiTriggered)
    }

    func test_shouldPassProperOutletIds_whenLoadingOutletsFromFAPI() {
        // given

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: containsQueryParams(["outlets": "false"])
        )

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_outlets"
        )

        // when
        let result = checkoutService.loadOrderOptionsWithOutlets(defaultParams).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            let outlets = optionsResult.options?.shops.flatMap { shop in
                shop.deliveryOptions.flatMap { deliveryOption -> [YBMDeliveryOutlet] in
                    if let outletOption = deliveryOption as? YBMDeliveryOptionOutlet {
                        return outletOption.outlets
                    } else if let postOption = deliveryOption as? YBMDeliveryOptionPost {
                        return postOption.outlet.map { [$0] } ?? []
                    }
                    return []
                }
            } ?? []

            for outlet in outlets {
                XCTAssertNotNil(outlet.name)
                XCTAssertNotNil(outlet.address)
            }
            XCTAssertFalse(outlets.isEmpty)
        default:
            XCTFail("Should succeed")
        }
    }

    func test_shouldFillOutlets_whenOutletsLoadedFromFAPI() {
        // given
        var requestedOutletsIds: [String] = []

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: containsQueryParams(["outlets": "false"])
        )

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            requestedOutletsIds = json["params"].array?.first?["outletIds"].array?.compactMap { $0.stringValue } ?? []
            return true
        }

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_outlets",
            testBlock: containsQueryParams(["name": "resolveOutlets"]) && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = checkoutService.loadOrderOptionsWithOutlets(defaultParams).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            let outletIds = optionsResult.options?.shops.flatMap { shop in
                shop.deliveryOptions.flatMap { deliveryOption -> [String] in
                    if let outletOption = deliveryOption as? YBMDeliveryOptionOutlet {
                        return outletOption.outlets.map { $0.id.stringValue }
                    } else if let postOption = deliveryOption as? YBMDeliveryOptionPost {
                        return postOption.outlet.map { [$0.id.stringValue] } ?? []
                    }
                    return []
                }
            }

            XCTAssertEqual(outletIds.map { Set($0) }, Set(requestedOutletsIds))
        default:
            XCTFail("Should succeed")
        }
    }

    // MARK: - Base logic tests

    func test_shouldSendShowCredits_whenShowCreditsParamterProvided() {
        // given
        let showCredits = true
        let severalShops = makeSeveralShops()
        let parameters = CheckoutLoadOptionsParameters(
            shops: severalShops,
            paymentOptions: [],
            showCredits: showCredits,
            isCheckoutSpreadAlgorithmV2: false
        )

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: containsQueryParams(["show_credits": "true"])
        )

        // when
        let result = checkoutService.loadOrderOptionsWithOutlets(parameters).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            XCTAssertNotNil(optionsResult.options)
        default:
            XCTFail("Should succeed")
        }
    }

    func test_shouldSendShowInstallments_whenShowInstallmentsParamterProvided() {
        // given
        let showInstallments = true
        let severalShops = makeSeveralShops()
        let parameters = CheckoutLoadOptionsParameters(
            shops: severalShops,
            paymentOptions: [],
            showInstallments: showInstallments,
            isCheckoutSpreadAlgorithmV2: false
        )

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: containsQueryParams(["show_installments": "true"])
        )

        // when
        let result = checkoutService.loadOrderOptionsWithOutlets(parameters).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            XCTAssertNotNil(optionsResult.options)
        default:
            XCTFail("Should succeed")
        }
    }

    func test_shouldSendShowCreditBroker_whenShowCreditBrokerParameterProvided() {
        // given
        let showCreditBroker = true
        let severalShops = makeSeveralShops()
        let parameters = CheckoutLoadOptionsParameters(
            shops: severalShops,
            paymentOptions: [],
            showCreditBroker: showCreditBroker,
            isCheckoutSpreadAlgorithmV2: false
        )

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: containsQueryParams(["show_credit_broker": "true"])
        )

        // when
        let result = checkoutService.loadOrderOptionsWithOutlets(parameters).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            XCTAssertNotNil(optionsResult.options)
        default:
            XCTFail("Should succeed")
        }
    }

    func test_shouldSendShowStationSubscription_whenShowStationSubscriptionParamterProvided() {
        // given
        let showStationSubscription = true
        let severalShops = makeSeveralShops()
        let parameters = CheckoutLoadOptionsParameters(
            shops: severalShops,
            paymentOptions: [],
            showStationSubscription: showStationSubscription,
            isCheckoutSpreadAlgorithmV2: false
        )

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: containsQueryParams(["show_station_subscription": "true"])
        )

        // when
        let result = checkoutService.loadOrderOptionsWithOutlets(parameters).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            XCTAssertNotNil(optionsResult.options)
        default:
            XCTFail("Should succeed")
        }
    }

    func test_shouldSendProperRequestParameters() {
        // given
        let severalShops = makeSeveralShops()
        let coins = ["41", "42", "43"]
        let promocode = "universe"
        let parameters = CheckoutLoadOptionsParameters(
            shops: severalShops,
            coinsIds: coins,
            paymentOptions: [],
            promocode: promocode,
            isCheckoutSpreadAlgorithmV2: false
        )
        let regionId = 267
        checkoutService.preferredRegion = YMTRegion(id: NSNumber(value: regionId), name: "TestRegion", type: .city)

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let item_1 = json["shops"].array?[0]["items"].array?[0]
            let item_2 = json["shops"].array?[0]["items"].array?[1]
            let item_3 = json["shops"].array?[1]["items"].array?[0]

            let originalItem_1 = JSON(severalShops.first?.items.first?.jsonForRequest() as Any)
            let originalItem_2 = JSON(severalShops.first?.items.last?.jsonForRequest() as Any)
            let originalItem_3 = JSON(severalShops.last?.items.last?.jsonForRequest() as Any)

            return originalItem_1 == item_1 &&
                originalItem_2 == item_2 &&
                originalItem_3 == item_3 &&
                json["promoCode"].string == promocode &&
                Set(json["coinIds"].arrayValue.compactMap { $0.stringValue }) == Set(coins) &&
                json["regionId"].intValue == regionId
        }

        stub(
            requestPartName: "order/options",
            responseFileName: "simple_order_options",
            testBlock: isMethodPOST() && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = checkoutService.loadOrderOptionsWithFavoritesOutlets(parameters).expect(in: self)

        // then
        switch result {
        case let .success(optionsResult):
            XCTAssertNotNil(optionsResult.options)
        default:
            XCTFail("Should succeed")
        }
    }
}

private extension LoadInitialOrderOptionsTests {

    // MARK: - Private

    func makeOfferFromJsonWithName(_ name: String) -> YMTOffer? {
        guard let jsonDictionary: [AnyHashable: Any] = loadJson(with: name) else { return nil }

        let jsonRepresentation = YBMJSONRepresentation(targetObject: jsonDictionary)

        return YMTOffer.model(withJSON: jsonRepresentation)
    }

    func makeDefaultShop() -> YBMOrderOptionsRequestShop {
        let offer = makeOfferFromJsonWithName("capi_offer_1") ?? YMTOffer()

        let shopItem = YBMOrderOptionsRequestShopItem(
            count: 5,
            price: YMTOfferPrice(price: 100, currency: .RUB),
            offer: offer,
            specs: nil,
            bundleId: "bundleId",
            label: "itemLabel",
            feeShow: "feeShow",
            cartFee: "cartFee",
            isPriceDropPromoEnabled: false,
            skuID: "12345",
            title: "some",
            categoryId: nil,
            services: []
        )

        return YBMOrderOptionsRequestShop(
            shopId: 431_782,
            items: [shopItem],
            deliveryPoint: nil,
            label: "shopLabel",
            preorder: true,
            wasSplitByCombinator: false,
            isPartialDeliveryAvailable: false
        )
    }

    func makeSeveralShops() -> [YBMOrderOptionsRequestShop] {
        guard
            let offer_1 = makeOfferFromJsonWithName("capi_offer_1"),
            let offer_2 = makeOfferFromJsonWithName("capi_offer_2"),
            let offer_3 = makeOfferFromJsonWithName("capi_offer_3")
        else {
            XCTFail("Unable to create YMTOffer model")
            return []
        }

        let firstShopItems = [offer_1, offer_2].map {
            YBMOrderOptionsRequestShopItem(
                count: 5,
                price: YMTOfferPrice(price: 100, currency: .RUB),
                offer: $0,
                specs: nil,
                bundleId: "bundleId",
                label: "itemLabel",
                feeShow: "feeShow",
                cartFee: "cartFee",
                isPriceDropPromoEnabled: false,
                skuID: "12345",
                title: "some",
                categoryId: nil,
                services: []
            )
        }

        let secondShopItems = [offer_3].map {
            YBMOrderOptionsRequestShopItem(
                count: 5,
                price: YMTOfferPrice(price: 100, currency: .RUB),
                offer: $0,
                specs: nil,
                bundleId: "bundleId",
                label: "itemLabel",
                feeShow: "feeShow",
                cartFee: "cartFee",
                isPriceDropPromoEnabled: false,
                skuID: "12345",
                title: "some",
                categoryId: nil,
                services: []
            )
        }

        let firstShop = YBMOrderOptionsRequestShop(
            shopId: 431_782,
            items: firstShopItems,
            deliveryPoint: nil,
            label: "shopLabel_1",
            preorder: true,
            wasSplitByCombinator: false,
            isPartialDeliveryAvailable: false
        )

        let secondShop = YBMOrderOptionsRequestShop(
            shopId: 431_783,
            items: secondShopItems,
            deliveryPoint: nil,
            label: "shopLabel_2",
            preorder: true,
            wasSplitByCombinator: false,
            isPartialDeliveryAvailable: false
        )

        return [firstShop, secondShop]
    }

    func makeAddress() -> Address {
        let address = Address()
        address.regionId = NSNumber(value: 213)
        address.city = "Москва"
        address.street = "Льва Толстого"
        address.house = "16"
        return address
    }

}
