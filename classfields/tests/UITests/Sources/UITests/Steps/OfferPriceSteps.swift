import Foundation
import XCTest
import Snapshots

final class OfferPriceSteps: BaseSteps {
    @discardableResult
    func checkPrice(state: OfferPriceScreen.PriceState) -> Self {
        step("Скриншотим и проверяем блок цены: \(state.title)") {
            let screenshot = self.onOfferPriceScreen().findItem(id: "price").waitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot.image, identifier: "price_" + state.id)
        }
    }

    @discardableResult
    func checkDiscounts() -> Self {
        step("Скриншотим и проверяем блок скидок") {
            let screenshot = self.onOfferPriceScreen().findItem(id: "discounts").waitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot.image, identifier: "discounts")
        }
    }

    @discardableResult
    func checkPriceHistory(countOfRecords: Int) -> Self {
        step("Скриншотим и проверяем блок истории цены") {
            let firstCell = self.onOfferPriceScreen().findItem(id: "price_change_0")
            let lastCell = self.onOfferPriceScreen().findItem(id: "price_change_\(countOfRecords - 1)")
            let screenshot = Snapshot.screenshotCollectionView(
                fromCell: firstCell,
                toCell: lastCell,
                windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 106, right: 0),
                timeout: 1.0
            )
            Snapshot.compareWithSnapshot(image: screenshot, identifier: "price_change")
        }
    }

    @discardableResult
    func checkGreatDeal(_ type: OfferPriceScreen.GreatDeal, hasReport: Bool) -> Self {
        step("Скриншотим и проверяем блок грейт-дила '\(type.title)'") {
            let firstCell = self.onOfferPriceScreen().findItem(id: "great_deal_plot")
            let lastCell = self.onOfferPriceScreen().findItem(id: "great_deal_description")
            let screenshot = Snapshot.screenshotCollectionView(
                fromCell: firstCell,
                toCell: lastCell,
                windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 106, right: 0),
                timeout: 1.0
            )

            let snapshotID = type.rawValue + "\(hasReport ? "_with_report" : "")"
            Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotID)

            if hasReport {
                // отдельно проверим, есть ли ссылка на отчёт. на большом скриншоте различие не заметно из-за tolerance
                Snapshot.compareWithSnapshot(
                    image: screenshot,
                    identifier: snapshotID,
                    ignoreEdges: UIEdgeInsets(top: 800, left: 16, bottom: 0, right: 16)
                )
            }
        }
    }

    @discardableResult
    func checkPopup(id: String) -> Self {
        step("Скриншотим и проверяем контент попапа без кнопки") {
            _ = self.tapStatusBar()

            let firstCell = self.onOfferPriceScreen().findItem(id: "price")
            let lastCell = self.onOfferPriceScreen().findItem(id: "bottom_space")
            let screenshot = Snapshot.screenshotCollectionView(
                fromCell: firstCell,
                toCell: lastCell,
                windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 90, right: 0),
                timeout: 1.0
            )
            Snapshot.compareWithSnapshot(image: screenshot, identifier: id)
        }
    }

    @discardableResult
    func tapOnActionButton(_ type: OfferPriceScreen.ActionButton) -> Self {
        step("Тапаем на кнопку '\(type.rawValue)'") {
            self.onOfferPriceScreen().actionButton(type).tap()
        }
    }

    @discardableResult
    func checkButton(_ actionButton: OfferPriceScreen.ActionButton) -> Self {
        step("Скриншотим и проверяем кнопку '\(actionButton.rawValue)'") {
            let screenshot = onOfferPriceScreen().actionButton(actionButton).screenshot()
            Snapshot.compareWithSnapshot(image: screenshot.image, identifier: actionButton.rawValue)
        }
    }

    // MARK: - Private

    private func onOfferPriceScreen() -> OfferPriceScreen {
        self.baseScreen.on(screen: OfferPriceScreen.self)
    }
}

typealias OfferPriceScreen_ = OfferPriceSteps

extension OfferPriceScreen_: UIRootedElementProvider {
    static let rootElementID: String = "offer_price_screen"
    static let rootElementName: String = "Боттомшит цены"

    enum Element {
        case garageBanner
        case tradeInBlock
        case tradeInButton
        case youGarageCar
        case tradeInPayment
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .garageBanner:
            return "garage_promo"
        case .tradeInBlock:
            return "garage_trade_in"
        case .tradeInButton:
            return "Отправить заявку на трейд-ин"
        case .youGarageCar:
            return "you_garage_car"
        case .tradeInPayment:
            return "trade_in_payment"
        }
    }
}
