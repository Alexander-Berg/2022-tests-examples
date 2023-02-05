#pragma once

#include "library/cpp/testing/unittest/registar.h"

#include "graph.h"
#include "weight.h"

#include <contrib/libs/flatbuffers/include/flatbuffers/flatbuffers.h>
#include <maps/contrib/succinct/bit_vector.hpp>
#include <maps/contrib/succinct/rs_bit_vector.hpp>
#include <maps/contrib/succinct/elias_fano_compressed_list.hpp>
#include <maps/contrib/succinct/elias_fano.hpp>
#include <maps/contrib/succinct/mapper.hpp>
#include <maps/doc/flatbuffers/offline-driving/driving.fbs.h>
#include <maps/libs/zigzag/include/zigzag.h>
#include <maps/libs/geolib/include/point.h>

namespace od = yandex::maps::offline::driving;

using maps::geolib3::Point2;

bool equalToScale(const Point2& lhs, const Point2& rhs);

struct FixedWidthVector {
    uint64_t at(size_t index) const {
        return data.get_bits(index * width, width);
    }

    size_t width;
    succinct::bit_vector data;
};

struct Fringe {
    succinct::elias_fano rowOffsets;
    succinct::bit_vector data;
};

struct EdgesBegin {
    VertexId source(EdgeId edgeId) const {
        const auto edgePos = edgesBegin.select0(edgeId);
        return edgesBegin.rank(edgePos) - 1;
    }

    EdgeId firstEdgeId(VertexId v) const {
        return edgesBegin.select(v) - v;
    }

    size_t verticesNumber() const {
        return edgesBegin.num_ones() - 1;
    }

    size_t edgesNumber() const {
        return edgesBegin.num_zeros();
    }

    succinct::rs_bit_vector edgesBegin;
};

struct AdjacencyLists : public EdgesBegin {
    VertexId target(EdgeId edgeId) const {
        return source(edgeId) - yandex::maps::zigzagDecode(
            sourceTargetDiff[edgeId]);
    }

    succinct::elias_fano_compressed_list sourceTargetDiff;
};

struct TurnsFringe {
    int duration(size_t row, size_t column) const {
        return durations.at(index(row, column));
    }

    bool requiresAccessPass(size_t row, size_t column) const {
        return accessPass[index(row, column)];
    }

    succinct::elias_fano rowOffsets;
    std::vector<int> durations;
    succinct::bit_vector accessPass;

private:
    size_t index(size_t row, size_t column) const {
        const size_t rowBegin = rowOffsets.select(row) - row;
        const size_t rowEnd = rowOffsets.select(row + 1) - row - 1;
        UNIT_ASSERT(rowBegin + column < rowEnd);
        return rowBegin + column;
    }
};

struct TurnTables {
    int operator()(VertexId v, size_t tableIndex) const
    {
        return tables.duration(indices.at(v), tableIndex);
    }

    bool requiresAccessPass(VertexId v, size_t tableIndex) const
    {
        return tables.requiresAccessPass(indices.at(v), tableIndex);
    }

    TurnsFringe tables;
    FixedWidthVector indices;
};

template<typename Buffer, typename Struct>
void map(
        const Buffer* flatBuffer,
        Struct& value)
{
    auto buf = flatBuffer
        // ->flatbuffers::Vector->uint8_t*
        ->data()->data();
    succinct::mapper::map(
        value, reinterpret_cast<const char*>(buf));
}

void map(
    const od::FixedWidthVector* fbVector,
    FixedWidthVector& vector);

void map(
    const od::GeometriesFringe* fbFringe,
    Fringe& fringe);

void map(
    const od::EdgesBegin* fbEdgesBegin,
    EdgesBegin& edgesBegin);

void map(
    const od::AdjacencyLists* fbAdjacencyLists,
    AdjacencyLists& adjacencyLists);

void map(
    const od::TurnsFringe* fbFringe,
    TurnsFringe& fringe);

void map(
    const od::TurnTables* fbTurnTables,
    TurnTables& turnTables);

bool weightsAreClose(int32_t w1, int32_t w2, int errorMultiplier = 1);
