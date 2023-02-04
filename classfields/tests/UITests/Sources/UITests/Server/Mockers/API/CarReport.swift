//
//  Carfax.swift
//  UITests
//
//  Created by Andrei Iarosh on 28.05.2021.
//

import AutoRuProtoModels

extension Mocker {
    @discardableResult
    func mock_reportLayoutForOffer(bought: Bool, userCard: Bool = false, copyVIN: Bool = false, quotaLeft: UInt32 = 0) -> Self {
        let responseFileName: String
        if userCard {
            responseFileName = "CarReport-makeXmlForOffer-user"
        } else {
            if copyVIN {
                responseFileName = "CarReport-makeXmlForOffer-bought-copyVIN"
            } else {
                responseFileName = bought ? "CarReport-makeXmlForOffer-bought" : "CarReport-makeXmlForOffer"
            }
        }
        let response: Auto_Api_ReportLayoutResponse = .init(mockFile: responseFileName) {
            $0.billing.quotaLeft = quotaLeft
        }
        server.addHandler("GET /ios/makeXmlForOffer *") {
            response
        }
        return self
    }

    @discardableResult
    func mock_reportLayoutForSearch(bought: Bool) -> Self {
        server.addHandler("GET /ios/makeXmlForSearch *") { (_, _) -> Response? in
            return Response.okResponse(fileName: bought ? "CarReport-makeXmlForSearch-bought" : "CarReport-makeXmlForSearch", userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_reportLayoutForReport(bought: Bool, quotaLeft: UInt32 = 0) -> Self {
        let responseFileName = bought ? "CarReport-makeXmlForReport-bought" : "CarReport-makeXmlForReport"

        let response: Auto_Api_ReportLayoutResponse = .init(mockFile: responseFileName) {
            $0.billing.quotaLeft = quotaLeft
        }
        server.addHandler("GET /ios/makeXmlForReport *") {
            response
        }
        return self
    }

    @discardableResult
    func mock_reportsList() -> Self {
        server.addHandler("GET /carfax/bought-reports/raw *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "carfax_bought-reports_GET_ok")
        }
        return self
    }

    @discardableResult
    func mock_reportRaw(mutation: @escaping (inout Auto_Api_RawVinReportResponse) -> Void) -> Self {
        server.addHandler("GET /carfax/report/raw *") {
            let model = Auto_Api_RawVinReportResponse.with(mutation)
            return Response.okResponse(message: model, userAuthorized: true)
        }
        return self
    }

    @discardableResult
    func mock_reportRaw() -> Self {
        server.addHandler("GET /carfax/report/raw *") {
            let model = Auto_Api_RawVinReportResponse()
            return Response.okResponse(message: model, userAuthorized: true)
        }
        return self
    }
}
