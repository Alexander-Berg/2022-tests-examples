import OHHTTPStubs
import XCTest

@testable import BeruServices

class OrderStatusUpdatedAndGetCoinsTests: CoinsServiceTest {

    func test_shouldReceiveProperCoins() {
        // given
        let orderIds: [OrderId] = [122, 133]

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let params = body["params"] as? [[AnyHashable: Any]]
            let orderIds = params?.first?["orderIds"] as? [Int]
            return orderIds == [122, 133]
        }

        stub(
            requestPartName: "resolveOrderStatusUpdatedAndGetCoins",
            responseFileName: "simple_get_coins",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveOrderStatusUpdatedAndGetCoins"])
                && verifyJsonBody(checkBodyBlock)
        )
        // when
        let result = coinsService
            .getCoins(for: orderIds)
            .expect(in: self)

        // then
        switch result {
        case let .success(responseObject):
            XCTAssertEqual(responseObject.newBonuses.count, 2)
            XCTAssertEqual(responseObject.oldBonuses.count, 2)
            XCTAssertEqual(responseObject.recommendedBonuses.count, 2)
            XCTAssertEqual(responseObject.chooseFromBonuses.count, 3)
        default:
            XCTFail("Wrong create coin result \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let orderIds: [OrderId] = [122, 133]

        stubError(requestPartName: "resolveOrderStatusUpdatedAndGetCoins", code: 500)

        // when
        let result = coinsService
            .getCoins(for: orderIds)
            .expect(in: self)

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
