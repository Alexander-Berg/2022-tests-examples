import BeruLegacyNetworking
import BeruMapping
import MarketModels
import XCTest

@testable import BeruServices

class OutOfStockReturnDateTests: NetworkingTestCase {

    var service: OutOfStockReturnDateServiceImpl!

    override func setUp() {
        super.setUp()

        service = OutOfStockReturnDateServiceImpl(
            apiClient: DependencyProvider().apiClient
        )
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_getOutOfStockReturnDate() throws {
        // given
        stub(
            requestPartName: "resolveOutStockReturnInfoByIds",
            responseFileName: "out_of_stock_return_date"
        )
        let skuId = "101197965774"
        let regionId = 10_940
        let productId = 56_789

        // when
        let result = service?.getOutOfStockReturnDate(
            skuIds: [skuId],
            productIds: [productId],
            offerIds: [],
            regionId: regionId
        )
        .expect(in: self)

        // then
        let outOfStockReturnDate = try XCTUnwrap(result?.get())
        XCTAssertEqual(
            outOfStockReturnDate.dateOfReturningInStock(skuId: skuId),
            "02.02"
        )
    }

}
