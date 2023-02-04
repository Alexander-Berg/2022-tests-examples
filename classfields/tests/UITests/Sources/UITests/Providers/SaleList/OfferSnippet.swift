import XCTest
import Snapshots

final class OfferSnippet: BaseSteps, UIElementProvider {
    enum Element {
        case callButton
        case titleLabel
        case priceLabel
        case creditLink
        case panorama(_ positionInGallery: Int)
        case photo(_ positionInGallery: Int)
        case video(_ positionInGallery: Int)
        case videoPlayIcon
        case contactInfo
        case callButtonInPhotos
        case chatButton
        case showReportButton
        case characteristics
        case catalogPhotosBadge
        case saleFromCompanyIcon
        case dealerContacts
        case deliveryIcon
        case footerSubtitleLabel
        case moreButton
        case comparisonButton
        case favoriteButton(_ id: String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .callButtonInPhotos, .callButton:
            return "call_button"
        case .titleLabel:
            return "title_label"
        case .priceLabel:
            return "price_label"
        case .creditLink:
            return "carCreditPrice"
        case let .panorama(index):
            return "gallery_panorama_\(index)"
        case let .video(index):
            return "gallery_video_\(index)"
        case let .photo(index):
            return "gallery_photo_\(index)"
        case .videoPlayIcon:
            return "gallery_video_play_icon"
        case .contactInfo:
            return "gallery_contact_cell"
        case .chatButton:
            return "chat_button"
        case .showReportButton:
            return "view.snippet.show-report-btn"
        case .characteristics:
            return "characteristics"
        case .catalogPhotosBadge:
            return "catalog_photos"
        case .saleFromCompanyIcon:
            return "sale_from_company_icon"
        case .dealerContacts:
            return "Контакты"
        case .deliveryIcon:
            return "delivery_icon"
        case .footerSubtitleLabel:
            return "footer_subtitle_label"
        case .moreButton:
            return "more_button"
        case .comparisonButton:
            return "comparison_button"
        case let .favoriteButton(id):
            return "fav_offer_\(id)"
        }
    }
}
