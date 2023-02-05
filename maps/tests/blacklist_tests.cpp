#include <library/cpp/testing/unittest/registar.h>

#include <maps/wikimap/jams_arm2/libs/db/include/renderer_blacklist.h>

namespace maps::wiki::jams_arm2::db {

Y_UNIT_TEST_SUITE(renderer_blacklist)
{

Y_UNIT_TEST(RendererBlacklist_Data)
{
    BlacklistArea::Data dataRef(
        "test descr",
        {db::BlacklistTag::Transit, db::BlacklistTag::Road},
        {"first text", "second text"},
        {"1", "22", "33333333"});

    {
        auto data = BlacklistArea::Data::fromJsonStr(R"({
            "description": "test descr",
            "tags": ["transit", "road"],
            "texts": ["first text", "second text"],
            "objectIds": ["1", "22", "33333333"]
        })");
        UNIT_ASSERT_EQUAL(data.description, dataRef.description);
        UNIT_ASSERT_EQUAL(data.tags, dataRef.tags);
        UNIT_ASSERT_EQUAL(data.texts, dataRef.texts);
        UNIT_ASSERT_EQUAL(data.objectIds, dataRef.objectIds);
    }
    {
        auto data = BlacklistArea::Data::fromJsonStr(dataRef.toJsonStr());
        UNIT_ASSERT_EQUAL(data.description, dataRef.description);
        UNIT_ASSERT_EQUAL(data.tags, dataRef.tags);
        UNIT_ASSERT_EQUAL(data.texts, dataRef.texts);
        UNIT_ASSERT_EQUAL(data.objectIds, dataRef.objectIds);
    }
}

} // Y_UNIT_TEST_SUITE(renderer_blacklist)
} // namespace maps::wiki::jams_arm2::db
