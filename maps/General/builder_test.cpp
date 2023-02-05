#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/road_graph_snippets/include/road_graph_snippets.h>
#include <maps/libs/road_graph_snippets/include/road_graph_snippets_builder.h>

using namespace maps::road_graph;
using namespace maps::road_graph::literals;

Y_UNIT_TEST_SUITE(BuilderTest) {

Y_UNIT_TEST(EmptySnippetsTest) {
    const std::string_view filePath = "road_graph_snippets_1.fb";
    const std::string_view version = "test-version-1";
    const EdgeId numEdges = 1000_e;

    snippets::SnippetsBuilder builder(version, numEdges.value());
    std::move(builder).build(filePath);

    maps::road_graph::snippets::Snippets snippets(filePath);
    UNIT_ASSERT_EQUAL(version, snippets.version());

    for (EdgeId e = 0_e; e < numEdges; ++e) {
        UNIT_ASSERT_EQUAL(snippets.edgeMessages(e).size(), 0);
    }
}

Y_UNIT_TEST(SetInvalidEdgeTest) {
    const std::string_view version = "test-version";
    const EdgeId numEdges = 1000_e;

    snippets::SnippetsBuilder builder(version, numEdges.value());

    UNIT_CHECK_GENERATED_NO_EXCEPTION(builder.addEdgeMessage(999_e, 101, "aaa"), std::out_of_range);
    UNIT_CHECK_GENERATED_EXCEPTION(builder.addEdgeMessage(1000_e, 102, "bbb"), std::out_of_range);
    UNIT_CHECK_GENERATED_EXCEPTION(builder.addEdgeMessage(1111_e, 103, "ccc"), std::out_of_range);
}

Y_UNIT_TEST(OneEdgeSnippetTest) {
    const std::string_view filePath = "road_graph_snippets_2.fb";
    const std::string_view version = "test-version-2";
    const EdgeId numEdges = 1000_e;

    snippets::SnippetsBuilder builder(version, numEdges.value());
    builder.addEdgeMessage(100_e, 501, "some binary data");
    builder.addEdgeMessage(100_e, 502, "another binary data");
    std::move(builder).build(filePath);

    maps::road_graph::snippets::Snippets snippets(filePath);
    UNIT_ASSERT_EQUAL(version, snippets.version());

    for (EdgeId e = 0_e; e < numEdges; ++e) {
        if (e != 100_e) {
            UNIT_ASSERT_EQUAL(snippets.edgeMessages(e).size(), 0);
        }
    }

    UNIT_ASSERT_EQUAL(snippets.edgeMessages(100_e).size(), 2);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(100_e)[0].id, 501);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(100_e)[0].data, "some binary data");
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(100_e)[1].id, 502);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(100_e)[1].data, "another binary data");
}

Y_UNIT_TEST(FewEdgeSnippetsTest) {
    const std::string_view filePath = "road_graph_snippets_3.fb";
    const std::string_view version = "test-version-3";
    const EdgeId numEdges = 1000_e;

    snippets::SnippetsBuilder builder(version, numEdges.value());
    builder.addEdgeMessage(0_e, 101, "aaa");
    builder.addEdgeMessage(0_e, 102, "bbbb");
    builder.addEdgeMessage(100_e, 401, "");
    builder.addEdgeMessage(500_e, 201, "ccccc");
    builder.addEdgeMessage(999_e, 301, "dddddd");
    builder.addEdgeMessage(999_e, 302, "eeeeeee");
    std::move(builder).build(filePath);

    maps::road_graph::snippets::Snippets snippets(filePath);
    UNIT_ASSERT_EQUAL(version, snippets.version());

    UNIT_ASSERT_EQUAL(snippets.edgeMessages(0_e).size(), 2);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(0_e)[0].id, 101);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(0_e)[0].data, "aaa");
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(0_e)[1].id, 102);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(0_e)[1].data, "bbbb");

    UNIT_ASSERT_EQUAL(snippets.edgeMessages(1_e).size(), 0);

    UNIT_ASSERT_EQUAL(snippets.edgeMessages(99_e).size(), 0);

    UNIT_ASSERT_EQUAL(snippets.edgeMessages(100_e).size(), 1);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(100_e)[0].id, 401);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(100_e)[0].data, "");

    UNIT_ASSERT_EQUAL(snippets.edgeMessages(101_e).size(), 0);

    UNIT_ASSERT_EQUAL(snippets.edgeMessages(500_e).size(), 1);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(500_e)[0].id, 201);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(500_e)[0].data, "ccccc");

    UNIT_ASSERT_EQUAL(snippets.edgeMessages(998_e).size(), 0);

    UNIT_ASSERT_EQUAL(snippets.edgeMessages(999_e).size(), 2);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(999_e)[0].id, 301);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(999_e)[0].data, "dddddd");
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(999_e)[1].id, 302);
    UNIT_ASSERT_EQUAL(snippets.edgeMessages(999_e)[1].data, "eeeeeee");
}

};
