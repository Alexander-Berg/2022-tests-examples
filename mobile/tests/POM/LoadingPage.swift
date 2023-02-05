import FormKit
import UIUtils
import XCTest

final class LoadingPage: PageObject {}

extension PageObject {
    var loadingPage: LoadingPage {
        let elem = element
            .scrollViews
            .matching(identifier: LoadingAccessibility.spinner)
            .firstMatch
        return LoadingPage(element: elem)
    }

    var simpleSpinner: LoadingPage {
        let elem = element
            .scrollViews
            .matching(identifier: LoadingAccessibility.simpleSpinner)
            .firstMatch
        return LoadingPage(element: elem)
    }
}
