//
//  ProfileScreen.swift
//  UITests
//
//  Created by Dmitry Sinev on 2/2/22.
//

import XCTest
import Snapshots

final class UserProfileScreen_: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case publicProfileSwitchSnippet = "public_profile_snippet"
    }

    static let rootElementID = "UserProfileViewController"
    static let rootElementName = "Экран профиля"
}
