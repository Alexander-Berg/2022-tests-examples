import XCTest

/// @depends_on AutoRuCalls AutoRuCallsUI AutoRuCallStrategies AutoRuCallsCore
final class App2AppCallsTests: BackendStatefulTests {
    func testOutgoingCallUnavailableWithHideOptionExp() {
        state.experiments.add(exp: BackendState.Experiments.HideApp2AppCallOption())

        checkUnavailableCall()
    }

    private let disabledApp2AppUserDefaultsSettings: [String: Any] = ["app2appCallsAreEnabled": false]
    private let enabledApp2AppUserDefaultsSettings: [String: Any] = ["app2appCallsAreEnabled": true]

    func testOutgoingCallAvailableWithInOutExp() {
        launchMain(options: .init(overrideAppSettings: disabledApp2AppUserDefaultsSettings))
            .toggle(to: \.favorites)
            .focus(on: .offer(.alias(.bmw3g20)), ofType: .offerSnippet) { cell in
                cell.tap(.callButton)
            }
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker
                    .should(.app2AppAudioOption, .exist)
                    .validateSnapshot()
            }
    }

    func testOutgoingCallUnavailableForUnauthorizedUser() {
        state.user.authorized = false
        checkUnavailableCall()
    }

    func testOutgoingCallAvailableFromOfferScreen() {
        state.search.listing = [.bmw3g20]

        launchMain(options: .init(overrideAppSettings: disabledApp2AppUserDefaultsSettings))
            .tap(.filterParametersButton)
            .should(provider: .filtersScreen, .exist)
            .focus { screen in
                screen.tap(.searchButton)
            }
            .should(provider: .saleListScreen, .exist)
            .focus { screen in
                screen.tap(.offerCell(.alias(.bmw3g20)))
            }
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen.focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) {
                    $0.tap(.callButton)
                }
            }
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker
                    .should(.app2AppAudioOption, .exist)
                    .validateSnapshot()
            }
    }

    func testStartApp2AppCallFromOfferScreenIfSettingEnabled() {
        state.search.listing = [.bmw3g20]

        launchMain(
            options: .init(
                overrideAppSettings: enabledApp2AppUserDefaultsSettings,
                environment: ["app2AppFakeMicStatus": "authorized"]
            )
        )
            .tap(.filterParametersButton)
            .should(provider: .filtersScreen, .exist)
            .focus { screen in
                screen.tap(.searchButton)
            }
            .should(provider: .saleListScreen, .exist)
            .focus { screen in
                screen.tap(.offerCell(.alias(.bmw3g20)))
            }
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen.focus(on: .bottomButtonsContainer, ofType: .saleCardBottomContainer) {
                    $0.tap(.callButton)
                }
            }
            .should(provider: .callScreen, .exist)
    }

    func testOutgoingCallAvailableFromListingOfferSnippetCallButtonInGallery() {
        state.search.listing = [.bmw3g20]

        launchMain(options: .init(overrideAppSettings: disabledApp2AppUserDefaultsSettings))
            .tap(.filterParametersButton)
            .should(provider: .filtersScreen, .exist)
            .focus { screen in
                screen.tap(.searchButton)
            }
            .should(provider: .saleListScreen, .exist)
            .focus { screen in
                screen.focus(on: .offerCell(.alias(.bmw3g20), .gallery), ofType: .offerSnippet) { cell in
                    cell
                        .scroll(to: .callButtonInPhotos, direction: .left)
                        .focus { $0.tap() }
                }
            }
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker
                    .should(.app2AppAudioOption, .exist)
                    .validateSnapshot()
            }
    }

    func testOutgoingCallAvailableFromListingOfferSnippetCallButton() {
        state.search.listing = [.bmw3g20]
        state.modifiers.addOfferModifier(for: .bmw3g20) { offer, _ in
            offer.services = [.with { msg in
                msg.service = "all_sale_toplist"
                msg.isActive = true
            }]
        }

        launchMain(options: .init(overrideAppSettings: disabledApp2AppUserDefaultsSettings))
            .tap(.filterParametersButton)
            .should(provider: .filtersScreen, .exist)
            .focus { screen in
                screen.tap(.searchButton)
            }
            .should(provider: .saleListScreen, .exist)
            .focus { screen in
                screen.scroll(to: .offerCell(.alias(.bmw3g20)), ofType: .offerSnippet)
                    .focus { screen in
                        screen.tap(.callButton)
                    }
            }
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker
                    .should(.app2AppAudioOption, .exist)
                    .validateSnapshot()
            }
    }

    func testOutgoingCallAvailableFromGallery() {
        state.search.listing = [.bmw3g20]

        launchMain(options: .init(overrideAppSettings: disabledApp2AppUserDefaultsSettings))
            .tap(.filterParametersButton)
            .should(provider: .filtersScreen, .exist)
            .focus { screen in
                screen.tap(.searchButton)
            }
            .should(provider: .saleListScreen, .exist)
            .focus { screen in
                screen.tap(.offerCell(.alias(.bmw3g20)))
            }
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .should(.images, .exist)
                    .focus { $0.tap("0") }
            }
            .should(provider: .galleryScreen, .exist)
            .focus { gallery in
                gallery.tap(.callButton)
            }
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker
                    .should(.app2AppAudioOption, .exist)
                    .validateSnapshot()
            }
    }

    func testOutgoingCallAvailableFromChat() {
        launchMain(options: .init(overrideAppSettings: disabledApp2AppUserDefaultsSettings))
            .toggle(to: \.chats)
            .tap(.chatRoom(id: BackendState.Chats.chatWithBmw3g20.id.rawValue))
            .should(provider: .chatScreen, .exist)
            .focus { screen in
                screen.tap(.callButton)
            }
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker
                    .should(.app2AppAudioOption, .exist)
                    .validateSnapshot()
            }
    }

    func testOutgoingCallAvailableFromFullReport() {
        launchMain(options: .init(overrideAppSettings: disabledApp2AppUserDefaultsSettings))
            .toggle(to: \.favorites)
            .scroll(to: .offer(.alias(.bmw3g20)), ofType: .offerSnippet)
            .focus { $0.tap() }
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .scroll(to: .fullReportButton)
                    .tap(.fullReportButton)
            }
            .should(provider: .carReportScreen, .exist)
            .focus { screen in
                screen.tap(.callButton)
            }
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker
                    .should(.app2AppAudioOption, .exist)
                    .validateSnapshot()
            }
    }

    private func checkUnavailableCall() {
        launchMain()
            .toggle(to: \.favorites)
            .scroll(to: .offer(.alias(.bmw3g20)), ofType: .offerSnippet)
            .focus { cell in
                cell.tap(.callButton)
            }
            .should(provider: .callOptionPicker, .exist)
            .focus { picker in
                picker
                    .should(.app2AppAudioOption, .be(.hidden))
                    .validateSnapshot()
            }
    }
}
