import Foundation
import Snapshots
import XCTest

final class GarageFormScreen: BaseScreen, Scrollable, NavigationControllerContent {
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    var saveButton: XCUIElement {
        find(by: "Сохранить").firstMatch
    }

    func scrollToSaveButton() {
        if !scrollTo(element: saveButton, maxSwipes: 5) {
            scrollTo(element: saveButton, maxSwipes: 5, swipeDirection: .down)
        }
    }

    var deleteButton: XCUIElement {
        find(by: "Удалить автомобиль").firstMatch
    }

    var addPhotoButton: XCUIElement {
        find(by: "AddPhotoButton").firstMatch
    }

    func scrollToDeleteButton() {
        if !scrollTo(element: deleteButton, maxSwipes: 2) {
            scrollTo(element: deleteButton, maxSwipes: 2, swipeDirection: .down)
        }
    }

    lazy var errorBlock = find(by: "error").firstMatch

    var addExtraCarInfoBanner: XCUIElement {
        find(by: "show_extra_car_info_banner").firstMatch
    }

    func scrollToExtraCarInfoBanner() {
        scrollTo(element: addExtraCarInfoBanner, maxSwipes: 2, swipeDirection: .down)
    }

    func fieldAndPlaceholder(_ field: Field) -> XCUIElement {
        find(by: "\(field)").firstMatch
    }

    func textField(_ field: Field) -> XCUIElement { fieldAndPlaceholder(field).textFields.element(boundBy: 0) }

    func scrollToFieldAndPlaceholder(_ field: Field) {
        let element = find(by: "\(field)").firstMatch
        if !scrollTo(element: element, maxSwipes: 3, windowInsets: .init(top: 64, left: 0, bottom: 0, right: 0)) {
            scrollTo(element: element, maxSwipes: 3, swipeDirection: .down)
        }
    }

    func photo(_ id: String) -> XCUIElement {
        find(by: id).firstMatch
    }

    enum Field: String, CustomStringConvertible {
        case mark
        case model
        case region
        case year
        case govNumber
        case vin
        case generation
        case body
        case engine
        case drive
        case transmission
        case modification
        case complectation
        case color
        case mileage
        case ownerCount
        case purchaseDate
        case saleDate

        var description: String {
            "garage_form_\(rawValue)"
        }

        var title: String {
            switch self {
            case .mark: return "Марка"
            case .model: return "Модель"
            case .region: return "Регион регистрации"
            case .year: return "Год выпуска"
            case .govNumber: return "Госномер"
            case .vin: return "VIN"
            case .generation: return "Поколение"
            case .body: return "Кузов"
            case .engine: return "Двигатель"
            case .drive: return "Привод"
            case .transmission: return "Коробка передач"
            case .modification: return "Модификация"
            case .complectation: return "Комплектация"
            case .color: return "Цвет"
            case .mileage: return "Пробег"
            case .ownerCount: return "Количество владельцев"
            case .purchaseDate: return "Дата приобретения"
            case .saleDate: return "Дата продажи"
            }
        }
    }
}
