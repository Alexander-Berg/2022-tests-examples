#include <maps/factory/libs/dataset/concurrent_transformer.h>

#include <maps/factory/libs/geometry/spatial_ref.h>
#include <maps/factory/libs/geometry/geolib.h>
#include <maps/factory/libs/unittest/tests_common.h>
#include <maps/libs/geolib/include/conversion.h>
#include <maps/libs/concurrent/include/threadpool.h>

#include <contrib/libs/gdal/alg/gdal_alg.h>

#include <util/random/fast.h>
#include <util/generic/scope.h>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;
using namespace geometry;

Y_UNIT_TEST_SUITE(concurrent_transformer_should) {

std::vector<Vector2d> generateGeoPoints(size_t n)
{
    std::vector<Vector2d> points;
    points.reserve(n);
    TFastRng<unsigned> rng{42};
    for (size_t i = 0; i < n; ++i) {
        points.emplace_back(rng.Uniform(10, 70), rng.Uniform(10, 70));
    }
    return points;
}

Y_UNIT_TEST(transform_points_in_one_thread)
{
    auto src = geodeticSr();
    auto dst = mercatorSr();

    ConcurrentTransformer ct(
        GDALCreateReprojectionTransformerEx(src.get(), dst.get(), nullptr));
    for (const auto& point: generateGeoPoints(100)) {
        Vector2d expected = toVector2d(geolib3::geoPoint2Mercator(toGeolibPoint(point)));
        Vector2d res = ct.transform(point);
        Vector2d back = ct.reverseTransform(res);
        EXPECT_THAT(res, EigEq(expected, 1e-6));
        EXPECT_THAT(back, EigEq(point, 1e-6));
    }
}

Y_UNIT_TEST(transform_points_concurrently)
{
    // Transformer should be destroyed before thread pool.
    concurrent::ThreadPool pool;

    auto src = geodeticSr();
    auto dst = mercatorSr();

    ConcurrentTransformer ct(
        GDALCreateReprojectionTransformerEx(src.get(), dst.get(), nullptr));

    auto points = generateGeoPoints(10000);
    std::vector<std::future<std::tuple<Vector2d, Vector2d, Vector2d>>> futures;
    for (const auto& point: points) {
        futures.push_back(pool.async([point, &ct] {
            Vector2d res = ct.transform(point);
            Vector2d back = ct.reverseTransform(res);
            return std::make_tuple(point, res, back);
        }));
    }

    for (auto& future: futures) {
        auto[point, res, back] = future.get();
        Vector2d expected = toVector2d(geolib3::geoPoint2Mercator(toGeolibPoint(point)));
        EXPECT_THAT(res, EigEq(expected, 1e-6));
        EXPECT_THAT(back, EigEq(point, 1e-6));
    }
}

} // suite

} //namespace maps::factory::dataset::tests
