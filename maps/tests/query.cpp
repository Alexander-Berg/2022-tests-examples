#include <maps/infopoint/lib/backend/view.h>
#include <maps/infopoint/lib/backend/query/filter.h>
#include <maps/infopoint/lib/backend/query/update.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <contrib/libs/mongo-cxx-driver/bsoncxx/builder/basic/document.hpp>
#include <contrib/libs/mongo-cxx-driver/bsoncxx/json.hpp>

using bsoncxx::builder::basic::make_array;
using bsoncxx::builder::basic::make_document;
using bsoncxx::builder::basic::kvp;

TEST(Query, BuildFilter)
{
    using namespace infopoint::db::query;

    EXPECT_EQ(
        bsoncxx::to_json(buildFilter().value()),
        bsoncxx::to_json(make_document()));

    EXPECT_EQ(
        bsoncxx::to_json(buildFilter(equal("a", "b")).value()),
        bsoncxx::to_json(make_document(kvp("a", "b"))));

    EXPECT_EQ(
        bsoncxx::to_json(
            buildFilter(equal("a", "b"), equal("c", "d")).value()),
        bsoncxx::to_json(
            make_document(kvp("$and", make_array(
                make_document(kvp("a", "b")),
                make_document(kvp("c", "d")))))));
}

TEST(Query, Predicates)
{
    using namespace infopoint::db::query;

    auto filter = buildFilter(
        orClause(
            andClause(
                equal("key1", 1),
                lte("key1", 5),
                gte("key2", "value1")),
            notEqual("key2", 1.0),
            containsAll("key.0", std::set<std::string>{"a", "b"}),
            containsIf("subarray", equal("key", "value2")),
            sizeEqual("subarray", 0),
            exists("key", false)));

    auto expected = make_document(
        kvp("$or", make_array(
            make_document(
                kvp("$and", make_array(
                    make_document(kvp("key1", 1)),
                    make_document(kvp("key1", make_document(
                        kvp("$lte", 5)))),
                    make_document(kvp("key2", make_document(
                        kvp("$gte", "value1"))))))),
            make_document(kvp("key2", make_document(
                kvp("$ne", 1.0)))),
            make_document(kvp("key.0", make_document(
                kvp("$all", make_array("a", "b"))))),
            make_document(kvp("subarray", make_document(
                kvp("$elemMatch", make_document(
                    kvp("key", "value2")))))),
            make_document(kvp("subarray", make_document(
                kvp("$size", 0)))),
            make_document(kvp("key", make_document(
                kvp("$exists", false))))
            )));

    EXPECT_EQ(
        bsoncxx::to_json(filter.value().view()),
        bsoncxx::to_json(expected.view()));
}

TEST(Query, BuildUpdate)
{
    using namespace infopoint::db::query;

    EXPECT_EQ(
        bsoncxx::to_json(buildUpdate().value()),
        bsoncxx::to_json(make_document()));

    EXPECT_EQ(
        bsoncxx::to_json(buildUpdate(set("a", "b")).value()),
        bsoncxx::to_json(make_document(kvp("$set", make_document(
            kvp("a", "b"))))));

    EXPECT_EQ(
        bsoncxx::to_json(buildUpdate(inc("a", 1)).value()),
        bsoncxx::to_json(make_document(kvp("$inc", make_document(
            kvp("a", 1))))));

    EXPECT_EQ(
        bsoncxx::to_json(buildUpdate(
            set("a", "b"), inc("a", 1), inc("b", 1), set("b", "a")).value()),
        bsoncxx::to_json(make_document(
            kvp("$set", make_document(
                kvp("a", "b"), kvp("b", "a"))),
            kvp("$inc", make_document(
                kvp("a", 1), kvp("b", 1))))));
}

TEST(Query, Projection)
{
    using namespace infopoint::db;

    EXPECT_EQ(
        bsoncxx::to_json(buildProjection().value()),
        bsoncxx::to_json(make_document()));

    EXPECT_EQ(
        bsoncxx::to_json(buildProjection(show("a")).value()),
        bsoncxx::to_json(make_document(kvp("a", 1))));

    EXPECT_EQ(
        bsoncxx::to_json(buildProjection(
            show("a"),
            show("b", 5),
            show("c")).value()),
        bsoncxx::to_json(make_document(
            kvp("a", 1),
            kvp("b", make_document(
                kvp("$slice", make_array(5, 1)))),
            kvp("c", 1))));
}
