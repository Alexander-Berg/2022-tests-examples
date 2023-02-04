//
//  UserOfferWizardSubtests+rentProvider.swift
//  UI Tests
//
//  Created by Alexey Salangin on 17.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest

extension UserOfferWizardSubtests {
    enum RentProviderScenario {
        case checkOwnerFormAndOpenPrivatePersonForm
        case checkOwnerFormAndSave
        case checkOwnerFormAndSubmit
    }

    func rentProvider(scenario: RentProviderScenario) {
        self.commonStep()

        switch scenario {
            case .checkOwnerFormAndOpenPrivatePersonForm:
                self.openPrivatePersonForm()
            case .checkOwnerFormAndSave:
                self.saveOwnerForm()
            case .checkOwnerFormAndSubmit:
                self.submitOwnerForm()
        }
    }

    private func commonStep() {
        let realtyTypeStep = UserOfferWizardSteps()

        let rentProviderSteps = realtyTypeStep
            .isRentProviderStepPresented()

        self.checkYaRentAnalytics {
            rentProviderSteps
                .tapYandexRentButton()
        }

        YaRentOwnerApplicationSteps()
            .isScreenPresented()
        
        let analyticsAgent = AnalyticsAgent.shared
        analyticsAgent.removeAllPreviousEvents()
        let event = MetricaAnalyticsEvent(
            name: "Аренда. Открытие анкеты собственника",
            payload: [
                "Источник": ["Визард"]
            ]
        )
        analyticsAgent.expect(event: event).yreEnsureFullFilledWithTimeout()
    }

    private func openPrivatePersonForm() {
        YaRentOwnerApplicationSteps()
            .tapOnCloseButton()

        // @salangin: Right now AnalyticsAgent can work only with one event. Uncomment when it fixed.
//        self.checkPrivatePersonAnalytics {
        RentProviderSteps()
            .tapPrivatePersonButton()
//        }
    }

    private func saveOwnerForm() {
        YaRentOwnerApplicationSteps()
            .tapOnSaveButton()
    }

    private func submitOwnerForm() {
        YaRentOwnerApplicationSteps()
            .tapOnSubmitButton()

        SMSConfirmationSteps()
            .ensurePresented()
            .enterSMS()
            .tapOnConfirmButton()
            .tapOnDoneButton()
    }

    private func checkYaRentAnalytics(actions: @escaping () -> Void) {
        let analyticsAgent = AnalyticsAgent.shared
        analyticsAgent.removeAllPreviousEvents()

        let fillEvent = MetricaAnalyticsEvent(
            name: "Визард нового объявления. Заполнение шага",
            payload: [
                "Шаг. Ловушка Яндекс.Аренды": [
                    "Тип недвижимости": "Квартира",
                    "Тип сделки": "Сдать длительно",
                    "Вариант": "Сдам через Яндекс.Аренду"
                ]
            ])

        let expectations = [fillEvent].map { analyticsAgent.expect(event: $0) }

        actions()

        expectations
            .yreEnsureFullFilledWithTimeout()
    }

    private func checkPrivatePersonAnalytics(actions: @escaping () -> Void) {
        let analyticsAgent = AnalyticsAgent.shared
        analyticsAgent.removeAllPreviousEvents()

        let fillEvent = MetricaAnalyticsEvent(
            name: "Визард нового объявления. Заполнение шага",
            payload: [
                "Шаг. Ловушка Яндекс.Аренды": [
                    "Тип недвижимости": "Квартира",
                    "Тип сделки": "Сдать длительно",
                    "Вариант": "Сдам сам"
                ]
            ])

        let expectations = [fillEvent].map { analyticsAgent.expect(event: $0) }

        actions()

        expectations
            .yreEnsureFullFilledWithTimeout()
    }
}
