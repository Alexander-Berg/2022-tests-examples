import XCTest

final class OptionPresetPicker: BaseSteps, UIElementProvider {
    enum Element: String {
        case compact = "Компактный"
        case medium = "Средний"
        case big = "Большой"
        case comfortable = "Комфортный"
        case spaciousBackSeats = "Просторный задний ряд"
        case bigTrunk = "Вместительный багажник"
        case more = "Ещё 12"
        case oversizedCargo = "Негабаритный груз"
    }
}

