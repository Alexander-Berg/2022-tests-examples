import XCTest
import AutoRuProtoModels

final class UserNotesTests: BaseTest {
    private let testTextNote = "Текст заметки"
    private let offerId = "1098252972-99d8c274"
    private let alertShownConst = "user_note_mark_as_favorite_alert_was_shown"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    private func setupServer() {
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
    }

    // MARK: - Tests
    func testEnterAndSaveNote() {
        mocker
            .mock_offerFromHistoryLastAll()
            .mock_addOfferToFavorites(offerId: offerId)
            .mock_addUserNote(offerId: offerId)
            .startMock()

        let requestAddFavoriteWasCalled =
            self.expectationForRequest(method: "POST",
                                       uri: "/user/favorites/cars/\(offerId)")
        let requestAddNoteWasCalled =
            self.expectationForRequest(method: "PUT",
                                       uri: "/user/notes/cars/\(offerId)")

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)")))
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .focus(on: .actionButtons, ofType: .actionButtonsCell) { cell in
                        cell.tap(.note)
                    }
            }
            .should(provider: .userNoteSсreen, .exist)
            .focus { screen in
                screen
                    .tap(.note)
                    .type(testTextNote)
                    .tap(.save)
            }
            .should(provider: .userNoteAlert, .exist)
            .focus { alert in
                alert
                    .tap(.ok)
            }
        wait(for: [requestAddFavoriteWasCalled, requestAddNoteWasCalled], timeout: 1)
    }

    func testEditNote() {
        mocker
            .mock_userNotes()
            .mock_addUserNoteToOffer(testTextNote: testTextNote)
            .mock_userFavoriteOffers()
            .mock_addUserNote(offerId: offerId)
            .startMock()

        let requestAddNoteWasCalled = expectationForRequest(method: "PUT",
                                                            uri: "/user/notes/cars/\(offerId)")

        launchWithOptions()
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .focus(on: .userNote, ofType: .userNoteCell) { cell in
                        cell
                            .should(.userNoteText, .match(testTextNote))
                    }
                    .tap(.userNote)
            }
            .should(provider: .userNoteSсreen, .exist)
            .focus { screen in
                screen
                    .tap(.note)
                    .type(" новая")
                    .tap(.save)
            }
            .wait(for: 1)
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .focus(on: .userNote, ofType: .userNoteCell) { cell in
                        cell
                            .should(.userNoteText, .match("\(testTextNote)" + " новая"))
                    }
            }
        wait(for: [requestAddNoteWasCalled], timeout: 1)
    }

    func testDeleteNote() {
        mocker
            .mock_userNotes()
            .mock_addUserNoteToOffer(testTextNote: testTextNote)
            .mock_userFavoriteOffers()
            .mock_addUserNote(offerId: offerId)
            .startMock()

        let requestDeleteNoteWasCalled = expectationForRequest(method: "DELETE",
                                                               uri: "/user/notes/cars/\(offerId)")

        launchWithOptions()
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .focus(on: .userNote, ofType: .userNoteCell) { cell in
                        cell
                            .should(.userNoteText, .match(testTextNote))
                    }
                    .tap(.userNote)
            }
            .should(provider: .userNoteSсreen, .exist)
            .focus { screen in
                screen
                    .tap(.note)
                    .clearText(in: .note)
                    .tap(.save)
            }
            .should(provider: .saleCardScreen, .exist)
            .focus { screen in
                screen
                    .should(.userNote, .be(.hidden))
            }
        wait(for: [requestDeleteNoteWasCalled], timeout: 1)
    }

    private func launchWithOptions() -> TransportScreen {
        let options = AppLaunchOptions(launchType: .deeplink("https://auto.ru/cars/used/sale/\(offerId)"),
                                       userDefaults: [alertShownConst: true])
        return launch(on: .transportScreen, options: options)
    }
}
