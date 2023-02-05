import MarketUITestMocks
import Metrics
import XCTest

class SearchMetricaTest: LocalMockTestCase {

    func testSearchMetrica() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3111")
        Allure.addEpic("ПОИСКОВАЯ СТРОКА")
        Allure.addFeature("Метрика")
        Allure.addTitle("Метрика на саджесты")

        var root: RootPage!
        var morda: MordaPage!
        var search: SearchPage!
        let searchText = "Синий"
        let chipsText = "синий трактор"

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SearchSet_Metric")
        }

        "Настраиваем стейт".run {
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(offer: FAPIOffer.default, model: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем поисковый экран".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
            morda = root.tabBar.mordaPage
            wait(forVisibilityOf: morda.searchButton.element)
            search = morda.searchButton.tap()
        }

        "Вводим текст \"Синий\"".ybm_run { _ in
            MetricRecorder.clear()

            search.navigationBar.searchTextField.tap()
            search.navigationBar.searchTextField.typeText(searchText)

            ybm_wait(forFulfillmentOf: { search.suggestsCells.first?.element.isVisible ?? false })
        }

        "Проверяем события метрики".ybm_run { _ in
            let suggestEvent = MetricRecorder.events(from: .appmetrica).first { event in
                guard
                    event.name == "SEARCH-BAR_SEARCH-FORM_SUGGEST_VISIBLE",
                    let suggests = event.parameters["suggests"] as? [[String: Any]]
                else { return false }

                let suggestTypes = Set<String>(suggests.compactMap { $0["suggestType"] as? String })

                return event.parameters["text"] == searchText
                    && suggests.count == 6
                    && (suggests[ble_safe: 0]?["suggestName"] as? String) == "Каталки и качалки для малышей"
                    && suggestTypes == ["model", "category"]
            }

            let chipsEvent = MetricRecorder.events(from: .appmetrica).first { event in
                guard
                    event.name == "SEARCH-BAR_SEARCH-FORM_CHIPS_VISIBLE",
                    let chips = event.parameters["chips"] as? [[String: Any]],
                    let chipsCompletion = chips[ble_safe: 0]?["chipsCompletion"] as? String,
                    let chipsValue = chips[ble_safe: 0]?["chipsValue"] as? String
                else { return false }

                return event.parameters["text"] == searchText
                    && chips.count == 4
                    && chipsCompletion == "трактор"
                    && chipsValue == "синий трактор"
            }

            ybm_wait(forFulfillmentOf: {
                suggestEvent != nil && chipsEvent != nil
            })
            XCTAssertNotNil(suggestEvent)
            XCTAssertNotNil(chipsEvent)
        }

        "Нажимаем на первый саджест".ybm_run { _ in
            MetricRecorder.clear()

            let skuPage = search.suggestsCells[2].tap()
            ybm_wait(forFulfillmentOf: { skuPage.gallery.element.isVisible })
        }

        "Проверяем события метрики".ybm_run { _ in
            ybm_wait {
                MetricRecorder.events(from: .appmetrica)
                    .with(name: "SEARCH-BAR_SEARCH-FORM_SUGGEST_NAVIGATE")
                    .with(params: [
                        "text": searchText,
                        "suggestText": "Каталка-игрушка Bochart Синий трактор (ККМ05)",
                        "suggestType": "model"
                    ])
                    .isNotEmpty
            }
        }

        "Заново открываем экран поиска и вводим запрос \"Синий\"".ybm_run { _ in
            NavigationBarPage.current.backButton.tap()
            wait(forVisibilityOf: morda.searchButton.element)
            search = morda.searchButton.tap()
            wait(forVisibilityOf: search.navigationBar.searchTextField)
            search.navigationBar.searchTextField.typeText(searchText)

            let cell = search.element.cells.matching(identifier: chipsText).firstMatch
            wait(forVisibilityOf: cell)

            cell.tap()
        }

        "Проверяем события метрики".ybm_run { _ in
            ybm_wait {
                MetricRecorder.events(from: .appmetrica)
                    .with(name: "SEARCH-BAR_SEARCH-FORM_CHIPS_NAVIGATE")
                    .with(params: [
                        "text": searchText,
                        "chipsValue": chipsText,
                        "chipsCompletion": "трактор"
                    ])
                    .isNotEmpty
            }
        }
    }

}
