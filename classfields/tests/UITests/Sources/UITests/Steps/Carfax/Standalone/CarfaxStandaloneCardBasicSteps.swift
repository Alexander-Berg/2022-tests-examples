import XCTest
import Snapshots

final class CarfaxStandaloneCardBasicSteps: BaseSteps {
    func onScreen() -> CarfaxStandaloneCardScreen {
        return baseScreen.on(screen: CarfaxStandaloneCardScreen.self)
    }

    @discardableResult
    func tap(headerButton button: CarfaxStandaloneCardScreen.HeaderButton) -> Self {
        let screen = onScreen()
        screen.scrollTo(element: screen.find(by: button.rawValue).firstMatch).tap()
        return self
    }

    func scrollBackToHeaderTop() -> Self {
        let screen = onScreen()
        screen.scrollTo(element: screen.headerBlockStart, swipeDirection: .down)
        return self
    }

    @discardableResult
    func scrollToHeaderBottom() -> Self {
        let screen = onScreen()
        screen.scrollTo(element: screen.headerBlockEnd)
        return self
    }

    func snapshotCollapsedHeader() -> UIImage {
        let screen = onScreen()
        // TODO: use ids in js
        return Snapshot.screenshotCollectionView(fromCell: screen.headerBlockStart, toCell: screen.headerBlockEnd)
    }

    func snapshotExpandedHeaderContent() -> UIImage {
        // TODO: use ids in js
        return onScreen().headerBlockExpandedContent.waitAndScreenshot().image
    }

    func snapshotRecallsBlock() -> UIImage {
        let screen = onScreen()
        return Snapshot.screenshotCollectionView(fromCell: screen.recallsPreHeader, toCell: screen.vehiclePhotosPreHeader)
    }

    func scrollToRecalls() -> Self {
        let screen = onScreen()
        screen.scrollTo(element: screen.recallsFooterLabel, maxSwipes: 40)
        // TODO: set ui id in js!!
        return self
    }

    func openRecall(name: String) -> ModalSteps<CarfaxStandaloneCardBasicSteps, LayoutPopUpModalScreen> {
        onScreen()
            .recall(like: name)
            .shouldExist()
            .tap()
        return ModalSteps(context: context, source: self)
    }

    func openHistoryOffer() -> SaleCardSteps {
        onScreen()
            .find(by: "Смотреть объявление").firstMatch
            .shouldExist()
            .tap()
        return SaleCardSteps(context: context)
    }

    @discardableResult
    func toggleEquipment(name: String) -> Self {
        onScreen().find(by: name).firstMatch.shouldExist().tap()
        return self
    }

    func findLabel(text: String) -> XCUIElement {
        let screen = onScreen()
        let label = screen.findContainedText(by: text).firstMatch
        screen.scrollTo(element: label, maxSwipes: 5)
        return label
    }

    @discardableResult
    func closePopup() -> Self {
        onScreen().find(by: "dismiss_modal_button").firstMatch.tap()
        return self
    }

    @discardableResult
    func scrollToReportPollFooter() -> Self {
        onScreen().scrollableElement.scrollTo(
            element: onScreen().reportPollLoaderFooter,
            swipeDirection: .up,
            maxSwipes: 20
        )
        return self
    }

    func snapshotReportPollFooter() -> UIImage {
        return onScreen()
            .reportPollLoaderFooter
            .waitAndScreenshot().image
    }

    func snapshotReportPollHeader() -> UIImage {
        return onScreen()
            .reportPollLoaderHeader
            .waitAndScreenshot().image
    }

    @discardableResult
    func checkHasReportPollHeader() -> Self {
        onScreen().reportPollLoaderHeader.shouldExist()
        return self
    }

    @discardableResult
    func checkHasNoReportPollHeader(timeout: TimeInterval = 5) -> Self {
        onScreen().reportPollLoaderHeader.shouldNotExist(timeout: timeout)
        return self
    }

    @discardableResult
    func checkHasNoReportPollFooter(timeout: TimeInterval = 5) -> Self {
        onScreen().reportPollLoaderFooter.shouldNotExist(timeout: timeout)
        return self
    }

    @discardableResult
    func checkHasActivityHUD(timeout: TimeInterval = 5) -> Self {
        onScreen().activityHUD.shouldExist(timeout: timeout)
        return self
    }

    @discardableResult
    func checkHasNoActivityHUD() -> Self {
        onScreen().activityHUD.shouldNotExist()
        return self
    }

    @discardableResult
    func validateActivityHUD() -> Self {
        onScreen().activityHUD.shouldExist()
        validateSnapshot(of: onScreen().activityHUD, ignoreEdges: UIEdgeInsets(top: 16, left: 24, bottom: 16, right: 24))
        return self
    }

    @discardableResult
    func snapshotOffersHistoryGallery() -> UIImage {
        return onScreen().offersHistoryGallery.waitAndScreenshot().image
    }

    func checkHasOffersHistoryCurrentBadge() {
        onScreen().offersHistoryCurrentOfferBadge.shouldExist()
    }

    func checkReportTitle(withText text: String) -> Self {
        onScreen().reportHeader.staticTexts[text].shouldExist()
        return self
    }

    func checkReportContainerIsDisplayed() -> Self {
        onScreen().reportContainer.shouldExist()
        return self
    }

    func tapExpandableComment(at index: Int) {
        onScreen().expandableComment(at: index).shouldExist().tap()
    }

    func tapExpandedComment(at index: Int) {
        onScreen().expandedComment(at: index).shouldExist().tap()
    }

    func findVIN(_ vin: String) -> [XCUIElement] {
        return onScreen().findContainedTextView(by: vin).allElementsBoundByIndex
    }

    func snapshot(element: XCUIElement) -> UIImage {
        return element.waitAndScreenshot().image
    }

    func scrollToMileage(_ value: Int) -> Self {
        onScreen().scrollTo(element: onScreen().find(by: "audatex_mileage_\(value)").firstMatch)
        return self
    }

    func scrollToCredit() -> Self {
        onScreen().scrollTo(element: onScreen().creditBanner, windowInsets: .init(top: 0, left: 0, bottom: 64, right: 0), swipeDirection: .up)
        return self
    }

    func snapshotMileage(value: Int) -> UIImage {
        return onScreen().find(by: "audatex_mileage_\(value)").firstMatch.waitAndScreenshot().image
    }

    @discardableResult
    func buyFullReport() -> Self {
        onScreen().buyCarReportButton.tap()
        return self
    }

    func chechBuyFullReportButtonExist() -> Self {
        onScreen().buyCarReportButton.shouldExist()
        return self
    }

    func chechBuyFullReportButtonNotExist() -> Self {
        onScreen().buyCarReportButton.shouldNotExist()
        return self
    }

    func tapBack() {
        onScreen().backButton.tap()
    }

    func snapshotOwnersBlockItem(at index: Int) -> UIImage {
        return onScreen().find(by: "owners_block_\(index)")
            .firstMatch.waitAndScreenshot()
            .image
    }

    func tapNotPaidFinesLabel() -> ModalSteps<CarfaxStandaloneCardBasicSteps, LayoutPopUpModalScreen> {
        onScreen().notPaidFinesLabel.tap()
        return ModalSteps(context: context, source: self)
    }
}
