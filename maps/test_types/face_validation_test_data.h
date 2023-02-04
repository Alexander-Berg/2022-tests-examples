#pragma once

#include "common.h"

namespace maps {
namespace wiki {
namespace topo {
namespace test {

class FaceValidationTestData : public CommonTestData
{
public:
    enum class Type { Correct, Incorrect };

    FaceValidationTestData(
            const std::string& description,
            test::MockStorage originalStorage,
            test::MockStorageDiff resultDiff,
            FaceID faceId,
            FaceRelationsAvailability faceRelationsAvailability,
            Type type,
            boost::optional<PrintInfo> printInfo = boost::none)
        : CommonTestData(
            description,
            originalStorage,
            MockStorage(originalStorage, resultDiff),
            printInfo,
            boost::none)
        , faceId_(faceId)
        , faceRelationsAvailability_(faceRelationsAvailability)
        , type_(type)
    {}

    FaceID faceId() const { return faceId_; }

    FaceRelationsAvailability faceRelationsAvailability() const
    { return faceRelationsAvailability_; }

    Type type() const { return type_; }

private:
    FaceID faceId_;
    FaceRelationsAvailability faceRelationsAvailability_;
    Type type_;
};

} // namespace test
} // namespace topo
} // namespace wiki
} // namespace maps
