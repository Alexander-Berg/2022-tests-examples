import MarketUITestMocks
import XCTest

final class PlusInfoSingleActionTestForNotPlusUser: PlusInfoSingleActionTestFlow {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testPlusInfoSingleActionWidgetIsNotVisible() {
        passWidgetCheckFlowWithZeroBalance()
    }

    func testPlusInfoSingleActionWidgetIsVisible() {
        passWidgetCheckFlowWithBalance()
    }

}
