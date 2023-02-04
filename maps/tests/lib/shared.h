#pragma once

#include <maps/bicycle/router/lib/data.h>
#include <maps/bicycle/router/lib/config.h>

#include <library/cpp/testing/unittest/env.h>
#include <util/folder/path.h>
#include <util/system/fs.h>


namespace maps::bicycle::tests {

struct Shared {
    static const BicycleConfig& config()
    {
        static BicycleConfig config = [] {
            NFs::SetCurrentWorkingDirectory(
                TFsPath(BuildRoot()) /
                "maps/bicycle/router/tests/data");
            BicycleConfig config("config.yaml");
            return config;
        }();
        return config;
    }

    static const GraphData& data()
    {
        return *dataPtr().get();
    }

    static std::shared_ptr<const GraphData>& dataPtr()
    {
        static auto data = std::make_shared<const GraphData>(Shared::config());
        return data;
    }
};

} // namespace maps::bicycle::tests
