import XCTest
import Snapshots

final class DealerSaleCardSteps: BaseSteps {
    func onDealerSaleCardScreen() -> DealerSaleCardScreen {
        return self.baseScreen.on(screen: DealerSaleCardScreen.self)
    }

    @discardableResult
    func shouldSeeCommonContent() -> Self {
        Step("Проверяем что есть основные элементы карточки: шапка, галерея, дата размещения") {
            let screen = self.onDealerSaleCardScreen()
            screen.scrollTo(element: screen.header, maxSwipes: 3)
            screen.header.shouldExist()

            screen.scrollTo(element: screen.gallery, maxSwipes: 3)
            screen.gallery.shouldExist()

            screen.scrollTo(element: screen.days, maxSwipes: 3)
            screen.days.shouldExist()
        }

        return self
    }

    @discardableResult
    func shouldSeeAllBlocks() -> Self {
        Step("Проверяем, что видны все требуемые блоки в карточки: шапка, галерея, дата разменения, хар-ки, описание, отчёт") {
            let screen = self.onDealerSaleCardScreen()
            screen.scrollTo(element: screen.header)
            screen.scrollTo(element: screen.gallery)
            screen.scrollTo(element: screen.days)
            screen.scrollTo(element: screen.offerDescription)
            screen.scrollTo(element: screen.characteristics)
            screen.scrollTo(element: screen.buyCarReportButton)
        }

        return self
    }

    @discardableResult
    func shouldSeeEditButton(isVisible: Bool = true) -> Self {
        Step("Проверяем, что есть кнопка 'Редактировать' внизу") {
            self.onDealerSaleCardScreen().editButton.shouldExist()
        }

        return self
    }

    @discardableResult
    func shouldSeeDeleteButton(isVisible: Bool = true) -> Self {
        Step("Проверяем, что есть кнопка 'Удалить' внизу") {
            self.onDealerSaleCardScreen().deleteButton.shouldExist()
        }

        return self
    }

    @discardableResult
    func shouldNotSeeEditButton(isVisible: Bool = true) -> Self {
        Step("Проверяем, что нет кнопки 'Редактировать' внизу") {
            self.onDealerSaleCardScreen().editButton.shouldNotExist()
        }

        return self
    }

    @discardableResult
    func shouldSeeActivateButton() -> Self {
        Step("Проверяем, что есть кнопка 'Активировать' внизу") {
            self.onDealerSaleCardScreen().activationButton.shouldExist()
        }

        return self
    }

    @discardableResult
    func checkSaleCardTopBlocksSnapshot(identifier: String) -> Self {
        Step("Делаем скриншот блоков сверху (шапка, галерея, дата) и сравниваем с '\(identifier)'") {
            let screen = self.onDealerSaleCardScreen()

            let fromElement: XCUIElement
            if screen.banReasons.isFullyVisible() {
                fromElement = screen.banReasons
            } else {
                fromElement = screen.header
            }

            let snapshot = Snapshot.screenshotCollectionView(
                fromCell: fromElement,
                toCell: screen.days,
                windowInsets: DealerSaleCardScreen.insetsWithoutFloatingButton
            )
            Snapshot.compareWithSnapshot(image: snapshot, identifier: identifier)
        }

        return self
    }

    @discardableResult
    func checkImagesPlaceholderSnapshot() -> Self {
        Step("Проверяем снепшот с плейсхолдером на галерее, когда нет возможности редактировать") {
            let gallery = self.onDealerSaleCardScreen().galleryPlaceholder
            self.onDealerSaleCardScreen().scrollTo(element: gallery, windowInsets: DealerSaleCardScreen.insetsWithoutFloatingButton)
            Snapshot.compareWithSnapshot(
                image: gallery.waitAndScreenshot().image,
                identifier: "dealer_sale_card_no_images_not_edit_placeholder"
            )
        }

        return self
    }

    @discardableResult
    func waitForLoading() -> Self {
        Step("Ждем, пока загрузится экран оффера") {
            self.onDealerSaleCardScreen().refreshingControl.shouldNotExist(timeout: 5.0)
        }

        return self
    }

    @discardableResult
    func tapOnUpdatePriceButton() -> DealerUpdatePriceSteps {
        Step("Тапаем на кнопку редактирования цены") {
            self.onDealerSaleCardScreen().updatePriceButton.tap()
        }

        return DealerUpdatePriceSteps(context: self.context)
    }

    @discardableResult
    func checkPrice(value: String) -> Self {
        Step("Проверяем, что цена равна '\(value)'") {
            _ = self.wait(for: 3)
            let text = self.onDealerSaleCardScreen().priceLabel.label
            XCTAssert(text == value, "Неверное значение: ожидается '\(value)', найдено '\(text)'")
        }

        return self
    }

    @discardableResult
    func tapOnMoreButton() -> DealerOfferActionSteps {
        Step("Тапаем на '...' в навбаре") {
            self.onDealerSaleCardScreen().moreButton.tap()
        }

        return DealerOfferActionSteps(context: self.context)
    }

    @discardableResult
    func tapOnActivationButton() -> Self {
        Step("Тапаем на кнопку 'Активировать' внизу") {
            self.onDealerSaleCardScreen().activationButton.tap()
        }

        return self
    }

    @discardableResult
    func tapOnEditButton() -> DealerFormSteps {
        Step("Тапаем на кноппку 'Редактировать' внизу") {
            self.onDealerSaleCardScreen().editButton.tap()
        }

        return DealerFormSteps(context: self.context)
    }

    @discardableResult
    func tapOnDeleteButton() -> DealerOfferActionSteps {
        Step("Тапаем на кноппку 'Удалить' внизу") {
            self.onDealerSaleCardScreen().deleteButton.tap()
        }

        return DealerOfferActionSteps(context: self.context)
    }

    @discardableResult
    func tapOnEmptyImagesPlaceholder() -> DealerFormSteps {
        Step("Тапаем на плейсхолдер на галерее, когда можно редактировать") {
            self.onDealerSaleCardScreen().galleryPlaceholder.tap()
        }

        return DealerFormSteps(context: self.context)
    }

    @discardableResult
    func shouldNotSeeActionControls() -> Self {
        Step("Проверяем, что нет контролов редактирования: '...', кнопка редактирования, кнопка обновления цены") {
            let screen = self.onDealerSaleCardScreen()
            screen.updatePriceButton.shouldNotExist()
            screen.moreButton.shouldNotExist()
            screen.editButton.shouldNotExist()
        }

        return self
    }

    @discardableResult
    func tapOnBackButton() -> DealerCabinetSteps {
        Step("Тапаем кнопку возврата в навбаре") {
            self.onDealerSaleCardScreen().backButton.tap()
        }

        return DealerCabinetSteps(context: self.context)
    }

    @discardableResult
    func scrollToChart(chart: XCUIElement, description: String) -> Self {
        Step("Скроллим до графика '\(description)'") {
            self.onDealerSaleCardScreen().scrollTo(element: chart, windowInsets: DealerSaleCardScreen.insetsWithoutFloatingButton)
        }

        return self
    }

    @discardableResult
    func scrollToCounters() -> Self {
        Step("Скроллим до счетчиков") {
            let element = self.onDealerSaleCardScreen().counters
            self.onDealerSaleCardScreen().scrollTo(element: element, windowInsets: DealerSaleCardScreen.insetsWithoutFloatingButton)
        }

        return self
    }

    @discardableResult
    func scrollToCarReport(inset: Bool = true) -> Self {
        Step("Скроллим к отчёту") {
            let screen = self.onDealerSaleCardScreen()
            screen.scrollTo(
                element: screen.buyCarReportButton,
                windowInsets: inset ? UIEdgeInsets(top: 0, left: 0, bottom: 110, right: 0) : .zero
            )
        }

        return self
    }

    @discardableResult
    func openFreeCarReport() -> Self {
        Step("Открываем бесплатный отчет из превью") {
            let screen = self.onDealerSaleCardScreen()
            screen.scrollTo(
                element: screen.buyCarReportButton,
                windowInsets: DealerSaleCardScreen.insetsWithoutFloatingButton
            )

            Step("Тапаем `Смотреть бесплатный отчёт`") {
                let btn = screen.freeCarReport
                Step("Ищем кнопку") {
                    btn.shouldExist()
                }
                btn.tap()
            }
        }

        return self
    }

    @discardableResult
    func tapBuyFullReport() -> Self {
        Step("Тапаем купить полный отчёт") {
            let screen = self.onDealerSaleCardScreen()
            let button = screen.buyCarReportButton
            Step("Ищем кнопку") {
                button.shouldExist()
            }
            button.tap()
        }

        return self
    }

    @discardableResult
    func tapConfirmBuyFullReport() -> Self {
        Step("Подтверждаем покупку отчёта") {
            let screen = self.onDealerSaleCardScreen()
            let button = screen.alertButton(text: "Оплатить 299 ₽")
            Step("Ищем кнопку") {
                button.shouldExist()
            }
            button.tap()
        }

        return self
    }

    @discardableResult
    func checkChartSnapshot(chart: XCUIElement, identifier: SnapshotIdentifier, withTap: Bool) -> Self {
        Step("Делаем скриншот графика и сравниваем снепшот '\(identifier)'" + (withTap ? ", предварительно тапаем в столбец" : "")) {
            if withTap {
                let coordinate = chart.coordinate(withNormalizedOffset: CGVector(dx: 0.67, dy: 0.5))
                coordinate.press(forDuration: 1.0)
            }

            Snapshot.compareWithSnapshot(
                image: chart.waitAndScreenshot(timeout: 0.0).image,
                identifier: identifier
            )
        }

        return self
    }

    @discardableResult
    func checkCountersSnapshot(identifier: String) -> Self {
        Step("Делаем скриншот счетчиков и сравниваем снепшот '\(identifier)'") {
            let screenshot = self.onDealerSaleCardScreen().counters.waitAndScreenshot()
            Snapshot.compareWithSnapshot(
                image: screenshot.image,
                identifier: identifier
            )
        }

        return self
    }

    @discardableResult
    func checkCarReportSnapshot(carReport: XCUIElement, identifier: String) -> Self {
        Step("Проверяем снепшот превью отчёта. Сравниваем с `\(identifier)`") {
            Snapshot.compareWithSnapshot(
                image: carReport.waitAndScreenshot(timeout: 0.0).image,
                identifier: identifier,
                overallTolerance: 0.02
            )
        }

        return self
    }

    @discardableResult
    func snapshotCarReportBuyBblock(identifier: String) -> Self {
        Step("Проверяем снепшот кнопок покупки отчёта. Сравниваем с `\(identifier)`") {
            let buyButtonCell = onDealerSaleCardScreen().find(by: "backend_layout_cell_2").firstMatch
            let image = Snapshot.screenshotCollectionView(
                fromCell: onDealerSaleCardScreen().find(by: "backend_layout_cell").firstMatch,
                toCell: buyButtonCell
            )
            Snapshot.compareWithSnapshot(
                image: image,
                identifier: identifier,
                overallTolerance: 0.02
            )
        }

        return self
    }

    @discardableResult
    func snapshotActivityHUD(identifier: String) -> Self {
        Step("Проверяем снепшот Activity HUD. Сравниваем с `\(identifier)`") {
            let hud = onDealerSaleCardScreen().find(by: "ActivityHUD").firstMatch
            Snapshot.compareWithSnapshot(
                image: hud.waitAndScreenshot().image,
                identifier: identifier,
                overallTolerance: 0.02
            )
        }

        return self
    }
}
