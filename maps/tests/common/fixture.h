#pragma once

#include <maps/infopoint/lib/misc/locale.h>
#include <maps/infopoint/lib/misc/coverage.h>
#include <maps/infopoint/lib/misc/timezones.h>

#include <yandex/maps/i18n/i18n.h>

#include <library/cpp/testing/gtest/gtest.h>

struct LocaleFixture {
    LocaleFixture();
    ~LocaleFixture();
};

struct ArcadiaGeofilesFixture {
    ArcadiaGeofilesFixture();
    ~ArcadiaGeofilesFixture();
};


class Fixture: public ::testing::Test {
private:
    LocaleFixture fixLocale_;
    ArcadiaGeofilesFixture setupGeofiles_;
};
