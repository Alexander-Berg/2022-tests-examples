//
//  MainSteps.swift
//  UITests
//
//  Created by Victor Orlovsky on 20/03/2019.
//
import XCTest
import Snapshots

class MainSteps: BaseSteps {
    @discardableResult
    func openQuickSearch() -> VehicleQuickSearchSteps {
        onMainScreen().quickSearchButton.shouldExist().tap()
        return VehicleQuickSearchSteps(context: context)
    }

    @discardableResult
    func openTab(_ item: LowTab) -> MainSteps {
        _openTab(item)
    }

    @discardableResult
    func openTab(_ item: MainTab) -> MainSteps {
        onMainScreen().mainTab(item).tap()
        return self
    }

    @discardableResult
    func tapLogin() -> LoginSteps {
        onEmptyFavoritesScreen().loginButton.tap()
        return LoginSteps(context: context)
    }

    @discardableResult
    func tapReportsBundleBanner() -> PaymentOptionsSteps<MainSteps> {
        step("Нажимаем на баннер пакета отчетов на морде") {
            let banner = onMainScreen()
                .bannerByPrefix("large_marketing_preset_car_report_bundle_buy")
            banner.shouldExist()
            banner.otherElements["purchase_report_bundle_button"].tap()
        }

        return PaymentOptionsSteps<MainSteps>(context: context, source: self)
    }

    @discardableResult
    func openSearchHistory(_ title: String) -> SaleCardListSteps {
        onMainScreen().find(by: title).firstMatch.tap()
        return SaleCardListSteps(context: context)
    }

    func scrollToBottom(of tab: MainTab) -> Self {
        let screen = onMainScreen()

        screen.scrollTo(element: screen.lastElementScrollView(tab))
        return self
    }

    @discardableResult
    func openDealerCabinetTab(isAttentions: Bool = false) -> DealerCabinetSteps {
        onMainScreen().tabBarItem(kind: isAttentions ? .offers_attentions : .offers).tap()
        return DealerCabinetSteps(context: context)
    }

    func openReview() -> ReviewSteps {
        onMainScreen().find(by: "ОТЗЫВЫ").firstMatch.tap()
        return ReviewSteps(context: context)
    }

    @discardableResult
    func openFilters() -> FiltersSteps {
        onMainScreen().parametersButton.shouldExist(timeout: 10, message: "").tap()
        return FiltersSteps(context: context)
    }

    @discardableResult
    func openChats() -> ChatsSteps {
        onMainScreen().tabBarItem(kind: .messages).shouldExist(timeout: 10).tap()
        return ChatsSteps(context: context)
    }

    @discardableResult
    override func exist(selector: String) -> Self {
        let element = onUserProfileScreen().find(by: selector).firstMatch
        element.shouldExist(timeout: 10)
        return self
    }

    func scrollToCreditBanner() -> Self {
        onMainScreen().scrollTo(element: onMainScreen().creditBanner, maxSwipes: 6, windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 120, right: 0))
        return self
    }

    func scrollToNewAutoBanner() -> Self {
        onMainScreen().scrollTo(element: onMainScreen().banner(.newAuto), maxSwipes: 2, windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 64, right: 0))
        return self
    }

    func scrollTo(_ selector: String) -> Self {
        onMainScreen().scrollTo(element: onMainScreen().find(by: selector).firstMatch, windowInsets: UIEdgeInsets(top: 0, left: 0, bottom: 100, right: 0))
        return self
    }

    @discardableResult
    func validateSnapShot(accessibilityId: String, snapshotId: String, message: String = "") -> Self {
        return XCTContext.runActivity(named: message.isEmpty ? "Validate snapshot \(snapshotId)" : message) { _ in
            let screenshot = app.descendants(matching: .any).matching(identifier: accessibilityId).firstMatch.screenshot().image
            Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 16))
            return self
        }
    }

    func tapOnCreditBanner() -> PreliminaryCreditSteps {
        onMainScreen().creditBanner.tap()
        return PreliminaryCreditSteps(context: context)
    }

    func tapOnNewAutoBanner() -> SaleCardListSteps {
        onMainScreen().banner(.newAuto)
            .staticTexts.firstMatch.tap()
        return SaleCardListSteps(context: context)
    }

    @discardableResult
    func swipeUp() -> Self {
        onMainScreen().swipe(.up)
        return self
    }

    func openCreditFormFromAlert() -> SharkFullFormSteps {
        onMainScreen().find(by: "Продолжить заполнение").firstMatch.tap()
        return SharkFullFormSteps(context: context)
    }

    func openCarReportsList() -> CarReportsListSteps {
        let element = onMainScreen().carReportsListButton()
        element.tap()
        return CarReportsListSteps(context: context)
    }

    func openCredits() {
        onMainScreen().creditsTab().tap()
    }

    func openInsurance() -> MainSteps {
        onMainScreen().tabsScrollView.scrollTo(
            element: onMainScreen().insuranceTab(),
            swipeDirection: .left
        )
        onMainScreen().insuranceTab().tap()
        return self
    }

    func openReviews() -> MainSteps {
        onMainScreen().tabsScrollView.scrollTo(
            element: onMainScreen().reviewsTab(),
            swipeDirection: .left
        )
        onMainScreen().reviewsTab().tap()
        return self
    }

    func openJournal() -> MainSteps {
        onMainScreen().tabsScrollView.scrollTo(
            element: onMainScreen().journalTab(),
            swipeDirection: .left
        )
        onMainScreen().journalTab().tap()
        return self
    }

    @discardableResult
    func checkHasNoTab(_ tab: MainTab) -> MainSteps {
        onMainScreen().find(by: tab.rawValue)
            .firstMatch.shouldNotExist()
        return self
    }

    func checkHasWebView(timeout: TimeInterval) {
        onMainScreen().find(by: "WebController").firstMatch.shouldExist(timeout: timeout)
    }
}

// MARK: open tabs
extension MainSteps {
    fileprivate func _openTab<Steps: BaseSteps>(_ item: LowTab) -> Steps {
        onMainScreen().tabBarItem(kind: item).shouldExist(timeout: 10)
        onMainScreen().tabBarItem(kind: item).tap()
        return self.as(Steps.self)
    }

    @discardableResult
    func openFavoritesTab() -> FavoritesSteps {
        _openTab(.favorites)
    }

    @discardableResult
    func openGarageTab() -> GarageSteps {
        _openTab(.garage)
    }

    @discardableResult
    func openOffersTab() -> OffersSteps {
        _openTab(.offers)
    }
}
