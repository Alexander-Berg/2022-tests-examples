#include <infra/yasm/stockpile_client/state_ut.h>
#include <infra/yasm/stockpile_client/common/base_types_ut.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace NHistDb::NStockpile;
using namespace NHistDb::NStockpile::NTest;
using namespace NTags;
using namespace NZoom::NSignal;
using namespace NZoom::NHost;
using namespace NZoom::NValue;
using yandex::solomon::model::MetricType;
using NHistDb::NStockpile::NTest::TClient;

Y_UNIT_TEST_SUITE(TStockpileResolvingTest) {
    Y_UNIT_TEST(ResolveSensors) {
        TClient& client(TClient::Get());

        THostName groupName(TStringBuf("MAN.000"));
        THostName hostName(TStringBuf("resolve_host"));
        TInstanceKey instanceKey(TInstanceKey::FromNamed("yasm_unittest||ctype,geo,prj,tier").AddGroupAndHost(groupName, hostName));
        TSignalName firstSignal(TStringBuf("profile-ext_comtr_after_50ms_summ"));
        TSignalName secondSignal(TStringBuf("profile-ext_comtr_after_100ms_summ"));
        TSignalName hgramSignal(TStringBuf("profile-hgram_dhhh"));
        TSignalName ugramSignal(TStringBuf("profile-ugram_dhhh"));
        TSignalName minSignal(TStringBuf("profile-counter_min"));
        TSignalName maxSignal(TStringBuf("profile-counter_max"));
        TSignalName averageSignal(TStringBuf("counter-instance_tmmv"));
        TVector<TSignalName> signals{firstSignal, secondSignal, hgramSignal, ugramSignal, minSignal, maxSignal, averageSignal};

        const auto requestKey = TRequestKey::FromString(TString::Join("itype=", instanceKey.GetItype(), ";host=", hostName.GetName()));
        auto metabaseShardKey = TMetabaseShardKey::Make(hostName, requestKey, firstSignal, 1);

        auto firstKey = TSeriesKey::Make(instanceKey, firstSignal);
        auto secondKey = TSeriesKey::Make(instanceKey, secondSignal);
        auto hgramKey = TSeriesKey::Make(instanceKey, hgramSignal);
        auto ugramKey = TSeriesKey::Make(instanceKey, ugramSignal);
        auto minKey = TSeriesKey::Make(instanceKey, minSignal);
        auto maxKey = TSeriesKey::Make(instanceKey, maxSignal);
        auto averageKey = TSeriesKey::Make(instanceKey, averageSignal);

        TVector<TTypedSeriesKey> series{
            TTypedSeriesKey{firstKey, MetricType::DSUMMARY},
            TTypedSeriesKey{secondKey, MetricType::DSUMMARY},
            TTypedSeriesKey{hgramKey, MetricType::LOG_HISTOGRAM},
            TTypedSeriesKey{ugramKey, MetricType::HIST},
            TTypedSeriesKey{minKey, MetricType::DSUMMARY},
            TTypedSeriesKey{maxKey, MetricType::DSUMMARY},
            TTypedSeriesKey{averageKey, MetricType::DSUMMARY},
        };

        TGrpcStateHandler handler(client.Log);
        UNIT_ASSERT_STRINGS_EQUAL(TGrpcSettings::Get().GetSolomonClientId(), NTest::SOLOMON_TESTS_CLIENT_ID);
        auto shardState = client.StockpileState.ResolveOneMetabaseShardForWrite(metabaseShardKey);
        UNIT_ASSERT(shardState.IsReady());
        TMetabaseResolveSensor state(series, shardState.Shard, shardState.GrpcRemoteHost, handler.GetQueue(), client.Log);
        handler.Wait();
        UNIT_ASSERT(state.IsSuccess());

        client.Log << TLOG_INFO << "Resolved, processing result...";

        auto sensors(state.GetResult());
        UNIT_ASSERT_VALUES_EQUAL(sensors.size(), series.size());
        for (const auto& sensorId : state.GetResult()) {
            UNIT_ASSERT_VALUES_UNEQUAL(sensorId.Type, MetricType::METRIC_TYPE_UNSPECIFIED);
            auto stockpileShard = client.StockpileState.ResolveOneStockpileShardForWrite(sensorId.ShardId);
            UNIT_ASSERT(stockpileShard);
        }

        shardState = client.StockpileState.ResolveOneMetabaseShardForRead(metabaseShardKey);
        auto findState = TMetabaseFindSensor::Make(hostName, requestKey , signals, shardState.Shard, shardState.GrpcRemoteHost,
            client.Log);
        findState->ScheduleForExecute(handler.GetQueue());
        handler.Wait();
        UNIT_ASSERT(findState->IsSuccess());

        auto foundSeries = findState->GetResult();
        UNIT_ASSERT_VALUES_EQUAL(foundSeries.size(), series.size());
        for (const auto& keyWithSensorId : foundSeries) {
            UNIT_ASSERT_VALUES_EQUAL(keyWithSensorId.first.InstanceKey, instanceKey);
            UNIT_ASSERT(Find(signals.begin(), signals.end(), keyWithSensorId.first.SignalName) != signals.end());
        }
    }
}
