import XCTest
import AutoRuAppearance
import AutoRuUserSaleSharedUI
import AutoRuViews
import AutoRuModernLayout
import AutoRuCellHelpers
import AutoRuModels
import AutoRuFormatters
import AutoRuYoga
import AutoRuYogaLayout
import Snapshots
@testable import AutoRuAppRouting
@testable import AutoRuDiscountPromo
@testable import AutoRuVASTrap
@testable import AutoRuSafeDeal
import AutoRuColorSchema
import Foundation

final class UserSaleListAppearanceTests: BaseUnitTest {
    private let userState = UserSaleInfoViewState(
        id: "id",
        name: "Land Rover Range Rover Evoque I, 2017",
        vehicleCategory: .cars,
        isDraft: false,
        isArchived: false,
        moderating: false,
        priceRUR: "100 000 ₽",
        viewsAll: 100,
        viewsWeekly: 200,
        callsAll: 300,
        callsWeekly: 400,
        favoritesAll: 500,
        position: 10,
        expiresIn: nil,
        isBlocked: false,
        updatedTimeAgo: "1 день назад",
        activationVAS: nil,
        hasVIP: false,
        vasModel: [],
        snippetImage: nil,
        previewVideo: nil,
        hasSpincar: false,
        saleUp: false,
        saleTop: false,
        priceHighlighted: false,
        blockingReasons: [],
        canEdit: false,
        canActivate: false,
        canHide: false,
        canArchive: false,
        isCar: false,
        canDelete: false,
        images: [],
        panoramaInfo: nil,
        hasResellerWarning: false,
        showPremiumAssistantBanner: false,
        showAddPanoramaBanner: false,
        safeDealInfo: nil,
        openAddPanorama: nil,
        isGaragePromoBannerClosed: true,
        acceptableForGarage: false,
        reactivateAt: nil
    )

    private func vas(vasType: SaleVASType) -> SaleVAS {
        return SaleVAS(name: "Имя сервиса",
                       days: 60,
                       alias: "",
                       price: 1000,
                       expires: -1,
                       createDate: Date(),
                       description: "описание сервиса",
                       activated: false,
                       type: vasType,
                       subtypes: [],
                       autopurchase: nil,
                       autoprolongation: .init(price: 500, allowed: true, enabled: false, isOnByDefault: false, expiresIn: 0),
                       discount: .init(oldPrice: 2000,
                                       startDate: Date(timeIntervalSinceNow: -60).timeIntervalSince1970,
                                       endDate: Date(timeIntervalSinceNow: 3600).timeIntervalSince1970,
                                       discount: 50,
                                       active: true),
                       paidReason: nil,
                       recommendationPriority: 1,
                       alternativeAliases: [])
    }

    private func vasModel(vas: SaleVAS) -> SaleVASViewModel {
        return SaleVasToViewModelConverter(saleVAS: vas, group: .cars).createState()
    }

    func test_checkBalanceWarning() {
        let model = CheckBalanceWarningModel()
        let layout = CheckBalanceWarningLayoutSpec.build(model: model)

        let wrappedLayout = StackLayout(childs: [layout], configNode: { $0.padding = .init(uniform: 16) })

        Snapshot.compareWithSnapshot(
            layout: wrappedLayout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_enablePush() {
        let layout = EnablePush.blockLayout(source: .office, onDismissTap: { _ in })

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_discountPromo() {
        let model = DiscountPromoSnippetModel(
            discountAmount: 10,
            endDateTimestamp: Date().addingTimeInterval(60 * 60).timeIntervalSince1970,
            actions: .init(onButtonTapped: { })
        )
        let layout = DiscountPromoSnippetLayout.build(model: model)

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_usedBannedWarning() {
        var model = UserBannedWarningModel()
        model.banMessages = ["Сообщение о забане"]
        let layout = UserBannedWarningLayoutSpec.build(model: model)

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_premiumAssistantBanner() {
        let layoutModel = SaleSnippetPremiumAssistantBannerLayoutModel(lightVersion: false, onChatTapped: { })
        let layoutSpec = SaleSnippetPremiumAssistantBannerLayoutSpec(model: layoutModel)

        Snapshot.compareWithSnapshot(
            layoutSpec: layoutSpec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_resellerWarning() {
        let warning = ResellerWarning(paidReason: .sameSale)!
        let layoutModel = SaleSnippetResellerWarningLayoutModel(resellerWarning: warning, onURLTapped: { _ in })
        let layoutSpec = SaleSnippetResellerWarningLayoutSpec(model: layoutModel)

        Snapshot.compareWithSnapshot(
            layoutSpec: layoutSpec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_saleInfo() {
        var userState = self.userState
        let vas = self.vas(vasType: .toplist)
        userState.vasModel = [vasModel(vas: vas)]

        let layout = UserSaleInfoLayoutSpec.build(model: userState, imageTap: { })

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_autoProlongationWarning() {
        let model = CardAutoprolongationWarningModel(
            expirationTime: ExpirationTime(expiresIn: Date().addingTimeInterval(60 * 60)),
            discount: 10
        )
        let layout = StackLayout(
            childs: [CardAutoprolongationWarningLayoutSpec.build(model: model)],
            configNode: { node in
                node.padding = Edges(left: 16, right: 16, bottom: 16, top: 0)
            }
        )

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_prolongation_autoprolongationAllowed() {
        let model = ProlongationStateModel(
            expirationTime: ExpirationTime(expiresIn: Date().addingTimeInterval(60 * 60 * 10)),
            autoprolongationEnabled: false,
            autoprolongationAllowed: true,
            autoprolongationPrice: 100,
            autoprolongationDays: 3,
            originalPrice: 1000,
            price: 800,
            isVIP: false
        )
        let layout = ProlongationStateModelLayoutSpec.build(model: model)

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_prolongation_autoprolongationDisallowed() {
        let model = ProlongationStateModel(
            expirationTime: ExpirationTime(expiresIn: Date().addingTimeInterval(60 * 60 * 10)),
            autoprolongationEnabled: false,
            autoprolongationAllowed: false,
            autoprolongationPrice: 100,
            autoprolongationDays: 3,
            originalPrice: 1000,
            price: 800,
            isVIP: false
        )
        let layout = ProlongationStateModelLayoutSpec.build(model: model)

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_saleActions_activeExpanded() {
        let model = UserSaleInfoActionViewModel(
            mode: .activeExpanded,
            canDelete: true,
            canHide: true,
            canEdit: true,
            canActivate: true,
            activateTitle: "Тайтл 1",
            activateSubtitle: "",
            activationDiscount: nil,
            actions: .init()
        )
        let layout = UserSaleInfoActionsLayoutSpec.build(model: model)

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_saleActions_activeCollapsed() {
        let model = UserSaleInfoActionViewModel(
            mode: .activeCollapsed,
            canDelete: true,
            canHide: true,
            canEdit: true,
            canActivate: true,
            activateTitle: "Тайтл 1",
            activateSubtitle: "Сабтайтл 1",
            activationDiscount: nil,
            actions: .init()
        )
        let layout = UserSaleInfoActionsLayoutSpec.build(model: model)

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_saleActions_active() {
        let model = UserSaleInfoActionViewModel(
            mode: .active,
            canDelete: true,
            canHide: true,
            canEdit: true,
            canActivate: true,
            activateTitle: "Тайтл 1",
            activateSubtitle: "Сабтайтл 1",
            activationDiscount: nil,
            actions: .init()
        )
        let layout = UserSaleInfoActionsLayoutSpec.build(model: model)

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_saleActions_archived() {
        let model = UserSaleInfoActionViewModel(
            mode: .archived,
            canDelete: true,
            canHide: false,
            canEdit: false,
            canActivate: false,
            activateTitle: "Тайтл 1",
            activateSubtitle: "Сабтайтл 1",
            activationDiscount: nil,
            actions: .init()
        )
        let layout = UserSaleInfoActionsLayoutSpec.build(model: model)

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_saleActions_blocked() {
        let model = UserSaleInfoActionViewModel(
            mode: .blocked,
            canDelete: true,
            canHide: false,
            canEdit: false,
            canActivate: false,
            activateTitle: "Тайтл 1",
            activateSubtitle: "Сабтайтл 1",
            activationDiscount: nil,
            actions: .init()
        )
        let layout = UserSaleInfoActionsLayoutSpec.build(model: model)

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_vasAutopurchase() {
        let model = VASAutopurchaseModel(
            title: "Тайтл",
            time: nil,
            daysPeriod: 3,
            isEnabled: false,
            price: "100 000 P",
            vasType: .fresh
        )
        let layout = VASAutopurchaseLayoutSpec.build(model: model, onSwitchValueChanged: { _ in })

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_pausedVAS() {
        let vas = SaleVAS(name: "Поднятие в топ",
                          days: 60,
                          alias: "all_sale_fresh",
                          price: 1000,
                          expires: 3,
                          createDate: Date(),
                          description: "Активация описание",
                          activated: true,
                          type: .fresh,
                          subtypes: [],
                          autopurchase: nil,
                          autoprolongation: .init(price: 500, allowed: false, enabled: false, isOnByDefault: false, expiresIn: 0),
                          discount: .init(oldPrice: 2000,
                                          startDate: Date(timeIntervalSinceNow: -60).timeIntervalSince1970,
                                          endDate: Date(timeIntervalSinceNow: 3600).timeIntervalSince1970,
                                          discount: 50,
                                          active: true),
                          paidReason: nil,
                          recommendationPriority: 1,
                          alternativeAliases: [])

        let layout = VASPausedSectionLayoutSpec.build(model: [vasModel(vas: vas)])

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_VASSnippet() {
        for vasType in SaleVASType.allCases.filter({ type in
            !type.isPackage && type != .activate
        }) {
            let vas = self.vas(vasType: vasType)

            let layout = VASSingleLayoutSpec.build(model: vasModel(vas: vas),
                                                   position: .middle,
                                                   onAutoprolongationTapped: {},
                                                   onAutoprolongationSwitchValueChanged: { _ in },
                                                   onPriceTapped: {},
                                                   onPriceViewCreate: { _ in })
            Snapshot.compareWithSnapshot(
                layout: layout,
                maxWidth: DeviceWidth.iPhone11,
                backgroundColor: ColorSchema.Background.surface,
                identifier: "\(#function)_\(vasType.rawValue)")
        }
    }

    func test_VASDescription() {
        for vasType in SaleVASType.allCases.filter({ type in
            !type.isPackage && type != .activate
        }) {
            let vas = self.vas(vasType: vasType)

            let model = VASDescriptionContainerViewController.vasNodePresentationModel(from: vasModel(vas: vas), extraButtonHidden: true, presenter: nil)
            let vc = VASDescriptionViewController(model: model)
            let purchaseButtonColor = VASUIFormatter.purchaseButtonColorForVAS(type: vas.type)
            let discountStyle = VASDescriptionDiscountView.Style(
                oldPriceTextColor: ColorSchema.Primary.white,
                badgeColor: VASUIFormatter.discountBadgeColorForVAS(type: vas.type),
                badgeTextColor: VASUIFormatter.discountBadgeTextColorForVAS(type: vas.type)
            )
            vc.updatePurchaseButtonAppearance(purchaseButtonColor, discountStyle: discountStyle, duration: 0)

            let cell = PageContainerCollectionViewCell.init(frame: .init(x: 0, y: 0, width: DeviceWidth.iPhone11, height: 600))
            cell.configure(with: vc)

            Snapshot.compareWithSnapshot(
                view: cell,
                identifier: "\(#function)_\(vasType.rawValue)")
        }
    }

    func test_VASDiscountPromo() {
        for vasType in SaleVASType.allCases.filter({ type in
            !type.isPackage && type != .activate
        }) {
            let vas = self.vas(vasType: vasType)

            let model = DiscoutPromoCreator.collapsedModel(vas: vas,
                                                           discountEndDate: Date().addingTimeInterval(500).timeIntervalSince1970,
                                                           actions: .init())
            let layout = DiscountPromoCollapsedLayoutSpec().build(model: model)
            Snapshot.compareWithSnapshot(
                layout: layout,
                maxWidth: DeviceWidth.iPhone11,
                backgroundColor: ColorSchema.Background.surface,
                identifier: "\(#function)_\(vasType.rawValue)_collapsed")

            let fullModel = DiscoutPromoCreator.fullLayoutModel(vas: vas,
                                                            discountEndDate: Date().addingTimeInterval(500).timeIntervalSince1970,
                                                            offer: nil,
                                                            actions: .init())
            let fullLayout = DiscountPromoFullLayoutSpec(model: fullModel)
            Snapshot.compareWithSnapshot(
                layoutSpec: fullLayout,
                maxWidth: DeviceWidth.iPhone11,
                backgroundColor: ColorSchema.Background.surface,
                identifier: "\(#function)_\(vasType.rawValue)_full")

        }
    }

    func test_VASTrapPromo() {
        for vasType in SaleVASType.allCases.filter({ type in
            !type.isPackage && type != .activate
        }) {
            let vas = self.vas(vasType: vasType)

            let model = VASTrapViewController.collapsedModel(vas: vas, onPriceTap: {})
            let layout = VASTrapCollapsedLayoutSpec(model: model)
            Snapshot.compareWithSnapshot(
                layoutSpec: layout,
                maxWidth: DeviceWidth.iPhone11,
                backgroundColor: ColorSchema.Background.surface,
                identifier: "\(#function)_\(vasType.rawValue)_collapsed")

            let fullModel = VASTrapViewController.fullLayoutModel(category: .cars, vas: vas, width: DeviceWidth.iPhone11, forAnimatedSnippet: true, onPurchaseTap: {})
            let fullLayout = VASTrapFullLayoutSpec(model: fullModel)
            Snapshot.compareWithSnapshot(
                layoutSpec: fullLayout,
                maxWidth: DeviceWidth.iPhone11,
                backgroundColor: ColorSchema.Background.surface,
                identifier: "\(#function)_\(vasType.rawValue)_full")

        }
    }

    // MARK: Safe Deal

    func test_safDealPromoBannerNoTitle() {
        let layout = SafeDealBannerLayout(needTitle: false, onMoreTap: {})

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_safeDealNewRequestsBanner() {
        let model = SafeDealInfoLayoutModel(
            title: "Безопасная сделка",
            text: "У вас несколько запросов на безопасную сделку по автомобилю Mazda 3 IV (BP)"
        )

        let layout = SafeDealInfoLayout(model: model, onMoreTap: {})

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    // MARK: Reactivate later

    func test_reactivateLater() {
        var snippet = userState.snippet
        let dateComponents = DateComponents(year: 2022, month: 4, day: 12)
        let date = Calendar(identifier: .gregorian).date(from: dateComponents)
        snippet.reactivateAt = date

        let layout = UserSaleInfoArchiveLayoutSpec.build(model: snippet, imageTap: { })

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_saleActionsReactivateLater() {
        let model = UserSaleInfoActionViewModel(
            mode: .archived,
            canDelete: true,
            canHide: false,
            canEdit: true,
            canActivate: true,
            activateTitle: "Активировать cейчас",
            activateSubtitle: "",
            activationDiscount: nil,
            actions: .init()
        )
        let layout = UserSaleInfoActionsLayoutSpec.build(model: model)

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }
}
