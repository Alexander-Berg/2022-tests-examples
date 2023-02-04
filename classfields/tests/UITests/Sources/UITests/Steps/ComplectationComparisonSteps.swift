import XCTest
import Snapshots

final class ComplectationComparisonSteps: ComparisonSteps {
    func onComplectationComparisonScreen() -> ComplectationComparisonScreen {
        return self.baseScreen.on(screen: ComplectationComparisonScreen.self)
    }

    @discardableResult
    func shouldSeeComplectationHeader(name: String) -> Self {
        Step("Проверяем, что видна комплектация '\(name)' в шапке") {
            self.onComplectationComparisonScreen().complectationHeader(name: name).shouldBeVisible()
        }

        return self
    }

    @discardableResult
    func checkScreenshotOfOption(optionName: String, columnIndex: Int, identifier: String) -> Self {
        Step("Делаем скриншот ячейки для опции '\(optionName)' в колонке #\(columnIndex)") {
            let screenshot = self.onComplectationComparisonScreen().optionMark(name: optionName, column: columnIndex)
                .waitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot.image, identifier: identifier)
        }

        return self
    }

    @discardableResult
    func tapOnComplectationHeader(name: String) -> Self {
        Step("Тапаем на комплектацию '\(name)' в шапке") {
            self.onComplectationComparisonScreen().complectationHeader(name: name).tap()
        }

        return self
    }

    @discardableResult
    func scrollTo(optionName: String, columnIndex: Int, windowInsets: UIEdgeInsets = .zero) -> Self {
        Step("Проскроллим вниз до опции '\(optionName)' в колонке #\(columnIndex)") {
            let element = self.onComplectationComparisonScreen().optionMark(name: optionName, column: columnIndex)
            self.onComplectationComparisonScreen().scrollTo(element: element, maxSwipes: 5, windowInsets: windowInsets)
        }

        return self
    }

    @discardableResult
    func tapOnOption(optionName: String, columnIndex: Int) -> Self {
        Step("Тапаем на ячейку для опции '\(optionName)' в колонке #\(columnIndex)") {
            self.onComplectationComparisonScreen().optionMark(name: optionName, column: columnIndex).tap()
        }

        return self
    }

    @discardableResult
    func shouldSeeDisclaimerPopup() -> Self {
        Step("Проверяем, что показан попап с применимостью") {
            self.onComplectationComparisonScreen().disclaimerPopupTitle.shouldExist()
        }

        return self
    }
}
