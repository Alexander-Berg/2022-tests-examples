//
//  UserOfferPublishingTests.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 20.04.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

/// These tests pay no respect to the special cases like "Offer from feed" and "Pending placement"
/// because we have no UI controls and cannot publish/activate anything.
final class UserOfferPublishingTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
    }
}

// MARK: - Publishing only

/// Test free/quota/paid Unpublished UserOffers.
/// Here we test Free offers only, but the same logics is applicable to "Paid Unpublished" and "Quota Unpublished" offers.
/// See `UnpublishedUserOfferTests` for more details.
extension UserOfferPublishingTests {
    func testPublishFreeUnpublishedUserOfferFromList() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupPublishFreeUserOffer(using: self.dynamicStubs)

        self.relaunchApp(with: .userOfferTests)

        self.performCommonListTests(specificSteps: { userOffersList in
            userOffersList
                .isSuccessNotificationViewPresented()
                .isSuccessNotificationViewDismissedAfterDelay()
        })
    }

    func testPublishFreeUnpublishedUserOfferFromCard() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupPublishFreeUserOffer(using: self.dynamicStubs)

        self.relaunchApp(with: .userOfferTests)

        self.performCommonCardTests(specificSteps: { cardSteps in
            // We expect API response with a published Card after the publishing process.
            UserOffersAPIStubConfigurator.setupPublishedUserOffersCard(using: self.dynamicStubs, stubKind: .free)

            cardSteps
                .scrollToPublishButton()
                .tapOnPublishButton()

                .isSuccessNotificationViewPresented()
                .isSuccessNotificationViewDismissedAfterDelay()
        })
    }

    func testPublishUserOfferWithFailureFromList() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupPublishFailure(using: self.dynamicStubs)

        self.relaunchApp(with: .userOfferTests)

        self.performCommonListTests(specificSteps: { userOffersList in
            let errorAlert = userOffersList.errorAlert()
            errorAlert
                .screenIsPresented()
                .tapOnButton(withID: "Ок")
                .screenIsDismissed()
        })
    }

    func testPublishUserOfferWithFailureFromCard() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupPublishFailure(using: self.dynamicStubs)

        self.relaunchApp(with: .userOfferTests)

        self.performCommonCardTests(specificSteps: { cardSteps in
            cardSteps
                .scrollToPublishButton()
                .tapOnPublishButton()

            let errorAlert = cardSteps.errorAlert()
            errorAlert
                .screenIsPresented()
                .tapOnButton(withID: "Ок")
                .screenIsDismissed()
        })
    }
}

// MARK: - Activation

/// Test unpaid Unpublished/Published UserOffers.
/// Here we test Unpaid Unpublished offers only, but the same logics is applicable to "Unpaid Published" offers.
/// See `UnpublishedUserOfferTests` and `PublishedUserOfferTests` for more details.
extension UserOfferPublishingTests {
    func testActivateUnpaidUnpublishedUserOfferFromList() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .unpaid)
        UserOffersAPIStubConfigurator.setupPublishUnpaidUserOffer(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .unpaid)
        
        self.relaunchApp(with: .userOfferTests)

        self.performCommonListTests(specificSteps: { _ in
            let activationSteps = UserOfferActivationSteps()
            activationSteps.isScreenPresented()
        })
    }

    func testActivateUnpaidUnpublishedUserOfferFromCard() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .unpaid)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .unpaid)
        UserOffersAPIStubConfigurator.setupPublishUnpaidUserOffer(using: self.dynamicStubs)

        self.relaunchApp(with: .userOfferTests)

        self.performCommonCardTests(specificSteps: { cardSteps in
            cardSteps
                .tapOnActivateButton()

            let activationSteps = UserOfferActivationSteps()
            activationSteps.isScreenPresented()
        })
    }

    /// No test for Activation of a UserOffer with failure.
    /// There're two possible error cases:
    /// 1. cannot perform the `update_status` request
    /// 2. cannot retrieve a Card after that
    /// We check the 1st case in the `testPublishUserOfferWithFailure` method,
    /// we check the 2nd case in the `testActivateFreeUserOfferWithFailure` method.
    /// Assumed UI behavior is identical.
}

// MARK: - Activation corner cases

/// Test free/quota/paid Unpublished UserOffers.
/// Here we test Free offers only, but the same logics is applicable to "Paid Unpublished" and "Quota Unpublished" offers.
///
/// When we get a List or a Card with free/quota/paid UserOffer(s) we display a Publish button.
/// However sometimes after `update_status` request we get the `PAYMENT_REQUIRED` flag,
/// which means something has been happened with User's offers, so now User must pay to publish the offer.
///
/// Then we request a Card again (even from the List screen) and if it succeeded we display the Activation screen.
/// If the Card request fails we display a publishing error.
extension UserOfferPublishingTests {
    func testActivateFreeUserOfferFromList() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupPublishUnpaidUserOffer(using: self.dynamicStubs)

        // After publishing attempt the UserOffer becomes Unpaid - that's why it has a Placement product
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .unpaid)

        self.relaunchApp(with: .userOfferTests)

        self.performCommonListTests(specificSteps: { _ in
            let activationSteps = UserOfferActivationSteps()
            activationSteps.isScreenPresented()
        })
    }

    func testActivateFreeUserOfferFromCard() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupPublishUnpaidUserOffer(using: self.dynamicStubs)

        self.relaunchApp(with: .userOfferTests)

        self.performCommonCardTests(specificSteps: { cardSteps in
            // After publishing attempt the UserOffer becomes Unpaid - that's why it has a Placement product
            UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .unpaid)

            cardSteps
                .scrollToPublishButton()
                .tapOnPublishButton()

            let activationSteps = UserOfferActivationSteps()
            activationSteps.isScreenPresented()
        })
    }

    func testActivateFreeUserOfferWithFailure() {
        // Get a List with Free offer
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .free)
        // Get `update_status` response with `PAYMENT_REQUIRED` flag
        UserOffersAPIStubConfigurator.setupPublishUnpaidUserOffer(using: self.dynamicStubs)
        // Get a Card with error
        UserOffersAPIStubConfigurator.setupUserOfferCardFailure(using: self.dynamicStubs)

        self.relaunchApp(with: .userOfferTests)

        self.performCommonListTests(specificSteps: { userOffersList in
            let errorAlert = userOffersList.errorAlert()
            errorAlert
                .screenIsPresented()
                .tapOnButton(withID: "Ок")
                .screenIsDismissed()
        })
    }
}


// MARK: - Publishing for `legalEntity` user

/// Test the Activation case only - Publishing is the same as for `naturalPerson` (see all the tests above).
///
/// If we get `update_status` response with `PAYMENT_REQUIRED` flag, but a user's paymentType is `legalEntity`,
/// we cannot move user to an Activation screen (there's no way to pay), so assume the publishing was unsuccessful.
extension UserOfferPublishingTests {
    func testActivateUserOfferForLegalEntityFromList() {
        APIStubConfigurator.setupLegalEntityUser(using: self.dynamicStubs)

        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .unpaid)
        UserOffersAPIStubConfigurator.setupPublishUnpaidUserOffer(using: self.dynamicStubs)

        // Not required for test pass, but added to clarify the intent.
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .unpaid)

        self.relaunchApp(with: .userOfferTests)

        self.performCommonListTests(specificSteps: { userOffersList in
            let errorAlert = userOffersList.errorAlert()
            errorAlert
                .screenIsPresented()
                .tapOnButton(withID: "Ок")
                .screenIsDismissed()
        })
    }

    func testActivateUserOfferForLegalEntityFromCard() {
        APIStubConfigurator.setupLegalEntityUser(using: self.dynamicStubs)

        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .unpaid)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .unpaid)
        UserOffersAPIStubConfigurator.setupPublishUnpaidUserOffer(using: self.dynamicStubs)

        self.relaunchApp(with: .userOfferTests)

        self.performCommonCardTests(specificSteps: { cardSteps in
            cardSteps
                .tapOnActivateButton()

            let errorAlert = cardSteps.errorAlert()
            errorAlert
                .screenIsPresented()
                .tapOnButton(withID: "Ок")
                .screenIsDismissed()
        })
    }
}

// MARK: - Common

extension UserOfferPublishingTests {
    private func performCommonListTests(specificSteps: (UserOffersListSteps) -> Void) {
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        let userOffersList = UserOffersListSteps()
        userOffersList
            .isScreenPresented()

        let cellSteps = userOffersList.cell(withIndex: 0)
        cellSteps
            .isPublishButtonTappable()
            .tapOnPublishButton()

        specificSteps(userOffersList)
    }

    private func performCommonCardTests(specificSteps: (UserOffersCardSteps) -> Void) {
        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        let userOffersList = UserOffersListSteps()
        userOffersList.isScreenPresented()

        let cellSteps = userOffersList.cell(withIndex: 0)
        cellSteps.openCard()

        let userOfferCard = UserOffersCardSteps()
        userOfferCard.isScreenPresented()

        specificSteps(userOfferCard)
    }
}
