import OHHTTPStubs
import XCTest

@testable import BeruServices

class ObtainCoinsCountTests: CoinsServiceTest {

    func test_shouldReceiveProperCoinsCount() {
        // given
        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_obtain_coins_count",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveActiveBonusesCount"])
        )

        // when
        let result = coinsService.obtainCoinsCount().expect(in: self)

        // then
        switch result {
        case let .success(count):
            XCTAssertEqual(count, 12)
        default:
            XCTFail("Wrong obtain coins count result \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        stubError(requestPartName: "resolveActiveBonusesCount", code: 500)

        // when
        let result = coinsService.obtainCoinsCount().expect(in: self)

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

    func test_shouldSendCoinsCountUpdatedNotification_whenReceivedUpdateRequest() {
        // given
        let count = 12
        stub(
            requestPartName: "api/v1",
            responseFileName: "simple_obtain_coins_count",
            testBlock: isMethodPOST() && containsQueryParams(["name": "resolveActiveBonusesCount"])
        )

        // when
        let condition = expectation(
            forNotification: Notification.Name.CoinsService.coinsCountUpdatedNotification,
            object: coinsService
        ) { notification in
            let countKey = Notification.CoinsService.coinsCountUpdatedNotificationCountKey
            let obtainedCount = notification.userInfo?[countKey] as? Int
            return obtainedCount == count
        }
        _ = coinsService.obtainCoinsCount()

        // then
        wait(for: [condition], timeout: 1)
    }
}
