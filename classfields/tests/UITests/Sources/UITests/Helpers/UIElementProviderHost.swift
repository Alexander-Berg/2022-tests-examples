import XCTest

// провайдер, позволяющий переключаться между несколькими представлениями. Например, UITabBarController
protocol UIToggleContainer: UIElementProvider {
    associatedtype ContentID

    func toggle(to content: ContentID)
}

struct UIToggleContainerContentItem<ContentID, Content>
where
Content: _InitializableContextProvider,
Content: _UIRootedElementProvider
{
    let contentID: ContentID
}

extension UIToggleContainer {
    static func contentItem<Content>(
        _: UIElementProviderWitness<Content>,
        with id: ContentID
    ) -> UIToggleContainerContentItem<ContentID, Content>
    where Content: _InitializableContextProvider,
          Content: _UIRootedElementProvider
    {
        .init(contentID: id)
    }
}

struct UIElementProviderHost<Container: UIToggleContainer, Content: UIElementProvider> {
    typealias Element = Content.Element

    var container: Container
    fileprivate let content: Content

    init(
        container: Container,
        content: Content
    ) {
        self.container = container
        self.content = content
    }

    func toggle<NewContent: UIElementProvider>(
        to content: KeyPath<Container, UIToggleContainerContentItem<Container.ContentID, NewContent>>
    ) -> UIElementProviderHost<Container, NewContent> {
        let id = container[keyPath: content].contentID

        container.toggle(to: id)

        let contentRoot = NewContent.findRoot(in: container.app, parent: container.rootElement)
        let content = NewContent(context: container.context, root: contentRoot)

        return UIElementProviderHost<Container, NewContent>(
            container: container,
            content: content
        )
    }
}

extension UIElementProviderHost: UIElementProvider {
    var context: StepsContext {
        container.context
    }

    var rootElement: XCUIElement {
        container.rootElement
    }

    var app: XCUIApplication {
        container.app
    }

    func find(element: Element) -> XCUIElement {
        content.find(element: element)
    }

    func name(of element: Element) -> String {
        content.name(of: element)
    }

    func identifier(of element: Element) -> String {
        content.identifier(of: element)
    }
}

extension UIElementProviderHost: _InitializableContextProvider, _UIRootedElementProvider
where
Container: _InitializableContextProvider,
Container: _UIRootedElementProvider,
Content: _InitializableContextProvider,
Content: _UIRootedElementProvider
{
    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        Container.findRoot(in: app, parent: parent)
    }

    static var rootElementName: String {
        Container.rootElementName
    }

    init(context: StepsContext, root: XCUIElement?) {
        let container = Container(context: context, root: root)
        let contentRoot = Content.findRoot(in: container.app, parent: container.rootElement)
        let content = Content(context: context, root: contentRoot)

        self = .init(container: container, content: content)
    }
}
