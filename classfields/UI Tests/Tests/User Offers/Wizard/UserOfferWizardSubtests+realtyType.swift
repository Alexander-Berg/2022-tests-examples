//
//  UserOfferWizardSubtests+realtyType.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 8/10/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation

extension UserOfferWizardSubtests {
    enum RealtyType: String, CaseIterable {
        case apartment
        case room
        case house
        case lot
        case commercial
        case garage

        var identifier: String {
            switch self {
                case .apartment:
                    return "apartment"
                case .room:
                    return "room"
                case .house:
                    return "house"
                case .lot:
                    return "lot"
                case .commercial:
                    return "commercial"
                case .garage:
                    return "garage"
            }
        }

        var localizedValue: String {
            switch self {
                case .apartment:
                    return "Квартира"
                case .room:
                    return "Комната"
                case .house:
                    return "Дом"
                case .lot:
                    return "Участок"
                case .commercial:
                    return "Коммерческая"
                case .garage:
                    return "Гараж"
            }
        }

        static func allCases(for offerType: OfferType) -> [RealtyType] {
            switch offerType {
                case .sell:
                    return self.allCases
                case .rentLong:
                    return  self.allCases.filter { $0 != .lot }
                case .rentShort:
                    return  [.apartment, .room, .house]
            }
        }
    }

    func realtyType(_ realtyType: RealtyType, for offerType: OfferType) {
        let realtyTypeStep = UserOfferWizardSteps()
        realtyTypeStep
            .isRealtyTypeStepPresented()

        self.checkAnalytics {
            realtyTypeStep
                .tapOnRealtyType(realtyType.identifier)
        }
    }

    private func checkAnalytics(actions: @escaping () -> Void) {
        let analyticsAgent = AnalyticsAgent.shared
        analyticsAgent.removeAllPreviousEvents()

        let showEvent = MetricaAnalyticsEvent(
            name: "Визард нового объявления. Показ шага",
            payload: [
                "Шаг. Адрес": [
                    "Тип недвижимости": "Квартира",
                    "Тип сделки": "Сдать длительно"
                ]
            ])

        let expectations = [showEvent].map { analyticsAgent.expect(event: $0) }

        actions()

        expectations
            .yreEnsureFullFilledWithTimeout()
    }
}
