#include <infra/libs/yp_updates_coordinator/client/client.h>
#include <infra/libs/yp_updates_coordinator/instance_state/state/state.h>
#include <infra/libs/logger/log_printer.h>

#include <infra/libs/logger/logger.h>

#include <library/cpp/getopt/small/modchooser.h>

#include <util/datetime/cputimer.h>

using namespace NYPUpdatesCoordinator;

int Run(int argc, const char* argv[]) {
    Y_UNUSED(argc, argv);

    NInfra::TLoggerConfig loggerConfig;
    loggerConfig.SetPath("current-eventlog");
    loggerConfig.SetLevel("DEBUG");
    NInfra::TLogger logger(loggerConfig);

    TClientOptions options;
    options.CypressRootPath = "//home/search-runtime/dima-zakharov/yp_updates_coordinator";
    options.ProviderAddress = "localhost:9091";
    options.Service = "test";
    options.YtProxy = "arnold";
    options.InstanceLocation = "sas";

    TClient client(options, logger.SpawnFrame());

    while (true) {
        try {
            client.SetCurrentState(TTimestampClientInfo(1337).ReceiveTime(Now()).UpdateTime(Now()), logger.SpawnFrame());
        } catch (...) {
            Cerr << "[1] " << CurrentExceptionMessage() << Endl;
            Sleep(TDuration::MilliSeconds(500));
            continue;
        }
        break;
    }

    Sleep(TDuration::Seconds(2));

    for (int i = 0; i < 10; ++i) {
        while (true) {
            NInfra::TLogFramePtr logFrame = logger.SpawnFrame();
            try {
                TTimer t("get target state + set current state: ");
                const ui64 result = client.GetTargetState(logFrame);
                Cerr << "new target timestamp: " << result << Endl;
                client.SetCurrentState(TTimestampClientInfo(result), logFrame);
            } catch (...) {
                Cerr << "[2] " << CurrentExceptionMessage() << Endl;
                Sleep(TDuration::MilliSeconds(500));
                continue;
            }
            break;
        }

        Sleep(TDuration::Seconds(5));
    }

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
