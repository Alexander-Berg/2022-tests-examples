import MarketUITestMocks
import XCTest

class SamplesSKUTests: ServicesTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testInfoIntoServicePopup() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6017")
        Allure.addEpic("Samples")
        Allure.addFeature("КО")
        Allure.addTitle("Не продается на карточке")

        var skuPage: SKUPage!

        "Мокаем ручки".ybm_run { _ in
            setupSampleSKUInfoState()
        }

        "Открываем карточку товара".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Добавляем товар в корзину".ybm_run { _ in
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.addToCartButton.element)
            XCTAssertEqual(skuPage.addToCartButton.element.label, "Не продаётся")
            XCTAssertFalse(skuPage.addToCartButton.element.isEnabled)
        }
    }
}

// MARK: - Mocks

private extension SamplesSKUTests {

    func setupSampleSKUInfoState() {
        var skuState = SKUInfoState()
        skuState.setSkuInfoState(offer: FAPIOffer.sample)
        stateManager?.setState(newState: skuState)
    }

}
