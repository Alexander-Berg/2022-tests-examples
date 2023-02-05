import MarketModels
import XCTest

@testable import BeruServices

class ActualizationMapperTests: XCTestCase {

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

    func test_actualizationMapping_whenSimpleOrderOptionsResult() throws {
        // given
        let summaryInput = mocksFactory.makeSummaryResult()
        let input = mocksFactory.makeOrderOptionsResult(summary: summaryInput)

        // when
        let result = try ActualizationMapper.extractActualization(from: input, itemsOrderByParcels: [:])

        // then
        let summary = mocksFactory.makeSummary()
        let commonPayment = mocksFactory.makeCommonPayment()
        let cashback = mocksFactory.makeCashback()
        let expectedResult = mocksFactory.makeActualization(
            summary: summary,
            commonPayment: commonPayment,
            cashback: cashback
        )
        XCTAssertEqual(result, expectedResult)
    }

    func test_actualizationMapping_whenParcelsNotEmpty() throws {
        // given
        let parcelsShop = mocksFactory.makeParcelResultShop()
        let parcelsInput = [
            mocksFactory.makeParcelResult(shop: parcelsShop),
            mocksFactory.makeParcelResult(shop: parcelsShop)
        ]
        let summaryInput = mocksFactory.makeSummaryResult()
        let input = mocksFactory.makeOrderOptionsResult(parcels: parcelsInput, summary: summaryInput)

        // when
        let result = try ActualizationMapper.extractActualization(from: input, itemsOrderByParcels: [:])

        // then
        let parcelInfo = mocksFactory.makeParcelInfo()
        let parcels = [
            mocksFactory.makeParcel(info: parcelInfo),
            mocksFactory.makeParcel(info: parcelInfo)
        ]
        let summary = mocksFactory.makeSummary()
        let commonPayment = mocksFactory.makeCommonPayment()
        let cashback = mocksFactory.makeCashback()
        let expectedResult = mocksFactory.makeActualization(
            parcels: parcels,
            summary: summary,
            commonPayment: commonPayment,
            cashback: cashback
        )
        XCTAssertEqual(result, expectedResult)
    }

    func test_actualizationMapping_whenSummaryFull() throws {
        // given
        let baseAmount = 9_990
        let dicountAmount = 9_991
        let promoDiscount = 9_992
        let promoCodeDiscount = 9_993
        let coinDiscount = 9_994
        let totalAmount = 9_995

        let summaryInput = mocksFactory.makeSummaryResult(
            baseAmount: baseAmount,
            discountAmount: dicountAmount,
            promoDiscount: promoDiscount,
            promoCodeDiscount: promoCodeDiscount,
            coinDiscount: coinDiscount,
            totalAmount: totalAmount
        )
        let input = mocksFactory.makeOrderOptionsResult(summary: summaryInput)

        // when
        let result = try ActualizationMapper.extractActualization(from: input, itemsOrderByParcels: [:])

        // then
        let summary = mocksFactory.makeSummary(
            baseAmount: baseAmount,
            discountAmount: dicountAmount,
            promoDiscount: promoDiscount,
            promoCodeDiscount: promoCodeDiscount,
            coinDiscount: coinDiscount,
            totalAmount: totalAmount
        )
        let commonPayment = mocksFactory.makeCommonPayment()
        let cashback = mocksFactory.makeCashback()
        let expectedResult = mocksFactory.makeActualization(
            summary: summary,
            commonPayment: commonPayment,
            cashback: cashback
        )
        XCTAssertEqual(result, expectedResult)
    }

    func test_actualizationMapping_whenSummaryIsMissing() throws {
        // given
        let input = mocksFactory.makeOrderOptionsResult(summary: nil)

        // when
        XCTAssertThrowsError(
            try ActualizationMapper.extractActualization(from: input, itemsOrderByParcels: [:])
        ) { error in
            guard case ActualizationMappingError.summaryIsMissing = error else {
                XCTFail("Invalid error thrown")
                return
            }
        }
    }

    func test_actualizationMapping_whenPresetsIsNotEmpty() throws {
        // given
        let label1 = "parcel1_label"
        let label2 = "parcel2_label"

        let availableParcelInput = mocksFactory.makePresetResultParcel(label: label1, deliveryAvailable: true)
        let unvailableParcelInput = mocksFactory.makePresetResultParcel(label: label2, deliveryAvailable: false)

        let deliveryPresetInput = mocksFactory.makePresetResult(
            type: .delivery,
            parcels: [availableParcelInput, unvailableParcelInput]
        )
        let pickupPresetInput = mocksFactory.makePresetResult(type: .pickup, parcels: [availableParcelInput])
        let postPresetInput = mocksFactory.makePresetResult(type: .post, parcels: [])

        let input = mocksFactory.makeOrderOptionsResult(
            presets: [deliveryPresetInput, pickupPresetInput, postPresetInput],
            summary: mocksFactory.makeSummaryResult()
        )
        let parcel1Properties = PresetDeliverability.ParcelProperties(
            availability: .available,
            isTryingAvailable: false
        )
        let parcel2Properties = PresetDeliverability.ParcelProperties(
            availability: .unavailable,
            isTryingAvailable: false
        )

        // when
        let result = try ActualizationMapper.extractActualization(from: input, itemsOrderByParcels: [:])

        // then
        let deliveryPresetDeliverability = mocksFactory.makePreset(
            kind: .service,
            parcels: [label1: parcel1Properties, label2: parcel2Properties]
        )
        let pickupPresetDeliverability = mocksFactory.makePreset(
            kind: .outlet,
            parcels: [label1: parcel1Properties]
        )
        let postPresetDeliverability = mocksFactory.makePreset(
            kind: .post,
            parcels: [:]
        )

        let expectedResult = mocksFactory.makeActualization(
            presetDeliverabilities: [
                deliveryPresetDeliverability,
                pickupPresetDeliverability,
                postPresetDeliverability
            ],
            summary: mocksFactory.makeSummary(),
            commonPayment: mocksFactory.makeCommonPayment(),
            cashback: mocksFactory.makeCashback()
        )
        XCTAssertEqual(result, expectedResult)
    }

    func test_actualizationMapping_whenSeparateDelivery() throws {
        // given
        let input = mocksFactory.makeOrderOptionsResult(
            summary: mocksFactory.makeSummaryResult(),
            hasCommonDelivery: false
        )

        // when
        let result = try ActualizationMapper.extractActualization(from: input, itemsOrderByParcels: [:])

        // then
        let expectedResult = mocksFactory.makeActualization(
            summary: mocksFactory.makeSummary(),
            commonPayment: mocksFactory.makeCommonPayment(),
            cashback: mocksFactory.makeCashback(),
            hasCommonDelivery: false
        )
        XCTAssertEqual(result, expectedResult)
    }

    func test_actualizationMapping_whenConsolidationProvided() throws {
        // given
        let input = mocksFactory.makeOrderOptionsResult(
            summary: mocksFactory.makeSummaryResult(),
            consolidation: mocksFactory.makeConsolidationResult(),
            hasCommonDelivery: false
        )

        // when
        let result = try ActualizationMapper.extractActualization(from: input, itemsOrderByParcels: [:])

        // then
        let expectedResult = mocksFactory.makeActualization(
            summary: mocksFactory.makeSummary(),
            commonPayment: mocksFactory.makeCommonPayment(),
            cashback: mocksFactory.makeCashback(),
            consolidation: mocksFactory.makeConsolidation(),
            hasCommonDelivery: false
        )
        XCTAssertEqual(result, expectedResult)
    }
}
