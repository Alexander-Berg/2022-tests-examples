//
//  ChatsListingTests.swift
//  AutoRu
//
//  Created by Sergey An. Sergeev on 09.09.2021.
//

import XCTest
import AutoRuAppearance
import AutoRuUtils
import AutoRuFetchableImage
import Snapshots
@testable import AutoRuChat
import AutoRuColorSchema
import Foundation

final class ChatDialogTests: BaseUnitTest {
    override func setUp() {
        super.setUp()
        self.setReplaceImagesWithStub(nil)
        FetchableImage.blockThreadUntilFinished = true
    }

    override func tearDown() {
        super.tearDown()
        self.setReplaceImagesDefaultBehavior()
        FetchableImage.blockThreadUntilFinished = false
    }

    func test_userStatusView_support() {
        XCTContext.runActivity(named: "Проверка лейаута плашки саппорт чата") { _ in }

        let mock = Mock.supportChat

        let model = UserStatusViewModel(title: mock.chat.userName ?? "",
                                        muted: mock.chat.info.muted,
                                        status: .online,
                                        avatar: mock.chat.userAvatar)

        Snapshot.compareWithSnapshot(layout: UserStatusViewLayout(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)
    }

    func test_userStatusView_withoutTitle_muted_online_withoutAvatar() {
        XCTContext.runActivity(named: "Проверка лейаута плашки пользователя замьюченная и прочитанная, с проданным оффером, с пользователем без имени и аватара") { _ in }

        let mock = Mock.offerChat(with: Mock.Info.mutedAndRead,
                                  offer: Mock.Offer.sold,
                                  users: [Mock.User.userWithoutNameAndAvatar])

        let model = UserStatusViewModel(title: mock.chat.userName ?? "",
                                        muted: mock.chat.info.muted,
                                        status: .online,
                                        avatar: mock.chat.userAvatar)

        Snapshot.compareWithSnapshot(layout: UserStatusViewLayout(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)
    }

    func test_userStatusView_withTitle_unmuted_typing_withAvatar() {
        XCTContext.runActivity(named: "Проверка лейаута плашки пользователя размьюченная и непрочитанная, с забанненым оффером, с пользователем с именем и аватаром") { _ in }

        let mock = Mock.offerChat(with: Mock.Info.unmutedAndUnread,
                                  offer: Mock.Offer.banned,
                                  users: [Mock.User.userBlocked])

        let model = UserStatusViewModel(title: mock.chat.userName ?? "",
                                        muted: mock.chat.info.muted,
                                        status: .typing,
                                        avatar: mock.chat.userAvatar)

        Snapshot.compareWithSnapshot(layout: UserStatusViewLayout(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)
    }

    func test_userStatusView_withTitle_unmuted_withStatus_withAvatar() {
        XCTContext.runActivity(named: "Проверка лейаута плашки пользователя размьюченная и непрочитанная, с активным оффером, с пользователем с именем и аватаром") { _ in }

        let mock = Mock.offerChat(with: Mock.Info.unmutedAndUnread,
                                  offer: Mock.Offer.active,
                                  users: [Mock.User.userBlocked])

        let model = UserStatusViewModel(title: mock.chat.userName ?? "",
                                        muted: mock.chat.info.muted,
                                        status: .text("Был в сети вчера"),
                                        avatar: mock.chat.userAvatar)

        Snapshot.compareWithSnapshot(layout: UserStatusViewLayout(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)
    }

    func test_offerPanelView_active() {
        XCTContext.runActivity(named: "Проверка лейаута ячейки активного оффера") { _ in }

        let mock = Mock.Offer.active

        let model = OfferPanelViewModel(image: URL(string: mock.photo), title: mock.name, subtitle: mock.active ? mock.info : "Продан")
        Snapshot.compareWithSnapshot(layout: OfferPanelViewLayout(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)
    }

    func test_offerPanelView_sold() {
        XCTContext.runActivity(named: "Проверка лейаута ячейки проданного оффера") { _ in }

        let mock = Mock.Offer.sold

        let model = OfferPanelViewModel(image: URL(string: mock.photo), title: mock.name, subtitle: mock.active ? mock.info : "Продан")
        Snapshot.compareWithSnapshot(layout: OfferPanelViewLayout(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)
    }

    func test_offerPanelView_banned() {
        XCTContext.runActivity(named: "Проверка лейаута ячейки забаненного оффера") { _ in }

        let mock = Mock.Offer.banned

        let model = OfferPanelViewModel(image: URL(string: mock.photo), title: mock.name, subtitle: mock.active ? mock.info : "Продан")
        Snapshot.compareWithSnapshot(layout: OfferPanelViewLayout(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)
    }
}

final class ChatListItemTests: BaseUnitTest {
    override func setUp() {
        super.setUp()
        self.setReplaceImagesWithStub(nil)
        FetchableImage.blockThreadUntilFinished = true
    }

    override func tearDown() {
        super.tearDown()
        self.setReplaceImagesDefaultBehavior()
        FetchableImage.blockThreadUntilFinished = false
    }

    func test_chatSupportListItem() {
        XCTContext.runActivity(named: "Проверка лейаута ячейки чата с саппортом") { _ in }
        let model = ChatListItemCellHelperModel(listItem: Mock.supportChat)
        Snapshot.compareWithSnapshot(cellHelper: ChatListItemCellHelper(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)
    }
    func test_chatNotificationsListItem() {
        XCTContext.runActivity(named: "Проверка лейаута ячейки чата с уведомлениями") { _ in }
        let model = ChatListItemCellHelperModel(listItem: Mock.notificationCenter)
        Snapshot.compareWithSnapshot(cellHelper: ChatListItemCellHelper(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)
    }
    func test_chatHelperListItem() {
        XCTContext.runActivity(named: "Проверка лейаута ячейки чата с помощником") { _ in }
        let model = ChatListItemCellHelperModel(listItem: Mock.helperChat)
        Snapshot.compareWithSnapshot(cellHelper: ChatListItemCellHelper(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)
    }
    func test_chatOfferListItem_mutedAndRead_activeOffer_userWithNameAndAvatar() {
        XCTContext.runActivity(named: "Проверка лейаута ячейки чата замьюченная и прочитанная, с активным оффером, с пользователем с именем и аватаром") { _ in }

        let mock = Mock.offerChat(with: Mock.Info.mutedAndRead,
                                  offer: Mock.Offer.active,
                                  users: [Mock.User.userWithNameAndAvatar])

        let model = ChatListItemCellHelperModel(listItem: mock)
        Snapshot.compareWithSnapshot(cellHelper: ChatListItemCellHelper(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)
    }
    func test_chatOfferListItem_unmutedAndUnread_soldOffer_userWithoutNameAndAvatar() {
        XCTContext.runActivity(named: "Проверка лейаута ячейки чата размьюченная и непрочитанная, с проданным оффером, с пользователем без имени и аватара") { _ in }

        let mock = Mock.offerChat(with: Mock.Info.unmutedAndUnread,
                                  offer: Mock.Offer.sold,
                                  users: [Mock.User.userWithoutNameAndAvatar])

        let model = ChatListItemCellHelperModel(listItem: mock)
        Snapshot.compareWithSnapshot(cellHelper: ChatListItemCellHelper(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)

    }
    func test_chatOfferListItem_unmutedAndUnread_bannedOffer_userBlocked() {
        XCTContext.runActivity(named: "Проверка лейаута ячейки чата размьюченная и непрочитанная, с забанненым оффером, с заблокированным пользователем") { _ in }

        let mock = Mock.offerChat(with: Mock.Info.unmutedAndUnread,
                                  offer: Mock.Offer.banned,
                                  users: [Mock.User.userBlocked])

        let model = ChatListItemCellHelperModel(listItem: mock)
        Snapshot.compareWithSnapshot(cellHelper: ChatListItemCellHelper(model: model),
                                     maxWidth: DeviceWidth.iPhone11,
                                     backgroundColor: ColorSchema.Background.surface)

    }
}

private struct Mock {
    static var supportChat: ChatListItem {
        let info = ChatEntity(
            id: "0ea296b962daa8b7ec924718a791560b",
            pinNumber: 2,
            offerId: "",
            ownerId: "",
            myId: "23116b8bae82962e",
            name: "Чат с поддержкой",
            photo: "https://vertis-frontend.s3.yandex.net/auto/frontend/chat-logo/icon-166x124.png",
            date: Date(timeIntervalSinceReferenceDate: 123),
            read: true,
            muted: false,
            roomType: AutoRuChat.ChatEntity.RoomType.techSupport)

        let users = [ChatUserEntity(
                        id: "23116b8bae82962e",
                        chatId: "0ea296b962daa8b7ec924718a791560b",
                        chatBlocked: false,
                        name: "",
                        avatar: "",
                        averageReplyDelay: -1,
                        lastSeen: Date(timeIntervalSinceReferenceDate: 0),
                        roomLastRead: nil,
                        isSystemUser: false),
                     ChatUserEntity(
                        id: "b26a546839bbf179",
                        chatId: "0ea296b962daa8b7ec924718a791560b",
                        chatBlocked: false,
                        name: "Авто.ру",
                        avatar: "",
                        averageReplyDelay: -1,
                        lastSeen: nil,
                        roomLastRead: nil,
                        isSystemUser: false),
                     ChatUserEntity(
                        id: "autoru_0ea296b962daa8b7ec924718a791560b",
                        chatId: "0ea296b962daa8b7ec924718a791560b",
                        chatBlocked: false,
                        name: "Авто.ру",
                        avatar: "",
                        averageReplyDelay: 0,
                        lastSeen: nil,
                        roomLastRead: nil,
                        isSystemUser: true)]

        let chat = ChatObject(
            info: info,
            draft: nil,
            offer: nil,
            users: users)

        return ChatListItem(chat: chat, message: nil)
    }

    static var notificationCenter: ChatListItem {
        let info = ChatEntity(
            id: "testChat-28199386-52470010",
            pinNumber: 0,
            offerId: "",
            ownerId: "",
            myId: "ebb8af287dbeefac",
            name: "Канал новостей и предложений",
            photo: "https://avatars.mds.yandex.net/get-autoru-users/42027/7c3cb5e4b6118329919465a8f8d20115/430x600",
            date: Date(timeIntervalSinceReferenceDate: 0),
            read: true,
            muted: false,
            roomType: AutoRuChat.ChatEntity.RoomType.simple)

        let users = [ChatUserEntity(
                        id: "983882ea336be748",
                        chatId: "testChat-28199386-52470010",
                        chatBlocked: false,
                        name: "Центр уведомлений",
                        avatar: "https://avatars.mds.yandex.net/get-autoru-users/42027/7c3cb5e4b6118329919465a8f8d20115/100x100",
                        averageReplyDelay: 37794,
                        lastSeen: Date(timeIntervalSinceReferenceDate: 0),
                        roomLastRead: nil,
                        isSystemUser: false),
                     ChatUserEntity(
                        id: "ebb8af287dbeefac",
                        chatId: "testChat-28199386-52470010",
                        chatBlocked: false,
                        name: "",
                        avatar: "",
                        averageReplyDelay: -1,
                        lastSeen: Date(timeIntervalSinceReferenceDate: 0),
                        roomLastRead: Date(timeIntervalSinceReferenceDate: 0),
                        isSystemUser: false),
                     ChatUserEntity(
                        id: "autoru_testChat-28199386-52470010",
                        chatId: "testChat-28199386-52470010",
                        chatBlocked: false,
                        name: "Авто.ру",
                        avatar: "",
                        averageReplyDelay: 0,
                        lastSeen: nil,
                        roomLastRead: nil,
                        isSystemUser: true)]

        let messageEntity = ChatMessageEntity(
            uid: "shark-46b642ee-0fd3-411e-a92d-1b6312386add",
            serverId: "11ec108ef677a78083f06d94e797d3a6",
            chatId: "testChat-28199386-52470010",
            userId: "983882ea336be748",
            date: Date(timeIntervalSinceReferenceDate: 0),
            status: AutoRuChat.ChatMessageEntity.Status.success,
            contentType: AutoRuChat.ChatMessageEntity.ContentType.plainText,
            content: "Выбираете машину?🚗 Нет смысла тянуть дальше с покупкой – машины дорожают, а деньги дешевеют! Просто <a href=\"https://auto.ru/my/credits\">оставьте заявку</a>, и мы подберём для вас идеальные условия по кредиту🔥!",
            messageType: AutoRuChat.ChatMessageEntity.MessageType.regular,
            encodedProperties: "",
            encodedSendingAttributes: "[]")

        let chatUserEntity = ChatUserEntity(
            id: "983882ea336be748",
            chatId: "testChat-28199386-52470010",
            chatBlocked: false,
            name: "Центр уведомлений",
            avatar: "https://avatars.mds.yandex.net/get-autoru-users/42027/7c3cb5e4b6118329919465a8f8d20115/100x100",
            averageReplyDelay: 37794,
            lastSeen: Date(timeIntervalSinceReferenceDate: 0),
            roomLastRead: nil,
            isSystemUser: false)

        let messageObject = ChatMessageObject(
            info: messageEntity,
            author: chatUserEntity,
            isSpam: false)

        let chat = ChatObject(
            info: info,
            draft: nil,
            offer: nil,
            users: users)

        return ChatListItem(chat: chat,
                            message: messageObject)
    }

    static var helperChat: ChatListItem {
        let info = ChatEntity(
            id: "c9f262ac58f3f38c3dad07a77e8296e3",
            pinNumber: 0,
            offerId: "",
            ownerId: "",
            myId: "d452fa3bd804fc99",
            name: "Покажу всё, что скрыто",
            photo: "https://avatars.mds.yandex.net/get-vertis-chat/1653616/9cc2a041324f5a9a16eac3a1215af130/1200x1200",
            date: Date(timeIntervalSinceReferenceDate: 0),
            read: true,
            muted: false,
            roomType: AutoRuChat.ChatEntity.RoomType.chatBot)

        let users = [ChatUserEntity(
                        id: "ca76a85c468fcebf",
                        chatId: "c9f262ac58f3f38c3dad07a77e8296e3",
                        chatBlocked: false,
                        name: "Помощник осмотра",
                        avatar: "",
                        averageReplyDelay: -1,
                        lastSeen: nil,
                        roomLastRead: nil,
                        isSystemUser: false),
                     ChatUserEntity(
                        id: "d452fa3bd804fc99",
                        chatId: "c9f262ac58f3f38c3dad07a77e8296e3",
                        chatBlocked: false,
                        name: "",
                        avatar: "",
                        averageReplyDelay: -1,
                        lastSeen: Date(timeIntervalSinceReferenceDate: 0),
                        roomLastRead: nil,
                        isSystemUser: false),
                     ChatUserEntity(
                        id: "autoru_c9f262ac58f3f38c3dad07a77e8296e3",
                        chatId: "c9f262ac58f3f38c3dad07a77e8296e3",
                        chatBlocked: false,
                        name: "Авто.ру",
                        avatar: "",
                        averageReplyDelay: 0,
                        lastSeen: nil,
                        roomLastRead: nil,
                        isSystemUser: true)]

        let messageEntity = ChatMessageEntity(
            uid: "0195B516-ACF7-41C6-9521-178DCE84197B",
            serverId: "11eba81da07b3880a8f869cd526705c6",
            chatId: "c9f262ac58f3f38c3dad07a77e8296e3",
            userId: "ca76a85c468fcebf",
            date: Date(timeIntervalSinceReferenceDate: 0),
            status: AutoRuChat.ChatMessageEntity.Status.success,
            contentType: AutoRuChat.ChatMessageEntity.ContentType.plainText,
            content: "<b>Привет!</b> ✌\n\nЭто Помощник осмотра от Авто.ру. Если вы собираетесь купить автомобиль и нашли подходящий вариант, значит, скоро поедете на его осмотр. Я бесплатно помогу вам правильно оценить автомобиль и покажу важную информацию о нём.\n\nКогда соберётесь на встречу с продавцом, нажмите <b>Еду смотреть</b>.\n\nОбратите внимание ⚠️ \nЯ помогаю только с легковыми автомобилями, которые продаются на Авто.ру. Объявление должно быть активно.",
            messageType: AutoRuChat.ChatMessageEntity.MessageType.withActions,
            encodedProperties: "{\"actions\":[{\"id\":\"\",\"value\":\"Еду смотреть\"}]}",
            encodedSendingAttributes: "[]")

        let chatUserEntity = ChatUserEntity(
            id: "ca76a85c468fcebf",
            chatId: "c9f262ac58f3f38c3dad07a77e8296e3",
            chatBlocked: false,
            name: "Помощник осмотра",
            avatar: "",
            averageReplyDelay: -1,
            lastSeen: nil,
            roomLastRead: nil,
            isSystemUser: false)

        let messageObject = ChatMessageObject(
            info: messageEntity,
            author: chatUserEntity,
            isSpam: false)

        let chat = ChatObject(
            info: info,
            draft: nil,
            offer: nil,
            users: users)

        return ChatListItem(chat: chat,
                            message: messageObject)
    }

    struct User {
        static var userWithNameAndAvatar: ChatUserEntity {
            ChatUserEntity(id: "4bd0886985f4e1a4",
                           chatId: "4d7cf478eaf6b3cf4c58cc94a2e151b3",
                           chatBlocked: false,
                           name: "Jhonny Jhonnynovich",
                           avatar: "https://avatars.mds.yandex.net/get-autoru-users/42027/7c3cb5e4b6118329919465a8f8d20115/100x100",
                           averageReplyDelay: 37794,
                           lastSeen: Date(timeIntervalSinceReferenceDate: 0),
                           roomLastRead: nil,
                           isSystemUser: false)
        }

        static var userWithoutNameAndAvatar: ChatUserEntity {
            ChatUserEntity(id: "4bd0886985f4e1a4",
                           chatId: "4d7cf478eaf6b3cf4c58cc94a2e151b3",
                           chatBlocked: false,
                           name: "",
                           avatar: "",
                           averageReplyDelay: 37794,
                           lastSeen: Date(timeIntervalSinceReferenceDate: 0),
                           roomLastRead: nil,
                           isSystemUser: false)
        }

        static var userBlocked: ChatUserEntity {
            ChatUserEntity(id: "4bd0886985f4e1a4",
                           chatId: "4d7cf478eaf6b3cf4c58cc94a2e151b3",
                           chatBlocked: true,
                           name: "Jhonny Jhonnynovich",
                           avatar: "https://avatars.mds.yandex.net/get-autoru-users/42027/7c3cb5e4b6118329919465a8f8d20115/100x100",
                           averageReplyDelay: 37794,
                           lastSeen: Date(timeIntervalSinceReferenceDate: 0),
                           roomLastRead: nil,
                           isSystemUser: false)
        }
    }

    struct Offer {
        static var active: ChatOfferEntity {
            ChatOfferEntity(
                id: "1113823662-8a682f1a",
                category: "cars",
                name: "BMW 3 серии VII (G2x), BMW 3 серии VII (G2x), BMW 3 серии VII (G2x), BMW 3 серии VII (G2x), 2019",
                info: "3 826 000 ₽",
                photo: "",
                active: true,
                banned: false,
                section: "USED",
                regionName: "Москва",
                noCalls: false,
                isSafeDealRequestAvailable: false,
                price: 3_826_000
            )
        }

        static var sold: ChatOfferEntity {
            ChatOfferEntity(
                id: "1113823662-8a682f1a",
                category: "cars",
                name: "BMW 3 серии VII (G2x), 2019",
                info: "3 826 000 ₽",
                photo: "",
                active: false,
                banned: false,
                section: "USED",
                regionName: "Москва",
                noCalls: false,
                isSafeDealRequestAvailable: false,
                price: 3_826_000
            )
        }

        static var banned: ChatOfferEntity {
            ChatOfferEntity(
                id: "1113823662-8a682f1a",
                category: "cars",
                name: "BMW 3 серии VII (G2x), 2019",
                info: "3 826 000 ₽",
                photo: "",
                active: false,
                banned: true,
                section: "USED",
                regionName: "Москва",
                noCalls: false,
                isSafeDealRequestAvailable: false,
                price: 3_826_000
            )
        }
    }

    struct Info {
        static var mutedAndRead: ChatEntity {
            ChatEntity(
                id: "4d7cf478eaf6b3cf4c58cc94a2e151b3",
                pinNumber: 0,
                offerId: "1113823662-8a682f1a",
                ownerId: "4bd0886985f4e1a4",
                myId: "49c6803a37848771",
                name: "BMW 3 серии VII (G2x), 2019",
                photo: "",
                date: Date(timeIntervalSinceReferenceDate: 0),
                read: true,
                muted: true,
                roomType: AutoRuChat.ChatEntity.RoomType.offer)
        }

        static var unmutedAndUnread: ChatEntity {
            ChatEntity(
                id: "4d7cf478eaf6b3cf4c58cc94a2e151b3",
                pinNumber: 0,
                offerId: "1113823662-8a682f1a",
                ownerId: "4bd0886985f4e1a4",
                myId: "49c6803a37848771",
                name: "BMW 3 серии VII (G2x), 2019",
                photo: "",
                date: Date(timeIntervalSinceReferenceDate: 0),
                read: false,
                muted: false,
                roomType: AutoRuChat.ChatEntity.RoomType.offer)
        }
    }

    static func offerChat(with info: ChatEntity, offer: ChatOfferEntity, users: [ChatUserEntity]) -> ChatListItem {
        let messageEntity = ChatMessageEntity(
            uid: "7C546BB7-EEF5-4F2F-8CF4-626237A61A2C",
            serverId: "11ec0f21875080209cda837ce2e089a0",
            chatId: "4d7cf478eaf6b3cf4c58cc94a2e151b3",
            userId: "49c6803a37848771",
            date: Date(timeIntervalSinceReferenceDate: 0),
            status: AutoRuChat.ChatMessageEntity.Status.success,
            contentType: AutoRuChat.ChatMessageEntity.ContentType.plainText,
            content: "Здравствуйте, ещё продаётся?",
            messageType: AutoRuChat.ChatMessageEntity.MessageType.regular,
            encodedProperties: "",
            encodedSendingAttributes: "[]")

        let chatUserEntity = ChatUserEntity(
            id: "ca76a85c468fcebf",
            chatId: "c9f262ac58f3f38c3dad07a77e8296e3",
            chatBlocked: false,
            name: "Помощник осмотра",
            avatar: "",
            averageReplyDelay: -1,
            lastSeen: nil,
            roomLastRead: nil,
            isSystemUser: false)

        let messageObject = ChatMessageObject(
            info: messageEntity,
            author: chatUserEntity,
            isSpam: false)

        let chat = ChatObject(
            info: info,
            draft: nil,
            offer: offer,
            users: users)

        return ChatListItem(chat: chat,
                            message: messageObject)
    }
}
