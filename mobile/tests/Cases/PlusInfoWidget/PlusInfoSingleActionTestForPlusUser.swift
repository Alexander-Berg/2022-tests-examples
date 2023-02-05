import MarketUITestMocks
import XCTest

final class PlusInfoSingleActionTestForPlusUser: PlusInfoSingleActionTestFlow {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testPlusInfoSingleActionWidgetIsNotVisibleForPlusUser() {
        passWidgetCheckFlowWithZeroBalance()
    }

    func testPlusInfoSingleActionWidgetIsVisibleForPlusUser() {
        passWidgetCheckFlowWithBalance()
    }

}
