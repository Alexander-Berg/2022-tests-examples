//
//  MessageModel+Stub.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 25.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swift:disable force_unwrapping

import Foundation
import ChattoAdditions

extension MessageModel {
    static func makeStub(
        isIncoming: Bool = true,
        date: Date = Date.make(string: "2022-06-17 12:42:00"),
        status: MessageStatus = .success
    ) -> MessageModel {
        MessageModel(
            uid: "",
            senderId: "",
            type: "",
            isIncoming: isIncoming,
            date: date,
            status: .success
        )
    }
}

extension Date {
    fileprivate static func make(string: String) -> Date! {
        return self.dateFormatter.date(from: string)
    }

    private static let dateFormatter: DateFormatter = {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return dateFormatter
    }()
}
