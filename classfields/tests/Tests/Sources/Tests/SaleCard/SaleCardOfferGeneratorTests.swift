//
//  SaleCardOfferGeneratorTests.swift
//  Tests
//
//  Created by Roman Bevza on 2/9/21.
//

import Foundation
import AutoRuModels
import AutoRuProtoModels
import AutoRuYogaLayout
import AutoRuProtobuf
import XCTest
@testable import AutoRuSaleCardSharedModels

final class SaleCardOfferModelGeneratorTests: BaseUnitTest {

    private lazy var offer: Auto_Api_Offer = {
        let url = Bundle.current
            .url(forResource: "offer_CARS_1098252972-99d8c274_ok", withExtension: "json")!
        let response = try! Auto_Api_OfferResponse(jsonUTF8Data: Data(contentsOf: url))
        return response.offer
    }()

    func test_detailsForNewCar() {
        XCTContext.runActivity(named: "Проверка что OfferModelGenerator верно генерует характеристики для нового авто") { _ in }
        var offer = self.offer
        let purchaseDate = Calendar.current.date(byAdding: .year, value: -1, to: Date())
        offer.documents.purchaseDate = purchaseDate.flatMap{ Auto_Api_Date(date: $0) } ?? Auto_Api_Date()
        offer.section = .new
        let generator = OfferModelGenerator()
        let saleCardModel = generator.generateSaleCardModel(offer: offer,
                                                            additionalInfo: nil,
                                                            userNote: "",
                                                            canDisplayPriceChange: false,
                                                            hasChatBotFeature: false,
                                                            hasCarReport: false,
                                                            isDealer: false,
                                                            relatedOffers: [],
                                                            specialOffers: [],
                                                            videos: [],
                                                            journalArticles: [],
                                                            descriptionExpanded: false,
                                                            initialImageIndex: nil,
                                                            creditParam: nil,
                                                            scrollDidScroll: nil,
                                                            shouldShowSafeDealNewBadge: false,
                                                            publicProfileInfo: nil,
                                                            discountedMainPriceVersion: .dealerNewOnly)
        let expectedDetails = [
            ("Статус", "В наличии"),
            ("Год выпуска", "2012"),
            ("Пробег", "100 000 км"),
            ("Кузов", "Седан"),
            ("Цвет", "Красный"),
            ("Двигатель", "2.0 л / 184 л.с. / Бензин"),
            ("Налог", "9 200 ₽ в год"),
            ("КПП", "Автоматическая"),
            ("Привод", "Задний"),
            ("Владельцы", "3 или более"),
            ("Срок владения", "1 год"),
            ("VIN", "X4X3B19400J120073"),
            ("Госномер", "К697ХР750")
        ]
        let details = saleCardModel.details.map { ($0.title, $0.detail) }
        let diff = details.difference(from: expectedDetails, by: { (lhs, rhs) -> Bool in
            return lhs == rhs
        })
        XCTAssertTrue(details.enumerated().allSatisfy({ $0.element == expectedDetails[$0.offset]}) && details.count == expectedDetails.count, "Характеристики не совпадают с ожидаемыми. \(diff)")
    }

    func test_detailsForUsedCar() {
        XCTContext.runActivity(named: "Проверка что OfferModelGenerator верно генерует характеристики для авто с пробегом") { _ in }
        var offer = self.offer
        offer.section = .used
        let purchaseDate = Calendar.current.date(byAdding: .year, value: -1, to: Date())
        offer.documents.purchaseDate = purchaseDate.flatMap{ Auto_Api_Date(date: $0) } ?? Auto_Api_Date()
        let generator = OfferModelGenerator()
        let saleCardModel = generator.generateSaleCardModel(offer: offer,
                                                            additionalInfo: nil,
                                                            userNote: "",
                                                            canDisplayPriceChange: false,
                                                            hasChatBotFeature: false,
                                                            hasCarReport: false,
                                                            isDealer: false,
                                                            relatedOffers: [],
                                                            specialOffers: [],
                                                            videos: [],
                                                            journalArticles: [],
                                                            descriptionExpanded: false,
                                                            initialImageIndex: nil,
                                                            creditParam: nil,
                                                            scrollDidScroll: nil,
                                                            shouldShowSafeDealNewBadge: false,
                                                            publicProfileInfo: nil,
                                                            discountedMainPriceVersion: .dealerNewOnly)
        let expectedDetails = [
            ("Год выпуска", "2012"),
            ("Пробег", "100 000 км"),
            ("Кузов", "Седан"),
            ("Цвет", "Красный"),
            ("Двигатель", "2.0 л / 184 л.с. / Бензин"),
            ("Налог", "9 200 ₽ в год"),
            ("КПП", "Автоматическая"),
            ("Привод", "Задний"),
            ("Руль", "Левый"),
            ("Состояние", "Не требует ремонта"),
            ("Владельцы", "3 или более"),
            ("ПТС", "Оригинал"),
            ("Срок владения", "1 год"),
            ("Таможня", "Растаможен"),
            ("Обмен", "Не интересует"),
            ("VIN", "X4X3B19400J120073"),
            ("Госномер", "К697ХР750")
        ]
        let details = saleCardModel.details.map { ($0.title, $0.detail) }

        XCTAssertTrue(details.enumerated().allSatisfy({ $0.element == expectedDetails[$0.offset]}) && details.count == expectedDetails.count, "Характеристики не совпадают с ожидаемыми")
    }
}
