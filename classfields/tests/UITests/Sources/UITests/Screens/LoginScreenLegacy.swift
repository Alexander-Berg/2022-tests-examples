//
//  LoginScreen.swift
//  UITests
//
//  Created by Victor Orlovsky on 26/03/2019.
//

class LoginScreen: BaseScreen {
    public lazy var titleText = findStaticText(by: "Войдите, чтобы продолжить")
    public lazy var phoneInput = findAll(.staticText)["Номер телефона"]
    public lazy var closeButton = app.buttons["close"]
}
