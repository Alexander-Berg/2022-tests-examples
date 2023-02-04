import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf
@testable import AutoRuInsurance
import Foundation

class InsuranceFormTests: BaseUnitTest {
    func test_insuranceCreateForm() {
        let insurance = InsuranceModel(insuranceType: .kasko)
        let model = InsuranceFormState(insurance: insurance, garageCardId: "", isEditing: false)
        let layout = InsuranceFormViewController.Layout(model: model, onEditFromDate: {}, onEditTillDate: {}, onValueChanged: { _,_ in })
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_insuranceEditForm() {
        var insurance = InsuranceModel(insuranceType: .osago)
        insurance.companyName = "ПАО РОСГОССТРАХ"
        insurance.companyPhone = "+7 (911) 123-45-67"
        insurance.policyFullString = "XXX 123456789"
        insurance.validFrom = Date(timeIntervalSince1970: 1640702186)
        insurance.validTill = Date(timeIntervalSince1970: 1641002186)
        let model = InsuranceFormState(insurance: insurance, garageCardId: "", isEditing: true)
        let layout = InsuranceFormViewController.Layout(model: model, onEditFromDate: {}, onEditTillDate: {}, onValueChanged: { _,_ in })
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }
}
