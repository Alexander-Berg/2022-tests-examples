import OHHTTPStubs
import XCTest
@testable import BeruServices

class SearchPopularsTests: NetworkingTestCase {

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

    func test_shouldSendProperRequestAndReturnModels() {
        // given
        let hid = 123

        let params = SearchParams()
        params.hid = NSNumber(value: hid)

        stub(
            requestPartName: "api/v1",
            responseFileName: "search_popular_products",
            testBlock: isMethodPOST() &&
                containsQueryParams(["name": "resolvePopularProducts"]) &&
                verifyFAPIParameters(["categoryId": hid])
        )

        // when
        let asyncResult = AsyncResult<Result<[YMTModel], Error>>(self)
        searchService?.searchPopulars(with: params) { result in
            asyncResult.result = result
        }

        // then
        asyncResult.wait(timeout: 1)

        switch asyncResult.result {
        case let .some(.success(models)):
            XCTAssertEqual(models.count, 24)
        default:
            XCTFail("Wrong search result \(String(describing: asyncResult.result))")
        }
    }

}
