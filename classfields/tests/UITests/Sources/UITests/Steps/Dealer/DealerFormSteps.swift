import XCTest
import Snapshots

final class DealerFormSteps: BaseSteps {
    enum Category {
        case auto, moto, trucks
    }

    @discardableResult
    func close() -> Self {
        Step("Закрываем форму размещения") {
            let screen = self.onDealerFormScreen()
            let button = screen.closeButton

            Step("Ищем кнопку закрыть") {
                button.shouldExist()
            }

            button.tap()
        }

        return self
    }

    // MARK: - Actions

    @discardableResult
    func waitForLoading() -> Self {
        Step("Ждем, пока загрузится экран формы добавления") {
            self.onDealerFormScreen().refreshingControl.shouldNotExist()
        }

        return self
    }

    @discardableResult
    func scrollToPublish() -> Self {
        Step("Пытаемся доскроллить до кнопки `Опубликовать`") {
            let form = self.onDealerFormScreen()

            form.scrollableElement.swipe(.up, while: { (numSwipes) -> Bool in
                if form.publishHiddenButton.exists || numSwipes >= 7 {
                    return false
                }
                return true
            })
        }

        return self
    }

    @discardableResult
    func scrollToPublishHidden() -> Self {
        Step("Пытаемся доскроллить до кнопки `Не публиковать сразу`") {
            let form = self.onDealerFormScreen()
            form.scrollableElement.scrollTo(element: form.publishHiddenButton, swipeDirection: .up, maxSwipes: 8, windowInsets: .init(top: 0, left: 0, bottom: 50, right: 0))
        }

        return self
    }

    @discardableResult
    func checkLicensePlate(plate: String, region: String) -> Self {
        Step("Проверяем гос. номер, `\(plate) \(region)`") {
            let form = self.onDealerFormScreen()
            let govNumber = form.govNumberInputField
            govNumber.staticTexts.matching(identifier: plate).firstMatch.shouldExist()
            govNumber.staticTexts.matching(identifier: region).firstMatch.shouldExist()
        }

        return self
    }

    @discardableResult
    func checkForFormScreen() -> Self {
        Step("Проверяем, что видим экран с формой размещения") {
            self.onDealerFormScreen().find(by: "Объявление").firstMatch.shouldExist()
        }

        return self
    }

    @discardableResult
    func checkNoPickerWithSnapshot(element: XCUIElement, identifier: String) -> Self {
        Step("Проверяем, что не открылся пикер. Сравниваем со скриншотом `\(identifier)`") {
            Snapshot.compareWithSnapshot(
                image: element.screenshot().image,
                identifier: identifier,
                overallTolerance: 0.02,
                ignoreEdges: UIEdgeInsets(top: 72, left: 0, bottom: 0, right: 0)
            )
        }

        return self
    }

    @discardableResult
    func checkHasNoCategoryPicker(for category: Category) -> Self {
        Step("Ищем пикер категории для `\(category)`") {
            switch category {
            case .auto: XCTFail("Не должны искать пикер категории для авто")
            case .trucks:
                self.app.staticTexts["Грузовики"].shouldNotExist()
            case .moto:
                self.app.staticTexts["Мотовездеходы"].shouldNotExist()
            }
        }

        return self
    }

    @discardableResult
    func checkHasNoNavbarSearch() -> Self {
        Step("Проверяем, что нет серчбара") {
            self.onDealerFormScreen()
                .navbarSearch.shouldNotExist()
        }
        return self
    }

    // MARK: - Screens

    func onDealerFormScreen() -> DealerFormScreen {
        return baseScreen.on(screen: DealerFormScreen.self)
    }

    func onDealerPanoramasScreen() -> DealerInteriorExteriorPanoramaScreen {
        return baseScreen.on(screen: DealerInteriorExteriorPanoramaScreen.self)
    }
}
