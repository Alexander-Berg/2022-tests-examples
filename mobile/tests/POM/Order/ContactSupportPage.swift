import UIUtils
import XCTest

final class ContactSupportPage: PageObject {

    // MARK: - Public

    var callButton: CallButton {
        let el = element.any
            .matching(identifier: OrderDetailsAccessibility.ContactSupport.callButton)
            .firstMatch
        return CallButton(element: el)
    }

    var chatButton: ChatButton {
        let el = element.any
            .matching(identifier: OrderDetailsAccessibility.ContactSupport.chatButton)
            .firstMatch
        return ChatButton(element: el)
    }
}

// MARK: - EntryPoint

protocol ContactSupportEntryPoint: PageObject {
    func tap() -> ContactSupportPage
}

extension ContactSupportEntryPoint {

    func tap() -> ContactSupportPage {
        element.tap()
        let contactSupportElem = XCUIApplication().descendants(matching: .sheet).firstMatch
        XCTAssertTrue(contactSupportElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return ContactSupportPage(element: contactSupportElem)
    }
}

// MARK: - Nested types

extension ContactSupportPage {

    class CallButton: PageObject {

        var button: XCUIElement {
            element.buttons.firstMatch
        }

    }

    class ChatButton: PageObject {

        var button: XCUIElement {
            element.buttons.firstMatch
        }

    }

}
