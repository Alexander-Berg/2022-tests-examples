import MarketUITestMocks
import XCTest

final class CartPlusCellTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testCartYandexPlusCell() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4194")
        Allure.addEpic("Корзина")
        Allure.addFeature("Кешбэк. Блок про баллы в корзине")
        Allure.addTitle("Текст в блоке в зависимости от баллов и кешбэка в корзине")

        enable(toggles: FeatureNames.plusBenefits)
        disable(toggles: FeatureNames.cartRedesign)

        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo

        var root: RootPage!
        var cartPage: CartPage!

        setState(with: 1_458)

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartPlusCellWithCashback")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Листаем до блока с Плюсом и сравниваем текст".ybm_run { _ in
            cartPage.collectionView.swipe(to: .down, until: cartPage.plusSubscriptionCell.element.isVisible)
            XCTAssertEqual(
                cartPage.plusSubscriptionCell.title.label,
                "Купите дешевле на 1 458 ₽ Спишите ﻿﻿ 1 458 баллов при оформлении или получите ﻿﻿ 34 балла за покупку"
            )
        }

        "Мок для удаления товара".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartPlusCellWithCashbackDeletedItem")
        }

        "Удаляем товар с кешбэком, остается товар без кешбэка".ybm_run { _ in
            cartPage.collectionView.ybm_swipe(toFullyReveal: cartPage.cartItem(at: 0).element)

            var mainOffer: CartPage.CartItem!
            var secondaryOffer: CartPage.CartItem!

            mainOffer = cartPage.cartItem(at: 0)
            mainOffer.removeButton.tap()

            secondaryOffer = cartPage.cartItem(at: 1)
            wait(forInvisibilityOf: secondaryOffer.element)
        }

        "Сравниваем текст у блока с Плюсом".ybm_run { _ in
            cartPage.collectionView.swipe(to: .down, until: cartPage.plusSubscriptionCell.element.isVisible)

            XCTAssertEqual(
                cartPage.plusSubscriptionCell.title.label,
                "Купите дешевле на 1 458 ₽ Спишите ﻿﻿ 1 458 баллов при оформлении"
            )
        }
    }

    func testCartYandexPlusCellWithoutScores() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4726")
        Allure.addEpic("Корзина.")
        Allure.addFeature("Кешбэк. Блок про баллы в корзине у плюсовика без баллов")
        Allure.addTitle("Блок про баллы не отображается")

        enable(toggles: FeatureNames.plusBenefits)
        disable(toggles: FeatureNames.cartRedesign)

        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo

        var root: RootPage!
        var cartPage: CartPage!

        setState(with: 0)

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartPlusCellWithoutScores")
        }

        "Открываем корзину".ybm_run { _ in
            root = appAfterOnboardingAndPopups()

            cartPage = goToCart(root: root)
            wait(forExistanceOf: cartPage.cartItem(at: 0).element)
        }

        "Листаем до блока саммари и проверяем отсутствие блока с баллами".ybm_run { _ in
            cartPage.collectionView.swipe(to: .down, until: cartPage.summary.element.isVisible)
            XCTAssertFalse(cartPage.plusSubscriptionCell.element.exists)
        }

    }

    private var toggleInfo: String {
        let name = FeatureNames.plusBenefits.lowercased()
        let toggleAdditionalInfo = [
            name: [
                "spendCashbackInfoAllowed": true
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

    private func setState(with balance: Int) {
        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withBalance(balance))
            stateManager?.setState(newState: authState)
        }
    }
}
