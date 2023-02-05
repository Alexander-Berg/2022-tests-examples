import MarketUITestMocks
import XCTest

final class SKUCardModelDisclaimersTest: LocalMockTestCase {

    func testBottomDisclaimer() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-956")
        Allure.addEpic("КМ")
        Allure.addFeature("Дисклеймеры")
        Allure
            .addTitle(
                "Тест проверяет отображение общего дисклеймера (находится в конце КМ, отображается для всех типов товаров)"
            )

        var sku: SKUPage!
        let disclaimerLabel = "Информация о технических характеристиках, комплекте поставки, "
            + "стране изготовления и внешнем виде товара носит справочный характер."

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение дисклеймера, содержание текста".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.disclaimer)
            XCTAssertEqual(sku.disclaimer.text, disclaimerLabel)
        }
    }

    func testAlcohol() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2761")
        Allure.addEpic("КМ")
        Allure.addFeature("Дисклеймеры")
        Allure.addTitle("Тест на алкоголь и характеристики")

        var purchasedCell: CartPage.CartItem!
        var sku: SKUPage!

        var skuState = SKUInfoState()

        disable(toggles: FeatureNames.cartRedesign)

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .alco)
            stateManager?.setState(newState: skuState)
        }

        "Переход в корзину с товарами".ybm_run { _ in
            let cartPage = goToCart()
            purchasedCell = cartPage.cartItem(at: 0)
        }

        "Переход на товар в корзине".ybm_run { _ in
            wait(forExistanceOf: purchasedCell.element)
            sku = purchasedCell.tap()
            ybm_wait(forFulfillmentOf: { sku.didFinishLoadingInfo })
        }

        let disclaimerLabel =
            "Чрезмерное употребление алкоголя вредит вашему здоровью. Приобретение алкогольной продукции осуществляется только в торговом зале магазина."

        "Проверяем отображение дисклеймера, содержание текста".ybm_run { _ in
            sku.element.swipe(to: sku.warning().target)

            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.warning().target
            )

            let alcoWarning = sku.warning()

            XCTAssertTrue(alcoWarning.target.isVisible)
            XCTAssertEqual(alcoWarning.target.text, disclaimerLabel)
        }
    }

    func testPharma() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2762")
        Allure.addEpic("КМ")
        Allure.addFeature("Дисклеймеры")
        Allure.addTitle("Проверяем отображение дисклеймера фармы")

        var purchasedCell: CartPage.CartItem!
        var sku: SKUPage!

        var skuState = SKUInfoState()

        disable(toggles: FeatureNames.cartRedesign)

        "Настраиваем стейт".ybm_run { _ in
            let offer = modify(FAPIOffer.default) {
                $0.warnings = .pharma
            }
            let model = modify(FAPIModel.default) {
                $0.warnings = .pharma
            }
            skuState.setSkuInfoState(offer: offer, model: model)
            stateManager?.setState(newState: skuState)
        }

        "Переход в корзину с товарами".ybm_run { _ in
            let cartPage = goToCart()
            purchasedCell = cartPage.cartItem(at: 0)
        }

        "Переход на товар в корзине".ybm_run { _ in
            wait(forExistanceOf: purchasedCell.element)
            sku = purchasedCell.tap()
            ybm_wait(forFulfillmentOf: { sku.didFinishLoadingInfo })
        }

        let disclaimerLabel = "Есть противопоказания, посоветуйтесь с врачом"

        "Проверяем отображение дисклеймера, содержание текста".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.warning().target
            )

            let pharmaWarning = sku.warning()

            XCTAssertTrue(pharmaWarning.target.isVisible)
            XCTAssertEqual(pharmaWarning.target.text, disclaimerLabel)
        }
    }

}
