#pragma once

#include <maps/libs/log8/include/log8.h>
#include <yandex/maps/wiki/unittest/arcadia.h>

namespace maps::wiki::common::tests {

struct DBFixture : unittest::ArcadiaDbFixture
{
    DBFixture()
    {
        log8::setLevel(log8::Level::FATAL);
    }
};

} // namespace maps::wiki::common::tests
