#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/jams/libs/joiner/impl/joiner.h>

using namespace maps::jams::joiner;
using namespace maps::jams;
using maps::road_graph::EdgeId;

using Crossroad = std::vector<std::pair<EdgeId, double>>;
using PJams = std::vector<JamsProvider::Jam>;

class FakeStream: public JamsProvider::Stream {
public:
    FakeStream(PJams jams): jams_(std::move(jams)), current_(0)
    {}

    bool next(JamsProvider::Jam* jam) override
    {
        if (current_ < jams_.size()) {
            *jam = jams_[current_];
            current_++;
            return true;
        }

        return false;
    }

    size_t skipped() const override { return 0; }
    time_t dataTimestamp() const override { return 0; }

private:
    PJams jams_;
    size_t current_;
};

class FakeJamsProvider: public JamsProvider {
public:
    FakeJamsProvider(PJams jams): jams_(std::move(jams))
    {}

    size_t streamsCount() const override { return 1; }
    bool reusableStream() const override { return true; }
    std::unique_ptr<Stream> stream(size_t n) override
    {
        UNIT_ASSERT_EQUAL(n, 0);
        return std::make_unique<FakeStream>(jams_);
    }

    std::unique_ptr<Stream> closuresStream() override
    {
        return std::make_unique<FakeStream>(PJams());
    }

private:
    PJams jams_;
};

const maps::road_graph::Graph& graph()
{
    static maps::road_graph::Graph graph(
        BinaryPath("maps/data/test/graph4/road_graph.fb"));
    return graph;
}

const maps::road_graph::PersistentIndex& persistentIndex()
{
    static maps::road_graph::PersistentIndex persistentIndex(
        BinaryPath("maps/data/test/graph4/edges_persistent_index.fb"));
    return persistentIndex;
}

std::unique_ptr<JamsProvider> createJamsProvider(PJams jams)
{
    return std::make_unique<FakeJamsProvider>(std::move(jams));
}

Crossroad crossRoadSameCategoriesGreen()
{
    return {
        {EdgeId(1792603), 16},
        {EdgeId(54464), 16},
        {EdgeId(134764), 16},
        {EdgeId(54457), 16}
    };
}

Crossroad crossRoadSameCategoriesGreenAndRed()
{
    return {
        {EdgeId(1792603), 16},
        {EdgeId(54464), 16},
        {EdgeId(134764), 1},
        {EdgeId(54457), 1}
    };
}


Crossroad crossRoadDifferentCategoriesGreen()
{
    return {
        // 5 cat
        {EdgeId(1915090), 16},
        {EdgeId(1915091), 16},
        {EdgeId(2809), 1},
        {EdgeId(418155), 1},
        // 4 cat
        {EdgeId(632475), 16},
        {EdgeId(2005081), 1}
    };
}

Crossroad crossRoadDifferentCategoriesGreenAndRed()
{
    return {
        // 5 cat
        {EdgeId(1915090), 16},
        {EdgeId(1915091), 16},
        {EdgeId(2809), 16},
        {EdgeId(418155), 1},
        // 4 cat
        {EdgeId(632475), 16},
        {EdgeId(2005081), 16}
    };
}

PJams makeJamsInMoscow(Crossroad crossroad)
{
    PJams ret;
    for (auto&& [edge, speed]: crossroad) {
        ret.push_back({edge, speed, 213});
    }

    return ret;
}


void test(PJams input, size_t shardsCount, size_t)
{
    NGeobase::TLookup geobase(BinaryPath("geobase/data/v6/geodata6.bin"));
    speedmap::SeverityConv severityConv(geobase);
    Joiner joiner(
        &graph(),
        &persistentIndex(),
        &severityConv,
        &geobase,
        {213},
        createJamsProvider(input),
        7, 100,
        shardsCount);

    auto jams = joiner.prepare();
    UNIT_ASSERT_EQUAL(jams.size(), shardsCount);

    auto jamsCount = std::accumulate(
        jams.begin(), jams.end(), size_t(0),
        [] (size_t acc, auto&& jams) { return acc + jams.jamsCount(); });
    UNIT_ASSERT_EQUAL(jamsCount, input.size());

    auto joined = joiner.join();
    auto joinedJamsCount = std::accumulate(
        joined.begin(), joined.end(), size_t(0),
        [] (size_t acc, auto&& jams) { return acc + jams.size(); });

    // Test graph data is changing periodically, so we actually
    // can not check anything based on edge ids.
    // so TODO: Need to make these tests better.
    std::cerr << "Joined jams count: " << joinedJamsCount << "\n";
}


Y_UNIT_TEST_SUITE(Joiner)
{
    Y_UNIT_TEST(JoinerSameColorsSameCategories)
    {
        test(makeJamsInMoscow(crossRoadSameCategoriesGreen()), 1, 2);
    }

    Y_UNIT_TEST(JoinerSameColorsDifferentCategories)
    {
        test(makeJamsInMoscow(crossRoadDifferentCategoriesGreen()), 1, 3);
    }

    Y_UNIT_TEST(JoinerDifferentColorsSameCategories)
    {
        // Jams are short, so will be joined even with different severities
        test(makeJamsInMoscow(crossRoadSameCategoriesGreenAndRed()), 1, 2);
    }
    Y_UNIT_TEST(JoinerDifferentColorsDifferentCategories)
    {
        // Jams are short, so will be joined even with different severities
        test(makeJamsInMoscow(crossRoadDifferentCategoriesGreenAndRed()), 1, 3);
    }
}
