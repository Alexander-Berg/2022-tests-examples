//
//  UserOfferWizardSubtests.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 8/10/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class UserOfferWizardSubtests {
    enum Subtest {
        case offerType(OfferType)
        case realtyType(RealtyType, for: OfferType)
        case address
        case rentProvider(RentProviderScenario)
    }

    
    init(stubs: HTTPDynamicStubs) {
        self.dynamicStubs = stubs
    }

    func run(_ subtests: Subtest...) {
        subtests.forEach { subtest in
            switch subtest {
                case .offerType(let offerType):
                    XCTContext.runActivity(named: "Выбор типа объявления - \(offerType.description)") { _ in
                        self.offerType(offerType)
                    }
                case let .realtyType(realtyType, for: offerType):
                    XCTContext.runActivity(named: "Выбор типа недвижимости - \(realtyType.localizedValue)") { _ in
                        self.realtyType(realtyType, for: offerType)
                    }
                case .address:
                    XCTContext.runActivity(named: "Выбор адреса") { _ in
                        self.address()
                    }
                case .rentProvider(let scenario):
                    XCTContext.runActivity(named: "Проверяем ловушку Яндекс Аренды") { _ in
                        self.rentProvider(scenario: scenario)
                    }
            }
        }
    }

    let dynamicStubs: HTTPDynamicStubs
}
