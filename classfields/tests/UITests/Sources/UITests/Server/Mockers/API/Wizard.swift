//
//  Wizard.swift
//  UITests
//
//  Created by Dmitry Sinev on 7/15/21.
//

import Foundation
import AutoRuProtoModels
import SwiftProtobuf

struct OfferParameters {
     var id: String?
     var category: Auto_Api_Category?
     var status: Auto_Api_OfferStatus?
     var nds: Bool?
     var description: String?
     var isBroken: Bool?
 }

extension Mocker {
    @discardableResult
    func mock_wizardDraftCars(id: String? = nil, isPartial: Bool = false, isPublished: Bool = false, nds: Bool = false, description: String? = nil, processingOffer: Bool = false, isBroken: Bool? = nil) -> Self {
        var postfix: String
        if let id = id {
            postfix = "/\(id)"
        } else {
            postfix = " *"
        }
        if isPublished {
            server.addHandler("POST /user/draft/CARS\(postfix)/publish?from=wizard") {
                var draftResponse = .init(mockFile: "POST user_draft_partial_wizard_publish") as Auto_Api_DraftResponse
                let params = OfferParameters(id: id, nds: nds, description: description, isBroken: isBroken)
                mock_fillOfferWithParameters(offer: &draftResponse.offer, params: params)
                return Response.responseWithStatus(body: try! draftResponse.jsonUTF8Data(), userAuthorized: false)
            }
        } else {
            server.addHandler("GET /user/draft/CARS\(postfix)") {
                var draftResponse = .init(mockFile: isPartial ? "GET user_draft_partial_wizard" : "GET user_draft_empty_wizard") as Auto_Api_DraftResponse
                let params = OfferParameters(nds: nds, description: description, isBroken: isBroken)
                mock_fillOfferWithParameters(offer: &draftResponse.offer, params: params)
                if processingOffer {
                    draftResponse.offer.state.externalPanorama.next.id = "panoramaID"
                    draftResponse.offer.state.externalPanorama.next.status = .processing
                }
                return Response.responseWithStatus(body: try! draftResponse.jsonUTF8Data(), userAuthorized: false)
            }
        }
        return self
    }

    @discardableResult
    func mock_wizardPUTEmptyDraftCars(id: String) -> Self {
        server.addHandler("PUT /user/draft/CARS/\(id)") { (request, index) -> Response? in
            var draftResponse = .init(mockFile: "GET user_draft_empty_wizard") as Auto_Api_DraftResponse
            draftResponse.offer.state.uploadURL = "http://127.0.0.1:\(port)/upload?sign=eyJhb"
            if index == 2 {
                draftResponse.offer.additionalInfo.recognizedLicensePlate = "a111aa111"
            }
            return Response.responseWithStatus(body: try! draftResponse.jsonUTF8Data(), userAuthorized: false)
        }
        return self
    }

    @discardableResult
    func mock_putDraftCars(id: String) -> Self {
        server.addHandler("PUT /user/draft/CARS/\(id)") {
            .init(mockFile: "GET user_draft_partial_wizard") as Auto_Api_DraftResponse
        }
        return self
    }

    @discardableResult
    func mock_postDraftCarsPublish(id: String) -> Self {
        server.addHandler("POST /user/draft/CARS/\(id)/publish") {
            .init(mockFile: "POST user_draft_partial_wizard_publish") as Auto_Api_DraftResponse
        }
        return self
    }

    @discardableResult
    func mock_wizardReferenceCatalogCars() -> Self {
        server.addHandler("GET /reference/catalog/cars/all-options") {
            .init(mockFile: "GET reference_catalog_cars_all-options") as Auto_Searcher_Filters_EquipmentFiltersResultMessage
        }
        return self
    }

    @discardableResult
    func mock_wizardReferenceCatalogCarsSuggest() -> Self {
        server.addHandler("GET /reference/catalog/CARS/suggest *") { (request, index) -> Response? in
            if request.uri.contains("?body_type=LIFTBACK&engine_type=GASOLINE&gear_type=ALL_WHEEL_DRIVE&mark=BMW&model=6ER&super_gen=22248286&transmission=AUTOMATIC&year=2020") {
                return Response.okResponse(fileName: "catalog_CARS_suggest_BMW_6ER_22248286_LIFTBACK_GASOLINE_ALL_WHEEL_DRIVE_AUTOMATIC", userAuthorized: false)
            } else if request.uri.contains("?body_type=LIFTBACK&engine_type=GASOLINE&gear_type=ALL_WHEEL_DRIVE&mark=BMW&model=6ER&super_gen=22248286&year=2020") {
                return Response.okResponse(fileName: "catalog_CARS_suggest_BMW_6ER_22248286_LIFTBACK_GASOLINE_ALL_WHEEL_DRIVE", userAuthorized: false)
            } else if request.uri.contains("?body_type=LIFTBACK&engine_type=GASOLINE&mark=BMW&model=6ER&super_gen=22248286&year=2020") {
                return Response.okResponse(fileName: "catalog_CARS_suggest_BMW_6ER_22248286_LIFTBACK_GASOLINE", userAuthorized: false)
            } else if request.uri.contains("?body_type=LIFTBACK&mark=BMW&model=6ER&super_gen=22248286&year=2020") {
                return Response.okResponse(fileName: "catalog_CARS_suggest_BMW_6ER_22248286_LIFTBACK", userAuthorized: false)
            } else if request.uri.contains("?mark=BMW&model=6ER&super_gen=22248286&year=2020") {
                return Response.okResponse(fileName: "catalog_CARS_suggest_BMW_6ER_22248286", userAuthorized: false)
            } else if request.uri.contains("?mark=BMW&model=6ER&year=2020") {
                return Response.okResponse(fileName: "catalog_CARS_suggest_BMW_6ER_2020", userAuthorized: false)
            } else if request.uri.contains("?mark=BMW&model=6ER") {
                return Response.okResponse(fileName: "catalog_CARS_suggest_BMW_6ER", userAuthorized: false)
            } else if request.uri.contains("?mark=BMW") {
                return Response.okResponse(fileName: "catalog_CARS_suggest_BMW", userAuthorized: false)
            } else {
                return Response.okResponse(fileName: "catalog_CARS_suggest_ok", userAuthorized: false)
            }
        }
        return self
    }

    @discardableResult
    func mock_wizardPhotoUpload() -> Self {
        server.addHandler("POST /upload?sign=eyJhb") {
            return Response.okResponse(fileName: "upload_photo_ok", userAuthorized: false)
        }
        return self
    }

    @discardableResult
    func mock_wizardUserAuthData() -> Self {
        let profileData: Data = {
            let userProfile: Auto_Api_UserResponse = {
                var profile = Auto_Api_UserResponse()
                profile.user.id = "112231"
                profile.user.profile.autoru.about = ""
                return profile
            }()
            return try! userProfile.jsonUTF8Data()
        }()
        server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            Response.responseWithStatus(body: profileData, userAuthorized: true)
        }
        return self
    }

    func mock_fillOfferWithParameters(
             offer: inout Auto_Api_Offer,
             params: OfferParameters) {
                 if let id = params.id {
                     offer.id = id
                 }
                 if let status = params.status {
                     offer.status = status
                 }
                 if let category = params.category {
                     offer.category = category
                 }
                 if let nds = params.nds {
                     offer.priceInfo.withNds.value = nds
                 }
                 if let desc = params.description {
                     offer.description_p = desc
                 }
                 if let isBroken = params.isBroken {
                     offer.state.condition = isBroken ? .broken : .ok
                 }
    }
}
