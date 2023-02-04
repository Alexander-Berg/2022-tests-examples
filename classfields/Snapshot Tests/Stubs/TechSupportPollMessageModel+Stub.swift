//
//  TechSupportPollMessageModel+Stub.swift
//  Unit Tests
//
//  Created by Dmitry Barillo on 25.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import ChattoAdditions
@testable import YREChatModule

extension TechSupportPollMessageModel {
    static func makeStub(pollState: TechSupportPollState) -> TechSupportPollMessageModel {
        TechSupportPollMessageModel(
            messageModel: MessageModel.makeStub(),
            state: Observable.init(pollState),
            title: TechSupportPollMessageModel.placeholder
        )
    }
}
