import XCTest
import AutoRuProtoModels

/// @depends_on AutoRuSafeDeal
final class SafeDealListTests: BaseTest {
    private static let dealOneID = "01675384-a256-4f61-92b6-db9d6e4c9fbd"
    private static let dealTwoID = "be52cd13-dfa0-46b2-bfd9-709e8707d7a8"
    private static let offerID = "265829954274310832-e92c7e2b"

    override func setUp() {
        super.setUp()

        setupServer()
    }

    func test_cancelSafeDealBySeller() {
        let rejectExpectation = api.safeDeal.deal.update
            .dealId(Self.dealOneID)
            .post(parameters: .wildcard)
            .expect { req, _ in
                .okIf(req.bySeller.cancelDealWithReason.cancelReason == .sellerAnotherReason)
            }

        openSafeDeals()
            .wait(for: 1)
            .focus(
                on: .deal(Self.dealOneID),
                ofType: .safeDealCell
            ) { offer in
                offer
                    .tap(.decline)
            }
            .should(provider: .safeDealRequestCancelPopup, .exist)
            .focus { popup in
                popup.tap(.reason("Другое"))
            }
            .should(provider: .textReasonPopup, .exist)
            .focus { popup in
                popup
                    .tap(.textView)
                    .type("Другая причина")
                    .tap(.confirm)
            }
            .should(provider: .safeDealRequestCancelPopup, .be(.hidden))
            .wait(for: [rejectExpectation])
    }

    func test_cancelSafeDealByBuyer() {
        let rejectExpectation = api.safeDeal.deal.update
            .dealId(Self.dealTwoID)
            .post(parameters: .wildcard)
            .expect { req, _ in
                .okIf(req.byBuyer.cancelDealWithReason.cancelReason == .buyerBecauseInactiveSeller)
            }

        openSafeDeals()
            .wait(for: 1)
            .focus(
                on: .deal(Self.dealTwoID),
                ofType: .safeDealCell
            ) { offer in
                offer
                    .tap(.cancel)
            }
            .should(provider: .safeDealRequestCancelPopup, .exist)
            .focus { popup in
                popup.tap(.reason("Продавец бездействует"))
            }
            .should(provider: .safeDealRequestCancelPopup, .be(.hidden))
            .wait(for: [rejectExpectation])
    }

    private func openSafeDeals() -> SafeDealListScreen {
        launch(on: .mainScreen) { screen in
            screen
                .focus(on: .tabBar, ofType: .tabBar) {
                    $0.tap(.tab(.garage))
                }
                .should(provider: .garageScreen, .exist)
                .should(provider: .navBar, .exist)
                .focus { $0.tap(.superMenuButton) }
                .should(provider: .superMenuScreen, .exist)
                .focus {
                    $0.tap(.safeDeal)
                }
                .should(provider: .safeDealListScreen, .exist)
        }
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .mock_garageListingEmpty()
            .mock_safeDealList()
            .mock_safeDealCancel(dealID: Self.dealOneID, offerID: Self.offerID)
            .mock_safeDealCancel(dealID: Self.dealTwoID, offerID: Self.offerID)

        mocker.startMock()
    }
}
