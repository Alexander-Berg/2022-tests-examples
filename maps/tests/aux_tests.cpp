#include "helpers.h"

#include <yandex/maps/wiki/revision/objectrevision.h>
#include <yandex/maps/wiki/revision/revisionid.h>
#include <yandex/maps/wiki/revision/range.h>
#include <maps/libs/common/include/exception.h>

#include <boost/test/unit_test.hpp>

#include <boost/lexical_cast.hpp>

namespace maps {
namespace wiki {
namespace revision {
namespace tests {

BOOST_AUTO_TEST_SUITE(AuxTests)

BOOST_AUTO_TEST_CASE(test_lexical_cast_id)
{
    ObjectRevision::ID id = createID(123, 456);

    BOOST_CHECK(!id.empty());
    BOOST_CHECK_EQUAL(id.objectId(), 123);
    BOOST_CHECK_EQUAL(id.commitId(), 456);

    ObjectRevision::ID id2;
    BOOST_CHECK(id2.empty());

    std::string str = boost::lexical_cast<std::string>(id);
    BOOST_CHECK_EQUAL(str, "123:456");

    id2 = boost::lexical_cast<ObjectRevision::ID>(str);
    BOOST_CHECK_EQUAL(id.objectId(), id2.objectId());
    BOOST_CHECK_EQUAL(id.commitId(), id2.commitId());

    std::string str2 = boost::lexical_cast<std::string>(id2);
    BOOST_CHECK_EQUAL(str, str2);
}

BOOST_AUTO_TEST_CASE(test_lexical_cast_id_fail)
{
    BOOST_CHECK_THROW(boost::lexical_cast<ObjectRevision::ID>(""), boost::bad_lexical_cast);
    BOOST_CHECK_THROW(boost::lexical_cast<ObjectRevision::ID>("123"), boost::bad_lexical_cast);
    BOOST_CHECK_THROW(boost::lexical_cast<ObjectRevision::ID>("123*456"), boost::bad_lexical_cast);
    BOOST_CHECK_THROW(boost::lexical_cast<ObjectRevision::ID>("123:456:"), boost::bad_lexical_cast);
    BOOST_CHECK_THROW(boost::lexical_cast<ObjectRevision::ID>(":123:456"), boost::bad_lexical_cast);
}

BOOST_AUTO_TEST_CASE(test_revision_id)
{
    ObjectRevision::ID rd11 = createID(1, 1);
    ObjectRevision::ID rd12 = createID(1, 2);
    ObjectRevision::ID rd21 = createID(2, 1);
    ObjectRevision::ID rd22 = createID(2, 2);

    BOOST_CHECK(rd12 != rd11);
    BOOST_CHECK(rd12 == rd12);
    BOOST_CHECK(rd12 != rd21);
    BOOST_CHECK(rd12 != rd22);

    BOOST_CHECK(rd11 < rd12);
    BOOST_CHECK(rd11 < rd21);
    BOOST_CHECK(rd11 < rd22);
    BOOST_CHECK(rd12 < rd21);
    BOOST_CHECK(rd12 < rd22);
    BOOST_CHECK(rd21 < rd22);

    std::set<ObjectRevision::ID> container;
    BOOST_CHECK(container.insert(rd22).second);
    BOOST_CHECK(!container.insert(rd22).second);

    BOOST_CHECK(container.insert(rd21).second);
    BOOST_CHECK(!container.insert(rd21).second);

    BOOST_CHECK(container.insert(rd12).second);
    BOOST_CHECK(!container.insert(rd12).second);

    BOOST_CHECK(container.insert(rd11).second);
    BOOST_CHECK(!container.insert(rd11).second);

    BOOST_REQUIRE(container.size() == 4);
    auto it = container.begin();
    BOOST_CHECK(*it == rd11);
    ++it;
    BOOST_CHECK(*it == rd12);
    ++it;
    BOOST_CHECK(*it == rd21);
    ++it;
    BOOST_CHECK(*it == rd22);
    ++it;
    BOOST_CHECK(it == container.end());
}

BOOST_AUTO_TEST_CASE(test_invalid_relation_data)
{
    BOOST_CHECK_THROW(RelationData(0, 0), maps::Exception);
}

BOOST_AUTO_TEST_CASE(test_relation_data)
{
    RelationData rd11(1, 1);
    RelationData rd12(1, 2);
    RelationData rd21(2, 1);
    RelationData rd22(2, 2);

    BOOST_CHECK(rd12 != rd11);
    BOOST_CHECK(rd12 == rd12);
    BOOST_CHECK(rd12 != rd21);
    BOOST_CHECK(rd12 != rd22);

    BOOST_CHECK(rd11 < rd12);
    BOOST_CHECK(rd11 < rd21);
    BOOST_CHECK(rd11 < rd22);
    BOOST_CHECK(rd12 < rd21);
    BOOST_CHECK(rd12 < rd22);
    BOOST_CHECK(rd21 < rd22);

    std::set<RelationData> container;
    BOOST_CHECK(container.insert(rd22).second);
    BOOST_CHECK(!container.insert(rd22).second);

    BOOST_CHECK(container.insert(rd21).second);
    BOOST_CHECK(!container.insert(rd21).second);

    BOOST_CHECK(container.insert(rd12).second);
    BOOST_CHECK(!container.insert(rd12).second);

    BOOST_CHECK(container.insert(rd11).second);
    BOOST_CHECK(!container.insert(rd11).second);

    BOOST_REQUIRE(container.size() == 4);
    auto it = container.begin();
    BOOST_CHECK(*it == rd11);
    ++it;
    BOOST_CHECK(*it == rd12);
    ++it;
    BOOST_CHECK(*it == rd21);
    ++it;
    BOOST_CHECK(*it == rd22);
    ++it;
    BOOST_CHECK(it == container.end());
}

std::string
toString(const ConstRange<char>& range)
{
    auto it = range.iterator();

    std::string res;
    for (const char* ptr = it->next(); ptr != 0; ptr = it->next()) {
        res.push_back(*ptr);
    }
    return res;
}

BOOST_AUTO_TEST_CASE(test_range)
{
    std::string str = "abracadabra";
    std::list<char> lst;
    std::vector<char> vec;

    for (const char* s = str.c_str(); *s; ++s) {
        lst.push_back(*s);
        vec.push_back(*s);
    }
    BOOST_CHECK_EQUAL(str.size(), lst.size());
    BOOST_CHECK_EQUAL(str.size(), vec.size());

    BOOST_CHECK_EQUAL(str, toString(lst));
    BOOST_CHECK_EQUAL(str, toString(vec));

    std::string strLst = toString(ConstRange<char>(lst.begin(), lst.end()));
    BOOST_CHECK_EQUAL(str, strLst);

    std::string strVec = toString(ConstRange<char>(vec.begin(), vec.end()));
    BOOST_CHECK_EQUAL(str, strVec);
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps
