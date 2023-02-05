import BeruLegacyNetworking
import MarketProtocols
import OHHTTPStubs
import SwiftyJSON
import XCTest

@testable import BeruServices

final class AgitationServiceTests: NetworkingTestCase {

    // MARK: - Properties

    private var agitationService: AgitationService!

    override func setUp() {
        super.setUp()
        agitationService = AgitationServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        super.tearDown()
        agitationService = nil
    }

    // MARK: - Reading agitations

    func test_readingAgitations_shouldSendProperRequest() throws {
        // given
        let agitationId = "342"
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard let parsedAgitationId = params?.first?["agitationId"].string else {
                return false
            }
            return parsedAgitationId == agitationId
        }

        stub(
            requestPartName: "completeAgitationById",
            responseFileName: "complete_agitation",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "completeAgitationById"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = agitationService.completeAgitation(by: agitationId).expect(in: self)

        // then
        _ = try XCTUnwrap(result.get())
    }

    func test_readingAgitations_shouldThrowIfErrorReceived() {
        // given
        stub(
            requestPartName: "completeAgitationById",
            responseFileName: "complete_agitation_error"
        )

        // when
        let result = agitationService.completeAgitation(by: "342").expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { _ in }
    }
}
