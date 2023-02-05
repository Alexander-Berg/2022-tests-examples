#include <maps/wikimap/infopoints_hypgen/libs/lb_client/include/infopoint_collector.h>

#include <ydb/core/protos/msgbus.pb.h>

#include <library/cpp/getopt/opt.h>

#include <util/datetime/base.h>
#include <util/string/join.h>
#include <util/string/vector.h>

#include <google/protobuf/message.h>

#include <kikimr/persqueue/sdk/deprecated/cpp/v2/persqueue.h>

#include <maps/libs/log8/include/log8.h>

using namespace NKikimrClient;
using namespace NLastGetopt;
using namespace NPersQueue;

namespace lb_client = maps::wiki::infopoints_hypgen::lb_client;

void SigHandler(int) {
    lb_client::InfopointCollector::stop();
}

int main(int argc, const char* argv[]) {
    signal(SIGTERM, &SigHandler);
    signal(SIGINT, &SigHandler);

    ui32 rate = 0;
    bool verbose = false;
    size_t threadCount = 1;

    ui32 tvmId = 0;
    TString tvmSecret;

    TConsumerSettings settings;
    settings.UseLockSession = true;
    settings.ReadMirroredPartitions = false;
    settings.MaxCount = 0; //unbounded
    settings.MaxSize = 150 << 20;
    settings.Unpack = true;
    settings.MaxUncommittedCount = 0;

    {
        TOpts opts;
        opts.AddHelpOption('h');
        opts.AddLongOption('s', "server", "server addr")
            .StoreResult(&settings.Server.Address)
            .Required();
        opts.AddLongOption('p', "port", "server port")
            .StoreResult(&settings.Server.Port)
            .DefaultValue(2135);
        opts.AddLongOption('c', "client", "clientId")
            .StoreResult(&settings.ClientId)
            .Required();
        opts.AddLongOption('t', "topic", "topics to read from, comma-separated")
            .SplitHandler(&settings.Topics, ',')
            .Required();
        opts.AddLongOption('g', "group", "groups to read from, comma-separated")
            .Handler1T<TString>([&] (const TString& val) {
                TVector<TString> groups = SplitString(val, ",");
                for (const auto g: groups) {
                    settings.PartitionGroups.push_back(FromString<ui32>(g));
                }
            });
        opts.AddLongOption('r', "commit-rate", "rate of commit")
            .StoreResult(&rate)
            .Required();
        opts.AddLongOption('i', "inflight", "inflight size in bytes")
            .StoreResult(&settings.MaxMemoryUsage)
            .Required();
        opts.AddLongOption('n', "inflight2", "inflight in requests")
            .StoreResult(&settings.MaxInflyRequests)
            .Required();
        opts.AddLongOption('v', "verbose", "verbose")
            .StoreResult(&verbose)
            .DefaultValue("no");

        opts.AddLongOption(0, "tvm-client-id", "tvm client id")
            .StoreResult(&tvmId)
            .Required();
        opts.AddLongOption(0, "tvm-secret", "tvm client id")
            .StoreResult(&tvmSecret)
            .Required();

        opts.AddLongOption(0, "threads", "number of threads")
            .StoreResult(&threadCount)
            .DefaultValue(1);


        TOptsParseResult res(&opts, argc, argv);
    }

    if (verbose) {
        maps::log8::setLevel(maps::log8::Level::DEBUG);
    }

    TCerrLogger logger(6);


    ///---------------------
    // patch for https://a.yandex-team.ru/arc/trunk/arcadia/kikimr/persqueue/sdk/deprecated/cpp/v2/samples/consumer/main.cpp

    std::shared_ptr<NTvmAuth::TTvmClient> tvmClient;

    // pkostikov: strange parameters, I don't know what it is
    ui32 dstClientId = 2001059;
    TString alias = "MyAlias";

    NTvmAuth::NTvmApi::TClientSettings tvmSettings;
    tvmSettings.SetSelfTvmId(tvmId);
    tvmSettings.EnableServiceTicketsFetchOptions(tvmSecret, {{alias, dstClientId}});
    auto tvmLogger = MakeIntrusive<NTvmAuth::TCerrLogger>(7);

    //you can use this tmvClient for getting tickets for other services, not just LB
    tvmClient = std::make_shared<NTvmAuth::TTvmClient>(tvmSettings, tvmLogger);
    settings.CredentialsProvider = CreateTVMCredentialsProvider(tvmClient, &logger, alias);

    //or you can use this if you need tickets only for LB:
    //settings.CredentialsProvider = CreateTVMCredentailsProvider(tvmSecret, tvmId, dstClientId, logger);

    ///---------------------

    lb_client::InfopointCollector collector(
        [](const std::vector<lb_client::Infopoint>& parsed) {
            if (parsed.empty()) {
                return;
            }

            DEBUG() << "Callback called";
            DEBUG() << "Messages of interest count: " << parsed.size();
            for (const auto& infopoint : parsed) {
                DEBUG() << "uuid=" << infopoint.uuid() <<
                    " val=" << infopoint.eventValue() <<
                    " ts=" << infopoint.timestamp();
            }
        },
        settings,
        &logger,
        rate
    );

    collector.runMultithreaded(threadCount);
}
