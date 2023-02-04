//
//  YaRentSatisfactionTests.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 07.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import Swifter
import XCTest
import YREAppConfig
import YREWebModels

final class YaRentSatisfactionTests: BaseTestCase {
    override func setUp() {
        super.setUp()
        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatWithSatisfactionNotification(using: self.dynamicStubs)
    }

    func testSatisfactionFromPromo() {
        let configuration = ExternalAppConfiguration.inAppServicesTests
        configuration.rentSatisfactionPromoWasShown = false
        self.relaunchApp(with: configuration)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        self.checkPromoShownAnalytics {
            InAppServicesSteps()
                .tapOnFirstFlat()

            YaRentFlatCardSteps()
                .isScreenPresented()
                .isContentLoaded()

            YaRentFlatNotificationSteps(.netPromoterScore)
                .isPresented()

            YaRentSatisfactionSteps()
                .isPromoPresented()
        }

        self.checkOpenSatisfactionFromPromoAnalytics {
            YaRentSatisfactionSteps()
                .tapOnPromoAction()
                .isPromoNotPresented()
        }

        YaRentSatisfactionSteps()
            .isViewPresented()

        let score: Int = 9
        let message: String = ""
        let expectation = XCTestExpectation(description: "Отправка оценки - \(score), с описанием \"\(message)\"")
        RentAPIStubConfiguration.setupSendNetPromoterScore(
            expectation: expectation,
            validateRequest: { $0 == Int32(score) && $1 == nil },
            using: self.dynamicStubs
        )

        self.checkRatingScrollingAnalytics {
            YaRentSatisfactionSteps()
                .tapOnItem(.score(score))
        }

        self.checkRatingSendAnalytics(with: score) {
            YaRentSatisfactionSteps()
                .tapOnSendButton()
        }

        YaRentSatisfactionSteps()
            .isViewNotPresented()

        expectation.yreEnsureFullFilledWithTimeout()
    }

    func testSatisfactionFromNotification() {
        let configuration = ExternalAppConfiguration.inAppServicesTests
        self.relaunchApp(with: configuration)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnFirstFlat()

        YaRentFlatCardSteps()
            .isScreenPresented()
            .isContentLoaded()

        YaRentSatisfactionSteps()
            .isPromoNotPresented()

        self.checkOpenSatisfactionFromNotificationAnalytics {
            YaRentFlatNotificationSteps(.netPromoterScore)
                .isPresented()
                .action()
        }

        YaRentSatisfactionSteps()
            .isViewPresented()

        let score: Int = 10
        let message: String = "Все нравится"
        let expectation = XCTestExpectation(description: "Отправка оценки - \(score), с описанием \"\(message)\"")
        RentAPIStubConfiguration.setupSendNetPromoterScore(
            expectation: expectation,
            validateRequest: { $0 == Int32(score) && $1 == message },
            using: self.dynamicStubs
        )

        self.checkRatingScrollingAnalytics {
            YaRentSatisfactionSteps()
                .tapOnItem(.score(score))
        }

        self.checkRatingSendAnalytics(with: score) {
            YaRentSatisfactionSteps()
                .fillMessage(with: message)
                .tapOnSendButton()
        }

        YaRentSatisfactionSteps()
            .isViewNotPresented()

        expectation.yreEnsureFullFilledWithTimeout()
    }


    // MARK: - Analytics

    private func checkAnalytics(event: MetricaAnalyticsEvent, steps: @escaping () -> Void) {
        let analyticsAgent = AnalyticsAgent.shared
        analyticsAgent.removeAllPreviousEvents()
        let expectation = analyticsAgent.expect(event: event)
        steps()
        expectation.yreEnsureFullFilledWithTimeout()
    }

    private func checkPromoShownAnalytics(steps: @escaping () -> Void) {
        let event = MetricaAnalyticsEvent(
            name: "Аренда. Показ промо оценки"
        )
        self.checkAnalytics(event: event, steps: steps)
    }

    private func checkOpenSatisfactionFromNotificationAnalytics(steps: @escaping () -> Void) {
        let event = MetricaAnalyticsEvent(
            name: "Аренда. Переход к оценке",
            payload: [
                "Источник": "Нотификация \"Оцените готовность рекомендовать\""
            ]
        )
        self.checkAnalytics(event: event, steps: steps)
    }

    private func checkOpenSatisfactionFromPromoAnalytics(steps: @escaping () -> Void) {
        let event = MetricaAnalyticsEvent(
            name: "Аренда. Переход к оценке",
            payload: [
                "Источник": "Промо"
            ]
        )
        self.checkAnalytics(event: event, steps: steps)
    }

    private func checkRatingScrollingAnalytics(steps: @escaping () -> Void) {
        let event = MetricaAnalyticsEvent(
            name: "Аренда. Прокрутка барабана оценки"
        )
        self.checkAnalytics(event: event, steps: steps)
    }

    private func checkRatingSendAnalytics(with score: Int, steps: @escaping () -> Void) {
        let event = MetricaAnalyticsEvent(
            name: "Аренда. Нажатие на отправку оценки",
            payload: [
                "Оценка": JSONObject(integerLiteral: score)
            ]
        )
        self.checkAnalytics(event: event, steps: steps)
    }
}
