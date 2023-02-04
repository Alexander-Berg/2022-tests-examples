#if DEBUG

import Foundation

public struct TestCallPreset: Identifiable, Codable {
    public var id: UUID
    public var name: String
    public var voxUsername: String
    public var mode: TestPayloadMode
    public var offerPayload: TestOfferPayload
    public var genericPayload: TestGenericPayload
    public var rawPayload: TestRawPayload

    public init(
        id: UUID,
        name: String,
        voxUsername: String = "app2app",
        mode: TestPayloadMode = TestPayloadMode.offer,
        offerPayload: TestOfferPayload = TestOfferPayload(),
        genericPayload: TestGenericPayload = TestGenericPayload(),
        rawPayload: TestRawPayload = TestRawPayload()
    ) {
        self.id = id
        self.name = name
        self.voxUsername = voxUsername
        self.mode = mode
        self.offerPayload = offerPayload
        self.genericPayload = genericPayload
        self.rawPayload = rawPayload
    }
}

public enum TestPayloadMode: String, CaseIterable, Codable {
    case offer
    case generic
    case raw
}

public struct TestOfferPayload: Codable {
    public var redirectID: String
    public var alias: String
    public var aliasAndSubject: String
    public var userPic: String
    public var mark: String
    public var model: String
    public var generation: String
    public var year: String
    public var pic: String
    public var link: String
    public var price: String
    public var currency: String

    public init(
        redirectID: String = "",
        alias: String = "Евгений",
        aliasAndSubject: String = "Евгений • Chevrolet Camaro II, 2000",
        userPic: String = "",
        mark: String = "Chevrolet",
        model: String = "Camaro",
        generation: String = "II",
        year: String = "2000",
        pic: String = "https://avatars.mds.yandex.net/get-autoru-vos/4121071/3069e95542b32eaff0ef3771670da45e/1200x900n",
        link: String = "https://auto.ru/cars/used/sale/chevrolet/camaro/1103478474-44689958/",
        price: String = "3500000",
        currency: String = "RUR"
    ) {
        self.redirectID = redirectID
        self.alias = alias
        self.aliasAndSubject = aliasAndSubject
        self.userPic = userPic
        self.mark = mark
        self.model = model
        self.generation = generation
        self.year = year
        self.pic = pic
        self.link = link
        self.price = price
        self.currency = currency
    }
}

public struct TestGenericPayload: Codable {
    public var redirectID: String
    public var alias: String
    public var userPic: String
    public var aliasAndSubject: String
    public var image: String
    public var url: String
    public var line1: String
    public var line2: String
    public var handle: String
    public var subjectType: String

    public init(
        redirectID: String = "",
        alias: String = "Евгений",
        aliasAndSubject: String = "Евгений • Chevrolet Camaro II, 2000",
        userPic: String = "",
        image: String = "https://avatars.mds.yandex.net/get-autoru-vos/4121071/3069e95542b32eaff0ef3771670da45e/1200x900n",
        url: String = "https://auto.ru/cars/used/sale/chevrolet/camaro/1103478474-44689958/",
        line1: String = "",
        line2: String = "",
        handle: String = "",
        subjectType: String = ""
    ) {
        self.redirectID = redirectID
        self.alias = alias
        self.aliasAndSubject = aliasAndSubject
        self.userPic = userPic
        self.image = image
        self.url = url
        self.line1 = line1
        self.line2 = line2
        self.handle = handle
        self.subjectType = subjectType
    }
}

public struct TestRawPayload: Codable {
    public struct Item: Hashable, Codable {
        public var header: String
        public var value: String

        public init(header: String = "", value: String = "") {
            self.header = header
            self.value = value
        }
    }

    public var items: [Item]

    public init(items: [Item] = []) {
        self.items = items
    }
}

#endif
