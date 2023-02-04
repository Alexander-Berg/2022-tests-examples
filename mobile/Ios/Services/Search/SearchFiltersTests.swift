import OHHTTPStubs
import SnapshotTesting
import XCTest
@testable import BeruServices

class SearchFiltersTests: NetworkingTestCase {

    private var searchService: SearchServiceImpl?

    override func setUp() {
        super.setUp()

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
        super.tearDown()
    }

    func test_shouldSendProperRequestAndReturnFilters_whenRequestSucceeded() {
        // given
        let params = SearchParams()
        params.text = "test text"

        stub(
            requestPartName: "api/v1",
            responseFileName: "search_filters",
            testBlock: isMethodPOST() &&
                containsQueryParams([
                    "name": "resolveSearch"
                ])
        )

        // when
        let asyncResult = AsyncResult<Result<YMTFilters, Error>>(self)
        searchService?.searchFilters(with: params) { result in
            asyncResult.result = result
        }

        // then
        asyncResult.wait(timeout: 1)

        switch asyncResult.result {
        case let .some(.success(filters)):
            assertSnapshot(matching: filters.json(), as: .dump)
        default:
            XCTFail("Wrong search result \(String(describing: asyncResult.result))")
        }
    }

    func test_shouldSendProperRequestAndReturnFilters_withBonusId_whenRequestSucceeded() {
        // given
        let params = SearchParams()
        params.bonusId = 209_294_310

        stub(
            requestPartName: "api/v1",
            responseFileName: "search_filters",
            testBlock: isMethodPOST() &&
                containsQueryParams([
                    "name": "resolveSearch"
                ])
        )

        // when
        let asyncResult = AsyncResult<Result<YMTFilters, Error>>(self)
        searchService?.searchFilters(with: params) { result in
            asyncResult.result = result
        }

        // then
        asyncResult.wait(timeout: 1)

        switch asyncResult.result {
        case let .some(.success(filters)):
            assertSnapshot(matching: filters.json(), as: .dump)
        default:
            XCTFail("Wrong search result \(String(describing: asyncResult.result))")
        }

    }

}
