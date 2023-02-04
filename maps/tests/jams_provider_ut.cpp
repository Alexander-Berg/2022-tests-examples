#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <yandex/maps/pb_stream2/writer.h>
#include <maps/jams/libs/joiner/include/jams_provider.h>
#include <maps/jams/libs/joiner/impl/util.h>
#include <maps/libs/jams/router/jams.pb.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/libs/edge_persistent_index/packer/lib/include/persistent_index_builder.h>

#include <boost/optional.hpp>

#include <limits>
#include <sstream>

using namespace maps::jams::joiner;
using namespace maps::jams;
using maps::road_graph::EdgeId;
using maps::road_graph::LongEdgeId;

class Store {
public:
    Store(): ss_(), w_(&ss_)
    {
        router::proto::JamsMetadata meta;
        meta.set_timestamp(10);
        meta.set_graph_version("1");

        push(meta);
    }

    template <typename Item>
    void push(const Item& item)
    {
        w_ << item;
    }

    std::unique_ptr<std::istream> ok()
    {
        w_.close();
        auto ret = std::make_unique<std::istringstream>(ss_.str());
        return ret;
    }

private:
    std::ostringstream ss_;
    maps::pb_stream2::Writer w_;
};


class Fixture: public NUnitTest::TBaseFixture {
public:
    static constexpr int FIXED_REGION_ID = 1;
    static constexpr uint32_t NOW = 1400000000;

    Fixture()
    {
        std::srand(std::time(nullptr));
    }

    void setFilesCount(size_t count) { filesCount_ = count; }
    void setStreamsCount(size_t count) { streamsCount_ = count; }
    void addJams(std::vector<std::pair<double, uint32_t>> jams) { addJams(std::move(jams), jams_); }
    void addExternal(std::vector<std::pair<double, uint32_t>> external) { addJams(std::move(external), external_); }
    void closeEdges(std::vector<uint32_t> edgesToClose, uint32_t start_time = 0, uint32_t end_time = std::numeric_limits<uint32_t>::max())
    {
        for (auto edgeId: edgesToClose) {
            router::proto::Closure closure;
            closure.set_persistent_edge_id(persistentId(edgeId));
            closure.set_region(FIXED_REGION_ID);
            closure.mutable_time_interval()->set_start_time(start_time);
            closure.mutable_time_interval()->set_end_time(end_time);
            closures_.push_back(closure);
        }
    }

    std::unique_ptr<JamsProvider> ecstatic()
    {
        index_ = std::make_unique<maps::road_graph::PersistentIndex>(indexBuilder_.build());

        externalJams_ = createExternal();
        closuresJams_ = createClosures();
        return createEcstaticJamsProvider(speeds(), externalJams_.get(), closuresJams_.get(), index_.get(), NOW, *streamsCount_);
    }

    std::vector<std::pair<double, uint32_t>> generateRandomJams(size_t count)
    {
        std::vector<std::pair<double, uint32_t>> jams(count);
        auto id = 0;
        std::generate(jams.begin(), jams.end(), [&id] { return std::make_pair((std::rand() + 1.0) / RAND_MAX * 100, id++); });
        return jams;
    }

    void baseCheckCorrectness(size_t filesCount, size_t streamsCount, size_t jamsCount)
    {
        setFilesCount(filesCount);
        setStreamsCount(streamsCount);

        addJams(generateRandomJams(jamsCount));

        auto provider = ecstatic();

        UNIT_ASSERT_EQUAL(streamsCount, provider->streamsCount());

        std::vector<bool> seen(jamsCount);
        for (size_t i = 0; i < provider->streamsCount(); ++i) {
            auto stream = provider->stream(i);
            JamsProvider::Jam jam;
            while (stream->next(&jam)) {
                /// Ensure values are not repeated
                UNIT_ASSERT(!seen[jam.edgeId.value()]);
                seen[jam.edgeId.value()] = true;
            }

            UNIT_ASSERT_EQUAL(stream->skipped(), 0);
        }

        /// Ensure all jams were handled
        UNIT_ASSERT(std::all_of(std::begin(seen), std::end(seen), [] (auto e) { return e; }));
    }

    void checkClosuresCorrectness(size_t filesCount, size_t streamsCount)
    {
        static constexpr auto COUNT = 20;
        static constexpr auto NEW_CLOSURES_COUNT = 3;

        setFilesCount(filesCount);
        setStreamsCount(streamsCount);

        addJams(generateRandomJams(COUNT));

        std::vector<uint32_t> closedAtThemoment = {0, 3, 9, 10, 11, 12};

        // add closures that not presents in the jams
        std::vector<uint32_t> newClosures(NEW_CLOSURES_COUNT);
        std::iota(newClosures.begin(), newClosures.end(), COUNT);
        closedAtThemoment.insert(closedAtThemoment.end(), newClosures.begin(), newClosures.end());

        closeEdges(closedAtThemoment, NOW - 3600, NOW + 3600);

        // edge with id = 3 is in closedAtThemoment too,
        // but closures logic must check both periods
        const std::vector<uint32_t> closedInFuture = {1, 2, 3};
        closeEdges(closedInFuture, NOW + 3600);

        auto provider = ecstatic();

        std::vector<double> speeds(COUNT + NEW_CLOSURES_COUNT, -1.0);

        for (size_t i = 0; i < provider->streamsCount() + 1; ++i) {
            auto stream = i < provider->streamsCount()? provider->stream(i): provider->closuresStream();
            JamsProvider::Jam jam;
            while (stream->next(&jam)) {
                if (i < provider->streamsCount() || jam.edgeId.value() >= COUNT) {
                    /// Ensure values are not repeated in speeds and new closures.
                    UNIT_ASSERT_EQUAL(speeds[jam.edgeId.value()], -1.0);
                }
                speeds[jam.edgeId.value()] = jam.speed;
            }

            UNIT_ASSERT_EQUAL(stream->skipped(), 0);
        }

        /// Ensure all jams were handled and closures were set
        for (size_t i = 0; i < speeds.size(); ++i) {
            if (std::find(closedAtThemoment.begin(), closedAtThemoment.end(), i) != closedAtThemoment.end()) {
                /// Ensure that closures were set correctly
                UNIT_ASSERT(speeds[i] == 0.0);
            } else {
                /// Ensure that jams were set correctly
                UNIT_ASSERT(speeds[i] > 0.0);
            }
        }
    }

    std::unique_ptr<router::Closures> createClosures()
    {
        Store store;
        for (auto&& closure: closures_) {
            store.push(closure);
        }

        return std::make_unique<router::Closures>(*index_, store.ok().get());
    }

    std::unique_ptr<router::Jams> createExternal()
    {
        Store store;
        for (auto&& ex: external_) {
            store.push(ex);
        }

        return std::make_unique<router::Jams>(*index_, store.ok().get());
    }

private:
    void addJams(std::vector<std::pair<double, uint32_t>> jams, std::vector<router::proto::JamItem>& to)
    {
        for (auto&& [speed, id]: jams) {
            router::proto::JamItem item;
            item.set_edge_id(id);
            item.set_persistent_edge_id(persistentId(id));
            item.mutable_edge_jam()->set_speed(speed);
            item.mutable_edge_jam()->set_region(FIXED_REGION_ID);

            to.push_back(item);
        }
    }

    uint64_t persistentId(uint32_t id)
    {
        if (auto it = shortToLong_.find(id); it != shortToLong_.end()) {
            return it->second;
        }

        indexBuilder_.setEdgePersistentId(EdgeId(id), LongEdgeId(curLongId_));
        shortToLong_.emplace(id, curLongId_);
        return curLongId_++;
    }

    std::vector<std::unique_ptr<std::istream>> speeds()
    {
        std::vector<std::unique_ptr<std::istream>> ret;

        auto iterators = fairDistributedIterators(jams_, *filesCount_);
        for (auto&& [from, to]: iterators) {
            Store store;
            for (; from != to; ++from) {
                store.push(*from);
            }
            ret.emplace_back(store.ok());
        }

        {
            Store externalStore;
            for (const auto& jam: external_) {
                externalStore.push(jam);
            }

            ret.emplace_back(externalStore.ok());
        }

        return ret;
    }


    maps::road_graph::PersistentIndexBuilder indexBuilder_{"test"};
    std::unique_ptr<maps::road_graph::PersistentIndex> index_;

    boost::optional<size_t> filesCount_;
    boost::optional<size_t> streamsCount_;

    std::vector<router::proto::JamItem> jams_;
    std::vector<router::proto::Closure> closures_;
    std::vector<router::proto::JamItem> external_;

    uint64_t curLongId_ = 1000;
    std::unordered_map<uint32_t, uint64_t> shortToLong_;

    std::unique_ptr<router::Jams> externalJams_;
    std::unique_ptr<router::Closures> closuresJams_;
};


Y_UNIT_TEST_SUITE_F(JamsProviderTests, Fixture)
{
    Y_UNIT_TEST(CheckEcstaticWithSingleFileSingleStream) { baseCheckCorrectness(1, 1, 100); }
    Y_UNIT_TEST(CheckEcstaticWithMultipleFilesSingleStream) { baseCheckCorrectness(8, 1, 100); }
    Y_UNIT_TEST(CheckEcstaticWithMultipleFilesMultipleStream) { baseCheckCorrectness(8, 4, 100); }
    Y_UNIT_TEST(CheckEcstaticInvalidStreamsCount) { UNIT_ASSERT_EXCEPTION(baseCheckCorrectness(1, 3, 1), maps::RuntimeError); }
    Y_UNIT_TEST(CheckEcstaticClosures1) { checkClosuresCorrectness(1, 1); }
    Y_UNIT_TEST(CheckEcstaticClosures2) { checkClosuresCorrectness(8, 1); }
    Y_UNIT_TEST(CheckEcstaticClosures3) { checkClosuresCorrectness(12, 3); }

    Y_UNIT_TEST(CheckEcstaticReuse)
    {
        setFilesCount(1);
        setStreamsCount(1);
        addJams({{16, 0}, {17, 1}, {18, 2}});

        auto provider = ecstatic();

        /// Check if we can fetch the same results multiple times
        for (size_t i = 0; i < 5; ++i) {
            auto stream = provider->stream(0);
            JamsProvider::Jam jam;

            for (EdgeId id: {EdgeId(0), EdgeId(1), EdgeId(2)}) {
                UNIT_ASSERT(stream->next(&jam));
                UNIT_ASSERT_EQUAL(jam.edgeId, id);
            }

            UNIT_ASSERT(!stream->next(&jam));
        }
    }

    Y_UNIT_TEST(CheckExternalSpeeds)
    {
        setFilesCount(2); setStreamsCount(2);

        addJams({{16, 0}, {17, 1}, {18, 2}});
        addExternal({{19, 1}, {20, 0}, {21, 3}});
        closeEdges({0});

        /// Check priority: closure > external > jam
        std::map<EdgeId, double> expected {
            {EdgeId(0), 0}, {EdgeId(1), 19}, {EdgeId(2), 18}, {EdgeId(3), 21}
        };


        auto provider = ecstatic();
        for (size_t i = 0; i < provider->streamsCount(); ++i) {
            auto stream = provider->stream(i);
            JamsProvider::Jam jam;

            size_t cnt = 0;
            while (stream->next(&jam)) {
                UNIT_ASSERT_EQUAL(expected.count(jam.edgeId), 1);
                UNIT_ASSERT_EQUAL(expected[jam.edgeId], jam.speed);
                cnt++;
            }
        }
    }
}
