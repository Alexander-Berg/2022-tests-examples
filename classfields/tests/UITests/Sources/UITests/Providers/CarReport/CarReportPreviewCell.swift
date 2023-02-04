import XCTest

final class CarReportPreviewCell: BaseSteps, UIElementProvider {
    enum Element {
        case vin(String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case let .vin(vin):
            return "\(vin)"
        }
    }

    static let rootElementID: String = "backend_layout_cell"
    static let rootElementName: String = "Превью отчета на карточке"
}
