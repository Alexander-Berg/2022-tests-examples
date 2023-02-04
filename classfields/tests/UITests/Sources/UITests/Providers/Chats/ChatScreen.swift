import XCTest
import Snapshots

typealias ChatScreen_ = ChatSteps

extension ChatScreen_: UIRootedElementProvider {
    enum Element {
        case callButton
        case moreButton
        case presets
        case inputBar
        case offerPanel
        case userPanel
        case message(MessageSelector)
        case delayLable(String)
    }

    enum MessageSelector {
        case byIndexFromTop(Int)
        case byID(String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .callButton:
            return "call_button"

        case .moreButton:
            return "more_button"

        case .inputBar:
            return "chat_message_input_bar"

        case .presets:
            return "chat_preset_item"

        case .offerPanel:
            return "chat_offer_panel"

        case .userPanel:
            return "chat_user_status_panel"

        case .message:
            fatalError("Undefined, overriden by find method")

        case let .delayLable(text):
            return text
        }
    }

    func find(element: Element) -> XCUIElement {
        switch element {
        case .message(let selector):
            switch selector {
            case .byID(let id):
                return rootElement
                    .descendants(matching: .any)
                    .matching(NSPredicate(format: "identifier like \"message_cell_\(id)\""))
                    .firstMatch
            case .byIndexFromTop(let indexFromTop):
                return rootElement
                    .descendants(matching: .any)
                    .matching(NSPredicate(format: #"identifier like "message_cell_*""#))
                    .element(boundBy: indexFromTop)
                    .firstMatch
            }
        default:
            return rootElement
                .descendants(matching: .any)
                .matching(identifier: identifier(of: element))
                .firstMatch
        }
    }

    static let rootElementID = "DialogViewController"
    static let rootElementName = "Комната чата"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)

        return app
    }
}
