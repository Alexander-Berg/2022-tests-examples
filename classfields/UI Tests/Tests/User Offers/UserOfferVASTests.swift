//
//  UserOfferVASTests.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 23.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation

final class UserOfferVASTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOfferPreview(using: self.dynamicStubs)
    }

    func testCommonVASUserOffer() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)

        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                snippetSteps
                    .isTurboContainerViewTappable()
                    .isTurboActivateButtonTappable()
                    .isRaisingContainerViewTappable()
                    .isRaisingPayButtonTappable()
                    .isPremiumContainerViewTappable()
                    .isPremiumPayButtonTappable()
                    .isPromotionContainerViewTappable()
                    .isPromotionPayButtonTappable()
                // TODO: 2 point from https://st.yandex-team.ru/VSAPPS-6372#5fe8f4edcc91ac793f1eca2
                //                    .compareWithScreenshot(identifier: "userOffers.list.snippetCell.vasCommon")
            },
            specificCardTests: { _ in }
        )
    }

    func testProceedToVASPaymentViaTurboScreen() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .turbo)

        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                snippetSteps
                    .isTurboContainerViewTappable()
                    .tapTurboContainerView()
                    .isScreenPresented()
                    .tapActivateButton()
                    .isPaymentMethodsScreenPresented()
            },
            specificCardTests: { _ in }
        )
    }

    func testProceedToVASPaymentViaPremiumScreen() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .premium)

        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                snippetSteps
                    .isPremiumContainerViewTappable()
                    .tapPremiumContainerView()
                    .isScreenPresented()
                    .tapActivateButton()
                    .isPaymentMethodsScreenPresented()
            },
            specificCardTests: { _ in }
        )
    }

    func testProceedToVASPaymentViaRaisingScreen() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .raising)

        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                snippetSteps
                    .isRaisingContainerViewTappable()
                    .tapRaisingContainerView()
                    .isScreenPresented()
                    .tapActivateButton()
                    .isPaymentMethodsScreenPresented()
            },
            specificCardTests: { _ in }
        )
    }

    func testProceedToVASPaymentViaPromotionScreen() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .promotion)

        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                snippetSteps
                    .isPromotionContainerViewTappable()
                    .tapPromotionContainerView()
                    .isScreenPresented()
                    .tapActivateButton()
                    .isPaymentMethodsScreenPresented()
            },
            specificCardTests: { _ in }
        )
    }

    func testProceedToVASTurboPaymentFromSnippet() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .turbo)

        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                snippetSteps
                    .isTurboContainerViewTappable()
                    .tapTurboActivateButton()
                    .isPaymentMethodsScreenPresented()
            },
            specificCardTests: { _ in }
        )
    }

    func testProceedToVASPremiumPaymentFromSnippet() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .premium)

        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                snippetSteps
                    .isPremiumContainerViewTappable()
                    .tapPremiumActivateButton()
                    .isPaymentMethodsScreenPresented()
            },
            specificCardTests: { _ in }
        )
    }

    func testProceedToVASRaisingPaymentFromSnippet() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .raising)

        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                snippetSteps
                    .isRaisingContainerViewTappable()
                    .tapRaisingActivateButton()
                    .isPaymentMethodsScreenPresented()
            },
            specificCardTests: { _ in }
        )
    }

    func testProceedToVASPromotionPaymentFromSnippet() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .promotion)

        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                snippetSteps
                    .isPromotionContainerViewTappable()
                    .tapPromotionActivateButton()
                    .isPaymentMethodsScreenPresented()
            },
            specificCardTests: { _ in }
        )
    }
    
    func testProceedToVASTurboPaymentViaCard() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .turbo)

        self.relaunchApp(with: .userOfferTests)
        
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()
        
        UserOffersListSteps()
            .isScreenPresented()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .openCard()
            .isScreenPresented()
            .activateOptionTurbo()
            .isPaymentMethodsScreenPresented()
    }
    
    func testProceedToVASTurboPaymentViaTurboOptionScreen() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .turbo)

        self.relaunchApp(with: .userOfferTests)
        
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()
        
        UserOffersListSteps()
            .isScreenPresented()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .openCard()
            .isScreenPresented()
            .openTurboScreen()
            .tapActivateButton()
            .isPaymentMethodsScreenPresented()
    }

    func testProceedToVASPremiumPaymentViaCard() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .premium)

        self.relaunchApp(with: .userOfferTests)
        
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()
        
        UserOffersListSteps()
            .isScreenPresented()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .openCard()
            .isScreenPresented()
            .activateOptionPremium()
            .isPaymentMethodsScreenPresented()
    }
    
    func testProceedToVASPremiumPaymentViaPremiumOptionScreen() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .premium)

        self.relaunchApp(with: .userOfferTests)
        
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()
        
        UserOffersListSteps()
            .isScreenPresented()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .openCard()
            .isScreenPresented()
            .openPremiumScreen()
            .tapActivateButton()
            .isPaymentMethodsScreenPresented()
    }
    
    func testProceedToVASRaisingPaymentViaCard() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .raising)

        self.relaunchApp(with: .userOfferTests)
        
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()
        
        UserOffersListSteps()
            .isScreenPresented()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .openCard()
            .isScreenPresented()
            .activateOptionRaising()
            .isPaymentMethodsScreenPresented()
    }
    
    func testProceedToVASRaisingPaymentViaRaisingOptionScreen() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .raising)

        self.relaunchApp(with: .userOfferTests)
        
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()
        
        UserOffersListSteps()
            .isScreenPresented()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .openCard()
            .isScreenPresented()
            .openRaisingScreen()
            .tapActivateButton()
            .isPaymentMethodsScreenPresented()
    }
    
    func testProceedToVASPromotionPaymentViaCard() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .promotion)

        self.relaunchApp(with: .userOfferTests)
        
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()
        
        UserOffersListSteps()
            .isScreenPresented()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .openCard()
            .isScreenPresented()
            .activateOptionPromotion()
            .isPaymentMethodsScreenPresented()
    }
    
    func testProceedToVASPromotionPaymentViaPromotionOptionScreen() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupUserOffersProducts(using: self.dynamicStubs, stubKind: .promotion)

        self.relaunchApp(with: .userOfferTests)
        
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()
        
        UserOffersListSteps()
            .isScreenPresented()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .openCard()
            .isScreenPresented()
            .openPromotionScreen()
            .tapActivateButton()
            .isPaymentMethodsScreenPresented()
    }
    
    func testShouldBeAutopaymentVASRaising() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .allInactivate)
        UserOffersAPIStubConfigurator.setupVASAutoPurchaseRaising(using: self.dynamicStubs, stubKind: .raisingActivate)
        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                let VASActivationSteps = snippetSteps
                    .tapRaisingAutoPurchaseSwitch()
                VASActivationSteps
                    .isScreenPresented()
                self.compareWithScreenshot(identifier: "userOffers.offerList.VASActivation.view.raising")
                VASActivationSteps
                    .tapAcceptButton()
                self.compareWithScreenshot(identifier: "userOffers.offerList.view.activeRaising")
            },
            specificCardTests: { _ in }
        )
    }

    func testShouldBeAutopaymentVASPremium() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .allInactivate)
        UserOffersAPIStubConfigurator.setupVASAutoPurchasePremium(using: self.dynamicStubs, stubKind: .premiumActivate)
        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                let VASActivationSteps = snippetSteps
                    .tapPremiumAutoPurchaseSwitch()
                VASActivationSteps
                    .isScreenPresented()
                self.compareWithScreenshot(identifier: "userOffers.offerList.VASActivation.view.premium")
                VASActivationSteps
                    .tapAcceptButton()
                self.compareWithScreenshot(identifier: "userOffers.offerList.view.activePremium")
            },
            specificCardTests: { _ in }
        )
    }

    func testShouldBeAutopaymentVASPromotion() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .allInactivate)
        UserOffersAPIStubConfigurator.setupVASAutoPurchasePromotion(using: self.dynamicStubs, stubKind: .promotionActivate)
        self.performCommonTests(
            specificSnippetTests: { snippetSteps in
                let VASActivationSteps = snippetSteps
                    .tapPromotionAutoPurchaseSwitch()
                VASActivationSteps
                    .isScreenPresented()
                self.compareWithScreenshot(identifier: "userOffers.offerList.VASActivation.view.promotion")
                VASActivationSteps
                    .tapAcceptButton()
                self.compareWithScreenshot(identifier: "userOffers.offerList.view.activePromotion")
            },
            specificCardTests: { _ in }
        )
    }

    func testDiscountVAS() {
        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .discount)
        self.performCommonTests { snippetSteps in
            self.runActivity(#"Проверяем скидки на сниппете пакета "Турбо" и услуги "Поднятие""#) {
                snippetSteps
                    .compareTurboViewWithSnapshot(identifier: "turboContainer.discount")
                    .compareRaisingViewWithSnapshot(identifier: "raisingContainer.discount")
            }

            self.runActivity(#"Проверяем скидку на экране с информацией об услуге "Поднятие""#) {
                snippetSteps
                    .tapRaisingContainerView()
                    .isScreenPresented()
                    .compareViewWithScreenshot(identifier: "userOffers.productInfo.view.promotion.discount")
                    .tapOnCloseButton()
            }
        }


        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .free)
        self.performCommonTests { snippetSteps in
            self.runActivity(#"Проверяем сниппет бесплатного пакета "Турбо" и услуги "Премиум""#) {
                snippetSteps
                    .compareTurboViewWithSnapshot(identifier: "turboContainer.free")
                    .comparePremiumViewWithSnapshot(identifier: "premiumContainer.free")
            }

            self.runActivity(#"Проверяем бесплатную услугу "Премиум" на экране с информацией о "Премиум"#) {
                snippetSteps
                    .tapPremiumContainerView()
                    .isScreenPresented()
                    .compareViewWithScreenshot(identifier: "userOffers.productInfo.view.premium.free")
                    .tapOnCloseButton()
            }
        }


        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .activatedTurboDiscount)
        self.performCommonTests { snippetSteps in
            self.runActivity(#"Проверяем скидку на сниппете активированного пакета "Турбо""#) {
                snippetSteps
                    .compareActivatedTurboViewWithSnapshot(identifier: "turboContainer.activated.discount")
            }

            self.runActivity(#"Проверяем скидку на экране с информацией о пакете "Турбо""#) {
                snippetSteps
                    .tapActivatedTurboContainerView()
                    .isScreenPresented()
                    .compareViewWithScreenshot(identifier: "userOffers.productInfo.view.turbo.discount")
                    .tapOnCloseButton()
            }
        }


        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .activatedTurboFree)
        self.performCommonTests { snippetSteps in
            self.runActivity(#"Проверяем сниппет бесплатного активированного пакета "Турбо""#) {
                snippetSteps
                    .compareActivatedTurboViewWithSnapshot(identifier: "turboContainer.activated.free")
            }
        }


        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .renewalTurboDiscount)
        self.performCommonTests { snippetSteps in
            self.runActivity(#"Проверяем скидку на сниппете активированного пакета "Турбо" с автопродлением"#) {
                snippetSteps
                    .compareActivatedTurboViewWithSnapshot(identifier: "turboContainer.renewal.discount")
            }
        }


        UserOffersAPIStubConfigurator.setupVASUserOffersList(using: self.dynamicStubs, stubKind: .renewalTurboFree)
        self.performCommonTests { snippetSteps in
            self.runActivity(#"Проверяем бесплатный активированный пакет "Турбо" с автопродлением"#) {
                snippetSteps
                    .compareActivatedTurboViewWithSnapshot(identifier: "turboContainer.renewal.free")
            }
        }
    }


    // MARK: - Publishing only

    private func performCommonTests(specificSnippetTests: (UserOfferSnippetSteps) -> Void,
                                    specificCardTests: (UserOffersCardSteps) -> Void = { _ in }) {
        self.relaunchApp(with: .userOfferTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        let userOffersList = UserOffersListSteps()
        userOffersList
            .isScreenPresented()
            .isListNonEmpty()

        let cellSteps = userOffersList.cell(withIndex: 0)
        specificSnippetTests(cellSteps)
//         TODO: 6 point from https://st.yandex-team.ru/VSAPPS-6372#5fe8f4edcc91ac793f1eca2e
//        cellSteps.tap()
//
//        let userOfferCard = UserOffersCardSteps()
//        userOfferCard.isScreenPresented()
//        specificCardTests(userOfferCard)
//        userOfferCard.openOfferPreview()
//
//        let previewOfferCard = OfferCardSteps()
//        previewOfferCard
//            .isOfferCardPresented()
//            .isCallButtonNotExists()
    }

    // @l-saveliy: on payment screen we have header that can't be identified by accessibilityID
    // This header appears with minor delay on screen. Wait it for a sec and then screenshot whole screen
    private func waitScreenUpdateAndScreenshot(identifier: String) {
        sleep(1)
        self.compareWithScreenshot(identifier: identifier)
    }
}
