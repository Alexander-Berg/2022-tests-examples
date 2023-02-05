//
// Created by Artem I. Novikov on 13/02/2020.
// Copyright (c) 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class LabelNavigatorApplication: LabelNavigator {
    private let messageListPage = MessageListPage()
    private let foldersListPage = FoldersListPage()

    public func getLabelList() -> YSArray<LabelName> {
        XCTContext.runActivity(named: "Getting user's labels list") { _ in
            let labels: [UserLabelElement] = self.foldersListPage.userLabels
            let labelNameList = labels.map {
                $0.labelName.label
            }
            return YSArray(array: labelNameList.map { labelName in
                LabelName(labelName)
            })
        }
    }

    public func goToLabel(_ labelDisplayName: String) throws {
        try XCTContext.runActivity(named: "Going to label with name \"\(labelDisplayName)\"") { _ in
            guard let userFolder = self.foldersListPage.findUserLabelByName(labelDisplayName) else {
                throw YSError("Unable to find user label with name \"\(labelDisplayName)\"")
            }
            try scrollIfNeeded(view: self.foldersListPage.view, andTapOn: userFolder.labelName)
        }
    }
}
