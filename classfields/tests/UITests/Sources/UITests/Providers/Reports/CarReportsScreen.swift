final class CarReportStandAloneScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "car_reports_screen"
    static let rootElementName = "Экран отчетов"

    enum Element {
        case report(_ vin: String)
        case reportButtons(_ vin: String)
        case certificationBlock
        case recallBlock
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .report(let vin):
            return "report_body_row_\(vin)"
        case .reportButtons(let vin):
            return "report_buttons_row_\(vin)"
        case .certificationBlock:
            return "vinchecker-certification"
        case .recallBlock:
            return "vinchecker-recall"
        }
    }
}

final class CarReportStandAloneCell: BaseSteps, UIElementProvider {
    enum Element {
        case favoriteButton(_ vin: String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .favoriteButton(let vin):
            return "favorite_button_\(vin)"
        }
    }
}
