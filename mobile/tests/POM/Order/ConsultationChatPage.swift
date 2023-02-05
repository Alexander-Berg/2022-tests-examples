import XCTest

class ConsultationChatPage: PageObject {

    static var current: ConsultationChatPage {
        let element = XCUIApplication().otherElements[ConsultationChatAccessibility.root]
        return ConsultationChatPage(element: element)
    }

}
