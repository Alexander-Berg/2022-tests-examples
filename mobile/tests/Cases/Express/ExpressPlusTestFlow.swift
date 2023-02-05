import MarketUITestMocks
import XCTest

class ExpressPlusTestFlow: LocalMockTestCase {

    func makeExpressPlusTestFlow() {
        var expressPage: ExpressPage!

        "Настраиваем стейт".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock

            var cmsState = CMSState()
            cmsState.setCMSState(with: CMSState.CMSCollections.expressCollections)
            stateManager?.setState(newState: cmsState)

            let expressState = ExpressState()
            stateManager?.setState(newState: expressState)

            var authState = UserAuthState()
            authState.setAddressesState(addresses: [.default])
            authState.setPlusBalanceState(.withZeroMarketCashback)
            stateManager?.setState(newState: authState)
        }

        "Открываем экран экспресса".ybm_run { _ in
            expressPage = goToExpress()
        }

        "Проверяем наличие бейджа Плюса, переходим в Дом Плюса".ybm_run { _ in
            wait(forVisibilityOf: expressPage.plusButton)

            expressPage.plusButton.tap()

            wait(forVisibilityOf: HomePlusPage.current.element)
        }
    }
}
