import MarketProtocols
import OHHTTPStubs
import PromiseKit
import XCTest

@testable import BeruServices

final class LoadOutletsTests: NetworkingTestCase {

    var outletsService: OutletsServiceImpl!
    var outletsStorage: OutletsStorage!

    override func setUp() {
        super.setUp()

        let apiClient = DependencyProvider().legacyAPIClient

        outletsStorage = OutletsStorageImpl()
        outletsService = OutletsServiceImpl(
            client: APIClient(apiClient: apiClient),
            storage: outletsStorage
        )
    }

    override func tearDown() {
        outletsService = nil
        outletsStorage = nil

        super.tearDown()
    }

    func test_shouldSendProperRequest() throws {
        // given
        let outletIds: [Int] = [1_198_001, 1_198_009, 1_198_012, 1_198_013, 1_198_014]
        let regionId = "213"

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            guard let parameters = body["params"] as? [[AnyHashable: Any]], !parameters.isEmpty else { return false }
            let sentOutletsIds = parameters[0]["outletIds"] as? [Int]
            return Set(outletIds) == Set(sentOutletsIds ?? [])
        }

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_outlets",
            testBlock: isMethodPOST() &&
                verifyJsonBody(checkBodyBlock) &&
                hasHeaderNamed("X-Region-Id", value: regionId) &&
                containsQueryParams(["name": "resolveOutlets"])
        )

        // when
        let result = outletsService.loadOutlets(with: outletIds, regionId: regionId).expect(in: self)

        // then
        let outlets = try result.get()
        XCTAssertFalse(outlets.isEmpty)
    }

    func test_shouldReturnOutlets_whenRequestedOutletsByIds() {
        // given
        let outletIds: [Int] = [1_198_001, 1_198_009, 1_198_012, 1_198_013, 1_198_014]

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_outlets",
            testBlock: containsQueryParams(["name": "resolveOutlets"])
        )

        // when
        let result = outletsService.loadOutlets(with: outletIds, regionId: "213").expect(in: self)

        // then
        switch result {
        case let .success(outlets):
            XCTAssertEqual(Set(outletIds), Set(outlets.map(\.id.intValue)))
        default:
            XCTFail("Request should not fail")
        }
    }

    func test_shouldNotAskServer_whenOutletsCached() {
        // given
        let outletIds: [Int] = [1_198_001, 1_198_009, 1_198_012, 1_198_013, 1_198_014]
        var serverTriggersCount = 0

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_outlets",
            testBlock: containsQueryParams(["name": "resolveOutlets"]) &&
                dummyTestBlock { serverTriggersCount += 1 }
        )

        // when
        outletsService.loadOutlets(with: outletIds, regionId: "213").expect(in: self)
        outletsService.loadOutlets(with: outletIds, regionId: "213").expect(in: self)

        // then
        // Ожидаем 2, т.к. OHHTTPStubs дергает каждый testBlock дважды при подмене
        XCTAssertEqual(serverTriggersCount, 2)
    }

    func test_shouldAskServerForRemainingOutlets_whenPartOfOutletsCached() {
        // given
        let firstRequestedIds: [Int] = [1_198_001, 1_198_009, 1_198_012]
        let secondRequestedIds: [Int] = [1_198_012, 1_198_013, 1_198_014]

        let checkFirstBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            guard let parameters = body["params"] as? [[AnyHashable: Any]], !parameters.isEmpty else { return false }
            let sentOutletsIds = parameters[0]["outletIds"] as? [Int]
            return Set(firstRequestedIds) == Set(sentOutletsIds ?? [])
        }

        let checkSecondBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            guard let parameters = body["params"] as? [[AnyHashable: Any]], !parameters.isEmpty else { return false }
            let sentOutletsIds = parameters[0]["outletIds"] as? [Int]
            let difference = secondRequestedIds.filter { !firstRequestedIds.contains($0) }
            return Set(difference) == Set(sentOutletsIds ?? [])
        }

        stub(
            requestPartName: "api/v1",
            responseFileName: "cached_outlets_1",
            testBlock: containsQueryParams(["name": "resolveOutlets"]) &&
                verifyJsonBody(checkFirstBodyBlock)
        )

        stub(
            requestPartName: "api/v1",
            responseFileName: "cached_outlets_2",
            testBlock: containsQueryParams(["name": "resolveOutlets"]) &&
                verifyJsonBody(checkSecondBodyBlock)
        )

        // when
        let firstResult = outletsService.loadOutlets(with: firstRequestedIds, regionId: "213").expect(in: self)
        let secondResult = outletsService.loadOutlets(with: secondRequestedIds, regionId: "213").expect(in: self)

        // then
        do {
            let firstOutlets = Set(try firstResult.get().map(\.id.intValue))
            let secondOutlets = Set(try secondResult.get().map(\.id.intValue))

            XCTAssertEqual(firstOutlets, Set(firstRequestedIds))
            XCTAssertEqual(secondOutlets, Set(secondRequestedIds))
        } catch {
            XCTFail("Result should not be failures")
        }
    }

    func test_shouldFail_whenServerRespondsWith500Error() {
        // given
        stubError(
            requestPartName: "api/v1",
            code: 500,
            testBlock: containsQueryParams(["name": "resolveOutlets"])
        )

        // when
        let result = outletsService.loadOutlets(with: [1_198_001], regionId: "213").expect(in: self)

        // then
        guard case let .failure(error as ApiClientError) = result else {
            XCTFail("Can't be successfull with 500 response")
            return
        }

        guard case let .network(response: response, _, _, _) = error else {
            XCTFail("Wrong type of error")
            return
        }

        XCTAssertEqual(response?.statusCode, 500)
    }

    func test_shouldStoreInCache_whenRequestCompleted() {
        // given
        let outletIds: [Int] = [1_198_001, 1_198_009, 1_198_012, 1_198_013, 1_198_014]
        let inCacheBeforeRequest: [YBMDeliveryOutlet] = outletsStorage.obtainOutlets(withId: outletIds).wait()

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_outlets",
            testBlock: containsQueryParams(["name": "resolveOutlets"])
        )

        // when
        outletsService.loadOutlets(with: outletIds, regionId: "213").expect(in: self)
        let inCacheAfterRequest = outletsStorage.obtainOutlets(withId: outletIds).wait().map(\.id.intValue)

        // then
        XCTAssertTrue(inCacheBeforeRequest.isEmpty)
        XCTAssertEqual(Set(outletIds), Set(inCacheAfterRequest))
    }

    func test_shouldNotAskServer_whenEmptyListRequested() {
        // given
        var networkTriggered = false

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_outlets",
            testBlock: isMethodPOST() &&
                containsQueryParams(["name": "resolveOutlets"]) &&
                dummyTestBlock { networkTriggered = true }
        )

        // when
        outletsService.loadOutlets(with: [], regionId: "213").expect(in: self)

        // then
        XCTAssertFalse(networkTriggered)
    }

    func test_shouldReturnEmptyList_whenEmptyListRequested() {
        // when
        let result = outletsService.loadOutlets(with: [], regionId: "213").expect(in: self)

        // then
        switch result {
        case let .success(outlets):
            XCTAssertTrue(outlets.isEmpty)
        default:
            XCTFail("Request should succeed")
        }
    }

    func test_shoudLoadOutletsInfoForMapArea_whenCorrectResponse() throws {

        // given
        stub(
            requestPartName: "api/v1",
            responseFileName: "area_outlets_info",
            testBlock: isMethodPOST() &&
                containsQueryParams(["name": "resolveOutletsForArea"])
        )

        let requestParameters = OutletPinsInAreaRequestParams(
            topRightCoordinates: LocationCoordinate(latitude: 37, longitude: 55),
            bottomLeftCoordinates: LocationCoordinate(latitude: 37, longitude: 55),
            zoom: 9,
            parcelCharacteristics: []
        )

        // when
        let result = outletsService
            .loadOutletPinsInArea(with: requestParameters)
            .expect(in: self)

        // then
        switch result {
        case let .success(outletsInfo):
            XCTAssertEqual(outletsInfo.count, 3)

            let outletPin: OutletPin = try XCTUnwrap(
                outletsInfo.first(where: { $0.id == "59.21209074,39.86777055" })?.outletPins.first
            )

            XCTAssertTrue(outletPin.isPostamat)
            XCTAssertTrue(outletPin.isEveryDayOpen)

        default:
            XCTFail("Request should succeed")
        }
    }

    func test_shouldLoadDeliveryOutlet_whenParcelsInfoProvided() throws {
        // given
        let outletIds = [1_198_001]
        let parcelCharacteristics = [
            "tp:3033;tpc:RUR;offers:JRi_dPsy1br1iaVSGk7UxA=1|w=1|p=3033|d=5x5x5|;"
        ]

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            guard let parameters = body["params"] as? [[AnyHashable: Any]], !parameters.isEmpty else { return false }
            guard let parcelInfo = parameters[0]["parcelCharacteristics"] as? [String],
                  !parcelInfo.isEmpty else { return false }
            let sentOutletsIds = parameters[0]["outletIds"] as? [Int]
            return Set(outletIds) == Set(sentOutletsIds ?? [])
        }

        stub(
            requestPartName: "api/v1",
            responseFileName: "delivery_outlets",
            testBlock: isMethodPOST() &&
                verifyJsonBody(checkBodyBlock) &&
                containsQueryParams(["name": "resolveOutlets"])
        )

        // when
        let result = outletsService.loadOutlets(with: outletIds, parcelCharacteristics: parcelCharacteristics)
            .expect(in: self)

        // then
        let outlets = try result.get()
        let outlet = try XCTUnwrap(outlets.first)
        XCTAssertEqual(outlet.deliveryPrice, 59)
        XCTAssertEqual(outlet.deliveryDateBegin, "2021-10-29")
        XCTAssertEqual(outlet.deliveryDateEnd, "2021-10-30")
    }
}
