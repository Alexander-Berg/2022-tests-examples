import Foundation
import Mediation
import Vapor

private struct EndpointResponse: Content {
    let status: ResponseCode
}

func completeBatch(
    req: Request,
    testRunID: String,
    batchID: String
) -> EventLoopFuture<Response> {
    do {
        try req.application.mediation?.completeBatch(testRunID: testRunID, batchID: batchID)

        return EndpointResponse(status: .completeBatchOk)
            .encodeResponse(status: .ok, for: req)
    } catch MediationState.Error.noActiveRunsForInitiator {
        return ErrorResponse(status: .invalidInitiator, message: "No runs for \(testRunID)")
            .encodeResponse(status: .badRequest, for: req)
    } catch MediationState.Error.uncompletedBatch {
        return ErrorResponse(status: .internalError, message: "Unable to complete batch \(batchID)")
            .encodeResponse(status: .badRequest, for: req)
    } catch {
        return ErrorResponse(status: .internalError, message: "Internal error: \(error)")
            .encodeResponse(status: .internalServerError, for: req)
    }
}
