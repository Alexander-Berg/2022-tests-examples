#pragma once

#include <yandex/maps/wiki/topo/common.h>
#include <yandex/maps/wiki/topo/cache.h>

#include <boost/test/test_tools.hpp>


namespace maps {
namespace wiki {
namespace topo {
namespace test {

class MockStorage;
struct Node;
struct Edge;

void checkNode(const topo::Node& recv, const topo::Node& exp);
void checkNode(const test::Node& recv, const test::Node& exp);

void checkEdge(const topo::Edge& recv, const topo::Edge& exp);
void checkEdge(const test::Edge& recv, const test::Edge& exp);

void checkStorageContents(
    const MockStorage& recvStorage, const MockStorage& expStorage);

void checkCacheContents(
    const Cache& recvCache, const Cache& expCache);

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
