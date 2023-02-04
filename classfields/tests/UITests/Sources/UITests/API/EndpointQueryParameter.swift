import XCTest

protocol EndpointQueryParameter {
    var queryRepresentation: String { get }
}

enum EndpointQueryParametersMatching<Parameter: EndpointQueryParameter> {
    case wildcard
    case parameters([Parameter])

    func getStubServerFullURL(for baseURL: String) -> String {
        switch self {
        case .wildcard:
            return "\(baseURL) *"
        case .parameters(let params):
            let paramsString = params.map(\.queryRepresentation).joined(separator: "&")
            return paramsString.isEmpty ? baseURL : "\(baseURL)?\(paramsString)"
        }
    }
}

extension EndpointQueryParametersMatching: ExpressibleByArrayLiteral {
    init(arrayLiteral parameters: Parameter...) {
        self = .parameters(parameters)
    }
}
