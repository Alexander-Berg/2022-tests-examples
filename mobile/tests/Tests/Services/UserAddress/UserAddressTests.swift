import MarketModels
import OHHTTPStubs
import XCTest

@testable import BeruServices

final class UserAddressTests: NetworkingTestCase {

    // MARK: - Properties

    var userAddressService: UserAddressServiceImpl!

    // MARK: - Lifecycle

    override func setUp() {
        super.setUp()

        let legacyApiClient = DependencyProvider().legacyAPIClient

        userAddressService = UserAddressServiceImpl(
            apiClient: APIClient(apiClient: legacyApiClient),
            isFilterAddressesFeatureEnabled: true
        )
    }

    override func tearDown() {
        userAddressService = nil
        super.tearDown()
    }

    // MARK: - Tests

    func test_shouldReturnUserAddresses() throws {
        // given

        let addressId = "79c2349b-8a7c-4fab-a725-12dcb3063276"

        stub(
            requestPartName: "api/v1",
            responseFileName: "obtain_user_address",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveUserAddresses"])
        )

        // when
        let result = userAddressService.obtainAddresses().expect(in: self)

        // then
        let addresses = try XCTUnwrap(result.get())
        XCTAssertTrue(addresses.compactMap { $0.id }.contains(addressId))
    }

    func test_shouldReturnUserAddress_whenAddAddressSuccessful() throws {
        // given
        let addressId = "79c2349b-8a7c-4fab-a725-12dcb3063276"

        stub(
            requestPartName: "api/v1",
            responseFileName: "add_user_address",
            testBlock: isMethodPOST() && containsQueryParams(["name": "addUserAddress"])
        )

        // when
        let result = userAddressService.addUserAddress(stubbedAddress).expect(in: self)

        // then
        let updatedAddress = try XCTUnwrap(result.get())
        XCTAssertEqual(updatedAddress.id, addressId)
    }

    func test_shouldReturnUserAddresses_whenUpdateAddressSuccessful() throws {
        // given

        stub(
            requestPartName: "api/v1",
            responseFileName: "update_user_address",
            testBlock: isMethodPOST() && containsQueryParams(["name": "updateUserAddress"])
        )

        // when
        let result = userAddressService.updateUserAddress(stubbedAddress).expect(in: self)

        // then
        let receivedAddress = try XCTUnwrap(result.get())
        XCTAssertNotNil(receivedAddress.regionId)
    }

    func test_shouldSuccess_whenDeleteUserAddressSuccessful() throws {
        // given
        let addressId = "79c2349b-8a7c-4fab-a725-12dcb3063276"

        stub(
            requestPartName: "api/v1",
            responseFileName: "delete_user_address",
            testBlock: isMethodPOST() && containsQueryParams(["name": "deleteUserAddress"])
        )

        // when
        let result = userAddressService.deleteUserAddress(addressId: addressId).expect(in: self)

        // then
        XCTAssertNoThrow(try XCTUnwrap(result.get()))
    }

    func test_shouldReturnUserAddressesInfo_whenCorrectCoordinatesPassed() throws {
        // given

        stub(
            requestPartName: "api/v1",
            responseFileName: "obtain_user_address_with_coordinates",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveUserAddressAndRegionByGpsCoordinate"])
        )

        // when
        let result = userAddressService.obtainUserAddress(
            latitude: 55.762_996,
            longitude: 37.622_486,
            prevRegionId: 213
        ).expect(in: self)

        // then
        let addressInfo = try XCTUnwrap(result.get())
        XCTAssertEqual(addressInfo.regionId, 117_069)
    }

    // MARK: - Private

    var stubbedAddress: Address {
        let address = Address()
        address.regionId = 213
        address.city = "Казань"
        address.house = "30"
        address.country = "Россия"
        address.street = "улица Баумана"
        address.coordinates = LocationCoordinate(
            latitude: 55.793_490_075_538_38,
            longitude: 49.107_923_159_223_8
        )
        return address
    }
}
