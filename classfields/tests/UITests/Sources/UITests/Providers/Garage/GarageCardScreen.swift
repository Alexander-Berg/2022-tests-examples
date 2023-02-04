import XCTest
import Snapshots

typealias GarageCardScreen_ = GarageCardSteps

extension GarageCardScreen_: UIRootedElementProvider {
    static var rootElementID: String = "garage_card"
    static var rootElementName: String = "Гараж. Карточка"

    // TODO @kvld: Часть элементов не относятся к гаражной карточке, а относятся
    // к экранам, которые открываются с гаражной карточки – нужно отрефакторить
    enum Element {
        case onSaleLabel
        case subtitleLabel
        case cardPhotos
        case ratingCell
        case ratingFeture(Int)
        case cheapingCell
        case button(String)

        // Шапка
        case addVinHeader
        case promoHeader
        case showOnSaleOfferButton
        case calculatePriceHeader
        case writeReviewHeader
        case reportHeader
        case headerCell
        case ratingHeader
        case serviceHeader
        case taxHeader
        case cheapingHeader
        case garageCars
        case pageControl(selectedIndex: Int)
        case padeControlDotView(index: Int)

        // Проверенный собственник
        case provenOwnerHeader
        case provenOwnerCell(ProvenOwnerStatus)
        case provenOwnerPhotoController
        case provenOwnerAddPhotoButton
        case provenOwnerContinueButton
        case retakeProvenOwnerPhoto
        case retryProvenOwnerUpload
        case passVerificationButton

        // Статьи и отзывы
        case reviewsAndArticlesHeaderButton
        case feed
        case feedItem(Int)
        case segmentControl
        case feedSegment(FeedSegment)
        case emptyFeed
        case reviewScreen
        case loadMore
        case rollUp
        case writeReview
        case reviewEditor
        case titleLabel
        case offerHeader
        case priceHeader
        case priceHeaderButton
        case sellButton
        case paramsHeader
        case changeParamsButton
        case goToOwnGarageButton(_ title: String)

        // Промо
        case specialOffers
        case promo(String)
        case allPromosButton
        case openAllPromosLabel

        // Страховки
        case addInsuranceHeader
        case insuranceHeader
        case insurances
        case insurancesMore
        case insuranceRow(_ index: Int)
        case insuranceAdd

        // Карточка мечты
        case offersCountHeader
        case creditHeader
        case offersCell
        case creditCell
        case proavtoPromoCell
        case proavtoPromoCellButton
        case offerItem
        case allOffersButton
        case taxCell
        case taxRegion
        case taxModification

        // Ячейки
        case priceStatsCell
        case priceStatsSegment(Int)
        case reportPreviewCell
        case recallsSwitch
        case addVinCell
        case moreButton
        case shareButton
        case addCarButton
        case supportButton
        case supportChat
    }

    enum ProvenOwnerStatus: String {
        case unverified
        case uploading, uploadFailed
        case verifying
        case proven
        case rejected
        case anyStatus
    }

    enum FeedSegment: Int {
        case all
        case artciles
        case reviews
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .onSaleLabel:
            return "onSale_label"
        case .button(let title):
            return title
        case .addVinHeader:
            return "add_vin_header"
        case .promoHeader:
            return "garage_promo_header"
        case .provenOwnerHeader:
            return "proven_owner_header"
        case .showOnSaleOfferButton:
            return "showOffer_button"
        case .calculatePriceHeader:
            return "calculate_price_header"
        case .writeReviewHeader:
            return "write_review_header"
        case .reportHeader:
            return "report_header"
        case .addInsuranceHeader:
            return "add_insurance_header"
        case .insuranceHeader:
            return "insurance_header"
        case .headerCell:
            return "header_cell"
        case let .provenOwnerCell(status):
            if case .anyStatus = status {
                return "proven_owner_cell"
            }
            return "proven_owner_cell_\(status.rawValue)"
        case .priceStatsCell:
            return "cell_price_stats"
        case .priceStatsSegment(let index):
            return "graph_price_segment_\(index)"
        case .reportPreviewCell:
            return "GarageReportCell"
        case .recallsSwitch:
            return "garage_recalls_switch"
        case .addVinCell:
            return "add_vin_cell"
        case .passVerificationButton:
            return "pass_verification_button"
        case .provenOwnerPhotoController:
            return "proven_owner_photo_controller"
        case .provenOwnerAddPhotoButton:
            return "proven_owner_add_photo_button"
        case .provenOwnerContinueButton:
            return "Продолжить"
        case .moreButton:
            return "more"
        case .shareButton:
            return "shareButton"
        case .addCarButton:
            return "add_car"
        case .retakeProvenOwnerPhoto:
            return "Переснять"
        case .retryProvenOwnerUpload:
            return "Повторить загрузку"
        case .supportButton:
            return "proven_owner_subtitle_link"
        case .supportChat:
            return "DialogViewController"
        case .reviewsAndArticlesHeaderButton:
            return "reviews_and_articles_header"
        case .feed:
            return "feed"
        case .feedItem(let index):
            return "feed_item_\(index)"
        case .feedSegment(let segment):
            return "segmentControlSegment_\(segment.rawValue)"
        case .segmentControl:
            return "segmentControl"
        case .emptyFeed:
            return "garage_empty_feed"
        case .reviewScreen:
            return "Отзыв"
        case .loadMore:
            return "Смотреть ещё"
        case .rollUp:
            return "Свернуть"
        case .writeReview:
            return "Написать отзыв"
        case .reviewEditor:
            return "ReviewEditor"
        case .titleLabel:
            return "title_label"
        case .priceHeader:
            return "price_header"
        case .priceHeaderButton:
            return "price_header_button"
        case .offerHeader:
            return "offer_header"
        case .sellButton:
            return "sell_button"
        case .paramsHeader:
            return "params_header"
        case .changeParamsButton:
            return "change_params_button"
        case let .goToOwnGarageButton(title):
            return title
        case let .promo(title):
            return title
        case .specialOffers:
            return "PromoCell"
        case .allPromosButton:
            return "show_all_promos_button"
        case .insurances:
            return "insurances"
        case .insurancesMore:
            return "insurances_more"
        case let .insuranceRow(index):
            return "insurance_row_\(index)"
        case .insuranceAdd:
            return "insurance_add"
        case .cardPhotos:
            return "garage_photos"
        case .ratingHeader:
            return "garage_rating_header"
        case .ratingFeture(let index):
            return "feature_\(index)"
        case .ratingCell:
            return "garage_rating_cell"
        case .serviceHeader:
            return "garage_service_header"
        case .taxHeader:
            return "garage_tax_header"
        case .creditHeader:
            return "garage_credit_header"
        case .cheapingHeader:
            return "header_cheaping_stats"
        case .offersCountHeader:
            return "offers_count_header"
        case .offersCell:
            return "garage_offers_cell"
        case .cheapingCell:
            return "cell_cheapening_graph"
        case .creditCell:
            return "garage_credit_cell"
        case .proavtoPromoCell:
            return "garage_proavto_promo_cell"
        case .proavtoPromoCellButton:
            return "garage_proavto_promo_cell_button"
        case .offerItem:
            return "garage_offer_item"
        case .allOffersButton:
            return "garage_all_offers_button"
        case .taxCell:
            return "TaxCell"
        case .taxRegion:
            return "Выберите регион учёта"
        case .taxModification:
            return "Укажите двигатель"
        case .subtitleLabel:
            return "subtitle_label"
        case .garageCars:
            return "garage_cars"
        case .pageControl(selectedIndex: let index):
            return "garage_card_page_control_\(index)"
        case .padeControlDotView(let index):
            return "pageControl_dotView_\(index)"
        case .openAllPromosLabel:
            return "См. все"
        }

    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}
