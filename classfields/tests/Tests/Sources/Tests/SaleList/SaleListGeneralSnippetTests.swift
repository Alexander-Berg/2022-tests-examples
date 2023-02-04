import XCTest
import AutoRuProtoModels
import AutoRuModels
import AutoRuAppearance
import AutoRuAppConfig
import AutoRuUtils
import AutoRuFetchableImage
import Snapshots
@testable import AutoRuViews
@testable import AutoRuSaleCard
@testable import AutoRuCellHelpers
import Foundation

class SaleListGeneralSnippetTests: BaseUnitTest {
    override func setUp() {
        super.setUp()

        setReplaceImagesWithStub("audi_snippet_stub")
        FetchableImage.blockThreadUntilFinished = true
    }

    override func tearDown() {
        super.tearDown()

        setReplaceImagesDefaultBehavior()
        FetchableImage.blockThreadUntilFinished = false
    }

    func test_snippetAppearanceWithBadges() {
        SharedDefaultsHelper.shared.callBadgeEnabled = true

        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1093580048-0872915d_with_source_last_calls_success")
        let offer = offerResponse.offer
        let model = makeSnippetModel(offer: offer)

        Step("Тестируем отрисовку значков на cниппете в листинге")
        Step("Вы звонили: ожидаем надпись 12 марта на темном фоне")
        checkModel(model, id: "listing_snippet_recent_call_12_march", onCreditLinkTap: onCreditLinkTap)

        Step("Вы звонили: ожидаем в надписи 16:20 сегодняшнего дня, корректное расположение вместе с другими значками + в свернутом состоянии")
        let calendar = Calendar(identifier: .gregorian)
        var components = calendar.dateComponents(in: .current, from: Date())
        components.hour = 16
        components.minute = 20 // used for fixed time value rendering on a snapshot, otherwise test will be failed

        model.callHistoryBadge?.time = calendar.date(from: components)!
        model.galleryBadges = [.panorama, .catalogPhotos]
        checkModel(model, id: "listing_snippet_recent_call_badge_today", onCreditLinkTap: onCreditLinkTap)
        checkModel(model, id: "listing_snippet_recent_call_badge_today_collapsed", onCreditLinkTap: onCreditLinkTap)

        Step("Вы звонили: ожидаем в надписи Вчера 4:20, красный фон (неуспешно), корректное расположение вместе с другими значками + в свернутом состоянии, значок о звонках за последние 24 часа отображаться не должен")
        components = calendar.dateComponents(in: .current, from: Date().addingTimeInterval(-60 * 60 * 24))
        components.hour = 4
        components.minute = 20
        model.callHistoryBadge?.time = calendar.date(from: components)!
        model.callHistoryBadge?.isFailed = true
        model.galleryBadges = [.new]
        model.callMotivationCount = 5
        model.hasCallMotivation = true
        checkModel(model, id: "listing_snippet_recent_call_badge_yesterday_failed", onCreditLinkTap: onCreditLinkTap)
        checkModel(model, id: "listing_snippet_recent_call_badge_yesterday_failed_collapsed", onCreditLinkTap: onCreditLinkTap)

        Step("Вы звонили: значок не отображается, но отображается значок о 5 звонках за последние 24 часа")
        model.callHistoryBadge = nil
        checkModel(model, id: "listing_snippet_call_motivation_badge", onCreditLinkTap: onCreditLinkTap)
    }

    func test_snippetGalleryBadgesGoodPrice() {
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1093580048-0872915d_with_source_last_calls_success")
        let offer = offerResponse.offer

        let model = makeSnippetModel(offer: offer)
        model.galleryBadges = [.new, .catalogPhotos, .panorama, .poi]
        model.badges = [.inactive, .autoRuOnly, .canBook, .credit("Кредит"), .manufacturerChecked("Производитель"), .option("Опция"), .provenOwner, .purchasedBadge("Купленный бадж"), .reportAvailable, .vatIncluded]
        model.priceData.priceBadge = .goodPrice
        checkModel(model, extended: false, showSold: true, onCreditLinkTap: onCreditLinkTap)
    }

    func test_snippetGalleryBadgesGreatPrice() {
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1093580048-0872915d_with_source_last_calls_success")
        let offer = offerResponse.offer

        let model = makeSnippetModel(offer: offer)
        model.galleryBadges = [.new, .panorama, .poi]
        model.badges = [.sold, .autoRuOnly, .canBook, .credit("Кредит"), .manufacturerChecked("Производитель"), .option("Опция"), .provenOwner, .purchasedBadge("Купленный бадж"), .reportAvailable, .vatIncluded]
        model.priceData.priceBadge = .excellentPrice
        checkModel(model, extended: false, showSold: true, onCreditLinkTap: onCreditLinkTap)
    }

    func test_snippetPeekedWithMetro_withExtended() {
        SharedDefaultsHelper.shared.callBadgeEnabled = true
        Step("Проверяем, просмотренное состояние сниппета листинга")
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1093580048-0872915d_with_source_last_calls_success")
        let offer = offerResponse.offer
            .configureUserInfo("Рамзан")
        let model = makeSnippetModel(offer: offer, peeked: true)
        checkModel(model, extended: false, showSold: true, onCreditLinkTap: onCreditLinkTap)
        checkModel(model, extended: true, showSold: true, id: "\(#function)_extended", onCreditLinkTap: onCreditLinkTap)
    }

    func test_snippetElectricRange() {
        Step("Проверяем наличие тайтла \"Заряд на 100 км\" в характеристиках")
        let offerListingResponse: Auto_Api_OfferListingResponse = .init(mockFile: "sale_list_by_fresh")
        var offer = offerListingResponse.offers.first!
        offer.carInfo.techParam.engineType = "ELECTRO"
        offer.carInfo.techParam.electricRange = 100
        let model = makeSnippetModel(offer: offer)
        checkModel(model)
    }

    func test_snippetAutomaticTransmission() {
        Step("Проверяем наличие тайтла \"Автоматическая\" в характеристиках")
        let offerListingResponse: Auto_Api_OfferListingResponse = .init(mockFile: "sale_list_by_fresh")
        var offer = offerListingResponse.offers.first!
        offer.carInfo.techParam.engineType = "ELECTRO"
        offer.carInfo.techParam.electricRange = .zero
        let model = makeSnippetModel(offer: offer)
        checkModel(model)
    }

    // MARK: -

    func test_snippetCanBook() {
        Step("Проверяем наличие бейджа \"Можно забронировать\"")
        let response: Auto_Api_OfferListingResponse = .init(mockFile: "sale_list_by_fresh")
        checkModel(makeSnippetModel(offer: response.offers.first!.setBookingAllowed()))
    }

    func test_snippetBookingNotAllowed() {
        Step("Проверяем отсутствие бейджа \"Можно забронировать\"")
        let response: Auto_Api_OfferListingResponse = .init(mockFile: "sale_list_by_fresh")
        checkModel(makeSnippetModel(offer: response.offers.first!.setBookingAllowed(false)))
    }

    // MARK: -

    func test_saleListSnippetDeliveryInfo() {
        Step("Проверяем наличие тайтла+иконки \"Доставка из Балашихи\"")
        let response: Auto_Api_OfferListingResponse = .init(mockFile: "sale_list_by_fresh")
        checkModel(makeSnippetModel(offer: response.offers.first!.addDeliveryInfo()))
    }

    //для отображения кредита
    private func onCreditLinkTap(){}

    func test_snippetWithMetro() {
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1093580048-0872915d_with_source_last_calls_success")
        let offer = offerResponse.offer
            .configureUserInfo("Рамзан")
        let model = makeSnippetModel(offer: offer)
        model.priceData.priceBadge = nil
        checkModel(model, onCreditLinkTap: onCreditLinkTap)
    }

    func test_snippetPriceDown() {
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1093580048-0872915d_with_tags_history_discount")
        let offer = offerResponse.offer
        let model = makeSnippetModel(offer: offer)
        model.priceData.priceBadge = nil
        checkModel(model, onCreditLinkTap: onCreditLinkTap)
    }

    func test_snippetGoodBadge() {
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1093580048-0872915d_with_source_last_calls_success")
        let offer = offerResponse.offer
        let model = makeSnippetModel(offer: offer)
        model.priceData.priceBadge = .goodPrice
        checkModel(model, onCreditLinkTap: onCreditLinkTap)
    }

    func test_snippetGoodBadgeWithPriceDown() {
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1093580048-0872915d_with_tags_history_discount")
        let offer = offerResponse.offer
        let model = makeSnippetModel(offer: offer)
        model.priceData.priceBadge = .goodPrice
        checkModel(model, onCreditLinkTap: onCreditLinkTap)
    }

    func test_snippetExcellentBadge() {
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1093580048-0872915d_with_source_last_calls_success")
        let offer = offerResponse.offer
        let model = makeSnippetModel(offer: offer)
        model.priceData.priceBadge = .excellentPrice
        checkModel(model, onCreditLinkTap: onCreditLinkTap)
    }

    func test_snippetExcellentBadgeWithPriceDown() {
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1093580048-0872915d_with_tags_history_discount")
        let offer = offerResponse.offer
        let model = makeSnippetModel(offer: offer)
        model.priceData.priceBadge = .excellentPrice
        checkModel(model, onCreditLinkTap: onCreditLinkTap)
    }

    // MARK: - Extended Snippet

    func test_extendedSnippetMisc() {
        let offerResponse: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1093580048-0872915d_with_source_last_calls_success")

        var offer = offerResponse.offer
            .setUsed()
            .mutate {
                $0.carInfo.engineType = "TWO-STROKE"
                $0.carInfo.horsePower = 100000
                $0.additionalInfo.freshDate = UInt64(Date().addingTimeInterval(-180).timeIntervalSince1970 * 1000) // 3 минуты назад
            }

        var model = makeSnippetModel(offer: offer)
        model.galleryBadges = [.new, .panorama, .poi]
        model.badges = [.autoRuOnly, .canBook, .credit("Кредит"), .manufacturerChecked("Производитель"), .option("Опция"), .provenOwner, .purchasedBadge("Купленный бадж"), .reportAvailable, .vatIncluded, .notCleared, .customClearedNoPts]
        checkModel(model, extended: true, id: "\(#function)_used", onCreditLinkTap: onCreditLinkTap)

        offer = offer
            .setNew()
            .setDealer()
            .mutate {
                $0.salon = Auto_Api_Salon.with({ salon in
                    salon.isOficial = true
                    salon.loyaltyProgram = true
                })
            }
        model = makeSnippetModel(offer: offer)
        checkModel(model, extended: true, id: "\(#function)_good", onCreditLinkTap: onCreditLinkTap)
    }

    // MARK: - Private
    private func checkModel(_ model: SaleSnippetModel,
                            extended: Bool = false,
                            showSold: Bool = false,
                            id: String = #function,
                            onCreditLinkTap: (() -> Void)? = nil) {
        let creator = SaleSnippetViewCreator(model: model, extended: extended, showSold: showSold, onCreditLinkTap: onCreditLinkTap)
        let size = creator.sizeForWidth(min(DeviceWidth.iPhone11, 400))
        let view = creator.createViewWithSize(size)
        Snapshot.compareWithSnapshot(view: view, identifier: id)
    }

    private func makeSnippetModel(offer: Auto_Api_Offer, peeked: Bool = false, creditInfo: CreditInfoProvider? = nil) -> SaleSnippetModel {
        let model = SaleSnippetModel.modelForData(
            offer: offer,
            peeked: peeked,
            favorite: false,
            canAddToComparison: true,
            isInComparison: false,
            userNote: nil,
            phoneState: .received(.init()),
            cachedBookedOffers: [:],
            creditInfo: creditInfo,
            currentImageIndex: nil,
            hasChatBotFeature: false,
            geoRadius: nil,
            isExtendedRadius: false,
            showExtendedDate: false,
            shouldShowNewBadge: false,
            hasChatAccess: false,
            shouldShowCreditPriceOnSnippet: true,
            isDealer: false
        )
        return model
    }
}
