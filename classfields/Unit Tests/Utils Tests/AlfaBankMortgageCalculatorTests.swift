//
//  AlfaBankMortgageCalculatorTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 5.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
@testable import YRECardComponents
@testable import YandexRealty

// swiftlint:disable file_length

// swiftlint:disable:next type_body_length
final class AlfaBankMortgageCalculatorTests: XCTestCase {
    func testPrimarySaleWithSupport() {
        let calculator = self.calculatorWithSupport

        let result = calculator.calculate(isNewFlatSale: true, input: .short(cost: 6_500_000))
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .shortSupport(.init(rate: 0.0599, sum: 12_000_000, downpaymentPercent: 0.15, downpayment: 975000))
        )

        XCTAssertEqual(result, expectedResult)
    }

    func testPrimarySaleWithoutSupport() {
        let calculator = self.calculatorWithoutSupport

        let result = calculator.calculate(isNewFlatSale: true, input: .short(cost: 6_500_000))
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .shortDiscount(
                .init(
                    rate: Decimal(0.0819).yre_rounded(scale: 4),
                    baseRate: 0.0859,
                    sum: 20_000_000,
                    downpayment: 1_950_000
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    func testResaleWithSupport() {
        let calculator = self.calculatorWithSupport

        let result = calculator.calculate(isNewFlatSale: false, input: .short(cost: 6_500_000))
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .shortDiscount(
                .init(
                    rate: 0.0839,
                    baseRate: Decimal(0.0879).yre_rounded(scale: 4),
                    sum: 21_000_000,
                    downpayment: 1_950_000
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    func testResaleWithoutSupport() {
        let calculator = self.calculatorWithoutSupport

        let result = calculator.calculate(isNewFlatSale: false, input: .short(cost: 6_500_000))
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .shortDiscount(
                .init(
                    rate: 0.0839,
                    baseRate: Decimal(0.0879).yre_rounded(scale: 4),
                    sum: 21_000_000,
                    downpayment: 1_950_000
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // MARK: - Support program
    // Test cases from https://wiki.yandex-team.ru/users/meltsina/notes/note-2020-07-29T113724/
    // See 'Новостройка' block at the bottom of page.

    // 1
    func testSupportProgram_minRate() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 8_600_000,
                    downpayment: 2_580_000,
                    period: 20,
                    supportMortgage: true
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .support(
                .init(
                    rate: Decimal(0.065).yre_rounded(scale: 4),
                    monthPayment: 44884,
                    sum: 6_020_000,
                    costRange: 705882...50_000_000,
                    downpayment: 2_580_000,
                    downpaymentRange: 1_290_000...8_600_000,
                    periodRange: 2...20
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 2
    func testSupportProgram_maxRate() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 8_500_000,
                    downpayment: 2_550_000,
                    period: 20,
                    supportMortgage: true
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .support(
                .init(
                    rate: Decimal(0.070).yre_rounded(scale: 4),
                    monthPayment: 46130,
                    sum: 5_950_000,
                    costRange: 705882...50_000_000,
                    downpayment: 2_550_000,
                    downpaymentRange: 1_275_000...8_500_000,
                    periodRange: 2...20
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 3
    func testSupportProgram_transitSum() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 8_571_429,
                    downpayment: 2_571_429,
                    period: 20,
                    supportMortgage: true
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .support(
                .init(
                    rate: Decimal(0.065).yre_rounded(scale: 4),
                    monthPayment: 44734,
                    sum: 6_000_000,
                    costRange: 705882...50_000_000,
                    downpayment: 2_571_429,
                    downpaymentRange: 1_285_714...8_571_429,
                    periodRange: 2...20
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 10
    func testSupportProgram_maxCredit_minPeriod() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 15_000_000,
                    downpayment: 3_000_000,
                    period: 2,
                    supportMortgage: true
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .support(
                .init(
                    rate: Decimal(0.065).yre_rounded(scale: 4),
                    monthPayment: 534555,
                    sum: 12_000_000,
                    costRange: 705882...50_000_000,
                    downpayment: 3_000_000,
                    downpaymentRange: 3_000_000...15_000_000,
                    periodRange: 2...20
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 11
    func testSupportProgram_minDownpayment_maxPeriod() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 11_000_000,
                    downpayment: 1_650_000,
                    period: 20,
                    supportMortgage: true
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .support(
                .init(
                    rate: Decimal(0.065).yre_rounded(scale: 4),
                    monthPayment: 69711,
                    sum: 9_350_000,
                    costRange: 705882...50_000_000,
                    downpayment: 1_650_000,
                    downpaymentRange: 1_650_000...11_000_000,
                    periodRange: 2...20
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 12
    func testSupportProgram_minCredit() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 3_700_000,
                    downpayment: 3_100_000,
                    period: 12,
                    supportMortgage: true
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .support(
                .init(
                    rate: Decimal(0.070).yre_rounded(scale: 4),
                    monthPayment: 6170,
                    sum: 600000,
                    costRange: 705882...50_000_000,
                    downpayment: 3_100_000,
                    downpaymentRange: 555000...3_700_000,
                    periodRange: 2...20
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // Support program. Error

    func testSupportProgram_minCreditSumError() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 10_000_000,
                    downpayment: 9_500_000,
                    period: 20,
                    supportMortgage: true
                )
            )
        )
        let expectedOutput = MortgageCalculator.Output.support(
            .init(
                rate: Decimal(0.070).yre_rounded(scale: 4),
                monthPayment: 3876,
                sum: 600000,
                costRange: 705882...50_000_000,
                downpayment: 9_500_000,
                downpaymentRange: 1_500_000...10_000_000,
                periodRange: 2...20
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.failure(.sumMin(600000, expectedOutput))

        XCTAssertEqual(result, expectedResult)
    }

    // MARK: - Primary sale program
    // Test cases from https://wiki.yandex-team.ru/users/meltsina/notes/note-2020-07-29T113724/
    // See 'ЖК' block at the bottom of page.

    // 1
    func testPrimarySaleProgram_default() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 6_500_000,
                    downpayment: 1_950_000,
                    period: 20,
                    supportMortgage: false
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .discount(
                .init(
                    rate: Decimal(0.0819).yre_rounded(scale: 4),
                    baseRate: Decimal(0.0859).yre_rounded(scale: 4),
                    monthPayment: 38598,
                    costRange: 670000...50_000_000,
                    downpayment: 1_950_000,
                    downpaymentRange: 650000...6_500_000,
                    periodRange: 3...30
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 2
    func testPrimarySaleProgram_maxPrice_maxCredit() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 50_000_000,
                    downpayment: 30_000_000,
                    period: 20,
                    supportMortgage: false
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .discount(
                .init(
                    rate: Decimal(0.0819).yre_rounded(scale: 4),
                    baseRate: Decimal(0.0859).yre_rounded(scale: 4),
                    monthPayment: 169661,
                    costRange: 670000...50_000_000,
                    downpayment: 30_000_000,
                    downpaymentRange: 30_000_000...50_000_000,
                    periodRange: 3...30
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 3
    func testPrimarySaleProgram_minCredit() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 50_000_000,
                    downpayment: 49_400_000,
                    period: 14,
                    supportMortgage: false
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .discount(
                .init(
                    rate: Decimal(0.0819).yre_rounded(scale: 4),
                    baseRate: Decimal(0.0859).yre_rounded(scale: 4),
                    monthPayment: 6013,
                    costRange: 670000...50_000_000,
                    downpayment: 49_400_000,
                    downpaymentRange: 30_000_000...50_000_000,
                    periodRange: 3...30
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 4
    func testPrimarySaleProgram_minPrice_minPeriod() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 670000,
                    downpayment: 67000,
                    period: 3,
                    supportMortgage: false
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .discount(
                .init(
                    rate: Decimal(0.0889).yre_rounded(scale: 4),
                    baseRate: Decimal(0.0929).yre_rounded(scale: 4),
                    monthPayment: 19144,
                    costRange: 670000...50_000_000,
                    downpayment: 67000,
                    downpaymentRange: 67000...670000,
                    periodRange: 3...30
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 5
    func testPrimarySaleProgram_minDownpayment_maxPeriod() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 3_700_000,
                    downpayment: 370000,
                    period: 30,
                    supportMortgage: false
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .discount(
                .init(
                    rate: Decimal(0.0889).yre_rounded(scale: 4),
                    baseRate: Decimal(0.0929).yre_rounded(scale: 4),
                    monthPayment: 26531,
                    costRange: 670000...50_000_000,
                    downpayment: 370000,
                    downpaymentRange: 370000...3_700_000,
                    periodRange: 3...30
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 6
    func testPrimarySaleProgram_sumTransit() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 3_700_000,
                    downpayment: 740000,
                    period: 20,
                    supportMortgage: false
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .discount(
                .init(
                    rate: Decimal(0.0819).yre_rounded(scale: 4),
                    baseRate: Decimal(0.0859).yre_rounded(scale: 4),
                    monthPayment: 25110,
                    costRange: 670000...50_000_000,
                    downpayment: 740000,
                    downpaymentRange: 370000...3_700_000,
                    periodRange: 3...30
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // Primary sale program. Error

    func testPrimarySaleProgram_minCreditSumError() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: true,
            input: .full(
                .init(
                    cost: 5_950_000,
                    downpayment: 5_500_000,
                    period: 20,
                    supportMortgage: false
                )
            )
        )
        let expectedOutput = MortgageCalculator.Output.discount(
            .init(
                rate: Decimal(0.0819).yre_rounded(scale: 4),
                baseRate: 0.0859,
                monthPayment: 3817,
                costRange: 670000...50_000_000,
                downpayment: 5_500_000,
                downpaymentRange: 595000...5_950_000,
                periodRange: 3...30
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.failure(.sumMin(600000, expectedOutput))

        XCTAssertEqual(result, expectedResult)
    }

    // MARK: - Resale program
    // Test cases from https://wiki.yandex-team.ru/users/meltsina/notes/note-2020-07-29T113724/
    // See 'Вторичка' block at the bottom of page.

    // 1
    func testResaleProgram_default() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: false,
            input: .full(
                .init(
                    cost: 5_000_000,
                    downpayment: 1_500_000,
                    period: 20,
                    supportMortgage: false
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .discount(
                .init(
                    rate: 0.0839,
                    baseRate: Decimal(0.0879).yre_rounded(scale: 4),
                    monthPayment: 30131,
                    costRange: 670000...50_000_000,
                    downpayment: 1_500_000,
                    downpaymentRange: 1_000_000...5_000_000,
                    periodRange: 3...30
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 2
    func testResaleProgram_maxPrice_maxCredit() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: false,
            input: .full(
                .init(
                    cost: 50_000_000,
                    downpayment: 30_000_000,
                    period: 20,
                    supportMortgage: false
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .discount(
                .init(
                    rate: 0.0839,
                    baseRate: Decimal(0.0879).yre_rounded(scale: 4),
                    monthPayment: 172175,
                    costRange: 670000...50_000_000,
                    downpayment: 30_000_000,
                    downpaymentRange: 30_000_000...50_000_000,
                    periodRange: 3...30
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 3
    func testResaleProgram_maxPrice_minCredit() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: false,
            input: .full(
                .init(
                    cost: 50_000_000,
                    downpayment: 49_400_000,
                    period: 20,
                    supportMortgage: false
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .discount(
                .init(
                    rate: 0.0839,
                    baseRate: Decimal(0.0879).yre_rounded(scale: 4),
                    monthPayment: 5165,
                    costRange: 670000...50_000_000,
                    downpayment: 49_400_000,
                    downpaymentRange: 30_000_000...50_000_000,
                    periodRange: 3...30
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 4
    func testResaleProgram_minPrice_minPeriod() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: false,
            input: .full(
                .init(
                    cost: 750000,
                    downpayment: 150000,
                    period: 3,
                    supportMortgage: false
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .discount(
                .init(
                    rate: 0.0839,
                    baseRate: Decimal(0.0879).yre_rounded(scale: 4),
                    monthPayment: 18910,
                    costRange: 670000...50_000_000,
                    downpayment: 150000,
                    downpaymentRange: 150000...750000,
                    periodRange: 3...30
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // 5
    func testResaleProgram_minDownpayment_maxPeriod() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: false,
            input: .full(
                .init(
                    cost: 3_750_000,
                    downpayment: 750000,
                    period: 30,
                    supportMortgage: false
                )
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.success(
            .discount(
                .init(
                    rate: 0.0839,
                    baseRate: Decimal(0.0879).yre_rounded(scale: 4),
                    monthPayment: 22834,
                    costRange: 670000...50_000_000,
                    downpayment: 750000,
                    downpaymentRange: 750000...3_750_000,
                    periodRange: 3...30
                )
            )
        )

        XCTAssertEqual(result, expectedResult)
    }

    // Resale program. Error

    func testResaleProgram_minCreditSumError() {
        let calculator = self.fullCalculator

        let result = calculator.calculate(
            isNewFlatSale: false,
            input: .full(
                .init(
                    cost: 5_950_000,
                    downpayment: 5_500_000,
                    period: 20,
                    supportMortgage: false
                )
            )
        )
        let expectedOutput = MortgageCalculator.Output.discount(
            .init(
                rate: Decimal(0.0839).yre_rounded(scale: 4),
                baseRate: Decimal(0.0879).yre_rounded(scale: 4),
                monthPayment: 3874,
                costRange: 670000...50_000_000,
                downpayment: 5_500_000,
                downpaymentRange: 1_190_000...5_950_000,
                periodRange: 3...30
            )
        )
        let expectedResult = MortgageCalculator.CalculationResult.failure(.sumMin(600000, expectedOutput))

        XCTAssertEqual(result, expectedResult)
    }

    // MARK: - Private

    private let fullCalculator = MortgageCalculator.Core(
        supportProgram: MortgageCalculator.SupportProgram(
            sumTransit: 6_000_000,
            rateSupportMin: 0.065,
            rateSupportMax: 0.070,
            sumMin: 600000,
            sumSupportMax: 12_000_000,
            downpaymentSupportMin: 0.15,
            downpaymentSupportMax: 1,
            costMin: 670000,
            costMax: 50_000_000,
            periodSupportMin: 2,
            periodSupportMax: 20
        ),
        primarySaleProgram: MortgageCalculator.PrimarySaleProgram(
            downpaymentTransit: 0.2,
            rateRange: 0.0859...0.0929,
            rateDiscountYandex: 0.004,
            sumRange: 600000...20_000_000,
            downpaymentRange: 0.1...1,
            costRange: 670000...50_000_000,
            periodRange: 3...30,
            downpaymentDefault: 0.299_999_999_99
        ),
        resaleProgram: MortgageCalculator.ResaleProgram(
            baseRate: 0.0879,
            rateDiscountYandex: 0.004,
            sumRange: 600000...20_000_000,
            downpaymentRange: 0.2...1,
            costRange: 670000...50_000_000,
            periodRange: 3...30,
            downpaymentDefault: 0.299_999_999_99
        )
    )

    private let calculatorWithSupport = MortgageCalculator.Core(
        supportProgram: MortgageCalculator.SupportProgram(
            sumTransit: 6_000_000,
            rateSupportMin: 0.0599,
            rateSupportMax: 0.0619,
            sumMin: 600000,
            sumSupportMax: 12_000_000,
            downpaymentSupportMin: 0.15,
            downpaymentSupportMax: 1,
            costMin: 600000,
            costMax: 50_000_000,
            periodSupportMin: 2,
            periodSupportMax: 20
        ),
        primarySaleProgram: MortgageCalculator.PrimarySaleProgram(
            downpaymentTransit: 0.2,
            rateRange: 0.0859...0.0929,
            rateDiscountYandex: 0.004,
            sumRange: 600000...20_000_000,
            downpaymentRange: 0.1...1,
            costRange: 670000...50_000_000,
            periodRange: 3...30,
            downpaymentDefault: 0.299_999_999_99
        ),
        resaleProgram: MortgageCalculator.ResaleProgram(
            baseRate: 0.0879,
            rateDiscountYandex: 0.004,
            sumRange: 600000...21_000_000,
            downpaymentRange: 0.2...1,
            costRange: 670000...50_000_000,
            periodRange: 3...30,
            downpaymentDefault: 0.299_999_999_99
        )
    )

    private let calculatorWithoutSupport = MortgageCalculator.Core(
        supportProgram: MortgageCalculator.SupportProgram(
            sumTransit: 6_000_000,
            rateSupportMin: 0.0599,
            rateSupportMax: 0.0619,
            sumMin: 600000,
            sumSupportMax: nil,
            downpaymentSupportMin: 0,
            downpaymentSupportMax: 1,
            costMin: 600000,
            costMax: 50_000_000,
            periodSupportMin: 2,
            periodSupportMax: 20
        ),
        primarySaleProgram: MortgageCalculator.PrimarySaleProgram(
            downpaymentTransit: 0.2,
            rateRange: 0.0859...0.0929,
            rateDiscountYandex: 0.004,
            sumRange: 600000...20_000_000,
            downpaymentRange: 0.1...1,
            costRange: 670000...50_000_000,
            periodRange: 3...30,
            downpaymentDefault: 0.299_999_999_99
        ),
        resaleProgram: MortgageCalculator.ResaleProgram(
            baseRate: 0.0879,
            rateDiscountYandex: 0.004,
            sumRange: 600000...21_000_000,
            downpaymentRange: 0.2...1,
            costRange: 670000...50_000_000,
            periodRange: 3...30,
            downpaymentDefault: 0.299_999_999_99
        )
    )
}
