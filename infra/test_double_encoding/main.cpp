#include <infra/yasm/server/lib/storage/float_list.h>
#include <infra/monitoring/common/msgpack.h>

#include <util/random/mersenne.h>
#include <util/generic/xrange.h>
#include <util/stream/str.h>

namespace {
    using TRng = TMersenne<ui64>;

    static constexpr size_t POINTS_COUNT = 300;
    static constexpr TInstant NOW = TInstant::Seconds(1533306600);
    static constexpr TDuration RESOLUTION = TDuration::Seconds(5);
    static constexpr TDuration CHUNK_DURATION = TDuration::Minutes(5);

    class TRetainingVisitor : public NYasmServer::ISeriesVisitor {
    public:
        void OnHeader(TInstant, size_t) override {
        }

        void OnValue(NZoom::NValue::TValueRef value) override {
            Values.emplace_back(value);
        }

        const TVector<NZoom::NValue::TValue>& GetValues() const {
            return Values;
        }

    private:
        TVector<NZoom::NValue::TValue> Values;
    };

    void ValidateSeries(const NYasmServer::TFloatList& recordList, const TVector<TMaybe<double>>& points) {
        TRetainingVisitor visitor;
        recordList.IterValues(NOW, NOW + RESOLUTION * POINTS_COUNT, visitor);
        const auto& values = visitor.GetValues();

        Y_ASSERT(recordList.GetStartTime() >= NOW);

        size_t offset = (recordList.GetStartTime() - NOW)/RESOLUTION;

        Y_ASSERT(offset + values.size() <= points.size());

        for (const auto idx : xrange(offset)) {
            Y_ASSERT(!points[idx].Defined());
        }

        for (const auto idx : xrange(values.size())) {
            const auto reference = points[idx + offset];
            const auto& given = visitor.GetValues()[idx];
            if (reference.Defined()) {
                Y_ASSERT(given == NZoom::NValue::TValue(*reference));
            } else {
                Y_ASSERT(given == NZoom::NValue::TValue());
            }
        }

        for (const auto idx : xrange(values.size() + offset, points.size())) {
            Y_ASSERT(!points[idx].Defined());
        }
    }
}

extern "C" {

    int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
        msgpack::object_handle oh;
        try {
            oh = msgpack::unpack(reinterpret_cast<const char*>(data), size);
        } catch(...) {
            Cerr << "wrong data: " << CurrentExceptionMessage() << Endl;
            return 0;
        }
        if (oh.get().type != msgpack::type::ARRAY) {
            return 0;
        }

        TVector<TMaybe<double>> points;
        points.reserve(POINTS_COUNT);
        for (const auto& value : NMonitoring::TArrayIterator(oh.get().via.array)) {
            if (value.is_nil()) {
                points.emplace_back(Nothing());
            } else {
                points.emplace_back(value.via.f64);
            }
        }
        Y_ASSERT(points.size() == POINTS_COUNT);

        NYasmServer::TFloatList recordList;
        for (const auto idx : xrange(points.size())) {
            if (points[idx].Defined()) {
                recordList.PushValue(NOW + RESOLUTION * idx, *points[idx]);
            }
        }

        ValidateSeries(recordList, points);

        TVector<NYasmServer::TSeriesChunk> chunks;
        for (const auto idx : xrange((RESOLUTION * POINTS_COUNT).GetValue() / CHUNK_DURATION.GetValue())) {
            auto chunk(recordList.GetChunkStartingAt(NOW + CHUNK_DURATION * idx, NYasmServer::ESnapshotMode::PadNulls));
            if (chunk.Defined()) {
                chunks.emplace_back(chunk.GetRef());
                Y_ASSERT(chunks.back().ValuesCount == CHUNK_DURATION.GetValue() / RESOLUTION.GetValue());
            }
        }

        NYasmServer::TFloatList restoredRecordList;
        for (const auto& chunk : chunks) {
            restoredRecordList.AppendChunk(chunk.StartTime, chunk.Data, chunk.ValuesCount);
        }

        ValidateSeries(restoredRecordList, points);

        return 0;
    }

    size_t LLVMFuzzerCustomMutator(uint8_t* data, size_t size, size_t maxSize, unsigned int seed) {
        const TString wrappedData(reinterpret_cast<const char*>(data), size);
        TStringInput stream(wrappedData);

        TRng rng(seed);

        TVector<TMaybe<double>> points;
        points.resize(POINTS_COUNT);
        for (const auto idx : xrange(points.size())) {
            const double prob = rng.GenRandReal1();
            if (prob < 0.1) {
                points[idx] = 0.0;
            } else if (prob < 0.2) {
                double value = 0.0;
                stream.Read(reinterpret_cast<char*>(&value), sizeof(value));
                points[idx] = value;
            } else {
                points[idx] = Nothing();
            }
        }

        msgpack::sbuffer buffer;
        msgpack::packer<msgpack::sbuffer> packer(&buffer);
        packer.pack_array(points.size());
        for (const auto value : points) {
            if (value.Defined()) {
                packer.pack_double(*value);
            } else {
                packer.pack_nil();
            }
        }

        if (buffer.size() > maxSize) {
            Cerr << "too big buffer" << Endl;
            return 0;
        }

        memcpy(data, buffer.data(), buffer.size());
        return buffer.size();
    }

}
