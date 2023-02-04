@testable import AutoRuDealerForm
import AutoRuProtoModels
import XCTest

extension DealerFormInteractorModel {
    static func dummy() -> DealerFormInteractorModel {
        return DealerFormInteractorModel(isEdit: false, requestNewDraft: false)
    }
}

extension DealerFormViewControllerModel.Price.Field {
    var isCreditDiscount: Bool {
        if case .creditDiscount = self {
            return true
        } else {
            return false
        }
    }

    var isInsuranceDiscount: Bool {
        if case .insuranceDiscount = self {
            return true
        } else {
            return false
        }
    }

    var isTradeInDiscount: Bool {
        if case .tradeInDiscount = self {
            return true
        } else {
            return false
        }
    }

    var isMaxDiscount: Bool {
        if case .maxDiscount = self {
            return true
        } else {
            return false
        }
    }

    var isLeasingDiscount: Bool {
        if case .leasingDisсount = self {
            return true
        } else {
            return false
        }
    }
}

final class DealerFormTests: BaseUnitTest {
    typealias PriceField = DealerFormViewControllerModel.Price.Field
    typealias PriceFieldPredicate = (DealerFormViewControllerModel.Price.Field) -> Bool

    private func createOffer(section: Auto_Api_Section, category: Auto_Api_Category) -> Auto_Api_Offer {
        var offer = Auto_Api_Offer()
        offer.section = section
        offer.category = category
        return offer
    }

    private func validatePriceFields(
        offer: Auto_Api_Offer,
        toContain containCallbacks: [PriceFieldPredicate],
        notToContain notContainCallbacks: [PriceFieldPredicate]
    ) {
        let decorator = DealerFormPriceDecorator(
            interactorModel: DealerFormInteractorModel.dummy(),
            model: offer,
            errors: [:]
        )

        let price = decorator.makeForm()

        for containCallback in containCallbacks {
            XCTAssert(price.fields.contains(where: containCallback))
        }

        for notContainCallback in notContainCallbacks {
            XCTAssertFalse(price.fields.contains(where: notContainCallback))
        }
    }

    static let isCreditDiscount: PriceFieldPredicate = { $0.isCreditDiscount }
    static let isLeasingDiscount: PriceFieldPredicate = { $0.isLeasingDiscount }
    static let isTradeInDiscount: PriceFieldPredicate = { $0.isTradeInDiscount }
    static let isInsuranceDiscount: PriceFieldPredicate = { $0.isInsuranceDiscount }
    static let isMaxDiscount: PriceFieldPredicate = { $0.isMaxDiscount }

    func test_newTrucksPriceDiscounts() {
        func newTrucksOffer(subcategory: Auto_Api_TruckCategory) -> Auto_Api_Offer {
            var offer = createOffer(section: .new, category: .trucks)
            offer.truckInfo.truckCategory = subcategory
            return offer
        }

        let lcvOffer = newTrucksOffer(subcategory: .lcv)
        validatePriceFields(
            offer: lcvOffer,
            toContain: [
                Self.isCreditDiscount,
                Self.isLeasingDiscount,
                Self.isTradeInDiscount,
                Self.isInsuranceDiscount,
                Self.isMaxDiscount
            ],
            notToContain: []
        )

        let otherSubcategories: [Auto_Api_TruckCategory] = [
            .truck, .trailer, .swapBody, .bus, .artic,
            .agricultural, .construction, .autoloader, .crane, .dredge,
            .bulldozers, .craneHydraulics, .municipal
        ]
        for subcategory in otherSubcategories {
            let otherSubcategoryOffer = newTrucksOffer(subcategory: subcategory)
            validatePriceFields(
                offer: otherSubcategoryOffer,
                toContain: [
                    Self.isLeasingDiscount,
                    Self.isMaxDiscount
                ],
                notToContain: [
                    Self.isCreditDiscount,
                    Self.isTradeInDiscount,
                    Self.isInsuranceDiscount
                ]
            )
        }
    }

    func test_usedTrucksPriceDiscounts() {
        let allSubcategories: [Auto_Api_TruckCategory] = [
            .lcv, .truck, .trailer, .swapBody, .bus, .artic,
            .agricultural, .construction, .autoloader, .crane, .dredge,
            .bulldozers, .craneHydraulics, .municipal
        ]
        for subcategory in allSubcategories {
            var offer = createOffer(section: .used, category: .trucks)
            offer.truckInfo.truckCategory = subcategory
            validatePriceFields(
                offer: offer,
                toContain: [],
                notToContain: [
                    Self.isLeasingDiscount,
                    Self.isMaxDiscount,
                    Self.isCreditDiscount,
                    Self.isTradeInDiscount,
                    Self.isInsuranceDiscount
                ]
            )
        }
    }

    func test_motoPriceDiscounts() {
        let allSubcategories: [Auto_Api_MotoCategory] = [
            .motorcycle, .atv, .snowmobile, .scooters
        ]
        for subcategory in allSubcategories {
            var newOffer = createOffer(section: .new, category: .moto)
            newOffer.motoInfo.motoCategory = subcategory
            validatePriceFields(
                offer: newOffer,
                toContain: [],
                notToContain: [
                    Self.isLeasingDiscount,
                    Self.isMaxDiscount,
                    Self.isCreditDiscount,
                    Self.isTradeInDiscount,
                    Self.isInsuranceDiscount
                ]
            )

            var usedOffer = createOffer(section: .used, category: .moto)
            usedOffer.motoInfo.motoCategory = subcategory
            validatePriceFields(
                offer: usedOffer,
                toContain: [],
                notToContain: [
                    Self.isLeasingDiscount,
                    Self.isMaxDiscount,
                    Self.isCreditDiscount,
                    Self.isTradeInDiscount,
                    Self.isInsuranceDiscount
                ]
            )
        }
    }

    func test_draftResetNewToUsed() {
        var offer = Auto_Api_Offer()
        offer.section = .used

        offer.discountOptions.credit = 123
        offer.discountOptions.insurance = 456
        offer.discountOptions.tradein = 789
        offer.discountOptions.maxDiscount = 2233

        do {
            // для легковых ничего не меняется
            offer.category = .cars

            var draft = Auto_Api_DraftResponse()
            draft.offer = offer

            DealerFormDraftCategoryChangeValueResetter.update(draft: &draft)
            XCTAssertTrue(draft.offer.hasDiscountOptions)
        }
        do {
            offer.category = .trucks

            var draft = Auto_Api_DraftResponse()
            draft.offer = offer

            DealerFormDraftCategoryChangeValueResetter.update(draft: &draft)
            XCTAssertFalse(draft.offer.hasDiscountOptions)
        }
        do {
            offer.category = .moto

            var draft = Auto_Api_DraftResponse()
            draft.offer = offer

            DealerFormDraftCategoryChangeValueResetter.update(draft: &draft)
            XCTAssertFalse(draft.offer.hasDiscountOptions)
        }
    }

    func test_draftResetUsedToNew() {
        var offer = Auto_Api_Offer()
        offer.section = .new

        offer.state.mileage = 123
        offer.state.damages = [Auto_Api_Damage()]
        offer.state.condition = .broken

        offer.documents.licensePlate = "plate"
        offer.documents.pts = .duplicate
        offer.documents.ownersNumber = 22
        offer.documents.warranty = true

        offer.documents.warrantyExpire = Auto_Api_Date()
        offer.documents.purchaseDate = Auto_Api_Date()

        var draft = Auto_Api_DraftResponse()
        draft.offer = offer

        DealerFormDraftCategoryChangeValueResetter.update(draft: &draft)
        offer = draft.offer // updated

        XCTAssertEqual(offer.state.mileage, 0)
        XCTAssertTrue(offer.state.damages.isEmpty)
        XCTAssertEqual(offer.state.condition, Auto_Api_Condition())

        XCTAssertTrue(offer.documents.licensePlate.isEmpty)
        XCTAssertEqual(offer.documents.pts, Auto_Api_PtsStatus())
        XCTAssertEqual(offer.documents.ownersNumber, 0)

        XCTAssertFalse(offer.documents.hasWarrantyExpire)
        XCTAssertFalse(offer.documents.hasPurchaseDate)
    }

    func test_draftPresetTrucksNDS() {
        do {
            // при добавлении новых
            var offer = Auto_Api_Offer()
            offer.section = .new
            offer.category = .trucks

            var draft = Auto_Api_DraftResponse()
            draft.offer = offer

            draft = DealerFormValuesPresetter.preset(to: draft, isEdit: false)!
            XCTAssertTrue(draft.offer.priceInfo.withNds.value)
        }
        do {
            // при добавлении с пробегом
            var offer = Auto_Api_Offer()
            offer.section = .used
            offer.category = .trucks

            var draft = Auto_Api_DraftResponse()
            draft.offer = offer

            draft = DealerFormValuesPresetter.preset(to: draft, isEdit: false)!
            XCTAssertFalse(draft.offer.priceInfo.withNds.value)
        }
        do {
            // при редактировании новых
            var offer = Auto_Api_Offer()
            offer.section = .new
            offer.category = .trucks

            var draft = Auto_Api_DraftResponse()
            draft.offer = offer

            draft = DealerFormValuesPresetter.preset(to: draft, isEdit: true)!
            XCTAssertFalse(draft.offer.priceInfo.withNds.value)
        }
        do {
            // при добавлении с пробегом
            var offer = Auto_Api_Offer()
            offer.section = .used
            offer.category = .trucks

            var draft = Auto_Api_DraftResponse()
            draft.offer = offer

            draft = DealerFormValuesPresetter.preset(to: draft, isEdit: true)!
            XCTAssertFalse(draft.offer.priceInfo.withNds.value)
        }
    }

    func test_draftPresetAvailabilityOnlyForNonEditOffers() {
        do {
            var offer = Auto_Api_Offer()
            offer.documents.vin = "VIN"
            offer.availability = .onOrder

            var draft = Auto_Api_DraftResponse()
            draft.offer = offer

            draft = DealerFormValuesPresetter.preset(to: draft, isEdit: false)!
            XCTAssertEqual(draft.offer.availability, .inStock)
        }
        do {
            var offer = Auto_Api_Offer()
            offer.documents.vin = "VIN"
            offer.availability = .onOrder

            var draft = Auto_Api_DraftResponse()
            draft.offer = offer

            draft = DealerFormValuesPresetter.preset(to: draft, isEdit: true)!
            XCTAssertEqual(draft.offer.availability, .onOrder)
        }
    }
}

enum DealerFormValuesPresetter {
    static func preset(to draft: Auto_Api_DraftResponse?, isEdit: Bool) -> Auto_Api_DraftResponse? {
        return DealerFormInteractorModel.presetValues(to: draft, isEdit: isEdit)
    }
}
