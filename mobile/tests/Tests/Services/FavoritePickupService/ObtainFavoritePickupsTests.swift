import MarketModels
import OHHTTPStubs
import SwiftyJSON
import XCTest
@testable import BeruServices

class ObtainFavoritePickupsTests: NetworkingTestCase {

    private var favoritePickupService: FavoritePickupServiceImpl!

    override func setUp() {
        super.setUp()
        favoritePickupService = FavoritePickupServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        favoritePickupService = nil
        super.tearDown()
    }

    func test_shouldReceiveProperFavoritePickups() {
        // given
        stub(
            requestPartName: "resolveFavoritePickupPoints",
            responseFileName: "obtain_favorite_pickups_response",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveFavoritePickupPoints"])
        )

        // when
        let result = favoritePickupService.obtainFavoritePickups().expect(in: self)

        // then
        XCTAssertEqual(try result.get().count, 2)
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        stubError(requestPartName: "resolveFavoritePickupPoints", code: 500)

        // when
        let result = favoritePickupService.obtainFavoritePickups().expect(in: self)

        // then
        guard case let .failure(error as ApiClientError) = result else {
            XCTFail("Can't be successfull with 500 response")
            return
        }

        guard case let .network(response: response, _, _, _) = error else {
            XCTFail("Wrong type of error")
            return
        }

        XCTAssertEqual(response?.statusCode, 500)
    }

    func test_shouldReturnError_whenReceivedFAPIError() {
        // given
        var thrownError: Error?
        stub(
            requestPartName: "resolveFavoritePickupPoints",
            responseFileName: "obtain_favorite_pickups_with_error"
        )

        // when
        let result = favoritePickupService.obtainFavoritePickups().expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            thrownError = error
        }

        XCTAssertEqual(thrownError as? ServiceError, .unknown())
    }

    func test_shouldReturnError_whenReceivedInvalidResponse() {
        // given
        var thrownError: Error?
        stub(
            requestPartName: "resolveFavoritePickupPoints",
            responseFileName: "obtain_favorite_pickups_invalid_response"
        )

        // when
        let result = favoritePickupService.obtainFavoritePickups().expect(in: self)

        // then
        XCTAssertThrowsError(try result.get()) { error in
            thrownError = error
        }

        XCTAssertEqual(thrownError as? ServiceError, .invalidResponseClass())
    }
}
