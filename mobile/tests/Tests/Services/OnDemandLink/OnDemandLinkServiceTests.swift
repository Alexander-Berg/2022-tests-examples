import BeruLegacyNetworking
import MarketProtocols
import OHHTTPStubs
import SwiftyJSON
import XCTest

@testable import BeruServices

final class OnDemandLinkServiceTests: NetworkingTestCase {

    // MARK: - Properties

    private var onDemandLinkService: OnDemandLinkService!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        onDemandLinkService = OnDemandLinkServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        super.tearDown()
        onDemandLinkService = nil
    }

    // MARK: - Get On Demand Link

    func test_getOnDemandLink_shouldSendProperRequestAndReturnProperUrl() throws {
        // given
        let isGoInstalled = false
        let trackCode = UUID().uuidString
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            guard
                let params = json["params"].array?.first,
                let parsedIsGoInstalled = params["isGoInstalled"].bool,
                let parsedTrackCode = params["id"].string
            else {
                return false
            }
            return parsedIsGoInstalled == isGoInstalled
                && parsedTrackCode == trackCode
        }
        stub(
            requestPartName: "resolveOnDemandLink",
            responseFileName: "resolve_on_demand_link",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveOnDemandLink"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = onDemandLinkService
            .getOnDemandLink(trackCode: trackCode, isGoInstalled: isGoInstalled)
            .expect(in: self)

        // then
        XCTAssertNotNil(try? result.get())
    }

    func test_getOnDemandLink_shouldThrowError() throws {
        // given
        stub(
            requestPartName: "resolveOnDemandLink",
            responseFileName: "resolve_on_demand_link_error"
        )

        // when
        let result = onDemandLinkService
            .getOnDemandLink(trackCode: "", isGoInstalled: true)
            .expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as? ServiceError, ServiceError.unknown())
        }
    }
}
