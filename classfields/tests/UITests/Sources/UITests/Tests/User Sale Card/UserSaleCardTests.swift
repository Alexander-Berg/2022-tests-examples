import XCTest
import AutoRuProtoModels
import SwiftProtobuf
import Foundation

/// @depends_on AutoRuUserSaleCard
final class UserSaleCardTests: BaseTest {
    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_cardContent() {
        var offer = makeOffer()

        offer.counters.favoriteTotalAll = 42

        offer.state.imageUrls.removeAll()
        offer.searchPosition = 11
        offer.additionalInfo.similarOffersCount = 999

        offer.counters.callsAll = 77
        offer.counters.all = 88

        offer.state.damages = [
            .with { damage in
                damage.carPart = .frontLeftDoor
                damage.type = [.dyed]
            },
            .with { damage in
                damage.carPart = .frontLeftMirror
                damage.type = [.scratch]
            }
        ]

        mockServer(with: offer)

        openUserSaleList(environment: ["MOCKED_DATE": "2022-06-15 15:00:00+03:00"])
            .tap(.offer(id: offer.id))
            .should(provider: .userSaleCardScreen, .exist)
            .focus { screen in
                screen
                    .should(.header, .exist)
                    .focus {
                        $0.validateSnapshot(snapshotId: "header")
                    }
                    .step("Проверяем открытие формы по заглушке фото") { $0
                        .tap(.imageStub)
                        .should(provider: .offerEditScreen, .exist)
                    }
                    .should(provider: .navBar, .exist)
                    .focus {
                        $0.tap(.close)
                    }

                screen
                    .should(.expirationCounters, .exist)
                    .focus { $0
                        .should("42 сохранили", .exist)
                        .should("45 дней до снятия", .exist)
                    }

                screen
                    .should(.statistics, .exist)
                    .focus {
                        $0.validateSnapshot(snapshotId: "statistics")
                    }

                screen
                    .scroll(to: .vas("VIP"), ofType: .userSaleCardOfferVAS)
                    .focus {
                        $0.tap(.buyButton)
                    }
                    .should(provider: .paymentOptionsScreen, .exist)
                    .focus {
                        $0.tap(.closeButton)
                    }

                screen
                    .scroll(to: .vas("Показ в Историях"))
                    .focus {
                        $0.tap()
                    }
                    .should(provider: .vasDescriptionContainerScreen, .exist)
                    .focus { $0
                        .should(.card("Показ в Историях"), ofType: .vasDescriptionCard, .exist)
                        .focus {
                            $0.tap(.purchaseButton)
                        }
                        .should(provider: .paymentOptionsScreen, .exist)
                        .focus {
                            $0.tap(.closeButton)
                        }
                        .tap(.closeButton)
                    }

                screen
                    .scroll(to: .characteristics)
                    .focus { $0
                        .validateSnapshot(snapshotId: "characteristics")
                        .tap(offer.documents.vin)
                    }
                    .should(provider: .paymentOptionsScreen, .exist)
                    .focus {
                        $0.tap(.closeButton)
                    }

                screen
                    .scroll(to: .damages)
                    .focus {
                        $0.validateSnapshot(snapshotId: "damages")
                    }

                screen
                    .scroll(to: .sellerInfo)
                    .scroll(to: .sellerAddress)
            }
    }

    func test_cardContent2() {
        var offer = makeOffer()
        offer.state.imageUrls = [
            .with { image in
                image.name = "autoru-vos:65714-ac6eef96e9cb6e1c3c3089722e35e474"
                image.sizes = [
                    "1200x900": "//images.mds-proxy.test.avto.ru/get-autoru-vos/65714/ac6eef96e9cb6e1c3c3089722e35e474/1200x900"
                ]
            }
        ]

        offer.tags.append("proven_owner")

        mockServer(with: offer)

        openUserSaleList(environment: ["MOCKED_DATE": "2022-06-15 15:00:00+03:00"])
            .tap(.offer(id: offer.id))
            .should(provider: .userSaleCardScreen, .exist)
            .focus { screen in
                screen
                    .tap(.shareButton)
                    .should(provider: .activityList, .exist)
                    .focus { list in
                        list
                            .tap(.copyButton)
                            .copyBufferContains("https://test.avto.ru/cars/used/sale/volvo/xc70/1514738848-a388d373/")
                    }

                screen
                    .tap(.editButton)
                    .should(provider: .offerEditScreen, .exist)
                    .should(provider: .navBar, .exist)
                    .focus {
                        $0.tap(.close)
                    }

                screen
                    .should(.addPanoramaBanner, .exist)
                    .tap(.closeAddPanoramaBannerButton)
                    .tap(.autoRuOnlyBadge)
                    .should(provider: .autoruOnlyPopupScreen, .exist)
                    .focus {
                        $0.tap(.closeButton)
                    }

                screen
                    .should(.advantages, .exist)
                    .focus {
                        $0.validateSnapshot(snapshotId: "advantages")
                    }

                screen
                    .should(.damages, .be(.hidden))
            }
    }

    func test_cardContentInactive() {
        var offer = makeOffer()
        offer.state.imageUrls = [
            .with { image in
                image.name = "autoru-vos:65714-ac6eef96e9cb6e1c3c3089722e35e474"
                image.sizes = [
                    "1200x900": "//images.mds-proxy.test.avto.ru/get-autoru-vos/65714/ac6eef96e9cb6e1c3c3089722e35e474/1200x900"
                ]
            }
        ]

        offer.status = .inactive

        mockServer(with: offer)

        openUserSaleList(environment: ["MOCKED_DATE": "2022-06-15 15:00:00+03:00"])
            .tap(.offer(id: offer.id))
            .should(provider: .userSaleCardScreen, .exist)
            .focus { screen in
                screen
                    .should(.expirationCounters, .exist)
                    .focus {
                        $0.should("Снято с продажи", .exist)
                    }
            }
    }

    func test_garagePromoBanner() {
        mocker
            .mock_userOffersWithGaragePromo()
            .mock_userOfferWithGaragePromo("16227978-01dc7292")
            .mock_garageListing()
            .mock_garageCard("16227978-01dc7292")

        openUserSaleList()
            .tap(.offer(id: "16227978-01dc7292"))
            .log("Проверяем наличие баннера гаража и тапаем на него")
            .wait(for: 2)
            .should(provider: .userSaleCardScreen, .exist)
            .focus { $0
                .scroll(to: .garagePromoBanner)
                .tap(.garagePromoBanner)
                .log("Проверяем, что открылась карточка гаража")
                .should(provider: .garageCardScreen, .exist)
                .log("Проверяем, что на карточке объявление баннер гаража больше не показывается")
            }

        MainSteps(context: self).openTab(.offers)
            .as(UserSaleListScreen.self)
            .tap(.offer(id: "16227978-01dc7292"))
            .log("Проверяем наличие баннера гаража и тапаем на него")
            .wait(for: 2)
            .should(provider: .userSaleCardScreen, .exist)
            .focus { $0
                .should(provider: .userSaleCardScreen, .exist)
                .should(.garagePromoBanner, .be(.hidden))
                .log("Проверяем, что в списке офферов баннер гаража больше не показывается")
                .should(provider: .navBar, .exist).focus { $0.tap(.back) }
            }
            .scroll(to: .garagePromoBanner)
            .should(.garagePromoBanner, .be(.hidden))

    }

    func test_deactivateWithReactivateLater() {
        let date = Calendar.current.date(byAdding: .day, value: 1, to: Date())!
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "EEEE, MMMM d"

        var formattedDate: [String] = []
        formattedDate.append(dateFormatter.string(from: date))

        dateFormatter.dateFormat = "EEEE, d MMMM"
        dateFormatter.locale = Locale(identifier: "ru_RU")
        formattedDate.append(dateFormatter.string(from: date))

        let offerID = "1103930878-40839e2d"
        let approveExpectation = api.user.offers.category(.cars)
            .offerID(offerID)
            .hide
            .post
            .expect()

        mocker
            .mock_userOffersVAS()
            .mock_userOfferHide(offerID)
            .mock_userOffer(id: offerID)

        openUserSaleList()
            .should(.activeSnippetActions(id: offerID), ofType: .userOfferActiveActionsSnippetCell, .exist)
            .focus(on: .activeSnippetActions(id: offerID), ofType: .userOfferActiveActionsSnippetCell) { snippet in
                snippet.tap(.disableButton)
            }
            .should(provider: .deactivateUserOfferAlert, .exist)
            .focus { alert in
                alert.tap(.reactivateLater)
            }
            .should(provider: .activateSaleLaterPopup, .exist)
            .focus { popup in
                popup.tap(.datePicker)
                popup.app.datePickers.buttons.matching(
                    NSPredicate(format: "label IN %@", ["Next Month", "Следующий месяц"])
                ).firstMatch.tap()
                popup.app.datePickers.buttons.matching(
                    NSPredicate(format: "label IN %@", ["Previous Month", "Предыдущий месяц"])
                ).firstMatch.tap()
                popup.wait(for: 2)
                popup.app.datePickers.buttons.matching(
                    NSPredicate(format: "label IN %@", formattedDate)
                ).firstMatch.tap()
                popup.tap(.confirmButton)
            }
            .wait(for: [approveExpectation], timeout: 10)
            .should(provider: .activateSaleLaterSuccessPopup, .exist)
            .focus { popup in
                popup
                    .tap(.confirmButton)
            }
    }

    // MARK: - Private
    private func openUserSaleList(environment: [String: String] = [:]) -> UserSaleListScreen {
        let options = AppLaunchOptions(
            launchType: .deeplink("https://auto.ru/my"),
            overrideAppSettings: [:],
            environment: environment
        )
        return launch(on: .userSaleListScreen, options: options)
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()

        mocker.startMock()
    }

    private func makeOffer() -> Auto_Api_Offer {
        let response = Auto_Api_OfferResponse(mockFile: "user_offer_102125077-f94ee448")
        return response.offer
    }

    private func mockServer(with offer: Auto_Api_Offer) {
        api.user.offers.category(._unknown("all")).get(parameters: .wildcard)
            .ok(
                mock: .dynamic { _, _ in
                    return .with { response in
                        response.offers = [
                            offer
                        ]

                        response.status = .success
                    }
                }
            )

        api.user.offers.category(.cars).offerID(offer.id).get
            .ok(
                mock: .dynamic { _, _ in
                    return .with { response in
                        response.offer = offer
                        response.status = .success
                    }
                }
            )

        api.safeDeal.deal.list.get(parameters: .wildcard)
            .ok(
                mock: .dynamic { _, _ in
                    return .with { response in
                        response.status = .success
                    }
                }
            )

        api.user.offers.category(.cars).offerID(offer.id).transparencyScoring.get.ok(mock: .dynamic { _, _ in
            return .with { response in
                response.status = .success
            }
        })

        server.addHandler("GET /ios/makeXmlForOffer?offer_id=\(offer.id)") {
            Auto_Api_ReportLayoutResponse.fromFile(named: "CarReport-makeXmlForOffer")
        }

        server.api.user.offers.category(.cars).offerID(offer.id).stats.get(parameters: .wildcard)
            .ok(
                mock: .dynamic { _, _ in
                    return .with { response in
                        response.status = .success
                        response.items = [
                            .with { list in
                                list.counters = [
                                    .with { counter in
                                        counter.phoneCalls = 22
                                        counter.views = 33
                                    }
                                ]
                            }
                        ]
                    }
                }
            )

        mocker.mock_garageCard(offer.id)
    }
}
