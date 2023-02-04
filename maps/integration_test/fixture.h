#pragma once

#include <maps/libs/auth/include/test_utils.h>


constexpr auto RECIPE_TVM_CONFIG_FILE = "maps/b2bgeo/libs/tvm/tests/tvmtool.test.conf";

class Fixture : public maps::auth::TvmtoolRecipeHelper {
public:
    Fixture() : TvmtoolRecipeHelper(RECIPE_TVM_CONFIG_FILE)
    {
    }
};

