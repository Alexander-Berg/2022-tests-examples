//
//  DiscountPromoAppearanceTest.swift
//  AutoRu
//
//  Created by Roman Bevza on 2/15/21.
//

import XCTest
import AutoRuProtoModels
import AutoRuModels
import AutoRuAppearance
import AutoRuFormatters
import Snapshots
@testable import AutoRuDiscountPromo
import AutoRuColorSchema
import Foundation

final class DiscountPromoAppearanceTests: BaseUnitTest {

    func test_collapsedVAS() {
        let vas = SaleVAS(name: "Поднятие в топ",
                          days: 60,
                          alias: "all_sale_toplist",
                          price: 1000,
                          expires: -1,
                          createDate: Date(),
                          description: "Активация описание",
                          activated: false,
                          type: .toplist,
                          subtypes: [],
                          autopurchase: nil,
                          autoprolongation: .init(price: 500, allowed: false, enabled: false, isOnByDefault: false, expiresIn: 0),
                          discount: .init(oldPrice: 2000,
                                          startDate: Date(timeIntervalSinceNow: -60).timeIntervalSince1970,
                                          endDate: Date(timeIntervalSinceNow: 3600).timeIntervalSince1970,
                                          discount: 50,
                                          active: true),
                          paidReason: nil,
                          recommendationPriority: 1,
                          alternativeAliases: [])
        let model = DiscoutPromoCreator.collapsedModel(
            vas: vas,
            discountEndDate: Date(timeIntervalSinceNow: 3600).timeIntervalSince1970,
            actions: .init(onPriceTap: {  }, onExpandTap: { }))
        Snapshot.compareWithSnapshot(
            layout: DiscountPromoCollapsedLayoutSpec().build(model: model),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Primary.blue,
            identifier: #function
        )
    }

    func test_fullVAS() {
        let vas = SaleVAS(name: "Поднятие в топ",
                          days: 60,
                          alias: "all_sale_toplist",
                          price: 1000,
                          expires: -1,
                          createDate: Date(),
                          description: "Активация описание",
                          activated: false,
                          type: .toplist,
                          subtypes: [],
                          autopurchase: nil,
                          autoprolongation: .init(price: 500, allowed: false, enabled: false, isOnByDefault: false, expiresIn: 0),
                          discount: .init(oldPrice: 2000,
                                          startDate: Date(timeIntervalSinceNow: -60).timeIntervalSince1970,
                                          endDate: Date(timeIntervalSinceNow: 3600).timeIntervalSince1970,
                                          discount: 50,
                                          active: true),
                          paidReason: nil,
                          recommendationPriority: 1,
                          alternativeAliases: [])

        let model = DiscoutPromoCreator.fullLayoutModel(vas: vas,
                                                        discountEndDate: Date(timeIntervalSinceNow: 3600).timeIntervalSince1970,
                                                        offer: nil,
                                                        actions: .init(onButtonTapped: {},
                                                                       buttonViewAdjustments: { _ in },
                                                                       titleAdjustments: { _ in }))

        Snapshot.compareWithSnapshot(
            layoutSpec: DiscountPromoFullLayoutSpec(model: model),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Primary.blue,
            identifier: #function
        )
    }
}
