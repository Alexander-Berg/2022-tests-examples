import OHHTTPStubs
import XCTest

@testable import BeruServices

class CategoryServiceTests: NetworkingTestCase {

    var service: CategoryServiceImpl?

    override func setUp() {
        super.setUp()

        service = CategoryServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_shouldCallCompletionBlockWithModel_whenLoadFiltersForCategoryWithParamsRequestSucceeded() {
        // given
        stub(
            requestPartName: "categories/16155466/filters",
            responseFileName: "resolve_filters_for_category_with_params"
        )

        let params = CategoryServiceParams(hid: 16_155_466, filtersSet: .popularFiltersSet)

        // when
        let result = service?.loadFiltersForCategory(with: params).expect(in: self)

        // then
        guard let secretSaleModel = try? result?.get() else {
            XCTFail("Result is nil")
            return
        }

        XCTAssertNotNil(secretSaleModel.filters)
        XCTAssertEqual(secretSaleModel.filters?[1].name, "Бесплатная доставка")
        XCTAssertNotNil(secretSaleModel.sorts)
    }

    func test_shoulPassCorrectParamsToRequest() {
        // given
        let params = CategoryServiceParams(hid: 16_155_466, filtersSet: .popularFiltersSet)
        params.afterRedirect = true
        params.height = 480
        params.width = 480
        params.reportState = "eJwzYgpgBAABcwCG"
        params.bonusId = 209_294_310

        stub(
            requestPartName: "categories/16155466/filters",
            responseFileName: "resolve_filters_for_category_with_params",
            testBlock: containsQueryParams([
                "height": params.height?.stringValue,
                "width": params.width?.stringValue,
                "rs": params.reportState,
                "was_redir": params.afterRedirect ? "1" : "0",
                "filter_set": "popular",
                "bonus_id": params.bonusId?.stringValue
            ])
        )

        // when
        let result = service?.loadFiltersForCategory(with: params).expect(in: self)

        // then
        XCTAssertNotNil(try? result?.get())
    }

    func test_shouldCallCompletionBlockWithError_whenNetworkErrorOccured() {
        // given
        stubError(requestPartName: "categories/16155466/filters", code: 404)

        let params = CategoryServiceParams(hid: 16_155_466, filtersSet: .popularFiltersSet)

        // when
        let result = service?.loadFiltersForCategory(with: params).expect(in: self)

        // then
        XCTAssertThrowsError(try result?.get())
    }
}
