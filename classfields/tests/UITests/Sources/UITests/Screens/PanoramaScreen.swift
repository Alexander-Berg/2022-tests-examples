//
//  PanoramaScreen.swift
//  UITests
//
//  Created by Dmitry Sinev on 8/4/20.
//

class PanoramaScreen: BaseScreen {
    lazy var bigCloseHelpButton = find(by: "bigCloseHelpButton").firstMatch
    lazy var recordPanoramaButton = find(by: "recordPanoramaButton").firstMatch
}
