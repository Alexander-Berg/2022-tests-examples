import BeruCore

final class TestExperminetsHelper: NSObject, AppConfigurationUpdateDaemonObserver {

    var updateDaemon: DeferredObject<AppConfigurationUpdateDaemon>?

    private var callbacks: [() -> Void] = []

    // MARK: - Public

    @objc func waitForExperiments(completion: @escaping () -> Void) {
        if updateDaemon?.object.lastUpdateDate != nil {
            return completion()
        }

        callbacks.append(completion)

        if callbacks.count == 1 {
            updateDaemon?.object.observers.append(WeakStorage(self))
        }
    }

    // MARK: - AppConfigurationUpdateDaemonObserver

    func daemonDidUpdated(_ daemon: AppConfigurationUpdateDaemon) {
        DispatchQueue.main.async {
            self.callbacks.forEach { $0() }
            self.callbacks.removeAll()

            if let daemon = self.updateDaemon?.object {
                daemon.observers = daemon.observers.filter { $0.getObject() !== self }
            }
        }
    }
}
