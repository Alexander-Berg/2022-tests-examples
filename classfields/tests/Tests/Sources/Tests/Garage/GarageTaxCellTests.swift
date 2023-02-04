//
//  GarageTaxCellTests.swift
//  Tests
//
//  Created by Igor Shamrin on 05.07.2021.
//
@testable import AutoRuGarageCard
import XCTest
import AutoRuModels
import AutoRuProtoModels
import Snapshots
import Foundation

final class GarageTaxCellTests: BaseUnitTest {

    func test_TaxCell() throws {
        let taxStub = try fetchCard().tax

        let layout = TaxCell(taxModel: taxStub)
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_TaxDreamCarCellWithInfo() throws {
        let card = try fetchCard()
        let regionInfo: RegionInfo = .init(id: 213, title: "Москва", subtitle: "")
        let model: GarageCardViewModel.TaxInfo = .init(
            apiTax: card.tax,
            regionInfo: regionInfo,
            modification: card.vehicleInfo.carInfo
        )

        let layout = TaxDreamCarCell(model: model, regionButtonTapped: { _ in }, modificationButtonTapped: { _ in })
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    func test_TaxDreamCarCellWithoutInfo() throws {
        var card = try fetchCard()
        card.tax.minTax = 75000
        card.vehicleInfo.carInfo.clearTechParam()

        let model: GarageCardViewModel.TaxInfo = .init(
            apiTax: card.tax,
            regionInfo: nil,
            modification: card.vehicleInfo.carInfo
        )

        let layout = TaxDreamCarCell(model: model, regionButtonTapped: { _ in }, modificationButtonTapped: { _ in })
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: DeviceWidth.iPhone11)
    }

    private func fetchCard() throws -> Auto_Api_Vin_Garage_Card {
        let fileURL = try XCTUnwrap(Bundle.current.url(forResource: "GaragePriceStatsGraphCell", withExtension: "json"))
        let data = try Data(contentsOf: fileURL)
        let card = try Auto_Api_Vin_Garage_GetCardResponse(jsonUTF8Data: data).card
        return card
    }
}
