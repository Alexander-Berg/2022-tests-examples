import MarketDTO
import MarketModels
import XCTest

@testable import BeruServices

class ParcelMapperTests: XCTestCase {

    // MARK: - Properties

    var mocksFactory: CheckoutMapperMocksFactory!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        mocksFactory = CheckoutMapperMocksFactory()
    }

    override func tearDown() {
        mocksFactory = nil
        super.tearDown()
    }

    // MARK: - Tests

    func test_parcelMapping_whenSimpleParcelResult() throws {
        // given
        let label = "172"
        let shopId = 12_345
        let shopName = "Ya.Market"
        let vghInfo = "some_vgh"

        let shop = mocksFactory.makeParcelResultShop(
            id: shopId,
            name: shopName
        )
        let items = [mocksFactory.makeParcelItemResult(), mocksFactory.makeParcelItemResult()]
        let outletDeliveryOptionInput = mocksFactory.makeDeliveryOptionResult(type: .outlet)
        let serviceDeliveryOptionInput1 = mocksFactory.makeDeliveryOptionResult(type: .service)
        let serviceDeliveryOptionInput2 = mocksFactory.makeDeliveryOptionResult(type: .service)
        let input = mocksFactory.makeParcelResult(
            shop: shop,
            label: label,
            parcelInfo: vghInfo,
            items: items,
            paymentMethods: [.cardOnDelivery, .applePay],
            deliveryOptions: [outletDeliveryOptionInput, serviceDeliveryOptionInput1, serviceDeliveryOptionInput2]
        )

        // when
        let result = try ParcelMapper.extractParcel(from: input, itemsOrderByParcels: [:])

        // then
        let outletType = mocksFactory.makeOutletDeliveryOptionType()
        let outletDeliveryOption = mocksFactory.makeDeliveryOption(type: outletType)

        let serviceType = mocksFactory.makeServiceDeliveryOptionType()
        let serviceDeliveryOption1 = mocksFactory.makeDeliveryOption(type: serviceType)
        let serviceDeliveryOption2 = mocksFactory.makeDeliveryOption(type: serviceType)

        let deliveryOptions: DeliveryOptionsByType = [
            .service: [serviceDeliveryOption1, serviceDeliveryOption2].compactMap { $0 },
            .outlet: [outletDeliveryOption].compactMap { $0 }
        ]

        let parcelItems = [mocksFactory.makeParcelItem(), mocksFactory.makeParcelItem()]
        let parcelInfo = mocksFactory.makeParcelInfo(
            label: label,
            shopId: shopId,
            shopName: shopName,
            items: parcelItems
        )
        let expectedResult = mocksFactory.makeParcel(
            info: parcelInfo,
            deliveryOptions: deliveryOptions,
            paymentMethods: [.cardOnDelivery, .applePay],
            vghInfo: vghInfo
        )
        XCTAssertEqual(result, expectedResult)
    }

    func test_parcelMapping_whenItemOrderResultIsMixed() throws {
        // given
        let label = "172"
        let shopId = 12_345
        let shopName = "Ya.Market"
        let itemLabel1 = "item1"
        let itemLabel2 = "item2"
        let itemLabel3 = "item3"
        let itemLabel4 = "item4"

        let shop = mocksFactory.makeParcelResultShop(
            id: shopId,
            name: shopName
        )
        let items = [
            mocksFactory.makeParcelItemResult(label: itemLabel2),
            mocksFactory.makeParcelItemResult(label: itemLabel3),
            mocksFactory.makeParcelItemResult(label: itemLabel1)
        ]
        let input = mocksFactory.makeParcelResult(
            shop: shop,
            label: label,
            items: items
        )

        // when
        let result = try ParcelMapper.extractParcel(
            from: input,
            itemsOrderByParcels: [label: [itemLabel1, itemLabel2, itemLabel4]]
        )

        // then
        let parcelItems = [
            mocksFactory.makeParcelItem(label: itemLabel1),
            mocksFactory.makeParcelItem(label: itemLabel2),
            mocksFactory.makeParcelItem(label: itemLabel3)
        ]
        let parcelInfo = mocksFactory.makeParcelInfo(
            label: label,
            shopId: shopId,
            shopName: shopName,
            items: parcelItems
        )
        let expectedResult = mocksFactory.makeParcel(info: parcelInfo)
        XCTAssertEqual(result, expectedResult)
    }

    func test_parcelMapping_whenShopIsMissing() throws {
        // given
        let label = "172"

        let items = [mocksFactory.makeParcelItemResult(), mocksFactory.makeParcelItemResult()]
        let outletDeliveryOptionInput = mocksFactory.makeDeliveryOptionResult(type: .outlet)
        let serviceDeliveryOptionInput = mocksFactory.makeDeliveryOptionResult(type: .service)
        let input = mocksFactory.makeParcelResult(
            shop: nil,
            label: label,
            items: items,
            paymentMethods: [.cardOnDelivery, .applePay],
            deliveryOptions: [outletDeliveryOptionInput, serviceDeliveryOptionInput]
        )

        // when
        XCTAssertThrowsError(try ParcelMapper.extractParcel(from: input, itemsOrderByParcels: [:])) { error in
            guard case ParcelMappingError.shopIsMissing = error else {
                XCTFail("Invalid error thrown")
                return
            }
        }
    }

    func test_parcelMapping_whenShopIdIsMissing() throws {
        // given
        let label = "172"
        let shopName = "Ya.Market"

        let shop = mocksFactory.makeParcelResultShop(
            id: nil,
            name: shopName
        )
        let items = [mocksFactory.makeParcelItemResult(), mocksFactory.makeParcelItemResult()]
        let outletDeliveryOptionInput = mocksFactory.makeDeliveryOptionResult(type: .outlet)
        let serviceDeliveryOptionInput = mocksFactory.makeDeliveryOptionResult(type: .service)
        let input = mocksFactory.makeParcelResult(
            shop: shop,
            label: label,
            items: items,
            paymentMethods: [.cardOnDelivery, .applePay],
            deliveryOptions: [outletDeliveryOptionInput, serviceDeliveryOptionInput]
        )

        // when
        XCTAssertThrowsError(try ParcelMapper.extractParcel(from: input, itemsOrderByParcels: [:])) { error in
            guard case ParcelMappingError.shopIsMissing = error else {
                XCTFail("Invalid error thrown")
                return
            }
        }
    }
}
