#pragma once

#include "common.h"

namespace maps {
namespace wiki {
namespace topo {
namespace test {

class SaveObjectsTestData : public CommonTestData
{
public:

    SaveObjectsTestData(
            const std::string& description,
            test::MockStorage storage,
            const Editor::TopologyData& data,
            const TopologyRestrictions& restrictions,
            test::MockStorageDiff resultDiff,
            boost::optional<PrintInfo> printInfo = boost::none,
            boost::optional<ErrorCode> expectedError = boost::none)
        : CommonTestData(
            description,
            storage,
            MockStorage(storage, resultDiff),
            printInfo,
            expectedError)
        , data_(data)
        , restrictions_(restrictions)
    {}

    const Editor::TopologyData& data() const { return data_; }
    const TopologyRestrictions& restrictions() const { return restrictions_; }

private:
    Editor::TopologyData data_;
    TopologyRestrictions restrictions_;
};

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
