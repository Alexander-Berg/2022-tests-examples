import MarketUITestMocks
import XCTest

final class SKUCardModelMLOpinionsTest: LocalMockTestCase {

    func testMLOpinionsCardModel() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4847")
        Allure.addEpic("КМ")
        Allure.addFeature("ML отзывы")
        Allure.addTitle("Проверяем корректное отображение ML отзывов")

        var sku: SKUPage!

        "Мокаем ML отзыв".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCard_MLOpinions")
        }

        "Настраиваем стейт".run {
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Свайпаем до МL отзыва и проверяем текст".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.reviewSummaryPros, inset: sku.stickyViewInset)

            XCTAssertEqual(
                sku.reviewSummaryPros.text,
                "«Скорость, тишина работы, качество картинки»\n«Дизайн»\n«С хорошими внутренними характеристиками»\n«Тихая»"
            )
            XCTAssertEqual(
                sku.reviewSummaryContra.text,
                "«Мало игр»\n«Usb для игры с зарядкой от плойки»\n«Жесткий диск маловат»\n«Большая»"
            )
        }
    }
}
