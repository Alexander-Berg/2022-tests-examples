import XCTest

class SearchHistoryTest: LocalMockTestCase {

    func testBasic() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-975")
        Allure.addEpic("Поиск")
        Allure.addFeature("История поиска")
        Allure.addTitle("Базовый тест")

        var feedPage: FeedPage!
        let search = "red"

        "Мокаем историю поиска".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SearchSet_History")
        }

        "Открываем приложение, делаем поиск".ybm_run { _ in
            feedPage = goToFeed(with: search)
        }

        "Переход обратно на КМ".ybm_run { _ in
            feedPage.navigationBar.backButton.tap()
            wait(forVisibilityOf: MordaPage.current.element)
        }

        "Переход в поисковую строку: проверка содержимого поискового экрана".ybm_run { _ in
            let searchPage = MordaPage.current.searchButton.tap()
            searchPage.navigationBar.searchTextField.tap()

            // пустая поисковая строка
            XCTAssertTrue(searchPage.navigationBar.searchTextField.label.isEmpty)
            XCTAssertEqual(searchPage.navigationBar.searchTextField.placeholderValue, "Я хочу купить...")

            // саджест из предыдущего поиска
            XCTAssertEqual(searchPage.suggestsCells.count, 1)
            XCTAssertEqual(searchPage.suggestsCells[0].text.label, search)

            // кнопка очистить историю и стрелка назад
            XCTAssertEqual(searchPage.clearHistoryButton.label, "Очистить историю")
            XCTAssertTrue(searchPage.navigationBar.backButton.isVisible)
        }
    }

}
