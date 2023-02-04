#include <infra/yasm/zoom/components/containers/group.h>
#include <infra/yasm/zoom/components/yasmconf/yasmconf.h>

#include <infra/yasm/zoom/components/serialization/deserializers/json.h>
#include <infra/yasm/zoom/components/serialization/zoom_to_msgpack/zoom_to_msgpack.h>

#include <library/cpp/json/json_reader.h>
#include <library/cpp/json/json_value.h>
#include <library/cpp/json/json_writer.h>

#include <util/random/mersenne.h>

#include <util/system/types.h>

#include <util/generic/set.h>

#include <cmath>

namespace {

    using TRng = TMersenne<ui64>;


    struct TUgramBucketsExtractor final: public NZoom::NValue::IUpdatable, public NZoom::NHgram::IHgramStorageCallback {

        const NZoom::NHgram::TUgramBuckets* BucketsPtr = nullptr;

        void MulNone() override {
        }

        void MulFloat(const double /*value*/) override {
        }

        void MulVec(const TVector<double>& /*value*/) override {
        }

        void MulCountedSum(const double /*sum*/, const ui64 /*count*/) override {
        }

        void MulHgram(const NZoom::NHgram::THgram& value) override {
            value.Store(*this);
        }

        void OnStoreSmall(const TVector<double>& /*values*/, const size_t /*zeros*/) override {
        }

        void OnStoreNormal(const TVector<double>& /*values*/, const size_t /*zeros*/, const i16 /*startPower*/) override {
        }

        void OnStoreUgram(const NZoom::NHgram::TUgramBuckets& buckets) final {
            BucketsPtr = &buckets;
        }
    };

    bool CheckUgram(const NZoom::NValue::TValue& value) {
        TUgramBucketsExtractor extractor;
        value.Update(extractor);
        if (extractor.BucketsPtr == nullptr) {
            return false;
        }
        Y_ASSERT(extractor.BucketsPtr->size() <= 100);
        return true;
    }


    NJson::TJsonValue GenerateRandomUgramHgram(TRng& rng) {
        const size_t len = rng.Uniform(100);
        TSet<double> bounds;
        while(bounds.size() < len) {
            bounds.emplace(rng.GenRandReal4() * (Max<double>() / 3));
        }
        NJson::TJsonValue res;
        res.AppendValue("ugram");
        auto& ugrams = res.AppendValue(NJson::JSON_ARRAY);
        for (auto bound: bounds) {
            double weight;
            do {
                weight = rng.GenRandReal4() * (Max<ui16>() / 2);
            } while (weight == 0.0 && ugrams.GetArray().empty());
            if (weight == 0.0) {
                const auto& array = ugrams.GetArray();
                const auto arraySize = array.size();
                if (arraySize >= 2) {
                    const double lastBound = ugrams.Back()[0].GetDouble();
                    if (lastBound != ugrams[arraySize - 2][0].GetDouble()) {
                        bound = lastBound;
                    }
                }
            }
            auto& ugram = ugrams.AppendValue(NJson::JSON_ARRAY);
            ugram.AppendValue(bound);
            ugram.AppendValue(weight);
        }
        return res;
    }

    NJson::TJsonValue GenerateRandomNormalHgram(TRng& rng) {
        const size_t len = rng.Uniform(100);
        const size_t zerosCount = rng.Uniform(Max<ui32>());
        NJson::TJsonValue res;
        auto& items = res.AppendValue(NJson::JSON_ARRAY);
        for (size_t i = 0; i < len; ++i) {
            double v = 0.0;
            do {
                v = rng.Uniform(Max<ui16>());
            } while (v <= 0.0);
            items.AppendValue(v);
        }
        res.AppendValue(zerosCount);
        res.AppendValue(rng.Uniform(i32(Min<i16>()), i32(Max<i16>())));
        return res;
    }

    NJson::TJsonValue GenerateRandomSmallHgram(TRng& rng) {
        const size_t len = rng.Uniform(100);
        const size_t zerosCount = rng.Uniform(Max<ui32>());
        NJson::TJsonValue res;
        auto& items = res.AppendValue(NJson::JSON_ARRAY);
        for (size_t i = 0; i < len; ++i) {
            double v = 0.0;
            do {
                v = rng.GenRandReal4() * (Max<double>() / 3);
            } while (v <= 0.0);
            items.AppendValue(v);
        }
        res.AppendValue(zerosCount);
        res.AppendValue(NJson::JSON_NULL);
        return res;
    }

    bool ParseInput(const uint8_t* data, const size_t size, NJson::TJsonValue& dst) {
        const TStringBuf dataStr(reinterpret_cast<const char*>(data), size);
        NJson::TJsonValue parsedValue;
        if (!NJson::ReadJsonTree(dataStr, &parsedValue) || !parsedValue.IsArray()) {
            return false;
        }
        dst = std::move(parsedValue);
        return true;
    }

    size_t WriteOutput(const NJson::TJsonValue& value, const size_t maxSize, uint8_t* data) {
        const TString str = NJson::WriteJson(value, false);
        if (str.length() <= maxSize) {
            memcpy(data, str.data(), str.length());
            return str.length();
        }
        return 0;
    }

    void TestZoomAccumulate(const TVector<NZoom::NValue::TValue>& values, const NZoom::NYasmConf::TYasmConf& conf) {
        NZoom::NContainers::TGroupContainer container(conf.GetTypeConf("itype", false));
        NZoom::NValue::TMetricManager metricManager;

        try {
            for (const auto& value: values) {
                TVector<std::pair<NZoom::NSignal::TSignalName, NZoom::NValue::TValue>> values;
                values.emplace_back(NZoom::NSignal::TSignalName(TStringBuf("signal_hgram")),
                    value.GetValue());
                const NZoom::NRecord::TRecord record(std::move(values));
                container.Mul(record, metricManager);
            }
        } catch (const yexception& e) {
            Cerr << e.what() << Endl;
            return;
        }
        // Let ugram compression to work
        NZoom::NPython::TMsgPackSerializer serializer(true);
        container.Process(serializer);
        THolder<NZoom::NRecord::TRecord> rec(NZoom::NPython::TMsgPackDeserializer::Deserialize(serializer.GetValue(), true));
        const auto& aggregatedValues = rec->GetValues();

        if (values.empty()) {
            Y_ASSERT(aggregatedValues.empty());
        } else {
            Y_ASSERT(aggregatedValues.size() == 1);
            Y_ASSERT(CheckUgram(aggregatedValues[0].second));
        }
    }
}

extern "C" {

    int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
        NJson::TJsonValue parsedValue;
        if (!ParseInput(data, size, parsedValue)) {
            Cerr << "Wrong input \"" << TStringBuf(reinterpret_cast<const char*>(data), size) << "\""<< Endl;
            return 0;
        }

        bool hasUgrams = false;
        TVector<NZoom::NValue::TValue> values;
        NZoom::NValue::TValue value;

        for (const auto& hgram: parsedValue.GetArray()) {
            value = NZoom::NPython::TJsonValueHierarchy::DeserializeValue(hgram, true);
            const bool isUgram = CheckUgram(value);
            hasUgrams |= isUgram;
            values.emplace_back(std::move(value));
        }

        if (values.empty() || hasUgrams) {
            const auto emptyYasmConf = NZoom::NYasmConf::TYasmConf::FromString(
                "{\"conflist\": {\"common\": {\"signals\": {}, \"patterns\": {}, \"periods\": {}}}}");
            TestZoomAccumulate(values, emptyYasmConf);
        }
        return 0; // Non-zero return values are reserved for future use.
    }

    size_t LLVMFuzzerCustomMutator(uint8_t* data, size_t size, size_t maxSize, unsigned int seed) {
        NJson::TJsonValue parsedValue;
        if (!ParseInput(data, size, parsedValue)) {
            return 0;
        }

        TRng rng(seed);
        if (rng.GenRandReal1() < 0.5) {
            NJson::TJsonValue::TArray& array = parsedValue.GetArraySafe();
            // Add something
            const size_t position = array.empty() ? 0 : rng.Uniform(array.size());
            const auto it = array.begin() + position;

            const double typeProb = rng.GenRandReal1() * 3;
            if (typeProb < 1.0) {
                array.emplace(it, GenerateRandomUgramHgram(rng));
            } else if (typeProb < 2.0) {
                array.emplace(it, GenerateRandomNormalHgram(rng));
            } else {
                array.emplace(it, GenerateRandomSmallHgram(rng));
            }
        }
        if (!parsedValue.GetArray().empty() && rng.GenRandReal1() < 0.5) {
            // Remove something
            parsedValue.EraseValue(rng.Uniform(parsedValue.GetArray().size()));
        }
        return WriteOutput(parsedValue, maxSize, data);
    }

    size_t LLVMFuzzerCustomCrossOver(const uint8_t* data1, size_t size1,
        const uint8_t* data2, size_t size2,
        uint8_t* out, size_t maxOutSize, unsigned int seed)
    {
        NJson::TJsonValue parsedValue1, parsedValue2;

        if (!ParseInput(data1, size1, parsedValue1)) {
            return 0;
        }

        if (!ParseInput(data2, size2, parsedValue2)) {
            return 0;
        }

        TRng rng(seed);
        const auto& first = parsedValue1.GetArray();
        const auto& second = parsedValue2.GetArray();

        const size_t iterLen = Max(first.size(), second.size());
        NJson::TJsonValue dst;

        for (size_t i = 0; i < iterLen; ++i) {
            if (rng.GenRandReal1() < 0.5 && i < first.size()) {
                dst.AppendValue(first[i]);
            }
            if (rng.GenRandReal1() < 0.5 && i < second.size()) {
                dst.AppendValue(second[i]);
            }
        }
        return WriteOutput(dst, maxOutSize, out);
    }
}
