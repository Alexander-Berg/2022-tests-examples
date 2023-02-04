import XCTest
import YandexMobileAds
import AutoRuProtoModels
import AutoRuModels
import AutoRuAppearance
import AutoRuTableController
import AutoRuUtils
import AutoRuFetchableImage
import AutoRuYogaLayout
import AutoRuAppConfig
import Snapshots
@testable import AutoRuCellHelpers
@testable import AutoRuViews
@testable import AutoRuSaleCardSharedModels
@testable import AutoRuSaleCardSharedUI
@testable import AutoRuOfferAdvantage
import AutoRuColorSchema
import CoreGraphics
import Foundation

final class SaleCardAppearanceTests: BaseUnitTest {

    func test_counters() {
        let model = SaleCardCounterModel(
            publicationDate: Date(timeIntervalSinceReferenceDate: 60 * 60 * 24 * 10000),
            region: "Москва",
            numberOfVisitsAll: 220,
            numberOfVisitsDaily: 100
        )

        Self.compareWithSnapshot(layout: CardCountersLayout(model: model), id: "counters")
    }

    func test_details() {
        let model = SaleCardCharacteristicsModel(
            characteristics: [
                SaleCardCharacteristicModel(
                    title: "Заголовок",
                    detail: "Простой",
                    specificItem: nil,
                    detailHighlightedByBold: false,
                    detailHighlightedAsUrl: false,
                    detailIsSelectable: false
                ),
                SaleCardCharacteristicModel(
                    title: "Заголовок",
                    detail: "Болд",
                    specificItem: nil,
                    detailHighlightedByBold: true,
                    detailHighlightedAsUrl: false,
                    detailIsSelectable: false
                ),
                SaleCardCharacteristicModel(
                    title: "Заголовок",
                    detail: "Ссылка",
                    specificItem: nil,
                    detailHighlightedByBold: false,
                    detailHighlightedAsUrl: true,
                    detailIsSelectable: false
                ),
                SaleCardCharacteristicModel(
                    title: "Заголовок",
                    detail: "Болд ссылка",
                    specificItem: nil,
                    detailHighlightedByBold: true,
                    detailHighlightedAsUrl: true,
                    detailIsSelectable: false
                ),
                SaleCardCharacteristicModel(
                    title: "Заголовок",
                    detail: "Broken",
                    specificItem: .brokenVehicleState,
                    detailHighlightedByBold: false,
                    detailHighlightedAsUrl: false,
                    detailIsSelectable: false
                ),
                SaleCardCharacteristicModel(
                    title: "Заголовок",
                    detail: "In stock",
                    specificItem: .orderInStock,
                    detailHighlightedByBold: false,
                    detailHighlightedAsUrl: false,
                    detailIsSelectable: false
                ),
                SaleCardCharacteristicModel(
                    title: "Заголовок",
                    detail: "On order",
                    specificItem: .orderOnOrder,
                    detailHighlightedByBold: false,
                    detailHighlightedAsUrl: false,
                    detailIsSelectable: false
                ),
                SaleCardCharacteristicModel(
                    title: "Заголовок",
                    detail: "Выделяемый",
                    specificItem: nil,
                    detailHighlightedByBold: false,
                    detailHighlightedAsUrl: false,
                    detailIsSelectable: true
                )
            ]
        )

        Self.compareWithSnapshot(layout: SaleCardCharacteristicsLayout(model: model, onTap: { _ in }), id: "details")
    }

    func test_actions() {
        let model = ActionButtonsCellHelperModel(
            inFavorite: false,
            hasNotes: false,
            isInComparison: true,
            showSafeDealButton: false,
            showSafeDealNewBadge: false,
            favoriteButtonFirst: false,
            onShareTap: { _ in },
            onEditTap: { },
            onFavoritesTap: { },
            onCompareTap: { },
            onSafeDealTap: { }
        )

        Self.compareWithSnapshot(cellHelper: ActionButtonsCellHelper(model: model), id: "actions")
    }

    func test_actions_safeDeal() {
        let model = ActionButtonsCellHelperModel(
            inFavorite: false,
            hasNotes: false,
            isInComparison: true,
            showSafeDealButton: true,
            showSafeDealNewBadge: false,
            favoriteButtonFirst: false,
            onShareTap: { _ in },
            onEditTap: { },
            onFavoritesTap: { },
            onCompareTap: { },
            onSafeDealTap: { }
        )

        Self.compareWithSnapshot(cellHelper: ActionButtonsCellHelper(model: model), id: "actions_safe_deal")
    }

    func test_actions_safeDeal_badge() {
        let model = ActionButtonsCellHelperModel(
            inFavorite: false,
            hasNotes: false,
            isInComparison: true,
            showSafeDealButton: true,
            showSafeDealNewBadge: true,
            favoriteButtonFirst: false,
            onShareTap: { _ in },
            onEditTap: { },
            onFavoritesTap: { },
            onCompareTap: { },
            onSafeDealTap: { }
        )

        Self.compareWithSnapshot(cellHelper: ActionButtonsCellHelper(model: model), id: "actions_safe_deal_badge")
    }

    func test_userNote() {
        let builder = SaleCardTableModelBuilder()
        builder.addUserNote("Текст заметки", onTap: { })
        let cellHelper = builder.tableItem(withIdStartsWith: "user_note")!.cellHelper

        Self.compareWithSnapshot(cellHelper: cellHelper, id: "user_note")
    }

    func test_advantages() {
        // Один
        let single = OfferAdvantagesCellHelper(
            model: OfferAdvantagesCellHelperModel(
                advantages: [
                    OfferAdvantageBadgeModel(
                        kind: .plain(.oneOwner),
                        title: "Заголовок",
                        description: .init(text: "Описание", type: .subtitle),
                        titleSuffix: "суффикс"
                    )
                ],
                actions: .init(onAdvantageTap: { _, _ in }, onAdvantagePresented: { _, _ in })
            )
        )
        Self.compareWithSnapshot(cellHelper: single, id: "single_advantage")

        // Проверенный собственник
        let singleProven = OfferAdvantagesCellHelper(
            model: OfferAdvantagesCellHelperModel(
                advantages: [
                    OfferAdvantageBadgeModel(
                        kind: .provenOwnerInactive(.uploadFailed),
                        title: "Заголовок",
                        description: .init(text: "Описание-ссылка", type: .link),
                        titleSuffix: nil
                    )
                ],
                actions: .init(onAdvantageTap: { _, _ in }, onAdvantagePresented: { _, _ in })
            )
        )
        Self.compareWithSnapshot(cellHelper: singleProven, id: "single_advantage_proven_owner")

        // Несколько
        let multiple = OfferAdvantagesCellHelper(
            model: OfferAdvantagesCellHelperModel(
                advantages: [
                    OfferAdvantageBadgeModel(
                        kind: .plain(.oneOwner),
                        title: "Заголовок",
                        description: .init(text: "Описание", type: .subtitle),
                        titleSuffix: nil
                    ),
                    OfferAdvantageBadgeModel(
                        kind: .plain(.noAccidents),
                        title: "Заголовок",
                        description: .init(text: "Описание", type: .subtitle),
                        titleSuffix: nil
                    )
                ],
                actions: .init(onAdvantageTap: { _, _ in }, onAdvantagePresented: { _, _ in })
            )
        )

        Snapshot.compareWithSnapshot(
            cellHelper: multiple,
            maxWidth: DeviceWidth.iPhone11,
            maxHeight: 100,
            backgroundColor: ColorSchema.Background.surface,
            identifier: "multiple_advantages"
        )
    }

    func test_bookingButton() {
        let builder = SaleCardTableModelBuilder()
        builder.addBookButton { }
        let cellHelper = builder.tableItem(withIdStartsWith: "book_btn")!.cellHelper

        Self.compareWithSnapshot(cellHelper: cellHelper, id: "booking_button")
    }

    func test_delivery() {
        let location = Auto_Api_Location.with {
            $0.regionInfo = Auto_Api_RegionInfo.with { model in
                model.name = "Москва"
            }
        }
        let info = Auto_Api_DeliveryInfo.with {
            $0.deliveryRegions = [
                Auto_Api_DeliveryRegion.with { model in
                    model.location = location
                }
            ]
        }
        let model = DeliveryInfoViewModel(sourceLocation: location, deliveryInfo: info)!

        Self.compareWithSnapshot(cellHelper: DeliveryInfoCellHelper(model: model), id: "delivery")
    }

    func test_tradeIn() {
        [
            (ServiceRequestState.initial, "initial"),
            (ServiceRequestState.completed, "completed")
        ].forEach {
            let initial = TradeInBlockCellHelper(
                model: TradeInBlockCellHelperModel(
                    state: $0.0,
                    onTap: { },
                    onInfoTap: { }
                )
            )

            Self.compareWithSnapshot(cellHelper: initial, id: "tradein_\($0.1)")
        }
    }

    func test_certification() {
        let tableModelBuilder = SaleCardTableModelBuilder()
        let saleCardCreator = SaleCardCreator(tableModelBuilder: tableModelBuilder)

        var certificationInfo = CertificationInfoViewModel(with: Auto_Api_Offer())
        certificationInfo.certificationDate = Date(timeIntervalSinceNow: 60 * 60 * 24 * 1000)
        certificationInfo.name = "Сертификация"
        certificationInfo.advantages = ["Преимущество сертификации 1"]
        certificationInfo.description = "Описание"
        certificationInfo.fullLogo = FetchableImage()
        certificationInfo.smallLogo = .testImage(withFixedSize: CGSize(width: 144, height: 48))

        saleCardCreator.addCertificationInfo(with: certificationInfo) { }

        let items = tableModelBuilder.build()[0].items
        let header = items.first(where: { $0.identifier.starts(with: "cert_header") })!
        let row = items.first(where: { $0.identifier.starts(with: "cert_row") })!

        Self.compareWithSnapshot(cellHelper: header.cellHelper, id: "cert_header")
        Self.compareWithSnapshot(cellHelper: row.cellHelper, id: "cert_row")
    }

    func test_certificationWithFullLogo() {
        let tableModelBuilder = SaleCardTableModelBuilder()
        let saleCardCreator = SaleCardCreator(tableModelBuilder: tableModelBuilder)

        var certificationInfo = CertificationInfoViewModel(with: Auto_Api_Offer())
        certificationInfo.certificationDate = Date(timeIntervalSinceNow: 60 * 60 * 24 * 1000)
        certificationInfo.name = "Сертификация"
        certificationInfo.advantages = ["Преимущество сертификации 1"]
        certificationInfo.description = "Описание"
        certificationInfo.fullLogo = .testImage(withFixedSize: CGSize(width: 144, height: 48))

        saleCardCreator.addCertificationInfo(with: certificationInfo) { }

        let items = tableModelBuilder.build()[0].items
        let header = items.first(where: { $0.identifier.starts(with: "cert_header") })!

        Self.compareWithSnapshot(cellHelper: header.cellHelper, id: "cert_header_fullLogo")
    }

    func test_badges() {
        let tableModelBuilder = SaleCardTableModelBuilder()
        tableModelBuilder.addBadges(["Бейдж 1", "Бейдж 2", "Бейдж 3"])

        let badges = tableModelBuilder.tableItem(withIdStartsWith: "badges")!
        Self.compareWithSnapshot(cellHelper: badges.cellHelper, id: "badges")
    }

    func test_damage() {
        let bodyTypes: [CarDamagesModel.BodyType] = [.cabrio, .coupe, .hatchback3Doors, .hatchback5Doors,
                                                     .jeep3Doors, .jeep5Doors, .liftback, .limo, .pickup,
                                                     .sedan, .van, .wagon]
        for bodyType in bodyTypes {
            let tableModelBuilder = SaleCardTableModelBuilder()
            let damageModel = CarDamagesModel(
                bodyType: bodyType,
                damages: [
                    .init(place: .frontLeftDoor, damage: [.scratch], description: "Повреждение 1"),
                    .init(place: .rightMirror, damage: [.scratch], description: "Повреждение 2")
                ]
            )
            tableModelBuilder.addDamagesSection(
                model: damageModel,
                onShowMore: { _, _, _ in},
                onSelectDamageItem: { _, _ in },
                onDisplay: { }
            )

            let damages = tableModelBuilder.tableItem(withIdStartsWith: "damage")!
            let view = damages.cellHelper.createCellView(width: DeviceWidth.iPhone11)

            view.backgroundColor = ColorSchema.Background.surface
            view.bounds = CGRect(origin: .zero, size: CGSize(width: DeviceWidth.iPhone11, height: 624.0))

            Snapshot.compareWithSnapshot(
                view: view,
                identifier: "damages_\(bodyType)"
            )
        }
    }

    func test_dealerName() {
        let layout = DealerInfoLayout(
            model: SaleCardSellerInfoModel(
                name: "Дилер",
                type: "Дилер",
                address: "Адрес",
                isDealer: true,
                hasDealerId: true,
                isOfficialDealer: true,
                isVerifiedDealer: true,
                isAuto: true,
                shouldDisableDealerTransition: false,
                metros: [MetroStationModel(name: "Станция метро", color: .purple)],
                category: .cars,
                categoryOffersCount: 100,
                avatar: nil,
                registrationDate: Date(timeIntervalSinceNow: 0.0),
                locationAddress: "Адрес"
            )
        )

        Self.compareWithSnapshot(layout: layout, id: "dealer_name")
    }

    func test_regularSellerNameAndAdress() {
        let tableModelBuilder = SaleCardTableModelBuilder()
        let privateSellerModel = SaleCardSellerInfoModel(
            name: "Джоконда",
            type: "Частное лицо",
            address: "Бангладеш, ул. Рабочая, д. 69",
            isDealer: false,
            hasDealerId: false,
            isOfficialDealer: false,
            isVerifiedDealer: false,
            isAuto: true,
            shouldDisableDealerTransition: true,
            metros: [MetroStationModel(name: "Авторушная", color: .purple)],
            category: .cars,
            categoryOffersCount: nil,
            avatar: nil,
            registrationDate: Date(timeIntervalSinceNow: 0.0),
            locationAddress: "Адрес"
        )

        tableModelBuilder.addRegularSellerName(privateSellerModel)
        tableModelBuilder.addSellerAddress(privateSellerModel) { return }

        let seller = tableModelBuilder.tableItem(withIdStartsWith: "seller_info")!
        let address = tableModelBuilder.tableItem(withIdStartsWith: "seller_address")!

        Self.compareWithSnapshot(cellHelper: seller.cellHelper, id: "regular_seller_name")
        Self.compareWithSnapshot(cellHelper: address.cellHelper, id: "regular_seller_address")
    }

    func test_callback() {
        let tableModelBuilder = SaleCardTableModelBuilder()
        tableModelBuilder.addCallbackHeader()
        tableModelBuilder.addCallbackRegionPromo()
        tableModelBuilder.addCallbackSleepPromo()
        tableModelBuilder.addCallbackForm(phone: "+79991112233", state: .initial, choosePhone: { }, onSendTap: { })

        let items = tableModelBuilder.build()[0].items

        let header = items.first(where: { $0.identifier.starts(with: "callback_header") })!
        let regionPromo = items.first(where: { $0.identifier.starts(with: "callback_region_promo") })!
        let sleepPromo = items.first(where: { $0.identifier.starts(with: "callback_sleep_promo") })!

        let formInitial = items.first(where: { $0.identifier.starts(with: "callback_form") })!
        Self.compareWithSnapshot(cellHelper: formInitial.cellHelper, id: "callback_form_initial")

        Self.compareWithSnapshot(cellHelper: header.cellHelper, id: "callback_header")
        Self.compareWithSnapshot(cellHelper: regionPromo.cellHelper, id: "callback_region_promo")
        Self.compareWithSnapshot(cellHelper: sleepPromo.cellHelper, id: "callback_sleep_promo")

        let tableModelBuilder1 = SaleCardTableModelBuilder()
        tableModelBuilder1.addCallbackForm(phone: "+79991112233", state: .completed, choosePhone: { }, onSendTap: { })
        let items1 = tableModelBuilder1.build()[0].items
        let formCompleted = items1.first(where: { $0.identifier.starts(with: "callback_form") })!
        Self.compareWithSnapshot(cellHelper: formCompleted.cellHelper, id: "callback_form_completed")

        let tableModelBuilder2 = SaleCardTableModelBuilder()
        tableModelBuilder2.addCallbackForm(phone: "", state: .initial, choosePhone: { }, onSendTap: { })
        let items2 = tableModelBuilder2.build()[0].items
        let formEmptyPhone = items2.first(where: { $0.identifier.starts(with: "callback_form") })!
        Self.compareWithSnapshot(cellHelper: formEmptyPhone.cellHelper, id: "callback_form_empty_phone")
    }

    func test_carRequest() {
        let tableModelBuilder = SaleCardTableModelBuilder()
        tableModelBuilder.addNewCarRequestButton(carName: "Audi Q7", onTap: { }, onDisplay: { })

        let chatBot = tableModelBuilder.tableItem(withIdStartsWith: "new_car_request")!
        Self.compareWithSnapshot(cellHelper: chatBot.cellHelper, id: "car_request")
    }

    func test_driveBanners() {
        let tableModelBuilder = SaleCardTableModelBuilder()
        tableModelBuilder.addDriveBanner(
            model: DriveBannerModel(
                carTitle: "Ауди",
                promocode: "PROMOCODE",
                carIconName: "audi_q3_white"
            ),
            onShow: { },
            onClose: { },
            onTap: { }
        )

        let banner = tableModelBuilder.tableItem(withIdStartsWith: "drive")!
        Self.compareWithSnapshot(cellHelper: banner.cellHelper, id: "drive")
    }

    func test_chatPresets() {
        let helper = ChatPresetsCellHelper(
            model: ChatPresetsCellHelperModel(
                presets: [SaleCardChatPreset(text: "Текст", helloText: "Текст 1")],
                onTap: { _ in }
            )
        )

        Self.compareWithSnapshot(cellHelper: helper, id: "chat_presets")
    }

    func test_blockedInfo() {
        let tableModelBuilder = SaleCardTableModelBuilder()
        tableModelBuilder.addBlockedOfferStub()

        let stub = tableModelBuilder.tableItem(withIdStartsWith: "blocked_offer_stub")!
        Self.compareWithSnapshot(cellHelper: stub.cellHelper, id: "blocked_offer_stub")
    }

    func test_emptyImageStub() {
        let tableModelBuilder = SaleCardTableModelBuilder()
        tableModelBuilder.addEmptyImagesStub(onDisplay: { _ in })

        let stub = tableModelBuilder.tableItem(withIdStartsWith: "images_stub")!
        Self.compareWithSnapshot(cellHelper: stub.cellHelper, id: "images_stub")
    }

    func test_galleryBadgesAppearance() {
        setReplaceImagesWithStub("audi_snippet_stub")
        func checkModel(_ model: SaleCardViewPresentationModel, id: String, timeout: TimeInterval = 1.0) {
            guard let mediaContent = model.mediaContent else {
                XCTFail("Media content must exist for this test")
                return
            }

            let tableModelBuilder = SaleCardTableModelBuilder()
            tableModelBuilder.addGallery(
                mediaContent: mediaContent,
                initialIndex: model.initialImageIndex,
                galleryBadge: model.galleryBadge,
                callHistoryBadge: model.callHistoryBadge,
                autoRuBadges: model.autoRuBadges,
                onTap: nil,
                onDisplay: nil,
                galleryContext: nil,
                ad: nil,
                onAdCloseTap: nil,
                scrollDidScroll: nil
            )

            guard let gallery = tableModelBuilder.build()[0].items.first else {
                XCTFail("Gallery must exist")
                return
            }

            gallery.cellHelper.precalculateLayout(indexPath: IndexPath(item: 0, section: 0), width: DeviceWidth.iPhone11)
            let view = gallery.cellHelper.createCellView(width: DeviceWidth.iPhone11)
            view.backgroundColor = ColorSchema.Background.surface

            Snapshot.compareWithSnapshot(
                view: view,
                identifier: id
            )
        }

        SharedDefaultsHelper.shared.callBadgeEnabled = true

        let offer = makeOffer("offer_CARS_1093580048-0872915d_with_source_last_calls_success")
        let generator = OfferModelGenerator()
        var model = generator.generateSaleCardModel(
            offer: offer,
            additionalInfo: nil,
            userNote: "",
            canDisplayPriceChange: false,
            hasChatBotFeature: false,
            hasCarReport: false,
            isDealer: false,
            relatedOffers: [],
            specialOffers: [],
            videos: [],
            journalArticles: [],
            descriptionExpanded: false,
            initialImageIndex: nil,
            creditParam: nil,
            scrollDidScroll: nil,
            shouldShowSafeDealNewBadge: false,
            publicProfileInfo: nil,
            discountedMainPriceVersion: .dealerNewOnly
        )

        Step("Проверка отрисовки значков на картинке галереи в карточке объявления")

        Step("Вы звонили: ожидаем надпись 12 марта на темном фоне")
        checkModel(model, id: "gallery_badges_recent_call_12_march")

        Step("Вы звонили: ожидаем в надписи 16:20 сегодняшнего дня, корректное расположение вместе с другими значками")
        let calendar = Calendar(identifier: .gregorian)
        var components = calendar.dateComponents(in: .current, from: Date())
        components.hour = 16
        components.minute = 20 // used for fixed time value rendering on a snapshot, otherwise test will be failed

        model.callHistoryBadge?.time = calendar.date(from: components)!
        model.autoRuBadges = [.panorama]
        model.galleryBadge = .autoRuOnly
        checkModel(model, id: "gallery_badges_recent_call_badge_today")

        Step("Вы звонили: корректное расположение вместе с другими значками в свернутом состоянии")
        model.callHistoryBadge?.time = calendar.date(from: components)!
        model.callHistoryBadge?.shouldAutoCollapse = true
        model.autoRuBadges = [.autoRuOnly, .panorama]
        model.galleryBadge = .new
        checkModel(model, id: "gallery_badges_recent_call_badge_collapsed", timeout: model.callHistoryBadge!.collapseTimeout + 1)

        Step("Вы звонили: ожидаем в надписи Вчера 4:20, красный фон (неуспешно), корректное расположение вместе с другими значками")
        components = calendar.dateComponents(in: .current, from: Date().addingTimeInterval(-60 * 60 * 24))
        components.hour = 4
        components.minute = 20
        model.callHistoryBadge?.time = calendar.date(from: components)!
        model.callHistoryBadge?.isFailed = true
        model.autoRuBadges = [.catalogPhotos, .autoRuOnly]
        checkModel(model, id: "gallery_badges_recent_call_badge_yesterday_failed")

        Step("Вы звонили: значок не отображается")
        model.callHistoryBadge = nil
        checkModel(model, id: "gallery_badges_recent_call_badge_absent")

        setReplaceImagesDefaultBehavior()
    }

    func test_safeDealRequest() {
        setReplaceImagesWithStub()
        defer { setReplaceImagesDefaultBehavior() }

        let tableModelBuilder = SaleCardTableModelBuilder()
        tableModelBuilder.addSafeDealRequest(
            model: SafeDealRequestModel(
                image: FetchableImage(urlString: "https://auto.ru"),
                onDealTap: { },
                onAboutTap: { }
            )
        )

        let banner = tableModelBuilder.tableItem(withIdStartsWith: "safe_deal")!
        Self.compareWithSnapshot(cellHelper: banner.cellHelper, id: "safe_deal")
    }

    // MARK: - Private

    private static func compareWithSnapshot(cellHelper: CellHelper, id: String) {
        Snapshot.compareWithSnapshot(
            cellHelper: cellHelper,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: id
        )
    }

    private static func compareWithSnapshot(layout: LayoutConvertible, id: String) {
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: id
        )
    }

    private func makeOffer(_ fileName: String) -> Auto_Api_Offer {
        let url = Bundle.current.url(forResource: fileName, withExtension: "json")
        XCTAssertNotNil(url, "File \(fileName).json doesn't exists in the bundle")
        let response = try! Auto_Api_OfferResponse(jsonUTF8Data: Data(contentsOf: url!))
        return response.offer
    }
}
