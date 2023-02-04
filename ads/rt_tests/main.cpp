#include <ads/quality/adv_machine/lib/protobuf/pb_utils.h>
#include <ads/quality/adv_machine/lib/protocgi/cgi2proto.h>
#include <ads/quality/adv_machine/lib/request/extender/iface/iface.h>
#include <ads/quality/adv_machine/lib/request_bundle/bundle/reqbundle.h>
#include <ads/quality/adv_machine/lib/runtime/daemon/daemon.h>
#include <ads/quality/adv_machine/lib/runtime/meta/core/meta.h>
#include <ads/quality/adv_machine/lib/mkl/init.h>

#include <ads/quality/lib/extract_urls/extract_urls.h>

#include <kernel/yt/logging/log.h>

#include <library/cpp/accurate_accumulate/accurate_accumulate.h>
#include <library/cpp/charset/recyr.hh>
#include <library/cpp/containers/top_keeper/top_keeper.h>
#include <library/cpp/getopt/last_getopt.h>
#include <library/cpp/getopt/modchooser.h>
#include <library/cpp/json/json_prettifier.h>
#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/monlib/metrics/metric_registry.h>
#include <library/cpp/streams/factory/factory.h>

#include <util/folder/dirut.h>
#include <util/system/condvar.h>
#include <util/system/valgrind.h>
#include <util/thread/pool.h>

#undef RUNNING_ON_VALGRIND
#undef VALGRIND_STACK_REGISTER
#undef VALGRIND_STACK_DEREGISTER
#include <contrib/libs/valgrind/valgrind/callgrind.h>

using namespace NAdvMachine;

class TCandsConsumer final: public NRuntime::ICandidateConsumer {
public:
    TCandsConsumer(IOutputStream* stream = nullptr)
        : Output(stream)
    {}

    void ConsumeInfo(const NAdvMachine::TMetaErrorInfo&) override {
    }

    void ConsumeTimings(ui64 daemonMillis, ui64) override {
        DaemonTime = daemonMillis;
    }

    void Consume(NAdvMachine::TDaemonResponseCandidate&& cand) override {
        if (Output) {
            *Output << Default<NJson::TJsonPrettifier>().Prettify(NProtobufJson::Proto2Json(cand)) << "\n";
        }

        auto cpm = cand.GetTmpData(0);
        for (size_t i = 0; i < cand.MXResultSize(); ++i) {
            cpm *= cand.GetMXResult(i);
        }

        SumCPM += cpm;
    }

public:
    ui32 DaemonTime = 0;
    TKahanAccumulator<double> SumCPM;

private:
    IOutputStream* const Output;
};

static TString Percent(double a, double b) {
    const auto val = i32((a - b) / b * 10000.0) / 100.0;
    return " (" + TString(val > 0.0 ? "+" : "") + ToString(val) + "%)";
}

int main_shoot_stand(int argc, const char** argv) {
    NLastGetopt::TOpts options;

    TString requestsFile;
    options
        .AddCharOption('r', "--  requests.tsv.gz file")
        .Required()
        .RequiredArgument("PATH")
        .StoreResult(&requestsFile);

    TString daemonConfigFile;
    options
        .AddCharOption('c', "--  path to daemons config file")
        .Optional()
        .RequiredArgument("PATH")
        .StoreResult(&daemonConfigFile);

    TString metaConfigFile;
    options
        .AddLongOption("mc", "--  path to meta config file")
        .Required()
        .RequiredArgument("PATH")
        .StoreResult(&metaConfigFile);

    NAdvMachine::TTemplateParams templates;
    options
        .AddLongOption("params", "--  comma-separated k=v list for template config substitution, ex. 'key1=value,key2=value'")
        .Optional()
        .RequiredArgument("PARAMS")
        .StoreMappedResultT<TStringBuf>(&templates, [&] (TStringBuf val) {
            return ParseTemplateParams(val);
        });

    TString outputFile;
    options
        .AddCharOption('o', "--  daemon responses output file")
        .DefaultValue("/dev/null")
        .RequiredArgument()
        .StoreResult(&outputFile);

    ui32 queriesCount2Process;
    options
        .AddLongOption("max-queries", "--  queries count to process (input is repeated or cut to fit this value)")
        .Optional()
        .RequiredArgument("NUM")
        .DefaultValue("1000")
        .StoreResult(&queriesCount2Process);

    ui32 rpsMetaThreads;
    options
        .AddLongOption("rps", "--  calc rps for certain number of threads")
        .Required()
        .RequiredArgument("NUM")
        .StoreResult(&rpsMetaThreads);

    ui32 rpsRetries;
    options
        .AddLongOption("retries", "--  rps measure retries")
        .Optional()
        .RequiredArgument("NUM")
        .DefaultValue("10")
        .StoreResult(&rpsRetries);

    TString addCgi;
    options
        .AddLongOption("add-cgi", "-- add cgi")
        .Optional()
        .RequiredArgument("STRING")
        .StoreResult(&addCgi);

    ui32 topSlowestRequestsSize;
    options
        .AddLongOption("extract-slow-requests-limit", "--  size of slow requests to extract")
        .Optional()
        .RequiredArgument("NUM")
        .DefaultValue("100")
        .StoreResult(&topSlowestRequestsSize);

    double cpmBase = 0;
    options
        .AddLongOption("cpm-base", "--  cpm-base for metrics")
        .Optional()
        .RequiredArgument("FLOAT")
        .StoreResult(&cpmBase);

    double rpsBase = 0;
    options
        .AddLongOption("rps-base", "--  rps-base for metrics")
        .Optional()
        .RequiredArgument("FLOAT")
        .StoreResult(&rpsBase);

    bool atStartNo = false;
    options
        .AddLongOption("profile-skip-load", "--  for profiling. Enable valgrind after loading index. Use with valgrind --instr-atstart=no")
        .NoArgument()
        .StoreValue(&atStartNo, true);

    bool verbose = false;
    options
        .AddLongOption('v', "verbose", "enable debug output")
        .NoArgument()
        .StoreValue(&verbose, true);

    NLastGetopt::TOptsParseResult(&options, argc, argv);

    if (verbose) {
        NOxygen::TOxygenLogger::GetInstance().SetLogLevel(TLOG_DEBUG);
    }

    const THolder<IOutputStream> resultOutput = (outputFile == "/dev/null") ? nullptr : OpenOutput(outputFile);

    TAdvMachineParameters addParameters;
    addParameters.SetMinimal(false);

    Cerr << "Reading requests..." << Endl;
    TVector<TString> wizCgis(queriesCount2Process);
    {
        auto rIn = OpenMaybeCompressedInput(requestsFile);
        TString line;

        for (auto reqIdx = 0u; reqIdx < queriesCount2Process; ++reqIdx) {
            if (!rIn->ReadLine(line)) {
                Y_ENSURE(OpenMaybeCompressedInput(requestsFile)->ReadLine(line), "empty file?");

                CopyN(wizCgis.data(), queriesCount2Process - reqIdx, wizCgis.begin() + reqIdx);
                break;
            }

            wizCgis[reqIdx] = line;
        }
    }

    {
        Cerr << "Initializing..." << Endl;

        THolder<NAdvMachine::NRuntime::IAdvMachineDaemon> daemon;
        if (daemonConfigFile) {
            const auto daemonsConfig = ReadPbTemplateFromFile<TAdvMachineDaemonConfig>(daemonConfigFile, templates);
            daemon = NAdvMachine::NRuntime::MakeDaemon(daemonsConfig);
        }

        const auto meta = NAdvMachine::NRuntime::MakeMeta(
            ReadPbTemplateFromFile<TAdvMachineMetaConfig>(metaConfigFile, templates),
            MakeAtomicShared<NMonitoring::TMetricRegistry>(),
            MakeAtomicShared<NMonitoring::TMetricRegistry>());

        Cerr << "Processing..." << Endl;
        if (atStartNo) {
            Cerr << "Switching on valgrind!\n";
            CALLGRIND_START_INSTRUMENTATION;
        }

        TDeque<ui32> daemonTimings;
        TDeque<ui32> totalTimings;
        CopyCgiToProto(addCgi, addParameters);

        {
            const auto mtp = CreateThreadPool(rpsMetaThreads, 0);

            auto cpm = 0.0;
            TAdaptiveLock cpmLock;

            auto rps = 0.0;

            for (auto retry = 0u; retry < rpsRetries; ++retry) {
                mtp->Start(rpsMetaThreads, 0);
                const auto startTime = TInstant::Now();
                for (auto metaThread : xrange(rpsMetaThreads)) {
                    mtp->SafeAddFunc([&, metaThread] () {
                        TCandsConsumer consumer(resultOutput.Get());

                        ui32 localDaemonTimings[queriesCount2Process];
                        size_t localDaemonTimingsSize = 0;

                        ui32 localTotalTimings[queriesCount2Process];
                        size_t localTotalTimingsSize = 0;

                        TAdvMachineRequest request;
                        request.MutableParameters()->CopyFrom(addParameters);
                        for (const auto reqIdx : xrange(metaThread, queriesCount2Process, rpsMetaThreads)) {
                            request.ClearQueries();
                            CgiToProto(TCgiParameters(wizCgis[reqIdx]), *request.AddQueries());
                            const auto startTotal = TInstant::Now();
                            meta->FetchResults(request, consumer);
                            const auto duration = TInstant::Now() - startTotal;
                            localTotalTimings[localTotalTimingsSize++] = duration.MilliSeconds();
                            localDaemonTimings[localDaemonTimingsSize++] = consumer.DaemonTime;
                        }

                        const auto g = Guard(cpmLock);
                        cpm += consumer.SumCPM.Get();
                        CopyN(localDaemonTimings, localDaemonTimingsSize, std::back_inserter(daemonTimings));
                        CopyN(localTotalTimings, localTotalTimingsSize, std::back_inserter(totalTimings));
                    });
                }
                mtp->Stop();
                const auto timeTaken = (TInstant::Now() - startTime).MicroSeconds();
                rps = Max(rps, 1E6 * queriesCount2Process / static_cast<double>(timeTaken));
            }

            const auto reportTimings = [&] (auto& timings) {
                Sort(timings);
                if (timings) {
                    Cout << "Mean time per request (ms): " << (Accumulate(timings, double(0)) / timings.size()) << Endl;
                    Cout << "50\% time (ms): " << timings[(timings.size() - 1) * 0.5] << Endl;
                    Cout << "75\% time (ms): " << timings[(timings.size() - 1) * 0.75] << Endl;
                    Cout << "90\% time (ms): " << timings[(timings.size() - 1) * 0.9] << Endl;
                    Cout << "95\% time (ms): " << timings[(timings.size() - 1) * 0.95] << Endl;
                    Cout << "99\% time (ms): " << timings[(timings.size() - 1) * 0.99] << Endl;
                    Cout << "100\% time (ms): " << timings.back() << Endl;
                } else {
                    Cout << "Found no timings!" << Endl;
                }
            };

            Cout << "Total request time stats:\n";
            reportTimings(totalTimings);

            Cout << "Daemon time stats:\n";
            reportTimings(daemonTimings);

            Cout << "RPS for meta threads = " << rpsMetaThreads << ": " << rps << (rpsBase > 0 ? Percent(rps, rpsBase) : "") << Endl;
            Cout << "CPM: " << cpm << (cpmBase > 0 ? Percent(cpm, cpmBase) : "") << Endl;
        }

        if (atStartNo) {
            Cerr << "Switching off valgrind!\n";
            CALLGRIND_STOP_INSTRUMENTATION;
        }
    }

    return EXIT_SUCCESS;
}

int main(int argc, const char** argv)
{
    NAdvMachine::InitMklSingleThread();

    TModChooser modChooser;

    modChooser.AddMode(
        "shoot-stand",
        main_shoot_stand,
        "-- runs requests.tsv[.gz] file over meta + daemons"
    );

    return modChooser.Run(argc, argv);
}
