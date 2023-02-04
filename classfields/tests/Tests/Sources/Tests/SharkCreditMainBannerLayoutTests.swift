//
//  SharkCreditMainBannerLayoutTest.swift
//  Tests
//
//  Created by Vitalii Stikhurov on 13.07.2021.
//

import XCTest
import AutoRuProtoModels
import AutoRuModels
import AutoRuUtils
import AutoRuFetchableImage
import Snapshots
@testable import AutoRuCredit
import UIKit

class SharkCreditMainBannerLayoutTests: BaseUnitTest {
    func model(creditStatus: CreditStatus) -> SharkCreditBannerLayoutModel {
        var creditParam = CreditParam(
            termSteps: [Int](12 ... 60),
            maxSum: 123211,
            minSum: 0,
            minInitialFeeRate: Double(7.0 / 100),
            defaultSum: min(500_000, 123211),
            shortAgreement: { _ in
                ("Согласен на обработку данных: ООО Яндекс.Вертикали для оформления заявки; АО Объединенное кредитное бюро и АО Яндекс Банк для подготовки отчета ООО Яндекс.Вертикали; Банками партнерами; в соответствии с условиями обработки данных.", "условиями обработки данных") },
            agreementURL: nil,
            banner: FetchableImage(image: .namedOrEmpty("auto-ru-finans")),
            discount: 0,
            dynamicAmountStepValues: [
                (upperBound: 300_000, stepValue: 5000),
                (upperBound: 1_000_000, stepValue: 10_000),
                (upperBound: 123211, stepValue: 100_000)
            ],
            discountInfoText: nil,
            isShark: true,
            helpText: nil,
            providerName: "Авто.ру",
            partners: [],
            roundMonthlyPaymentToNearest: 50,
            percent: 12
        )
        creditParam.partners = [
            .init(
                bankType: .unknownBankType,
                roundedLogo: nil,
                logo184X42: FetchableImage.testImage(withFixedSize: .init(width: 28, height: 28)),
                colorHexString: UIColor.red.aru_toHexString(),
                name: "name1",
                creditParam: creditParam,
                promoDesc: "",
                isPromoSuitable: true,
                exclusive: false),
            .init(
                bankType: .unknownBankType,
                roundedLogo: nil,
                logo184X42: FetchableImage.testImage(withFixedSize: .init(width: 28, height: 28)),
                colorHexString: UIColor.green.aru_toHexString(),
                name: "name2",
                creditParam: creditParam,
                promoDesc: "",
                isPromoSuitable: true,
                exclusive: false),
            .init(
                bankType: .unknownBankType,
                roundedLogo: nil,
                logo184X42: FetchableImage.testImage(withFixedSize: .init(width: 28, height: 28)),
                colorHexString: UIColor.blue.aru_toHexString(),
                name: "name3",
                creditParam: creditParam,
                promoDesc: "",
                isPromoSuitable: true,
                exclusive: false),
            .init(
                bankType: .unknownBankType,
                roundedLogo: nil,
                logo184X42: FetchableImage.testImage(withFixedSize: .init(width: 28, height: 28)),
                colorHexString: UIColor.black.aru_toHexString(),
                name: "name4",
                creditParam: creditParam,
                promoDesc: "",
                isPromoSuitable: true,
                exclusive: false)
        ]

        return SharkCreditBannerLayoutModel(creditStatus: creditStatus, creditParam: creditParam)
    }

    func test_stackAllBanks_new() {
        let layout = SharkCreditBannerLayout(
            model: model(creditStatus: .readyForCalculation),
            onCardBannerTap: ({})
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 400)
    }

    func test_stackAllBanks_draft() {
        let layout = SharkCreditBannerLayout(
            model: model(creditStatus: .draftExist(offer: nil, amount: nil, term: nil)),
            onCardBannerTap: ({})
        )
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 400)
    }

    func test_creditPromo_draft() {
        let layout = SharkCreditPromoBannerLayout(
            mode: .full,
            creditStatus: .draftExist(offer: nil, amount: nil, term: nil),
            sizeClass: .medium,
            onTap: {})
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 400)
    }

    func test_creditPromo_new() {
        let layout = SharkCreditPromoBannerLayout(
            mode: .full,
            creditStatus: .readyForCalculation,
            sizeClass: .medium,
            onTap: {})
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 400)
    }

    func test_creditPromo_short() {
        let layout = SharkCreditPromoBannerLayout(
            mode: .short,
            creditStatus: .readyForCalculation,
            sizeClass: .medium,
            onTap: {})
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 400)
    }

    func test_creditPromo_short_button() {
        let layout = SharkCreditPromoBannerLayout(
            mode: .shortWithButton,
            creditStatus: .readyForCalculation,
            sizeClass: .medium,
            onTap: {})
        Snapshot.compareWithSnapshot(layout: layout, maxWidth: 400)
    }
}
