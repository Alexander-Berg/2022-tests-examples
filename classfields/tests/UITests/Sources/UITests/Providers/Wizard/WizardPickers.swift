import Foundation
import XCTest

class PickerProvider: BaseSteps {
    enum Element {
        case wizardItem(String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .wizardItem(let item):
            return item
        }
    }
}

//MARK: - Пикеры визарда при создании объявления (не все)

final class WizardVINPicker: BaseSteps, UIRootedElementProvider {
    typealias Element = Void

    static let rootElementID = "vin_picker_view"
    static let rootElementName = "Ввода вина в визарде"
}

final class WizardGovNumberPicker: BaseSteps, UIRootedElementProvider {
    typealias Element = Void

    static let rootElementID = "grz_picker_view"
    static let rootElementName = "Ввода ГРЗ в визарде"
}

final class WizardMarkPicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "mark_picker_view"
    static let rootElementName = "Пикер марки в визарде"
}

final class WizardModelPicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "model_picker_view"
    static let rootElementName = "Пикер модели в визарде"
}

final class WizardYearPicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "year_picker_view"
    static let rootElementName = "Пикер года в визарде"
}

final class WizardGenerationPicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "generation_picker_view"
    static let rootElementName = "Пикер поколения в визарде"
}

final class WizardBodyTypePicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "bodyType_picker_view"
    static let rootElementName = "Пикер типа кузова в визарде"
}

final class WizardEngineTypePicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "engineType_picker_view"
    static let rootElementName = "Пикер типа двигателя в визарде"
}

final class WizardDriveTypePicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "driveType_picker_view"
    static let rootElementName = "Пикер типа привода в визарде"
}

final class WizardTransmissionTypePicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "transmissionType_picker_view"
    static let rootElementName = "Пикер типа коробки в визарде"
}

final class WizardModificationPicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "modification_picker_view"
    static let rootElementName = "Пикер модификации в визарде"
}

final class WizardColorPicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "color_picker_view"
    static let rootElementName = "Пикер цвета кузова в визарде"
}

final class WizardOwnersCountPicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "owners_count_view"
    static let rootElementName = "Пикер кол-ва владельцев в визарде"
}

final class WizardPTSPicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "pts_picker_view"
    static let rootElementName = "Пикер кол-ва владельцев в визарде"
}

final class WizardDescriptionPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "description_picker_view"
    static let rootElementName = "Пикер описания в визарде"

    typealias Element = Void
}

final class WizardContactsPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "contacts_picker_view"
    static let rootElementName = "Пикер контактов в визарде"

    typealias Element = Void
}

final class WizardProvenOwnerPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "proven_owner_picker_view"
    static let rootElementName = "Пикер проверенного собствнника в визарде"

    typealias Element = Void
}

final class WizardMileagePicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "mileage_picker_view"
    static let rootElementName = "Пикер пробега в визарде"

    enum Element: String {
        case mileageInput = "mileage_input"
    }
}

final class WizardPhonesPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "phones_picker_view"
    static let rootElementName = "Пикер телефонов в визарде"

    enum Element: String {
        case protectPhoneCell = "protectPhone"
        case enableApp2AppCallCell = "enableApp2AppCalls"
    }
}

final class WizardPricePicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "price_picker_view"
    static let rootElementName = "Пикер цены в визарде"

    typealias Element = Void
}

final class GarageWizardContainer: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "container_view"
    static let rootElementName = "Контейнер визарда в гараже"

    enum Element: String {
        case skipButton = "wizard_skip_button"
    }
}

// MARK: - Пикеры при создании отзыва

final class WizardOwningTimePicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "owningTime_picker_view"
    static let rootElementName = "Пикер срока владения в визарде"
}

final class WizardPlusesPicker: PickerProvider, UIRootedElementProvider {
    static let rootElementID = "pluses_picker_view"
    static let rootElementName = "Плюсы владения"

    enum Element: String {
        case more = "Ещё 13"
        case offRoad = "Проходимость"
        case nextButton = "wizard_next_button"
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}

final class WizardMinusesPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "minuses_picker_view"
    static let rootElementName = "Минусы владения"

    enum Element: String {
        case safety = "Безопасность"
        case nextButton = "wizard_next_button"
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}

final class WizardAppearancePicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "appearance_picker_view"
    static let rootElementName = "Внешность"

    enum Element: String {
        case starButton = "star_4" // Means 5 star
    }
}

final class WizardSafetyPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "safety_picker_view"
    static let rootElementName = "Безопасность"

    enum Element: String {
        case starButton = "star_3"
    }
}

final class WizardComfortPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "comfort_picker_view"
    static let rootElementName = "Комфорт"

    enum Element: String {
        case starButton = "star_2"
    }
}

final class WizardReliabilityPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "reliability_picker_view"
    static let rootElementName = "Надежность"

    enum Element: String {
        case starButton = "star_1"
    }
}

final class WizardDriveabilityPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "driveability_picker_view"
    static let rootElementName = "Управляемость"

    enum Element: String {
        case starButton = "star_0"
    }
}

// MARK: - Фото пикер

final class WizardPhotoPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "photo_picker_view"
    static let rootElementName = "Пикер фото в визарде"

    typealias Element = Void
}

final class WizardDescriptionPanoramaPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "panorama_description_picker_view"
    static let rootElementName = "Пикер предложения записать панораму в визарде"

    typealias Element = Void
}
