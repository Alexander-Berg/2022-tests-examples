#include <maps/wikimap/ugc/account/src/lib/assignments.h>
#include <maps/doc/proto/converters/geolib/include/yandex/maps/geolib3/proto.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

template <>
void Out<maps::geolib3::Point2>(
    IOutputStream& os,
    const maps::geolib3::Point2& point)
{
    os << "(x=" << point.x() << ", y=" << point.y() << ")";
}

namespace maps::wiki::ugc::account::tests {

using namespace std::literals::chrono_literals;

namespace {

std::vector<Assignment> createAssignments()
{
    return {
        Assignment{AssignmentId{"1"}, Uid{1}, AssignmentStatus::Active, {}, maps::chrono::TimePoint(), {}},
        Assignment{AssignmentId{"2"}, Uid{2}, AssignmentStatus::Active, {}, maps::chrono::TimePoint(), {}},
        Assignment{AssignmentId{"3"}, Uid{3}, AssignmentStatus::Active, {}, maps::chrono::TimePoint(), {}},
        Assignment{AssignmentId{"4"}, Uid{4}, AssignmentStatus::Active, {}, maps::chrono::TimePoint(), {}},
        Assignment{AssignmentId{"5"}, Uid{5}, AssignmentStatus::Active, {}, maps::chrono::TimePoint(), {}}
    };
}

} // namespace

Y_UNIT_TEST_SUITE(test_slice)
{

Y_UNIT_TEST(test_no_base_id)
{
    auto result = getAssignmentsSlice(
        createAssignments(),
        Paging{/*before*/ 5, /*after*/ 3, std::nullopt}
    );
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 3);
    UNIT_ASSERT_VALUES_EQUAL(result[0].id.value(), "1");
    UNIT_ASSERT_VALUES_EQUAL(result[1].id.value(), "2");
    UNIT_ASSERT_VALUES_EQUAL(result[2].id.value(), "3");
}

Y_UNIT_TEST(test_empty)
{
    auto result = getAssignmentsSlice(
        createAssignments(),
        Paging{/*before*/ 0, /*after*/ 0, "3"}
    );
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(result[0].id.value(), "3");
}

Y_UNIT_TEST(test_after)
{
    auto result = getAssignmentsSlice(
        createAssignments(),
        Paging{/*before*/ 0, /*after*/ 2, "2"}
    );
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(result[0].id.value(), "3");
    UNIT_ASSERT_VALUES_EQUAL(result[1].id.value(), "4");
}

Y_UNIT_TEST(test_after_4)
{
    auto result = getAssignmentsSlice(
        createAssignments(),
        Paging{/*before*/ 0, /*after*/ 2, "4"}
    );
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(result[0].id.value(), "5");
}

Y_UNIT_TEST(test_after_feed_ends)
{
    auto result = getAssignmentsSlice(
        createAssignments(),
        Paging{/*before*/ 0, /*after*/ 12, "2"}
    );
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 3);
    UNIT_ASSERT_VALUES_EQUAL(result[0].id.value(), "3");
    UNIT_ASSERT_VALUES_EQUAL(result[1].id.value(), "4");
    UNIT_ASSERT_VALUES_EQUAL(result[2].id.value(), "5");
}

Y_UNIT_TEST(test_after_last_item)
{
    auto result = getAssignmentsSlice(
        createAssignments(),
        Paging{/*before*/ 0, /*after*/ 12, "5"}
    );
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 0);
}

Y_UNIT_TEST(test_before)
{
    auto result = getAssignmentsSlice(
        createAssignments(),
        Paging{/*before*/ 1, /*after*/ 0, "4"}
    );
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(result[0].id.value(), "3");
}

Y_UNIT_TEST(test_before_feed_ends)
{
    auto result = getAssignmentsSlice(
        createAssignments(),
        Paging{/*before*/ 10, /*after*/ 0, "3"}
    );
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);
    UNIT_ASSERT_VALUES_EQUAL(result[0].id.value(), "1");
    UNIT_ASSERT_VALUES_EQUAL(result[1].id.value(), "2");
}

Y_UNIT_TEST(test_before_after)
{
    auto result = getAssignmentsSlice(
        createAssignments(),
        Paging{/*before*/ 1, /*after*/ 1, "3"}
    );
    UNIT_ASSERT_VALUES_EQUAL(result.size(), 3);
    UNIT_ASSERT_VALUES_EQUAL(result[0].id.value(), "2");
    UNIT_ASSERT_VALUES_EQUAL(result[1].id.value(), "3");
    UNIT_ASSERT_VALUES_EQUAL(result[2].id.value(), "4");
}

} // test_slice suite

Y_UNIT_TEST_SUITE(test_convert)
{

Y_UNIT_TEST(test_convert)
{
    proto::assignment::AssignmentMetadata ruMetadata;
    auto* ruAddressAdd = ruMetadata.mutable_address_add_assignment();
    *ruAddressAdd = proto::assignments::address_add::AddressAddAssignment();
    *ruAddressAdd->mutable_uri() = "ru_uri";

    proto::assignment::AssignmentMetadata enMetadata;
    auto* enAddressAdd = enMetadata.mutable_address_add_assignment();
    *enAddressAdd = proto::assignments::address_add::AddressAddAssignment();
    *enAddressAdd->mutable_uri() = "en_uri";

    std::vector<Assignment> assignments{
        Assignment{
            AssignmentId{"1"},
            Uid{1},
            AssignmentStatus::Active,
            {{"ru_RU", proto::assignment::AssignmentMetadata()}},
            maps::chrono::TimePoint(10s),
            {/*position*/}
        },
        Assignment{
            AssignmentId{"2"},
            Uid{2},
            AssignmentStatus::Active,
            {
                {"ru_RU", ruMetadata},
                {"en_US", enMetadata}
            },
            maps::chrono::TimePoint(15s),
            {geolib3::Point2{27.27, 54.54}}
        },
    };

    auto result = convertAssignments(assignments, Lang{"ru_RU"}.locale());
    UNIT_ASSERT_VALUES_EQUAL(result.assignment_size(), 2);
    auto assignment1 = result.assignment(0);
    UNIT_ASSERT_VALUES_EQUAL(assignment1.task_id(), "1");
    UNIT_ASSERT(assignment1.status() == proto::assignment::ACTIVE);
    UNIT_ASSERT(!assignment1.has_point());
    auto assignment2 = result.assignment(1);
    UNIT_ASSERT_VALUES_EQUAL(assignment2.task_id(), "2");
    UNIT_ASSERT(assignment2.metadata().has_address_add_assignment());
    UNIT_ASSERT_VALUES_EQUAL(assignment2.metadata().address_add_assignment().uri(), "ru_uri");
    UNIT_ASSERT(assignment2.has_point());
    UNIT_ASSERT_VALUES_EQUAL(
        geolib3::proto::decode(assignment2.point()),
        geolib3::Point2(27.27, 54.54)
    );

    auto enResult = convertAssignments(assignments, Lang{"en_US"}.locale());
    UNIT_ASSERT_VALUES_EQUAL(enResult.assignment_size(), 2);
    UNIT_ASSERT(enResult.assignment(1).metadata().has_address_add_assignment());
    UNIT_ASSERT_VALUES_EQUAL(enResult.assignment(1).metadata().address_add_assignment().uri(), "en_uri");
}

} // test_convert suite


} // namespace maps::wiki::ugc::account::tests
