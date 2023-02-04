import MarketModels
import OHHTTPStubs
import XCTest
@testable import BeruServices

final class RegionServiceTests: NetworkingTestCase {

    // MARK: - Properties

    var regionService: RegionServiceImpl!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        let apiClient = DependencyProvider().legacyAPIClient
        let dummySettings = DummyRegionSettings()

        regionService = RegionServiceImpl(
            apiClient: APIClient(apiClient: apiClient),
            settings: dummySettings
        )
    }

    override func tearDown() {
        regionService = nil
        super.tearDown()
    }

    // MARK: - Tests

    func test_sholdGetCurrentRegionFromSettings() {
        // given

        let apiClient = DependencyProvider().legacyAPIClient
        let dummySettings = DummyRegionSettings()
        let fakeRegion = YMTRegion(id: 277, name: "StubRegion", type: .city)
        dummySettings.selectedRegion = fakeRegion

        regionService = RegionServiceImpl(
            apiClient: APIClient(apiClient: apiClient),
            settings: dummySettings
        )

        // when
        let regionServiceRegion = regionService.currentRegion

        // then
        XCTAssertEqual(regionServiceRegion?.id, fakeRegion.id)
        XCTAssertEqual(regionServiceRegion?.name, fakeRegion.name)
        XCTAssertEqual(regionServiceRegion?.type, fakeRegion.type)
    }

    func test_sholdChangeCurrentRegoin_whenUpdateRegionCalled() {
        // given
        let fakeRegion = YMTRegion(id: 299, name: "StubRegion2", type: .country)

        // when
        regionService.updateSelectedRegion(fakeRegion)
        let regionServiceRegion = regionService.currentRegion

        // then
        XCTAssertEqual(regionServiceRegion?.id, fakeRegion.id)
        XCTAssertEqual(regionServiceRegion?.name, fakeRegion.name)
        XCTAssertEqual(regionServiceRegion?.type, fakeRegion.type)
    }

    func test_sholdUpdateRegionInSettings_whenUpdateRegionCalled() {
        // given
        let apiClient = DependencyProvider().legacyAPIClient
        let dummySettings = DummyRegionSettings()
        let fakeRegion = YMTRegion(id: 301, name: "StubRegion3", type: .cityDistrict)
        dummySettings.selectedRegion = fakeRegion

        regionService = RegionServiceImpl(
            apiClient: APIClient(apiClient: apiClient),
            settings: dummySettings
        )

        // when
        regionService.updateSelectedRegion(fakeRegion)
        let regionFromSettings = dummySettings.selectedRegion

        // then
        XCTAssertEqual(regionFromSettings?.id, fakeRegion.id)
        XCTAssertEqual(regionFromSettings?.name, fakeRegion.name)
        XCTAssertEqual(regionFromSettings?.type, fakeRegion.type)
    }

    func test_sholdReturnAutodectedFlag_whenNoSelectedRegion() {
        let apiClient = DependencyProvider().legacyAPIClient
        let dummySettings = DummyRegionSettings()

        regionService = RegionServiceImpl(
            apiClient: APIClient(apiClient: apiClient),
            settings: dummySettings
        )

        // when
        let autodetected = regionService.isAutodetected

        // then
        XCTAssertTrue(autodetected)
    }

    func test_sholdReturnNotAutodectedFlag_whenNoSelectedRegion() {
        // given
        let apiClient = DependencyProvider().legacyAPIClient
        let dummySettings = DummyRegionSettings()
        let fakeRegion = YMTRegion(id: 301, name: "StubRegion3", type: .cityDistrict)
        dummySettings.selectedRegion = fakeRegion

        regionService = RegionServiceImpl(
            apiClient: APIClient(apiClient: apiClient),
            settings: dummySettings
        )

        // when
        let autodetected = regionService.isAutodetected

        // then
        XCTAssertFalse(autodetected)
    }

}
