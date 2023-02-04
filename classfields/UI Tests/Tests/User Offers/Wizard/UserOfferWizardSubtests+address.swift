//
//  UserOfferWizardSubtests+address.swift
//  UI Tests
//
//  Created by Alexey Salangin on 17.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

extension UserOfferWizardSubtests {
    func address() {
        let offerTypeStep = UserOfferWizardSteps()

        let addressStep = offerTypeStep
            .isAddressScreenPresented()
            .waitAddressLoading()

        self.checkAnalytics {
            addressStep.tapSubmitButton()
        }
    }

    private func checkAnalytics(actions: @escaping () -> Void) {
        let analyticsAgent = AnalyticsAgent.shared
        analyticsAgent.removeAllPreviousEvents()

        let fillEvent = MetricaAnalyticsEvent(
            name: "Визард нового объявления. Заполнение шага",
            payload: [
                "Шаг. Адрес": [
                    "Тип недвижимости": "Квартира",
                    "Тип сделки": "Сдать длительно"
                ]
            ]
        )
        let showEvent = MetricaAnalyticsEvent(
            name: "Визард нового объявления. Показ шага",
            payload: [
                "Шаг. Ловушка Яндекс.Аренды": [
                    "Тип недвижимости": "Квартира",
                    "Тип сделки": "Сдать длительно"
                ]
            ])
        let fillExpectation = analyticsAgent.expect(event: fillEvent)
        let showExpectations = analyticsAgent.expect(event: showEvent)

        actions()

        [fillExpectation, showExpectations].yreEnsureFullFilledWithTimeout()
    }
}
