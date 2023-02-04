import XCTest
import Snapshots

final class CarReportPreviewSteps: BaseSteps {
    func onCarReportPreviewScreen() -> CarReportPreviewScreen {
        return self.baseScreen.on(screen: CarReportPreviewScreen.self)
    }

    @discardableResult
    func waitForLoading() -> Self {
        Step("Ждем, пока загрузится отчёт") {
            _ = self.onCarReportPreviewScreen().refreshingControl.exists
            self.onCarReportPreviewScreen().refreshingControl.shouldNotExist(timeout: Const.timeout)
        }

        return self
    }

    @discardableResult
    func shouldSeeContent() -> Self {
        Step("Проверяем, что видим контент отчета") {
            self.onCarReportPreviewScreen().firstContentCell.shouldExist(timeout: Const.timeout)
        }

        return self
    }

    @discardableResult
    func tapOnBackButton<Steps: BaseSteps>() -> Steps {
        Step("Тапаем на кнопку возврата назад") {
            self.onCarReportPreviewScreen().backButton.tap()
        }

        return Steps(context: self.context)
    }

    @discardableResult
    func checkSnapshot(view: XCUIElement, identifier: String, ignoreEdges: UIEdgeInsets = .zero) -> Self {
        Step("Проверяем снепшот отчёта. Сравниваем с `\(identifier)`") {
            wait(for: 1)
            Snapshot.compareWithSnapshot(
                image: view.waitAndScreenshot(timeout: 0.0).image,
                identifier: identifier,
                ignoreEdges: ignoreEdges
            )
        }

        return self
    }

    @discardableResult
    func checkSnapshotHasNoBuyPackButton(identifier: String) -> Self {
        Step("Проверяем, что нет кнопки купить пак отчётов. Сравниваем с `\(identifier)`") {
            let screen = self.onCarReportPreviewScreen()
            wait(for: 1)

            let snapshot = Snapshot.screenshotCollectionView(
                fromCell: screen.promoBlock,
                toCell: screen.cell(index: 42)
            )

            Snapshot.compareWithSnapshot(
                image: snapshot,
                identifier: identifier,
                overallTolerance: 0
            )
        }

        return self
    }

    @discardableResult
    func checkSnapshotHasNoBuyButton(cell: XCUIElement, identifier: String) -> Self {
        Step("Проверяем, что нет кнопки купить. Сравниваем с `\(identifier)`") {
            wait(for: 1)

            Snapshot.compareWithSnapshot(
                image: cell.waitAndScreenshot(timeout: 0.0).image,
                identifier: identifier
            )
        }

        return self
    }

    @discardableResult
    func checkHasNoBuyPackageButton() -> Self {
        Step("Проверяем, что нет кнопки купить пакет") {
            let button = self.onCarReportPreviewScreen().buyPackageButton
            Step("Ищем кнопку") {
                button.shouldNotExist()
            }
        }

        return self
    }

    @discardableResult
    func checkHasNoBuyButton() -> Self {
        Step("Проверяем, что нет кнопки купить") {
            let button = self.onCarReportPreviewScreen().buyButton
            Step("Ищем кнопку") {
                button.shouldNotExist()
            }
        }

        return self
    }

    @discardableResult
    func checkHasBuyButton() -> Self {
        Step("Проверяем, что есть кнопка купить") {
            let button = self.onCarReportPreviewScreen().buyButton
            Step("Ищем кнопку") {
                button.shouldExist()
            }
        }

        return self
    }

    @discardableResult
    func scrollToFreeReportBottom() -> Self {
        Step("Скроллим бесплатный отчёт до конца вниз") {
            let screen = self.onCarReportPreviewScreen()
            screen.scrollTo(element: screen.cell(index: 42))
        }

        return self
    }

    @discardableResult
    func scrollToPaidReportBottom() -> Self {
        Step("Скроллим купленный отчёт до конца вниз") {
            let screen = self.onCarReportPreviewScreen()
            screen.scrollTo(element: screen.cell(index: 55))
        }

        return self
    }

    @discardableResult
    func checkModeratorWarning(visible: Bool) -> Self {
        step("Проверяем, что \(visible ? "видим" : "не видим") ворнинг для модератора") {
            let element = self.onCarReportPreviewScreen().findStaticText(by: "Отчёт под модератором")
            if visible {
                element.shouldExist()
            } else {
                element.shouldNotExist()
            }
        }
    }
}
