import Foundation
import AutoRuProtoModels

extension Responses {
    enum Chat {
        enum Room { }
    }
}

extension Responses.Chat.Room {
    static func get(for state: BackendState) -> Auto_Api_RoomListingResponse {
        return .with { msg in
            msg.rooms = state.chats.rooms.map { room in
                makeRoom(room, state: state)
            }

            msg.status = .success
        }
    }

    static func get(id: BackendState.Chats.Room.ID, for state: BackendState) -> Auto_Api_RoomResponse {
        guard let room = state.chats.rooms.first(where: { $0.id == id })
        else { fatalError() }

        return .with { msg in
            msg.room = makeRoom(room, state: state)
            msg.status = .success
        }
    }

    private static func makeRoom(_ room: BackendState.Chats.Room, state: BackendState) -> Auto_Api_Chat_Room {
        return .with { msg in
            msg.id = room.id.rawValue
            if let offer = room.offer {
                let offer = makeOffer(offer, state: state)

                msg.roomType = .offer
                msg.subject = .with { msg in
                    msg.offer.value = offer
                    msg.offer.source = .with { msg in
                        msg.category = "cars"
                        msg.id = offer.id
                    }
                    if let user = room.users.first {
                        msg.offer.owner = user.id
                    }
                }

                for user in room.users {
                    msg.users.append(.with { msg in
                        msg.id = user.id
                        msg.profile = .with { msg in
                            msg.alias = user.alias
                        }
                    })
                }
            }

            msg.me = state.user.id
            msg.users.append(.with { msg in
                msg.id = state.user.id
                msg.profile = .with { msg in
                    msg.alias = state.user.alias
                }
            })

            if let message = room.messages.last {
                msg.lastMessage = .with { msg in
                    msg.roomID = room.id.rawValue
                    msg.id = message.id
                    msg.author = message.author ?? state.user.id
                    msg.payload = .with { msg in
                        msg.contentType = .textPlain
                        msg.value = message.text
                    }
                }
            }
        }
    }
}
