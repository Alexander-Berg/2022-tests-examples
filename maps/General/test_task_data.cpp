#include "common.h"

#include <maps/sprav/callcenter/libs/task/errors.h>
#include <maps/sprav/callcenter/libs/task/task_data.h>

#include <maps/sprav/callcenter/libs/test_helpers/util.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

#include <sprav/algorithms/unification/working_time/operators.h>

#include <util/generic/xrange.h>


void PrintTo(const TWorkingTimes& wt, std::ostream* stream) {
    TString result;
    TStringOutput out(result);
    out << wt;
    *stream << result;
}

namespace maps::sprav::callcenter::task::tests {

using ::testing::ElementsAre;
using ::testing::UnorderedElementsAre;
using ::testing::Pair;
using ::testing::Property;
using ::testing::Pointee;

TEST(TaskDataTest, TestCompanies) {
    TaskData taskData(createTask(1));

    EXPECT_THROW(taskData.addRequests({createRequest(1, 1)}), NotFilledError);
    EXPECT_THROW(taskData.requests(), NotFilledError);
    EXPECT_THROW(taskData.companies(), NotFilledError);
    EXPECT_NO_THROW(taskData.setRequests({createRequest(1, 1)}));
    EXPECT_THROW(taskData.setRequests({createRequest(1, 1)}), AlreadyFilledError);
    EXPECT_NO_THROW(taskData.addRequests({createRequest(2, 1), createRequest(3, 2)}));

    EXPECT_THAT(
        taskData.companies(),
        UnorderedElementsAre(
            Pointee(Property(&CompanyDataBase::requests, ElementsAre(
                NGTest::EqualsProto(createRequest(1, 1)),
                NGTest::EqualsProto(createRequest(2, 1))
            ))),
            Pointee(Property(&CompanyDataBase::requests, ElementsAre(
                NGTest::EqualsProto(createRequest(3, 2))
            )))
        )
    );

    EXPECT_THAT(
        taskData.requests(),
        UnorderedElementsAre(
            NGTest::EqualsProto(createRequest(1, 1)),
            NGTest::EqualsProto(createRequest(2, 1)),
            NGTest::EqualsProto(createRequest(3, 2))
        )
    );
}

TEST(TaskDataTest, TestStatistics) {
    TaskData taskData(createTask(1));
    proto::TaskStatistics statistics;
    statistics.set_id(2);
    statistics.set_content_size(1);
    statistics.set_number_of_companies(5);

    EXPECT_THROW(taskData.statistics(), NotFilledError);
    EXPECT_THROW(taskData.setStatistics(statistics), ConflictError);
    statistics.set_id(1);

    EXPECT_NO_THROW(taskData.setStatistics(statistics));
    EXPECT_THAT(taskData.statistics(), NGTest::EqualsProto(statistics));
}

TEST(TaskDataTest, TestCommitId) {
    {
        TaskData taskData(createTask(1));
        taskData.setRequests({createRequest(1, 1), createRequest(2, 2), createRequest(3, 1)});

        EXPECT_THROW(taskData.setPermalinkCommits({createTaskPermalinkCommit(2, 1, 1)}), ConflictError);
    }
    {
        TaskData taskData(createTask(1));
        taskData.setRequests({createRequest(1, 1), createRequest(2, 2), createRequest(3, 1)});
        EXPECT_NO_THROW(taskData.setPermalinkCommits({createTaskPermalinkCommit(1, 10, 1)}));
    }
    {
        TaskData taskData(createTask(1));
        taskData.setRequests({createRequest(1, 1), createRequest(2, 2), createRequest(3, 1)});
        EXPECT_NO_THROW(taskData.setPermalinkCommits({createTaskPermalinkCommit(1, 2, 1)}));
        EXPECT_THROW(taskData.setPermalinkCommits({createTaskPermalinkCommit(1, 2, 1)}), AlreadyFilledError);

        for (const auto& [_, company] : taskData.permalinkCompanies()) {
            if (company.permalink() == 2) {
                EXPECT_EQ(company.commitId().value(), 1);
            } else {
                EXPECT_FALSE(company.commitId().has_value());
            }
        }
    }
    {
        TaskData taskData(createTask(1));
        taskData.setRequests({createRequest(1, 1), createRequest(2, 2), createRequest(3, 1)});
        taskData.setPermalinkCommits({
            createTaskPermalinkCommit(1, 1, 1),
            createTaskPermalinkCommit(1, 2, 2)
        });
        auto update = createTaskCompanyUpdate(1, std::nullopt, 1);
        update.mutable_company_update();
        taskData.setCompanyUpdates({update});

        proto::TaskType config;
        config.mutable_config()->set_update_commit_id(false);
        taskData.refreshPermalinkCommits(config);
        EXPECT_EQ(taskData.permalinkCompanies().at(1).commitId(), 1);
        EXPECT_EQ(taskData.permalinkCompanies().at(2).commitId(), 2);

        config.mutable_config()->set_update_commit_id(true);
        taskData.refreshPermalinkCommits(config);
        EXPECT_EQ(taskData.permalinkCompanies().at(1).commitId(), 1);
        EXPECT_EQ(taskData.permalinkCompanies().at(2).commitId(), std::nullopt);
    }
}

TEST(TaskDataTest, TestCompanyUpdate) {
    {
        TaskData taskData(createTask(1));
        taskData.setRequests({createRequest(1, 1), createRequest(2, 2), createRequest(3, 1)});

        EXPECT_THROW(taskData.setCompanyUpdates({createTaskCompanyUpdate(2, 1, 1)}), ConflictError);
    }
    {
        TaskData taskData(createTask(1));
        taskData.setRequests({createRequest(1, 1), createRequest(2, 2), createRequest(3, 1)});
        EXPECT_NO_THROW(taskData.setCompanyUpdates({createTaskCompanyUpdate(1, 1, 10)}));
    }
    {
        TaskData taskData(createTask(1));
        taskData.setRequests({createRequest(1, 1), createRequest(2, 2), createRequest(3, 1)});
        EXPECT_NO_THROW(taskData.setCompanyUpdates({createTaskCompanyUpdate(1, 10, std::nullopt)}));
    }
    {
        TaskData taskData(createTask(1));
        taskData.setRequests({createRequest(1, 1), createRequest(2, 2), createRequest(3, 1)});
        EXPECT_THROW(taskData.setCompanyUpdates({createTaskCompanyUpdate(1, std::nullopt, std::nullopt)}), maps::DataValidationError);
    }
    {
        TaskData taskData(createTask(1));
        taskData.setRequests({createRequest(1, 1), createRequest(2, std::nullopt), createRequest(3, 1)});
        EXPECT_NO_THROW(taskData.setCompanyUpdates({createTaskCompanyUpdate(1, 1, 1), createTaskCompanyUpdate(1, 2, std::nullopt)}));
        EXPECT_THROW(taskData.setCompanyUpdates({createTaskCompanyUpdate(1, 1, 1)}), AlreadyFilledError);
    }
}

TEST(TaskDataTest, TestTdsCompany) {
    TaskData taskData(createTask(1));
    EXPECT_THROW(taskData.setTdsCompanies({createTdsCompany(1, 1)}), NotFilledError);

    taskData.setRequests({createRequest(1, 1), createRequest(2, 2), createRequest(3, 1)});
    EXPECT_THROW(taskData.setTdsCompanies({createTdsCompany(1, 1)}), NotFilledError);

    taskData.setPermalinkCommits({createTaskPermalinkCommit(1, 1, 1)});
    EXPECT_NO_THROW(taskData.setTdsCompanies({createTdsCompany(1, 1)}));

    EXPECT_THAT(taskData.permalinkCompanies().at(1).tdsCompany(), NGTest::EqualsProto(createTdsCompany(1, 1)));

    EXPECT_THROW(taskData.setTdsCompanies({createTdsCompany(1, 1)}), AlreadyFilledError);
}

TEST(TaskDataTest, TestSecondScreen) {
    TaskData taskData(createTask(1));
    EXPECT_THROW(taskData.computeDraftlessSecondScreen(), NotFilledError);

    auto creationRequest = createRequest(1, std::nullopt);
    auto name = creationRequest.mutable_feedback()->mutable_prepared_changes()->add_names();
    name->set_action(NSprav::ACTUALIZE);
    name->set_lang(NSprav::NLanguage::RU);
    name->set_value("тест");
    taskData.setRequests({creationRequest, createRequest(2, 1)});

    EXPECT_THROW(taskData.computeDraftlessSecondScreen(), NotFilledError);

    taskData.setPermalinkCommits({});
    taskData.setTdsCompanies({createTdsCompany(1, 1)});

    EXPECT_NO_THROW(taskData.computeDraftlessSecondScreen());

    EXPECT_THAT(
        taskData.permalinkCompanies().at(1).draftlessSecondScreen(),
        NGTest::EqualsProto(createTdsCompany(1, 1))
    );


    NSpravTDS::Company expectedCreationCompany;
    auto tdsName = expectedCreationCompany.add_names();
    tdsName->mutable_value()->mutable_lang()->set_locale("RU");
    tdsName->mutable_value()->set_value("тест");

    EXPECT_THAT(
        taskData.creationCompanies().at(1).draftlessSecondScreen(),
        NGTest::EqualsProto(expectedCreationCompany)
    );

    EXPECT_THROW(taskData.computeDraftlessSecondScreen(), AlreadyFilledError);
}

TEST(TaskDataTest, TestCompanyOrdering) {
    auto request1 = createRequest(1, 1);
    request1.set_type_priority(1);
    request1.set_receive_time(10);

    auto request2 = createRequest(2, 2);
    request2.set_type_priority(1);
    request2.set_receive_time(10);

    auto request3 = createRequest(3, 3);
    request3.set_type_priority(3);
    request3.set_receive_time(5);

    auto request4 = createRequest(4, 3);
    request4.set_type_priority(1);
    request4.set_receive_time(5);

    auto request5 = createRequest(5, 4);
    request5.set_type_priority(10);
    request5.set_receive_time(5);

    TaskData taskData(createTask(1));
    taskData.setRequests({request1, request2, request3, request4, request5});

    EXPECT_EQ(dynamic_cast<const PermalinkCompanyData*>(taskData.mainCompany())->permalink(), 3u);
}

class TaskDataWorkingTimeTest: public testing::Test {
public:
    static void SetUpTestSuite() {
        workingTimeCalculator = std::make_unique<NSprav::NWorkingTime::TWorkingTimeCalculator>(
            new NGeobase::TLookup(BinaryPath("geobase/data/v6/geodata6.bin"))
        );
    }

    static std::unique_ptr<NSprav::NWorkingTime::TWorkingTimeCalculator> workingTimeCalculator;
};

std::unique_ptr<NSprav::NWorkingTime::TWorkingTimeCalculator> TaskDataWorkingTimeTest::workingTimeCalculator = nullptr;


TEST_F(TaskDataWorkingTimeTest, DisregardWorkingTime) {
    TaskData taskData(createTask(1));
    taskData.setRequests({createRequest(1, 1), createRequest(2, 2), createRequest(3, 1)});
    taskData.setPermalinkCommits({});
    taskData.setTdsCompanies({});
    taskData.computeDraftlessSecondScreen();

    proto::TaskType taskType;
    taskType.mutable_config()->mutable_yang_config()->set_disregard_work_time(true);

    TWorkingTimes expected = {{EDay::Everyday, 0, 24 * 60}};
    EXPECT_EQ(
        taskData.calculateWorkingTimes(taskType, *workingTimeCalculator),
        expected
    );
}

TEST_F(TaskDataWorkingTimeTest, Normal) {
    TaskData taskData(createTask(1));
    taskData.setRequests({createRequest(1, 1), createRequest(2, 2)});
    taskData.setPermalinkCommits({});
    taskData.setTdsCompanies({
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            export_id: 1
            work_intervals {
                day: Everyday
                time_minutes_begin: 360
                time_minutes_end: 900
            }
        )"),
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            export_id: 2
            work_intervals {
                day: Everyday
                time_minutes_begin: 600
                time_minutes_end: 1200
            }
        )"),
    });
    taskData.computeDraftlessSecondScreen();

    proto::TaskType taskType;
    taskType.mutable_config()->set_days_to_expand(std::numeric_limits<uint64_t>().max());

    TWorkingTimes expected = {{EDay::Everyday, 600, 900}};
    RevealDayGroups(expected);

    EXPECT_EQ(
        taskData.calculateWorkingTimes(taskType, *workingTimeCalculator),
        expected
    );
}

TEST_F(TaskDataWorkingTimeTest, DaysToExpand) {
    TaskData taskData(createTask(1));
    taskData.setRequests({createRequest(1, 1), createRequest(2, 2)});
    taskData.setPermalinkCommits({});
    taskData.setTdsCompanies({
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            export_id: 1
            work_intervals {
                day: Everyday
                time_minutes_begin: 360
                time_minutes_end: 900
            }
        )"),
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            export_id: 2
            work_intervals {
                day: Everyday
                time_minutes_begin: 600
                time_minutes_end: 1200
            }
        )"),
    });
    taskData.computeDraftlessSecondScreen();

    proto::TaskType taskType;
    taskType.mutable_config()->set_days_to_expand(0);

    TWorkingTimes expected = {{EDay::Weekdays, 540, 1080}};

    EXPECT_EQ(
        taskData.calculateWorkingTimes(taskType, *workingTimeCalculator),
        expected
    );
}

TEST_F(TaskDataWorkingTimeTest, ShortIntervals) {
    TaskData taskData(createTask(1));
    taskData.setRequests({createRequest(1, 1), createRequest(2, 2)});
    taskData.setPermalinkCommits({});
    taskData.setTdsCompanies({
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            export_id: 1
            work_intervals {
                day: Everyday
                time_minutes_begin: 300
                time_minutes_end: 450
            }
            work_intervals {
                day: Everyday
                time_minutes_begin: 600
                time_minutes_end: 750
            }
        )"),
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            export_id: 2
            work_intervals {
                day: Everyday
                time_minutes_begin: 400
                time_minutes_end: 550
            }
            work_intervals {
                day: Everyday
                time_minutes_begin: 700
                time_minutes_end: 850
            }
        )")
    });
    taskData.computeDraftlessSecondScreen();

    proto::TaskType taskType;
    taskType.mutable_config()->set_days_to_expand(std::numeric_limits<uint64_t>().max());

    TWorkingTimes expected = {{EDay::Weekdays, 540, 1080}};

    EXPECT_EQ(
        taskData.calculateWorkingTimes(taskType, *workingTimeCalculator),
        expected
    );
}

TEST_F(TaskDataWorkingTimeTest, TurkeyDefaultWt) {
    TaskData taskData(createTask(1));
    taskData.setRequests({createRequest(1, 1), createRequest(2, 2)});
    taskData.setPermalinkCommits({});
    taskData.setTdsCompanies({
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            export_id: 1
            address {
                geo_id: 116106
            }
            work_intervals {
                day: Everyday
                time_minutes_begin: 360
                time_minutes_end: 900
            }
        )"),
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            export_id: 2
            address {
                geo_id: 116106
            }
            work_intervals {
                day: Everyday
                time_minutes_begin: 600
                time_minutes_end: 1200
            }
        )"),
    });
    taskData.computeDraftlessSecondScreen();

    proto::TaskType taskType;
    taskType.mutable_config()->set_days_to_expand(0);

    TWorkingTimes expected = {
        {EDay::Weekdays, 9 * 60, 18 * 60},
        {EDay::Saturday, 9 * 60, 18 * 60}
    };

    EXPECT_EQ(
        taskData.calculateWorkingTimes(taskType, *workingTimeCalculator),
        expected
    );
}

TEST_F(TaskDataWorkingTimeTest, TurkeySpecialRubrics) {
    TaskData taskData(createTask(1));
    taskData.setRequests({createRequest(1, 1), createRequest(2, 2)});
    taskData.setPermalinkCommits({});
    taskData.setTdsCompanies({
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            export_id: 1
            address {
                geo_id: 116106
            }
            work_intervals {
                day: Everyday
                time_minutes_begin: 360
                time_minutes_end: 900
            }
        )"),
        test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
            export_id: 2
            address {
                geo_id: 116106
            }
            work_intervals {
                day: Everyday
                time_minutes_begin: 600
                time_minutes_end: 1200
            }
            rubrics {
                rubric_id: 30426
            }
        )"),
    });
    taskData.computeDraftlessSecondScreen();

    proto::TaskType taskType;
    taskType.mutable_config()->set_days_to_expand(0);

    TWorkingTimes expected = {
        {EDay::Weekdays, 8 * 60 + 30, 17 * 60},
    };

    EXPECT_EQ(
        taskData.calculateWorkingTimes(taskType, *workingTimeCalculator),
        expected
    );
}

} // maps::sprav::callcenter::task::tests
