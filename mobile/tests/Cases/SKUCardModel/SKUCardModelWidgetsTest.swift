import XCTest

final class SKUCardModelWidgetsTest: LocalMockTestCase {
    /*
     Тест отображения сниппетов и добавления товара в корзину
     вынесен в CartRecommendationsWidgetAdapterTest для сокращения времени прогона тестов
     */
    func testBaseWidgetsBehavior() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-930")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3025")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3026")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3219")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3220")
        Allure.addEpic("КМ")
        Allure.addFeature("Виджеты")
        Allure.addTitle("Проверяем что отображаются виджеты похожих товаров")

        var sku: SKUPage!

        "Открываем карточку товара".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        // Вспомогательная функция

        func test(header: XCUIElement, scrollbox: XCUIElement, with title: String) {
            sku.element.ybm_swipeCollectionView(toFullyReveal: header)
            XCTAssertEqual(header.label, title)

            sku.element.ybm_swipeCollectionView(toFullyReveal: scrollbox)
            XCTAssertTrue(scrollbox.isVisible)
        }

        "Проверяем виджет \"С этим товаром смотрят\"".ybm_run { _ in
            test(
                header: sku.analogsTitle,
                scrollbox: sku.analogs,
                with: "С этим товаром смотрят"
            )
        }
    }
}
