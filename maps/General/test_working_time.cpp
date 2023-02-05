#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/working_time.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class WorkingTimeChangeBuilderTest : public testing::Test {
public:
    WorkingTimeChangeBuilder changeBuilder;

    NSprav::WorkingTime::WorkingInterval buildWorkInterval(NSprav::WorkingTime::Day day, int from, int to) {
        NSprav::WorkingTime::WorkingInterval result;
        result.set_day(day);
        auto work = result.add_work();
        work->set_from(from);
        work->set_to(to);

        return result;
    }

    NSprav::WorkingTime buildWorkingTime(
        NSprav::Action action, const std::vector<NSprav::WorkingTime::WorkingInterval>& workIntervals
    )  {
        NSprav::WorkingTime result;
        result.set_action(action);
        for (const auto& interval : workIntervals) {
            result.mutable_structured()->add_interval()->CopyFrom(interval);
        }
        return result;
    }

    NSprav::Company buildChanges(std::vector<NSprav::WorkingTime> workingTime) {
        NSprav::Company result;
        *result.mutable_working_time() = {workingTime.begin(), workingTime.end()};
        return result;
    }

    NSpravTDS::WorkInterval buildTDSInterval(NSpravTDS::WorkInterval::Day day, int from, int to) {
        NSpravTDS::WorkInterval result;
        result.set_day(day);
        result.set_time_minutes_begin(from);
        result.set_time_minutes_end(to);
        return result;
    }

    NSpravTDS::Company buildCompany(std::vector<NSpravTDS::WorkInterval> workingTime) {
        NSpravTDS::Company result;
        *result.mutable_work_intervals() = {workingTime.begin(), workingTime.end()};
        return result;
    }
};

TEST_F(WorkingTimeChangeBuilderTest, Test) {
    proto::Request request1 = buildRequest(1, buildChanges({
        buildWorkingTime(
            NSprav::Action::DELETE,
            {
                buildWorkInterval(NSprav::WorkingTime::Monday, 0, 640),
                buildWorkInterval(NSprav::WorkingTime::Thursday, 0, 640),
            }
        ),
        buildWorkingTime(
            NSprav::Action::ACTUALIZE,
            {
                buildWorkInterval(NSprav::WorkingTime::Everyday, 0, 740),
            }
        ),
    }));

    proto::Request request2 = buildRequest(2, buildChanges({
        buildWorkingTime(
            NSprav::Action::DELETE,
            {
                buildWorkInterval(NSprav::WorkingTime::Monday, 0, 640),
                buildWorkInterval(NSprav::WorkingTime::Thursday, 0, 640),
            }
        ),
    }));

    NSpravTDS::Company company = buildCompany({
        buildTDSInterval(NSpravTDS::WorkInterval::Monday, 0, 640),
        buildTDSInterval(NSpravTDS::WorkInterval::Thursday, 0, 640),
    });

    std::vector<AttributeChanges<TDSWorkingTime>> expected = {
        {
            TDSWorkingTime({
                buildTDSInterval(NSpravTDS::WorkInterval::Monday, 0, 640),
                buildTDSInterval(NSpravTDS::WorkInterval::Thursday, 0, 640),
            }),
            {{
                {
                    buildTDSInterval(NSpravTDS::WorkInterval::Everyday, 0, 740),
                },
                {1},
                NSprav::Action::NONE
            }},
            true
        },
    };

    std::vector<AttributeChanges<TDSWorkingTime>> result = changeBuilder.apply({request1, request2}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

TEST_F(WorkingTimeChangeBuilderTest, EmptyCompany) {
    proto::Request request = buildRequest(1, buildChanges({
        buildWorkingTime(
            NSprav::Action::ACTUALIZE,
            {
                buildWorkInterval(NSprav::WorkingTime::Everyday, 0, 740),
            }
        ),
    }));

    NSpravTDS::Company company = buildCompany({});

    std::vector<AttributeChanges<TDSWorkingTime>> expected = {
        {
            TDSWorkingTime(),
            {{
                {
                    buildTDSInterval(NSpravTDS::WorkInterval::Everyday, 0, 740),
                },
                {1},
                NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<TDSWorkingTime>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(WorkingTimeChangeBuilderTest, EmptyCompanyAndRequest) {
    proto::Request request = buildRequest(1, buildChanges({
        buildWorkingTime(
            NSprav::Action::ACTUALIZE,
            {}
        ),
    }));

    NSpravTDS::Company company = buildCompany({});

    std::vector<AttributeChanges<TDSWorkingTime>> expected = {
        {
            TDSWorkingTime(),
            {{
                {},
                {1},
                NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<TDSWorkingTime>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

} // maps::sprav::callcenter::company_changes::tests
