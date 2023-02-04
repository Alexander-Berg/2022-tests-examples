//
//  GeoAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 12.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

final class GeoAPIStubConfigurator {
    static func setupRegionAutodetectWithRegion_MoscowAndMO(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/regionAutodetect.json", filename: "regionAutodetect-msk-mo.debug")
    }

    static func setupRegionAutodetectWithRegion_SpbLO(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/regionAutodetect.json", filename: "regionAutodetect-spb-lo.debug")
    }
}

// MARK: - Region Info

extension GeoAPIStubConfigurator {
    static func setupRegionInfo_Spb(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/getRegionInfoV15.json", filename: "getRegionInfo-spb.debug")
    }

    static func setupRegionInfo_SpbLO(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/getRegionInfoV15.json", filename: "getRegionInfo-spb-lo.debug")
    }
    
    static func setupRegionInfo_SpbLO_hasPaidSites(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/getRegionInfoV15.json", filename: "getRegionInfo-spb-lo-hasPaidSites.debug")
    }

    static func setupRegionInfo_Moscow(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/getRegionInfoV15.json", filename: "getRegionInfo-moscow.debug")
    }

    static func setupRegionInfo_MoscowAndMO(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/getRegionInfoV15.json", filename: "getRegionInfo-moscow-mo.debug")
    }

    static func setupRegionInfo_OrenburgRegion(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/getRegionInfoV15.json", filename: "getRegionInfo-orenburgRegion.debug")
    }
}

// MARK: - Region List

extension GeoAPIStubConfigurator {
    static func setupRegionListMoscowAndMO(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/regionList.json", filename: "regionList-moscowAndMO.debug")
    }

    static func setupRegionList_SpbAndLO(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/regionList.json", filename: "regionList-spb-lo.debug")
    }

    static func setupRegionList_OrenburgRegion(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/1.0/regionList.json", filename: "regionList-orenburgRegion.debug")
    }
}

// MARK: - Suggests

extension GeoAPIStubConfigurator {
    static func setupGeoSuggestList(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/2.0/suggest/geo", filename: "geoSuggest.debug")
    }

    static func setupGeoSuggestMoscowMetro(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "1.0/geosuggest.json", filename: "geosuggest-moscow-metro.debug")
    }

    static func setupGeoSuggestCommuteRed(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/2.0/suggest/geo", filename: "geoSuggest-commute-red.debug")
    }

    static func setupGeoSuggestCommuteEmpty(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "/2.0/suggest/geo", filename: "geoSuggest-commute-empty.debug")
    }

    static func setupAddressGeocoder(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(remotePath: "1.0/addressGeocoder.json", filename: "addressGeocoder.debug")
    }
}
