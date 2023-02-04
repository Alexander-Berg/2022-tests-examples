//
//  OfferCardStub.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 08.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import YREModelObjc
import YRETestsUtils
import XCTest

enum OfferCardStub: String, CaseIterable {
    case sellNewSaleApartment
    case sellNewPrimarySaleApartment
    case sellSecondarySaleApartment
    case sellRoom
    case sellHouse
    case sellLot
    case sellGarage
    case sellCommercial
    case rentLongApartment
    case rentShortRoom
    case rentHouse
    case rentGarage
    case rentCommercial

    case outdated
    case withChat

    static var allRootNodeCases: [Self] {
        [
            .sellNewSaleApartment,
            .sellNewPrimarySaleApartment,
            .sellSecondarySaleApartment,
            .sellRoom,
            .sellHouse,
            .sellLot,
            .sellGarage,
            .sellCommercial,
            .rentLongApartment,
            .rentShortRoom,
            .rentHouse,
            .rentGarage,
            .rentCommercial,
        ]
    }
}

extension OfferCardStub {
    func loadOffer() -> YREOffer? {
        guard let json = Self.loadStubJSON(from: "offerCard-\(self.rawValue).debug") else {
            XCTFail("Couldn't load JSON from resources.")
            return nil
        }

        guard let offer = YREOffer(json: json) else {
            XCTFail("Couldn't create offer from JSON")
            return nil
        }

        return offer
    }

    private static func loadStubJSON(from filename: String) -> [AnyHashable: Any]? {
        final class Class {}
        let data = ResourceProvider.jsonData(from: filename, in: Bundle(for: Class.self))

        guard let jsonResponse = try? JSONSerialization.jsonObject(with: data, options: []) as? [AnyHashable: Any] else {
            return nil
        }

        return jsonResponse["response"] as? [AnyHashable: Any]
    }
}
