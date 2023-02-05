#pragma once

#include <maps/libs/auth/include/tvm.h>
#include <maps/libs/common/include/file_utils.h>

#include <library/cpp/testing/unittest/env.h>

namespace maps::auth {

class TvmtoolRecipeHelper {
public:
    // recipe files
    static constexpr auto RECIPE_PORT_FILE = "tvmtool.port";
    static constexpr auto RECIPE_AUTHTOKEN_FILE = "tvmtool.authtoken";

    explicit TvmtoolRecipeHelper(std::string_view configPath)
    {
        paths_.setConfigPath(common::joinPath(ArcadiaSourceRoot(), configPath))
            .setTokenPath(RECIPE_AUTHTOKEN_FILE);
    }

    TvmtoolSettings::Paths tvmtoolPaths() const { return paths_; }

    TvmtoolSettings tvmtoolSettings() const {
        auto settings = TvmtoolSettings(tvmtoolPaths());
        auto recipePort = maps::common::readFileToString(RECIPE_PORT_FILE);
        settings.setPort(std::stoi(recipePort));
        return settings;
    }
private:
    TvmtoolSettings::Paths paths_;
};

} // namespace maps::auth
