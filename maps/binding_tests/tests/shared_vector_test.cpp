#include <yandex/maps/runtime/bindings/internal/shared_vector.h>

#include <boost/test/unit_test.hpp>

#include <algorithm>
#include <memory>

template<class T>
using SharedVector = yandex::maps::runtime::bindings::internal::SharedVector<T, std::allocator<T>>;

BOOST_AUTO_TEST_CASE(push_gets_element_in_shared_vector)
{
    auto vec = SharedVector<int>();
    vec.push_back(0);
    BOOST_CHECK_EQUAL(0, vec[0]);
    vec.push_back_shared(std::make_shared<int>(1));
    BOOST_CHECK_EQUAL(1, vec[1]);
}

BOOST_AUTO_TEST_CASE(iterator_iterates_over_shared_vector)
{
    auto vec = SharedVector<int>();
    vec.push_back(0);
    vec.push_back(1);
    auto it = vec.begin();
    BOOST_CHECK_EQUAL(0, *it);
    ++it;
    BOOST_CHECK_EQUAL(1, *it);
    ++it;
    BOOST_CHECK(vec.end() == it);
}

BOOST_AUTO_TEST_CASE(shared_vector_stl_interoperability_for_erase_remove)
{
    auto vec = SharedVector<int>();
    vec.push_back(0);
    vec.push_back(1);
    vec.push_back(2);
    auto eraseFrom = std::remove_if(vec.begin(), vec.end(), [](int i)
    {
        return i % 2 == 0;
    });
    vec.erase(eraseFrom, vec.end());
    BOOST_CHECK_EQUAL(1, vec.size());
    BOOST_CHECK_EQUAL(1, vec[0]);
}

BOOST_AUTO_TEST_CASE(shared_vector_from_stl_vector)
{
    auto stlVec = std::vector<int>{ 0, 1, 2, 3 };
    auto vec = SharedVector<int>(stlVec);
    BOOST_CHECK_EQUAL(0, vec[0]);
    BOOST_CHECK_EQUAL(1, vec[1]);
    BOOST_CHECK_EQUAL(2, vec[2]);
    BOOST_CHECK_EQUAL(3, vec[3]);
}

BOOST_AUTO_TEST_CASE(shared_vector_shares_elements)
{
    auto vec = SharedVector<int>();
    vec.push_back(0);
    auto elem = vec.sharedAt(0);
    *elem = 2;
    BOOST_CHECK_EQUAL(2, vec[0]);
}

BOOST_AUTO_TEST_CASE(shared_vector_equals_same)
{
    auto lhs = SharedVector<int>({1, 2, 3});
    auto rhs = SharedVector<int>({1, 2, 3});

    BOOST_CHECK(lhs == rhs);
    BOOST_CHECK(!(lhs != rhs));
}

BOOST_AUTO_TEST_CASE(shared_vector_equals_different)
{
    auto lhs = SharedVector<int>({1, 2, 3});
    auto rhs = SharedVector<int>({4, 5, 6});

    BOOST_CHECK(!(lhs == rhs));
    BOOST_CHECK(lhs != rhs);
}

BOOST_AUTO_TEST_CASE(shared_vector_equals_first_is_prefix_of_second)
{
    auto lhs = SharedVector<int>({1, 2, 3});
    auto rhs = SharedVector<int>({1, 2, 3, 4, 5});

    BOOST_CHECK(!(lhs == rhs));
    BOOST_CHECK(lhs != rhs);
}

BOOST_AUTO_TEST_CASE(shared_vector_equals_second_is_prefix_of_first)
{
    auto lhs = SharedVector<int>({1, 2, 3, 4, 5});
    auto rhs = SharedVector<int>({1, 2, 3});

    BOOST_CHECK(!(lhs == rhs));
    BOOST_CHECK(lhs != rhs);
}
