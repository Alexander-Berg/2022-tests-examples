import Foundation
import Mediation
import Vapor

private struct EndpointRequest: Content {
    let tests: [String]
}

private struct EndpointResponse: Content {
    let status: ResponseCode
}

func retryTests(req: Request, testRunID: String) -> EventLoopFuture<Response> {
    do {
        let payload = try req.content.decode(EndpointRequest.self)
        try req.application.mediation?.retryTests(
            testRunID: testRunID,
            tests: payload.tests.map { TestCase($0) }
        )

        return EndpointResponse(status: .retryTestsOk)
            .encodeResponse(status: .ok, for: req)
    } catch DecodingError.keyNotFound(let codingKeys, _) {
        return ErrorResponse(status: .invalidParameter, message: "Invalid parameter '\(codingKeys.stringValue)'")
            .encodeResponse(status: .badRequest, for: req)
    } catch MediationState.Error.noActiveRunsForInitiator {
        return ErrorResponse(status: .invalidInitiator, message: "No runs for \(testRunID)")
            .encodeResponse(status: .badRequest, for: req)
    } catch {
        return ErrorResponse(status: .internalError, message: "Internal error: \(error)")
            .encodeResponse(status: .internalServerError, for: req)
    }
}
