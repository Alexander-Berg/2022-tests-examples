#include <solomon/libs/cpp/clients/memstore/rpc.h>
#include <solomon/libs/cpp/kv/testlib/testlib.h>
#include <solomon/libs/cpp/slog/testlib/testlib.h>
#include <solomon/libs/cpp/stockpile_codec/metric_archive.h>
#include <solomon/libs/cpp/ts_model/iterator_decode.h>
#include <solomon/libs/cpp/ts_model/iterator_list.h>
#include <solomon/libs/cpp/ts_model/points.h>
#include <solomon/libs/cpp/ts_model/testlib/testlib.h>

#include <library/cpp/monlib/metrics/metric_registry.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/common/network.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <util/stream/buffered.h>
#include <util/stream/file.h>
#include <util/system/shellcommand.h>

using namespace NSolomon;
using namespace NSolomon::NTs;
using namespace NSolomon::NTsModel;
using namespace yandex::monitoring::memstore;

class MemStoreTest: public NSolomon::NKv::TTestFixtureTablet<> {
protected:
    void SetUp() override {
        TTestFixtureTablet::SetUp();

        GrpcPort_ = ::NTesting::GetFreePort();
        MonPort_ = ::NTesting::GetFreePort();

        TString name = ::testing::UnitTest::GetInstance()->current_test_info()->name();
        TString configPath = GetOutputPath() / (name + ".MemstoreConf");

        {
            auto config = TFileOutput{configPath};

            config << R"(client_id: "solomon-memstore-ut"
                         actor_system {
                             Executors: [
                                 { Name: "api", Threads: 1 },
                                 { Name: "mon", Threads: 1 },
                                 { Name: "wal", Threads: 1 },
                                 { Name: "index", Threads: 4 }
                             ]

                             LogConfig: {
                                 DefaultLevel: INFO
                                 Type: STDERR
                             }
                         }

                         grpc_server {
                             Port: )" << GrpcPort_ << R"(
                             MaxMessageSize { Value: 10, Unit: MEGABYTES }
                         }

                         index {
                             chunk_length { Value: 1, Unit: MINUTES }
                             index_capacity { Value: 5, Unit: MINUTES }
                             subshard_count: 32

                             main_pool: "index"
                             request_pool: "index"
                             fts_pool: "index"
                         }

                         cluster {
                             nodes [
                                 {
                                     host: "localhost"
                                     node_id: 0
                                 }
                             ]
                         }

                         volume_path: ")" << SolomonVolumePath << R"("

                         ydb_client {
                             GrpcConfig {
                         Addresses: ")" << Endpoint << R"("
                                 WorkerThreads: 2
                             }
                         }

                         mon_server {
                             Port: 4580
                             Bind: "::"
                             ThreadPoolName: "mon"
                         }

                         configs_puller {
                             local_shards_for_testing: [
                                 {
                                     num_id: 10
                                     project: "p_10"
                                     cluster: "c_10"
                                     service: "s_10"
                                 },
                                 {
                                     num_id: 11
                                     project: "p_11"
                                     cluster: "c_11"
                                     service: "s_11"
                                 }
                             ]
                         }

                         slicelet_config: {
                             local_slices_for_testing: {
                                 assigned_starts: [10, 11]
                                 assigned_ends: [10, 11]
                             }
                         }
             )";
        }

        TString path = BinaryPath("solomon/services/memstore/bin/memstore");

        MemStore_.Reset(new TShellCommand{
            path,
            TShellCommandOptions{}
                .SetOutputStream(&Cout)
                .SetErrorStream(&Cerr)
                .SetAsync(true)});
        *MemStore_ << "--config" << configPath;
        MemStore_->Run();
    }

    void TearDown() override {
        if (MemStore_->GetStatus() != TShellCommand::SHELL_RUNNING) {
            ADD_FAILURE() << "memstore failed with code " << MemStore_->GetExitCode().GetRef();
        }

        MemStore_->Terminate();

        TTestFixtureTablet::TearDown();
    }

protected:
    THolder<TShellCommand> MemStore_;
    ::NTesting::TPortHolder GrpcPort_;
    ::NTesting::TPortHolder MonPort_;
    NMonitoring::TMetricRegistry Registry_;
};

TEST_F(MemStoreTest, Test) {
    yandex::solomon::config::rpc::TGrpcClientConfig conf;
    *conf.add_addresses() = TStringBuilder() << "localhost:" << GrpcPort_;
    auto client = NMemStore::CreateNodeGrpc(conf, Registry_);

    for (size_t i = 0; i < 10; ++i) {
        auto res = client->ListShards(ListShardsRequest{}).ExtractValueSync();
        if (res.Success()) {
            break;
        } else {
            Cerr << "failed to connect to memstore: " << res.Error().Msg << Endl;
            Sleep(TDuration::MilliSeconds(250));
        }
    }

    Cout << "memstore connected";

    {
        auto res = client->ListShards(ListShardsRequest{}).ExtractValueSync();
        if (res.Success()) {
            EXPECT_EQ(res.Value().num_ids_size(), 2);
        } else {
            ADD_FAILURE() << res.Error().Msg;
        }
    }

    {
        auto [meta, data] = NSolomon::NSlog::MakeSlog(10)
            .IGauge({{"sensor", "index.mem"}})
                .Add("2020-01-01T00:00:00Z", 100)
                .Add("2020-01-01T00:00:10Z", 200)
                .Add("2020-01-01T00:00:20Z", 300)
                .Done()
            .Gauge({{"sensor", "index.cpu"}})
                .Add("2020-01-01T00:00:00Z", 0.3)
                .Add("2020-01-01T00:00:10Z", 0.4)
                .Add("2020-01-01T00:00:20Z", 0.2)
                .Done()
            .Rate({{"sensor", "api.rps"}})
                .Add("2020-01-01T00:00:00Z", 5)
                .Add("2020-01-01T00:00:10Z", 7)
                .Add("2020-01-01T00:00:20Z", 8)
                .Done()
            .Done();

        WriteRequest req;
        req.set_num_id(10);
        req.set_metadata(meta);
        req.set_data(data);

        auto res = client->Write(req).ExtractValueSync();
        EXPECT_TRUE(res.Fail());
        EXPECT_EQ(grpc::StatusCode::UNAVAILABLE, res.Error().GRpcStatusCode);

        res = client->Write(req).ExtractValueSync();
        if (!res.Success()) {
            ADD_FAILURE() << res.Error().Msg;
        }
    }

    {
        auto [meta, data] = NSolomon::NSlog::MakeSlog(11)
            .IGauge({{"sensor", "inflight"}})
                .Add("2020-01-01T00:00:00Z", 5)
                .Done()
            .Rate({{"sensor", "rps"}})
                .Add("2020-01-01T00:00:00Z", 5)
                .Done()
            .Done();

        WriteRequest req;
        req.set_num_id(11);
        req.set_metadata(meta);
        req.set_data(data);

        auto res = client->Write(req).ExtractValueSync();
        if (!res.Success()) {
            ADD_FAILURE() << res.Error().Msg;
        }
    }

    // there is some background processes inside shards, so we have to wait some time
    // before reading data back
    Sleep(TDuration::Seconds(15));

    {
        auto res = client->ListShards(ListShardsRequest{}).ExtractValueSync();
        if (res.Success()) {
            EXPECT_THAT(res.Value().num_ids(), testing::ElementsAreArray({10, 11}));
        } else {
            ADD_FAILURE() << res.Error().Msg;
        }
    }

    {
        FindRequest req;
        req.set_num_id(10);

        auto res = client->Find(req).ExtractValueSync();
        if (res.Success()) {
            auto& val = res.Value();
            EXPECT_EQ(val.total_count(), 3u);
            EXPECT_FALSE(val.truncated());
        } else {
            ADD_FAILURE() << res.Error().Msg;
        }
    }

    {
        ReadOneRequest req;
        req.set_num_id(10);
        req.set_max_timeseries_format(static_cast<ui32>(NStockpile::EFormat::DELETED_SHARDS_39));
        req.add_labels("sensor");
        req.add_labels("index.mem");
        req.set_from_millis(TInstant::ParseIso8601("2020-01-01T00:00:00Z").MilliSeconds());
        req.set_to_millis(TInstant::ParseIso8601("2020-01-01T00:10:00Z").MilliSeconds());

        auto res = client->ReadOne(req).ExtractValueSync();
        if (res.Success()) {
            auto& val = res.Value();
            EXPECT_EQ(val.type(), yandex::solomon::model::MetricType::IGAUGE);
            EXPECT_EQ(val.time_series().format_version(), 39u);
            EXPECT_EQ(val.time_series().chunks_size(), 1);
            if (val.time_series().chunks_size() == 1) {
                auto& chunk = val.time_series().chunks(0);
                EXPECT_EQ(chunk.point_count(), 3u);

                NStockpile::TMetricArchiveCodec codec{NStockpile::FormatFromInt(val.time_series().format_version())};
                auto input = NStockpile::TCodecInput{chunk.content()};
                auto data = codec.Decode(&input);
                EXPECT_EQ(data.Header().Owner.ProjectId, 0u);
                EXPECT_EQ(data.Header().Owner.ShardId, 10u);
                EXPECT_EQ(data.Header().Type, yandex::solomon::model::MetricType::IGAUGE);
                EXPECT_EQ(data.Columns(), TIGaugePoint::AggrColumns);
                auto decoder = TDecodeIterator<TIGaugePoint>::Aggr(data.Data());
                ExpectIteratorsEq(std::move(decoder), TListIterator<TIGaugePoint>{{
                    IGauge("2020-01-01T00:00:00Z", 100),
                    IGauge("2020-01-01T00:00:10Z", 200),
                    IGauge("2020-01-01T00:00:20Z", 300)
                }});
            }
        } else {
            ADD_FAILURE() << res.Error().Msg;
        }
    }
}
