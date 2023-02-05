import MarketProtocols
import XCTest
@testable import BeruServices

class OutletsStorageImplTests: XCTestCase {

    private var outletsStorage: OutletsStorage!

    override func setUp() {
        super.setUp()

        outletsStorage = OutletsStorageImpl()
    }

    override func tearDown() {
        outletsStorage = nil
        super.tearDown()
    }

    func test_shouldObtainOutlets_whenFewOutletsAdded() {
        // given
        let outlets: [YBMDeliveryOutlet] = makeOutletsFromJSON(filename: "few_outlets")
        let initialOutletIds = outlets.map(\.id.intValue)
        outletsStorage.save(outlets: outlets)

        // when
        let cachedOutletsIds = outletsStorage.obtainOutlets(withId: initialOutletIds).wait().map { $0.id.intValue }

        // then
        XCTAssertFalse(cachedOutletsIds.isEmpty)
        XCTAssertEqual(cachedOutletsIds, initialOutletIds)
    }

    func test_shouldReturnMinifiedOutlets() {
        // given
        let savedOutlets: [MinifiedOutlet] = makeOutletsFromJSON(filename: "minified_outlets")
        outletsStorage.save(outlets: savedOutlets)

        // when
        let outletsFromStorage: [MinifiedOutlet] = outletsStorage.obtainOutlets(withId: savedOutlets.map(\.id))
            .wait()

        // then
        XCTAssertFalse(outletsFromStorage.isEmpty)
        XCTAssertEqual(savedOutlets, outletsFromStorage)
    }

    // MARK: - Private

    private func makeOutlet() -> MinifiedOutlet {
        MinifiedOutlet(
            id: Int.random(in: Int.min ... Int.max),
            name: UUID().uuidString,
            coordinates: LocationCoordinate(
                latitude: Double.random(in: -180 ... 180),
                longitude: Double.random(in: -180 ... 180)
            ),
            isMarketBranded: Bool.random(),
            isAroundTheClock: Bool.random(),
            isDaily: Bool.random(),
            purpose: [],
            isTryingAvailable: Bool.random()
        )
    }

    private func makeOutletsFromJSON<T: YBMJSONInstantiable>(filename: String) -> [T] {
        guard let jsonData: [[AnyHashable: Any]] = loadJson(with: filename) else { return [] }

        let parsed = jsonData
            .map { YBMJSONRepresentation(targetObject: $0) }
            .compactMap { $0.map(toClass: T.self) as? T }

        return parsed
    }
}
