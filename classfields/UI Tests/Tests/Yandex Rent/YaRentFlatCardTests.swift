//
//  YaRentFlatCardTests.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 28.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class YaRentFlatCardTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
    }

    func testFlatHeaderChanging() {
        RentAPIStubConfiguration.setupOwnerFlatWithNotifications(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatCard()
            .ensureNavigationTitle(isVisible: false)
            .ensureFlatHeader(isVisible: true)
            .scroll(swipe: .up)
            .ensureNavigationTitle(isVisible: true)
            .ensureFlatHeader(isVisible: false)
    }

    func testShowingFallbackForOwner() {
        RentAPIStubConfiguration.setupOwnerFlatWithFallbacks(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatCard()

        YaRentFlatNotificationSteps(.fallback)
            .isPresented()

        YaRentFlatNotificationSteps(.appUpdate)
            .isPresented()
    }

    func testAddOwnerINN() {
        RentAPIStubConfiguration.setupOwnerFlatWithUserNotifications(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupPatchINN(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatCard()

        YaRentFlatNotificationSteps(.ownerWithoutINN)
            .isPresented()
            .action()

        AddOwnerINNSteps()
            .isScreenPresented()
            .submitINN()
            .isErrorShown(with: "Пожалуйста, укажите ИНН")
            .enterText(text: "012345678912")
            .isErrorHidden()
            .submitINN()
            .isErrorShown(with: "Неверно указан ИНН")
            .enterText(text: "500100732259")
            .submitINN()
            .isScreenNotPresented()
    }

    func testOwnerKeysHandedOverToManager() {
        RentAPIStubConfiguration.setupGetDownloadUrl(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupOwnerFlatWithKeysHandedOverToManager(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatCard()

        YaRentFlatNotificationSteps(.ownerKeysHandedOverToManager)
            .isPresented()
            .action()

        DocumentPreviewSteps()
            .isScreenPresented()
    }

    func testOwnerPaymentInfoTodoNotification() {
        RentAPIStubConfiguration.setupOwnerFlatWithUserNotifications(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatCard()

        YaRentFlatTodoNotificationSteps()
            .isPresented(type: .ownerPaymentInfoTodo)
            .isTodoItemDone(inItemAt: 0)
            .isErrorDescriptionNotExists(inItemAt: 1)
            .isTodoItemWaitingForAction(inItemAt: 1)
            .tapOnActionButton(inItemAt: 1)

        AddOwnerINNSteps()
            .isScreenPresented()
    }

    func testOwnerConfirmedTodoNotification() {
        RentAPIStubConfiguration.setupOwnerFlatWithConfirmedTodo(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatCard()

        let ownerConfirmedTodo = YaRentFlatTodoNotificationSteps()
            .isPresented(type: .ownerConfirmedTodo)

        ownerConfirmedTodo
            .isTodoItemDone(inItemAt: 0)
            .isErrorDescriptionNotExists(inItemAt: 1)
            .isTodoItemWaitingForAction(inItemAt: 1)
            .tapOnActionButton(inItemAt: 1)

        WebPageSteps()
            .screenIsPresented()
            .tapOnCloseButton()

        ownerConfirmedTodo
            .isErrorDescriptionExists(inItemAt: 2)
            .isTodoItemWaitingForAction(inItemAt: 2)
            .tapOnActionButton(inItemAt: 2)
    }

    func testOwnerFlatInDraft() {
        RentAPIStubConfiguration.setupOwnerFlatWithDraft(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        self.openFlatCard {
            $0.ensureFlatStatus("черновик анкеты")
        }

        YaRentFlatNotificationSteps(.ownerDraftNeedToFinish)
            .isPresented()
            .action()

        YaRentOwnerApplicationSteps()
            .isScreenPresented()
            .tapOnCloseButton()
            .isScreenNotPresented()
    }

    func testOwnerFlatInWaitingForConfirmation() {
        RentAPIStubConfiguration.setupOwnerFlatWithWaitingForConfirmation(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        self.openFlatCard {
            $0.ensureFlatStatus("черновик анкеты")
        }

        YaRentFlatNotificationSteps(.ownerDraftNeedConfirmation)
            .isPresented()
            .action()

        YaRentOwnerApplicationSteps()
            .isScreenPresented()
            .tapOnCloseButton()
            .isScreenNotPresented()
    }

    func testOwnerFlatInConfirmed() {
        RentAPIStubConfiguration.setupOwnerFlatWithConfirmedDraft(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        self.openFlatCard {
            $0.ensureFlatStatus("заявка подтверждена")
        }

        YaRentFlatNotificationSteps(.ownerWaitingForArendaTeamContact)
            .isPresented()

        YaRentFlatNotificationSteps(.ownerPrepareFlatForMeeting)
            .isPresented()

        YaRentFlatTodoNotificationSteps()
            .isPresented(type: .ownerConfirmedTodo)
    }

    func testOwnerFlatInWorkInProgress() {
        RentAPIStubConfiguration.setupOwnerFlatWithWorkInProgress(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        self.openFlatCard {
            $0.ensureFlatStatus("принята в работу")
        }

        YaRentFlatNotificationSteps(.ownerKeysStillWithYou)
            .isPresented()

        YaRentFlatNotificationSteps(.ownerPreparingFlatForExposition)
            .isPresented()
    }

    func testOwnerFlatInLookingForTenant() {
        RentAPIStubConfiguration.setupOwnerFlatWithLookingForTenant(using: self.dynamicStubs)

        // Setted up for `ownerLookingForTenants` notification CTA
        OfferCardAPIStubConfiguration.setupOfferCard(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        self.openFlatCard {
            $0.ensureFlatStatus("ищем жильцов")
        }

        YaRentFlatNotificationSteps(.ownerCheckTenantCandidates)
            .isPresented()
            .action()

        WebPageSteps()
            .screenIsPresented()
            .tapOnCloseButton()
            .screenIsDismissed()

        YaRentFlatNotificationSteps(.ownerLookingForTenants)
            .isPresented()
            .action()

        OfferCardSteps()
            .isOfferCardPresented()
    }

    func testOwnerFlatInDenied() {
        RentAPIStubConfiguration.setupOwnerFlatWithDenied(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        self.openFlatCard {
            $0.ensureFlatStatus("отказались от сотрудничества")
        }

        YaRentFlatNotificationSteps(.ownerRequestCanceled)
            .isPresented()
            .action()

        YaRentOwnerApplicationSteps()
            .isScreenPresented()
            .tapOnCloseButton()
            .isScreenNotPresented()
    }

    func testOwnerFlatInCanceled() {
        RentAPIStubConfiguration.setupOwnerFlatWithCanceled(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        self.openFlatCard {
            $0.ensureFlatStatus("заявка отклонена")
        }

        YaRentFlatNotificationSteps(.ownerRequestDeclined)
            .isPresented()
    }

    func testOwnerFlatInAfterRent() {
        RentAPIStubConfiguration.setupOwnerFlatWithAfterRent(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        self.openFlatCard {
            $0.ensureFlatStatus("аренда окончена")
        }

        YaRentFlatNotificationSteps(.ownerRentOver)
            .isPresented()
            .action()

        YaRentOwnerApplicationSteps()
            .isScreenPresented()
            .tapOnCloseButton()
            .isScreenNotPresented()
    }

    func testFlatInsurance() {
        RentAPIStubConfiguration.setupOwnerFlatWithInsurance(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatCard {
            $0.ensureInsurancePopupShowing()
              .closePopup()

            $0.ensureFlatStatus("сдана")
              .ensureInsuranceBadgeExists()
              .tapInsuranceBadge()
              .ensureInsurancePopupShowing()
              .tapInsurancePopupActionButton()
        }

        SafariSteps()
            .isOpened()
    }

    // Doesn't work at all. Something wrong with bank card list api pathes
    // https://st.yandex-team.ru/VSAPPS-8876
    func disabled_testRentOwnerWithManyCardsNotification() {
        APIStubConfigurator.setupLegalEntityUser(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupUserWithOwner(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupOwnerFlatWithUserNotifications(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupOwnerCards(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatCard()

        YaRentFlatNotificationSteps(.ownerWithManyCards)
            .isPresented()
            .action()

        BankCardsListSteps()
            .screenIsPresented()
            .bankCardsListIsPresented(timeout: Constants.timeout)
            .bankCardCell(containing: "0017")
            .cellIsPresented()

        BankCardsListSteps()
            .bankCardCell(containing: "0018")
            .cellIsPresented()
    }

    func testNotificationSheetWithLink() {
        RentAPIStubConfiguration.setupFlatWithNextPayment(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatCard()

        YaRentFlatNotificationSteps(.tenantFirstPayment)
            .isPresented()
            .isSheetPresented()
            .tapOnSheetLink()

        SafariSteps()
            .isOpened()
    }

    func testTenantRentEndedNotification() {
        APIStubConfigurator.setupOfferListDeeplink_YaRent_Moscow(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)
        APIStubConfigurator.setupSiteSearchResultsList_MoscowAndMO(using: self.dynamicStubs)

        OfferCardAPIStubConfiguration.setupOfferSearchResultsListYandexRent(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupTenantFlatWithRentEnded(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatCard()

        YaRentFlatNotificationSteps(.tenantRentEnded)
            .isPresented()
            .action()

        SearchResultsListSteps()
            .isScreenPresented()
            .withOfferList()
            .isListNonEmpty()
            .filterPromoBanner(kind: .yandexRent)
            .isPresented()
    }

    // MARK: - Private

    @discardableResult
    private func openFlatCard(
        _ cardActions: ((YaRentFlatCardSteps) -> Void)? = nil
    ) -> YaRentFlatCardSteps {
        let servicesSteps = InAppServicesSteps()

        servicesSteps
            .isScreenPresented()
            .isContentPresented()
            .tapOnFirstFlat()

        let flatSteps = YaRentFlatCardSteps()

        flatSteps
            .isScreenPresented()
            .isContentLoaded()

        cardActions?(flatSteps)

        return flatSteps
    }
}
