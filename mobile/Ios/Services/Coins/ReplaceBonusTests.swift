import OHHTTPStubs
import XCTest

@testable import BeruServices

class ReplaceBonusTests: CoinsServiceTest {

    func test_replaceBonus() {
        // given

        let bonusId = "3857487"
        let recommendedBonusId = "3857488"

        let checkBodyBlock: ([AnyHashable: Any]) -> Bool = { body in
            let params = body["params"] as? [[AnyHashable: Any]]
            let bonusId = params?.first?["bonusId"] as? String
            let recommendedBonusId = params?.first?["recommendedBonusId"] as? String
            return bonusId == "3857487" && recommendedBonusId == "3857488"
        }

        stub(
            requestPartName: "resolveReplaceBonus",
            responseFileName: "simple_replace_bonus",
            testBlock: isMethodPOST()
                && containsQueryParams(["name": "resolveReplaceBonus"])
                && verifyJsonBody(checkBodyBlock)
        )
        // when
        let result = coinsService
            .replaceBonus(bonusId: bonusId, recommendedBonusId: recommendedBonusId)
            .expect(in: self)

        // then
        switch result {
        case let .success(responseObject):
            XCTAssertEqual(responseObject.count, 1)
        default:
            XCTFail("Wrong create coin result \(String(describing: result))")
        }
    }

    func test_shouldReturnError_whenStatusCodeIs500() {
        // given
        let bonusId = "3857487"
        let recommendedBonusId = "3857488"

        stubError(requestPartName: "resolveReplaceBonus", code: 500)

        // when
        let result = coinsService
            .replaceBonus(bonusId: bonusId, recommendedBonusId: recommendedBonusId)
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
