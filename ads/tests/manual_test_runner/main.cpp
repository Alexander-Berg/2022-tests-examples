#include <ads/bsyeti/libs/backtrace/backtrace.h>
#include <ads/bsyeti/libs/yt_storage/tests/manual_test/test.h>

#include <yt/yt/core/logging/log_manager.h>

#include <library/cpp/getopt/last_getopt.h>

using namespace NYTRpc;

void Run(int argc, const char** argv) {
    NYT::NLogging::TLogManager::Get()->ConfigureFromEnv();
    NLastGetopt::TOpts options;
    TString master;
    options.AddLongOption("master", "-- master")
        .Required()
        .StoreResult(&master);
    TString rawReplicas;
    options.AddLongOption("replicas", "-- replicas")
        .Required()
        .StoreResult(&rawReplicas);

    TString path;
    options.AddLongOption("table", "-- table")
        .Required()
        .StoreResult(&path);

    TString counters;
    options.AddLongOption("counters", "-- counters path")
        .Required()
        .StoreResult(&counters);

    NLastGetopt::TOptsParseResult opts(&options, argc, argv);

    TVector<TString> replicas;
    Split(rawReplicas, ",", replicas);
    auto client = CreateYTMasterClient(
        replicas,
        master
    );

    ManualTest(path, counters, client);
}

int main(int argc, const char** argv) {
    int code = 0;
    try {
        Run(argc, argv);
    } catch (...) {
        ERROR_LOG << NBSYeti::CurrentExceptionWithBacktrace();
        code = 1;
    }
    NYT::Shutdown();
    INFO_LOG << "Finished\n";
    return code;
}
