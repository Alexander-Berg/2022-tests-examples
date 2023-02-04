#include <yandex/maps/wiki/social/gateway.h>

#include <maps/libs/common/include/exception.h>
#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/mapspro/libs/social/helpers.h>
#include <maps/wikimap/mapspro/libs/social/magic_strings.h>

#include <maps/wikimap/mapspro/libs/social/tests/medium/helpers.h>
#include <maps/wikimap/mapspro/libs/social/tests/helpers/event_creator.h>

namespace maps::wiki::social::tests {

using namespace std::chrono_literals;

namespace {

const TUid TEST_UID = 111;
const TId FAKE_ID = 123;
const auto LOCAL_IP = "127.0.0.1";
const uint16_t PORT = 12345;

} // namespace

Y_UNIT_TEST_SUITE(user_activity_suite) {

Y_UNIT_TEST_F(user_activity, DbFixture)
{
    using namespace comments;

    const std::string DATA = "'test comment \"abcdd\"' !11";
    const TId TEST_AOI_ID = 1234;
    const TId TEST_COMMIT_ID = 100500;
    const TId TEST_OBJECT_ID = 12345;
    const TUid OTHER_TEST_UID = 123;
    const TIds NO_AOI_IDS;
    const std::vector<std::chrono::seconds> TIME_INTERVALS = {
        std::chrono::seconds(1),
        std::chrono::seconds(3),
        std::chrono::seconds(5),
        std::chrono::seconds(7),
        std::chrono::seconds(9)};

    {
        pqxx::work txn(conn);
        Gateway gateway(txn);

        for (int i = 0; i < 3; ++i) {

            auto commentId = gateway.createComment(
                TEST_UID, CommentType::Info, DATA,
                TEST_COMMIT_ID, TEST_OBJECT_ID,
                ANY_FEEDBACK_TASK_ID,
                NO_AOI_IDS).id(); // comment created by test user

            gateway.createComment(
                OTHER_TEST_UID, CommentType::Info, DATA,
                TEST_COMMIT_ID, TEST_OBJECT_ID,
                ANY_FEEDBACK_TASK_ID,
                NO_AOI_IDS); // comment created by other test user

            EventCreator(txn)
                .uid(TEST_UID)
                .commitId(i)
                .action("object_created")
                .aoiIds({TEST_AOI_ID})
                .create();

            txn.exec(
                "UPDATE " + sql::table::COMMENT +
                " SET " + sql::col::CREATED_AT +
                "= (" + sql::value::NOW + "- INTERVAL '" + std::to_string(2 * i + 2) + " sec') " +
                "WHERE " + sql::col::ID + "=" + std::to_string(commentId));

            txn.exec(
                "UPDATE " + sql::table::COMMIT_EVENT +
                " SET " + sql::col::CREATED_AT +
                "= (" + sql::value::NOW + "- INTERVAL '" + std::to_string(2 * i + 2) + " sec') " +
                "WHERE " + sql::col::COMMIT_ID + "=" + std::to_string(i));
        } // creating comments and objects on seconds 0, 2, 4

        txn.commit();
    }

    {
        pqxx::work txn(conn);
        Gateway gtw(txn);

        auto activity = gtw.getUserActivity(TEST_UID, TIME_INTERVALS, ActivityType::Comments); // happens on second 6
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(1)), 0);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(3)), 1);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(5)), 2);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(7)), 3);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(9)), 3);

        activity = gtw.getUserActivity(TEST_UID, TIME_INTERVALS, ActivityType::Edits);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(1)), 0);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(3)), 1);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(5)), 2);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(7)), 3);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(9)), 3);

        activity = gtw.getUserActivity(TEST_UID, TIME_INTERVALS, ActivityType::FeedbackResolve);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(1)), 0);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(3)), 0);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(5)), 0);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(7)), 0);
        UNIT_ASSERT_EQUAL(activity.at(std::chrono::seconds(9)), 0);

        UNIT_ASSERT_NO_EXCEPTION(gtw.saveUserActivityAlert(TEST_UID, "test alert reason 1"));
        UNIT_ASSERT_NO_EXCEPTION(gtw.saveUserActivityAlert(TEST_UID, "test alert reason 2"));
        UNIT_ASSERT_NO_EXCEPTION(gtw.saveUserActivityAlert(TEST_UID, "test alert reason 3"));
    }
}

Y_UNIT_TEST_F(invalid_ip, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gtw(txn);

    UNIT_ASSERT_EXCEPTION(
        gtw.saveUserActivity(TEST_UID, "", PORT, UserActivityAction::CreateCommit, FAKE_ID),
        maps::RuntimeError);
}

Y_UNIT_TEST_F(save_all_actions, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gtw(txn);

    for (auto action : enum_io::enumerateValues<UserActivityAction>()) {
        UNIT_ASSERT_NO_EXCEPTION(
            gtw.saveUserActivity(TEST_UID, LOCAL_IP, PORT, action, FAKE_ID));
    }
}

Y_UNIT_TEST_F(save_all_actions_no_id, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gtw(txn);

    for (auto action : enum_io::enumerateValues<UserActivityAction>()) {
        UNIT_ASSERT_NO_EXCEPTION(
            gtw.saveUserActivity(TEST_UID, LOCAL_IP, PORT, action, std::nullopt));
    }
}

Y_UNIT_TEST_F(save_all_actions_no_id_port, DbFixture)
{
    pqxx::work txn(conn);
    Gateway gtw(txn);

    for (auto action : enum_io::enumerateValues<UserActivityAction>()) {
        UNIT_ASSERT_NO_EXCEPTION(
            gtw.saveUserActivity(TEST_UID, LOCAL_IP, std::nullopt, action, std::nullopt));
    }
}

} // user_activity_suite

} // namespace maps::wiki::social::tests
