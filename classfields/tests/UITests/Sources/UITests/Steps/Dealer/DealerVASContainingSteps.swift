import XCTest
import Snapshots

final class DealerVASContainingSteps: BaseSteps {
    func onDealerVASContainingScreen() -> DealerVASContainingScreen {
        return self.baseScreen.on(screen: DealerVASContainingScreen.self)
    }

    @discardableResult
    func scrollToVASButtonOrSwitch(offerID: String, type: DealerVASContainingScreen.VASListItemType) -> Self {
        Step("Скроллим до кнопки покупки или свитча у васа '\(type.rawValue)'") {
            let element = self.onDealerVASContainingScreen().vasPurchaseElement(offerID: offerID, type: type)
            self.onDealerVASContainingScreen().scrollTo(element: element, windowInsets: .init(top: 0, left: 0, bottom: 60, right: 0))
        }

        return self
    }

    @discardableResult
    func scrollToVASTitle(offerID: String, type: DealerVASContainingScreen.VASListItemType) -> Self {
        Step("Скроллим до васа '\(type.rawValue)'") {
            let element = self.onDealerVASContainingScreen().vasTitle(offerID: offerID, type: type)
            self.onDealerVASContainingScreen().scrollTo(element: element)
        }

        return self
    }

    @discardableResult
    func tapVASButtonOrSwitch(offerID: String, type: DealerVASContainingScreen.VASListItemType) -> Self {
        Step("Тапаем на кнопку покупки или свитч у васа '\(type.rawValue)'") {
            self.onDealerVASContainingScreen().vasPurchaseElement(offerID: offerID, type: type).tap()
        }

        return self
    }

    @discardableResult
    func checkVASSwitch(offerID: String, type: DealerVASContainingScreen.VASListItemType, isOn: Bool) -> Self {
        Step("Проверяем свитч для васа '\(type.rawValue)': ожидается состояние \(isOn ? "'активно'" : "'неактивно'")") {
            let element = self.onDealerVASContainingScreen().vasPurchaseElement(offerID: offerID, type: type)

            XCTAssert(element.elementType == .switch, "Элемент не является свитчем")
            XCTAssert(
                (element.value as? String) == (isOn ? "1" : "0"),
                "Неверное состояние свитча, ожидается \(isOn ? "активное" : "неактивное")"
            )
        }

        return self
    }

    @discardableResult
    func checkVASPurchaseElementNotExists(offerID: String, type: DealerVASContainingScreen.VASListItemType) -> Self {
        Step("Проверяем отсутствие свитча и кнопки для васа '\(type.rawValue)'") {
            self.onDealerVASContainingScreen().vasPurchaseElement(offerID: offerID, type: type).shouldNotExist()
        }

        return self
    }

    @discardableResult
    func tapOnVASItem(offerID: String, type: DealerVASContainingScreen.VASListItemType) -> DealerVASDescriptionSteps {
        Step("Тапаем на вас '\(type.rawValue)'") {
            self.onDealerVASContainingScreen().expandedVASListItem(offerID: offerID, type: type).tap()
        }

        return self.as(DealerVASDescriptionSteps.self)
    }

    @discardableResult
    func checkVASItemsSnapshot(
        offerID: String,
        identifier: String,
        from: DealerVASContainingScreen.VASListItemType,
        to: DealerVASContainingScreen.VASListItemType
    ) -> Self {
        Step("Делаем скриншот части списка васов и сравниваем со снепшотом '\(identifier)'") {
            let screen = self.onDealerVASContainingScreen()
            let firstVAS = screen.expandedVASListItem(offerID: offerID, type: from)
            let lastVAS = screen.expandedVASListItem(offerID: offerID, type: to)

            self.wait(for: 2)
            let snippet = Snapshot.screenshotCollectionView(
                fromCell: firstVAS,
                toCell: lastVAS,
                windowInsets: DealerCabinetScreen.insetsWithoutFilterAndTabBar
            )
            Snapshot.compareWithSnapshot(image: snippet, identifier: identifier)
        }

        return self
    }

    @discardableResult
    func checkVASItemSnapshot(
        offerID: String,
        identifier: String,
        vas: DealerVASContainingScreen.VASListItemType
    ) -> Self {
        Step("Делаем скриншот васа и сравниваем со снепшотом '\(identifier)'") {
            let screen = self.onDealerVASContainingScreen()
            let vas = screen.expandedVASListItem(offerID: offerID, type: vas)
            screen.scrollTo(element: vas, windowInsets: DealerCabinetScreen.insetsWithoutFilterAndTabBar)
            Snapshot.compareWithSnapshot(image: vas.waitAndScreenshot().image, identifier: identifier)
        }

        return self
    }
}
