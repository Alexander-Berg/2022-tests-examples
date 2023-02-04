import Foundation
import Vapor

enum ResponseCode: String, Content {
    case methodNotFound = "METHOD_NOT_FOUND"
    case internalError = "INTERNAL_ERROR"
    case invalidInitiator = "INVALID_INITIATOR"
    case invalidParameter = "INVALID_PARAMETER"
    case mediationRunOk = "MEDIATION_RUN_OK"
    case nextBatchOk = "NEXT_BATCH_OK"
    case nextBatchEmpty = "NEXT_BATCH_EMPTY"
    case completeBuildOk = "COMPLETE_BUILD_OK"
    case completeBatchOk = "COMPLETE_BATCH_OK"
    case retryTestsOk = "RETRY_TESTS_OK"
}

struct ErrorResponse: Content {
    let status: ResponseCode
    let message: String?
}
