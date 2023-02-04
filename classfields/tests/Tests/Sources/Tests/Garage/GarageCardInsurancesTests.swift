import XCTest
import AutoRuProtoModels
import Snapshots
import SwiftProtobuf
@testable import AutoRuGarageCard
@testable import AutoRuInsurance
import AutoRuUtils
import Darwin
import Foundation

final class GarageCardInsurancesTests: BaseUnitTest {
    func test_onlyNonExpiredInsurances() {
        setenv("MOCKED_DATE", "2022-01-19 15:00:00+03:00", 1)

        let insuranceCell = GarageInsuranceCell(insurances: insurances.filter {
            $0.to.date > MockableDate.date() &&
            $0.status != .expired
        })

        Snapshot.compareWithSnapshot(layout: insuranceCell, maxWidth: DeviceWidth.iPhone11)
    }

    func test_hasWithExpiredStatusInsurances() {
        setenv("MOCKED_DATE", "2022-01-19 15:00:00+03:00", 1)

        let insuranceCell = GarageInsuranceCell(insurances: insurances)
        Snapshot.compareWithSnapshot(layout: insuranceCell, maxWidth: DeviceWidth.iPhone11)
    }

    func test_expandedInsurances() {
        setenv("MOCKED_DATE", "2022-01-19 15:00:00+03:00", 1)

        let insuranceCell = GarageInsuranceCell(insurances: insurances, collapseExpiredInsurances: false)
        Snapshot.compareWithSnapshot(layout: insuranceCell, maxWidth: DeviceWidth.iPhone11)
    }
}

extension GarageCardInsurancesTests {
    private var date: Google_Protobuf_Timestamp { .init(date: Date(timeIntervalSince1970: 2000000000)) }
    private var expiredDate: Google_Protobuf_Timestamp { .init(date: Date(timeIntervalSince1970: 0)) }

    var insurances: [Auto_Api_Vin_Garage_Insurance] {
        [
            .with { insurance in
                insurance.insuranceType = .osago
                insurance.to = date
                insurance.serial = "XXX"
                insurance.number = "1234567890"
            },
            .with { insurance in
                insurance.insuranceType = .kasko
                insurance.to = date
                insurance.number = "1234567890"
            },
            .with { insurance in
                insurance.insuranceType = .unknownInsurance
                insurance.to = date
                insurance.number = "ЗАСТРАХУЙ БРАТУХУ"
            },
            .with { insurance in
                insurance.insuranceType = .unknownInsurance
                insurance.to = expiredDate
                insurance.number = "TINEK STRAHOVANIE"
            },
            .with { insurance in
                insurance.insuranceType = .osago
                insurance.to = date
                insurance.status = .expired
                insurance.number = "AAB 9998887770"
            }
        ]
    }
}
