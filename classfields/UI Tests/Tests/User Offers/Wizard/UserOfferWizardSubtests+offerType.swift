//
//  UserOfferWizardSubtests+offerType.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 8/10/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension UserOfferWizardSubtests {
    enum OfferType: String, CaseIterable {
        case sell
        case rentLong
        case rentShort

        var localizedValue: String {
            switch self {
                case .sell: return "Продать\nнедвижимость"
                case .rentLong: return "Сдать\nдлительно"
                case .rentShort: return "Сдать\nпосуточно"
            }
        }

        var identifier: String {
            switch self {
                case .sell:
                    return "sell"
                case .rentLong:
                    return "rentLong"
                case .rentShort:
                    return "rentShort"
            }
        }

        var description: String {
            return self.localizedValue.replacingOccurrences(of: "\n", with: " ")
        }
    }

    func offerType(_ offerType: OfferType) {
        let offerTypeStep = UserOfferWizardSteps()
        offerTypeStep
            .isOfferTypeStepPresented()
            .tapOnOfferType(offerType.identifier)
    }
}
