import XCTest
import Snapshots

typealias OfferEditScreen_ = OfferEditSteps

extension OfferEditScreen_: UIRootedElementProvider {
    enum Element {
        case app2appCalls
        case phonesRedirect
        case activationButton
        case publishWithoutActivationButton
        case pts
        case ownersNumber
        case nds
        case customCleared
        case ndsHelp
        case panoramaButton
        case panoramaReshootMenu
        case panoramaReshootButton
        case disableChats
    }

    static let rootElementName = "Форма добавления офера"
    static var rootElementID = "OfferEditViewController"

    func identifier(of element: Element) -> String {
        switch element {
        case .app2appCalls:
            return ".root.contacts.enable_app2app_calls"

        case .phonesRedirect:
            return ".root.contacts.phones_redirect"

        case .activationButton:
            return "activateButton"

        case .publishWithoutActivationButton:
            return "publishWithoutActivation"

        case .ownersNumber:
            return ".root.extra.owners_number"

        case .pts:
            return ".root.extra.pts"

        case .customCleared:
            return ".root.extra.custom"

        case .nds:
            return ".root.price.with_nds"

        case .ndsHelp:
            return "Выберите эту опцию, если будете готовы предоставить покупателю-юрлицу счёт-фактуру для вычета НДС."

        case .panoramaButton:
            return "panoramaButton"

        case .panoramaReshootMenu:
            return "Панорама обрабатывается"

        case .panoramaReshootButton:
            return "Переснять"

        case .disableChats:
            return ".root.contacts.disableChats"
        }
    }
    
    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)

        return app
    }
}
