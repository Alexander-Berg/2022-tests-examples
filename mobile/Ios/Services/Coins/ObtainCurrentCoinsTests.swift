import OHHTTPStubs
import XCTest

@testable import BeruServices

class ObtainCurrentCoinsTests: CoinsServiceTest {

    func test_shouldReceiveRightAmountOfCoins() {
        let fileName = "bonuses"
        let bonusCount = 2
        // given
        stub(
            requestPartName: "resolveBonusesForPerson",
            responseFileName: fileName
        )

        // when
        let result = coinsService.obtainCurrentCoins().expect(in: self)
        // then
        switch result {
        case let .success(items):
            XCTAssertEqual(items.count, bonusCount, fileName)
        default:
            XCTFail("Wrong obtain coins count result for \(fileName)")
        }
    }

}
