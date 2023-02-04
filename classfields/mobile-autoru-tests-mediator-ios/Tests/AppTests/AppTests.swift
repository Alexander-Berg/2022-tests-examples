@testable import App
import XCTVapor

final class APITests: XCTestCase {
    func test_pingEndpoint() throws {
        let app = Application(.testing)

        defer { app.shutdown() }

        let config = Config(teamcityHost: "", teamcityPort: 0, teamcityToken: "", useTeamcityStub: true, apiPort: 80)

        try configure(app, config: config)

        try app.test(.GET, "ping") { res in
            XCTAssertEqual(res.status, .ok)
        }
    }
}
