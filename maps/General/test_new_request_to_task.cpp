#include <maps/sprav/callcenter/libs/new_request_to_task/new_request_to_task.h>

#include <maps/sprav/callcenter/libs/dao/task.h>
#include <maps/sprav/callcenter/libs/dao/task_type.h>
#include <maps/sprav/callcenter/libs/request_creation/create_request.h>
#include <maps/sprav/callcenter/libs/test_helpers/db_fixture.h>
#include <maps/sprav/callcenter/libs/test_helpers/types.h>
#include <maps/sprav/callcenter/libs/test_helpers/util.h>
#include <maps/sprav/callcenter/libs/yang_client/yang_client.h>

#include <maps/libs/http/include/test_utils.h>

#include <library/cpp/resource/resource.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_extensions/assertions.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

#include <search/idl/meta.pb.h>

#include <sprav/lib/testing/mock_server/mock_server.h>


namespace maps::sprav::callcenter::tests {

class RequestBuilder {
public:
    RequestBuilder(
        NSprav::TSpravEditorClient& spravEditor,
        const NGeobase::TLookup& geobase,
        pgpool3::Pool& pgPool
    )
        : spravEditor_(spravEditor)
        , geobase_(geobase)
        , pgPool_(pgPool)
    {}

    RequestBuilder& withPermalink(int64_t permalink) {
        permalink_ = permalink;
        return *this;
    }

    RequestBuilder& withChain(int64_t chainId) {
        chainId_ = chainId;
        return *this;
    }

    RequestBuilder& withUid(int64_t uid) {
        uid_ = uid;
        return *this;
    }

    RequestBuilder& withPhones(const std::vector<std::string>& phones) {
        phones_ = phones;
        return *this;
    }

    RequestBuilder& withWorkingTime(const TWorkingTimes& workingTime) {
        workingTime_ = workingTime;
        return *this;
    }

    RequestBuilder& withComment(const std::string& comment) {
        comment_ = comment;
        return *this;
    }

    RequestBuilder& withHeadCompanyId(int64_t id) {
        headCompanyId_ = id;
        return *this;
    }

    RequestBuilder& withFormattedPhone(const std::string& formattedPhone) {
        formattedPhone_ = formattedPhone;
        return *this;
    }

    proto::Request build() {
        NSprav::Actualization actualization;
        actualization.mutable_prepared_changes()->set_publishing_status(NSprav::PUBLISH);
        if (permalink_.has_value()) {
            actualization.set_permalink(permalink_.value());
        }
        if (uid_.has_value()) {
            actualization.set_user_id(uid_.value());
        }
        if (!workingTime_.empty()) {
            actualization.mutable_prepared_changes()
                ->add_working_time()
                ->CopyFrom(MakeWorkingTime(workingTime_));
        }
        if (formattedPhone_.has_value()) {
            actualization.mutable_prepared_changes()
                ->add_phones()
                ->set_formatted(formattedPhone_.value().c_str());
        }
        if (comment_.has_value()) {
            actualization.set_comment(TString(comment_.value()));
        }
        if (headCompanyId_.has_value()) {
            actualization.mutable_prepared_changes()
                ->add_head_company_id()
                ->set_value(headCompanyId_.value());
        }

        auto request = request_creation::makeRequest(
            request_creation::ActualizationData{
                .actualization = std::move(actualization),
                .baseTypeId = "actualization",
                .priority = 2,
                .oidSpace = NSprav::OriginalIdSpace::CC_OIS_NONE,
            },
            spravEditor_,
            geobase_,
            pgPool_
        );

        if (chainId_.has_value()) {
            request.set_belongs_to_chain(true);
            request.set_chain_id(chainId_.value());
        }

        if (!phones_.empty()) {
            *request.mutable_related_phones() = {phones_.begin(), phones_.end()};
        }

        auto tx = pgPool_.masterWriteableTransaction();
        request.set_id(dao::Request::create(request, *tx));
        tx->commit();
        return request;
    }

private:
    std::optional<int64_t> permalink_;
    std::optional<int64_t> chainId_;
    std::optional<int64_t> uid_;
    std::optional<int64_t> headCompanyId_;
    std::optional<std::string> formattedPhone_;
    std::optional<std::string> comment_;
    std::vector<std::string> phones_;
    TWorkingTimes workingTime_;

    NSprav::TSpravEditorClient& spravEditor_;
    const NGeobase::TLookup& geobase_;
    pgpool3::Pool& pgPool_;
};


class TaskTypeBuilder {
public:
    TaskTypeBuilder(
        pgpool3::Pool& pgPool
    )
        : pgPool_(pgPool)
    {}

    TaskTypeBuilder& joinMode(proto::TaskTypeConfig::RequestJoinMode::Value requestJoinMode) {
        taskType.mutable_config()->set_request_join_mode(requestJoinMode);
        return *this;
    }

    TaskTypeBuilder& joinByUid() {
        taskType.mutable_config()->set_join_requests_by_uid(true);
        return *this;
    }

    TaskTypeBuilder& addHypotheses() {
        taskType.mutable_config()->set_add_hypotheses(true);
        return *this;
    }

    proto::TaskType build() {
        auto tx = pgPool_.masterWriteableTransaction();
        dao::TaskType::create(taskType, *tx);
        tx->commit();
        return taskType;
    }

private:
    proto::TaskType taskType = test_helpers::protoFromTextFormat<proto::TaskType>(R"(
      id: "actualization"
      is_deleted: false
      config {
        request_join_mode: NORMAL
        join_requests_by_uid: false
        add_hypotheses: false
        yang_config {
          disregard_work_time: false
        }
      }
    )");

    pgpool3::Pool& pgPool_;
};

class NewRequestToTaskTest: public testing::Test {
protected:
    void SetUp() override {
        db = std::make_unique<DbFixture>();
        auto tx = db->pool().masterWriteableTransaction();
        setUpTypes(*tx);
        tx->commit();

    }
    static void SetUpTestSuite() {
        spravEditorMock = std::make_unique<NSprav::TMockServerContainer>(
            TFsPath(GetWorkPath()).Child("sprav_editor_mock"),
            GetOutputPath().Child("sprav_editor_mock"),
            "SPRAV_EDITOR_MOCK_PROXY"
        );
        spravEditor = std::make_unique<NSprav::TSpravEditorClient>(
            "localhost", spravEditorMock->GetPort(), 100, "1120000000036585"
        );
        geobase = new NGeobase::TLookup(BinaryPath("geobase/data/v6/geodata6.bin"));
        workingTimeCalculator = std::make_unique<NSprav::NWorkingTime::TWorkingTimeCalculator>(geobase);
        yangClient = std::make_unique<yang::Client>("http://mockYangApi", "yangToken");
    }

    auto emptySaasMock() {
        return maps::http::addMock(
            "http://saas-test/",
            [](const maps::http::MockRequest&) {
                auto response = maps::http::MockResponse::withStatus(HTTP_OK);

                return response;
            }
        );
    }

    task::TaskDataPtr newRequestToTask(const proto::Request& request, bool addSaasMock = true) {
        std::unique_ptr<maps::http::MockHandle> mock;
        if (addSaasMock) {
            mock = std::make_unique<maps::http::MockHandle>(emptySaasMock());
        }
        return new_request_to_task::newRequestToTask(
            request, *workingTimeCalculator, *spravEditor, *yangClient, *geobase,
            db->pool(), company_searcher::CompanySearcher("http://saas-test/", "43", {})
        );
    }

    RequestBuilder requestBuilder() {
        return RequestBuilder(*spravEditor, *geobase, db->pool());
    }

    TaskTypeBuilder taskTypeBuilder() {
        return TaskTypeBuilder(db->pool());
    }

    static void TearDownTestSuite() {
        spravEditorMock.reset();
        spravEditor.reset();
        workingTimeCalculator.reset();
    }

    std::unique_ptr<DbFixture> db;

    static std::unique_ptr<NSprav::TMockServerContainer> spravEditorMock;
    static std::unique_ptr<NSprav::TSpravEditorClient> spravEditor;
    static NSprav::NWorkingTime::TGeobaseLookupPtr geobase;
    static std::unique_ptr<NSprav::NWorkingTime::TWorkingTimeCalculator> workingTimeCalculator;
    static std::unique_ptr<yang::Client> yangClient;
};

std::unique_ptr<NSprav::TMockServerContainer> NewRequestToTaskTest::spravEditorMock = nullptr;
std::unique_ptr<NSprav::TSpravEditorClient> NewRequestToTaskTest::spravEditor = nullptr;
std::unique_ptr<NSprav::NWorkingTime::TWorkingTimeCalculator> NewRequestToTaskTest::workingTimeCalculator = nullptr;
NSprav::NWorkingTime::TGeobaseLookupPtr NewRequestToTaskTest::geobase = nullptr;
std::unique_ptr<yang::Client> NewRequestToTaskTest::yangClient = nullptr;

TEST_F(NewRequestToTaskTest, NormalNoMergableTasks) {
    auto request = requestBuilder().withPermalink(1124715036).build();
    taskTypeBuilder().build();

    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);

        auto expected = test_helpers::protoFromTextFormat<proto::Task>(R"(
            id: 1
            status: IN_PROGRESS
            type: "actualization"
        )");

        EXPECT_EQ(task->task().status(), proto::Task::Status::IN_PROGRESS);
        EXPECT_EQ(task->task().type(), "actualization");
        EXPECT_EQ(task->requests().size(), 1u);
        EXPECT_EQ(task->requests()[0].id(), request.id());
        EXPECT_EQ(task->requests()[0].workflow_status(), proto::Request::WorkflowStatus::IN_PROGRESS);
    }
}

TEST_F(NewRequestToTaskTest, NormalRequiredMergableTasks) {
    taskTypeBuilder().build();

    auto request = requestBuilder().withPermalink(1124715036).build();
    newRequestToTask(request);

    request = requestBuilder().withPermalink(1124715036).build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 2u);
    }
}

TEST_F(NewRequestToTaskTest, NormalOptionalMergableTasksPhones) {
    taskTypeBuilder().build();

    auto request = requestBuilder().withPermalink(1124715036).build();
    newRequestToTask(request);

    request = requestBuilder().withPhones({"+7 (495) 739-70-00"}).build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 2u);
    }
}

TEST_F(NewRequestToTaskTest, NormalOptionalMergableTasksNoUid) {
    taskTypeBuilder().build();

    auto request = requestBuilder().withPermalink(1124715036).withUid(123).build();
    newRequestToTask(request);

    request = requestBuilder().withPermalink(1210735541).withUid(123).build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }
}

TEST_F(NewRequestToTaskTest, NormalOptionalMergableTasksWithUid) {
    taskTypeBuilder().joinByUid().build();

    auto request = requestBuilder().withPermalink(1124715036).withUid(123).build();
    newRequestToTask(request);

    request = requestBuilder().withPermalink(1210735541).withUid(123).build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 2u);
    }
}


TEST_F(NewRequestToTaskTest, NormalOptionalMergableTasksWithRobotUid) {
    taskTypeBuilder().joinByUid().build();

    auto request = requestBuilder().withPermalink(1124715036).withUid(1120000000022090).build();
    newRequestToTask(request);

    request = requestBuilder().withPermalink(1210735541).withUid(1120000000022090).build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }
}


TEST_F(NewRequestToTaskTest, NormalMultipleMergable) {
    taskTypeBuilder().joinByUid().build();

    auto request = requestBuilder().withPermalink(1124715036).build();
    newRequestToTask(request);

    request = requestBuilder().withPermalink(1210735541).withUid(123).build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }

    request = requestBuilder().withPhones({"+7 (999) 999-99-99"}).build();
    task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }

    request = requestBuilder().withPermalink(1124715036).withUid(123).build();
    request.add_related_phones("+7 (999) 999-99-99");

    {
        auto tx = db->pool().masterWriteableTransaction();
        dao::Request::update(request, *tx);
        tx->commit();
    }

    task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 4u);
    }
}

TEST_F(NewRequestToTaskTest, OffNoMergableTasks) {
    taskTypeBuilder().joinMode(proto::TaskTypeConfig::RequestJoinMode::OFF).build();

    auto request = requestBuilder().withPermalink(1124715036).withUid(123).build();
    newRequestToTask(request);

    request = requestBuilder().withPermalink(1124715036).withUid(123).build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }

    request = requestBuilder().withPhones({"+7 (495) 739-70-00"}).build();
    task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }

}

TEST_F(NewRequestToTaskTest, ChainRequiredMergableTasks) {
    taskTypeBuilder().joinMode(proto::TaskTypeConfig::RequestJoinMode::CHAIN).build();

    auto request = requestBuilder().withPermalink(1124715036).build();
    newRequestToTask(request);

    request = requestBuilder().withPermalink(1124715036).build();
    auto task = newRequestToTask(request);
    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 2u);
    }
}

TEST_F(NewRequestToTaskTest, ChainOptionalMergableTasks) {
    taskTypeBuilder().joinMode(proto::TaskTypeConfig::RequestJoinMode::CHAIN).build();

    auto request = requestBuilder().withChain(123).build();
    newRequestToTask(request);

    request = requestBuilder().withChain(123).build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 2u);
    }
}

TEST_F(NewRequestToTaskTest, NormalCanMergeByWt) {
    taskTypeBuilder().build();

    auto request = requestBuilder()
        .withPhones({"+7 (495) 739-70-00"})
        .withWorkingTime({{EDay::Weekdays, 600, 1000}})
        .build();
    newRequestToTask(request);

    request = requestBuilder()
            .withPhones({"+7 (495) 739-70-00"})
            .withWorkingTime({{EDay::Weekdays, 600, 1100}})
            .build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 2u);
    }
}

TEST_F(NewRequestToTaskTest, NormalUnableToMergeByWtNoIntersection) {
    taskTypeBuilder().build();

    auto request = requestBuilder()
        .withPhones({"+7 (495) 739-70-00"})
        .withWorkingTime({{EDay::Weekdays, 600, 1000}})
        .build();
    newRequestToTask(request);

    request = requestBuilder()
        .withPhones({"+7 (495) 739-70-00"})
        .withWorkingTime({{EDay::Weekdays, 0, 600}, {EDay::Weekdays, 1200, 1440}})
        .build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }
}

TEST_F(NewRequestToTaskTest, NormalUnableToMergeByWtReductionThreshold) {
    taskTypeBuilder().build();

    auto request = requestBuilder()
        .withPhones({"+7 (495) 739-70-00"})
        .withWorkingTime({{EDay::Weekdays, 600, 1000}})
        .build();
    newRequestToTask(request);

    request = requestBuilder()
            .withPhones({"+7 (495) 739-70-00"})
            .withWorkingTime({{EDay::Weekdays, 600, 1200}})
            .build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }
}

TEST_F(NewRequestToTaskTest, NormalUnableToMergeByWtMinThreshold) {
    taskTypeBuilder().build();

    auto request = requestBuilder()
        .withPhones({"+7 (495) 739-70-00"})
        .withWorkingTime({{EDay::Monday, 600, 1080}})
        .build();
    newRequestToTask(request);

    request = requestBuilder()
            .withPhones({"+7 (495) 739-70-00"})
            .withWorkingTime({{EDay::Monday, 660, 1140}})
            .build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }
}

TEST_F(NewRequestToTaskTest, ChainDoNotCheckForWt) {
    taskTypeBuilder().joinMode(proto::TaskTypeConfig::RequestJoinMode::CHAIN).build();

    auto request = requestBuilder()
        .withChain(123)
        .withWorkingTime({{EDay::Weekdays, 600, 1000}})
        .build();
    newRequestToTask(request);

    request = requestBuilder()
        .withChain(123)
        .withWorkingTime({{EDay::Weekdays, 0, 600}, {EDay::Weekdays, 1200, 1440}})
        .build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 2u);
    }
}

TEST_F(NewRequestToTaskTest, UnableToMergeBySize) {
    taskTypeBuilder().build();

    auto request = requestBuilder().withPermalink(1124715036).withComment(std::string("*", 15_MB)).build();
    newRequestToTask(request);

    request = requestBuilder().withPhones({"+7 (495) 739-70-00"}).build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }
}

TEST_F(NewRequestToTaskTest, UnableToMergeByNumberOfCompanies) {
    taskTypeBuilder().build();

    for (size_t i : xrange(10)) {
        auto request = requestBuilder().withPhones({"+7 (495) 739-70-00"}).build();
        auto task = newRequestToTask(request);

        {
            auto tx = db->pool().slaveTransaction();
            dao::TaskActive::fillRequests({task}, *tx);
            EXPECT_EQ(task->requests().size(), i + 1);
        }
    }

    auto request = requestBuilder().withPhones({"+7 (495) 739-70-00"}).build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }
}

TEST_F(NewRequestToTaskTest, RevokeFromYangSuccess) {
    taskTypeBuilder().build();

    auto request = requestBuilder().withPermalink(1124715036).build();
    auto task = newRequestToTask(request);
    {
        auto tx = db->pool().masterWriteableTransaction();
        tx->exec("UPDATE tasker.task_active SET yang_pool_id = 1, yang_task_id = 1 WHERE id = " + std::to_string(task->id()));
        tx->commit();
    }

    request = requestBuilder().withPhones({"+7 (999) 999-99-99"}).build();
    task = newRequestToTask(request);
    {
        auto tx = db->pool().masterWriteableTransaction();
        tx->exec("UPDATE tasker.task_active SET yang_pool_id = 2, yang_task_id = 2 WHERE id = " + std::to_string(task->id()));
        tx->commit();
    }

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }

    auto mocks = {
        maps::http::addMock(
            "http://mockYangApi/api/v1/task-suites/1/set-overlap-or-min",
            [](const maps::http::MockRequest&) {
                return maps::http::MockResponse{"{\"overlap\":0}"};
            }
        ),
        maps::http::addMock(
            "http://mockYangApi/api/v1/task-suites/2/set-overlap-or-min",
            [](const maps::http::MockRequest&) {
                return maps::http::MockResponse{"{\"overlap\":0}"};
            }
        )
    };
    Y_UNUSED(mocks);

    request = requestBuilder().withPermalink(1124715036).withPhones({"+7 (999) 999-99-99"}).build();
    task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 3u);
    }
}


TEST_F(NewRequestToTaskTest, RevokeFromYangUnableToRevokeRequired) {
    taskTypeBuilder().build();

    auto request = requestBuilder().withPermalink(1124715036).build();
    auto task = newRequestToTask(request);
    {
        auto tx = db->pool().masterWriteableTransaction();
        tx->exec("UPDATE tasker.task_active SET yang_pool_id = 1, yang_task_id = 1 WHERE id = " + std::to_string(task->id()));
        tx->commit();
    }

    request = requestBuilder().withPhones({"+7 (999) 999-99-99"}).build();
    task = newRequestToTask(request);
    {
        auto tx = db->pool().masterWriteableTransaction();
        tx->exec("UPDATE tasker.task_active SET yang_pool_id = 2, yang_task_id = 2 WHERE id = " + std::to_string(task->id()));
        tx->commit();
    }

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }

    auto mocks = {
        maps::http::addMock(
            "http://mockYangApi/api/v1/task-suites/1/set-overlap-or-min",
            [](const maps::http::MockRequest&) {
                return maps::http::MockResponse{"{\"response\":\"body\"}"};
            }
        ),
        maps::http::addMock(
            "http://mockYangApi/api/v1/task-suites/2/set-overlap-or-min",
            [](const maps::http::MockRequest&) {
                return maps::http::MockResponse{"{\"overlap\":0}"};
            }
        )
    };
    Y_UNUSED(mocks);

    request = requestBuilder().withPermalink(1124715036).withPhones({"+7 (999) 999-99-99"}).build();
    EXPECT_THROW(newRequestToTask(request), std::exception);
}

TEST_F(NewRequestToTaskTest, RevokeFromYangUnableToRevokeOptional) {
    taskTypeBuilder().build();

    auto request = requestBuilder().withPermalink(1124715036).build();
    auto task = newRequestToTask(request);
    {
        auto tx = db->pool().masterWriteableTransaction();
        tx->exec("UPDATE tasker.task_active SET yang_pool_id = 1, yang_task_id = 1 WHERE id = " + std::to_string(task->id()));
        tx->commit();
    }

    request = requestBuilder().withPhones({"+7 (999) 999-99-99"}).build();
    task = newRequestToTask(request);
    {
        auto tx = db->pool().masterWriteableTransaction();
        tx->exec("UPDATE tasker.task_active SET yang_pool_id = 2, yang_task_id = 2 WHERE id = " + std::to_string(task->id()));
        tx->commit();
    }

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
    }

    auto mocks = {
        maps::http::addMock(
            "http://mockYangApi/api/v1/task-suites/1/set-overlap-or-min",
            [](const maps::http::MockRequest&) {
                return maps::http::MockResponse{"{\"overlap\":0}"};
            }
        ),
        maps::http::addMock(
            "http://mockYangApi/api/v1/task-suites/2/set-overlap-or-min",
            [](const maps::http::MockRequest&) {
                return maps::http::MockResponse{"{\"response\":\"body\"}"};
            }
        )
    };
    Y_UNUSED(mocks);

    request = requestBuilder().withPermalink(1124715036).withPhones({"+7 (999) 999-99-99"}).build();
    task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 2u);
    }
}

TEST_F(NewRequestToTaskTest, addHypothesisPersistent) {
    taskTypeBuilder().build();

    auto request = requestBuilder()
        .withHeadCompanyId(1124715036)
        .build();
    auto task = newRequestToTask(request);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 2u);
        bool requestFound = false;
        for (const auto& request : task->requests()) {
            if (request.permalink() == 1124715036) {
                requestFound = true;
                EXPECT_EQ(request.workflow_status(), proto::Request::WorkflowStatus::IN_PROGRESS);
            }
        }
        EXPECT_TRUE(requestFound);
    }
}

TEST_F(NewRequestToTaskTest, addHypothesisDetachable) {
    taskTypeBuilder().addHypotheses().build();

    auto mock = maps::http::addMock(
        "http://saas-test/",
        [](const maps::http::MockRequest&) {
            auto response = maps::http::MockResponse::withStatus(HTTP_OK);
            response.body = test_helpers::protoFromTextFormat<NMetaProtocol::TReport>(R"(
                Grouping {
                  NumGroups: 1
                  Group {
                    Document {
                      ArchiveInfo {
                        GtaRelatedAttribute {
                          Key: "s_body"
                          Value: "{\"export_id\":1124715036}"
                        }
                      }
                    }
                  }
                }
            )").SerializeAsString();

            return response;
        }
    );

    auto request = requestBuilder()
        .withFormattedPhone("+7(999)999-99-99")
        .build();
    auto task = newRequestToTask(request, false);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 2u);
        bool requestFound = false;
        for (const auto& request : task->requests()) {
            if (request.permalink() == 1124715036) {
                requestFound = true;
                EXPECT_EQ(request.workflow_status(), proto::Request::WorkflowStatus::IN_PROGRESS);
            }
        }
        EXPECT_TRUE(requestFound);
    }
}


TEST_F(NewRequestToTaskTest, addHypothesisDetachableDisabled) {
    taskTypeBuilder().build();

    auto mock = maps::http::addMock(
        "http://saas-test/",
        [](const maps::http::MockRequest&) {
            auto response = maps::http::MockResponse::withStatus(HTTP_OK);
            response.body = test_helpers::protoFromTextFormat<NMetaProtocol::TReport>(R"(
                Grouping {
                  NumGroups: 1
                  Group {
                    Document {
                      ArchiveInfo {
                        GtaRelatedAttribute {
                          Key: "s_body"
                          Value: "{\"export_id\":1124715036}"
                        }
                      }
                    }
                  }
                }
            )").SerializeAsString();

            return response;
        }
    );

    auto request = requestBuilder()
        .withFormattedPhone("+7(999)999-99-99")
        .build();
    auto task = newRequestToTask(request, false);

    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 1u);
        bool requestFound = false;
        for (const auto& request : task->requests()) {
            if (request.permalink() == 1124715036) {
                requestFound = true;
                EXPECT_EQ(request.workflow_status(), proto::Request::WorkflowStatus::IN_PROGRESS);
            }
        }
        EXPECT_FALSE(requestFound);
    }
}


TEST_F(NewRequestToTaskTest, refreshHypothesisOnNewRequest) {
    taskTypeBuilder().build();

    auto request = requestBuilder()
        .withPhones({"123"})
        .withHeadCompanyId(1124715036)
        .build();
    auto task = newRequestToTask(request);

    uint64_t requestId;
    bool requestFound = false;
    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 2u);
        for (const auto& request : task->requests()) {
            if (request.permalink() == 1124715036) {
                requestId = request.id();
                requestFound = true;
            }
        }
    }
    EXPECT_TRUE(requestFound);

    request = requestBuilder()
        .withPhones({"123"})
        .withHeadCompanyId(1210735541)
        .build();
    task = newRequestToTask(request);

    requestFound = false;
    {
        auto tx = db->pool().slaveTransaction();
        dao::TaskActive::fillRequests({task}, *tx);
        EXPECT_EQ(task->requests().size(), 4u);
        for (const auto& request : task->requests()) {
            if (request.permalink() == 1124715036) {
                EXPECT_NE(requestId, request.id());
                requestFound = true;
            }
        }
    }
    EXPECT_TRUE(requestFound);
}

} // maps::sprav::callcenter::tests
