import AutoMate
import MarketUITestMocks
import XCTest

final class SKUCartModelSupplierInfoTest: LocalMockTestCase {

    func testSkuSelfEmployedSupplierInfo() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6200")
        Allure.addEpic("КМ")
        Allure.addFeature("Самозанятые")
        Allure.addTitle("Проверяем отображение юр. информации о самозанятом на КМ")

        var skuState = SKUInfoState()

        "Настраиваем FT".ybm_run { _ in
            enable(
                toggles:
                FeatureNames.newSupplierAndTrustInfo
            )
        }

        "Настраиваем стейт".run {
            skuState.setSkuInfoState(with: .default)
            skuState.setSupplierInfoById(suppliers: [.selfEmployed])
            stateManager?.setState(newState: skuState)
        }

        var skuPage: SKUPage!
        var supplierPage: SupplierTrustInfoLegalPage!

        "Открываем SKU".run {
            skuPage = goToDefaultSKUPage()
        }

        "Скроллим до байбокса, и кнопки i в байбоксе".ybm_run { _ in
            wait(forVisibilityOf: skuPage.collectionView)
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.supplierInfo.element)
            wait(forVisibilityOf: skuPage.supplierInfo.element)
            wait(forVisibilityOf: skuPage.supplierInfo.infoButton.element)
        }

        "Тапаем по кнопке supplierInfo байбокса".ybm_run { _ in
            supplierPage = skuPage.supplierInfo.infoButton.tap()
        }

        "Проверяем наличие и совпадение текста в supplierInfo попапе".ybm_run { _ in
            wait(forVisibilityOf: supplierPage.legalInfoText)
            XCTAssertEqual(supplierPage.legalInfoText.text, SupplierInfo.selfEmployed.makeLegalInfoString())
        }
    }
}
