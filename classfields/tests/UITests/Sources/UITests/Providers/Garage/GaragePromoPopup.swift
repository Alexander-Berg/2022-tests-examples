final class GaragePromoPopup: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "partner_promo_popup"
    static let rootElementName = "PopUp промо в гараже"

    enum Element: String {
        case promoPopUpButton = "promo_action_button"
        case disclaimer = "disclaimer"
        case closeButton = "garage_popUp_close_button"
        case logo = "garage_popUp_logo"
    }
}
