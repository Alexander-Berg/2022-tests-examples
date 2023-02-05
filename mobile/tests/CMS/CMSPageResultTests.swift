import MarketUnitTestHelpers
import XCTest
@testable import MarketDTO

class CMSPageResultTests: XCTestCase {
    func test_cmsPageResult() throws {
        // given
        let jsonData = try extractJSONData(fileName: "resolveFlatCMS_result")

        // when
        let result = try JSONDecoder().decode(
            CMSPageResult.self,
            from: try XCTUnwrap(jsonData)
        )

        // then
        XCTAssertEqual(result.document.id, 191_228)
        XCTAssertEqual(result.document.type, "apps_home_page")
        XCTAssertFalse(result.widgets.isEmpty)
    }
}
