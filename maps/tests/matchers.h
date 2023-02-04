#pragma once

#include <library/cpp/testing/gtest/gtest.h>
#include <maps/automotive/store_internal/lib/helpers.h>

namespace maps::automotive::store_internal {

MATCHER_P(BucketPath, expectedUrl, "")
{
    auto [bucket, path] = extractBucketPath(expectedUrl);
    return bucket == arg.GetBucket() && path == arg.GetKey();
}

}
