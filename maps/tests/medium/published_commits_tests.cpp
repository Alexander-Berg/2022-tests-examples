#include "helpers.h"
#include <maps/wikimap/mapspro/libs/social/magic_strings.h>

#include <yandex/maps/wiki/social/published_commits.h>

namespace maps::wiki::social::tests {

namespace {

const std::string HANDLER_A = "handler_a";

const TId COMMIT_A = 1;
const TId COMMIT_B = 2;
const TId COMMIT_C = 3;
const TId COMMIT_D = 4;

}  // namespace

class PcgTest : public PublishedCommits {
public:
    explicit PcgTest(pqxx::transaction_base& txn) : PublishedCommits(txn) {}

    void pushHandlers(const TIds& commitIds, const std::set<std::string>& handlers) {
        push(commitIds, handlers);
    }
};

Y_UNIT_TEST_SUITE(published_commits) {

Y_UNIT_TEST_F(smoke, DbFixture) {
    pqxx::work txn(conn);
    PcgTest pcg(txn);

    auto initialUnprocessed = pcg.getUnprocessedIds(commits_handlers::RELEASES_NOTIFICATION);
    UNIT_ASSERT_VALUES_EQUAL(initialUnprocessed.size(), 0);

    pcg.push({COMMIT_A});
    auto defaultFanoutRnw = pcg.getUnprocessedIds(commits_handlers::RELEASES_NOTIFICATION);
    auto defaultFanoutOther = pcg.getUnprocessedIds(HANDLER_A);
    UNIT_ASSERT_VALUES_EQUAL(defaultFanoutRnw.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(defaultFanoutRnw.count(COMMIT_A), 1);
    UNIT_ASSERT_VALUES_EQUAL(defaultFanoutOther.size(), 0);

    pcg.process({COMMIT_A}, commits_handlers::RELEASES_NOTIFICATION);
    auto afterProcess = pcg.getUnprocessedIds(commits_handlers::RELEASES_NOTIFICATION);
    UNIT_ASSERT_VALUES_EQUAL(afterProcess.size(), 0);

    pcg.pushHandlers({COMMIT_B, COMMIT_C, COMMIT_D}, {commits_handlers::RELEASES_NOTIFICATION, HANDLER_A});
    auto multiPushRnw = pcg.getUnprocessedIds(commits_handlers::RELEASES_NOTIFICATION);
    auto multiPushOther = pcg.getUnprocessedIds(HANDLER_A);
    UNIT_ASSERT_VALUES_EQUAL(multiPushRnw.size(), 3);
    UNIT_ASSERT_VALUES_EQUAL(multiPushRnw.count(COMMIT_B), 1);
    UNIT_ASSERT_VALUES_EQUAL(multiPushRnw.count(COMMIT_C), 1);
    UNIT_ASSERT_VALUES_EQUAL(multiPushRnw.count(COMMIT_D), 1);
    UNIT_ASSERT_VALUES_EQUAL(multiPushOther.size(), 3);
    UNIT_ASSERT_VALUES_EQUAL(multiPushOther.count(COMMIT_B), 1);
    UNIT_ASSERT_VALUES_EQUAL(multiPushOther.count(COMMIT_C), 1);
    UNIT_ASSERT_VALUES_EQUAL(multiPushOther.count(COMMIT_D), 1);

    pcg.process({COMMIT_B, COMMIT_D}, commits_handlers::RELEASES_NOTIFICATION);
    pcg.process({COMMIT_C}, HANDLER_A);
    auto afterPartialProcessRnw = pcg.getUnprocessedIds(commits_handlers::RELEASES_NOTIFICATION);
    auto afterPartialProcessOther = pcg.getUnprocessedIds(HANDLER_A);
    UNIT_ASSERT_VALUES_EQUAL(afterPartialProcessRnw.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(afterPartialProcessRnw.count(COMMIT_C), 1);
    UNIT_ASSERT_VALUES_EQUAL(afterPartialProcessOther.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(afterPartialProcessOther.count(COMMIT_B), 1);
    UNIT_ASSERT_VALUES_EQUAL(afterPartialProcessOther.count(COMMIT_D), 1);

    pcg.process({COMMIT_C}, commits_handlers::RELEASES_NOTIFICATION);
    pcg.process({COMMIT_B, COMMIT_D}, HANDLER_A);
    auto afterFinalProcessRnw = pcg.getUnprocessedIds(commits_handlers::RELEASES_NOTIFICATION);
    UNIT_ASSERT_VALUES_EQUAL(afterFinalProcessRnw.size(), 0);
    auto afterFinalProcessOther = pcg.getUnprocessedIds(HANDLER_A);
    UNIT_ASSERT_VALUES_EQUAL(afterFinalProcessOther.size(), 0);
}

}

}  // maps::wiki::social::tests
