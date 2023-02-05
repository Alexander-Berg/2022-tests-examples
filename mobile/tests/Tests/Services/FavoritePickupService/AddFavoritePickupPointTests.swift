import MarketModels
import OHHTTPStubs
import SwiftyJSON
import XCTest
@testable import BeruServices

class AddFavoritePickupPointTests: NetworkingTestCase {

    private var favoritePickupService: FavoritePickupServiceImpl!

    override func setUp() {
        super.setUp()
        favoritePickupService = FavoritePickupServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        favoritePickupService = nil
        super.tearDown()
    }

    func test_shouldAddFavoritePickupPoint() {
        // given
        let favoritePickup = FavoritePickup(id: 123, regionId: 456)
        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let json = JSON(body)
            let params = json["params"].array
            guard
                let pickupPointId = params?.first?["pickupPointId"].int,
                let regionId = params?.first?["regionId"].int
            else {
                return false
            }
            return pickupPointId == 123
                && regionId == 456
        }

        stub(
            requestPartName: "addFavoritePickupPoint",
            responseFileName: "add_favorite_pickup_point_response",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "addFavoritePickupPoint"])
                && verifyJsonBody(checkBodyBlock)
        )

        // when
        let result = favoritePickupService.addFavoritePikcup(favoritePickup).expect(in: self)

        // then
        XCTAssertNoThrow(try result.get())
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let favoritePickup = FavoritePickup(id: 123, regionId: 456)
        stubError(requestPartName: "addFavoritePickupPoint", code: 500)

        // when
        let result = favoritePickupService.addFavoritePikcup(favoritePickup).expect(in: self)

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
}
