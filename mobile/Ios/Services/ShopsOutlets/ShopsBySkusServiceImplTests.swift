import MarketProtocols
import OHHTTPStubs
import XCTest

@testable import BeruServices

final class ShopsBySkusServiceImplTests: NetworkingTestCase {

    var shopsBySkusService: ShopsBySkusService?

    override func setUp() {
        super.setUp()
        shopsBySkusService = ShopsBySkusServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        shopsBySkusService = nil
        super.tearDown()
    }

    func test_shouldLoadPharmacyShopsTests() {
        // given
        stub(
            requestPartName: Constants.requestPartName,
            responseFileName: Constants.LoadPharmacyShops.responseFile,
            testBlock: containsQueryParams(["name": Constants.LoadPharmacyShops.resolverName])
        )
        let items = [
            makeShopOutletItem()
        ]

        // when
        let result = shopsBySkusService?.loadPharmacyShops(
            regionId: Constants.regionId,
            items: items,
            maxOutlets: nil,
            shopOutletGeo: nil
        ).expect(in: self)

        // then
        switch result {
        case let .success(shops):
            XCTAssertFalse(shops.isEmpty)
        default:
            XCTFail("Request should not fail")
        }
    }

    func test_shouldFail_whenServerRespondsWith500Error() {
        // given
        stubError(
            requestPartName: Constants.requestPartName,
            code: Int32(Constants.errorNumber500),
            testBlock: containsQueryParams(["name": Constants.LoadPharmacyShops.resolverName])
        )
        let items = [
            makeShopOutletItem()
        ]

        // when
        let result = shopsBySkusService?.loadPharmacyShops(
            regionId: Constants.regionId,
            items: items,
            maxOutlets: nil,
            shopOutletGeo: nil
        ).expect(in: self)

        // then
        guard case let .failure(error as ApiClientError) = result else {
            XCTFail("Can't be successfull with 500 response")
            return
        }

        guard case let .network(response: response, _, _, _) = error else {
            XCTFail("Wrong type of error")
            return
        }

        XCTAssertEqual(response?.statusCode, Constants.errorNumber500)
    }

    // MARK: - Private

    private func makeShopOutletItem() -> ShopOutletItem {
        ShopOutletItem(
            msku: Constants.LoadPharmacyShops.marketSku,
            count: Constants.LoadPharmacyShops.count
        )
    }
}

// MARK: - Nested Types

extension ShopsBySkusServiceImplTests {
    enum Constants {
        static let regionId: Int = 213
        static let requestPartName: String = "api/v1"
        static let errorNumber500: Int = 500

        enum LoadPharmacyShops {
            static let responseFile: String = "pharmacy_shops"
            static let marketSku: String = "101348309728"
            static let count: Int = 1
            static let resolverName: String = "resolveShopsBySkus"
        }
    }
}
