import MarketUITestMocks
import XCTest

final class CartUpsalePlusTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testUpsalePlusReturnWithoutSubscription() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5264")
        Allure.addEpic("Корзина")
        Allure.addFeature("Апсейл плюса")
        Allure.addTitle("Проверяем наличие и переход по апсейлу плюса")

        enable(toggles: FeatureNames.upsaleYandexPlusOnCart, FeatureNames.showPlus)

        var cartPage: CartPage!

        "Настраиваем стейты".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.noMarketCashback)
            stateManager?.setState(newState: authState)
            stateManager?.setState(newState: UpsalePlusState())
        }

        "Открываем корзину.".ybm_run { _ in
            cartPage = goToCart()
        }

        "Свайпаем до блока апсейла. Проверяем текст.".ybm_run { _ in
            let cell = cartPage.upsalePlus.cell
            cartPage.collectionView.ybm_swipeCollectionView(toFullyReveal: cell)
            XCTAssertEqual(
                cell.staticTexts.firstMatch.label,
                "–50% на годовую подписку Плюса Подключите сейчас и получите бесплатную доставку"
            )
        }

        "Открываем стори плюса и закрываем.".ybm_run { _ in
            cartPage.upsalePlus.cell.tap()
            let storyPlusPage = StoryPlusPage.current
            wait(forVisibilityOf: storyPlusPage.element)
            storyPlusPage.element.swipeDown()
        }

        "Проверяем, что блок апсейла на месте".ybm_run { _ in
            wait(forVisibilityOf: cartPage.upsalePlus.cell)
        }
    }
}
