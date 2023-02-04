//
//  FiltersSubtests.swift
//  UI Tests
//
//  Created by Alexey Salangin on 6/30/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Swifter
import XCTest
import YREFiltersModel

final class FiltersSubtests {
    enum Subtest {
        enum PriceType {
            case singleCase(numberRangeConfiguration: NumberRangeParameter.Configuration) // without type and period
            case multiple([PriceConfiguration])
        }
        
        case action(Action)
        case category(Category)
        case apartmentType(ApartmentType)
        case garageTypes
        case villageOfferTypes([FiltersSubtests.VillageOfferType], withPriceConfigurations: [PriceConfiguration])
        case objectType(ObjectType)
        case rentTime(RentTime)
        case totalRooms
        case price(PriceType)
        case onlySamolet

        case boolParameters([BoolParameter])
        case numberRange([NumberRangeParameter.Configuration])
        case singleSelectParameters([SingleSelectParameter])
        case multiParameters([MultiParameter])
        case multipleSelectParameters([MultipleSelectParameter])
        case metroDistance(Set<TimeToMetroOption>)
        case tagsToInclude
        case tagsToExclude
        case commercialTypeCancellation
        case commercialType(CommercialType)
        case geoIntent(options: GeoIntentOptions)
        case developer
        case buildingSeries
        case villageDeveloper
    }

    enum AnyOfferKind {
        case offer
        case site
        case village
    }

    // Collect info about subtest is runned or not.
    static var subtestsExecutionMarks = [String: Bool]()

    let filtersSteps: FiltersSteps
    let api: FiltersAPIStubConfigurator

    init(stubs: HTTPDynamicStubs, anyOfferKind: AnyOfferKind) {
        self.filtersSteps = FiltersSteps()

        let apiAnyOfferKind = Self.apiAnyOfferKind(anyOfferKind)
        self.api = FiltersAPIStubConfigurator(dynamicStubs: stubs, anyOfferKind: apiAnyOfferKind)
    }

    // swiftlint:disable:next function_body_length
    func run(_ subtests: Subtest...) {
        // swiftlint:disable:next closure_body_length
        subtests.forEach { subtest in
            switch subtest {
                case .action(let action):
                    XCTContext.runActivity(named: "Проверка смены типа сделки на \(action.readableName)") { _ in
                        self.action(action)
                    }
                case .category(let category):
                    XCTContext.runActivity(named: "Проверка смены категории недвижимости на \(category.readableName)") { _ in
                        self.category(category)
                    }
                case .apartmentType(let type):
                    XCTContext.runActivity(named: "Проверка смены типа квартиры на \(type.readableName)") { _ in
                        self.apartmentType(type)
                    }
                case .garageTypes:
                    XCTContext.runActivity(named: "Проверка выбора типа гаража") { _ in
                        self.garageTypes()
                    }
                case let .villageOfferTypes(types, configurations):
                    XCTContext.runActivity(named: "Проверка выбора типа коттеджного поселка") { _ in
                        self.villageOfferTypes(types: types, configurations: configurations)
                    }
                case .objectType(let type):
                    XCTContext.runActivity(named: "Проверка смены типа объекта (участка или дома) на \(type.readableName)") { _ in
                        self.objectType(type)
                    }
                case .rentTime(let rentTime):
                    XCTContext.runActivity(named: "Смена срока аренды на \(rentTime.readableName)") { _ in
                        self.rentTime(rentTime)
                    }
                case .totalRooms:
                    XCTContext.runActivity(named: "Проверка выбора комнат") { _ in
                        self.totalRooms()
                    }
                case .price(let .singleCase(configuration)):
                    XCTContext.runActivity(named: "Проверка изменения цены") { _ in
                        self.numberRangeParameter(
                            configuration.parameter,
                            configuration.value,
                            configuration.expectationScreenshotID
                        )
                    }
                case .price(.multiple(let configurations)):
                    XCTContext.runActivity(named: "Проверка изменения цены") { _ in
                        self.price(configurations: configurations)
                    }
                case .onlySamolet:
                    XCTContext.runActivity(named: "Проверка параметра \"ЖК от Самолет\"") { _ in
                        self.onlySamolet()
                    }
                case .boolParameters(let parameters):
                    parameters.forEach { parameter in
                        XCTContext.runActivity(named: "Проверка параметра \"\(parameter.readableName)\"") { _ in
                            self.boolParameter(parameter)
                        }
                    }
                case .numberRange(let configurations):
                    configurations.forEach { configuration in
                        XCTContext.runActivity(named: "Проверка параметра \"\(configuration.parameter.readableName)\"") { _ in
                            self.numberRangeParameter(
                                configuration.parameter,
                                configuration.value,
                                configuration.expectationScreenshotID
                            )
                        }
                    }
                case .singleSelectParameters(let parameters):
                    parameters.forEach { parameter in
                        XCTContext.runActivity(named: "Проверка параметра \"\(parameter.readableName)\"") { _ in
                            self.singleSelectParameter(parameter)
                        }
                    }
                case .multipleSelectParameters(let parameters):
                    parameters.forEach { parameter in
                        XCTContext.runActivity(named: "Проверка параметра \"\(parameter.readableName)\"") { _ in
                            self.multipleSelectParameter(parameter)
                        }
                    }
                case .multiParameters(let parameters):
                    parameters.forEach { parameter in
                        XCTContext.runActivity(named: "Проверка параметра \"\(parameter.readableName)\"") { _ in
                            self.multiParameter(parameter)
                        }
                    }
                case .metroDistance(let options):
                    XCTContext.runActivity(named: "Проверка параметра \"Время до метро\"") { _ in
                        self.metroDistance(options: options)
                    }
                case .tagsToInclude:
                    XCTContext.runActivity(named: "Проверка параметра \"Искать в описании объявления\"") { _ in
                        self.tagsToInclude()
                    }
                case .tagsToExclude:
                    XCTContext.runActivity(named: "Проверка параметра \"Не показывать объявления, если в описании\"") { _ in
                        self.tagsToExclude()
                    }
                case .commercialTypeCancellation:
                    XCTContext.runActivity(named: "Проверка отмены параметра \"Объект\" (тип коммерческой недвижимости)") { _ in
                        self.commercialTypeCancellation()
                    }
                case .commercialType(let value):
                    XCTContext.runActivity(named: "Проверка параметра \"Объект\" (тип коммерческой недвижимости)") { _ in
                        self.commercialType(value)
                    }
                case let .geoIntent(options: options):
                    XCTContext.runActivity(named: "Проверка выбора региона") { _ in
                        self.geoIntent(options)
                    }
                case .developer:
                    XCTContext.runActivity(named: "Проверка параметра \"Застройщик\"") { _ in
                        self.developer()
                    }
                case .villageDeveloper:
                    XCTContext.runActivity(named: "Проверка параметра \"Застройщик\"") { _ in
                        self.developer(isVillageDeveloper: true)
                    }
                case .buildingSeries:
                    XCTContext.runActivity(named: "Проверка параметра \"Серия дома\"") { _ in
                        self.buildingSeries()
                    }
            }
        }
    }

    private static func apiAnyOfferKind(_ anyOfferKind: AnyOfferKind) -> FiltersAPIStubConfigurator.AnyOfferKind {
        switch anyOfferKind {
            case .offer:
                return .offer
            case .site:
                return .site
            case .village:
                return .village
        }
    }
}
