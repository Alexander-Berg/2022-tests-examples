#include <infra/yasm/stockpile_client/state_ut.h>
#include <infra/yasm/stockpile_client/stockpile_client.h>
#include <infra/yasm/stockpile_client/points.h>
#include <infra/yasm/stockpile_client/common/base_types_ut.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/random/random.h>

using namespace NHistDb::NStockpile;
using namespace NTags;
using namespace NZoom::NSignal;
using namespace NZoom::NHost;
using namespace NZoom::NValue;
using namespace NZoom::NAccumulators;
using yandex::solomon::model::MetricType;
using NHistDb::NStockpile::NTest::TClient;

namespace {
    static constexpr TDuration RESOLUTION = TDuration::Seconds(5);

    class TMockedStockpileDumperStats: public IStockpileClientStats {
        void OnRecordsError(ui64 count) override {
            Y_UNUSED(count);
        };

        void OnRecordsRejected(ui64 count) override {
            Y_UNUSED(count);
        };

        void OnRecordsEmpty(ui64 count) override {
            Y_UNUSED(count);
        };
    };

    TInstant AdjustTime(TInstant time) {
        return time - TDuration::FromValue(time.GetValue() % RESOLUTION.GetValue());
    }

    TInstant AdjustTimeToHours(TInstant time) {
        return time - TDuration::FromValue(time.GetValue() % TInstant::Hours(1).GetValue());
    }

    class TSensorValuesAccumulator final : public ISensorValuesVisitor {
    public:
        using TResult = TVector<std::pair<TInstant, TValue>>;

        TSensorValuesAccumulator(const TSensorId& sensorId)
            : ExpectedSensorId(sensorId)
            , Visited(false)
        {
        }

        void OnSensorId(const TSensorId& sensorId) override {
            if (ExpectedSensorId.ShardId != sensorId.ShardId || ExpectedSensorId.LocalId != sensorId.LocalId) {
                ythrow yexception() << "wrong sensor given";
            }
            Visited = true;
        }

        void OnValue(TInstant timestamp, NZoom::NValue::TValue value) override {
            Values.emplace_back(timestamp, std::move(value));
        }

        TResult GetResult() {
            if (!Visited) {
                ythrow yexception() << "sensor not found";
            }
            return std::move(Values);
        }

    private:
        TSensorId ExpectedSensorId;
        bool Visited;
        TResult Values;
    };

    struct TReadWriteClient {
        TReadWriteClient()
            : Client(TClient::Get())
            , Handler(Client.Log)
        {
        }

        static TString SimpleLogFormatter(ELogPriority, TStringBuf data) {
            TStringBuilder result;
            result << TInstant::Now().ToStringLocalUpToSeconds() << " ";
            return result << data << Endl;
        }

        static TLog CreateLog() {
            TLog log;
            log.ResetBackend(MakeHolder<TStreamLogBackend>(&Cerr));
            log.SetFormatter(SimpleLogFormatter);
            return log;
        }

        TMaybe<TSensorId> ResolveSensor(const TMetabaseShardKey& metabaseShardKey, const TSeriesKey& seriesKey, MetricType type) {
            const auto shardState = Client.StockpileState.ResolveOneMetabaseShardForWrite(metabaseShardKey);
            UNIT_ASSERT(shardState.IsReady());
            TMetabaseResolveSensor sensorState(
                TVector<TTypedSeriesKey>{TTypedSeriesKey{seriesKey, type}},
                shardState.Shard,
                shardState.GrpcRemoteHost,
                Handler.GetQueue(),
                Client.Log
            );
            Handler.Wait();
            UNIT_ASSERT(sensorState.IsSuccess());
            return sensorState.GetResult().at(0);
        }

        TNumId ResolveNumId(const TMetabaseShardKey& metabaseShardKey) {
            const auto shardState = Client.StockpileState.ResolveOneMetabaseShardForWrite(metabaseShardKey);
            UNIT_ASSERT(shardState.IsReady());
            return shardState.Shard->GetShardNumId();
        }

        void WritePoints(
                TSensorId sensorId,
                TNumId numId,
                EAccumulatorType aggregationType,
                const TVector<std::pair<TInstant, TValueRef>>& points,
                TMaybe<NZoom::NHgram::TUgramBucketsFreezer> freezer = Nothing()
        ) {
            TMockedStockpileDumperStats stats{};
            TStockpileWriteManyState writeState(
                Client.StockpileState.ResolveOneStockpileShardForWrite(sensorId.ShardId),
                Client.Log,
                stats
            );
            UNIT_ASSERT(writeState.Empty());
            TRecordSerializeState recordSerializeState {sensorId, numId, aggregationType};
            if (freezer.Defined()) {
                recordSerializeState.SetUgramFreezer(std::move(freezer));
            }
            writeState.StartRecord(std::move(recordSerializeState));
            for (const auto & [ timestamp, value ] : points) {
                writeState.AddPoint(timestamp, value);
            }
            writeState.FinishRecord();
            writeState.FinishBuild();
            UNIT_ASSERT(!writeState.Empty());
            writeState.ScheduleForExecute(Handler.GetQueue());
            Handler.Wait();
            UNIT_ASSERT(writeState.IsSuccess());
        }

        TSensorValuesAccumulator::TResult ReadPoints(
                TSensorId sensorId,
                EAccumulatorType aggregationType,
                TInstant start,
                TInstant end,
                TMaybe<TStockpileDownsamplingOptions> downsampling = Nothing()
        ) {
            TStockpileReadOptions options;
            options.Sensors.emplace_back(sensorId);
            options.SensorTypes.emplace_back(aggregationType);
            options.Start = start;
            options.End = end;
            options.Downsampling = downsampling;
            TStockpileReadManyState readState(
                options,
                Client.StockpileState.ResolveOneStockpileShardForRead(sensorId.ShardId),
                Client.Log
            );
            readState.ScheduleForExecute(Handler.GetQueue());
            Handler.Wait();
            UNIT_ASSERT(readState.IsSuccess());
            TSensorValuesAccumulator visitor(sensorId);
            readState.VisitResult(visitor);
            return visitor.GetResult();
        }

        TClient& Client;
        TGrpcStateHandler Handler;
    };

    class TValueHolder {
    public:
        TValueHolder(EAccumulatorType aggregationType)
            : AggregationType(aggregationType)
        {
        }

        TVector<std::pair<TInstant, TValueRef>> Get() const {
            TVector<std::pair<TInstant, TValueRef>> points;
            points.reserve(Storage.size());
            for (const auto & [ timestamp, value ] : Storage) {
                points.emplace_back(timestamp, value.GetValue());
            }
            return points;
        }

        const std::pair<TInstant, TValue>& Get(size_t idx) const {
            return Storage.at(idx);
        }

        template <typename... Args>
        void Add(TInstant timestamp, Args&&... args) {
            Storage.emplace_back(timestamp, std::forward<Args>(args)...);
        }

        size_t Size() const {
            return Storage.size();
        }

        MetricType GetKind() const {
            TValueTypeDetector detector(AggregationType);
            for (const auto & [ timestamp, value ] : Storage) {
                detector.OnValue(value.GetValue());
            }
            return detector.GetType();
        }

    private:
        TVector<std::pair<TInstant, TValue>> Storage;
        EAccumulatorType AggregationType;
    };
}

Y_UNIT_TEST_SUITE(TStockpileReadingTest) {
    Y_UNIT_TEST(WriteReadSensors) {
        TReadWriteClient client;
        THostName groupName(TStringBuf("MAN.000"));
        THostName hostName(TStringBuf("man1-3578.search.yandex.net"));
        TInstanceKey instanceKey(TInstanceKey::FromNamed("yasm_unittest||ctype,geo,prj,tier").AddGroupAndHost(groupName, hostName));
        TSignalName signal(TStringBuf("signal_summ"));
        auto seriesKey = TSeriesKey::Make(instanceKey, signal);
        auto metabaseShardKey = TMetabaseShardKey::Make(instanceKey, signal, 1);

        EAccumulatorType aggregationType(GetAggregationType(seriesKey.SignalName));

        auto startTime(AdjustTime(TInstant::Now()) - TDuration::Hours(1));
        TValueHolder storage(aggregationType);
        for (const auto idx : xrange(60)) {
            storage.Add(startTime + RESOLUTION * idx, idx);
        }
        UNIT_ASSERT(storage.Size() > 0);
        UNIT_ASSERT_VALUES_EQUAL(storage.GetKind(), MetricType::DSUMMARY);

        UNIT_ASSERT_STRINGS_EQUAL(TGrpcSettings::Get().GetSolomonClientId(), NTest::SOLOMON_TESTS_CLIENT_ID);
        TSensorId sensorId(client.ResolveSensor(metabaseShardKey, seriesKey, storage.GetKind()).GetRef());
        TNumId numId = client.ResolveNumId(metabaseShardKey);
        client.WritePoints(sensorId, numId, aggregationType, storage.Get());

        auto seriesValues(client.ReadPoints(sensorId, aggregationType, startTime, startTime + RESOLUTION * 60));
        UNIT_ASSERT_VALUES_EQUAL(seriesValues.size(), storage.Size());

        for (const auto idx : xrange(storage.Size())) {
            const auto & [ expectedTimestamp, expectedValue ] = storage.Get(idx);
            const auto& pair(seriesValues[idx]);
            UNIT_ASSERT_VALUES_EQUAL(pair.first, expectedTimestamp);
            UNIT_ASSERT_VALUES_EQUAL(pair.second, expectedValue);
        }
    }

    Y_UNIT_TEST(AverageSensors) {
        TReadWriteClient client;
        THostName groupName(TStringBuf("SAS.000"));
        THostName hostName(TStringBuf("reading_host"));
        TInstanceKey instanceKey(TInstanceKey::FromNamed("yasm_unittest||ctype,geo,prj,tier").AddGroupAndHost(groupName, hostName));
        TSignalName signal(TStringBuf("counter-instance_tmmv"));
        auto seriesKey = TSeriesKey::Make(instanceKey, signal);
        auto metabaseShardKey = TMetabaseShardKey::Make(instanceKey, signal, 1);

        EAccumulatorType aggregationType(GetAggregationType(seriesKey.SignalName));
        UNIT_ASSERT_VALUES_EQUAL(aggregationType, EAccumulatorType::Average);

        auto startTime(AdjustTime(TInstant::Now()) - TDuration::Hours(1));
        TValueHolder storage(aggregationType);
        for (const auto idx : xrange(60)) {
            storage.Add(startTime + RESOLUTION * idx, TValue(1.0, idx + 1));
        }
        UNIT_ASSERT(storage.Size() > 0);
        UNIT_ASSERT_VALUES_EQUAL(storage.GetKind(), MetricType::DSUMMARY);

        UNIT_ASSERT_STRINGS_EQUAL(TGrpcSettings::Get().GetSolomonClientId(), NTest::SOLOMON_TESTS_CLIENT_ID);
        TSensorId sensorId(client.ResolveSensor(metabaseShardKey, seriesKey, storage.GetKind()).GetRef());
        TNumId numId = client.ResolveNumId(metabaseShardKey);
        client.WritePoints(sensorId, numId, aggregationType, storage.Get());

        auto seriesValues(client.ReadPoints(sensorId, aggregationType, startTime, startTime + RESOLUTION * 60));
        UNIT_ASSERT_VALUES_EQUAL(seriesValues.size(), storage.Size());

        for (const auto idx : xrange(storage.Size())) {
            const auto & [ expectedTimestamp, expectedValue ] = storage.Get(idx);
            const auto& pair(seriesValues[idx]);
            UNIT_ASSERT_VALUES_EQUAL(pair.first, expectedTimestamp);
            UNIT_ASSERT_VALUES_EQUAL(pair.second, expectedValue);
        }
    }

    Y_UNIT_TEST(DownsamplingUgramWithOverlappedBuckets) {
        using namespace NZoom::NHgram;
        TReadWriteClient client;
        THostName groupName(TStringBuf("SAS.000"));
        THostName hostName(TStringBuf("reading_host"));
        TInstanceKey instanceKey(TInstanceKey::FromNamed("yasm_unittest||ctype,geo,prj,tier").AddGroupAndHost(groupName, hostName));
        TStringBuilder tempSignalName;
        tempSignalName << "for_test-" << RandomNumber(static_cast<ui64>(10000)) << "_hgram";
        TSignalName signal(tempSignalName);
        auto seriesKey = TSeriesKey::Make(instanceKey, signal);
        auto metabaseShardKey = TMetabaseShardKey::Make(instanceKey, signal, 1);

        EAccumulatorType aggregationType(GetAggregationType(seriesKey.SignalName));
        UNIT_ASSERT_VALUES_EQUAL(aggregationType, EAccumulatorType::Hgram);

        // 1 result downsampled point 2 min = 8 * 3 * 5 seconds raw points
        TDuration downsampledResolution = TDuration::Minutes(2);
        constexpr ui64 pointsInChunk {8};
        auto startTime(
                AdjustTimeToHours(TInstant::Now()) -
                TDuration::Hours(RandomNumber(static_cast<ui64>(10 * 365 * 24)))
        );
        TValueHolder storage1(aggregationType);
        for (const auto idx : xrange(8)) {
            storage1.Add(
                    startTime + RESOLUTION * idx,
                    THgram::Ugram({TUgramBucket(1.0, 2.0, 1.0)})
            );
        }
        for (const auto idx : xrange(8)) {
            storage1.Add(
                    startTime + RESOLUTION * (idx + pointsInChunk),
                    THgram::Ugram({TUgramBucket(3.0, 4.0, 1.0)})
            );
        }
        TValueHolder storage2(aggregationType);
        for (const auto idx : xrange(8)) {
            storage2.Add(
                    startTime + RESOLUTION * (idx + pointsInChunk * 2),
                    THgram::Ugram({TUgramBucket(4.0, 5.0, 1.0)})
            );
        }

        UNIT_ASSERT_STRINGS_EQUAL(TGrpcSettings::Get().GetSolomonClientId(), NTest::SOLOMON_TESTS_CLIENT_ID);
        TSensorId sensorId(client.ResolveSensor(metabaseShardKey, seriesKey, storage1.GetKind()).GetRef());
        TNumId numId = client.ResolveNumId(metabaseShardKey);
        TUgramBuckets freezerBuckets1;
        TUgramBuckets freezerBuckets2;
        for (const auto idx: xrange(5)) {
            freezerBuckets1.emplace_back(TUgramBucket(idx, idx + 1, 0.0));
            freezerBuckets2.emplace_back(TUgramBucket(idx + 3, idx + 1 + 3, 0.0));
        }
        client.WritePoints(sensorId, numId, aggregationType, storage1.Get(), {freezerBuckets1});
        client.WritePoints(sensorId, numId, aggregationType, storage2.Get(), {freezerBuckets2});

        TStockpileDownsamplingOptions downsampling {
                downsampledResolution,
                EAccumulatorType::Hgram,
                MetricType::HIST
        };
        auto seriesValues(client.ReadPoints(
                sensorId,
                aggregationType,
                startTime,
                startTime + RESOLUTION * (pointsInChunk * 3),
                {downsampling}
        ));

        TValueHolder oracleStorage(aggregationType);
        oracleStorage.Add(
            startTime,
            THgram::Ugram({
                TUgramBucket::Point(3, 8),
                TUgramBucket(3, 4, 8),
                TUgramBucket(4, 5, 8),
            })
        );
        UNIT_ASSERT_VALUES_EQUAL(seriesValues.size(), oracleStorage.Size());
        for (const auto idx : xrange(oracleStorage.Size())) {
            const auto & [_, expectedValue ] = oracleStorage.Get(idx);
            const auto& pair(seriesValues[idx]);
            UNIT_ASSERT_VALUES_EQUAL(pair.second, expectedValue);
        }
    }
}
