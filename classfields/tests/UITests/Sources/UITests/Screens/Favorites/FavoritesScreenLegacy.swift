import XCTest

final class FavoritesScreen: BaseScreen, Scrollable {
    enum Segment: Int {
        case offers = 0
        case searches = 1
        case comparison = 2
    }

    lazy var navbarView = find(by: "NavBarView").firstMatch
    lazy var scrollableElement = findAll(.collectionView).firstMatch

    lazy var collectionView = findAll(.collectionView).firstMatch
    lazy var refreshingControl = self.scrollableElement.activityIndicators.firstMatch

    lazy var segmentControl = find(by: "segmentControl").firstMatch

    lazy var updatesView = find(by: "user.favs.view.updates").firstMatch

    lazy var tabbar = find(by: "tabbar_item_Избранное").firstMatch
    lazy var tabbarBadge = find(by: "app.tabbar.item_Избранное.badge.flag").firstMatch

    lazy var offerCallButton = find(by: "call_button").firstMatch
    lazy var offerChatButton = find(by: "chat_button").firstMatch
    lazy var offerShowReportButton = find(by: "show_report_button").firstMatch
    lazy var offer24HoursCallsCount = find(by: "offer.24HoursCallsCount").firstMatch

    lazy var recommendationsCell = find(by: "SpecialsCellView").firstMatch
    lazy var titleNavLabel = findStaticText(by: "NavBarTitleView_titleLabel")

    lazy var logInButton = find(by: "Войти").firstMatch

    func segmentControl(at tab: Segment) -> XCUIElement {
        return find(by: "segmentControlSegment_\(tab.rawValue)").firstMatch
    }

    func segmentBadge(at tab: Segment) -> XCUIElement {
        return find(by: "segmentControlSegmentBadge_\(tab.rawValue)").firstMatch
    }

    func savedSearch(id: String, index: Int) -> XCUIElement {
        return find(by: "_\(id)_\(index)").firstMatch
    }
}
