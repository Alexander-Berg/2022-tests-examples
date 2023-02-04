import XCTest

final class OnboardingRoleSelectionSlide: BaseSteps, UIElementProvider {
    enum Element {
        case roleButton(Role)
    }

    enum Role: String {
        case seller
        case buyer
        case owner
        case reader
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .roleButton(let role): return "role_button_\(role.rawValue)"
        }
    }
}

final class OnboardingSlide: BaseSteps, UIElementProvider {
    enum Element: String {
        case title = "slide_title"
        case text = "slide_text"
    }

    enum Role: String {
        case seller
        case buyer
        case owner
        case reader
    }
}

final class OnboardingNavigationButton: BaseSteps, UIElementProvider {
    enum Element: String {
        case title
    }

    func find(element: Element) -> XCUIElement {
        switch element {
        case .title: return rootElement
                .descendants(matching: .staticText)
                .matching(identifier: "navigation_button_title")
                .firstMatch
        }
    }
}
