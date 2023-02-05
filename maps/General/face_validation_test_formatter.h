#pragma once

#include "gen_doc/test_formatter.h"
#include "test_types/face_validation_test_data.h"

#include <yandex/maps/geotex/document.h>

namespace maps {
namespace wiki {
namespace topo {
namespace doc {

class FaceValidationTestFormatter : public TestFormatter<test::FaceValidationTestData> {
public:
    typedef test::FaceValidationTestData TestType;

    explicit FaceValidationTestFormatter(geotex::Document& document)
        : TestFormatter(document)
    {}

protected:

    virtual LayerPtrList geomLayersBefore(const TestType& testData) const;
    virtual LayerPtrList geomLayersAfter(const TestType& testData) const;
};

} // namespace doc
} // namespace topo
} // namespace wiki
} // namespace maps
