#include <maps/wikimap/mapspro/libs/validator/common/utils.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::validator::tests {

namespace {

void compareBoundingBoxes(
    const std::vector<geolib3::BoundingBox>& result,
    const std::vector<geolib3::BoundingBox>& test)
{
    UNIT_ASSERT_EQUAL(result.size(), test.size());
    for (size_t i = 0; i < result.size(); ++i) {
        UNIT_ASSERT_EQUAL(result[i], test[i]);
    }
}

}

Y_UNIT_TEST_SUITE(utils) {

Y_UNIT_TEST(union_intersected_bboxes_empty)
{
    const std::vector<geolib3::BoundingBox> empty;

    compareBoundingBoxes(unionIntersectedBoundingBoxes(empty), empty);
}

Y_UNIT_TEST(union_intersected_bboxes_union_all)
{
    const std::vector<geolib3::BoundingBox> bboxes({
        { {1, 1}, {3, 3} },
        { {2, 2}, {4, 4} },
        { {0, 3}, {5, 6} },
    });

    const std::vector<geolib3::BoundingBox> test({
        { {0, 1}, {5, 6} }
    });

    compareBoundingBoxes(unionIntersectedBoundingBoxes(bboxes), test);
}

Y_UNIT_TEST(union_intersected_bboxes_divided_all)
{
    const std::vector<geolib3::BoundingBox> bboxes({
        { {1, 1}, {2, 2} },
        { {3, 4}, {5, 6} },
        { {4, 1}, {5, 2} },
    });

    compareBoundingBoxes(unionIntersectedBoundingBoxes(bboxes), bboxes);
}

Y_UNIT_TEST(union_intersected_bboxes_union_two_and_alone)
{
    const std::vector<geolib3::BoundingBox> bboxes({
        { {1, 1}, {3, 3} }, // 0
        { {3, 4}, {5, 6} }, // 1 : alone
        { {3, 1}, {5, 2} }, // 2 : moved and merged with 0
    });

    const std::vector<geolib3::BoundingBox> test({
        { {1, 1}, {5, 3} }, // 0 | 2
        { {3, 4}, {5, 6} }, // 1
    });

    compareBoundingBoxes(unionIntersectedBoundingBoxes(bboxes), test);
}

Y_UNIT_TEST(union_intersected_bboxes_union_twice)
{
    const std::vector<geolib3::BoundingBox> bboxes({
        { {1, 1}, {3, 3} }, // 0
        { {3, 4}, {5, 6} }, // 1
        { {3, 1}, {5, 2} }, // 2 : moved and merged with 0
        { {0, 4}, {4, 5} }, // 3 : moved and merged with 1
    });

    const std::vector<geolib3::BoundingBox> test({
        { {1, 1}, {5, 3} }, // 0 | 2
        { {0, 4}, {5, 6} }, // 1 | 3
    });

    compareBoundingBoxes(unionIntersectedBoundingBoxes(bboxes), test);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::validator::tests
