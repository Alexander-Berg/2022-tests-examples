//
//  Payment.swift
//  UITests
//
//  Created by Andrei Iarosh on 28.05.2021.
//

extension Mocker {
    @discardableResult
    func mock_paymentInitWithAttachedCard() -> Self {
        server.addHandler("POST /billing/autoru/payment/init") { (_, _) -> Response? in
            Response.okResponse(fileName: "billing_autoru_payment_init_attached_card", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_paymentProcess() -> Self {
        server.addHandler("POST /billing/autoru/payment/process") { (_, _) -> Response? in
            Response.okResponse(fileName: "billing_autoru_payment_process", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_paymentClosed() -> Self {
        server.addHandler("GET /billing/autoru/payment *") { (_, _) -> Response? in
            Response.okResponse(fileName: "billing_autoru_payment", userAuthorized: true)
        }
        return self
    }
}
