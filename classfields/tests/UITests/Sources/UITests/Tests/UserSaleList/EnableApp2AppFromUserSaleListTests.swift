import XCTest
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuUserSaleList
final class EnableApp2AppFromUserSaleListTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    var offer: Auto_Api_Offer!
    var listingResponse: Auto_Api_OfferListingResponse!

    override func setUp() {
        super.setUp()

        listingResponse = Auto_Api_OfferListingResponse.fromFile(named: "GET user_offers_all")
        listingResponse.offers[0].seller.redirectPhones = true
        listingResponse.offers[0].additionalInfo.app2AppCallsDisabled = true
        offer = listingResponse.offers[0]
    }

    func test_enableApp2AppWhenMicWasDenied() {
        setupServer()

        launchMain(options: .init(environment: ["app2AppFakeMicStatus": "denied"]))
            .toggle(to: \.offers)
            .scroll(to: .enableApp2App(offerID: offer.id), ofType: .switchCell)
            .focus { cell in
                cell
                    .should(.switch, .be(.off))
                    .validateSnapshot(snapshotId: "app2appDisabled")
                    .tap(.switch)
            }
            .should(provider: .microphonePermissionScreen, .exist)
            .focus { screen in
                screen
                    .should(.description(.forSeller), .exist)
                    .tap(.settingsButton)
            }
    }

    func test_enableApp2AppWhenMicWasAuthorized() {
        setupServer()

        launchMain(options: .init(environment: ["app2AppFakeMicStatus": "authorized"]))
            .toggle(to: \.offers)
            .scroll(to: .enableApp2App(offerID: offer.id), ofType: .switchCell)
            .focus { cell in
                cell
                    .should(.switch, .be(.off))
                    .validateSnapshot(snapshotId: "app2appDisabled")
                    .tap(.switch)
                    .should(.switch, .be(.on))
                    .validateSnapshot(snapshotId: "app2appEnabled")
            }
            .should(provider: .microphonePermissionScreen, .be(.hidden))
    }

    func test_enableApp2AppWhenMicWasNotDetermined() {
        setupServer()

        launchMain(options: .init(environment: ["app2AppFakeMicStatus": "notDetermined"]))
            .toggle(to: \.offers)
            .scroll(to: .enableApp2App(offerID: offer.id), ofType: .switchCell)
            .focus { cell in
                cell
                    .should(.switch, .be(.off))
                    .validateSnapshot(snapshotId: "app2appDisabled")
                    .tap(.switch)
            }
            .should(provider: .systemAlert, .exist)
            .focus {
                $0.tap(.button("OK"))
            }
    }

    func test_enableApp2AppShouldBeHiddenIfRedirectPhonesIsDisabled() {
        for index in listingResponse.offers.indices {
            listingResponse.offers[index].seller.redirectPhones = false
        }

        setupServer()

        launchMain()
            .toggle(to: \.offers)
            .scroll(to: .offer(id: offer.id))
            .should(.enableApp2App(offerID: offer.id), .be(.hidden))
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()

        defer { mocker.startMock() }

        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.App2AppAskMic())

        api.device.hello.post.ok(mock: experiments.toMockSource())

        let draftID = "draftID1"

        let userOffers = listingResponse!
        let offer = self.offer!

        api.user.offers.category(._unknown("all")).get(parameters: .wildcard)
            .ok(mock: .model(userOffers))

        api.user.offers.category(.cars).offerID(offer.id).edit.post
            .ok(mock: .model())

        api.user.draft.category(.cars).offerId(draftID).get
            .ok(mock: .dynamic { _, _ in
                return .with { msg in
                    msg.status = .success
                    msg.offer = offer
                    msg.offerID = draftID
                    msg.offer.id = draftID
                }
            })

        api.user.draft.category(.cars).offerId(draftID).put
            .ok(mock: .dynamic { _, request in
                return .with { msg in
                    msg.status = .success
                    msg.offer = offer
                    msg.offerID = draftID
                    msg.offer.id = draftID
                    msg.offer.additionalInfo.app2AppCallsDisabled = false
                }
            })

        let putDraftExpectation = api.user.draft.category(.cars).offerId(draftID).put
            .expect { offer, _ in
                offer.additionalInfo.app2AppCallsDisabled
                ? .fail(reason: "app2app should be enabled")
                : .ok
            }

        api.user.draft.category(.cars).offerId(draftID).publish.post(parameters: [])
            .ok(mock: .dynamic { _, request in
                return .with { msg in
                    msg.offer = offer
                    msg.offer.additionalInfo.app2AppCallsDisabled = false
                    msg.status = .success
                }
            })

        addTeardownBlock {
            _ = XCTWaiter.wait(for: [putDraftExpectation], timeout: 1)
        }
    }
}
