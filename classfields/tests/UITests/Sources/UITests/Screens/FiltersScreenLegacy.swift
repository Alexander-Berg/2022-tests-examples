//
//  FiltersScreen.swift
//  UITests
//
//  Created by Alexander Malnev on 03/12/2019.
//

import XCTest
import Snapshots

class FiltersScreen: BaseScreen, Scrollable {
    enum Field: String {
        case geo
        case geoRadius
        case category
        case subcategory
        case section
        case mark
        case model
        case generation
        case year
        case price
        case creditPrice
        case operatingHours
        case transmission = "transmission_CARS"
        case saddleHeight
        case type
        case suspension
        case brakes
        case loading
        case loadHeight
        case craneRadius
        case bucketVolume
        case tractionClass
        case axis
        case cabin
        case cabinSuspension
        case wheelDrive
        case chassisSuspension
        case bodytype
        case seats
        case engine = "engine_CARS"
        case engineVolume = "engineVolume_CARS"
        case gear = "gear_CARS"
        case power = "power_CARS"
        case ecoClass
        case cylinders
        case cylindersType
        case strokes
        case run
        case acceleration = "acceleration_CARS"
        case fuelRate = "fuelRate_CARS"
        case clearance
        case trunkVolume
        case color = "color_CARS"
        case wheel
        case equipment
        case sellerType
        case officialDealer
        case ownersCount
        case owningTime
        case state
        case customs
        case pts
        case searchTag
        case manufacturerCheck
        case warranty
        case inStock
        case exchange
        case photo
        case video
        case panorama
        case delivery
        case topDays
        case allowedForCredit = "allowedForCredit_CARS"
        case nds
        case tags
        case forPro
        case onlineView
    }
    lazy var scrollableElement: XCUIElement = findAll(.collectionView).firstMatch

    lazy var confirmFiltersButton: XCUIElement = app.buttons["Готово"].firstMatch

    lazy var resetButton: XCUIElement = app.buttons["Сбросить"].firstMatch
    lazy var regionField: XCUIElement = find(by: "geo").firstMatch
    lazy var resultsButton: XCUIElement = app.buttons.matching(identifier: "show_results").firstMatch

    func regionCell(index: Int) -> XCUIElement {
        return find(by: "region:\(index)").firstMatch
    }

    func regionCityCell(regionIndex: Int, index: Int) -> XCUIElement {
        return find(by: "region:\(regionIndex)_\(index)").firstMatch
    }

    func field(_ type: Field) -> XCUIElement {
        return find(by: type.rawValue).firstMatch
    }
}
