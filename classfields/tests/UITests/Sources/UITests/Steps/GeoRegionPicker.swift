import XCTest
import Snapshots

final class GeoRegionPickerCell: BaseSteps, UIElementProvider {
    enum Element: String {
        case checkbox = "region_checkbox"
    }
}

final class GeoRegionPicker: BaseSteps, UIRootedElementProvider {
    enum Element {
        case expandableRegion(Int)
        case leafRegion(Int)
        case doneButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .expandableRegion(let rid): return "region:\(rid)"
        case .leafRegion(let rid): return "selected:\(rid)"
        case .doneButton: return "done_button"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .expandableRegion(let rid): return "Регион (группа) id=\(rid)"
        case .leafRegion(let rid): return "Регион (один пункт) id=\(rid)"
        case .doneButton: return "Кнопка 'Готово'"
        }
    }

    static let rootElementID = "GeoSuggestViewController"
    static let rootElementName = "Гео пикер"
}
