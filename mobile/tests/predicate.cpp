#include <boost/test/unit_test.hpp>

#include <yandex/maps/navikit/predicate.h>

namespace yandex::maps::navikit {

namespace {

enum class TestEnum {
	Case1,
	Case2,
	Case3
};

} // namespace

BOOST_AUTO_TEST_CASE(TestPredicateAnyOfEquality)
{
	BOOST_CHECK(navikit::any_of(TestEnum::Case1, TestEnum::Case2) == TestEnum::Case1);
	BOOST_CHECK(navikit::any_of(TestEnum::Case1, TestEnum::Case2) == TestEnum::Case2);
	BOOST_CHECK(navikit::any_of(TestEnum::Case1, TestEnum::Case2) != TestEnum::Case3);
  
	BOOST_CHECK(!(navikit::any_of(TestEnum::Case1, TestEnum::Case2) == TestEnum::Case3));

	BOOST_CHECK(navikit::any_of(3, 4, 5) == 3);
	BOOST_CHECK(navikit::any_of(3, 4, 5) != 6);
}

BOOST_AUTO_TEST_CASE(TestPredicateAllOfEquality)
{
	BOOST_CHECK(navikit::all_of(TestEnum::Case1, TestEnum::Case1) == TestEnum::Case1);
	BOOST_CHECK(navikit::all_of(TestEnum::Case2, TestEnum::Case2) == TestEnum::Case2);
	BOOST_CHECK(navikit::all_of(TestEnum::Case1, TestEnum::Case2) != TestEnum::Case3);
  
	BOOST_CHECK(!(navikit::all_of(TestEnum::Case1, TestEnum::Case2) != TestEnum::Case2));
	BOOST_CHECK(!(navikit::all_of(TestEnum::Case1, TestEnum::Case2) == TestEnum::Case2));

	BOOST_CHECK(navikit::all_of(1, 1, 1) == 1);
	BOOST_CHECK(navikit::all_of(1, 2, 3) != 4);
}

BOOST_AUTO_TEST_CASE(TestPredicateAnyOfGreaterThan)
{
	BOOST_CHECK(navikit::any_of(3, 3, 3) > 2);
	BOOST_CHECK(navikit::any_of(3, 2, 3) > 2);
	BOOST_CHECK(navikit::any_of(1, 2, 3) > 0);
  
  	BOOST_CHECK(!(navikit::any_of(1, 2, 3) > 3));
}

BOOST_AUTO_TEST_CASE(TestPredicateAllOfGreaterThan)
{
	BOOST_CHECK(navikit::all_of(3, 3, 3) > 2);
	BOOST_CHECK(!(navikit::all_of(3, 2, 3) > 2));
	BOOST_CHECK(navikit::all_of(1, 2, 3) > 0);
}

BOOST_AUTO_TEST_CASE(TestPredicateAnyOfLowerThan)
{
	BOOST_CHECK(navikit::any_of(2, 2, 2) < 3);
	BOOST_CHECK(navikit::any_of(3, 2, 3) < 3);
	BOOST_CHECK(navikit::any_of(1, 2, 3) < 4);
  
  	BOOST_CHECK(!(navikit::any_of(3, 2, 3) < 2));
}

BOOST_AUTO_TEST_CASE(TestPredicateAllOfLowerThan)
{
	BOOST_CHECK(navikit::all_of(2, 2, 2) < 3);
	BOOST_CHECK(!(navikit::all_of(3, 2, 3) < 3));
	BOOST_CHECK(navikit::all_of(1, 2, 3) < 4);
}

BOOST_AUTO_TEST_CASE(TestPredicateAnyOfGEQ)
{
	BOOST_CHECK(navikit::any_of(3, 3, 3) >= 2);
	BOOST_CHECK(navikit::any_of(3, 2, 3) >= 2);
	BOOST_CHECK(navikit::any_of(0, -1, -1) >= 0);
  
  	BOOST_CHECK(!(navikit::any_of(-1, -1, -1) >= 0));
}

BOOST_AUTO_TEST_CASE(TestPredicateAllOfGEQ)
{
	BOOST_CHECK(navikit::all_of(3, 3, 3) >= 3);
	BOOST_CHECK(!(navikit::all_of(3, 1, 3) >= 2));
	BOOST_CHECK(navikit::all_of(0, 1, 2) >= 0);
  
  	BOOST_CHECK(!(navikit::all_of(0, 1, 2) >= 2));
}

BOOST_AUTO_TEST_CASE(TestPredicateAnyOfLEQ)
{
	BOOST_CHECK(navikit::any_of(2, 2, 2) <= 3);
	BOOST_CHECK(navikit::any_of(4, 2, 4) <= 3);
	BOOST_CHECK(navikit::any_of(1, 2, 3) <= 4);
  
  	BOOST_CHECK(!(navikit::any_of(4, 2, 4) <= 1));
}

BOOST_AUTO_TEST_CASE(TestPredicateAllOfLEQ)
{
	BOOST_CHECK(navikit::all_of(2, 2, 3) <= 3);
	BOOST_CHECK(!(navikit::all_of(3, 2, 4) <= 3));
	BOOST_CHECK(navikit::all_of(1, 2, 3) <= 4);
}

} // namespace yandex
