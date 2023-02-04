import BeruLegacyNetworking
import BeruMapping
import MarketModels
import XCTest

@testable import BeruServices

class ShopInfoServiceTests: NetworkingTestCase {

    var service: ShopInfoServiceImpl!

    override func setUp() {
        super.setUp()

        service = ShopInfoServiceImpl(
            apiClient: DependencyProvider().apiClient
        )
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_shouldParseShopInfo_whenGetShopInfoSucceeded() throws {
        // given
        stub(
            requestPartName: "api/v1",
            responseFileName: "resolve_shop_info"
        )

        // when
        let result = service?.getInfoForShop(withId: 12_345_678).expect(in: self)

        // then
        let shopInfo = try XCTUnwrap(result?.get())

        XCTAssertEqual(shopInfo.id, 12_345_678)
        XCTAssertEqual(shopInfo.name, "ООО Рога и копыта")
        XCTAssertEqual(shopInfo.feedsCount, 2)
    }

}
