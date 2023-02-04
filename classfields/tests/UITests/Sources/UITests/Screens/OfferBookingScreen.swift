//
//  OfferBookingScreen.swift
//  UITests
//
//  Created by Alexander Ignatyev on 12/11/20.
//

import Foundation

final class OfferBookingScreen: BaseScreen {
    private(set) lazy var bookButton = find(by: "book_button").firstMatch
    private(set) lazy var nameTextField = find(by: "field_name").firstMatch
    private(set) lazy var phoneTextField = find(by: "field_phone").firstMatch
    private(set) lazy var addPhoneButton = find(by: "add_phone").firstMatch
    private(set) lazy var doneButton = find(by: "Готово").firstMatch
    private(set) lazy var showFavoritesButton = find(by: "show_favs").firstMatch
}
