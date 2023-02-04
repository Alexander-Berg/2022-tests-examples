#pragma once

#include <maps/libs/auth/include/test_utils.h>

namespace maps::auth::tests {

constexpr auto RECIPE_TVM_CONFIG_FILE = "maps/libs/auth/tests/tvmtool.recipe.conf";
constexpr auto MULTICLIENT_TVM_CONFIG_FILE = "maps/libs/auth/tests/tvmtool.multiclient.conf";

class Fixture : public TvmtoolRecipeHelper {
public:
    Fixture() : TvmtoolRecipeHelper(RECIPE_TVM_CONFIG_FILE)
    {
    }
};

} // namespace maps::auth::tests
