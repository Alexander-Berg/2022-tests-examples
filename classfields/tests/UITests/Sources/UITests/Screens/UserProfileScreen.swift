//
//  UserProfileScreen.swift
//  AutoRu
//
//  Created by Vitalii Stikhurov on 02.03.2020.
//

import XCTest
import Snapshots

class UserProfileScreen: BaseScreen {
    enum Picker: String {
        case experience = "Стаж вождения"
        case birthday = "Дата рождения"
        case city = "Город"
        case email = "email"
        case email_phoneConfirmation = "Готово"
        case password = "Пароль"
        case social = "social"
        case currentSocial = "SocialAccounts"
        case avatar = "UserAvatar"

        var validationLabel: String {
            switch self {
            case .experience:
                return "Год начала вождения"
            case .birthday:
                return "Сбросить"
            case .city:
                return "Город"
            case .email:
                return "Новая почта"
            case .email_phoneConfirmation:
                return "Подтвердите операцию"
            case .password:
                return "Сменить пароль"
            case .social:
                return "Добавить аккаунт"
            case .currentSocial:
                return "Привязанные аккаунты"
            case .avatar:
                return "AttachmentsPickerViewController"
            }
        }
    }

    lazy var title = find(by: "Профиль").firstMatch
    lazy var about = find(by: "about").firstMatch
    lazy var name = find(by: "name").firstMatch
    lazy var nameTitle = find(by: "NameTitle").firstMatch
    lazy var nameSubtitle = find(by: "NameSubtitle").firstMatch
    lazy var logout = find(by: "logout").firstMatch
    lazy var delete = find(by: "delete_account").firstMatch
    lazy var addNameButton = find(by: "Добавить имя").firstMatch
    lazy var vkicon = find(by: "soc_vk").firstMatch

    func pickerElement(_ picker: Picker) -> XCUIElement {
        return find(by: picker.rawValue).firstMatch
    }
}
