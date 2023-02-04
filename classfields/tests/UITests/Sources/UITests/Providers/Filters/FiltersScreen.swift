import XCTest

typealias FiltersScreen_ = FiltersSteps

extension FiltersScreen_: UIRootedElementProvider {
    enum Element {
        case mmng
        case mark
        case model
        case generation
        case original_pts
        case manufacturerCheck
        case onlineView
        case warranty
        case exchange
        case photo
        case video
        case panorama
        case credit
        case delivery
        case geo
        case stateGroup
        case year
        case price
        case nds
        case creditPrice
        case transmission
        case bodytype
        case engine
        case engineVolume
        case gear
        case power
        case run
        case acceleration
        case fuelRate
        case clearance
        case trunkVolume
        case colors
        case color(_ index: Int)
        case wheel
        case optionPresets
        case complectation
        case seller
        case ownersCount
        case owningTime
        case inStock
        case state
        case customs
        case postingTime
        case searchButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .mmng:
            return "mark_CARS_add"
        case .mark:
            return "filters.mmngblock.mark"
        case .model:
            return "filters.mmngblock.model"
        case .generation:
            return "filters.mmngblock.generation"
        case .stateGroup:
            return "section"
        case .original_pts:
            return "filters.checkbox.pts_CARS"
        case .photo:
            return "filters.checkbox.photo"
        case .manufacturerCheck:
            return "filters.checkbox.manufacturerCheck_CARS"
        case .onlineView:
            return "filters.checkbox.onlineView_CARS"
        case .warranty:
            return "filters.checkbox.warranty_CARS"
        case .exchange:
            return "filters.checkbox.exchange"
        case .video:
            return "filters.checkbox.video_CARS"
        case .panorama:
            return "filters.checkbox.panorama_CARS"
        case .credit:
            return "filters.checkbox.tinkoff_CARS"
        case .delivery:
            return "filters.checkbox.delivery_CARS"
        case .geo:
            return "filters.picker.geo"
        case .year:
            return "filters.picker.year"
        case .price:
            return "filters.picker.price"
        case .nds:
            return "filters.checkbox.nds"
        case .creditPrice:
            return "filters.picker.creditPrice"
        case .transmission:
            return "filters.picker.transmission_CARS"
        case .bodytype:
            return "filters.picker.bodytype_CARS"
        case .engine:
            return "filters.picker.engine_CARS"
        case .engineVolume:
            return "filters.picker.engineVolume_CARS"
        case .gear:
            return "filters.picker.gear_CARS"
        case .power:
            return "filters.picker.power_CARS"
        case .run:
            return "filters.picker.run_CARS"
        case .acceleration:
            return "filters.picker.acceleration_CARS"
        case .fuelRate:
            return "filters.picker.fuelRate_CARS"
        case .clearance:
            return "filters.picker.clearance_CARS"
        case .trunkVolume:
            return "filters.picker.trunkVolume_CARS"
        case .colors:
            return "color_CARS"
        case let .color(index):
            return "filters.picker.color_CARS_\(index)"
        case .wheel:
            return "filters.picker.wheel_CARS"
        case .optionPresets:
            return "tags_CARS"
        case .complectation:
            return "equipment"
        case .seller:
            return "sellerType"
        case .ownersCount:
            return "filters.picker.ownersCount_CARS"
        case .owningTime:
            return "filters.picker.owningTime_CARS"
        case .state:
            return "filters.picker.state"
        case .customs:
            return "filters.picker.customs"
        case .postingTime:
            return "filters.picker.topDays"
        case .searchButton:
            return "show_results"
        case .inStock:
            return "filters.checkbox.inStock"
        }
    }

    static let rootElementID = "FiltersViewController"
    static let rootElementName = "Параметры поиска"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
