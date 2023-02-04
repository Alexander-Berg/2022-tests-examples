final class WebViewPicker: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "webView"
    static let rootElementName = "Пикер с вебвью"

    enum Element {
        case closeButton
        case _webViewURL // чтобы получать урл у вебвью
    }

    func name(of element: Element) -> String {
        switch element {
        case .closeButton: return "Кнопка закрытия"
        case ._webViewURL: return "URL вебвью (невидимый элемент)"
        }
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .closeButton: return "webViewCloseButton"
        case ._webViewURL: return "web_view_url"
        }
    }

    var currentURL: String {
        find(element: ._webViewURL).label
    }

    func closeBySwipe() {
        Step("Закрываем вебвью свайпом вниз") {
            rootElement.swipe(direction: .down)
        }
    }
}
