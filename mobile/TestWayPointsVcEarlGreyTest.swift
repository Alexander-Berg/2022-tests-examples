//
//  TestWayPointsVcExpandedEarlGreyTest.swift
//  Directions-YandexMapsTests
//
//  Created by Dmitry Trimonov on 14/12/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation
import RxSwift
import RxCocoa
import EarlGrey
import XCTest
import YandexMapsCommonTypes
import YandexMapsDirections
import YandexMapsSuggest

//let wayPointCellHeight: CGFloat = 56.0
//
//enum AssertionLayoutType {
//    case collapsedLayout
//    case expandedLayout
//    case transitionLayout
//}
//
//var EarlGrey: EarlGreyImpl {
//    return EarlGreyImpl.invoked(fromFile: #file, lineNumber: #line)!
//}
//
//func matcherForWayPointCellView(withTextPosition textPosition: WayPointCellViewModel.TextPosition) -> GREYElementMatcherBlock {
//    let matchesBlock: MatchesBlock = { (element: Any?) -> Bool in
//        if let vm = (element as? WayPointCellView)?.viewModel {
//            return vm.textPosition.value == textPosition
//        } else {
//            return false
//        }
//    }
//    let describe: DescribeToBlock = { (description: Any) -> Void in
//        let greyDescription: GREYDescription = description as! GREYDescription
//        greyDescription.appendText("MatchesTextPosition textPosition: \(textPosition)")
//    }
//    let matcher: GREYElementMatcherBlock! =
//        GREYElementMatcherBlock(matchesBlock: matchesBlock, descriptionBlock: describe)
//    return matcher
//}
//
//func matcherForWayPointCell(predicate: @escaping (WayPointCellView) -> Bool) -> GREYElementMatcherBlock {
//    let matchesBlock: MatchesBlock = { (element: Any?) -> Bool in
//        if let cellView = element as? WayPointCellView {
//            return predicate(cellView)
//        } else {
//            return false
//        }
//    }
//    let describe: DescribeToBlock = { (description: Any) -> Void in
//        let greyDescription: GREYDescription = description as! GREYDescription
//        greyDescription.appendText("MatchesWayPointCellView")
//    }
//    let matcher: GREYElementMatcherBlock! =
//        GREYElementMatcherBlock(matchesBlock: matchesBlock, descriptionBlock: describe)
//    return matcher
//}
//
//func matcherForTableView(withContentOffsetY contentOffsetY: CGFloat) -> GREYElementMatcherBlock {
//    let matchesTableLayoutContentOffset: MatchesBlock = { (element: Any?) -> Bool in
//        if let tv = element as? UITableView {
//            let contentOffsetY = tv.contentOffset.y
//            let isEqual = contentOffsetY.equal(to: contentOffsetY, withEps: 1 / UIScreen.main.scale)
//            if !isEqual {
//                print("getContentOffsetMatcher false: contentOffsetY is \(contentOffsetY), expectedY is \(contentOffsetY)")
//            }
//            return isEqual
//        } else {
//            return false
//        }
//    }
//    let describe: DescribeToBlock = { (description: Any) -> Void in
//        let greyDescription: GREYDescription = description as! GREYDescription
//        greyDescription.appendText("MatchesTableLayoutContentOffset y: \(contentOffsetY)")
//    }
//    let matcher: GREYElementMatcherBlock! =
//        GREYElementMatcherBlock(matchesBlock: matchesTableLayoutContentOffset, descriptionBlock: describe)
//    return matcher
//}
//
//func assertionForLayoutType(withLayoutType: AssertionLayoutType) -> GREYAssertionBlock {
//    return GREYAssertionBlock.assertion(
//        withName: "Assert WayPointsView state is equal to \(withLayoutType)",
//        assertionBlockWithError: {
//            (element: Any?, errorOrNil: UnsafeMutablePointer<NSError?>?) -> Bool in
//            guard let collectionView = element! as! UICollectionView as UICollectionView! else {
//                let errorInfo = [NSLocalizedDescriptionKey:
//                    "Element is not UICollectionView"]
//                errorOrNil?.pointee =
//                    NSError(domain: kGREYInteractionErrorDomain,
//                            code: 2,
//                            userInfo: errorInfo)
//                return false
//            }
//            let collectionViewLayoutClassName = NSStringFromClass(type(of: collectionView.collectionViewLayout))
//            let result: Bool
//            switch collectionView.collectionViewLayout {
//            case _ as WayPointsViewCollapsedLayout:
//                result = withLayoutType == .collapsedLayout
//            case _ as WayPointsViewExpandedLayout:
//                result = withLayoutType == .expandedLayout
//            case _ as UICollectionViewTransitionLayout:
//                result = withLayoutType == .transitionLayout
//            default:
//                result = false
//            }
//            if result == false {
//                let errorInfo = [NSLocalizedDescriptionKey:
//                    "State is not \(withLayoutType), collectionViewLayout is: \(collectionViewLayoutClassName)"]
//                errorOrNil?.pointee =
//                    NSError(domain: kGREYInteractionErrorDomain,
//                            code: 2,
//                            userInfo: errorInfo)
//                return false
//            } else {
//                return true
//            }
//    })
//}
//
//struct Matchers {
//
//    static var wayPointsView: GREYMatcher {
//        return grey_kindOfClass(UICollectionView.self)
//    }
//
//    static var constantSuggestsView: GREYMatcher {
//        return grey_accessibilityID(AccessibilityIdentifiers.constantSuggestsTable)
//    }
//
//    static var suggestsView: GREYMatcher {
//        return grey_accessibilityID(AccessibilityIdentifiers.suggestsTable)
//    }
//
//    static var fromText: GREYMatcher {
//        return grey_text(Strings.fromPointPlaceholder)
//    }
//
//    static var toText: GREYMatcher {
//        return grey_text(Strings.toPointPlaceholder)
//    }
//
//    static var addPointText: GREYMatcher {
//        return grey_text(Strings.addPointPlaceholder)
//    }
//
//    static var activeInCollapsedCellText: GREYMatcher {
//        return fromText
//    }
//
//    static var inactiveInCollapsedCellText: GREYMatcher {
//        return toText
//    }
//
//    static var activeInExpandedCellText: GREYMatcher {
//        return activeInCollapsedCellText
//    }
//
//    static var inactiveInExpandedCellText: GREYMatcher {
//        return inactiveInCollapsedCellText
//    }
//
//    static var fromCellMatcher: GREYElementMatcherBlock {
//        return matcherForWayPointCell(predicate: { $0.viewModel?.field.state.value.iconPlaceholder == .from })
//    }
//
//    static var toCellMatcher: GREYElementMatcherBlock {
//        return matcherForWayPointCell(predicate: { $0.viewModel?.field.state.value.iconPlaceholder == .to })
//    }
//
//    static var visibleCellMatcher: GREYElementMatcherBlock {
//        return fromCellMatcher
//    }
//
//    static var invisibleCellMatcher: GREYElementMatcherBlock {
//        return toCellMatcher
//    }
//
//    static var cellClearButton: GREYMatcher {
//        return grey_accessibilityID(AccessibilityIdentifiers.cellClearButton)
//    }
//
//    static var cellCancelButton: GREYMatcher {
//        return grey_accessibilityID(AccessibilityIdentifiers.cellCancelButton)
//    }
//}
//
//extension WayPointsSelectionViewController {
//    convenience init() {
//        let wayPointsRepository = TestWayPointsRepository(from: nil, to: nil)
//        let suggestsService = TestSuggestService.init(suggestFunc: { suggestText -> Single<[Suggest]> in
//            let suggest = TestSuggest(
//                title: "Иван Саджест",
//                subtitle: "Subtitle",
//                searchText: "Поисковый текст саджеста",
//                displayText: nil,
//                action: .search
//            )
//            return Single.just([suggest])
//        })
//
//        let searchService = TestSearchService.init(searchFunc: { searchText -> Single<[SearchResult]> in
//            let searchResult = TestSearchResult.init(
//                name: "Коля",
//                description: "Fake description",
//                info: SearchResultAdditionalInfo.toponym(info: TestToponymInfo()),
//                location: CoordinatePoint(lat: 0.0, lon: 0.0)
//            )
//            return Single.just([searchResult])
//        })
//        let wayPointsAssemblyFactory = WayPointsAssemblyFactoryImpl(
//            wayPointsRepository: wayPointsRepository
//        )
//        let routesOptionsRepository = TestRoutesOptionsRepository()
//        let specialBookmarksService = TestImportantPlacesService()
//        let directionsHistoryService = TestDirectionsHistoryService()
//        let bookmarksService = TestBookmarksService()
//        let bookmarkResolutionService = TestBookmarkResolutionService()
//        let locationService  = TestLocationService()
//        let i18nManager = TestI18nManager()
//        let suggestImageService = TestSuggestImageService()
//        let suggestsAssemblyFactory = SuggestsAssemblyFactoryImpl(
//            suggestService: suggestsService,
//            suggestImageService: suggestImageService,
//            i18nManager: TestI18nManager
//        )
//        let searchResultsAssemblyFactory = SearchResultsAssemblyFactoryImpl(
//            searchService: searchService,
//            routesOptionsRepository: routesOptionsRepository,
//            locationService: locationService,
//            i18nManager: i18nManager
//        )
//        let constantSuggestAssemblyFactory = ConstantSuggestAssemblyFactoryImpl(
//            specialBookmarksService: specialBookmarksService,
//            directionsHistoryService: directionsHistoryService,
//            bookmarksService: bookmarksService,
//            bookmarkResolutionService: bookmarkResolutionService,
//            locationService: locationService,
//            i18nManager: i18nManager,
//            routesService: TestRoutesService(),
//            routesOptionsRepository: routesOptionsRepository,
//            trafficJamsTypeService: TestTrafficJamsTypeService()
//        )
//
//        let wayPointsSelectionAssembly = WayPointsSelectionAssemblyImpl(
//            wayPointsAssemblyFactory: wayPointsAssemblyFactory,
//            suggestsAssemblyFactory: suggestsAssemblyFactory,
//            constantSuggestAssemblyFactory: constantSuggestAssemblyFactory,
//            searchResultsAssemblyFactory: searchResultsAssemblyFactory
//        )
//        self.init(wayPointsSelectionAssembly: wayPointsSelectionAssembly)
//    }
//}
//
//class TestWayPointsViewControllerExpandedEGTest: XCTestCase {
//    class func rootVc() -> UIViewController {
//        return WayPointsSelectionViewController()
//    }
//
//    override func setUp() {
//        super.setUp()
//        UIApplication.shared.keyWindow?.rootViewController = type(of: self).rootVc()
//    }
//
//    // TextPosition
//
//    func testInitialStateTextPosition() {
//        let fromCellMatcher: GREYElementMatcherBlock = Matchers.fromCellMatcher
//        let toCellMatcher: GREYElementMatcherBlock = Matchers.toCellMatcher
//
//        EarlGrey.selectElement(with: fromCellMatcher)
//            .inRoot(Matchers.wayPointsView)
//            .assert(with: matcherForWayPointCellView(withTextPosition: .maxMargin))
//        EarlGrey.selectElement(with: toCellMatcher)
//            .inRoot(Matchers.wayPointsView)
//            .assert(with: matcherForWayPointCellView(withTextPosition: .maxMargin))
//    }
//
//    // Paging
//
//    func testScrollUpForLessThanAHalfOfWayPointCellHeightDoesntSetCollapsedState() {
//        let amount: CGFloat = wayPointCellHeight / 2.0 - 5
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .perform(grey_scrollInDirection(.down, amount))
//        let expandedStateContentOffsetMatcher = matcherForTableView(withContentOffsetY: -(2 * wayPointCellHeight))
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .assert(with: expandedStateContentOffsetMatcher)
//    }
//
//    func testScrollUpForMoreThanAHalfOfWayPointCellHeightSetsCollapsedState() {
//
//        let amount: CGFloat = wayPointCellHeight / 2.0 + 5
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .perform(grey_scrollInDirection(.down, amount))
//        let collapsedStateContentOffsetMatcher = matcherForTableView(withContentOffsetY: -wayPointCellHeight)
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .assert(with: collapsedStateContentOffsetMatcher)
//    }
//
//    // AddPoint
//
//    func testAddWayPointCellIsInvisibleInInitialState() {
//        EarlGrey.selectElement(with: Matchers.addPointText)
//            .assert(with: grey_notVisible())
//    }
//
//    func testAddWayPointCellIsInvisibleAfterFromFieldIsFilled() {
//        EarlGrey.selectElement(with: Matchers.fromText)
//            .inRoot(Matchers.wayPointsView)
//            .perform(grey_typeText("Typing"))
//        EarlGrey.selectElement(with: grey_allOf([grey_kindOfClass(UITableViewCell.self), grey_interactable()]))
//            .atIndex(0)
//            .inRoot(Matchers.suggestsView)
//            .perform(grey_tap())
//
//        EarlGrey.selectElement(with: Matchers.addPointText)
//            .assert(with: grey_notVisible())
//    }
//
//    func testAddWayPointCellIsVisibleAfterFromAndToFieldsAreFilled() {
//        EarlGrey.selectElement(with: Matchers.fromText)
//            .inRoot(Matchers.wayPointsView)
//            .perform(grey_typeText("Some from text"))
//        EarlGrey.selectElement(with: grey_allOf([grey_kindOfClass(UITableViewCell.self), grey_interactable()]))
//            .atIndex(0)
//            .inRoot(Matchers.suggestsView)
//            .perform(grey_tap())
//
//        EarlGrey.selectElement(with: Matchers.toText)
//            .inRoot(Matchers.wayPointsView)
//            .perform(grey_typeText("Some to text"))
//        EarlGrey.selectElement(with: grey_allOf([grey_kindOfClass(UITableViewCell.self), grey_interactable()]))
//            .atIndex(0)
//            .inRoot(Matchers.suggestsView)
//            .perform(grey_tap())
//
//        EarlGrey.selectElement(with: Matchers.addPointText)
//            .assert(with: grey_sufficientlyVisible())
//    }
//
//    func testAddWayPointCellIsVisibleAfterAfterTapOnAddPointCellAndSelectSuggest() {
//        EarlGrey.selectElement(with: Matchers.fromText)
//            .inRoot(Matchers.wayPointsView)
//            .perform(grey_typeText("Some from text"))
//        EarlGrey.selectElement(with: grey_allOf([grey_kindOfClass(UITableViewCell.self), grey_interactable()]))
//            .atIndex(0)
//            .inRoot(Matchers.suggestsView)
//            .perform(grey_tap())
//
//        EarlGrey.selectElement(with: Matchers.toText)
//            .inRoot(Matchers.wayPointsView)
//            .perform(grey_typeText("Some to text"))
//        EarlGrey.selectElement(with: grey_allOf([grey_kindOfClass(UITableViewCell.self), grey_interactable()]))
//            .atIndex(0)
//            .inRoot(Matchers.suggestsView)
//            .perform(grey_tap())
//
//        EarlGrey.selectElement(with: Matchers.addPointText)
//            .inRoot(Matchers.wayPointsView)
//            .perform(grey_typeText("Some addPoint text"))
//        EarlGrey.selectElement(with: grey_allOf([grey_kindOfClass(UITableViewCell.self), grey_interactable()]))
//            .atIndex(0)
//            .inRoot(Matchers.suggestsView)
//            .perform(grey_tap())
//
//        EarlGrey.selectElement(with: Matchers.addPointText)
//            .assert(with: grey_sufficientlyVisible())
//    }
//
//    // Text Visibilities
//
//    func testCellVisibilitiesAfterScrollToContentEdgeTop() {
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .perform(grey_scrollToContentEdge(.top))
//        EarlGrey.selectElement(with: Matchers.fromText)
//            .assert(with: grey_sufficientlyVisible())
//        EarlGrey.selectElement(with: Matchers.toText)
//            .assert(with: grey_sufficientlyVisible())
//    }
//
//    func testCellVisibilitiesAfterSwipeSlowDown() {
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .perform(grey_swipeSlowInDirection(.down))
//        EarlGrey.selectElement(with: Matchers.fromText)
//            .assert(with: grey_sufficientlyVisible())
//        EarlGrey.selectElement(with: Matchers.toText)
//            .assert(with: grey_sufficientlyVisible())
//    }
//
//    func testCellVisibilitiesAfterSwipeFastDown() {
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .perform(grey_swipeFastInDirection(.down))
//        EarlGrey.selectElement(with: Matchers.toText)
//            .assert(with: grey_sufficientlyVisible())
//        EarlGrey.selectElement(with: Matchers.fromText)
//            .assert(with: grey_sufficientlyVisible())
//    }
//
//    func testCellVisibilitiesAfterScrollToContentEdgeBottom() {
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .perform(grey_scrollToContentEdge(.bottom))
//        EarlGrey.selectElement(with: Matchers.activeInCollapsedCellText)
//            .assert(with: grey_sufficientlyVisible())
//        EarlGrey.selectElement(with: Matchers.inactiveInCollapsedCellText)
//            .assert(with: grey_notVisible())
//    }
//
//    func testCellVisibilitiesAfterSwipeSlowUp() {
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .perform(grey_swipeSlowInDirection(.up))
//        EarlGrey.selectElement(with: Matchers.activeInCollapsedCellText)
//            .assert(with: grey_sufficientlyVisible())
//        EarlGrey.selectElement(with: Matchers.inactiveInCollapsedCellText)
//            .assert(with: grey_notVisible())
//    }
//
//    func testCellVisibilitiesAfterSwipeFastUp() {
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .perform(grey_swipeFastInDirection(.up))
//        EarlGrey.selectElement(with: Matchers.activeInCollapsedCellText)
//            .assert(with: grey_sufficientlyVisible())
//        EarlGrey.selectElement(with: Matchers.inactiveInCollapsedCellText)
//            .assert(with: grey_notVisible())
//    }
//
//    func testFromToLabelsAreVisibleAfterSwapStateButtonTapInSingleItemLayoutState() {
//        //TODO: - Implement same logic like integrationTest
//    }
//
//    func testFromLabelIsInvisibleToLabelIsVisibleAfterSwapStateButtonTapInTableLayoutState() {
//        //TODO: - Implement same logic like integrationTest
//    }
//
//    // Initial state
//
//    func testInitialState() {
//        let layoutTypeAssertion = assertionForLayoutType(withLayoutType: .expandedLayout)
//        let contentOffsetMather = matcherForTableView(withContentOffsetY: -(2 * wayPointCellHeight))
//        EarlGrey.selectElement(with: Matchers.wayPointsView).assert(layoutTypeAssertion)
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView).assert(with: contentOffsetMather)
//    }
//
//    func testTableViewContentOffsetInTableLayoutState() {
//        let contentOffsetMather = matcherForTableView(withContentOffsetY: -(2 * wayPointCellHeight))
//        let layoutTypeAssertion = assertionForLayoutType(withLayoutType: .expandedLayout)
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .perform(grey_scrollToContentEdge(.top))
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .assert(with: contentOffsetMather)
//        EarlGrey.selectElement(with: Matchers.wayPointsView)
//            .assert(layoutTypeAssertion)
//    }
//
//    func testScrollToBottomSetsCollapsedState() {
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .perform(grey_scrollToContentEdge(.bottom))
//
//        // Hack: Fails on iOS 9 if doesn't invoke one more bit scroll down
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .perform(grey_scrollInDirection(.up, 1.0))
//
//        let collapsedStateAssertion = assertionForLayoutType(withLayoutType: .collapsedLayout)
//        EarlGrey.selectElement(with: Matchers.wayPointsView)
//            .assert(collapsedStateAssertion)
//    }
//
//    // First Responder
//
//    func testTapOnInactiveCellSetsCollapsedState() {
//
//        EarlGrey.selectElement(with: Matchers.activeInExpandedCellText)
//            .inRoot(Matchers.wayPointsView)
//            .perform(grey_tap())
//
//        let contentOffsetMather = matcherForTableView(withContentOffsetY: -wayPointCellHeight)
//        EarlGrey.selectElement(with: Matchers.constantSuggestsView)
//            .assert(with: contentOffsetMather)
//    }
//
//    func testTapOnActiveCellDoesntSetCollapsedStateBecaseItIsAlreadyFirstResponder() {
//
//        EarlGrey.selectElement(with: Matchers.activeInExpandedCellText)
//            .inRoot(Matchers.wayPointsView)
//            .perform(grey_tap())
//
//        let layoutTypeAssertion = assertionForLayoutType(withLayoutType: .expandedLayout)
//        EarlGrey.selectElement(with: Matchers.wayPointsView)
//            .assert(layoutTypeAssertion)
//    }
//
//    func testStartOfTypingInActiveCellSetsCollapsedState() {
//
//        //Важно! Он не умеет печатать на русском если не переключить))
//        EarlGrey.selectElement(with: Matchers.activeInExpandedCellText)
//            .inRoot(Matchers.wayPointsView)
//            .perform(grey_typeText("Some kuda text"))
//        let layoutTypeAssertion = assertionForLayoutType(withLayoutType: .collapsedLayout)
//        EarlGrey.selectElement(with: Matchers.wayPointsView)
//            .assert(layoutTypeAssertion)
//    }
//
//    func testStartOfTypingInInactiveCellSetsCollapsedState() {
//
//        //Важно! Он не умеет печатать на русском если не переключить))
//        EarlGrey.selectElement(with: Matchers.inactiveInExpandedCellText)
//            .inRoot(Matchers.wayPointsView)
//            .perform(grey_typeText("Some from text"))
//        let layoutTypeAssertion = assertionForLayoutType(withLayoutType: .collapsedLayout)
//        EarlGrey.selectElement(with: Matchers.wayPointsView)
//            .assert(layoutTypeAssertion)
//    }
//}
//
//class TestWayPointsViewControllerExpandedInNavBarEGTest: TestWayPointsViewControllerExpandedEGTest {
//    override class func rootVc() -> UIViewController {
//        return UINavigationController(rootViewController: super.rootVc())
//    }
//}
