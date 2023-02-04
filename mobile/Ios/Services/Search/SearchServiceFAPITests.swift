import MarketModels
import OHHTTPStubs
import SnapshotTesting
import SwiftyJSON
import XCTest
@testable import BeruServices

class SearchServiceFAPITests: NetworkingTestCase {

    private var searchService: SearchServiceImpl!

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

    func test_shouldSendProperRequestAndReturnSearchResult_whenRequestSucceeded() {
        // given
        let params = SearchParams()
        params.text = "iphone"
        params.count = 24
        params.page = 1
        params.how = .asc

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_search_fapi",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveSearch"])
        )

        // when
        let asyncResult = AsyncResult<Result<SearchResult, Error>>(self)
        searchService.search(with: params) { result in
            asyncResult.result = result
        }

        // then
        asyncResult.wait(timeout: 1)

        switch asyncResult.result {
        case let .some(.success(searchResult)):
            XCTAssertEqual(searchResult.legacyResults.count, params.count.intValue)
            XCTAssertEqual(searchResult.filters?.filters?.count, 6)
            XCTAssertEqual(searchResult.filters?.sorts?.count, 4)
            XCTAssertEqual(searchResult.page, params.page.intValue)
            XCTAssertFalse(searchResult.adult)
            XCTAssertFalse(searchResult.restrictionAge18)
        default:
            XCTFail("Wrong search result \(String(describing: asyncResult.result))")
        }
    }

    func test_shouldHaveSortParameters_whenHaveSortName() {
        // given
        let params = SearchParams()
        params.sortName = "1111"

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let paramsJSON = json["params"].arrayValue.first
            return paramsJSON?["how"].string == params.sortName
        }

        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_search_fapi",
            testBlock: verifyJsonBody(checkBodyBlock) && containsQueryParams(["name": "resolveSearch"])
        )

        // when
        let asyncResult = AsyncResult<Result<SearchResult, Error>>(self)
        searchService.search(with: params) { result in
            asyncResult.result = result
        }

        // then
        asyncResult.wait(timeout: 1)
        XCTAssertNoThrow(try asyncResult.result?.get())
    }

    func test_shouldCorrectTypo() {
        // given
        let correctText = "iphone"

        let params = SearchParams()
        params.text = "ipgone"

        stub(
            requestPartName: "api/v1",
            responseFileName: "search_with_typo",
            testBlock: containsQueryParams(["name": "resolveSearch"])
        )

        // when
        let asyncResult = AsyncResult<Result<SearchResult, Error>>(self)
        searchService.search(with: params) { result in
            asyncResult.result = result
        }

        // then
        asyncResult.wait(timeout: 1)

        switch asyncResult.result {
        case let .some(.success(searchResult)):
            XCTAssertEqual(searchResult.spelling?.source, params.text)
            XCTAssertEqual(searchResult.spelling?.result, correctText)
        default:
            XCTFail("Wrong search result \(String(describing: asyncResult.result))")
        }
    }

    func test_popularProducts() {
        // given
        let params = SearchParams()
        params.hid = 123

        stub(
            requestPartName: "api/v1",
            responseFileName: "popular_products_fapi",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolvePopularProducts"])
        )

        // when
        let asyncResult = AsyncResult<Result<[YMTModel], Error>>(self)
        searchService.searchPopulars(with: params) { result in
            asyncResult.result = result
        }

        // then
        asyncResult.wait(timeout: 1)

        switch asyncResult.result {
        case let .some(.success(popularResult)):
            XCTAssertNotEqual(popularResult.count, 0)
        default:
            XCTFail("Wrong result with popular request")
        }
    }

    func test_searchOrRedirect_withSearch() throws {
        // given
        let params = SearchParams()

        stub(
            requestPartName: "api/v1",
            responseFileName: "search_or_redirect_with_search_fapi",
            testBlock: containsQueryParams(["name": "resolveSearchOrRedirect"])
        )

        // when
        let result = searchService.searchOrRedirect(with: params).expect(in: self)

        // then
        let searchResult = try result.get()
        if case let SearchVariousResult.searchResult(expectedSearchResult, _) = searchResult {
            let hid = try XCTUnwrap(expectedSearchResult.hid)
            XCTAssertEqual(hid, 10_470_548)
            let nid = try XCTUnwrap(expectedSearchResult.nid)
            XCTAssertEqual(nid, 59_749)
        } else {
            XCTFail("Wrong result with search or redirect request")
        }
    }

    func test_searchOrRedirect_withRedirect() throws {
        // given
        let params = SearchParams()

        stub(
            requestPartName: "api/v1",
            responseFileName: "search_or_redirect_with_redirect_fapi",
            testBlock: containsQueryParams(["name": "resolveSearchOrRedirect"])
        )

        // when
        let result = searchService.searchOrRedirect(with: params).expect(in: self)

        // then
        let redirect = try result.get()
        if case let SearchVariousResult.redirect(expectedRedirect) = redirect {
            XCTAssertEqual(expectedRedirect.vendorID, 3_732_937)
        } else {
            XCTFail("Wrong result with search or redirect request")
        }
    }

    func test_searchOrUrlTransform_withSearch() throws {
        // given
        let params = SearchParams()
        let url = "https://m.yandex.ru/search?&text=зелёный"

        stub(
            requestPartName: "api/v1",
            responseFileName: "search_or_url_transform_with_search_fapi",
            testBlock: containsQueryParams(["name": "resolveSearchOrUrlTransform"])
        )

        // when
        let result = searchService.searchOrTransform(url: url, params: params).expect(in: self)

        // then
        let searchResult = try result.get()
        if case let SearchVariousResult.searchResult(expectedSearchResult, _) = searchResult {
            XCTAssertEqual(expectedSearchResult.page, 0)
            XCTAssertEqual(expectedSearchResult.legacyResults.count, 24)
        } else {
            XCTFail("Wrong result with search or redirect request")
        }
    }

    func test_searchOrUrlTransform_withRedirect() throws {
        // given
        let params = SearchParams()
        let url =
            "https://m.yandex.ru/product/560271037?suggest=1&suggest_text=Конструктор+LEGO+Technic&suggest_type=model"

        stub(
            requestPartName: "api/v1",
            responseFileName: "search_or_url_transform_with_redirect_fapi",
            testBlock: containsQueryParams(["name": "resolveSearchOrUrlTransform"])
        )

        // when
        let result = searchService.searchOrTransform(url: url, params: params).expect(in: self)

        // then
        let redirect = try result.get()
        if case let SearchVariousResult.redirect(expectedRedirect) = redirect {
            XCTAssertEqual(expectedRedirect.skuID, "560271037")
            XCTAssertEqual(expectedRedirect.type, .SKU)
        } else {
            XCTFail("Wrong result with search or redirect request")
        }
    }
}
