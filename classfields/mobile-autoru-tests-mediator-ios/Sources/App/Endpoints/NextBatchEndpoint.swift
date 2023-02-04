import Foundation
import Mediation
import Vapor

private struct EndpointRequest: Content {
    let buildId: TeamcityBuildID
}

private struct EndpointResponse: Content {
    let status: ResponseCode
    let batch: Batch?
}

func nextBatch(req: Request, testRunID: String) -> EventLoopFuture<Response> {
    do {
        let payload = try req.content.decode(EndpointRequest.self)

        let batch = try req.application.mediation?.getNextBatch(
            testRunID: testRunID,
            executorBuildID: payload.buildId
        )

        return EndpointResponse(
            status: batch == nil ? .nextBatchEmpty : .nextBatchOk,
            batch: batch.flatMap { .init(from: $0) }
        ).encodeResponse(status: .ok, for: req)
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
