//
//  CreditTests.swift
//  Tests
//
//  Created by Vitalii Stikhurov on 28.01.2021.
//

import XCTest
import AutoRuProtoModels
@testable import AutoRuCredit

final class CreditTests: BaseUnitTest {

    func test_productSortByStatus() {
        let productArray: [ActiveCreditLKViewModel.Product] = [
            .init(
                state: .canBeSend,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "3"),
            .init(
                state: .selfReject,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "6"),
            .init(
                state: .bankDeclined,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "5"),
            .init(
                state: .inProgress,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "4"),
            .init(
                state: .approve,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "2"),
            .init(
                state: .canBeRejected,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "1")
        ]

        XCTAssert(productArray.sortByPriority(orderCache: []).map(\.id) == ["1", "2", "3", "4", "5", "6"])
    }

    func test_productSortByStatusWithCache() {
        let productArray: [ActiveCreditLKViewModel.Product] = [
            .init(
                state: .canBeSend,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "3"),
            .init(
                state: .selfReject,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "6"),
            .init(
                state: .bankDeclined,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "5"),
            .init(
                state: .inProgress,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "4"),
            .init(
                state: .approve,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "2"),
            .init(
                state: .canBeRejected,
                productType: .creditCard(.init(limitLabel: "",
                                               gracePeriodDaysLabel: "",
                                               interestLabel: "",
                                               yearlyFeeRubLabel: "")),
                logo: .init(),
                id: "1")
        ]

        XCTAssert(productArray.sortByPriority(orderCache: ["2", "1", "4", "3", "5", "6"]).map(\.id) == ["2", "1", "4", "3", "5", "6"])
    }

    func test_mapClaimStateToViewState() {
        let claimStates: [Vertis_Shark_CreditApplication.Claim.ClaimState] = [
            .UNRECOGNIZED(0),
            .approved,
            .cancel,
            .canceledDraft,
            .draft,
            .issue,
            .needInfo,
            .new,
            .notSent,
            .preapproved,
            .reject,
            .unknownClaimState
        ]

        let expectedStates: [ActiveCreditLKViewModel.Product.State] = [
            .canBeSend,
            .approve,
            .selfReject,
            .canBeSend,
            .canBeRejected,
            .approve,
            .inProgress,
            .inProgress,
            .bankDeclined,
            .approve,
            .bankDeclined,
            .canBeSend
        ]

        XCTAssert(claimStates.map(\.viewState) == expectedStates)
    }
}
