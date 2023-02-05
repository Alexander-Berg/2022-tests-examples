import MarketFlexActions

class ActionDecodingLoggerStub: ActionDecodingLogger {

    var loggedUnsupportedActions: [String] = []

    var loggedCorruptedActions: [(type: String?, error: Error)] = []

    func logUnsupportedAction(type: String) {
        loggedUnsupportedActions.append(type)
    }

    func logCorruptedAction(type: String?, error: Error) {
        loggedCorruptedActions.append((type, error))
    }
}
