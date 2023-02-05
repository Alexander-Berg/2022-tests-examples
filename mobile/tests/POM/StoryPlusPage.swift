import XCTest

final class StoryPlusPage: PageObject {

    static var current: StoryPlusPage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: PlusAccessibility.storyRoot)
            .firstMatch
        return StoryPlusPage(element: elem)
    }
}
