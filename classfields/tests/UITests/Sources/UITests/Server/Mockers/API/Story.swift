import Foundation
import AutoRuProtoModels

extension Mocker {
    @discardableResult
    func mock_storySearch(stories: [Vertis_Story_Story]) -> Self {
        server.addMessageHandler("GET /story/search") { _, _ in
            return Vertis_Story_StoryResponse.with { response in
                response.stories = stories
            }
        }

        return self
    }
}
