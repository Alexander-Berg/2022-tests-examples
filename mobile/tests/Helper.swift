//
//  Created by Timur Turaev on 17.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import Utils
@testable import UnsubscribeMessage

internal final class TestNetworkPerformer: NetworkPerformer {
    var completeWithError = false

    var trashUnsubscribedMessages: [Bool] = .empty

    // swiftlint:disable:next function_parameter_count
    func unsubscribeMessage(email: String,
                            displayName: String,
                            messageType: Int,
                            destinatationFolderID: Int64,
                            trashUnsubscribedMessages: Bool,
                            completion: @escaping (Swift.Error?) -> Void) {
        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(100)) { [weak self] in
            self?.trashUnsubscribedMessages.append(trashUnsubscribedMessages)
            completion((self?.completeWithError ?? false) ? AppError.unknown("TestError") : nil)
        }
    }
}
