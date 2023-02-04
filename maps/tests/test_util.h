#pragma once

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>
#include <maps/libs/log8/include/log8.h>
#include <maps/factory/tools/eval_visible_mosaics_geometries/lib/eval_mosaics_geometries.h>

#include <fstream>
#include <random>

#include <library/cpp/testing/common/env.h>
#include <maps/factory/libs/geometry/geolib.h>

namespace maps::factory::eval_mosaics_geometries::tests {

std::vector<Geometry> getTest12();

std::vector<Geometry> addAndTwistGeometries(
    std::vector<Geometry> geometries,
    size_t amount = 100);

void checkSameGeometries(const std::vector<TMap<size_t, Geometry>>& solutions);

}
