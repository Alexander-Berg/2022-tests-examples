import Foundation
import XCTest
import Snapshots

final class GarageCardSteps: BaseSteps {
    @discardableResult
    func shouldSeeCard() -> Self {
        step("Проверяем, что показали карточку") {
            self.onGarageCardScreen().editButton.shouldExist(timeout: 5)
        }
    }

    func shouldSeeRecalls() -> Self {
        step("Проверяем, что показали записи об отзывных компаниях") {
            self.onGarageCardScreen().recallsButton.shouldExist(timeout: 5)
        }
    }

    func shoulSeeProvenOwner() -> Self {
        step("Проверяем, что показали ячейку проверенного собственника в заголовке") {
            self.onGarageCardScreen().provenOwnerHeaderButton.shouldExist(timeout: 5)
        }
    }

    func shouldSeePriceStats() -> Self {
        step("Проверяем, что показали блок средней цены") {
            self.onGarageCardScreen().priceStatsButton.shouldExist(timeout: 5)
        }
    }

    @discardableResult
    func tapOnBackButton() -> Self {
        step("Тапаем на кнопку Назад") {
            self.onGarageCardScreen().backButton.tap()
        }
    }

    @discardableResult
    func tapOnEditButton() -> GarageFormSteps {
        step("Тапаем на кнопку Изменить") {
            let editButton = self.onGarageCardScreen().editButton
            editButton.shouldExist(timeout: 5, message: "Кнопка 'Изменить' отсутствует")
            editButton.tap()
        }
        .as(GarageFormSteps.self)
    }

    @discardableResult
    func tapOnPriceRow() -> Self {
        step("Тапаем на строку со стоимостью") {
            let editButton = self.onGarageCardScreen().priceRow
            editButton.shouldExist(timeout: 5, message: "Строка стоимости отсутствует")
            editButton.tap()
        }
    }

    @discardableResult
    func tapOnContentsAndCheckScroll(contentsTitle: String, blockTitle: String) -> Self {
        step("Тапаем в оглавлении и проверяем, что подскроллили к блоку '\(blockTitle)'") {
            self.onGarageCardScreen().findText(contentsTitle).tap()
            self.onGarageCardScreen().findText(blockTitle).shouldBeVisible()
        }
    }

    @discardableResult
    func tapOnContentsItem(description: String, title: String) -> Self {
        step("Тапаем в оглавлении на '\(description)'") {
            self.onGarageCardScreen().findText(title).tap()
        }
    }

    @discardableResult
    func checkPriceStats() -> Self {
        step("Скриншотим и проверяем блок с диаграммой стоимости") {
            let screenshot = self.onGarageCardScreen().priceStatsBlock.waitAndScreenshot()
            Snapshot.compareWithSnapshot(
                image: screenshot.image,
                identifier: .make()
            )
        }
    }

    @discardableResult
    func checkCheapening() -> Self {
        step("Скриншотим и проверяем блок с графиком удешевления") {
            let screenshot = onGarageCardScreen().cheapeningBlock.waitAndScreenshot()
            Snapshot.compareWithSnapshot(
                image: screenshot.image,
                identifier: .make()
            )
        }
    }

    @discardableResult
    func tapBuyReport() -> PaymentOptionsSteps<GarageCardSteps> {
        Step("Тапаем в \"Купить полный отчет\"") {
            onGarageCardScreen().scrollTo(element: onGarageCardScreen().buyReportButton)
            onGarageCardScreen().buyReportButton.tap()
        }
        return PaymentOptionsSteps(context: context, source: self)
    }

    @discardableResult
    func tapOnCheapeningBlockExchangeLink() -> Self {
        step("Тапаем на ссылку 'К объявлениям'") {
            onGarageCardScreen().priceStatsBlockExchangeLink.tap()
        }
    }

    @discardableResult
    func shouldSeeAddExtraParametersBanner() -> Self {
        step("Проверяем наличие блока 'Рассчитайте стоимость автомобиля'") {
            onGarageCardScreen().calculateCarPriceBanner.shouldExist()
        }
    }

    @discardableResult
    func shouldNotSeeAddExtraParametersBanner() -> Self {
        step("Проверяем отсутствие блока 'Рассчитайте стоимость автомобиля'") {
            onGarageCardScreen().calculateCarPriceBanner.shouldNotExist()
        }
    }

    @discardableResult
    func checkPhoto(withID id: String) -> Self {
        step("Проверка наличия фото \(id) на карточке") {
            onGarageCardScreen().photo(id).shouldExist()
        }
    }

    @discardableResult
    func tapOnAutoServicesBlockItem(_ item: GarageCardScreen.AutoServicesBlockItem) -> Self {
        step("Тапаем на пункт '\(item.rawValue)' в блоке с автосервисами") {
            let element = onGarageCardScreen().find(by: item.rawValue).firstMatch
            onGarageCardScreen().scrollTo(element: element).tap()
        }
    }

    func tapRecalls() -> Self {
        step("Тапаем отзывные (хедер)") {
            onGarageCardScreen().find(by: "header_recalls").firstMatch.tap()
        }
    }

    func tapProvenOwner() -> Self {
        step("Тапаем проверенного собственника (хедер)") {
            onGarageCardScreen().find(by: "proven_owner_header").firstMatch.tap()
        }
    }

    func tapCell(content: String) -> ModalSteps<GarageCardSteps, LayoutPopUpModalScreen> {
        step("Ищем ячейку со строкой '\(content)'") {
            onGarageCardScreen().find(by: content)
                .firstMatch.shouldExist()
                .tap()
        }
        return ModalSteps(context: context, source: self)
    }

    // MARK: - Private

    private func onGarageCardScreen() -> GarageCardScreen {
        self.baseScreen.on(screen: GarageCardScreen.self)
    }
}

final class GarageCardPromoSteps: BaseSteps {
    func onGarageCardPromoScreen() -> GarageCardPromoScreen {
        return self.baseScreen.on(screen: GarageCardPromoScreen.self)
    }

    @discardableResult
    func checkPromoPopup() -> Self {
        Step("Скриншотим и проверяем попап с промо") {
            let screenshot = self.onGarageCardPromoScreen().promoPopup.waitAndScreenshot().image
            Snapshot.compareWithSnapshot(
                image: screenshot.cropping(insets: UIEdgeInsets(top: 0, left: 0, bottom: 34, right: 0)),
                identifier: .make()
            )
        }

        return self
    }

    @discardableResult
    func tapOnActionButton() -> Self {
        Step("Тапаем на кнопку в попапе промки") {
            self.onGarageCardPromoScreen().actionButton.tap()
        }

        return self
    }

    @discardableResult
    func checkPromocodeInPasteboard(promocode: String) -> Self {
        Step("Проверяем, что скопирован промокод '\(promocode)'") {
            let fromPasteboard = UIPasteboard.general.string ?? "<пусто>"
            XCTAssert(promocode == fromPasteboard, "В буфере обмена неверный промокод: \(fromPasteboard) вместо \(promocode)")
        }

        return self
    }
}
