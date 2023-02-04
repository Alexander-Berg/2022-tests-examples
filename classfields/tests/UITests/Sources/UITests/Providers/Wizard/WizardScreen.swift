import Foundation

typealias WizardScreen_ = WizardSteps

extension WizardScreen_: UIRootedElementProvider {
    enum Element {
        case continueButton
        case skipButton
        case addPhotoButton
        case nextButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .continueButton:
            return "Продолжить"
        case .skipButton:
            return "Пропустить"
        case .addPhotoButton:
            return "Добавить фото"
        case .nextButton:
            return "Далее"
        }
    }

    static let rootElementID = "wizard"
    static let rootElementName = "Визард"
}
