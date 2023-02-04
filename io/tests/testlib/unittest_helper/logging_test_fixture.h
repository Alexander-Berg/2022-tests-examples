#pragma once

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

/* no device, no ipc, only logging */
struct QuasarLoggingTestFixture: public NUnitTest::TBaseFixture {
    using Base = NUnitTest::TBaseFixture;

    void SetUp(NUnitTest::TTestContext& context) override;
    void TearDown(NUnitTest::TTestContext& context) override;
};
