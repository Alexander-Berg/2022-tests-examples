#include <maps/factory/libs/dataset/geoid.h>

#include <maps/factory/libs/image/image.h>

#include <maps/factory/libs/unittest/tests_common.h>

#include <boost/filesystem.hpp>

namespace maps::factory::dataset::tests {
using namespace maps::factory::tests;

Y_UNIT_TEST_SUITE(geiod_file_should) {

Y_UNIT_TEST(switch_proj_to_filesystem)
{
    TFakeEnv env;
    env.WithSystemTempDir("./local_tmp");

    TGeoidFile::SwitchProjToFilesystem(env);

    EXPECT_EQ(env.Variable("PROJ_LIB").value(), "./local_tmp/proj");
    EXPECT_TRUE(boost::filesystem::exists("./local_tmp/proj/egm96_15.gtx"));
}

Y_UNIT_TEST(apply_mercator_correction)
{
    TMercatorGeoidCorrection corr(Affine2d::Identity());
    Image<int16_t> elev({1, 1}, 1, {0});
    corr.Apply(elev, boxFromSize(1));
    EXPECT_EQ(elev.val(0, 0), 17);
}

} //suite
} //namespace maps::factory::dataset::tests
