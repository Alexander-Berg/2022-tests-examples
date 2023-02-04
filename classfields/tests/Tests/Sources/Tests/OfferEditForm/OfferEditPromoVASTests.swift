//
//  OfferEditPromoVASTests.swift
//  Tests
//
//  Created by Roman Bevza on 4/23/21.
//
import XCTest
import AutoRuProtoModels
import AutoRuModels
import AutoRuAppearance
import AutoRuAppConfig
import AutoRuYogaLayout
import AutoRuUserSaleSharedUI
import Snapshots
@testable import AutoRuNewForm
@testable import AutoRuFormatters
import AutoRuColorSchema

class OfferEditPromoVASTests: BaseUnitTest {
    func test_vipVASWithPaidActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let vipIndex = services.firstIndex { (service) -> Bool in
            return service.service == "package_vip"
            }!
        var vipVAS = services[vipIndex]
        vipVAS.recommendationPriority = 1
        vipVAS.price = 10000
        vipVAS.originalPrice = 20000
        vipVAS.days = 60

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
        }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 1000
        activationVASService.paidReason = .paymentGroup
        activationVASService.originalPrice = 2000
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let model = OfferEditVASPromoLayoutModel(promoVAS: SaleVAS(service: vipVAS)!,
                                                 activationVAS: activationVAS,
                                                 icons: VASUIFormatter.packageIcons(type: .vipPackage),
                                                 actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditVIPPromoLayoutSpec.build(model: model),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_vip"
        )

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_vipVASWithFreeActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let vipIndex = services.firstIndex { (service) -> Bool in
            return service.service == "package_vip"
            }!
        var vipVAS = services[vipIndex]
        vipVAS.recommendationPriority = 1
        vipVAS.price = 10000
        vipVAS.originalPrice = 20000
        vipVAS.days = 60

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 0
        activationVASService.originalPrice = 2000
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let model = OfferEditVASPromoLayoutModel(promoVAS: SaleVAS(service: vipVAS)!,
                                                 activationVAS: activationVAS,
                                                 icons: VASUIFormatter.packageIcons(type: .vipPackage),
                                                 actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditVIPPromoLayoutSpec.build(model: model),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_vip"
        )

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_turboVASWithRegressiveActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let turboIndex = services.firstIndex { (service) -> Bool in
            return service.service == "package_turbo"
            }!
        var turboVAS = services[turboIndex]
        turboVAS.recommendationPriority = 2
        turboVAS.price = 10000
        turboVAS.originalPrice = 20000
        turboVAS.days = 5

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 1000
        activationVASService.originalPrice = 2000
        activationVASService.autoProlongPrice = 500
        activationVASService.prolongationForcedNotTogglable = true
        activationVASService.prolongationAllowed = true
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let model = OfferEditVASPromoLayoutModel(promoVAS: SaleVAS(service: turboVAS)!,
                                                 activationVAS: activationVAS,
                                                 icons: VASUIFormatter.packageIcons(type: .turboPackage),
                                                 actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditVIPPromoLayoutSpec.build(model: model),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_turbo"
        )

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_turboVASWithForcedProlongationActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let turboIndex = services.firstIndex { (service) -> Bool in
            return service.service == "package_turbo"
            }!
        var turboVAS = services[turboIndex]
        turboVAS.recommendationPriority = 2
        turboVAS.price = 10000
        turboVAS.originalPrice = 20000
        turboVAS.days = 5

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 1000
        activationVASService.originalPrice = 2000
        activationVASService.autoProlongPrice = 1000
        activationVASService.prolongationForcedNotTogglable = true
        activationVASService.prolongationAllowed = true
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let model = OfferEditVASPromoLayoutModel(promoVAS: SaleVAS(service: turboVAS)!,
                                                 activationVAS: activationVAS,
                                                 icons: VASUIFormatter.packageIcons(type: .turboPackage),
                                                 actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditVIPPromoLayoutSpec.build(model: model),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_turbo"
        )

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_turboVASWithPaidActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let turboIndex = services.firstIndex { (service) -> Bool in
            return service.service == "package_turbo"
            }!
        var turboVAS = services[turboIndex]
        turboVAS.recommendationPriority = 2
        turboVAS.price = 10000
        turboVAS.originalPrice = 20000
        turboVAS.days = 5

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 1000
        activationVASService.originalPrice = 2000
        activationVASService.autoProlongPrice = 1000
        activationVASService.prolongationAllowed = false
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let model = OfferEditVASPromoLayoutModel(promoVAS: SaleVAS(service: turboVAS)!,
                                                 activationVAS: activationVAS,
                                                 icons: VASUIFormatter.packageIcons(type: .turboPackage),
                                                 actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditVIPPromoLayoutSpec.build(model: model),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_turbo"
        )

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_turboVASWithFreeWithDiscountActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let turboIndex = services.firstIndex { (service) -> Bool in
            return service.service == "package_turbo"
            }!
        var turboVAS = services[turboIndex]
        turboVAS.recommendationPriority = 2
        turboVAS.price = 10000
        turboVAS.originalPrice = 20000
        turboVAS.days = 5

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 0
        activationVASService.originalPrice = 2000
        activationVASService.autoProlongPrice = 0
        activationVASService.prolongationAllowed = false
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let model = OfferEditVASPromoLayoutModel(promoVAS: SaleVAS(service: turboVAS)!,
                                                 activationVAS: activationVAS,
                                                 icons: VASUIFormatter.packageIcons(type: .turboPackage),
                                                 actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditVIPPromoLayoutSpec.build(model: model),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_turbo"
        )

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_turboVASWithZeroOriginalPriceActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let turboIndex = services.firstIndex { (service) -> Bool in
            return service.service == "package_turbo"
            }!
        var turboVAS = services[turboIndex]
        turboVAS.recommendationPriority = 2
        turboVAS.price = 0
        turboVAS.originalPrice = 0
        turboVAS.autoApplyPrice = 500
        turboVAS.days = 5

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 0
        activationVASService.originalPrice = 0
        activationVASService.autoProlongPrice = 0
        activationVASService.prolongationAllowed = false
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let model = OfferEditVASPromoLayoutModel(promoVAS: SaleVAS(service: turboVAS)!,
                                                 activationVAS: activationVAS,
                                                 icons: VASUIFormatter.packageIcons(type: .turboPackage),
                                                 actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditVIPPromoLayoutSpec.build(model: model),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_turbo"
        )

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_turboVASWithFreeActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let turboIndex = services.firstIndex { (service) -> Bool in
            return service.service == "package_turbo"
            }!
        var turboVAS = services[turboIndex]
        turboVAS.recommendationPriority = 2
        turboVAS.price = 10000
        turboVAS.originalPrice = 20000
        turboVAS.days = 5

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 0
        activationVASService.originalPrice = 0
        activationVASService.autoProlongPrice = 0
        activationVASService.prolongationAllowed = false
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let model = OfferEditVASPromoLayoutModel(promoVAS: SaleVAS(service: turboVAS)!,
                                                 activationVAS: activationVAS,
                                                 icons: VASUIFormatter.packageIcons(type: .turboPackage),
                                                 actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditVIPPromoLayoutSpec.build(model: model),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_turbo"
        )

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    // MARK: Single Activation

    func test_singleRegressiveActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 1000
        activationVASService.originalPrice = 2000
        activationVASService.autoProlongPrice = 500
        activationVASService.prolongationForcedNotTogglable = true
        activationVASService.prolongationAllowed = true
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_singlePromocodeActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 0
        activationVASService.originalPrice = 2000
        activationVASService.autoProlongPrice = 2000
        activationVASService.prolongationForcedNotTogglable = true
        activationVASService.prolongationAllowed = true
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_singleForcedProlongationActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 1000
        activationVASService.originalPrice = 2000
        activationVASService.autoProlongPrice = 1000
        activationVASService.prolongationForcedNotTogglable = true
        activationVASService.prolongationAllowed = true
        activationVASService.days = 7
        activationVASService.paidReason = .premiumOffer
        let activationVAS = SaleVAS(service: activationVASService)!

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_singlePaidActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 1000
        activationVASService.originalPrice = 2000
        activationVASService.autoProlongPrice = 1000
        activationVASService.prolongationAllowed = false
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_singleFreeWithDiscountActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 0
        activationVASService.originalPrice = 2000
        activationVASService.autoProlongPrice = 0
        activationVASService.prolongationAllowed = false
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }

    func test_singleFreeActivation() {
        let draftResponse: Auto_Api_DraftResponse = .init(mockFile: "offer_edit_get_draft")
        let services = draftResponse.servicePrices

        let activationVASIndex = services.firstIndex { (service) -> Bool in
            return service.service == "all_sale_activate"
            }!
        var activationVASService = services[activationVASIndex]
        activationVASService.recommendationPriority = 2
        activationVASService.price = 0
        activationVASService.originalPrice = 0
        activationVASService.autoProlongPrice = 0
        activationVASService.prolongationAllowed = false
        activationVASService.days = 7
        let activationVAS = SaleVAS(service: activationVASService)!

        let layout = OfferEditActivationLayoutSpec.build(model: activationVAS,
                                            noPromoMode: true,
                                            onPurchaseTap: {})
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_activate"
        )

        let descriptionModel = OfferEditActivationDescriptionModel(prolongationForcedNotTogglable: activationVAS.autoprolongation.allowed,
                                                                   hasProgressiveDiscount: activationVAS.hasProgressiveDiscount,
                                                                   activationDays: activationVAS.days,
                                                                   resellerWarning: activationVAS.resellerWarning,
                                                                   actions: .init())
        Snapshot.compareWithSnapshot(
            layout: OfferEditActivationDescriptionLayoutSpec.build(model: descriptionModel),
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: #function + "_description"
        )
    }
}
