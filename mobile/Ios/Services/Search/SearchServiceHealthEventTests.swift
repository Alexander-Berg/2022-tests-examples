import MarketModels
import MarketUnitTestHelpers
import PassKit
import SwiftyJSON
import XCTest
@testable import BeruHealthMetrics
@testable import BeruServices
@testable import Metrics

final class SearchServiceHealthEventTests: NetworkingTestCase {

    private var searchService: SearchServiceImpl!

    override func setUp() {
        super.setUp()
        MetricRecorder.isRecording = true
        searchService = SearchServiceImpl(
            apiClient: DependencyProvider().apiClient,
            sinsCommissionManager: SinsCommissionManagerStub(),
            isSuggestSourceFeatureEnabled: false,
            cartSnapshot: [],
            lavkaShopId: nil,
            lavkaRootLayoutId: nil
        )
    }

    override func tearDown() {
        searchService = nil
        MetricRecorder.isRecording = false
        MetricRecorder.clear()
        super.tearDown()
    }

    func testSerarchService_shouldSendSearchError_whenSearchFails() {
        // given
        stubError(requestPartName: "search", code: 500)

        let expectedParams: [AnyHashable: AnyHashable] = [
            "name": HealthEventName.searchProductsError.name,
            "level": HealthEventLevel.error.value,
            "portion": HealthEventPortion.core.value
        ]

        // when
        let exp = XCTestExpectation()

        let searchParams = SearchParams()

        searchService.search(with: searchParams) { _ in
            exp.fulfill()
        }

        wait(for: [exp], timeout: 2)

        // then
        wait {
            MetricRecorder.events(from: .health).with(params: expectedParams).isNotEmpty
        }
    }

    func testSerarchService_shouldSendSearchError_whenSearchInCategoryFails() {
        // given
        stubError(requestPartName: "categories/search", code: 500)

        let expectedParams: [AnyHashable: AnyHashable] = [
            "name": HealthEventName.searchInCategoryError.name,
            "level": HealthEventLevel.error.value,
            "portion": HealthEventPortion.core.value
        ]

        // when
        let exp = XCTestExpectation()

        let searchParams = SearchParams()

        searchService.searchInCategory(with: searchParams) { _ in
            exp.fulfill()
        }

        wait(for: [exp], timeout: 2)

        // then
        wait {
            MetricRecorder.events(from: .health).with(params: expectedParams).isNotEmpty
        }
    }

    func testSerarchService_shouldSendSearchError_whenRedirectFails() {
        // given
        stubError(requestPartName: "redirect", code: 500)

        let expectedParams: [AnyHashable: AnyHashable] = [
            "name": HealthEventName.searchRedirectError.name,
            "level": HealthEventLevel.error.value,
            "portion": HealthEventPortion.core.value
        ]

        // when
        let exp = XCTestExpectation()

        let searchParams = SearchParams()

        searchService.redirectWithSearchInCategory(with: searchParams) { _ in
            exp.fulfill()
        }

        wait(for: [exp], timeout: 2)

        // then
        wait {
            MetricRecorder.events(from: .health).with(params: expectedParams).isNotEmpty
        }
    }

    func testSerarchService_shouldSendSearchError_whenSearchFiltersFails() {
        // given
        stubError(requestPartName: "search/filters", code: 500)

        let expectedParams: [AnyHashable: AnyHashable] = [
            "name": HealthEventName.searchFiltersError.name,
            "level": HealthEventLevel.error.value,
            "portion": HealthEventPortion.core.value
        ]

        // when
        let exp = XCTestExpectation()

        let searchParams = SearchParams()
        searchParams.text = "abc"

        searchService.searchFilters(with: searchParams) { _ in
            exp.fulfill()
        }

        wait(for: [exp], timeout: 2)

        // then
        wait {
            MetricRecorder.events(from: .health).with(params: expectedParams).isNotEmpty
        }
    }
}
