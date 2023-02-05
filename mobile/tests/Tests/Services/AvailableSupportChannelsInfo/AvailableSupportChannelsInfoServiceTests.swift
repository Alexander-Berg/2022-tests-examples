import BeruLegacyNetworking
import MarketProtocols
import OHHTTPStubs
import SwiftyJSON
import XCTest

@testable import BeruServices

final class AvailableSupportChannelsInfoServiceTests: NetworkingTestCase {

    // MARK: - Properties

    private var availableSupportChannelsInfoService: AvailableSupportChannelsInfoService!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()
        availableSupportChannelsInfoService = AvailableSupportChannelsInfoServiceImpl(
            apiClient: DependencyProvider()
                .apiClient
        )
    }

    override func tearDown() {
        super.tearDown()
        availableSupportChannelsInfoService = nil
    }

    // MARK: - Check Brand Zone

    func test_getAvailableSupportChannelsInfo_shouldSendProperRequestAndReturnProperResult() throws {
        // given
        stub(
            requestPartName: "resolveAvailableSupportChannelsInfo",
            responseFileName: "available_support_channels_info",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveAvailableSupportChannelsInfo"])
        )

        // when
        let result = availableSupportChannelsInfoService
            .getAvailableSupportChannelsInfo()
            .expect(in: self)

        // then
        let response = try XCTUnwrap(result.get())
        XCTAssertTrue(response.isChatAvailable)
        XCTAssertTrue(response.isPhoneAvailable)
        XCTAssertTrue(response.isFbsOrExpressChatAvailable)
    }

    func test_getAvailableSupportChannelsInfo_shouldThrowError() throws {
        // given
        stub(
            requestPartName: "resolveAvailableSupportChannelsInfo",
            responseFileName: "available_support_channels_info_error"
        )

        // when
        let result = availableSupportChannelsInfoService
            .getAvailableSupportChannelsInfo()
            .expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            XCTAssertEqual(error as? ServiceError, ServiceError.invalidResponseFormat())
        }
    }
}
