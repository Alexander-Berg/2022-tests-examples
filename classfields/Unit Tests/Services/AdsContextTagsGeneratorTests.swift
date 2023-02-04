//
//  AdsContextTagsGeneratorTests.swift
//  Unit Tests
//
//  Created by Pavel Zhuravlev on 09.10.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import YREServiceLayer
@testable import YRELegacyFiltersCore
import YREFiltersModel

final class AdsContextTagsGeneratorTests: XCTestCase {
    enum Traits {
        static let newbuilding = NSNumber(value: FilterObjectType.site.rawValue)
        static let village = NSNumber(value: FilterObjectType.village.rawValue)

        static let allRooms = NSOrderedSet(array: [
            NSNumber(value: kYREFilterRoomsTotal.studio.rawValue),
            NSNumber(value: kYREFilterRoomsTotal.total1.rawValue),
            NSNumber(value: kYREFilterRoomsTotal.total2.rawValue),
            NSNumber(value: kYREFilterRoomsTotal.total3.rawValue),
            NSNumber(value: kYREFilterRoomsTotal.total4Plus.rawValue),
        ])

        static let allVillageOfferTypes = NSOrderedSet(array: [
            NSNumber(value: FilterVillageOfferType.cottage.rawValue),
            NSNumber(value: FilterVillageOfferType.townhouse.rawValue),
            NSNumber(value: FilterVillageOfferType.land.rawValue)
        ])

        static let allHouseTypes = NSOrderedSet(array: [
            NSNumber(value: kYREFilterHouseType.townhouse.rawValue),
            NSNumber(value: kYREFilterHouseType.duplex.rawValue),
            NSNumber(value: kYREFilterHouseType.part.rawValue),
            NSNumber(value: kYREFilterHouseType.whole.rawValue),
        ])

        static let allGarageTypes = NSOrderedSet(array: [
            NSNumber(value: FilterGarageType.box.rawValue),
            NSNumber(value: FilterGarageType.garage.rawValue),
            NSNumber(value: FilterGarageType.parkingPlace.rawValue)
        ])

        static let allBuyCommercialTypes = NSOrderedSet(array: [
            NSNumber(value: FilterCommercialType.office.rawValue),
            NSNumber(value: FilterCommercialType.retail.rawValue),
            NSNumber(value: FilterCommercialType.freePurpose.rawValue),
            NSNumber(value: FilterCommercialType.warehouse.rawValue),
            NSNumber(value: FilterCommercialType.publicCatering.rawValue),
            NSNumber(value: FilterCommercialType.hotel.rawValue),
            NSNumber(value: FilterCommercialType.autoRepair.rawValue),
            NSNumber(value: FilterCommercialType.manufacturing.rawValue),
            // no legal address
            NSNumber(value: FilterCommercialType.land.rawValue),
            NSNumber(value: FilterCommercialType.business.rawValue)
        ])

        static let allRentCommercialTypes = NSOrderedSet(array: [
            NSNumber(value: FilterCommercialType.office.rawValue),
            NSNumber(value: FilterCommercialType.retail.rawValue),
            NSNumber(value: FilterCommercialType.freePurpose.rawValue),
            NSNumber(value: FilterCommercialType.warehouse.rawValue),
            NSNumber(value: FilterCommercialType.publicCatering.rawValue),
            NSNumber(value: FilterCommercialType.hotel.rawValue),
            NSNumber(value: FilterCommercialType.autoRepair.rawValue),
            NSNumber(value: FilterCommercialType.manufacturing.rawValue),
            NSNumber(value: FilterCommercialType.legalAddress.rawValue),
            NSNumber(value: FilterCommercialType.land.rawValue),
            NSNumber(value: FilterCommercialType.business.rawValue)
        ])

        enum RentTime {
            static let long = NSNumber(value: kYREFilterRentTime.timeLong.rawValue)
            static let short = NSNumber(value: kYREFilterRentTime.timeShort.rawValue)
        }
    }
}
