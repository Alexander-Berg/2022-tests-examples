import LangExtensions
import OHHTTPStubs
import XCTest

@testable import BeruServices

final class ITunesServiceTests: NetworkingTestCase {

    private var service: ITunesServiceImpl!

    override func setUp() {
        super.setUp()

        service = ITunesServiceImpl(
            session: URLSession.shared,
            isRunningTests: true,
            iTunesLookupUrl: "https://itunes.apple.com/lookup",
            softUpdateEnabled: true
        )
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func testCurrentVersionLowerThanAvailableVersion() {
        // given
        stub(requestPartName: "itunes.apple.com/lookup", responseFileName: "lookup_result")

        // when
        let isAvailable = service.checkUpdateAvailability(bundleId: "123", currentVersion: 274).expect(in: self)

        // then
        XCTAssertTrue(try isAvailable.get())
    }

    func testCurrentVersionEqualsAvailableVersion() {
        // given
        stub(requestPartName: "itunes.apple.com/lookup", responseFileName: "lookup_result")

        // when
        let isAvailable = service.checkUpdateAvailability(bundleId: "123", currentVersion: 275).expect(in: self)

        // then
        XCTAssertFalse(try isAvailable.get())
    }

    /// Такая ситуация возможна во время тестирования новой версии приложения
    func testCurrentVersionGreaterThanAvailableVersion() {
        // given
        stub(requestPartName: "itunes.apple.com/lookup", responseFileName: "lookup_result")

        // when
        let isAvailable = service.checkUpdateAvailability(bundleId: "123", currentVersion: 276).expect(in: self)

        // then
        XCTAssertFalse(try isAvailable.get())
    }

}
