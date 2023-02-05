import MarketUITestMocks
import SwiftUI
import XCTest

class OrderWithCashbackFlow: LocalMockTestCase {

    func makeOrder(with mock: String...) -> FinishMultiorderPage {
        enable(toggles: FeatureNames.plusBenefits)
        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo

        var root: RootPage!
        var cart: CartPage!
        var checkoutPage: CheckoutPage!
        var finishPage: FinishMultiorderPage!

        setState()

        "Мокаем ручки".ybm_run { _ in
            mock.forEach {
                mockStateManager?.pushState(bundleName: $0)
            }
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Идем в корзину".ybm_run { _ in
            cart = goToCart(root: root)
            wait(forExistanceOf: cart.compactSummary.orderButton.element)
        }

        "Нажимаем на кнопку \"Оформить заказ\"".ybm_run { _ in
            checkoutPage = cart.compactSummary.orderButton.tap()
        }

        "Подтверждаем заказ".ybm_run { _ in
            checkoutPage.element.ybm_swipeCollectionView(toFullyReveal: checkoutPage.paymentButton.element)
            finishPage = checkoutPage.paymentButton.tap()
        }

        return finishPage
    }

    func setState() {
        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withMarketCashback_5)
            stateManager?.setState(newState: authState)
        }
    }

    private var toggleInfo: String {
        let name = FeatureNames.plusBenefits.lowercased()
        let toggleAdditionalInfo = [
            name: [
                "deliveryPopupEnabled": true
            ]
        ]
        guard let toggleInfosData = try? JSONSerialization.data(
            withJSONObject: toggleAdditionalInfo,
            options: .prettyPrinted
        )
        else {
            return ""
        }
        return String(data: toggleInfosData, encoding: .utf8) ?? ""
    }
}
