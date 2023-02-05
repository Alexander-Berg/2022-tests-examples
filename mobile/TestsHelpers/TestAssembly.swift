import BeruCore
import DI
import Foundation

final class TestAssembly: Assembly {

    override init() {
        super.init()

        register(lifetime: .singleton(lazy: true), initCall: { TestSettings() })
        register(lifetime: .singleton(lazy: true), initCall: { TestExperminetsHelper() }, injectCall: { obj in
            obj.updateDaemon = DeferredObject<AppConfigurationUpdateDaemon> {
                let assembly = AssemblyActivator.shared.resolve() as DaemonsAssembly
                return assembly.resolve() as AppConfigurationUpdateDaemon
            }
        })
    }
}
