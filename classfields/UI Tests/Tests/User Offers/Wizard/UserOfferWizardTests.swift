//
//  UserOfferWizardTests.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 8/6/20.
//  Copyright © 2020 Yandex. All rights reserved.
//


import XCTest
import YRECoreUtils

final class UserOfferWizardTests: BaseTestCase {
    func testDraftAlert() {
        let userOffersList = UserOffersListSteps()
        let wizard = UserOfferWizardSteps()
        let alert = ModalActionsAlertSteps()
        let form = UserOfferFormSteps()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)

        self.relaunchApp(with: .userOfferWizardTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        self.runActivity("Проходим по шагам визарда, чтобы сохранился черновик") {
            userOffersList
                .isScreenPresented()
                .tapAddOfferNavbarButton()

            wizard
                .isScreenPresented()

            self.subtests.run(
                .offerType(.rentLong),
                .realtyType(.apartment, for: .rentLong)
            )

            form
                .isScreenPresented()
                .tapOnCloseButton()
        }

        self.runActivity("Проверка открытия формы из черновика") {
            userOffersList
                .isScreenPresented()
                .tapAddOfferNavbarButton()

            alert
                .isScreenPresented()
                .tapOn("Продолжить из черновика")

            form
                .isScreenPresented()
                .tapOnCloseButton()
        }

        self.runActivity("Проверка открытия визарда по нажатию на \"Начать заново\"") {
            userOffersList
                .isScreenPresented()
                .tapAddOfferNavbarButton()

            alert
                .isScreenPresented()
                .tapOn("Начать заново")

            wizard
                .isScreenPresented()
        }
    }

    func testOfferTypeAndRealtyTypeSelection() {
        let userOffersList = UserOffersListSteps()
        let wizard = UserOfferWizardSteps()
        let form = UserOfferFormSteps()
        let alert = ModalActionsAlertSteps()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)

        for offerType in UserOfferWizardSubtests.OfferType.allCases {
            for realtyType in UserOfferWizardSubtests.RealtyType.allCases(for: offerType) {

                // swiftlint:disable:next closure_body_length
                self.runActivity("Проверка визарда с шагами: '\(offerType.description)', '\(realtyType.localizedValue)'") {
                    self.relaunchApp(with: .userOfferWizardTests)

                    InAppServicesSteps()
                        .isScreenPresented()
                        .isContentPresented()
                        .tapOnUserOffersListSection()

                    self.runActivity("Открытие визарда формы подачи") {
                        userOffersList
                            .isScreenPresented()
                            .isListPresented()
                            .tapAddOfferNavbarButton()
                        alert
                            .isScreenPresented()
                            .tapOn("Начать заново")
                        wizard
                            .isScreenPresented()
                    }

                    self.runActivity("Проход по шагам визарда формы подачи - '\(offerType.description)', '\(realtyType.localizedValue)'") {
                        self.subtests.run(
                            .offerType(offerType),
                            .realtyType(realtyType, for: offerType)
                        )
                    }


                    if realtyType == .commercial || realtyType == .garage {
                        let screenShotname = "UserOfferWizardTests_" + #function + "_categoryNotAvailableAlert"
                        self.runActivity("Проверка открытия алерта недоступности категории оффера") {
                            alert
                                .isScreenPresented()
                                .compareWithScreenshot(identifier: screenShotname)
                                .tapOnCloseButton()
                                .isScreenNotPresented()
                        }
                    }
                    else {
                        let screenShotname = "UserOfferWizardTests_" + #function + "\(offerType.rawValue)_\(realtyType.rawValue)_results"
                        self.runActivity("Проверяем заполненые поля формы подачи после прохода визарда") {
                            form
                                .isScreenPresented()
                                .compareWithScreenshot(identifier: screenShotname )
                        }
                    }
                }
            }
        }
    }

    func testRentProvider() {
        let userOffersList = UserOffersListSteps()
        let wizard = UserOfferWizardSteps()
        let form = UserOfferFormSteps()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)
        GeoAPIStubConfigurator.setupAddressGeocoder(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupIsPointInsideRentTrueSpb(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupOwnerApplicationDraft(using: self.dynamicStubs)

        self.relaunchApp(with: .userOfferWizardTests)
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        self.runActivity("Открытие визарда формы подачи") {
            userOffersList
                .isScreenPresented()
                .isListPresented()
                .tapAddOfferNavbarButton()
            wizard
                .isScreenPresented()
        }

        self.subtests.run(
            .offerType(.rentLong),
            .realtyType(.apartment, for: .rentLong),
            .address,
            .rentProvider(.checkOwnerFormAndOpenPrivatePersonForm)
        )

        form
            .isScreenPresented()
    }

    func testOwnerApplicationFormClosing() {
        let userOffersList = UserOffersListSteps()
        let wizard = UserOfferWizardSteps()
        let form = UserOfferFormSteps()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)
        GeoAPIStubConfigurator.setupAddressGeocoder(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupIsPointInsideRentTrueSpb(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupOwnerApplicationDraft(using: self.dynamicStubs)

        self.relaunchApp(with: .userOfferWizardTests)
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        self.runActivity("Открытие визарда формы подачи") {
            userOffersList
                .isScreenPresented()
                .isListPresented()
                .tapAddOfferNavbarButton()
            wizard
                .isScreenPresented()
        }

        self.subtests.run(
            .offerType(.rentLong),
            .realtyType(.apartment, for: .rentLong),
            .address,
            .rentProvider(.checkOwnerFormAndSave)
        )

        form
            .isScreenNotPresented()
        userOffersList
            .isScreenPresented()
    }

    func testOwnerApplicationFormSubmitting() {
        let userOffersList = UserOffersListSteps()
        let wizard = UserOfferWizardSteps()
        let form = UserOfferFormSteps()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)
        GeoAPIStubConfigurator.setupAddressGeocoder(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupIsPointInsideRentTrueSpb(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupOwnerApplicationDraft(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupSaveOwnerApplicationDraft(using: self.dynamicStubs)
        YaRentFlatsAPIStubConfiguration.setupSMSForSigning(using: self.dynamicStubs)
        YaRentFlatsAPIStubConfiguration.setupSignSMS(using: self.dynamicStubs)

        self.relaunchApp(with: .userOfferWizardTests)
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        self.runActivity("Открытие визарда формы подачи") {
            userOffersList
                .isScreenPresented()
                .isListPresented()
                .tapAddOfferNavbarButton()
            wizard
                .isScreenPresented()
        }

        self.subtests.run(
            .offerType(.rentLong),
            .realtyType(.apartment, for: .rentLong),
            .address,
            .rentProvider(.checkOwnerFormAndSubmit)
        )

        form
            .isScreenNotPresented()
        userOffersList
            .isScreenPresented()
    }

    func testLayout() {
        let userOffersList = UserOffersListSteps()
        let wizard = UserOfferWizardSteps()
        let form = UserOfferFormSteps()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)

        self.relaunchApp(with: .userOfferWizardTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        self.runActivity("Открытие визарда формы подачи") {
            userOffersList
                .isScreenPresented()
                .isListPresented()
                .tapAddOfferNavbarButton()

            wizard
                .isScreenPresented()
        }

        self.runActivity("Проход по шагам визарда формы подачи") {
            self.subtests.run(
                .offerType(.sell),
                .realtyType(.apartment, for: .sell)
            )
        }

        self.runActivity("Проверяем заполненые поля формы подачи после прохода визарда") {
            form
                .isScreenPresented()
                .compareWithScreenshot(identifier: "UserOfferWizardTests_" + #function + "_results")
        }
    }

    private lazy var subtests = UserOfferWizardSubtests(stubs: self.dynamicStubs)
}
