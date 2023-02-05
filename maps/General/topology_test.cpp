#include "base_test.h"

#include <util/generic/xrange.h>

#include "util.h"

using namespace maps;
using namespace maps::road_graph;

class TopologyTest : public BaseTest {
public:
    void operator()() const override {

        size_t countCheckedEdges = 0;
        size_t equalInOutEdges = 0;
        size_t unequalInEdges = 0;
        size_t unequalOutEdges = 0;
        size_t lessInEdges = 0;
        size_t lessOutEdges = 0;
        size_t moreInEdges = 0;
        size_t moreOutEdges = 0;

        for (EdgeId expEdgeId: xrange(experimentRoadGraph().edgesNumber())) {
            if (!experimentRoadGraph().isBase(expEdgeId)) {
                continue;
            }

            const auto longEdgeId = experimentPersistentIndex().findLongId(expEdgeId).value();
            const auto etlEdgeId = etalonPersistentIndex().findShortId(longEdgeId);
            if (!etlEdgeId) {
                continue;
            }

            ++countCheckedEdges;

            const auto expInEdges = inEdges(
                experimentRoadGraph(), experimentPersistentIndex(), expEdgeId);
            const auto etlInEdges = inEdges(
                etalonRoadGraph(), etalonPersistentIndex(), *etlEdgeId);
            const auto expOutEdges = outEdges(
                experimentRoadGraph(), experimentPersistentIndex(), expEdgeId);
            const auto etlOutEdges = outEdges(
                etalonRoadGraph(), etalonPersistentIndex(), *etlEdgeId);

            if (expInEdges.size() == etlInEdges.size() &&
                    expOutEdges.size() == etlOutEdges.size() &&
                    equal(expInEdges, etlInEdges) &&
                    equal(expOutEdges, etlOutEdges)) {
                ++equalInOutEdges;
            }

            if (expInEdges.size() == etlInEdges.size() &&
                    !equal(expInEdges, etlInEdges)) {
                ++unequalInEdges;
            }

            if (expOutEdges.size() == etlOutEdges.size() &&
                    !equal(expOutEdges, etlOutEdges)) {
                ++unequalOutEdges;
            }

            if (expInEdges.size() < etlInEdges.size() &&
                    intersectionSize(expInEdges, etlInEdges) > 0) {
                ++lessInEdges;
            }

            if (expOutEdges.size() < etlOutEdges.size() &&
                    intersectionSize(expInEdges, etlInEdges) > 0) {
                ++lessOutEdges;
            }

            if (expInEdges.size() > etlInEdges.size() &&
                    intersectionSize(expInEdges, etlInEdges) > 0) {
                ++moreInEdges;
            }

            if (expOutEdges.size() > etlOutEdges.size() &&
                    intersectionSize(expOutEdges, etlOutEdges) > 0) {
                ++moreOutEdges;
            }
        }

        INFO() << "\tChecked edges: " <<
            outOfStr(countCheckedEdges, experimentRoadGraph().edgesNumber().value());

        INFO() << "\tEqual in/out-edges: " <<
            outOfStr(equalInOutEdges, countCheckedEdges);

        INFO() << "\tUnequal in-edges: " <<
            outOfStr(unequalInEdges, countCheckedEdges);

        INFO() << "\tUnequal out-edges: " <<
            outOfStr(unequalOutEdges, countCheckedEdges);

        INFO() << "\tLess in-edges: " <<
            outOfStr(lessInEdges, countCheckedEdges);

        INFO() << "\tLess out-edges: " <<
            outOfStr(lessOutEdges, countCheckedEdges);

        INFO() << "\tMore in-edges: " <<
            outOfStr(moreInEdges, countCheckedEdges);

        INFO() << "\tMore out-edges: " <<
            outOfStr(moreOutEdges, countCheckedEdges);
    }

private:
    static std::vector<LongEdgeId> transform(
            const Graph& graph,
            const PersistentIndex& persistentIndex,
            const xrange_view::XRangeView<EdgeId>& edgeIds) {
        std::vector<LongEdgeId> longEdgeIds;
        longEdgeIds.reserve(edgeIds.size());
        for (EdgeId edgeId: edgeIds) {
            if (!graph.isBase(edgeId)) {
                continue;
            }
            longEdgeIds.push_back(persistentIndex.findLongId(edgeId).value());
        }
        std::sort(longEdgeIds.begin(), longEdgeIds.end());
        return longEdgeIds;
    }

    static std::vector<LongEdgeId> inEdges(
            const Graph& graph,
            const PersistentIndex& persistentIndex,
            EdgeId edgeId) {
        return transform(
            graph, persistentIndex, graph.inEdgeIds(graph.edge(edgeId).source));
    }

    static std::vector<LongEdgeId> outEdges(
            const Graph& graph,
            const PersistentIndex& persistentIndex,
            EdgeId edgeId) {
        return transform(
            graph, persistentIndex, graph.outEdgeIds(graph.edge(edgeId).target));
    }

    static bool equal(
            const std::vector<LongEdgeId>& a,
            const std::vector<LongEdgeId>& b) {
        return (a.size() == b.size()) &&
            std::equal(a.begin(), a.end(), b.begin());
    }

    static size_t intersectionSize(
            const std::vector<LongEdgeId>& a,
            const std::vector<LongEdgeId>& b) {
        // a and b are sorted
        size_t res = 0;
        auto ai = a.begin(), aend = a.end();
        auto bi = b.begin(), bend = b.end();
        while (ai != aend && bi != bend) {
            if (*ai < *bi) {
                ++ai;
            } else if (*bi < *ai) {
                ++bi;
            } else {
                ++res;
                ++ai;
                ++bi;
            }
        }
        return res;
    }
};

DECLARE_TEST(TopologyTest)
