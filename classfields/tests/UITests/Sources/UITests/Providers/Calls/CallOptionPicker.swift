import XCTest
import Snapshots

final class CallOptionPicker: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case app2AppAudioOption = "audio_call_option"
        case phoneOption = "phone_call_option"
    }

    static let rootElementID = "call_option_picker"
    static let rootElementName = "Пикер телефонных номеров"
}
