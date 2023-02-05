import OHHTTPStubs
import XCTest

@testable import BeruServices

class ForceUpdateServiceTests: NetworkingTestCase {

    private var forceUpdateService: ForceUpdateServiceImpl!

    override func setUp() {
        super.setUp()
        forceUpdateService = ForceUpdateServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        forceUpdateService = nil
        super.tearDown()
    }

    func test_shouldReturnConfiguration_whenRequestSucceeded() throws {
        // given
        stub(
            requestPartName: "api/v1",
            responseFileName: "resolveAppForceUpdate_success",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveAppForceUpdate"])
        )

        // when
        let result = forceUpdateService.obtainConfiguration().expect(in: self)

        // then
        let configuration = try result.get()
        XCTAssertFalse(configuration.enabled)
        XCTAssertEqual(configuration.storeURL, URL(string: "https://itunes.apple.com/ru/app/id1369890634"))
        XCTAssertEqual(
            configuration.message,
            "Вы используете устаревшую версию приложения.\nМы много работали и приложение стало лучше."
        )
    }

    func test_shouldReturnError_whenRequestFailed() {
        // given
        stub(
            requestPartName: "api/v1",
            responseFileName: "resolveAppForceUpdate_error",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveAppForceUpdate"])
        )

        // when
        let result = forceUpdateService.obtainConfiguration().expect(in: self)

        // then
        XCTAssertThrowsError(try result.get())
    }
}
