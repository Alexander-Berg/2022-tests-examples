#include "logging_test_fixture.h"

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/logging/setup/setup.h>

#include <util/system/env.h>

void QuasarLoggingTestFixture::SetUp(NUnitTest::TTestContext& context) {
    Base::SetUp(context);

    const auto level = GetTestParam("log_level", GetEnv("YIO_LOG_LEVEL", "debug"));
    quasar::Logging::initLoggingToStdout(level);
}

void QuasarLoggingTestFixture::TearDown(NUnitTest::TTestContext& context) {
    /* Clean up global resources */
    quasar::Logging::deinitLogging();

    Base::TearDown(context);
}
