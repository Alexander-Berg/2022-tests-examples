import Foundation
import Mediation
import Vapor

final class MediationLogger: Mediation.Logger {
    private let logger: Logging.Logger

    init(logger: Logging.Logger) {
        self.logger = logger
    }

    func info(_ string: String) {
        logger.info(.init(stringLiteral: string))
    }

    func error(_ string: String) {
        logger.error(.init(stringLiteral: string))
    }
}
