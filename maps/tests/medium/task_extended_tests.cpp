#include <library/cpp/testing/unittest/registar.h>
#include <maps/wikimap/mapspro/libs/social_serv_serialize/tests/medium/db_fixture.h>
#include <maps/wikimap/mapspro/libs/social_serv_serialize/include/task_extended.h>
#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/attribute_names.h>
#include <maps/wikimap/mapspro/libs/social/tests/helpers/fb_task_creator.h>

namespace maps::wiki::socialsrv::serialize::tests {

namespace sf = social::feedback;
using sf::tests::FbTaskCreator;

namespace {

static const social::TUid USER_ID = 12;

} // namespace

Y_UNIT_TEST_SUITE(task_extended)
{

Y_UNIT_TEST_F(constructor_test, DbFixture)
{
    // create feedback
    pqxx::work txn(conn);
    auto task = FbTaskCreator(txn, sf::TaskState::Opened)
        .objectUri("object uri value")
        .create();

    // create TaskExtended
    TaskForUI taskForUi(
        std::move(task),
        sf::History{},
        SubstitutionStrategy::None,
        {},
        {},
        USER_ID
    );
    TaskExtended taskExtended(taskForUi);

    // check
    UNIT_ASSERT(taskExtended.objectUri.has_value());
    UNIT_ASSERT_VALUES_EQUAL(taskExtended.objectUri.value(), "object uri value");
}

} // Y_UNIT_TEST_SUITE(task_extended)

} //maps::wiki::socialsrv::serialize::tests
