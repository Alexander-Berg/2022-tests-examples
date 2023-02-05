#include "test_cmp.h"

#include "../test_types/mock_storage.h"
#include "test_io.h"
#include "geom_comparison.h"
#include "../util/io.h"


namespace maps {
namespace wiki {
namespace topo {
namespace test {

void checkNode(const topo::Node& recv, const topo::Node& exp)
{
    BOOST_REQUIRE_MESSAGE(
        recv.id() == exp.id(),
        "Node ids differ: received " << recv.id()
            << ", expected " << exp.id()
    );
    BOOST_REQUIRE_MESSAGE(
        test::compare(recv.pos(), exp.pos()),
        "Node pos differs, node id: " << recv.id()
            << ", received pos: " << util::print(recv.pos())
            << ", expected pos: " << util::print(exp.pos())
    );
    auto recvEdges = recv.incidentEdges();
    auto expEdges = exp.incidentEdges();
    if (recvEdges.size() != expEdges.size()) {
        std::ostringstream os;
        os << "received edge ids:";
        for (auto edgePtr : recvEdges) {
            os << " " << edgePtr->id();
        }
        os << ", expected edge ids:";
        for (auto edgePtr : expEdges) {
            os << " " << edgePtr->id();
        }
        BOOST_CHECK_MESSAGE(
            false,
            "Node " << recv.id() << " incident edges differ: "
                << os.str()
        );
    }
    for (auto edgePtr : recvEdges) {
        auto recvId = edgePtr->id();
        BOOST_CHECK_MESSAGE(
            std::find_if(
                expEdges.begin(), expEdges.end(),
                [recvId] (const topo::Edge* edge) -> bool
                {
                    return recvId == edge->id();
                }
            ) != expEdges.end(),
            "Edge " << recvId << " must be incident to node "
                << exp.id() << " but it is not"
        );
    }
    for (auto edgePtr : expEdges) {
        auto expId = edgePtr->id();
        BOOST_CHECK_MESSAGE(
            std::find_if(
                recvEdges.begin(), recvEdges.end(),
                [expId] (const topo::Edge* edge) -> bool
                {
                    return expId == edge->id();
                }
            ) != recvEdges.end(),
            "Edge " << expId << " must be incident to node "
                << recv.id() << " but it is not"
        );
    }
}

void checkNode(const test::Node& recv, const test::Node& exp)
{
    BOOST_REQUIRE_MESSAGE(
        recv.id == exp.id,
        "Node ids differ: received " << recv.id
            << ", expected " << exp.id
    );
    BOOST_REQUIRE_MESSAGE(
        test::compare(recv.pos, exp.pos),
        "Node pos differs, node id: " << recv.id
            << ", received pos: " << util::print(recv.pos)
            << ", expected pos: " << util::print(exp.pos)
    );
}

void checkEdge(const topo::Edge& recv, const topo::Edge& exp)
{
    BOOST_REQUIRE_MESSAGE(
        recv.id() == exp.id(),
        "Edge ids differ: received " << recv.id()
            << ", expected " << exp.id()
    );
    BOOST_REQUIRE_MESSAGE(
        recv.startNode() == exp.startNode(),
        "Edge start nodes differ, edge id :" << recv.id()
            << ", received start: " << util::print(recv.startNode())
            << ", expected start: " << util::print(exp.startNode())
    );
    BOOST_REQUIRE_MESSAGE(
        recv.endNode() == exp.endNode(),
        "Edge end nodes differ, edge id :" << recv.id()
            << ", received end: " << util::print(recv.endNode())
            << ", expected end: " << util::print(exp.endNode())
    );
    BOOST_REQUIRE_MESSAGE(
        test::compare(recv.geom(), exp.geom()),
        "Edge geom differs, edge id: " << recv.id()
            << ", received geom: " << util::print(recv.geom())
            << ", expected geom: " << util::print(exp.geom())
    );
}

void checkEdge(const test::Edge& recv, const test::Edge& exp)
{
    BOOST_REQUIRE_MESSAGE(
        recv.id == exp.id,
        "Edge ids differ: received " << recv.id
            << ", expected " << exp.id
    );
    BOOST_REQUIRE_MESSAGE(
        recv.start == exp.start,
        "Edge start nodes differ, edge id :" << recv.id
            << ", received start: " << util::print(recv.start)
            << ", expected start: " << util::print(exp.start)
    );
    BOOST_REQUIRE_MESSAGE(
        recv.end == exp.end,
        "Edge end nodes differ, edge id :" << recv.id
            << ", received end: " << util::print(recv.end)
            << ", expected end: " << util::print(exp.end)
    );
    BOOST_REQUIRE_MESSAGE(test::compare(recv.geom, exp.geom),
        "Edge geom differs, edge id: " << recv.id
            << ", received geom: " << util::print(recv.geom)
            << ", expected geom: " << util::print(exp.geom)
    );
}

void checkStorageContents(
    const MockStorage& recvStorage, const MockStorage& expStorage)
{
    BOOST_REQUIRE_EQUAL(recvStorage.nodes_.size(), expStorage.nodes_.size());
    auto recvNodeIt = recvStorage.nodes_.begin();
    auto expNodeIt = expStorage.nodes_.begin();
    for (; recvNodeIt != recvStorage.nodes_.end(); ++recvNodeIt, ++expNodeIt) {
        checkNode(recvNodeIt->second, expNodeIt->second);
    }
    BOOST_REQUIRE_EQUAL(recvStorage.edges_.size(), expStorage.edges_.size());
    auto recvEdgeIt = recvStorage.edges_.begin();
    auto expEdgeIt = expStorage.edges_.begin();
    for (; recvEdgeIt != recvStorage.edges_.end(); ++recvEdgeIt, ++expEdgeIt) {
        checkEdge(recvEdgeIt->second, expEdgeIt->second);
    }
}

void checkCacheContents(
    const Cache& recvCache, const Cache& expCache)
{
    NodeConstIteratorRange recvNodes = recvCache.nodes();
    for (const auto& recvNode : recvNodes) {
        BOOST_REQUIRE_MESSAGE(
            expCache.nodeExists(recvNode.id()),
            "Node " << recvNode.id() << " not found in expected cache"
        );
        checkNode(recvNode, expCache.node(recvNode.id()));
    }
    EdgeConstIteratorRange recvEdges = recvCache.edges();
    for (const auto& recvEdge : recvEdges) {
        BOOST_REQUIRE_MESSAGE(
            expCache.edgeExists(recvEdge.id()),
            "Edge " << recvEdge.id() << " not found in expected cache"
        );
        checkEdge(recvEdge, expCache.edge(recvEdge.id()));
    }
}

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps

