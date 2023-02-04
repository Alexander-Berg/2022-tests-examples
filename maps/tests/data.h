#pragma once

#include <library/cpp/geobase/lookup.hpp>
#include <library/cpp/testing/unittest/env.h>
#include <maps/analyzer/libs/light_calendar/calendar.h>
#include <maps/analyzer/libs/masstransit/time_prediction/track.h>
#include <maps/masstransit/info/libs/common/data.h>
#include <yandex/maps/mms/defs.h>
#include <yandex/maps/mms/holder2.h>

#include <string>

static const auto GEODATA_FILE = BinaryPath("maps/data/test/geobase/geodata4.bin");
static const auto CALENDAR_FILE = BinaryPath("maps/data/test/calendar/light_calendar.fb");
static const auto TZDATA_PATH = BinaryPath("maps/data/test/geobase/zones_bin");

constexpr auto threadId = "43A_57_minibus_default";
// Vehicle type: minibus
// Length: 510.66537
// Stops: 2
// Type: Linear
// Segmentation:
// Index Stage               Type         Length   Stage offset  Thread offset
//     0    0    StopPointSegment        0.00000        0.00000        0.00000
//     1    0         StopSegment       24.99951        0.00000        0.00000
//     2    0        StageSegment      230.32817       24.99951       24.99951
//     3    0        StageSegment      230.32817      255.32768      255.32768
//     4    0         StopSegment       25.01951      485.65586      485.65586
//     5    0    StopPointSegment        0.00000      510.67537      510.67537

inline maps::masstransit::time_prediction::LegacyThreadData readTestData(const std::string& thread) {
    static const auto path = BinaryPath("maps/analyzer/libs/masstransit/time_prediction/tests/data/data.mms");
    static const mms::Holder2<Data<mms::Mmapped>> data {path};

    const auto& threads = data->mtData().threads();
    const auto& segmentations = data->segmentations();

    const auto threadsIt = threads.find(thread);
    const auto segmentationsIt = segmentations.find(thread);

    return {threadsIt->second, segmentationsIt->second, nullptr};
}

static const auto threadData = readTestData(threadId);

static const NGeobase::TLookup geobase{
    NGeobase::TLookup::TInitTraits()
        .Datafile(GEODATA_FILE.c_str())
        .TzDataPath(TZDATA_PATH)
};

static const maps::analyzer::light_calendar::CalendarStorage calendar{
    CALENDAR_FILE,
    /* populate = */ true,
    /* lockMemory = */ false
};
