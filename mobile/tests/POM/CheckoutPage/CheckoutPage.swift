import FormKit
import MarketCashback
import MarketCheckoutFeature
import MarketUI
import UIUtils
import XCTest

final class CheckoutPage: PageObject, UniformCollectionViewPage {

    typealias CellPage = CheckoutCellPage

    typealias AccessibilityIdentifierProvider = CheckoutCollectionViewCellsAccessibility

    final class CheckoutCellPage: PageObject {}

    static var current: CheckoutPage {
        let elem = XCUIApplication().otherElements[CheckoutAccessibility.root]
        return CheckoutPage(element: elem)
    }

    var collectionView: XCUIElement {
        element
    }

    var recipientInfoCell: RecipientInfoCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.recipientInfoCell)
        return RecipientInfoCell(element: elem)
    }

    var paymentMethodCell: PaymentMethodCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.paymentMethodCell)
        return PaymentMethodCell(element: elem)
    }

    var liftingDetailsTypeCell: LiftingDetailsTypeCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.liftingOptionsChooserButton)
        return LiftingDetailsTypeCell(element: elem)
    }

    var summaryItemsCell: SummaryPrimaryCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.itemsCell)
        return SummaryPrimaryCell(element: elem)
    }

    var summaryServiceCell: SummaryPrimaryCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.servicesCell)
        return SummaryPrimaryCell(element: elem)
    }

    var summaryDiscountCell: SummaryPrimaryCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.discountCell)
        return SummaryPrimaryCell(element: elem)
    }

    var summaryPromoCodeDiscountCell: SummaryPrimaryCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.promoCodeDiscountCell)
        return SummaryPrimaryCell(element: elem)
    }

    var summaryPriceDropDiscountCell: SummaryPrimaryCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.priceDropDiscountCell)
        return SummaryPrimaryCell(element: elem)
    }

    var summaryDeliveryCell: SummaryPrimaryCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.deliveryCell)
        return SummaryPrimaryCell(element: elem)
    }

    var summaryCashbackCell: SummaryCashbackCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.cashbackCell)
        return SummaryCashbackCell(element: elem)
    }

    var summaryTotalCell: SummaryPrimaryCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.totalCell)
        return SummaryPrimaryCell(element: elem)
    }

    var summaryTotalPostpaidCell: SummaryPrimaryCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.totalPostpaidCell)
        return SummaryPrimaryCell(element: elem)
    }

    var summaryTotalPrepaidCell: SummaryPrimaryCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.totalPrepaidCell)
        return SummaryPrimaryCell(element: elem)
    }

    var summaryLiftingCell: SummaryPrimaryCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.liftingCell)
        return SummaryPrimaryCell(element: elem)
    }

    var summaryHelpIsNearCell: SummaryPrimaryCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.SummaryCell.helpIsNearCell)
        return SummaryPrimaryCell(element: elem)
    }

    var bnplSwitchLabel: XCUIElement {
        element
            .textViews
            .matching(identifier: CheckoutBNPLAccessibility.bnplSwitchLabel)
            .firstMatch
    }

    var bnplSwitch: XCUIElement {
        element
            .switches
            .matching(identifier: CheckoutBNPLAccessibility.bnplSwitch)
            .firstMatch
    }

    var bnplPlanCell: BNPLPlanCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.bnplPlanCell)
        return BNPLPlanCell(element: elem)
    }

    var bnplFirstPaymentDetailsButton: BnplFirstPaymentDetailsButton {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.bnplFirstPaymentDetailsButton)
        return BnplFirstPaymentDetailsButton(element: elem)
    }

    var installmentsCell: InstallmentsCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.installmentsCell)
        return InstallmentsCell(element: elem)
    }

    var paymentButton: PaymentButton {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.paymentButton)
        return PaymentButton(element: elem)
    }

    var homePlusCell: HomePlusCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.homePlusCell)
        return HomePlusCell(element: elem)
    }

    var disclaimerHomePlusCell: DisclaimerHomePlusCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.disclaimerHomePlusCell)
        return DisclaimerHomePlusCell(element: elem)
    }

    var globalNoticeSection: XCUIElement {
        cellUniqueElement(withIdentifier: CheckoutAccessibility.globalNotice).otherElements.firstMatch
    }

    var secureOfferBadgeCell: SecureOfferBadgeCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.secureOfferBadgeCell)
        return SecureOfferBadgeCell(element: elem)
    }

    var termsDisclaimerCell: TermsDisclaimerCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.termsDisclaimerCell)
        return TermsDisclaimerCell(element: elem)
    }

    var merchantInfoDisclaimerCell: MerchantInfoDisclaimer {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.merchantInfoDisclaimerCell)
        return MerchantInfoDisclaimer(element: elem)
    }

    var cashbackSpendOptionCell: CashbackOptionCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.cashbackSpendCell)
        return CashbackOptionCell(element: elem)
    }

    var cashbackEmitOptionCell: CashbackOptionCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.cashbackEmitCell)
        return CashbackOptionCell(element: elem)
    }

    var cashbackDeliveryPromotionCell: CashbackDeliveryPromotionCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.deliveryPromotionCell)
        return CashbackDeliveryPromotionCell(element: elem)
    }

    var betterDeliveryOptionCell: XCUIElement {
        cellUniqueElement(withIdentifier: CheckoutAccessibility.betterDeliveryOptionCell)
            .textViews
            .firstMatch
    }

    var onDemandHintView: XCUIElement {
        XCUIApplication()
            .otherElements[CheckoutAccessibility.onDemandHintView]
            .staticTexts
            .firstMatch
    }

    func deliverySlotsCell(at index: Int = 0) -> DeliverySlotsCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.DeliverySlots.root, index: index)
        return DeliverySlotsCell(element: elem)
    }

    func addressChooserButton(at index: Int = 0) -> AddressChooserButton {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.addressChooserButton, index: index)
        return AddressChooserButton(element: elem)
    }

    func outletChooserButton(at index: Int = 0) -> OutletChooserButton {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.outletChooserButton, index: index)
        return OutletChooserButton(element: elem)
    }

    func noticeCell(at index: Int = 0) -> NoticeCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.noticeCell, index: index)
        return NoticeCell(element: elem)
    }

    func shipmentHeaderCell(at index: Int = 0) -> ShipmentHeaderCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.shipmentHeaderCell, index: index)
        return ShipmentHeaderCell(element: elem)
    }

    func onDemandDateSelectorCell(at index: Int) -> SelectorPage {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.onDemandDateSelectorCell, index: index)
        return SelectorPage(element: elem)
    }

    func serviceDateSelectorCell(at index: Int) -> SelectorPage {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.serviceDateSelectorCell, index: index)
        return SelectorPage(element: elem)
    }

    func dateSelectorCell(at index: Int) -> SelectorPage {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.dateSelectorCell, index: index)
        return SelectorPage(element: elem)
    }

    func shipmentCell(at index: Int) -> ShipmentCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.shipmentItemCell, index: index)
        return ShipmentCell(element: elem)
    }

    func legalInfoCell(at index: Int = 0) -> LegalInfoCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.legalInfoCell, index: index)
        return LegalInfoCell(element: elem)
    }

    func postpaidCell(at index: Int = 0) -> NoticeCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.postpaidCell, index: index)
        return NoticeCell(element: elem)
    }

    func onlyCashCell(at index: Int = 0) -> NoticeCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.onlyCashCell, index: index)
        return NoticeCell(element: elem)
    }

    func additionalServiceSelectorCell(at index: Int = 0) -> AdditionalServiceCell {
        let elemt = cellUniqueElement(withIdentifier: CheckoutAccessibility.additionalServiceSelectorCell, index: index)
        return AdditionalServiceCell(element: elemt)
    }

    func timeslotSelectorCell(at index: Int = 0) -> SelectorPage {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.timeslotSelectorCell, index: index)
        return SelectorPage(element: elem)
    }

    func stationDisclaimerCell() -> TermsDisclaimerCell {
        let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.stationDisclaimerCell)
        return TermsDisclaimerCell(element: elem)
    }
}

// MARK: - Nested types

extension CheckoutPage {

    final class AddressChooserButton: PageObject {
        var title: XCUIElement {
            element.textViews
                .firstMatch
        }
    }

    final class OutletChooserButton: PageObject {
        var title: XCUIElement {
            element.textViews.firstMatch
        }

        var titleLabel: XCUIElement {
            element.staticTexts
                .matching(identifier: CheckoutOutletInfoViewAccessibility.titleLabel)
                .element
        }

        var addressLabel: XCUIElement {
            element.staticTexts
                .matching(identifier: CheckoutOutletInfoViewAccessibility.addressLabel)
                .element
        }

        var shortCashbackLabel: XCUIElement {
            element.staticTexts
                .matching(identifier: CheckoutOutletInfoViewAccessibility.shortCashbackInfoLabel)
                .element
        }

        var longCashbackLabel: XCUIElement {
            element.staticTexts
                .matching(identifier: CheckoutOutletInfoViewAccessibility.longCashbackInfoLabel)
                .element
        }
    }

    final class ShipmentCell: PageObject {
        func tap() -> CheckoutShipmentPopupPage {
            element.tap()
            let elem = XCUIApplication().otherElements[CheckoutShipmentPopupViewControllerAccessibility.root]
            return CheckoutShipmentPopupPage(element: elem)
        }
    }

    final class RecipientInfoCell: PageObject {
        var title: XCUIElement {
            element.textViews
                .firstMatch
        }

        func tap() -> CheckoutRecipientPage {
            element.tap()
            let elem = XCUIApplication().otherElements[CheckoutRecipientViewAccessibility.root]
            return CheckoutRecipientPage(element: elem)
        }

        func tap() -> CheckoutContactsPage {
            element.tap()
            let elem = XCUIApplication().otherElements[CheckoutContactsChooserAccessibility.root]
            return CheckoutContactsPage(element: elem)
        }
    }

    final class PaymentMethodCell: PageObject {
        var title: XCUIElement {
            element.textViews.firstMatch
        }

        func tap() -> CheckoutPaymentMethodPopupPage {
            element.tap()
            let elem = XCUIApplication().otherElements[CheckoutPaymentMethodAccessibility.root]
            return CheckoutPaymentMethodPopupPage(element: elem)
        }
    }

    final class LiftingDetailsTypeCell: PageObject {
        var title: XCUIElement {
            element.textViews.firstMatch
        }

        func tap() -> CheckoutLiftingDetailsPage {
            element.tap()
            let elem = XCUIApplication().otherElements[CheckoutLiftingDetailsAccessibility.root]
            return CheckoutLiftingDetailsPage(element: elem)
        }
    }

    final class SummaryCashbackCell: PageObject {
        var title: XCUIElement {
            element.staticTexts
                .matching(identifier: HorizontalTitleDetailsAccessibility.title)
                .firstMatch
        }

        var details: XCUIElement {
            element.staticTexts
                .matching(identifier: HorizontalTitleDetailsAccessibility.details)
                .firstMatch
        }

        func tap() -> CashbackAboutPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: CashbackAboutAccessibility.root)
                .firstMatch

            return CashbackAboutPage(element: elem)
        }
    }

    final class SummaryPrimaryCell: PageObject {
        var title: XCUIElement {
            element.staticTexts
                .matching(identifier: HorizontalTitleDetailsAccessibility.title)
                .firstMatch
        }

        var details: XCUIElement {
            element.staticTexts
                .matching(identifier: HorizontalTitleDetailsAccessibility.details)
                .firstMatch
        }
    }

    final class BNPLPlanCell: PageObject {

        var planView: XCUIElement {
            element
                .otherElements
                .matching(identifier: CheckoutBNPLAccessibility.bnplPlanView)
                .firstMatch
        }
    }

    final class BnplFirstPaymentDetailsButton: PageObject {

        var title: XCUIElement {
            element
                .textViews
                .matching(identifier: CheckoutAccessibility.bnplFirstPaymentDetailsButtonTitle)
                .firstMatch
        }
    }

    final class InstallmentsCell: PageObject {

        var periodTitle: XCUIElement {
            element
                .staticTexts
                .matching(identifier: CheckoutInstallmentsAccessibility.installmentsPeriodTitle)
                .firstMatch
        }

        var selector: InstallmentsSelectorPage {
            let elem = element
                .collectionViews
                .matching(identifier: CheckoutInstallmentsAccessibility.installmentsSelector)
                .firstMatch
            return InstallmentsSelectorPage(element: elem)
        }

        var monthlyPayment: XCUIElement {
            element
                .staticTexts
                .matching(identifier: CheckoutInstallmentsAccessibility.installmentsMonthlyPayment)
                .firstMatch
        }
    }

    final class InstallmentsSelectorPage: PageObject, InstallmentsSelectorPopupEntryPoint {

        var selectedCell: SelectedInstallmentCellPage {
            let elem = element
                .cells
                .matching(identifier: CheckoutInstallmentsAccessibility.installmentsSelectedCell)
                .firstMatch
            return SelectedInstallmentCellPage(element: elem)
        }
    }

    final class SelectedInstallmentCellPage: PageObject {

        var term: XCUIElement {
            element.staticTexts.firstMatch
        }
    }

    final class PaymentButton: PageObject {
        var title: XCUIElement {
            element.staticTexts
                .firstMatch
        }

        @discardableResult
        func tap() -> FinishMultiorderPage {
            element.tap()
            let elem = XCUIApplication().otherElements[FinishMultiorderAccessibilty.root]
            return FinishMultiorderPage(element: elem)
        }

        func tapToBarrierView() -> BarrierViewPage {
            element.tap()
            let elem = XCUIApplication().otherElements[BarrierViewAccessibility.root]
            return BarrierViewPage(element: elem)
        }
    }

    final class HomePlusCell: PageObject {
        var title: XCUIElement {
            element.textViews
                .firstMatch
        }

        func tap() -> HomePlusPage {
            element.tap()
            let elem = XCUIApplication().otherElements[PlusOnboardingAccessibility.root]
            return HomePlusPage(element: elem)
        }
    }

    final class DisclaimerHomePlusCell: PageObject {
        var title: XCUIElement {
            element.textViews
                .firstMatch
        }
    }

    final class SecureOfferBadgeCell: PageObject {
        var title: XCUIElement {
            element.staticTexts
                .firstMatch
        }
    }

    final class NoticeCell: PageObject {
        var title: XCUIElement {
            element.staticTexts
                .firstMatch
        }
    }

    final class ShipmentHeaderCell: PageObject {
        var title: XCUIElement {
            element.textViews
                .firstMatch
        }
    }

    final class MerchantInfoCell: PageObject, MerchantPopupEntryPoint {}

    final class LegalInfoCell: PageObject {
        var title: XCUIElement {
            element.textViews
                .firstMatch
        }
    }

    final class TermsDisclaimerCell: PageObject {
        var title: XCUIElement {
            element.textViews.firstMatch
        }
    }

    final class MerchantInfoDisclaimer: PageObject, MerchantPopupEntryPoint {
        var title: XCUIElement {
            element.textViews
                .firstMatch
        }
    }

    final class CashbackOptionCell: PageObject {
        var title: XCUIElement {
            element.staticTexts
                .firstMatch
        }
    }

    final class CashbackDeliveryPromotionCell: PageObject {
        var promotionView: CashbackDeliveryPromotionPage {
            CashbackDeliveryPromotionPage(
                element: element.otherElements[CashbackDeliveryPromotionViewAccessibility.root]
                    .firstMatch
            )
        }
    }

    final class OnDemandSelectorCell: PageObject {
        var isSelected: Bool {
            element.otherElements.matching(identifier: TiledSelectorAccessibility.Selectability.selected).element
                .isVisible
        }

        var isUnavailable: Bool {
            element.otherElements.matching(identifier: TiledSelectorAccessibility.Selectability.unavailable).element
                .isVisible
        }

        func tapUnavailable() -> BarrierViewPage {
            element.tap()
            let elem = XCUIApplication().otherElements[BarrierViewAccessibility.root]
            return BarrierViewPage(element: elem)
        }

        var infoButton: XCUIElement {
            element.buttons.matching(identifier: TiledSelectorAccessibility.infoButton).element
        }
    }

    final class DefaultServiceSelectorCell: PageObject {
        var isSelected: Bool {
            element.otherElements.matching(identifier: TiledSelectorAccessibility.Selectability.selected).element
                .isVisible
        }

        var title: String {
            element.staticTexts.firstMatch.label
        }
    }

    final class AdditionalServiceCell: PageObject, ServicesEntryPoint {
        var title: XCUIElement {
            element
                .textViews
                .firstMatch
        }

        var image: XCUIElement {
            element
                .images
                .matching(identifier: "CheckoutSetupService")
                .firstMatch
        }
    }

    final class DeliverySlotsCell: PageObject, CollectionViewPage {
        typealias AccessibilityIdentifierProvider = ScrollBoxCellsAccessibility

        var collectionView: XCUIElement {
            element
                .collectionViews
                .matching(identifier: CheckoutAccessibility.DeliverySlots.collectionView)
                .element
        }

        func slot(at index: Int) -> DefaultServiceSelectorCell {
            let element = cellElement(at: IndexPath(row: index, section: 0))
            return DefaultServiceSelectorCell(element: element)
        }

        var onDemandSelectorCell: OnDemandSelectorCell {
            let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.DeliverySlots.onDemandSelectorCell)
            return OnDemandSelectorCell(element: elem)
        }

        var defaultServiceSelectorCell: DefaultServiceSelectorCell {
            let elem = cellUniqueElement(withIdentifier: CheckoutAccessibility.DeliverySlots.defaultServiceSelectorCell)
            return DefaultServiceSelectorCell(element: elem)
        }
    }
}

final class CashbackDeliveryPromotionPage: PageObject {
    var title: XCUIElement {
        element.staticTexts.matching(identifier: CashbackDeliveryPromotionViewAccessibility.title).element
    }

    var description: XCUIElement {
        element.staticTexts.matching(identifier: CashbackDeliveryPromotionViewAccessibility.description).firstMatch
    }

    var questionMark: CashbackPromotionInfoButton {
        let el = element.buttons.matching(identifier: CashbackDeliveryPromotionViewAccessibility.questionMark).element
        return CashbackPromotionInfoButton(element: el)
    }

    final class CashbackPromotionInfoButton: PageObject {
        func tap() -> CashbackDeliveryPromotionAboutPage {
            element.tap()

            let elem = XCUIApplication()
                .otherElements
                .matching(identifier: CashbackDeliveryPromotionAboutAccessibility.root)
                .firstMatch

            return CashbackDeliveryPromotionAboutPage(element: elem)
        }
    }
}

final class CashbackDeliveryPromotionAboutPage: PageObject, PopupPage {
    static var rootIdentifier: String = CashbackDeliveryPromotionAboutAccessibility.root
}
