import Foundation
import XCTest
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuSaleList AutoRuCellHelpers
final class SaleListSnippetTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)

    private let requestTimeout: TimeInterval = 2

    private let sellerName = "Herr Morzhovy"

    override func setUp() {
        super.setUp()

        mocker
            .mock_base()
            .mock_searchHistory(state: .used)
            .mock_sharkCreditApplicationActiveEmpty()
            .mock_sharkCreditProductCalculator()
            .mock_sharkCreditProductList(products: [.tinkoff_1, .tinkoff_2], appProduct: [])
            .mock_user()
            .startMock()
    }

    func test_saleListSnippetAppearanceForUsedAuto() {
        // https://testpalm.yandex-team.ru/testcase/appsautoru-33
        Step("Проверяем обычный сниппет для б/у")
        mockRegularSnippet()
        mocker.setForceLoginMode(.forceLoggedIn)

        launch(on: .transportScreen)
            .tap(.searchHistory(title: "LADA (ВАЗ) 1111 Ока"))
            .should(provider: .saleListScreen, .exist)
            .focus { saleList in
                saleList.focus(on: .offerCell(.alias(.bmw3g20), .title), ofType: .offerSnippet) { snippet in
                    checkTitleAndPrice(snippet)
                    checkCreditInfo(snippet)
                }
                saleList.focus(on: .offerCell(.alias(.bmw3g20), .gallery), ofType: .offerSnippet) { snippet in
                    checkGallery(snippet, extended: false)
                }
                saleList.focus(on: .offerCell(.alias(.bmw3g20), .body), ofType: .offerSnippet) { snippet in
                    checkCharacteristics(snippet)
                    checkSellerInfo(snippet)
                    checkFooterButtons(snippet, isLogin: true)
                }
            }
    }

    func test_saleListSnippetAppearanceForUsedAuto_extended() {
        Step("Проверяем расширенный сниппет для б/у")

        mockExtendedSnippet()
        mocker.setForceLoginMode(.forceLoggedOut)

        launch(on: .transportScreen)
            .tap(.searchHistory(title: "LADA (ВАЗ) 1111 Ока"))
            .should(provider: .saleListScreen, .exist)
            .focus { saleList in
                saleList.scroll(to: .offerCell(.alias(.bmw3g20), .footer), maxSwipes: 2)
                saleList.focus(on: .offerCell(.alias(.bmw3g20), .title), ofType: .offerSnippet) { snippet in
                    checkTitleAndPrice(snippet)
                    checkCreditInfo(snippet)
                }
                saleList.focus(on: .offerCell(.alias(.bmw3g20), .gallery), ofType: .offerSnippet) { snippet in
                    checkGallery(snippet, extended: true)
                }
                saleList.focus(on: .offerCell(.alias(.bmw3g20), .body), ofType: .offerSnippet) { snippet in
                    checkSellerInfo(snippet)
                    checkFooterButtons(snippet, isLogin: false)
                    snippet.should(.dealerContacts, .exist).focus { button in
                        button.tap()
                    }
                }
            }
            .should(provider: .dealerСontactsModal, .exist)
            .focus { modal in
                modal.swipe(.down)
            }
    }

    func test_shouldLogFrontlogWhenOpenCardFromSmallSnippet() {
        mocker.mock_eventsLog()
        mockRegularSnippet()

        let frontlogExpectation = expectationForRequest(
            method: "POST",
            uri: "/events/log",
            requestChecker: { (req: Auto_Api_EventsReportRequest) -> Bool in
                return req.events.contains(
                    where: {
                        $0.cardShowEvent.cardID == "1103768888-7759ab74"
                            && $0.cardShowEvent.hasAppVersion
                            && $0.cardShowEvent.category == AutoRuProtoModels.Auto_Api_Category.cars
                            && $0.cardShowEvent.contextBlock == AutoRuProtoModels.Auto_Api_ContextBlock.blockListing
                            && $0.cardShowEvent.contextPage == AutoRuProtoModels.Auto_Api_ContextPage.pageListing
                            && $0.cardShowEvent.contextService == AutoRuProtoModels.Auto_Api_ContextService.serviceAutoru
                            && $0.cardShowEvent.index == 0
                            && $0.cardShowEvent.hasSearchQueryID
                            && $0.cardShowEvent.section == AutoRuProtoModels.Auto_Api_Section.used
                    }
                )
            }
        )

        launch(on: .transportScreen)
            .tap(.searchHistory(title: "LADA (ВАЗ) 1111 Ока"))
            .should(provider: .saleListScreen, .exist)
            .focus { saleList in
                saleList.focus(on: .offerCell(.alias(.bmw3g20), .title), ofType: .offerSnippet) { snippet in
                    snippet
                        .tap(.titleLabel)
                }
            }

        Step("Проверяем, что отправили событие card_view_event во фронтлог с ожидаемыми параметрами") {
            wait(for: [frontlogExpectation], timeout: 2.0)
        }
    }

    func test_shouldOpenChatFromSnippet() {
        mocker
            .mock_getChatRoom()
            .mock_postChatRoom()
            .mock_getChatMessage()
            .mock_getChatMessageSpam()
            .mock_deleteChatMessageUnread()
            .mock_dictionariesMessagePresets()
            .mock_dictionariesMessageHelloPresets()
            .mock_dictionariesSellerMessagePresets()
            .mock_dictionariesSellerMessageHelloPresets()

        mockSnippetWithOnePhoto()
        let requestCreateChatRoom = expectationForRequest(method: "POST", uri: "/chat/room")

        launch(on: .transportScreen)
            .tap(.searchHistory(title: "LADA (ВАЗ) 1111 Ока"))
            .should(provider: .saleListScreen, .exist)
            .focus { saleList in
                saleList
                    .focus(on: .offerCell(.alias(.bmw3g20), .gallery), ofType: .offerSnippet) { snippet in
                        snippet
                            .scroll(to: .chatButton, direction: .left, maxSwipes: 5)
                            .tap(.chatButton)
                    }
            }
            .should(provider: .chatScreen, .exist)
            .focus { chatRoom in
                chatRoom
                    .should(.presets, .exist)
                    .focus(on: .offerPanel, ofType: .chatOfferPanel) { element in
                        element
                            .should(.title, .contain("Toyota Camry VI (XV40), 2008"))
                            .should(.subtitle, .contain("659 000"))
                    }
                    .focus(on: .inputBar, ofType: .chatInputBar) { element in
                        element
                            .should(.hint, .exist)
                            .should(.text, .exist)
                    }
            }

        wait(for: [requestCreateChatRoom], timeout: 1)
    }
    // MARK: - Checks

    private func checkTitleAndPrice(_ snippet: OfferSnippet) {
        Step("Проверяем заголовок и цену")

        snippet
            .should(.titleLabel, .match("BMW 3 серии 330d xDrive VII (G2x), 2019"))
            .should(.priceLabel, .match("3 990 000 ₽"))
    }

    private func checkCreditInfo(_ snippet: OfferSnippet) {
        Step("Проверяем информацию о кредитовании")

        snippet
            .should(.creditLink, .match("от 61 100 ₽/мес."))
            .tap(.creditLink)
            .should(provider: .creditBannerPopup, .exist)
            .focus { popup in
                popup.tap(.dismissButton)
            }
    }

    private func checkGallery(_ snippet: OfferSnippet, extended: Bool) {
        Step("Проверяем расположение панорамы в галерее") {
            snippet
                .should(.panorama(0), .exist)
        }

        if !extended {
            Step("Проверяем наличие бейджа \"Фото из каталога\"") {
                snippet.should(.catalogPhotosBadge, .exist)
            }
        }

        Step("Проверяем расположение фото-превью в галерее") {
            snippet
                .should(.photo(1), .exist)
        }

        Step("Проверяем расположение видео в галерее") {
            snippet
                .scroll(to: .video(2), direction: .left, maxSwipes: 1)
                .should(.video(2), .exist)
        }

        Step("Проверяем наличие иконки play у видео в галерее") {
            snippet
                .should(.videoPlayIcon, .exist)
        }

        if !extended {
            Step("Проверяем наличие блока контактов в галерее") {
                snippet
                    .scroll(to: .contactInfo, direction: .left, maxSwipes: 5)
                    .focus(on: .contactInfo) { view in
                        view.should("Дилер", .exist)
                        view.should(sellerName, .exist)
                    }
            }

            Step("Проверяем наличие кнопки \"Позвонить\" в галерее") {
                snippet
                    .scroll(to: .callButtonInPhotos, direction: .left, maxSwipes: 5)
                    .focus(on: .callButtonInPhotos) { view in
                        view.should("Позвонить", .exist)
                    }
            }

            Step("Проверяем наличие кнопки \"Написать\" в галерее") {
                snippet
                    .scroll(to: .chatButton, direction: .left, maxSwipes: 5)
                    .focus(on: .chatButton) { view in
                        view.should("Написать", .exist)
                    }
            }

            Step("Проверяем наличие кнопки покупки отчета в галерее") {
                snippet
                    .scroll(to: .showReportButton, direction: .left, maxSwipes: 5)
                    .focus(on: .showReportButton) { view in
                        view.should("Смотреть отчёт", .exist)
                    }
            }
        } else {
            Step("Проверяем отсутствие блока контактов в галерее") {
                snippet
                    .swipe(.left)
                    .should(.contactInfo, .be(.hidden))
            }

            Step("Проверяем отсутствие кнопки \"Позвонить\" в галерее") {
                snippet
                    .swipe(.left)
                    .should(.callButtonInPhotos, .be(.hidden))
            }
        }
    }

    private func checkCharacteristics(_ snippet: OfferSnippet) {
        Step("Проверяем характеристики ТС")

        snippet.focus(on: .characteristics) { view in
            view.should("12 633 км", .exist)
            view.should("3.0 л / 265 л.с. / Дизель", .exist)
            view.should("Автоматическая", .exist)
            view.should("Черный", .exist)
            view.should("Седан", .exist)
        }
    }

    private func checkSellerInfo(_ snippet: OfferSnippet) {
        Step("Проверяем иконку проверенного дилера") {
            snippet.should(.saleFromCompanyIcon, .exist).focus { view in
                view.validateSnapshot(snapshotId: "checkSellerInfo")
            }
        }

        Step("Проверяем метро и дату размещения") {
            snippet.should(.footerSubtitleLabel, .exist)
        }

    }

    private func checkFooterButtons(_ snippet: OfferSnippet, isLogin: Bool) {
        Step("Проверяем кнопку c действиями для сниппета") {
            let expectation = api.user.favorites.category(.cars).offerId(OfferIdentifier.alias(.bmw3g20).id)
                .post
                .expect()

            snippet.should(.favoriteButton(OfferIdentifier.alias(.bmw3g20).id), .exist).focus { button in
                button.tap()
            }
            .wait(for: [expectation])
        }

        Step("Проверяем кнопку c действиями для сниппета") {
            snippet.should(.moreButton, .exist).focus { button in
                button.validateSnapshot(snapshotId: "snippet_more_button")
                button.tap()
            }
            .wait(for: 1)
            .should(provider: .actionsMenuPopup, .exist).focus { menu in
                menu.should(.writeNote, .exist)
                menu.should(.share, .exist)
                menu.should(.copyLink, .exist)
                menu.should(.complain, .exist)
                menu.should(.downloadOffer, isLogin ? .exist : .be(.hidden))

                menu.focus(on: .container) {
                    $0.validateSnapshot(snapshotId: "action_menu_\(isLogin ? "login" : "no_login")")
                }
                menu.tap(.dismissButton)
            }
        }

        Step("Проверяем кнопку добавления в сравнение") {
            snippet.should(.comparisonButton, .exist).focus { button in
                button.validateSnapshot(snapshotId: "snippet_compare_button")
            }
        }
    }

    // MARK: - Model modifyers

    private func mockRegularSnippet() {
        mocker.mock_searchCars {
            var response = Responses.Search.Cars.success(for: .global)
            response.offers[0] = response.offers[0]
                .mutate { offer in
                    offer.tags = ["catalog_photo"]
                    offer.salon = .with { salon in
                        salon.loyaltyProgram = true
                    }
                    offer.url = "https://auto.ru"
                }
                .configureUserInfo(self.sellerName)
                .addSharkInfo()
                .addPanorama()
                .addVideo()
            return response
        }
    }

    private func mockExtendedSnippet() {
        mocker.mock_searchCars {
            var response = Responses.Search.Cars.success(for: .global)
            response.offers[0] = response.offers[0]
                .mutate { offer in
                    offer.services.append(Auto_Api_PaidService.with {
                        $0.service = "all_sale_premium"
                    })
                    offer.tags = ["catalog_photo"]
                    offer.salon = .with { salon in
                        salon.loyaltyProgram = true
                    }
                    offer.url = "https://auto.ru"
                }
                .configureUserInfo(self.sellerName)
                .addSharkInfo()
                .setNew()
                .setDealer()
                .addPanorama()
                .addVideo()
            return response
        }
        mocker.mock_offerCars(id: OfferIdentifier.alias(.bmw3g20).id, isSalon: true)
    }

    private func mockSnippetWithOnePhoto() {
        mocker.mock_searchCars {
            var response = Responses.Search.Cars.success(for: .global)
            response.offers[0] = response.offers[0]
                .configureUserInfo(self.sellerName)
                .mutate {
                    $0.state.imageUrls = [response.offers[0].state.imageUrls[0]]
                }
            return response
        }
    }
}
