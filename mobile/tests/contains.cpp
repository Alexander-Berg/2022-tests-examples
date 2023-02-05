#include <boost/test/unit_test.hpp>

#include <yandex/maps/navikit/algorithm.h>

#include <gmock/gmock.h>

#include <vector>
#include <list>
#include <set>
#include <map>
#include <unordered_set>
#include <unordered_map>

namespace yandex::maps::navikit {

namespace {

template <class Container, class T>
Container createContainer(std::initializer_list<T> list)
{
    return Container{ list };
}

template <class Container, class Element, class Key>
void testContains(std::initializer_list<Element> elements, const Key& excluded)
{
    auto container = createContainer<Container>(elements);
    for (const auto& element : container) {
        BOOST_CHECK(contains(container, element));
    }

    BOOST_CHECK(!contains(container, excluded));
}

template <class Container>
class FastSearchContainer {
public:
    MOCK_CONST_METHOD0(mockFind, void());
    typename Container::const_iterator find(const typename Container::key_type& key) const
    {
        mockFind();
        return container_.find(key);
    }

    typename Container::const_iterator begin() const
    {
        return container_.begin();
    }

    typename Container::const_iterator end() const
    {
        return container_.end();
    }

private:
    Container container_;
};

template <class Container>
void testFastSearch()
{
    FastSearchContainer<Container> container;
    EXPECT_CALL(container, mockFind()).Times(1);
    contains(container, 1);
}

} // namespace

BOOST_AUTO_TEST_CASE(ContainsTest)
{
    testContains<std::vector<int>>({1, 2, 3, 4, 5}, 6);
    testContains<std::list<int>>({1, 2, 3, 4, 5}, 6);
    testContains<std::set<int>>({1, 2, 3, 4, 5}, 6);
    testContains<std::unordered_set<int>>({1, 2, 3, 4, 5}, 6);
    testContains<std::map<int, int>, std::pair<const int, int>>({
        {1, 1},
        {2, 2},
        {3, 3},
        {4, 4},
        {5, 5}
    }, 6);
    testContains<std::unordered_map<int, int>, std::pair<const int, int>>({
        {1, 1},
        {2, 2},
        {3, 3},
        {4, 4},
        {5, 5}
    }, 6);
}

BOOST_AUTO_TEST_CASE(AnyOfTest)
{
    std::vector<int> vector {1, 2, 3, 4, 5, 6, 7};
    BOOST_CHECK(anyOf(vector, [](int x) { return x % 2 == 0; }));
    BOOST_CHECK(anyOf(vector, [](int x) { return x == 7; }));
    BOOST_CHECK(!anyOf(vector, [](int x) { return x > 7; }));
    BOOST_CHECK(anyOf(vector, [](int x) { return x <= 7; }));
}

BOOST_AUTO_TEST_CASE(FastSearchTest)
{
    testFastSearch<std::set<int>>();
    testFastSearch<std::map<int, int>>();
    testFastSearch<std::unordered_map<int, int>>();
    testFastSearch<std::unordered_set<int>>();
}

BOOST_AUTO_TEST_CASE(TransformIntoVectorTest)
{
    auto text = std::string("text");
    auto vecOfText = transformIntoVector(text, [](char c) { return c; });
    BOOST_TEST(vecOfText == std::vector<char>({'t', 'e', 'x', 't'}), boost::test_tools::per_element());

    auto original = std::vector<int> { 1, 2, 3, 4 };
    auto tripled = transformIntoVector(original, [](int i) { return i * 3; });
    BOOST_TEST(tripled == std::vector<int>({3, 6, 9, 12}), boost::test_tools::per_element());

    auto empty = std::list<std::string>();
    auto stillEmpty = transformIntoVector(empty, [](auto /* s */) { return 42; });
    BOOST_TEST(stillEmpty == std::vector<int>(), boost::test_tools::per_element());
}

} // namespace yandex
