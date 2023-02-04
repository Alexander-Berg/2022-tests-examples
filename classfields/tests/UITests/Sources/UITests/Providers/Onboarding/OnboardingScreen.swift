import XCTest

final class OnboardingScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case closeButton
        case nextButton
        case previousButton
        case actionButton
        case roleSelectionSlide
        case slide(index: Int)
    }

    static let rootElementName = "Экран онбординга"
    static var rootElementID = "OnboardingContainerController"

    func identifier(of element: Element) -> String {
        switch element {
        case .closeButton: return "close_button"
        case .roleSelectionSlide: return "OnboardingRoleViewController"
        case .nextButton: return "next_slide_button"
        case .previousButton: return "previous_slide_button"
        case .actionButton: return "slide_action_button"
        case .slide(let index): return "onboarding_slide_\(index)"
        }
    }
}
