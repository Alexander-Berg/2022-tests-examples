#include <maps/wikimap/feedback/api/src/synctool/lib/sync.h>
#include <maps/wikimap/feedback/api/src/synctool/lib/types.h>
#include <maps/wikimap/feedback/api/src/synctool/tests/medium_tests/unittest_translations.h>

#include <maps/wikimap/feedback/api/src/libs/common/config.h>
#include <maps/wikimap/feedback/api/src/libs/dbqueries/constants.h>
#include <maps/wikimap/feedback/api/src/libs/feedback_task_query_builder/insert_query.h>
#include <maps/wikimap/feedback/api/src/libs/feedback_task_query_builder/select_query.h>
#include <maps/wikimap/feedback/api/src/libs/samsara/tests/helpers/censor_fields.h>
#include <maps/wikimap/feedback/api/src/libs/samsara/tests/helpers/mock_log.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/constants.h>
#include <maps/wikimap/feedback/api/src/libs/sync_queue/dbqueries.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/common.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/db_fixture.h>
#include <maps/wikimap/feedback/api/src/libs/test_helpers/printers.h>

#include <maps/wikimap/mapspro/libs/query_builder/include/delete_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/insert_query.h>
#include <maps/wikimap/mapspro/libs/query_builder/include/select_query.h>

#include <maps/libs/auth/include/test_utils.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <fstream>
#include <sstream>

namespace maps::wiki::feedback::api::sync::tests {

namespace sq = sync_queue;

using namespace internal;

namespace {

namespace fbqb = maps::wiki::feedback::api::feedback_task_query_builder;

const TaskId TASK_ID("fa5dd2f3-dd1b-7fb4-4425-21aaf5dbb885");

const FeedbackTask SAMPLE_TASK_ONE{
    TASK_ID,
    Service::Support,
    ServiceObjectId("serviceObjectId"),
    "http://serviceObjectUrl",
    TaskStatus::NeedInfo,
    OriginalTask{maps::json::Value::fromString(R"(
        {
            "form_id": "organization",
            "form_point": {
                "lon": 37.37,
                "lat": 55.55
            },
            "message": "test",
            "metadata": {
                "uid": 42,
                "locale": "en_US",
                "client_id": "mobile_maps_android",
                "version": "1.0"
            },
            "user_email": "vasya.pupkin@yandex.ru",
            "answer_id": "entrance",
            "object_uri": "ymapsbm1://org?oid=28397857759",
            "object_url": "object_url",
            "question_id": "add_object",
            "answer_context": {
                "company": {
                    "name": "Изумруд",
                    "status": "open",
                    "address": "Россия, Ростовская область, Шахты, переулок Вишневского, 7",
                    "coordinates": {
                        "lat": 47.683122,
                        "lon": 40.238885
                    }
                }
            },
            "attached_photos": [
                "1895250/2a000001740a793daaec2195b31b1a6c01af"
            ]
        })"
    )},
    {/*integration*/},
    maps::chrono::parseIsoDateTime("2020-04-01 01:00:00+00:00"),
    maps::chrono::parseIsoDateTime("2020-04-02 01:00:00+00:00")
};

void insertTaskInSyncQueue(
    pqxx::transaction_base& txn,
    const FeedbackTask& task,
    sq::SyncAction action,
    std::optional<std::string> syncInfo = std::nullopt)
{
    maps::wiki::query_builder::InsertQuery query(sq::TABLE_SYNC_QUEUE);
    query
        .appendQuoted(dbqueries::columns::TASK_ID, TASK_ID.value())
        .appendQuoted(
            sq::columns::FEEDBACK_TASK,
            (maps::json::Builder() << task).str()
        )
        .appendQuoted(
            sq::columns::ACTION,
            std::string{toString(action)}
        );
    if (syncInfo) {
        query.appendQuoted(sq::columns::SYNC_INFO, *syncInfo);
    }
    query.exec(txn);
}

class TestKeyMaker : public IUniquenessKeyMaker {
public:
    TestKeyMaker() : prefix_("testKey_") {}

    virtual samsara::UniquenessKey make(const UniquenessKeyTag& tag) const override {
        return samsara::UniquenessKey{prefix_ + tag.value()};
    }

    virtual samsara::UniquenessKey make(const UniquenessKeyTag& tag, size_t index) const override {
        return samsara::UniquenessKey{prefix_ + tag.value() + std::to_string(index)};
    }

private:
    std::string prefix_;
};

struct TestGlobals : Globals
{
    TestGlobals(
        const maps::json::Value& config,
        const maps::xml3::Doc& xml)
    : Globals{
        ugc::Client(config, "FAKE_TANKER_TOKEN", nullptr),
        samsara::Client(config, "fake_token", SAMSARA_TRANSLATIONS),
        sender::Client(config),
        sprav::Client(config),
        sup::Client(config),
        geosearch::Client(config, nullptr),
        static_api::QueryMaker(
            config["yandexHosts"]["staticApi"].as<std::string>(),
            "api_key",
            "signing_secret"),
        EMAILS_TEST_TRANSLATIONS,
        PushSettings(
            config["sup"],
            PUSHES_TEST_TRANSLATIONS,
            config["yandexHosts"]["ugcFeedback"].as<std::string>()),
        NeedInfoEmailSettings(
            config["samsara"]["templates"],
            NEED_INFO_EMAILS_TEST_TRANSLATIONS,
            config["tanker"]["need_info_emails"]["keys"]),
        st::Configuration("https://st-api.yandex-team.ru", "st-token"),
        xml}
    {}

    pgpool3::Pool& pool() const override
    {
        return db.pool();
    }

    mutable api::tests::DbFixture db;
    TestKeyMaker keyMaker;
};

TestGlobals makeGlobals()
{
    auto config = maps::json::Value::fromFile(SRC_("data/feedback_api.conf"));
    auto dbConfigDoc = maps::xml3::Doc::fromString(dbConfigFromJson(config));

    return TestGlobals{config, dbConfigDoc};
}

void putFeedbackTask(pqxx::transaction_base& txn, const FeedbackTask& task)
{
    fbqb::InsertQuery()
        .id(task.id)
        .service(task.service)
        .serviceObjectId(task.serviceObjectId)
        .serviceObjectUrl(task.serviceObjectUrl)
        .originalTask(task.originalTask)
        .createdAt(task.createdAt)
        .status(task.status)
        .integration(task.integration)
        .exec(txn);
}

size_t syncQueueSize(
    pqxx::transaction_base& txn,
    std::optional<sq::SyncAction> syncAction = std::nullopt)
{
    maps::wiki::query_builder::WhereConditions conditions;
    if (syncAction) {
        conditions.appendQuoted(sq::columns::ACTION, std::string{toString(*syncAction)});
    }

    auto query = maps::wiki::query_builder::SelectQuery(
        sq::TABLE_SYNC_QUEUE, conditions);

    return query.exec(txn).size();
}

struct GeosearchMock {
    struct Params {
        geolib3::Point2 formPoint;
        std::string locale;
        std::string responseFilename;
    };

    GeosearchMock(const std::vector<Params>& params)
        : mock(maps::http::addMock(
            "http://geosearch/",
            [params](const maps::http::MockRequest& request) {
                for (const auto& param : params) {
                    if (request.url.params() == makeGeosearchParams(param.formPoint, param.locale)) {
                        return maps::common::readFileToString(param.responseFilename);
                    }
                }
                UNIT_ASSERT(false && "geosearch mock not found");
                return std::string{};
            }))
    {
    }

    maps::http::MockHandle mock;

private:
    static std::string makeGeosearchParams(geolib3::Point2 point, const std::string& locale)
    {
        return (std::ostringstream()
            << "origin=feedback&lang=" << locale
            << "&type=geo&mode=reverse&ll="
            << std::fixed << std::setprecision(9)
            << point.x() << "%2C" << point.y()).str();
    }
};

struct SenderMock {
    SenderMock (
        const std::string& campaign,
        const maps::json::Value& expectedRequest)
        : mock(maps::http::addMock(
            "https://test.sender.yandex-team.ru/api/0/maps/transactional/" + campaign + "/send",
            [this, expectedRequest](const maps::http::MockRequest& request) {
                isTriggered = true;
                UNIT_ASSERT_VALUES_EQUAL(
                    maps::json::Value::fromString(request.body),
                    expectedRequest);
                return maps::http::MockResponse::withStatus(200);
            }))
    {
    }

    bool isTriggered{false};
    maps::http::MockHandle mock;
};

} // namespace

Y_UNIT_TEST_SUITE(test_sync)
{

Y_UNIT_TEST(make_need_info_request)
{
    auto globals = makeGlobals();

    {
        auto txn = globals.pool().masterWriteableTransaction();
        putFeedbackTask(*txn, SAMPLE_TASK_ONE);
        txn->commit();
    }

    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::SamsaraMakeNeedInfoRequest,
        SAMPLE_TASK_ONE,
        maps::json::Value::fromString(
            (maps::json::Builder() << sq::SamsaraMakeNeedInfoRequestParams{
                RequestTemplateId::ToponymNeedProof,
                "test article message",
            }).str())
    };

    samsara::tests::MockLog mockLog;

    mockLog.addLoggingMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/withArticles",
        maps::http::MockResponse{R"({"entityId":12345})"});

    mockLog.addLoggingMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/12345/articles",
        maps::http::MockResponse::withStatus(200));

    mockLog.addLoggingMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/12345/info",
        maps::http::MockResponse::withStatus(200));

    mockLog.addLoggingMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/multi",
        maps::http::MockResponse::fromFile(SRC_("data/tickets.json")));

    mockLog.addLoggingMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/pattern/compile/full/100560",
        maps::http::MockResponse::fromFile(SRC_("data/compiled_pattern.json")));

    // Actually call tested function
    {
        auto txn = globals.pool().masterWriteableTransaction();
        UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
        txn->commit();
    }

    auto url = [](const std::string& handle) {
        return "https://test-api.samsara.yandex-team.ru/api/v2/" + handle;
    };
    auto jsonFile = [](const std::string& filename) {
        return maps::json::Value::fromFile(SRC_(filename));
    };
    auto jsonString = [](const std::string& string) {
        return maps::json::Value::fromString(string);
    };

    UNIT_ASSERT_EQUAL(mockLog.log.size(), 7);

    // Checking both order and content of api requests
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[0].url,        url("tickets/withArticles"));
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[0].params,     "");
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[0].bodyJson(), jsonFile("data/ticket_with_articles_in.json"));

    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[1].url,        url("tickets/12345/articles"));
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[1].params,     "uniquenessKey=testKey_noteArticle");
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[1].bodyJson(), jsonFile("data/article_note_toponym.json"));

    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[2].url,        url("tickets/12345/info"));
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[2].params,     "");
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[2].bodyJson(), jsonString(R"({"status":"OPEN","resolution":"NEW"})"));

    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[3].url,        url("tickets/multi"));
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[3].params,     "ticketId=12345");
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[3].body,       "");

    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[4].url,        url("pattern/compile/full/100560"));
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[4].params,     "queueId=100263&ticketId=12345&articleId=1234");
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[4].bodyJson(), jsonString(R"({"placeholders": {}})"));

    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[5].url,        url("tickets/12345/articles"));
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[5].params,     "uniquenessKey=testKey_outArticle");
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[5].bodyJson(), jsonFile("data/article_out_auto_toponym.json"));

    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[6].url,        url("tickets/12345/info"));
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[6].params,     "");
    UNIT_ASSERT_VALUES_EQUAL(mockLog.log[6].bodyJson(), jsonString(
        R"({"status":"CLOSED","resolution":"WAIT_FOR_USER","queueId":100263})"));

    const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(SAMPLE_TASK_ONE.id))
        .exec(*globals.pool().slaveTransaction());
    UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);

    const auto& integration = tasks[0].integration;
    UNIT_ASSERT_VALUES_EQUAL(integration.services().size(), 1);
    UNIT_ASSERT(integration.hasService(Service::SupportNeedInfo));
    auto serviceDesc = integration.at(Service::SupportNeedInfo);
    UNIT_ASSERT(serviceDesc.serviceObjectId);
    UNIT_ASSERT_VALUES_EQUAL(serviceDesc.serviceObjectId->value(), "12345");
    UNIT_ASSERT(integration.samsaraTicketsHistory().empty());
}

Y_UNIT_TEST(make_need_info_request_missing_email)
{
    auto globals = makeGlobals();

    auto task = SAMPLE_TASK_ONE;
    task.originalTask.resetUserData();

    {
        auto txn = globals.pool().masterWriteableTransaction();
        putFeedbackTask(*txn, task);
        txn->commit();
    }

    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::SamsaraMakeNeedInfoRequest,
        task,
        maps::json::Value::fromString(
            (maps::json::Builder() << sq::SamsaraMakeNeedInfoRequestParams{
                RequestTemplateId::ToponymNeedProof,
                "test article message",
            }).str())
    };

    // For maps::http to react to unregistered handles
    samsara::tests::MockLog mockLog;
    mockLog.addLoggingMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/withArticles",
        maps::http::MockResponse{R"({"entityId":12345})"});

    // Actually call tested function
    {
        auto txn = globals.pool().masterWriteableTransaction();
        UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
        txn->commit();
    }

    // No requests should be sent
    UNIT_ASSERT_EQUAL(mockLog.log.size(), 0);

    const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(task.id))
        .exec(*globals.pool().slaveTransaction());
    UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(tasks[0].service, Service::Nmaps);
    UNIT_ASSERT_VALUES_EQUAL(tasks[0].status, TaskStatus::New);

    const auto& integration = tasks[0].integration;
    UNIT_ASSERT_VALUES_EQUAL(integration.services().size(), 0);
    UNIT_ASSERT(integration.samsaraTicketsHistory().empty());

    const auto history = query_builder::SelectQuery(
        dbqueries::tables::TASK_CHANGES,
        query_builder::WhereConditions().appendQuoted(
            dbqueries::columns::TASK_ID, task.id.value())).exec(*globals.pool().slaveTransaction());
    UNIT_ASSERT_VALUES_EQUAL(history.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(history[0][dbqueries::columns::STATUS].as<std::string>(), "new");
    UNIT_ASSERT_VALUES_EQUAL(history[0][dbqueries::columns::SERVICE].as<std::string>(), "nmaps");
    UNIT_ASSERT_STRING_CONTAINS(history[0][dbqueries::columns::MESSAGE].as<std::string>(), "отсутствует e-mail адрес");
}

Y_UNIT_TEST(sync_samsara_link_tracker_new)
{
    auto globals = makeGlobals();

    {
        auto txn = globals.pool().masterWriteableTransaction();
        auto task = SAMPLE_TASK_ONE;
        task.integration.addServiceOrThrow(
            Service::Tracker,
            ServiceDesc{.serviceObjectId = ServiceObjectId("NMAPS-555")});
        putFeedbackTask(*txn, task);
        txn->commit();
    }

    sq::SamsaraLinkToTrackerParams actionParams{
        samsara::TicketStatus::Hold,
        samsara::TicketResolution::WaitForService,
        samsara::QUEUE_NEED_INFO_1L,
        "link message"
    };
    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::SamsaraLinkToTracker,
        SAMPLE_TASK_ONE,
        maps::json::Value::fromString(
            (maps::json::Builder() << actionParams).str())
    };

    bool handleTriggered = false;

    auto mockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/withArticles",
        [&handleTriggered](const maps::http::MockRequest& request) {
            handleTriggered = true;

            auto expectedBody = maps::json::Value::fromFile(
                SRC_("data/samsara_link_tracker.json"));
            auto body = samsara::tests::censorTimeDependentFields(request.body);
            UNIT_ASSERT_VALUES_EQUAL(expectedBody, maps::json::Value::fromString(body));
            return maps::http::MockResponse{R"({"entityId":5577})"};
        });

    {
        auto txn = globals.pool().masterWriteableTransaction();
        UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
        txn->commit();
    }

    UNIT_ASSERT(handleTriggered);
    {
        const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(SAMPLE_TASK_ONE.id))
            .exec(*globals.pool().slaveTransaction());
        UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);
        const auto& integration = tasks[0].integration;

        UNIT_ASSERT_VALUES_EQUAL(integration.services().size(), 0);
        auto samsaraHistory = integration.samsaraTicketsHistory();
        UNIT_ASSERT_VALUES_EQUAL(samsaraHistory.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(samsaraHistory.at(0).value(), "5577");
    }
}

Y_UNIT_TEST(sync_samsara_link_tracker_update)
{
    auto globals = makeGlobals();

    {
        auto txn = globals.pool().masterWriteableTransaction();
        auto task = SAMPLE_TASK_ONE;
        task.integration.addServiceOrThrow(
            Service::Tracker,
            ServiceDesc{.serviceObjectId = ServiceObjectId("NMAPS-555")});
        task.integration.addServiceOrThrow(
            Service::SupportNeedInfo,
            ServiceDesc{.serviceObjectId = ServiceObjectId("1213")});
        putFeedbackTask(*txn, task);
        txn->commit();
    }

    sq::SamsaraLinkToTrackerParams actionParams{
        samsara::TicketStatus::Hold,
        samsara::TicketResolution::WaitForService,
        samsara::QUEUE_NEED_INFO_1L,
        "link message"
    };
    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::SamsaraLinkToTracker,
        SAMPLE_TASK_ONE,
        maps::json::Value::fromString(
            (maps::json::Builder() << actionParams).str())
    };

    bool handleInfoTriggered = false;
    bool handleLinkTriggered = false;
    bool handleArticleTriggered = false;

    auto mockHandleLinks = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/1213/stLinks",
        [&handleLinkTriggered](const maps::http::MockRequest& request) {
            UNIT_ASSERT_VALUES_EQUAL(
                "stTicketId=NMAPS-555&updateSt=true",
                request.url.params());
            handleLinkTriggered = true;
            return maps::http::MockResponse::withStatus(200);
        });

    auto mockHandleInfo = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/1213/info",
        [&handleInfoTriggered](const maps::http::MockRequest& request) {
            handleInfoTriggered = true;
            auto expectedBody = R"({"status":"HOLD","resolution":"WAIT_FOR_SERVICE","queueId":100334})";
            UNIT_ASSERT_VALUES_EQUAL(expectedBody, request.body);
            return maps::http::MockResponse::withStatus(200);
        });

    auto mockHandleArticle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/1213/articles",
        [&handleArticleTriggered](const maps::http::MockRequest& request) {
            handleArticleTriggered = true;

            UNIT_ASSERT_VALUES_EQUAL(request.url.params(), "uniquenessKey=testKey_noteArticle");

            auto expectedBody = maps::json::Value::fromString(R"({
                "body": "link message",
                "type": "NOTE",
                "from": {
                    "login": "testLogin",
                    "name": "testName",
                    "email": "testEmail@yandex-team.ru"
                }
            })");
            UNIT_ASSERT_VALUES_EQUAL(expectedBody, maps::json::Value::fromString(request.body));

            return maps::http::MockResponse::withStatus(200);
        });

    {
        auto txn = globals.pool().masterWriteableTransaction();
        UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
        txn->commit();
    }

    UNIT_ASSERT(handleInfoTriggered);
    UNIT_ASSERT(handleLinkTriggered);
    UNIT_ASSERT(handleArticleTriggered);
    {
        const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(SAMPLE_TASK_ONE.id))
            .exec(*globals.pool().slaveTransaction());
        UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);
        const auto& integration = tasks[0].integration;

        UNIT_ASSERT_VALUES_EQUAL(integration.services().size(), 0);
        auto samsaraHistory = integration.samsaraTicketsHistory();
        UNIT_ASSERT_VALUES_EQUAL(samsaraHistory.size(), 1);
        UNIT_ASSERT_VALUES_EQUAL(samsaraHistory.at(0).value(), "1213");
    }
}

Y_UNIT_TEST(sync_tracker_create_issue)
{
    auto globals = makeGlobals();

    {
        auto txn = globals.pool().masterWriteableTransaction();
        putFeedbackTask(*txn, SAMPLE_TASK_ONE);
        txn->commit();
    }

    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::TrackerCreateIssue,
        SAMPLE_TASK_ONE,
        maps::json::Value::fromString(
            (maps::json::Builder() << [&] (json::ObjectBuilder b) {
                b["queueKey"] = "GEOCONTENTFB";
                b["component"] = 88067;
                b["description"] = "redirect message";
            }).str())
    };

    bool createIssueMockTriggered = false;
    auto createIssueMockHandle = maps::http::addMock(
        "https://st-api.yandex-team.ru/v2/issues",
        [&createIssueMockTriggered](const maps::http::MockRequest& request) {
            UNIT_ASSERT_EQUAL(request.header("Authorization"), "OAuth st-token");
            UNIT_ASSERT_EQUAL(request.url.params(), "");

            auto expectedBody = maps::json::Value::fromString(R"({
                "queue":"GEOCONTENTFB",
                "description":"redirect message",
                "summary":"Redirected from Nmaps",
                "components":[88067]
            })");
            UNIT_ASSERT_VALUES_EQUAL(
                maps::json::Value::fromString(request.body), expectedBody);

            createIssueMockTriggered = true;
            return maps::http::MockResponse{R"({"key": "GEOCONTENTFB-1"})"};
        });

    {
        auto txn = globals.pool().masterWriteableTransaction();
        UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
        txn->commit();
    }

    UNIT_ASSERT(createIssueMockTriggered);
    {
        const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(SAMPLE_TASK_ONE.id))
            .exec(*globals.pool().slaveTransaction());
        UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);
        const auto& integration = tasks[0].integration;

        UNIT_ASSERT_VALUES_EQUAL(integration.services().size(), 1);
        UNIT_ASSERT(integration.hasService(Service::Tracker));
        auto serviceDesc = integration.at(Service::Tracker);
        UNIT_ASSERT(serviceDesc.serviceObjectId);
        UNIT_ASSERT_VALUES_EQUAL(serviceDesc.serviceObjectId->value(), "GEOCONTENTFB-1");
    }
}

Y_UNIT_TEST(sync_samsara_close_ticket)
{
    auto globals = makeGlobals();

    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::SamsaraCloseTicket,
        SAMPLE_TASK_ONE,
        maps::json::Value::fromString(
            (maps::json::Builder() << [&] (json::ObjectBuilder b) {
                b["ticket_id"] = "12345";
                b["resolution"] = std::string{toString(samsara::TicketResolution::Rejected)};
            }).str())
    };

    bool closeTicketMockTriggered = false;
    auto createTicketMockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/12345/info",
        [&closeTicketMockTriggered](const maps::http::MockRequest& request) {
            closeTicketMockTriggered = true;

            auto body = samsara::tests::censorTimeDependentFields(request.body);

            auto expectedBody = R"({"status":"CLOSED","resolution":"REJECTED"})";
            UNIT_ASSERT_VALUES_EQUAL(
                maps::json::Value::fromString(body),
                maps::json::Value::fromString(expectedBody));

            return maps::http::MockResponse::withStatus(200);
        });

    {
        auto txn = globals.pool().masterWriteableTransaction();
        UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
        txn->commit();
    }

    UNIT_ASSERT(closeTicketMockTriggered);
}

Y_UNIT_TEST(sync_samsara_update_ticket_create_ticket)
{
    auto globals = makeGlobals();

    {
        auto txn = globals.pool().masterWriteableTransaction();
        putFeedbackTask(*txn, SAMPLE_TASK_ONE);
        txn->commit();
    }

    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::SamsaraUpdateTicket,
        SAMPLE_TASK_ONE,
        maps::json::Value::fromString(
            (maps::json::Builder() << sq::SamsaraUpdateTicketParams(
                samsara::QUEUE_NEED_INFO_1L,
                "test article message",
                true, // keepIntegration
                false // forceSamsaraQueue
            )).str())
    };

    bool createTicketWithArticlesMockTriggered = false;
    auto createTicketMockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/withArticles",
        [&createTicketWithArticlesMockTriggered](const maps::http::MockRequest& request) {
            createTicketWithArticlesMockTriggered = true;

            auto body = samsara::tests::censorTimeDependentFields(request.body);

            auto expectedBody = maps::json::Value::fromFile(
                SRC_("data/create_ticket_request.json"));
            UNIT_ASSERT_VALUES_EQUAL(
                maps::json::Value::fromString(body), expectedBody);

            return maps::http::MockResponse{R"({"entityId":12345})"};
        });

    {
        auto txn = globals.pool().masterWriteableTransaction();
        UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
        txn->commit();
    }

    UNIT_ASSERT(createTicketWithArticlesMockTriggered);

    const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(SAMPLE_TASK_ONE.id))
        .exec(*globals.pool().slaveTransaction());
    UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);

    UNIT_ASSERT_VALUES_EQUAL(tasks[0].serviceObjectId.value(), "12345");
    UNIT_ASSERT_VALUES_EQUAL(
        tasks[0].serviceObjectUrl,
        globals.samsaraClient.makeTicketUrl(ServiceObjectId{"12345"}));

    const auto& integration = tasks[0].integration;
    UNIT_ASSERT_VALUES_EQUAL(integration.services().size(), 1);
    UNIT_ASSERT(integration.hasService(Service::SupportNeedInfo));
    auto serviceDesc = integration.at(Service::SupportNeedInfo);
    UNIT_ASSERT(serviceDesc.serviceObjectId);
    UNIT_ASSERT_VALUES_EQUAL(serviceDesc.serviceObjectId->value(), "12345");
    UNIT_ASSERT(integration.samsaraTicketsHistory().empty());
}

Y_UNIT_TEST(sync_samsara_update_ticket_create_ticket_invalid_email_retry)
{
    auto globals = makeGlobals();

    {
        auto txn = globals.pool().masterWriteableTransaction();
        putFeedbackTask(*txn, SAMPLE_TASK_ONE);
        txn->commit();
    }

    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::SamsaraUpdateTicket,
        SAMPLE_TASK_ONE,
        maps::json::Value::fromString(
            (maps::json::Builder() << sq::SamsaraUpdateTicketParams(
                samsara::QUEUE_NEED_INFO_1L,
                "test article message",
                true, // keepIntegration
                false // forceSamsaraQueue
            )).str())
    };

    std::stringstream logString;
    auto createTicketMockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/withArticles",
        [&](const maps::http::MockRequest& request) {
            auto body = samsara::tests::censorTimeDependentFields(request.body);
            auto bodyJson = maps::json::Value::fromString(body);

            bool hasEmail = false;
            for (const auto& article : bodyJson["articles"]) {
                if (article["type"].as<std::string>() == "IN" &&
                    article["from"]["email"].exists())
                {
                    hasEmail = true;
                    break;
                }
            }

            if (hasEmail) {
                logString << "HasEmail ";

                auto expectedBody = maps::json::Value::fromFile(
                    SRC_("data/create_ticket_request.json"));
                UNIT_ASSERT_VALUES_EQUAL(bodyJson, expectedBody);

                auto response = maps::http::MockResponse{R"(Failed to validate email)"};
                response.status = 400;
                return response;
            } else {
                logString << "NoEmail ";

                auto expectedBody = maps::json::Value::fromFile(
                    SRC_("data/create_ticket_request_no_email.json"));
                UNIT_ASSERT_VALUES_EQUAL(bodyJson, expectedBody);

                return maps::http::MockResponse{R"({"entityId":12345})"};
            }
        });

    {
        auto txn = globals.pool().masterWriteableTransaction();
        UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
        txn->commit();
    }

    UNIT_ASSERT_VALUES_EQUAL(logString.str(), "HasEmail NoEmail ");

    const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(SAMPLE_TASK_ONE.id))
        .exec(*globals.pool().slaveTransaction());
    UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);

    UNIT_ASSERT_VALUES_EQUAL(tasks[0].serviceObjectId.value(), "12345");
    UNIT_ASSERT_VALUES_EQUAL(
        tasks[0].serviceObjectUrl,
        globals.samsaraClient.makeTicketUrl(ServiceObjectId{"12345"}));

    const auto& integration = tasks[0].integration;
    UNIT_ASSERT_VALUES_EQUAL(integration.services().size(), 1);
    UNIT_ASSERT(integration.hasService(Service::SupportNeedInfo));
    auto serviceDesc = integration.at(Service::SupportNeedInfo);
    UNIT_ASSERT(serviceDesc.serviceObjectId);
    UNIT_ASSERT_VALUES_EQUAL(serviceDesc.serviceObjectId->value(), "12345");
    UNIT_ASSERT(integration.samsaraTicketsHistory().empty());
}

Y_UNIT_TEST(sync_samsara_update_ticket_reopen_ticket)
{
    auto task = SAMPLE_TASK_ONE;
    task.integration.addServiceOrThrow(
        Service::SupportNeedInfo,
        ServiceDesc{
            ServiceObjectId{"12345"},
            std::nullopt,
            std::nullopt
        });

    auto globals = makeGlobals();

    {
        auto txn = globals.pool().masterWriteableTransaction();
        putFeedbackTask(*txn, task);
        txn->commit();
    }

    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::SamsaraUpdateTicket,
        task,
        maps::json::Value::fromString(
            (maps::json::Builder() << sq::SamsaraUpdateTicketParams(
                samsara::QUEUE_NEED_INFO_1L,
                "test article message",
                true, // keepIntegration
                false // forceSamsaraQueue
            )).str())
    };

    bool updateTicketMockTriggered = false;
    auto updateTicketMockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/12345/info",
        [&updateTicketMockTriggered](const maps::http::MockRequest& request) {
            updateTicketMockTriggered = true;
            auto expectedBody = R"({"status":"OPEN","resolution":"NEW"})";
            UNIT_ASSERT_VALUES_EQUAL(request.body, expectedBody);
            return maps::http::MockResponse::withStatus(200);
        });

    bool addArticleMockTriggered = false;
    auto addArticleMockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/12345/articles",
        [&addArticleMockTriggered](const maps::http::MockRequest& request) {
            addArticleMockTriggered = true;

            UNIT_ASSERT_VALUES_EQUAL(request.url.params(), "uniquenessKey=testKey_article0");

            auto expectedBody = maps::json::Value::fromFile(
                SRC_("data/article_request.json"));
            UNIT_ASSERT_VALUES_EQUAL(
                maps::json::Value::fromString(request.body), expectedBody);

            return maps::http::MockResponse::withStatus(200);
        });


    auto txn = globals.pool().masterWriteableTransaction();
    UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
    txn->commit();

    UNIT_ASSERT(updateTicketMockTriggered);
    UNIT_ASSERT(addArticleMockTriggered);


    const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(SAMPLE_TASK_ONE.id))
        .exec(*globals.pool().slaveTransaction());
    UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);
    const auto& integration = tasks[0].integration;

    UNIT_ASSERT_VALUES_EQUAL(integration.services().size(), 1);
    UNIT_ASSERT(integration.hasService(Service::SupportNeedInfo));
    auto serviceDesc = integration.at(Service::SupportNeedInfo);
    UNIT_ASSERT(serviceDesc.serviceObjectId);
    UNIT_ASSERT_VALUES_EQUAL(serviceDesc.serviceObjectId->value(), "12345");
    UNIT_ASSERT(integration.samsaraTicketsHistory().empty());
}

Y_UNIT_TEST(sync_samsara_update_ticket_patch_queue)
{
    auto task = SAMPLE_TASK_ONE;
    task.integration.addServiceOrThrow(
        Service::SupportNeedInfo,
        ServiceDesc{
            ServiceObjectId{"12345"},
            std::nullopt,
            std::nullopt
        });

    auto globals = makeGlobals();

    {
        auto txn = globals.pool().masterWriteableTransaction();
        putFeedbackTask(*txn, task);
        txn->commit();
    }

    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::SamsaraUpdateTicket,
        task,
        maps::json::Value::fromString(
            (maps::json::Builder() << sq::SamsaraUpdateTicketParams(
                samsara::QUEUE_NEED_INFO_1L,
                "test article message",
                true, // keepIntegration
                true // forceSamsaraQueue
            )).str())
    };

    bool updateTicketMockTriggered = false;
    auto updateTicketMockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/12345/info",
        [&updateTicketMockTriggered](const maps::http::MockRequest& request) {
            updateTicketMockTriggered = true;
            auto expectedBody = R"({"status":"OPEN","resolution":"NEW","queueId":100334})";
            UNIT_ASSERT_VALUES_EQUAL(request.body, expectedBody);
            return maps::http::MockResponse::withStatus(200);
        });

    bool addArticleMockTriggered = false;
    auto addArticleMockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/12345/articles",
        [&addArticleMockTriggered](const maps::http::MockRequest& request) {
            addArticleMockTriggered = true;

            UNIT_ASSERT_VALUES_EQUAL(request.url.params(), "uniquenessKey=testKey_article0");

            auto expectedBody = maps::json::Value::fromFile(
                SRC_("data/article_request.json"));
            UNIT_ASSERT_VALUES_EQUAL(
                maps::json::Value::fromString(request.body), expectedBody);

            return maps::http::MockResponse::withStatus(200);
        });


    auto txn = globals.pool().masterWriteableTransaction();
    UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
    txn->commit();

    UNIT_ASSERT(updateTicketMockTriggered);
    UNIT_ASSERT(addArticleMockTriggered);


    const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(SAMPLE_TASK_ONE.id))
        .exec(*globals.pool().slaveTransaction());
    UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);
    const auto& integration = tasks[0].integration;

    UNIT_ASSERT_VALUES_EQUAL(integration.services().size(), 1);
    UNIT_ASSERT(integration.hasService(Service::SupportNeedInfo));
    auto serviceDesc = integration.at(Service::SupportNeedInfo);
    UNIT_ASSERT(serviceDesc.serviceObjectId);
    UNIT_ASSERT_VALUES_EQUAL(serviceDesc.serviceObjectId->value(), "12345");
    UNIT_ASSERT(integration.samsaraTicketsHistory().empty());
}

Y_UNIT_TEST(sync_samsara_update_ticket_to_history)
{
    auto task = SAMPLE_TASK_ONE;
    task.integration.addServiceOrThrow(
        Service::SupportNeedInfo,
        ServiceDesc{
            ServiceObjectId{"12345"},
            std::nullopt,
            std::nullopt
        });

    auto globals = makeGlobals();

    {
        auto txn = globals.pool().masterWriteableTransaction();
        putFeedbackTask(*txn, task);
        txn->commit();
    }

    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::SamsaraUpdateTicket,
        task,
        maps::json::Value::fromString(
            (maps::json::Builder() << sq::SamsaraUpdateTicketParams(
                samsara::QUEUE_NEED_INFO_1L,
                "test article message",
                false, // keepIntegration
                false // forceSamsaraQueue
            )).str())
    };

    bool updateTicketMockTriggered = false;
    auto updateTicketMockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/12345/info",
        [&updateTicketMockTriggered](const maps::http::MockRequest& request) {
            updateTicketMockTriggered = true;
            auto expectedBody = R"({"status":"OPEN","resolution":"NEW"})";
            UNIT_ASSERT_VALUES_EQUAL(request.body, expectedBody);
            return maps::http::MockResponse::withStatus(200);
        });

    bool addArticleMockTriggered = false;
    auto addArticleMockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/12345/articles",
        [&addArticleMockTriggered](const maps::http::MockRequest& request) {
            addArticleMockTriggered = true;

            UNIT_ASSERT_VALUES_EQUAL(request.url.params(), "uniquenessKey=testKey_article0");

            auto expectedBody = maps::json::Value::fromFile(
                SRC_("data/article_request.json"));
            UNIT_ASSERT_VALUES_EQUAL(
                maps::json::Value::fromString(request.body), expectedBody);

            return maps::http::MockResponse::withStatus(200);
        });

    auto txn = globals.pool().masterWriteableTransaction();
    UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
    txn->commit();

    UNIT_ASSERT(updateTicketMockTriggered);
    UNIT_ASSERT(addArticleMockTriggered);


    const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(SAMPLE_TASK_ONE.id))
        .exec(*globals.pool().slaveTransaction());
    UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);
    const auto& integration = tasks[0].integration;

    UNIT_ASSERT_VALUES_EQUAL(integration.services().size(), 0);
    UNIT_ASSERT_VALUES_EQUAL(integration.samsaraTicketsHistory().size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(integration.samsaraTicketsHistory()[0].value(), "12345");
}

Y_UNIT_TEST(sync_samsara_update_ticket_create_ticket_into_history)
{
    auto globals = makeGlobals();

    {
        auto txn = globals.pool().masterWriteableTransaction();
        putFeedbackTask(*txn, SAMPLE_TASK_ONE);
        txn->commit();
    }

    sq::ActionInfo actionInfo{
        sq::OrderId{0},
        sq::SyncAction::SamsaraUpdateTicket,
        SAMPLE_TASK_ONE,
        maps::json::Value::fromString(
            (maps::json::Builder() << sq::SamsaraUpdateTicketParams(
                samsara::QUEUE_NEED_INFO_1L,
                "test article message",
                false, // keepIntegration
                false // forceSamsaraQueue
            )).str())
    };

    bool createTicketWithArticlesMockTriggered = false;
    auto createTicketMockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/withArticles",
        [&createTicketWithArticlesMockTriggered](const maps::http::MockRequest& request) {
            createTicketWithArticlesMockTriggered = true;

            auto body = samsara::tests::censorTimeDependentFields(request.body);

            auto expectedBody = maps::json::Value::fromFile(
                SRC_("data/create_ticket_request.json"));
            UNIT_ASSERT_VALUES_EQUAL(
                maps::json::Value::fromString(body), expectedBody);

            return maps::http::MockResponse{R"({"entityId":12345})"};
        });

    {
        auto txn = globals.pool().masterWriteableTransaction();
        UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
        txn->commit();
    }

    UNIT_ASSERT(createTicketWithArticlesMockTriggered);

    const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(SAMPLE_TASK_ONE.id))
        .exec(*globals.pool().slaveTransaction());
    UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);

    UNIT_ASSERT_VALUES_EQUAL(tasks[0].serviceObjectId.value(), "12345");
    UNIT_ASSERT_VALUES_EQUAL(
        tasks[0].serviceObjectUrl,
        globals.samsaraClient.makeTicketUrl(ServiceObjectId{"12345"}));

    const auto& integration = tasks[0].integration;
    UNIT_ASSERT_VALUES_EQUAL(integration.services().size(), 0);
    UNIT_ASSERT_VALUES_EQUAL(integration.samsaraTicketsHistory().size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(integration.samsaraTicketsHistory()[0].value(), "12345");
}

Y_UNIT_TEST(sync_samsara_add_note)
{
    auto globals = makeGlobals();

    sq::ActionInfo actionInfo{
        sq::OrderId{0},
            sq::SyncAction::SamsaraAddNote,
            SAMPLE_TASK_ONE,
            maps::json::Value::fromString(
                (maps::json::Builder() << [&] (json::ObjectBuilder b) {
                    b["ticket_id"] = "12345";
                    b["message"] = "test article message";
                }).str())
    };

    bool addArticleMockTriggered = false;
    auto addArticleMockHandle = maps::http::addMock(
        "https://test-api.samsara.yandex-team.ru/api/v2/tickets/12345/articles",
        [&addArticleMockTriggered](const maps::http::MockRequest& request) {
            addArticleMockTriggered = true;

            UNIT_ASSERT_VALUES_EQUAL(request.url.params(), "uniquenessKey=testKey_noteArticle");

            auto expectedBody = maps::json::Value::fromFile(
                SRC_("data/article_request.json"));
            UNIT_ASSERT_VALUES_EQUAL(
                maps::json::Value::fromString(request.body), expectedBody);

            return maps::http::MockResponse::withStatus(200);
        });

    auto txn = globals.pool().masterWriteableTransaction();
    UNIT_ASSERT_NO_EXCEPTION(processAction(globals, *txn, globals.keyMaker, actionInfo));
    txn->commit();

    UNIT_ASSERT(addArticleMockTriggered);
}

Y_UNIT_TEST(sync_ugc_contribution)
{
    auto globals = makeGlobals();

    auto mockAvatars = maps::http::addMock(
        "http://avatarsInt/getimageinfo-maps-feedback/1895250/2a000001740a793daaec2195b31b1a6c01af",
        [] (const maps::http::MockRequest&) {
            return maps::common::readFileToString(SRC_("data/imageInfo.json"));
        });

    GeosearchMock geosearchMock({
        {geolib3::Point2{37.37, 55.55}, "en_UA", SRC_("data/3755.pb")},
        {geolib3::Point2{37.37, 55.55}, "en_RU", SRC_("data/3755.pb")},
        {geolib3::Point2{37.37, 55.55}, "ru_RU", SRC_("data/3755.pb")},
        {geolib3::Point2{37.37, 55.55}, "ru_UA", SRC_("data/3755.pb")},
    });

    bool mockUgcBackofficeTriggered = false;
    auto mockUgcBackoffice = maps::http::addMock(
        "http://ugcBackoffice/v1/contributions/modify",
        [&mockUgcBackofficeTriggered](const maps::http::MockRequest& request) {
            mockUgcBackofficeTriggered = true;
            UNIT_ASSERT_VALUES_EQUAL(request.url.params(), "uid=42");
            return maps::http::MockResponse();
        });

    {
        auto txn = globals.pool().masterWriteableTransaction();
        insertTaskInSyncQueue(*txn, SAMPLE_TASK_ONE, sq::SyncAction::UgcContribution);
        txn->commit();
    }

    UNIT_ASSERT_VALUES_EQUAL(syncQueueSize(*globals.pool().slaveTransaction()), 1);

    UNIT_ASSERT_NO_EXCEPTION(syncFeedback(globals));
    UNIT_ASSERT(mockUgcBackofficeTriggered);

    UNIT_ASSERT_VALUES_EQUAL(syncQueueSize(*globals.pool().slaveTransaction()), 0);
}

Y_UNIT_TEST(sync_sprav_create)
{
    auto globals = makeGlobals();

    auto mockAvatars = maps::http::addMock(
        "http://sprav/post",
        [] (const maps::http::MockRequest&) {
            maps::http::MockResponse response;
            response.body = R"({ "unique_id" : "42" })";
            response.status = 201;
            return response;
        });


    {
        auto txn = globals.pool().masterWriteableTransaction();
        putFeedbackTask(*txn, SAMPLE_TASK_ONE);
        insertTaskInSyncQueue(
            *txn,
            SAMPLE_TASK_ONE,
            sq::SyncAction::SpravCreate,
            R"({"cookie": "cookie", "user_agent": "user_agent", "fingerprint": "fingerprint"})"
        );
        txn->commit();
    }

    UNIT_ASSERT_VALUES_EQUAL(syncQueueSize(*globals.pool().slaveTransaction()), 1);

    UNIT_ASSERT_NO_EXCEPTION(syncFeedback(globals));

    UNIT_ASSERT_VALUES_EQUAL(syncQueueSize(*globals.pool().slaveTransaction()), 0);

    const auto tasks = fbqb::SelectQuery(fbqb::WhereConditions().id(SAMPLE_TASK_ONE.id))
        .exec(*globals.pool().slaveTransaction());
    UNIT_ASSERT_VALUES_EQUAL(tasks.size(), 1);
    const auto& integration = tasks[0].integration;
    UNIT_ASSERT(integration.hasService(Service::Sprav));
    UNIT_ASSERT(integration.at(Service::Sprav).serviceObjectId);
    UNIT_ASSERT_VALUES_EQUAL(integration.at(Service::Sprav).serviceObjectId->value(), "42");
    UNIT_ASSERT_VALUES_EQUAL(tasks[0].service, Service::Sprav);
    UNIT_ASSERT_VALUES_EQUAL(tasks[0].serviceObjectId.value(), "42");
    UNIT_ASSERT(integration.samsaraTicketsHistory().empty());
}

Y_UNIT_TEST(sync_send_email_toponym)
{
    auto globals = makeGlobals();

    GeosearchMock geosearchMock({
        {geolib3::Point2{37.37, 55.55}, "ru_RU", SRC_("data/3755.pb")}
    });
    SenderMock senderMock(
        "8IFRGQG4-50U",
        maps::json::Value::fromFile(SRC_("data/sender_toponym_request.json")));

    {
        auto txn = globals.pool().masterWriteableTransaction();
        putFeedbackTask(*txn, SAMPLE_TASK_ONE);
        insertTaskInSyncQueue(
            *txn,
            SAMPLE_TASK_ONE,
            sq::SyncAction::SenderSendEmail,
            R"({
                "mail_type": "toponym_new",
                "locale": "ru_RU",
                "email": "sample@yandex.ru",
                "comment": "Комментарий.",
                "form_point": {
                    "lon": 37.37,
                    "lat": 55.55
                }
            })"
        );
        txn->commit();
    }

    UNIT_ASSERT_VALUES_EQUAL(syncQueueSize(*globals.pool().slaveTransaction()), 1);
    UNIT_ASSERT_NO_EXCEPTION(syncFeedback(globals));
    UNIT_ASSERT_VALUES_EQUAL(syncQueueSize(*globals.pool().slaveTransaction()), 0);

    UNIT_ASSERT(senderMock.isTriggered);
}

Y_UNIT_TEST(sync_send_email_route)
{
    auto globals = makeGlobals();

    GeosearchMock geosearchMock({
        {geolib3::Point2{60.621843000,56.836465000}, "en_US", SRC_("data/route_start_geosearch.pb")},
        {geolib3::Point2{60.601472000,56.828003000}, "en_US", SRC_("data/route_end_geosearch.pb")},
    });

    SenderMock senderMock(
        "OI7BJQG4-BBN1",
        maps::json::Value::fromFile(SRC_("data/sender_route_request.json")));

    std::ifstream input(SRC_("data/route.txt"));
    std::string route;
    input >> route;

    {
        auto txn = globals.pool().masterWriteableTransaction();
        FeedbackTask task{SAMPLE_TASK_ONE};
        task.originalTask.setFormId(FormId::Route);
        task.originalTask.setAnswerId(AnswerId::ReportRoute);
        task.originalTask.setQuestionId(QuestionId::WrongRoute);

        putFeedbackTask(*txn, task);
        insertTaskInSyncQueue(
            *txn,
            task,
            sq::SyncAction::SenderSendEmail,
            R"({
                "mail_type": "route_published",
                "locale": "en_US",
                "email": "sample@yandex.com",
                "comment": "Comment.",
                "form_point": {
                    "lon": 37.37,
                    "lat": 55.55
                },
                "route_encoded_points": ")" + route + "\"}");
        txn->commit();
    }

    UNIT_ASSERT_VALUES_EQUAL(syncQueueSize(*globals.pool().slaveTransaction()), 1);
    UNIT_ASSERT_NO_EXCEPTION(syncFeedback(globals));
    UNIT_ASSERT_VALUES_EQUAL(syncQueueSize(*globals.pool().slaveTransaction()), 0);

    UNIT_ASSERT(senderMock.isTriggered);
}

} // test_sync suite

} // namespace maps::wiki::feedback::api::sync::tests
