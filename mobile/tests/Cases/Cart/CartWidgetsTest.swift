import MarketUITestMocks
import XCTest

final class CartWidgetsTest: LocalMockTestCase {
    /*
     Тест отображения сниппетов и добавления товара в корзину
     вынесен в CartRecommendationsWidgetAdapterTest для сокращения времени прогона тестов
     */
    func testCartScrollboxWidgetsAppearance() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3022")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3023")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3216")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3217")
        Allure.addEpic("Корзина с товарами")
        Allure.addFeature("Виджеты")
        Allure.addTitle("Проверяем отображение скроллбоксов в корзине")

        // Вспомогательные функции

        var cartPage: CartPage!
        var defaultState = DefaultState()

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем startup для получения эксперимента express_fix_control (компактное саммари)".ybm_run { _ in
            defaultState.setExperiments(experiments: [.expressFixControl])
            stateManager?.setState(newState: defaultState)
        }

        "Мокаем ручки".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CartSet_Reasons")
        }

        "Открываем корзину".ybm_run { _ in
            cartPage = goToCart()
            wait(forVisibilityOf: cartPage.compactSummary.element)
        }

        "Проверяем виджет \"Не забыть купить\"".ybm_run { _ in
            let purchasedWidget = cartPage.sectionHeaderTitle(at: 0)
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: purchasedWidget)
            XCTAssertEqual(purchasedWidget.label, "Не забыть купить")
        }

        "Проверяем виджет \"C этим товаром часто покупают\"".ybm_run { _ in
            let accessoriesWidget = cartPage.sectionHeaderTitle(at: 1)
            cartPage.element.ybm_swipeCollectionView(toFullyReveal: accessoriesWidget)
            XCTAssertEqual(accessoriesWidget.label, "С этими товарами часто покупают")
        }
    }

    // MARK: - Private

    private func verifySnippets<T: SnippetPage>(snippets: [SnippetInfo], widget: LegacyScrollBoxWidgetPage<T>) {
        widget.enumerateCells(cellCount: snippets.count) { snippet, indexPath in
            let snippetInfo = snippets[indexPath.item]

            if snippetInfo.hasGift {
                XCTAssertTrue(snippet.giftView.imageView.element.isVisible)
            } else {
                XCTAssertFalse(snippet.giftView.imageView.element.isVisible)
            }

            if snippetInfo.hasCheapestAsGift {
                XCTAssertTrue(snippet.cheapestAsGiftView.imageView.isVisible)
            } else {
                XCTAssertFalse(snippet.cheapestAsGiftView.imageView.isVisible)
            }
        }
    }

    private func generateSnippetsInfo() -> [SnippetInfo] {
        [
            SnippetInfo(
                price: "",
                discountPercent: nil,
                oldPrice: nil,
                starsValue: "",
                reviewsCountLabel: "",
                skuName: "",
                isInCart: false,
                reasonsText: nil,
                isSoldOut: false,
                hasGift: true,
                hasCheapestAsGift: false
            ),
            SnippetInfo.makeEmpty(),
            SnippetInfo(
                price: "",
                discountPercent: nil,
                oldPrice: nil,
                starsValue: "",
                reviewsCountLabel: "",
                skuName: "",
                isInCart: false,
                reasonsText: nil,
                isSoldOut: false,
                hasGift: false,
                hasCheapestAsGift: true
            ),
            SnippetInfo.makeEmpty()
        ]
    }
}
