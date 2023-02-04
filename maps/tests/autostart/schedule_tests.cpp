#include <library/cpp/testing/unittest/registar.h> 
#include <library/cpp/testing/gmock_in_unittest/gmock.h>

#include <maps/automotive/remote_access/server/lib/autostart/pandora_scheduler.h>

Y_UNIT_TEST_SUITE(test_pandora_autostart_scheduling) {

using maps::automotive::autostart::PandoraScheduler;
using maps::automotive::autostart::PandoraCarSettings;
using maps::automotive::autostart::DatabaseSchedule;

using set_minutes = std::set<std::chrono::minutes>;

const auto t00_00 = std::chrono::minutes(0);
const auto t08_00 = std::chrono::hours(8) + std::chrono::minutes(0);
const auto t08_30 = std::chrono::hours(8) + std::chrono::minutes(30);
const auto t09_00 = std::chrono::hours(9) + std::chrono::minutes(0);

Y_UNIT_TEST(test_get_schedule)
{
    const std::vector<DatabaseSchedule> schedules{
        DatabaseSchedule(
            "",
            std::bitset<7>(0b1000101),
            true,
            std::chrono::hours(8),
            std::chrono::minutes(30)
        ),
        DatabaseSchedule(
            "",
            std::bitset<7>(0b0110000),
            false,
            std::chrono::hours(10),
            std::chrono::minutes(20)
        ),
        DatabaseSchedule(
            "",
            std::bitset<7>(0b0010001),
            true,
            std::chrono::hours(9),
            std::chrono::minutes(0)
        ),
    };

    PandoraScheduler scheduler;
    const auto plan = scheduler.computeAutostartPlan(schedules);
    const auto expected_plan = PandoraScheduler::AutostartPlan{
        set_minutes{t08_30, t09_00},
        set_minutes{},
        set_minutes{t08_30},
        set_minutes{},
        set_minutes{t09_00},
        set_minutes{},
        set_minutes{t08_30}
    };
    ASSERT_EQ(plan, expected_plan);
}

Y_UNIT_TEST(test_duplicate_schedules_are_not_allowed)
{
    const std::vector<DatabaseSchedule> schedules{
        DatabaseSchedule(
            "",
            std::bitset<7>(0b1000101),
            true,
            std::chrono::hours(8),
            std::chrono::minutes(30)
        ),
        DatabaseSchedule(
            "",
            std::bitset<7>(0b1000101),
            false,
            std::chrono::hours(8),
            std::chrono::minutes(30)
        ),
    };

    PandoraScheduler scheduler;
    ASSERT_THROW(scheduler.computeAutostartPlan(schedules), PandoraScheduler::PlanningException);
}

Y_UNIT_TEST(test_compute_right_plan_with_everyday_schedule)
{
    const auto schedule = PandoraScheduler::AutostartPlan{
        set_minutes{t08_00, t08_30},
        set_minutes{t08_00},
        set_minutes{t08_00, t08_30},
        set_minutes{t08_00},
        set_minutes{t08_00, t09_00},
        set_minutes{t08_00},
        set_minutes{t08_00, t08_30}
    };

    PandoraScheduler scheduler;
    const auto pandora_schedule = scheduler.computePandoraSchedule(schedule);
    const auto expected_result = PandoraCarSettings::AutostartSchedule{
        .weekSchedules = {
            PandoraCarSettings::AutostartSchedule::WeekSchedule {
                PandoraCarSettings::ScheduledStartOptions(true, t08_00),
                PandoraCarSettings::ScheduledStartOptions(true, t08_00),
                PandoraCarSettings::ScheduledStartOptions(true, t08_00),
                PandoraCarSettings::ScheduledStartOptions(true, t08_00),
                PandoraCarSettings::ScheduledStartOptions(true, t08_00),
                PandoraCarSettings::ScheduledStartOptions(true, t08_00),
                PandoraCarSettings::ScheduledStartOptions(true, t08_00)
            },
            PandoraCarSettings::AutostartSchedule::WeekSchedule {
                PandoraCarSettings::ScheduledStartOptions(true, t08_30),
                {},
                PandoraCarSettings::ScheduledStartOptions(true, t08_30),
                {},
                PandoraCarSettings::ScheduledStartOptions(true, t09_00),
                {},
                PandoraCarSettings::ScheduledStartOptions(true, t08_30),
            }
        },
        .everydaySchedule = PandoraCarSettings::ScheduledStartOptions()
    };
    ASSERT_EQ(pandora_schedule, expected_result);
}

Y_UNIT_TEST(test_compute_wrong_plan)
{
    const auto schedule_too_much_one_day = PandoraScheduler::AutostartPlan{
        set_minutes{t00_00, t08_00, t08_30},
        set_minutes{t08_00},
        set_minutes{t08_00, t08_30},
        set_minutes{t08_00},
        set_minutes{t08_00, t09_00},
        set_minutes{t08_00},
        set_minutes{t08_00, t08_30}
    };

    PandoraScheduler scheduler;
    ASSERT_THROW(scheduler.computePandoraSchedule(schedule_too_much_one_day), PandoraScheduler::PlanningException);
}

}
