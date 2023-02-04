import XCTest
import Snapshots

final class DealerVINSearchSteps: BaseSteps {
    func onDealerVINSearchScreen() -> DealerVINSearchScreen {
        return self.baseScreen.on(screen: DealerVINSearchScreen.self)
    }

    @discardableResult
    func shouldSeeVINSearchScreen() -> Self {
        Step("Проверяем, что есть серчбар и кнопка отмены (закрытия)") {
            let screen = self.onDealerVINSearchScreen()
            screen.searchBar.shouldExist()
            screen.cancelButton.shouldExist()
        }

        return self
    }

    @discardableResult
    func shouldSeeNotFoundPlaceholder() -> Self {
        Step("Проверяем, что есть плейсхолер 'не найдено'") {
            let screen = self.onDealerVINSearchScreen()
            screen.notFoundPlaceholder.shouldExist(timeout: 3.0)
            screen.resetQueryButton.shouldExist(timeout: 3.0)
        }

        return self
    }

    @discardableResult
    func tapOnResetQueryOnPlaceholder() -> Self {
        Step("Тапаем на сброс в плейсхолдере") {
            self.onDealerVINSearchScreen().resetQueryButton.tap()
        }

        return self
    }

    @discardableResult
    func tapOnSearchBarAndType(text: String, moveCursorToEnd: Bool = false) -> Self {
        Step("Тапаем на серчбар и вводим текст \"\(text)\"") {
            let searchBar = self.onDealerVINSearchScreen().searchBar
            searchBar.tap()

            if moveCursorToEnd {
                let moveCursorPosition = searchBar.coordinate(withNormalizedOffset: CGVector(dx: 0.6, dy: 0.6))
                moveCursorPosition.tap()
            }
            searchBar.typeText(text)
        }

        return self
    }

    @discardableResult
    func tapOnCancelButton() -> DealerCabinetSteps {
        Step("Тапаем на 'Отмена'") {
            self.onDealerVINSearchScreen().cancelButton.tap()
        }

        return DealerCabinetSteps(context: self.context)
    }

    @discardableResult
    func scrollTo(suggest: String) -> Self {
        Step("Скроллим до саджеста '\(suggest)'") {
            _ = self.onDealerVINSearchScreen().scrollToElementWith(text: suggest)
        }

        return self
    }

    @discardableResult
    func tapOnVINSuggest(text: String) -> Self {
        Step("Тапаем на саджест '\(text)'") {
            self.onDealerVINSearchScreen().collectionView.cells.staticTexts[text].tap()
        }

        return self
    }

    @discardableResult
    func shouldSeeOnlyOneSuggest() -> Self {
        Step("Проверяем, что в саджесте только один элемент") {
            let cellsCount = self.onDealerVINSearchScreen().collectionView.cells.count
            XCTAssertEqual(cellsCount, 1, "Список содержит \(cellsCount) ячеек, ожидается 1 ячейка")
        }

        return self
    }
}
