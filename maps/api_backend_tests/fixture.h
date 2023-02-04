#pragma once
#include <maps/factory/libs/unittest/fixture.h>
#include <maps/factory/services/sputnica_back/lib/globals.h>
#include <maps/factory/services/sputnica_back/lib/yacare_helpers.h>
#include <maps/infra/yacare/include/test_utils.h>

namespace maps::factory::sputnica::tests {

class Fixture : public unittest::Fixture {
public:
    Fixture()
    {
        backend::globals::init();
        yacare::setErrorReporter(handleError);
    }

    ~Fixture()
    {
        yacare::setDefaultErrorReporter();
        backend::globals::shutdown();
    }

    yacare::tests::UserIdHeaderFixture yacareUserIdHeaderFixture{};
};

} //namespace maps::factory::sputnica::tests
