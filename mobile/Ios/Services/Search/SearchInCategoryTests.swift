import OHHTTPStubs
import XCTest
@testable import BeruServices
@testable import MarketModels

class SearchInCategoryTests: NetworkingTestCase {

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

    func test_shouldCreateCorrectRequestAndReturnSearchResult_whenRequestSucceeded() {
        // given
        let categoryID: NSNumber = 91_491

        let params = SearchParams()
        params.count = 24
        params.page = 1
        params.hid = categoryID
        params.nid = 456
        params.bonusId = 209_294_310
        params.reportState = "reportState"
        params.how = .asc
        params.showCredits = true
        params.showInstallments = true

        stub(
            requestPartName: "api/v1",
            responseFileName: "search_in_category",
            testBlock: isMethodPOST() &&
                verifyFAPIParameters([
                    "nid": params.nid,
                    "hid": params.hid,
                    "bonusId": params.bonusId,
                    "rs": params.reportState,
                    "count": params.count,
                    "page": params.page
                ], errorHandler: { path in
                    print(path)
                })
        )

        // when
        let asyncResult = AsyncResult<Result<SearchResult, Error>>(self)
        searchService?.searchInCategory(with: params) { result in
            asyncResult.result = result
        }

        // then
        asyncResult.wait(timeout: 1)

        switch asyncResult.result {
        case let .some(.success(searchResult)):
            XCTAssertEqual(searchResult.legacyResults.count, params.count.intValue)
            XCTAssertEqual(searchResult.page, params.page.intValue)
            XCTAssertFalse(searchResult.adult)
            XCTAssertFalse(searchResult.restrictionAge18)
        default:
            XCTFail("Wrong search result \(String(describing: asyncResult.result))")
        }
    }

    func test_shouldHaveSortParameters_whenHaveSortName() {
        // given
        let categoryID: NSNumber = 91_491

        let params = SearchParams()
        params.sortName = "1111"
        params.hid = categoryID

        stub(
            requestPartName: "api/v1",
            responseFileName: "search_in_category",
            testBlock: isMethodPOST() &&
                containsQueryParams([
                    "name": "resolveSearch"
                ]) && verifyFAPIParameters(["how": params.sortName])
        )

        // when
        let asyncResult = AsyncResult<Result<SearchResult, Error>>(self)
        searchService?.searchInCategory(with: params) { result in
            asyncResult.result = result
        }

        // then
        asyncResult.wait(timeout: 1)
        XCTAssertNoThrow(try asyncResult.result?.get())
    }

}
