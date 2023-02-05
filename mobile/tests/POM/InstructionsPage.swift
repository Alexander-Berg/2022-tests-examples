import XCTest

/// Экран инструкций, у медецинских товаров

class InstructionsPage: PageObject {

    static var current: InstructionsPage {
        let instrEl = XCUIApplication().otherElements[InstructionsAccessibility.root]
        return InstructionsPage(element: instrEl)
    }

    /// Блоки с заголовками инструкции
    var titlesBlocks: [XCUIElement] {
        element
            .cells
            .matching(identifier: InstructionsAccessibility.instructionCellTitle)
            .allElementsBoundByIndex
    }

    /// Блоки с текстом инструкции
    var specsCellTexts: [XCUIElement] {
        element
            .cells
            .matching(identifier: InstructionsAccessibility.instructionCellText)
            .allElementsBoundByIndex
    }

    /// NavigationBar на экране инструкций
    var navigationBar: NavigationBarPage {
        NavigationBarPage(element: NavigationBarPage.current.element)
    }
}
