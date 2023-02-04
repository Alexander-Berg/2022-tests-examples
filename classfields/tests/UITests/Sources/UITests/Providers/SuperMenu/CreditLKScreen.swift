//
//  CreditLKScreen.swift
//  UITests
//
//  Created by Dmitry Sinev on 2/4/22.
//

import XCTest

final class CreditLKScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case promoTop
        case promoCalculatorTop
        case promoBetterWithUs
        case promo3Steps
        case promo3StepsNote
        case promoSendFormButton
        case promoPartners
        case promoPartners0
        case promoPartners1
        case promoFAQ_question
        case promoFAQ_answer
        case fieldFIO
        case fieldEmail
        case fieldPhone
        case submitButton
        case fieldSMSCode
        case editCreditSum
        case agreementCheckBoxOn
        case agreementCheckBoxOff
        case agreementText
        case questionmark
        case creditSumSliderThumb
        case percentlessTitle
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .promoTop:
            return "credit_full_promo_top"
        case .promoCalculatorTop:
            return "credit_calculator_promo_top"
        case .promoBetterWithUs:
            return "credit_better_with_us_promo"
        case .promo3Steps:
            return "credit_3steps_promo"
        case .promo3StepsNote:
            return "credit_form_note_promo"
        case .promoSendFormButton:
            return "credit_send_form_promo"
        case .promoPartners:
            return "credit_partners_promo"
        case .promoPartners0:
            return "tinkoff_auto"
        case .promoPartners1:
            return "sberbank"
        case .promoFAQ_question:
            return "credit_faq_question_promo_0"
        case .promoFAQ_answer:
            return "credit_faq_answer_promo_0"
        case .fieldFIO:
            return "field_name"
        case .fieldEmail:
            return "field_email"
        case .fieldPhone:
            return "field_phone"
        case .submitButton:
            return "button"
        case .fieldSMSCode:
            return "field_login"
        case .editCreditSum:
            return "edit-credit-sum"
        case .agreementCheckBoxOn:
            return "check-on"
        case .agreementCheckBoxOff:
            return "check-off"
        case .agreementText:
            return "agreement"
        case .questionmark:
            return "questionmark"
        case .creditSumSliderThumb:
            return "creditSumSlider_thumb"
        case .percentlessTitle:
            return "Подбор кредита в разных банках"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .promoTop:
            return "Кредитное промо верх"
        case .promoCalculatorTop:
            return "Кредитное промо калькулятор верх"
        case .promoBetterWithUs:
            return "С нами удобнее"
        case .promo3Steps:
            return "Всего 3 шага"
        case .promo3StepsNote:
            return "Не обязательность кредита"
        case .promoSendFormButton:
            return "Рассчитать кредит"
        case .promoPartners:
            return "С кем работаем"
        case .promoPartners0:
            return "tinkoff_auto"
        case .promoPartners1:
            return "sberbank"
        case .promoFAQ_question:
            return "Вопрос"
        case .promoFAQ_answer:
            return "Ответ"
        case .fieldFIO:
            return "FIO"
        case .fieldEmail:
            return "Email"
        case .fieldPhone:
            return "Phone"
        case .submitButton:
            return "Submit"
        case .fieldSMSCode:
            return "СМС-код"
        case .editCreditSum:
            return "Сумма кредита, редактирование"
        case .agreementCheckBoxOn:
            return "Чекбокс согласия включён"
        case .agreementCheckBoxOff:
            return "Чекбокс согласия выключен"
        case .agreementText:
            return "Согласие"
        case .questionmark:
            return "Иконка вопроса"
        case .creditSumSliderThumb:
            return "Ползунок слайдера кредита"
        case .percentlessTitle:
            return "Подбор кредита в разных банках"
        }
    }
    static let rootElementID = "CreditLKViewController"
    static let rootElementName = "Список кредитов"
}

final class CreditWizardScreen: BaseSteps, UIRootedElementProvider {
    typealias Element = Void
    static let rootElementID = "WizardViewController"
    static let rootElementName = "Кредитный визард"
}

final class CreditFormScreen: BaseSteps, UIRootedElementProvider {
    typealias Element = Void
    static let rootElementID = "CreditFormViewController"
    static let rootElementName = "Кредитная анкета"
}
