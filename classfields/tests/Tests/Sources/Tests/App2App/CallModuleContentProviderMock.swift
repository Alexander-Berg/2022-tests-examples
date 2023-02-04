import AutoRuCallsUI

final class CallModuleContentProviderMock: CallModuleContentProvider {
    var content: CallModuleContent

    init(content: CallModuleContent) {
        self.content = content
    }

    func getContent() -> CallModuleContent {
        content
    }
}
