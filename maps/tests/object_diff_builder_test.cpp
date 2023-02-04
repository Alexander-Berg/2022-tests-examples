#include <library/cpp/testing/unittest/registar.h>
#include <yandex/maps/wiki/geom_tools/object_diff_builder.h>

#include <maps/libs/json/include/builder.h>
#include <maps/libs/geolib/include/point.h>
#include <maps/libs/geolib/include/polyline.h>

#include <maps/libs/log8/include/log8.h>
#include <maps/libs/common/include/exception.h>

namespace maps::wiki::geom_tools::test {

namespace {

template<class T>
json::Value getJson(const ObjectDiffBuilder<T>& diffBuilder) {
    json::Builder builder;
    builder << [&](json::ObjectBuilder builder) {
        diffBuilder.json(builder);
    };
    return json::Value::fromString(builder.str());
}

} // namespace

Y_UNIT_TEST_SUITE(object_diff_builder) {

using geolib3::Point2;
using geolib3::Polyline2;

Y_UNIT_TEST(points_object_diff_builder_test)
{
    Point2 before{-1.1, 1.2};
    Point2 after{2.3, -2.4};
    const auto objectDiffBuilder = ObjectDiffBuilder<Point2>()
        .setBefore({before})
        .setAfter({after});
    const auto expectedDiff = json::Value::fromString(R"(
    {
        "bounds": [-1.1, -2.4, 2.3, 1.2],
        "modified": {
            "geometry": {
                "before": [
                    {
                        "type": "Point",
                        "coordinates": [-1.1, 1.2]
                    }
                ],
                "after": [
                    {
                        "type": "Point",
                        "coordinates": [2.3, -2.4]
                    }
                ]
            }
        }
    }
    )");
    const auto actualDiff = getJson(objectDiffBuilder);
    INFO() << "actual diff  : '" << actualDiff << "'";
    INFO() << "expected diff: '" << expectedDiff << "'";

    UNIT_ASSERT_EQUAL(expectedDiff, actualDiff);
}

Y_UNIT_TEST(polyline_object_diff_test)
{
    const Point2 p0{0.0, 0.0};
    const Polyline2 before{std::vector{p0}};

    const Point2 p1{-1.0, 0.0};
    const Point2 p2{0.0, -2.0};
    const Point2 p3{3.0, 0.0};
    const Point2 p4{0.0, 4.0};
    const Polyline2 after{std::vector{p1, p2, p3, p4}};
    const auto objectDiffBuilder = ObjectDiffBuilder<Polyline2>()
        .setBefore({before})
        .setAfter({after});

    const auto expectedDiff = json::Value::fromString(
        R"({
            "bounds": [-1, -2, 3, 4],
            "modified": {
                "geometry": {
                    "before": [
                        {
                            "type": "LineString",
                            "coordinates": [
                                [0, 0]
                            ]
                        }
                    ],
                    "after": [
                        {
                            "type": "LineString",
                            "coordinates": [
                                [-1, 0],
                                [0, -2],
                                [3, 0],
                                [0, 4]
                            ]
                        }
                    ]
                }
            }
        })"
    );
    const auto actualDiff = getJson(objectDiffBuilder);
    INFO() << "actual diff  : '" << actualDiff << "'";
    INFO() << "expected diff: '" << expectedDiff << "'";

    UNIT_ASSERT_EQUAL(actualDiff, expectedDiff);
}

Y_UNIT_TEST(empty_object_diff_exception_test)
{
    const auto objectDiffBuilder = ObjectDiffBuilder<Polyline2>();

    // Throw when building bbox for empty diff.
    UNIT_ASSERT_EXCEPTION(
        getJson(objectDiffBuilder),
        maps::RuntimeError);
}

Y_UNIT_TEST(empty_polyline_exception_test)
{
    const auto emptyPolyline = Polyline2();
    const auto objectDiffBuilder = ObjectDiffBuilder<Polyline2>()
        .setBefore({emptyPolyline});

    // geolib throws when trying to find bbox of empty polyline.
    UNIT_ASSERT_EXCEPTION(
        getJson(objectDiffBuilder),
        maps::RuntimeError);
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::geom_tools::test
