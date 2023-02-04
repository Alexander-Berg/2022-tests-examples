//
//  PresetMessageModel+Stub.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 25.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import ChattoAdditions
@testable import YREChatModule

extension PresetMessageModel {
    static func makeStub(
        presets: [PresetViewModel],
        messageStatus: MessageStatus = .success
    ) -> PresetMessageModel {
        PresetMessageModel(
            messageModel: MessageModel.makeStub(status: messageStatus),
            presets: Observable<[PresetViewModel]>(presets)
        )
    }
}
