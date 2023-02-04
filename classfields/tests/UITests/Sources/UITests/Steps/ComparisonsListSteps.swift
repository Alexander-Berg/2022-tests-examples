import XCTest
import Snapshots

final class ComparisonsListSteps: BaseSteps {
    func onComparisonsListScreen() -> ComparisonsListScreen {
        self.baseScreen.on(screen: ComparisonsListScreen.self)
    }

    @discardableResult
    func checkScreenshotOfOffers(identifier: String) -> Self {
        step("Делаем скриншот плашки с объявлениями и сравниваем со снепшотом '\(identifier)'") {
            let screenshot = self.onComparisonsListScreen().offersComparison.waitAndScreenshot().image
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
    }

    @discardableResult
    func tapOnListingButton() -> Self {
        step("Тап на кнопку открытия листинга") {
            self.onComparisonsListScreen().openListingButton.tap()
        }
    }

    @discardableResult
    func tapOnAddOfferToComparisonButton() -> Self {
        step("Тап на кнопку добавления оффера в сравнение") {
            self.onComparisonsListScreen().addOfferToComparisonButton.tap()
        }
    }

    @discardableResult
    func shouldSeeUnauthorizedPlaceholder() -> Self {
        step("Проверяем, что видна кнопка 'Войти' на плейсхолдере") {
            self.onComparisonsListScreen().loginButton.shouldExist()
        }
    }

    @discardableResult
    func checkScreenshotOfEmptyOffers(identifier: String) -> Self {
        step("Делаем скриншот пустой плашки офферов и сравниваем со снепшотом '\(identifier)'") {
            let screenshot = self.onComparisonsListScreen().emptyComparison.waitAndScreenshot().image
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
    }

    @discardableResult
    func checkScreenshotOfEmptyOffersFromFavorites(identifier: String) -> Self {
        step("Делаем скриншот плашки офферов из избранного и сравниваем со снепшотом '\(identifier)'") {
            let screenshot = self.onComparisonsListScreen().offersFromFavoritesComparison.waitAndScreenshot().image
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
    }

    @discardableResult
    func tapEmptyOffersFromFavorites() -> Self {
        step("Тапаем на плашку добавления офферов из избранного") {
            self.onComparisonsListScreen().offersFromFavoritesComparison.tap()
        }
    }

    @discardableResult
    func tapOffersComparison() -> ComparisonSteps {
        step("Тапаем на плашку сравнения офферов") {
            self.onComparisonsListScreen().offersComparison.tap()
        }
        .as(ComparisonSteps.self)
    }

    @discardableResult
    func waitForLoading() -> Self {
        step("Ждем, пока загрузятся сравнения") {
            self.onComparisonsListScreen().refreshingControl.shouldNotExist(timeout: 5.0)
        }
    }
}
