#pragma once
#include <yandex/maps/renderer/feature/feature.h>
#include <yandex/maps/renderer/feature/feature_iter.h>
#include <yandex/maps/renderer/feature/detail_range.h>
#include <maps/renderer/libs/base/include/zoom_range.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <string>

namespace maps {
namespace renderer {
namespace data_set_test_util {

inline std::string getTestDataPath(const std::string& filename)
{
    return ArcadiaSourceRoot()
           + "/maps/renderer/libs/data_sets/data_set_test_util/data/"
           + filename;
}

inline void matchIds(feature::FeatureIter& fts,
                     const std::vector<uint64_t>& ids)
{
    std::vector<uint64_t> ftIds;
    while (fts.hasNext()) {
        ftIds.push_back(fts.next().sourceId());
    }
    EXPECT_EQ(ftIds, ids);
}

inline void matchZoomRange(feature::FeatureIter& fts, base::ZoomRange range)
{
    ASSERT_TRUE(fts.hasNext());
    while (fts.hasNext()) {
        const auto& f = fts.next();
        auto attrs = f.attr.get();
        EXPECT_TRUE(base::math::intersects(
            range,
            base::ZoomRange{f.detailRange().min(), f.detailRange().max()}));
        EXPECT_TRUE(base::math::intersects(
            range, {static_cast<base::Zoom>((*attrs)["zmin"].GetInt()),
                    static_cast<base::Zoom>((*attrs)["zmax"].GetInt())}));
    }
}

inline void matchAttribute(feature::FeatureIter& fts,
                           const std::string& attrName,
                           int attrValue)
{
    ASSERT_TRUE(fts.hasNext());
    while (fts.hasNext()) {
        const auto& f = fts.next();
        EXPECT_EQ((*f.attr.get())[attrName.c_str()].GetInt(), attrValue);
    }
}

}
}
} // namespace maps::renderer::data_set_test_util
