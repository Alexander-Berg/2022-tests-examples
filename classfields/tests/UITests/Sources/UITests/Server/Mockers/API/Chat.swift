//
//  Chat.swift
//  UITests
//
//  Created by Pavel Savchenkov on 20.10.2021.
//

import Foundation

extension Mocker {
    @discardableResult
    func mock_postChatRoom(_ file: String = "chat_new_room") -> Self {
        server.addHandler("POST /chat/room *") { request, _ -> Response? in
            return Response.okResponse(fileName: file, userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_getChatRoom(_ file: String = "chat_rooms_only_techsup_room") -> Self {
        server.addHandler("GET /chat/room *") { request, _ -> Response? in
            return Response.okResponse(fileName: file, userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_getChatMessage() -> Self {
        server.addHandler("GET /chat/message *") { request, _ -> Response? in
            return Response.okResponse(fileName: "success", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_getChatMessageSpam() -> Self {
        server.addHandler("GET /chat/message/spam *") { request, _ -> Response? in
            return Response.okResponse(fileName: "success", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_deleteChatMessageUnread() -> Self {
        server.addHandler("DELETE /chat/message/unread *") { request, _ -> Response? in
            return Response.okResponse(fileName: "success", userAuthorized: true)
        }
        return self
    }
}
