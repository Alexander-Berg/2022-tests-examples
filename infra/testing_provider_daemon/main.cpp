#include <infra/libs/yp_updates_coordinator/service/protos/config/config.pb.h>
#include <infra/libs/yp_updates_coordinator/service/service.h>

#include <infra/libs/logger/log_printer.h>

#include <library/cpp/getopt/small/modchooser.h>
#include <library/cpp/proto_config/config.h>
#include <library/cpp/proto_config/load.h>

#include <library/cpp/resource/resource.h>

using namespace NYPUpdatesCoordinator;

int Run(int argc, const char* argv[]) {
    const TServiceConfig config = NProtoConfig::GetOpt<TServiceConfig>(argc, argv, "/provider_daemon/proto_config.json");

    TService service(config);
    service.Start();
    service.Wait();

    return EXIT_SUCCESS;
}

int main(int argc, const char* argv[]) {
    TModChooser modChooser;

    modChooser.AddMode(
        "run",
        Run,
        "Run"
    );

    modChooser.AddMode(
        "print_log",
        NInfra::PrintEventLog,
        "Print log"
    );

    try {
        return modChooser.Run(argc, argv);
    } catch (...) {
        Cerr << CurrentExceptionMessage() << Endl;
        return EXIT_FAILURE;
    }

    return EXIT_SUCCESS;
}
