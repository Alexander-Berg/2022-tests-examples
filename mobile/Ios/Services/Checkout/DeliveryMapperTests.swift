import MarketDTO
import MarketModels
import XCTest

@testable import BeruServices

class DeliveryMapperTests: XCTestCase {

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

    // MARK: - Dates Tests

    func test_deliveryOptionMappingThrows_whenDateFlowViolated() {
        // given
        let input = mocksFactory.makeDeliveryOptionResult(
            type: .outlet,
            beginDate: "2020-01-03",
            endDate: "2020-01-01"
        )

        // when
        XCTAssertThrowsError(try DeliveryMapper.extractDeliveryOption(from: input)) { error in
            guard case DeliveryOptionMappingError.dateFlowViolated = error else {
                XCTFail("Invalid error thrown")
                return
            }
        }
    }

    // MARK: - Outlet Tests

    func test_deliveryOptionMapping_whenSimpleOutletResult() throws {
        // given
        let isMarketBranded = true
        let outletIds = [9_999, 9_998]

        let outlets = outletIds.map(mocksFactory.makeDeliveryOptionResultOutlet)
        let input = mocksFactory.makeDeliveryOptionResult(
            type: .outlet,
            isMarketBranded: isMarketBranded,
            outlets: outlets
        )

        // when
        let result = try DeliveryMapper.extractDeliveryOption(from: input)

        // then
        let type = mocksFactory.makeOutletDeliveryOptionType(
            isMarketBranded: isMarketBranded,
            outletIds: outletIds
        )
        let expectedResult = mocksFactory.makeDeliveryOption(type: type)
        XCTAssertEqual(result, expectedResult)
    }

    func test_deliveryOptionMapping_whenEmptyOutletsOutletResult() throws {
        // given
        let isMarketBranded = false
        let outletIds: [Int] = []

        let outlets = outletIds.map(mocksFactory.makeDeliveryOptionResultOutlet)
        let input = mocksFactory.makeDeliveryOptionResult(
            type: .outlet,
            isMarketBranded: isMarketBranded,
            outlets: outlets
        )

        // when
        let result = try DeliveryMapper.extractDeliveryOption(from: input)

        // then
        let type = mocksFactory.makeOutletDeliveryOptionType(
            isMarketBranded: isMarketBranded,
            outletIds: outletIds
        )
        let expectedResult = mocksFactory.makeDeliveryOption(type: type)
        XCTAssertEqual(result, expectedResult)
    }

    // MARK: - Post Tests

    func test_deliveryOptionMapping_whenSimplePostResult() throws {
        // given
        let outletId = 9_999
        let outlet = mocksFactory.makeDeliveryOptionResultOutlet(id: outletId)
        let input = mocksFactory.makeDeliveryOptionResult(
            type: .post,
            outlet: outlet
        )

        // when
        let result = try DeliveryMapper.extractDeliveryOption(from: input)

        // then
        let type = mocksFactory.makePostDeliveryOptionType(outletId: outletId)
        let expectedResult = mocksFactory.makeDeliveryOption(type: type)
        XCTAssertEqual(result, expectedResult)
    }

    func test_deliveryOptionMapping_whenEmptyOutletPostResult() throws {
        // given
        let outletId: Int? = nil
        let input = mocksFactory.makeDeliveryOptionResult(
            type: .post,
            outlet: nil
        )

        // when
        let result = try DeliveryMapper.extractDeliveryOption(from: input)

        // then
        let type = mocksFactory.makePostDeliveryOptionType(outletId: outletId)
        let expectedResult = mocksFactory.makeDeliveryOption(type: type)
        XCTAssertEqual(result, expectedResult)
    }

    // MARK: - Service Tests

    func test_deliveryOptionMapping_whenSimpleServiceResult() throws {
        // given
        let fromTime1 = "12:00:00"
        let toTime1 = "12:15:00"
        let isDefault1 = true
        let timeInterval1 = mocksFactory.makeDeliveryOptionResultTimeInterval(
            fromTime: fromTime1,
            toTime: toTime1,
            isDefault: isDefault1
        )

        let fromTime2 = "12:15:00"
        let toTime2 = "12:30:00"
        let isDefault2 = false
        let timeInterval2 = mocksFactory.makeDeliveryOptionResultTimeInterval(
            fromTime: fromTime2,
            toTime: toTime2,
            isDefault: isDefault2
        )

        let input = mocksFactory.makeDeliveryOptionResult(
            type: .service,
            features: [],
            intervals: [timeInterval1, timeInterval2],
            liftPrice: 0,
            liftingOptions: nil,
            isLeaveAtTheDoor: true
        )

        // when
        let result = try DeliveryMapper.extractDeliveryOption(from: input)

        // then
        let deliveryTime1 = mocksFactory.makeDeliveryTime(
            fromTime: fromTime1,
            toTime: toTime1,
            isDefault: isDefault1
        )
        let deliveryTime2 = mocksFactory.makeDeliveryTime(
            fromTime: fromTime2,
            toTime: toTime2,
            isDefault: isDefault2
        )
        let type = mocksFactory.makeServiceDeliveryOptionType(
            intervals: [deliveryTime1, deliveryTime2].compactMap { $0 },
            isOnDemand: false,
            liftPrice: 0,
            liftingOptions: nil,
            isLeaveAtTheDoor: true
        )
        let expectedResult = mocksFactory.makeDeliveryOption(type: type)
        XCTAssertEqual(result, expectedResult)
    }

    // MARK: - Service On Demand Tests

    func test_deliveryOptionMapping_whenOnDemandServiceResult() throws {
        // given
        let inputOnDemand = mocksFactory.makeDeliveryOptionResult(
            type: .service,
            features: [.onDemand],
            intervals: [],
            liftPrice: 0,
            liftingOptions: nil,
            isLeaveAtTheDoor: false
        )
        let inputOnDemandCombinator = mocksFactory.makeDeliveryOptionResult(
            type: .service,
            features: [.onDemand],
            intervals: [],
            liftPrice: 0,
            liftingOptions: nil,
            isLeaveAtTheDoor: false
        )
        let inputOnDemandAll = mocksFactory.makeDeliveryOptionResult(
            type: .service,
            features: [.onDemand, .onDemandCombinator],
            intervals: [],
            liftPrice: 0,
            liftingOptions: nil,
            isLeaveAtTheDoor: false
        )

        // when
        let result1 = try DeliveryMapper.extractDeliveryOption(from: inputOnDemand)
        let result2 = try DeliveryMapper.extractDeliveryOption(from: inputOnDemandCombinator)
        let result3 = try DeliveryMapper.extractDeliveryOption(from: inputOnDemandAll)

        // then
        let type = mocksFactory.makeServiceDeliveryOptionType(
            intervals: [],
            isOnDemand: true,
            liftPrice: 0,
            liftingOptions: nil,
            isLeaveAtTheDoor: false
        )
        let expectedResult = mocksFactory.makeDeliveryOption(type: type)
        XCTAssertEqual(result1, expectedResult)
        XCTAssertEqual(result2, expectedResult)
        XCTAssertEqual(result3, expectedResult)
    }

    func test_deliveryOptionMapping_whenNoOnDemandServiceResult() throws {
        // given
        let input = mocksFactory.makeDeliveryOptionResult(
            type: .service,
            features: [.expressDelivery, .onDemandMarketPickup, .onDemandYalavka],
            intervals: [],
            liftPrice: 0,
            liftingOptions: nil,
            isLeaveAtTheDoor: false
        )

        // when
        let result = try DeliveryMapper.extractDeliveryOption(from: input)

        // then
        let type = mocksFactory.makeServiceDeliveryOptionType(
            intervals: [],
            isOnDemand: false,
            liftPrice: 0,
            liftingOptions: nil,
            isLeaveAtTheDoor: false
        )
        let expectedResult = mocksFactory.makeDeliveryOption(type: type)
        XCTAssertEqual(result, expectedResult)
    }

    // MARK: - Service Lifting Options Tests

    func test_deliveryOptionMapping_whenLiftingOptionsServiceResult(
        inputType: DeliveryOptionResult.LiftingOptions.LiftingOptionsType,
        outputType: LiftingOptions.AvailabilityType
    ) throws {
        // given
        let liftPrice = 1_000
        let manualLiftPerFloorCost = 150
        let elevatorLiftCost = 550
        let cargoElevatorLiftCost = 450
        let unloadCost = 350

        let liftingOptions = mocksFactory.makeDeliveryOptionResultLiftingOptions(
            manualLiftPerFloorCost: manualLiftPerFloorCost,
            elevatorLiftCost: elevatorLiftCost,
            cargoElevatorLiftCost: cargoElevatorLiftCost,
            unloadCost: unloadCost,
            type: inputType
        )
        let input = mocksFactory.makeDeliveryOptionResult(
            type: .service,
            features: [],
            intervals: [],
            liftPrice: liftPrice,
            liftingOptions: liftingOptions,
            isLeaveAtTheDoor: false
        )

        // when
        let result = try DeliveryMapper.extractDeliveryOption(from: input)

        // then
        let liftingOptionsOutput = mocksFactory.makeLiftingOptions(
            type: outputType,
            manualLiftPerFloorCost: manualLiftPerFloorCost,
            elevatorLiftCost: elevatorLiftCost,
            cargoElevatorLiftCost: cargoElevatorLiftCost,
            unloadCost: unloadCost
        )
        let type = mocksFactory.makeServiceDeliveryOptionType(
            intervals: [],
            isOnDemand: false,
            liftPrice: liftPrice,
            liftingOptions: liftingOptionsOutput,
            isLeaveAtTheDoor: false
        )
        let expectedResult = mocksFactory.makeDeliveryOption(type: type)
        XCTAssertEqual(result, expectedResult)
    }

    func test_deliveryOptionMapping_whenLiftingOptionsAvailableServiceResult() throws {
        try test_deliveryOptionMapping_whenLiftingOptionsServiceResult(
            inputType: .available,
            outputType: .available
        )
    }

    func test_deliveryOptionMapping_whenLiftingOptionsIncludedServiceResult() throws {
        try test_deliveryOptionMapping_whenLiftingOptionsServiceResult(
            inputType: .included,
            outputType: .included
        )
    }

    func test_deliveryOptionMapping_whenLiftingOptionsnotAvailableServiceResult() throws {
        try test_deliveryOptionMapping_whenLiftingOptionsServiceResult(
            inputType: .notAvailable,
            outputType: .notAvailable
        )
    }

    // MARK: - Digital Tests

    func test_deliveryOptionMapping_whenSimpleDigitalResult() throws {
        // given
        let input = mocksFactory.makeDeliveryOptionResult(type: .digital)

        // when
        let result = try DeliveryMapper.extractDeliveryOption(from: input)

        // then
        let expectedResult = mocksFactory.makeDeliveryOption(type: .digital)
        XCTAssertEqual(result, expectedResult)
    }
}
