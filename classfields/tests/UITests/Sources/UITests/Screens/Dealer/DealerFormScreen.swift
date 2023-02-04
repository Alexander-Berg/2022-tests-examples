import XCTest
import Snapshots

final class DealerFormScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    lazy var collectionView = findAll(.collectionView).firstMatch
    lazy var refreshingControl = self.scrollableElement.activityIndicators.firstMatch

    lazy var closeButton = findAll(.button)["Закрыть"].firstMatch
    lazy var navbarSearch = find(by: "app.views.cv.searchbar").firstMatch
    lazy var headerView = find(by: "header.photo_video.list").firstMatch
    lazy var emptyPanoramaView = find(by: "Панорамы").firstMatch
    lazy var bothPanoramaView = find(by: "2 панорамы").firstMatch

    lazy var motoSubcategoryField = find(by: "dealer.form.group.moto.field.pickType").firstMatch
    lazy var trucksSubcategoryField = find(by: "dealer.form.group.trucks.field.pickCategory").firstMatch

    // MARK: - Cars

    // TODO: enum

    lazy var autoMarkField = find(by: "dealer.form.group.auto.field.pickMark").firstMatch
    lazy var autoModelField = find(by: "dealer.form.group.auto.field.pickModel").firstMatch
    lazy var autoYearField = find(by: "dealer.form.group.auto.field.pickYear").firstMatch
    lazy var autoGenerationField = find(by: "dealer.form.group.auto.field.pickGeneration").firstMatch
    lazy var autoBodyTypeField = find(by: "dealer.form.group.auto.field.pickBodyType").firstMatch
    lazy var autoEngineTypeField = find(by: "dealer.form.group.auto.field.pickEngineType").firstMatch
    lazy var autoGearTypeField = find(by: "dealer.form.group.auto.field.pickGearType").firstMatch
    lazy var autoTransmissionTypeField = find(by: "dealer.form.group.auto.field.pickTransmissionType").firstMatch
    lazy var autoModificationField = find(by: "dealer.form.group.auto.field.pickModification").firstMatch
    lazy var autoColorField = find(by: "dealer.form.group.auto.field.pickColor").firstMatch
    lazy var autoComplectationField = find(by: "dealer.form.group.auto.field.pickComplectation").firstMatch
    lazy var autoAvailabilityField = find(by: "dealer.form.group.auto.field.pickAvailability").firstMatch
    lazy var autoMileageField = find(by: "dealer.form.group.auto.field.mileage").firstMatch

    // MARK: -

    lazy var trucksCategoryField = find(by: "dealer.form.group.trucks.field.pickCategory").firstMatch
    lazy var trucksMarkField = find(by: "dealer.form.group.trucks.field.pickMark").firstMatch
    lazy var trucksModelField = find(by: "dealer.form.group.trucks.field.pickModel").firstMatch

    lazy var motoCategoryField = find(by: "dealer.form.group.moto.field.pickCategory").firstMatch
    lazy var motoMarkField = find(by: "dealer.form.group.moto.field.pickMark").firstMatch
    lazy var motoModelField = find(by: "dealer.form.group.moto.field.pickModel").firstMatch

    lazy var motoMileageField = find(by: "dealer.form.group.moto.field.Пробег, км").firstMatch
    lazy var trucksMileageField = find(by: "dealer.form.group.trucks.field.Пробег, км").firstMatch

    // MARK: - Extra

    lazy var notRegisteredInRuField = find(by: "dealer.form.group.extra.field.reg_ru").firstMatch
    lazy var licensePlateField = find(by: "dealer.form.group.extra.field.plate").firstMatch
    lazy var govNumberInputField = find(by: "app.views.gov_number").firstMatch
    lazy var vinField = find(by: "dealer.form.group.extra.field.vin").firstMatch
    lazy var stsField = find(by: "dealer.form.group.extra.field.sts").firstMatch
    lazy var ptsField = find(by: "dealer.form.group.extra.field.pickPTS").firstMatch
    lazy var ownersNumberField = find(by: "dealer.form.group.extra.field.pickOwnersNumberByPTS").firstMatch
    lazy var purchaseDateField = find(by: "dealer.form.group.extra.field.pickPurchaseDate").firstMatch
    lazy var warrantyValidField = find(by: "dealer.form.group.extra.field.warranty").firstMatch
    lazy var warrantyEndDateField = find(by: "dealer.form.group.extra.field.pickWarrantyEndDate").firstMatch
    lazy var aruOnlyField = find(by: "group.extra.aru_only").firstMatch
    lazy var offerTextField = find(by: "group.extra.offer_text").firstMatch
    lazy var modelOptionsField = find(by: "group.extra.field.select_model_opt").firstMatch
    lazy var isRightWheelField = find(by: "group.extra.field.right_wheel").firstMatch
    lazy var customsNotClearedField = find(by: "group.extra.field.customs").firstMatch
    lazy var isAutogasField = find(by: "group.extra.field.autogas").firstMatch
    lazy var isBeatenField = find(by: "dealer.form.group.extra.field.beaten").firstMatch
    lazy var damagePickerField = find(by: "dealer.form.group.extra.field.damage").firstMatch

    // MARK: - Price

    lazy var priceField = find(by: "dealer.form.group.price.field.price").firstMatch
    lazy var priceCurrencyField = find(by: "dealer.form.group.price.field.currency").firstMatch
    lazy var creditDiscountField = find(by: "dealer.form.group.price.field.credit_disc").firstMatch
    lazy var insuranceDiscountField = find(by: "dealer.form.group.price.field.insurance_disc").firstMatch
    lazy var tradeInDiscountField = find(by: "dealer.form.group.price.field.trade-in_disc").firstMatch
    lazy var maxDiscountField = find(by: "dealer.form.group.price.field.max_disc").firstMatch
    lazy var leasingDiscountField = find(by: "dealer.form.group.price.field.leasing_disc").firstMatch
    lazy var canExchangeField = find(by: "dealer.form.group.price.field.exchange").firstMatch
    lazy var withNDSField = find(by: "dealer.form.group.price.field.with_nds").firstMatch

    // MARK: - Footer

    lazy var publishButton = find(by: "dealer.form.footer.publish_btn").firstMatch
    lazy var publishHiddenButton = find(by: "dealer.form.footer.publish_hidden_btn").firstMatch

    // MARK: - Tests

    func field(_ elem: XCUIElement) -> XCUIElement {
        return elem
    }
}
