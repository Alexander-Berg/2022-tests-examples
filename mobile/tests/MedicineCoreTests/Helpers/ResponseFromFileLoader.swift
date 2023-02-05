//
// Created by Dmitrii Chikovinskii on 15.11.2021.
//

import Foundation
@testable import MedicineCore

public final class ResponseFromFileLoader {
    public static let shared = ResponseFromFileLoader()
    
    private static let decoder = JSONDecoder()

    enum Filename: String {
        case taxonomyDoctors = "TaxonomyDoctors"
        case contractDetail = "ContractDetail"
        case activateContract = "ActivateContract"
    }

    static func load(_ filename: Filename) -> DataIncludedResponse {
        let bundle = Bundle.module
        let pathString = bundle.path(forResource: filename.rawValue, ofType: "json", inDirectory: "json")!
        let jsonString = try! String(contentsOfFile: pathString, encoding: .utf8)
        let jsonData = jsonString.data(using: .utf8)!
        return try! self.decoder.decode(DataIncludedResponse.self, from: jsonData)
    }
}
