#pragma once

#include "partitioner.h"

#include <yandex/maps/geotex/document.h>
#include <yandex/maps/wiki/test_tools/doc/formatter.h>

namespace maps {
namespace wiki {
namespace geom_tools {
namespace test {

class PolygonPartitionTestFormatter
    : public test_tools::doc::TexTestFormatter<test::PolygonPartitionTestData> {
public:
    typedef test::PolygonPartitionTestData TestType;

    explicit PolygonPartitionTestFormatter(geotex::Document& doc)
        : test_tools::doc::TexTestFormatter<test::PolygonPartitionTestData>(doc)
    {}

protected:
    virtual test_tools::doc::LayerPtrList geomLayersBefore(const TestType&) const;
    virtual test_tools::doc::LayerPtrList geomLayersAfter(const TestType&) const;
};

} // namespace test
} // namespace geom_tools
} // namespace wiki
} // namespace maps
