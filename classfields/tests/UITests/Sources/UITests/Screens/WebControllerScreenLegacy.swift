import XCTest
import Snapshots

final class WebControllerScreen: BaseScreen, Scrollable {
    lazy var mainView = find(by: "WebController").firstMatch

    lazy var scrollableElement = findAll(.webView).firstMatch
}
