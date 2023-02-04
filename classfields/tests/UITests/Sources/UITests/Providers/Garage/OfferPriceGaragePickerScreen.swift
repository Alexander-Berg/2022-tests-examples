final class OfferPriceGarageCarPickerScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "offer_price_garage_car_picker"
    static let rootElementName = "Пикер машины из гаража в боттомшите цены оффера"

    enum Element {
        case pickerItem(String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .pickerItem(let title):
            return title
        }
    }
}
