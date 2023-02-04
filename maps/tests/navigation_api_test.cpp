#include <maps/factory/services/backend/tests/test_utils.h>

#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/project_gateway.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/db/source_gateway.h>
#include <maps/factory/libs/sproto_helpers/navigation.h>

#include <maps/infra/yacare/include/test_utils.h>
#include <maps/libs/http/include/test_utils.h>
#include <maps/libs/introspection/include/stream_output.h>
#include <maps/libs/geolib/include/bounding_box.h>
#include <yandex/maps/geolib3/sproto.h>

#include <list>
#include <optional>
#include <sstream>
#include <vector>

namespace maps::factory::backend::tests {

namespace {

using maps::introspection::operator<<;

const std::string SERVICE_BASE_URL = "http://localhost";
const std::string LIST_CHILDREN_URL = SERVICE_BASE_URL + "/v1/navigation/list_node_children";
const std::string GET_NODE_SIBLINGS_URL = SERVICE_BASE_URL + "/v1/navigation/get_node_siblings";
const std::string GET_NODE_BOUNDING_BOX_URL = SERVICE_BASE_URL + "/v1/navigation/get_node_bounding_box";
const std::string GET_NODE_OBJECTS_COUNT_URL = SERVICE_BASE_URL + "/v1/navigation/get_node_objects_count";
const std::string GET_NODE_OBJECTS_URL = SERVICE_BASE_URL + "/v1/navigation/get_node_objects";
const std::string
    FIND_CORRESPONDING_NODE_URL = SERVICE_BASE_URL + "/v1/navigation/find_corresponding_node_id";
const std::string GET_NODE_INFO_URL = SERVICE_BASE_URL + "/v1/navigation/get_node_info";
const std::string EXPORT_URL = SERVICE_BASE_URL + "/v1/navigation/export";
const std::string PROTOBUF_MEDIA_TYPE = "application/x-protobuf";

const geolib3::MultiPolygon2 MOSAIC_SOURCE_GEOMETRY({
    geolib3::Polygon2(
        geolib3::PointsVector{
            {12019966.3541429, 6904062.66721244},
            {12039319.7036828, 6904965.89392846},
            {12039442.3817891, 6890407.40886827},
            {12020140.7466942, 6889491.27668234},
            {12019966.3541429, 6904062.66721244}
        }
    )
});

const geolib3::MultiPolygon2 MOSAIC_GEOMETRY({
    geolib3::Polygon2(
        geolib3::PointsVector{
            {12020066.3529682, 6904063.15192575},
            {12039323.8689762, 6904865.98071446},
            {12039342.3818711, 6890407.28082854},
            {12020136.5013354, 6889591.18652634},
            {12020066.3529682, 6904063.15192575}
        }
    )
});

class NavigationFixture : public BackendFixture {
public:
    NavigationFixture()
    {
        log8::setLevel(log8::Level::DEBUG);
        insertData();
    }

    void insertData()
    {
        auto txn = txnHandle();
        releases = db::Releases{
            db::Release("draft_release").setStatus(db::ReleaseStatus::New),
            db::Release("ready_release").setStatus(db::ReleaseStatus::Ready),
            db::Release("testing_release").setStatus(db::ReleaseStatus::Testing),
            db::Release("production_release").setStatus(db::ReleaseStatus::Production)
        };
        db::ReleaseGateway(*txn).insert(releases);

        auto source = db::Source("source_name", db::SourceType::Local, "path");
        db::SourceGateway(*txn).insert(source);

        deliveries = db::Deliveries{
            db::Delivery(source.id(), "2020-01-01", "delivery_2020", "subfolder1").setYear(2020),
            db::Delivery(source.id(), "2019-01-01", "delivery_2019", "subfolder2").setYear(2019)
        };
        db::DeliveryGateway(*txn).insert(deliveries);

        projects = db::Projects{{"Moscow"}, {"Nino"}};
        db::ProjectGateway(*txn).insert(projects);

        const auto addMs = [&](const std::string& n, db::Id proj = -1) {
            db::MosaicSource ms(n);
            ms.setStatus(db::MosaicSourceStatus::Ready);
            ms.setDeliveryId(deliveries.at(0).id());
            ms.setMercatorGeom(MOSAIC_SOURCE_GEOMETRY);
            if (proj >= 0) { ms.setProjectId(projects.at(proj).id()); }
            mosaicSources.push_back(ms);
        };
        mosaicSources.clear();
        addMs("unassigned", 1);
        addMs("draft", 0);
        addMs("ready");
        addMs("testing");
        addMs("production");
        db::MosaicSourceGateway(*txn).insert(mosaicSources);

        int zorder = 1;
        const int minZoom = 10;
        const int maxZoom = 19;

        const auto addM = [&](db::Id ms, db::Id rel) {
            db::Mosaic m(mosaicSources.at(ms).id(), ++zorder, minZoom, maxZoom, MOSAIC_GEOMETRY);
            m.setReleaseId(releases.at(rel).id());
            mosaics.push_back(m);
        };
        mosaics.clear();
        addM(1, 0);
        addM(2, 1);
        addM(3, 2);
        addM(4, 3);
        db::MosaicGateway(*txn).insert(mosaics);
        txn->commit();
    }

    db::Releases releases;
    db::Deliveries deliveries;
    db::Projects projects;
    db::MosaicSources mosaicSources;
    db::Mosaics mosaics;
};

struct TreeNode {
    std::string id;
    std::string name;
    std::vector<TreeNode> childrenNodes = {};

    template <typename T>
    static auto introspect(T& treeNode)
    {
        return std::tie(treeNode.id, treeNode.name, treeNode.childrenNodes);
    }
};

MATCHER(TreeNodeEqual, "")
{
    const sproto_helpers::snavigation::TreeNode& snode = std::get<0>(arg);
    const TreeNode& node = std::get<1>(arg);
    return
        testing::ExplainMatchResult(
            testing::Eq(snode.id()), node.id, result_listener) &&
        testing::ExplainMatchResult(
            testing::Eq(snode.name()), node.name, result_listener) &&
        testing::ExplainMatchResult(
            ::testing::Pointwise(TreeNodeEqual(), node.childrenNodes),
            snode.childrenNodes(), result_listener);
}

MATCHER_P(BboxNear, val, "")
{
    const geolib3::BoundingBox& bbox1 = arg;
    const geolib3::BoundingBox& bbox2 = val;
    const double EPS = 0.01;
    return
        testing::ExplainMatchResult(
            testing::DoubleNear(bbox1.minX(), EPS), bbox2.minX(), result_listener) &&
        testing::ExplainMatchResult(
            testing::DoubleNear(bbox1.maxX(), EPS), bbox2.maxX(), result_listener) &&
        testing::ExplainMatchResult(
            testing::DoubleNear(bbox1.minY(), EPS), bbox2.minY(), result_listener) &&
        testing::ExplainMatchResult(
            testing::DoubleNear(bbox1.maxY(), EPS), bbox2.maxY(), result_listener);
}

MATCHER(IdsEqual, "")
{
    const auto& sprotoObject = std::get<0>(arg);
    db::Id id = std::get<1>(arg);
    return testing::ExplainMatchResult(
        testing::Eq(sprotoObject.id()),
        std::to_string(id),
        result_listener
    );
}

/// Traverses the navigation tree and calls given function for every node
void forEachNode(std::function<void(const sproto_helpers::snavigation::TreeNode&)> processNode)
{
    std::list<std::string> nodes{""};

    while (!nodes.empty()) {
        auto nodeId = nodes.front();
        nodes.pop_front();
        auto request = http::MockRequest(http::GET,
            http::URL(LIST_CHILDREN_URL).addParam("node_id", nodeId));
        request.headers.emplace("Accept", "application/x-protobuf");
        auto response = yacare::performTestRequest(request);
        auto sprotoTreeNodes = boost::lexical_cast<sproto_helpers::snavigation::TreeNodes>(response.body);

        for (const auto& node: sprotoTreeNodes.nodes()) {
            processNode(node);

            if (node.hasChildren()) {
                nodes.push_back(node.id());
            }
        }
    }
}

} // namespace

TEST_F(NavigationFixture, test_list_children_of_existing_node)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    struct TestDatum {
        std::string nodeId;
        std::vector<TreeNode> nodes;
    };

    std::vector<TestDatum> testData{
        {
            "",
            {
                {"deliveries", "Deliveries"},
                {"releases", "Releases"}
            }
        },
        {
            "deliveries",
            {
                {"deliveries/2019", "2019"},
                {"deliveries/2020", "2020"},
            }
        },
        {
            "deliveries/2020/" + std::to_string(deliveries[0].id()),
            {
                {"deliveries/2020/" + std::to_string(deliveries[0].id()) + "/releases", "Releases"},
                {"deliveries/2020/" + std::to_string(deliveries[0].id()) + "/unassigned", "Unassigned"}
            }
        },
        {
            "deliveries/2020/" + std::to_string(deliveries[0].id()) + "/releases",
            {
                {"deliveries/2020/" + std::to_string(deliveries[0].id()) + "/releases/new", "new"},
                {"deliveries/2020/" + std::to_string(deliveries[0].id()) + "/releases/ready", "ready"},
                {"deliveries/2020/" + std::to_string(deliveries[0].id()) + "/releases/testing", "testing"},
                {"deliveries/2020/" + std::to_string(deliveries[0].id()) + "/releases/production",
                    "production"}
            }
        },
        {
            "deliveries/2019/" + std::to_string(deliveries[1].id()) + "/releases/new",
            {}
        }
    };

    for (const auto&[nodeId, nodes]: testData) {
        SCOPED_TRACE(nodeId);
        auto request = http::MockRequest(http::GET,
            http::URL(LIST_CHILDREN_URL).addParam("node_id", nodeId));
        request.headers.emplace("Accept", "application/x-protobuf");
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.headers.at("Content-Type"), "application/x-protobuf");

        auto sprotoTreeNodes = boost::lexical_cast<sproto_helpers::snavigation::TreeNodes>(response.body);

        EXPECT_THAT(sprotoTreeNodes.nodes(),
            ::testing::Pointwise(TreeNodeEqual(), nodes));
    }
}

TEST_F(NavigationFixture, test_list_children_of_missing_node)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    std::vector<std::string> missingNodes{"unknown", "releases/unknown"};

    for (const auto& nodeId: missingNodes) {
        SCOPED_TRACE(nodeId);
        auto request = http::MockRequest(http::GET,
            http::URL(LIST_CHILDREN_URL).addParam("node_id", nodeId));
        request.headers.emplace("Accept", "application/x-protobuf");
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 404);
    }
}

TEST_F(NavigationFixture, test_list_children_holds_object)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));
    {
        auto request = http::MockRequest(http::GET,
            http::URL(LIST_CHILDREN_URL).addParam("node_id", "deliveries/2020"));
        request.headers.emplace("Accept", "application/x-protobuf");
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.headers.at("Content-Type"), "application/x-protobuf");

        auto sprotoTreeNodes = boost::lexical_cast<sproto_helpers::snavigation::TreeNodes>(response.body);

        const auto& sdelivery = sprotoTreeNodes.nodes().at(0).delivery();
        ASSERT_TRUE(sdelivery.defined());
        EXPECT_EQ(sdelivery->id(), std::to_string(deliveries.at(0).id()));
        EXPECT_FALSE(sprotoTreeNodes.nodes().at(0).release().defined());
    }
    {
        auto request = http::MockRequest(http::GET,
            http::URL(LIST_CHILDREN_URL).addParam("node_id",
                "releases/new"));
        request.headers.emplace("Accept", "application/x-protobuf");
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.headers.at("Content-Type"), "application/x-protobuf");

        auto sprotoTreeNodes = boost::lexical_cast<sproto_helpers::snavigation::TreeNodes>(response.body);

        const auto& srelease = sprotoTreeNodes.nodes().at(0).release();
        ASSERT_TRUE(srelease.defined());
        EXPECT_EQ(srelease->id(), std::to_string(releases.at(0).id()));
        EXPECT_FALSE(sprotoTreeNodes.nodes().at(0).delivery().defined());
    }
}

TEST_F(NavigationFixture, test_traverse_tree_with_list_children)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    std::list<std::string> nodes{""};
    size_t totalNodesListed = 0;

    while (!nodes.empty()) {
        auto nodeId = nodes.front();
        SCOPED_TRACE(nodeId);
        nodes.pop_front();
        auto request = http::MockRequest(http::GET,
            http::URL(LIST_CHILDREN_URL).addParam("node_id", nodeId));
        request.headers.emplace("Accept", "application/x-protobuf");
        auto response = yacare::performTestRequest(request);

        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.headers.at("Content-Type"), "application/x-protobuf");

        auto sprotoTreeNodes = boost::lexical_cast<sproto_helpers::snavigation::TreeNodes>(response.body);

        for (const auto& node: sprotoTreeNodes.nodes()) {
            if (node.hasChildren()) {
                nodes.push_back(node.id());
            }
        }
        ++totalNodesListed;
    }
    EXPECT_EQ(totalNodesListed, 31u);
}

TEST_F(NavigationFixture, test_get_node_siblings_for_existing_nodes)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    struct TestDatum {
        std::string nodeId;
        std::vector<TreeNode> nodes;
    };

    std::vector<TestDatum> testData{
        {
            "deliveries",
            {
                {"deliveries", "Deliveries"},
                {"releases", "Releases"}
            }
        },
        {
            "deliveries/2020/" + std::to_string(deliveries[0].id()),
            {
                {"deliveries", "Deliveries",
                    {
                        {"deliveries/2019", "2019"},
                        {"deliveries/2020", "2020",
                            {
                                {"deliveries/2020/" + std::to_string(deliveries[0].id()),
                                    deliveries[0].name()}
                            }
                        }
                    }
                },
                {"releases", "Releases"}
            }
        },
        {
            "deliveries/2020/" + std::to_string(deliveries[0].id()) + "/releases",
            {
                {"deliveries", "Deliveries",
                    {
                        {"deliveries/2019", "2019"},
                        {"deliveries/2020", "2020",
                            {
                                {"deliveries/2020/" + std::to_string(deliveries[0].id()),
                                    deliveries[0].name(),
                                    {
                                        {"deliveries/2020/" + std::to_string(deliveries[0].id())
                                         + "/releases", "Releases"},
                                        {"deliveries/2020/" + std::to_string(deliveries[0].id())
                                         + "/unassigned", "Unassigned"}
                                    }
                                }
                            }
                        }
                    }
                },
                {"releases", "Releases"}
            }
        },
        {
            "deliveries/2020/" + std::to_string(deliveries[0].id()) + "/releases/new",
            {
                {"deliveries", "Deliveries",
                    {
                        {"deliveries/2019", "2019"},
                        {"deliveries/2020", "2020",
                            {
                                {"deliveries/2020/" + std::to_string(deliveries[0].id()),
                                    deliveries[0].name(),
                                    {
                                        {"deliveries/2020/" + std::to_string(deliveries[0].id())
                                         + "/releases", "Releases",
                                            {
                                                {"deliveries/2020/" + std::to_string(deliveries[0].id())
                                                 + "/releases/new", "new"},
                                                {"deliveries/2020/" + std::to_string(deliveries[0].id())
                                                 + "/releases/ready", "ready"},
                                                {"deliveries/2020/" + std::to_string(deliveries[0].id())
                                                 + "/releases/testing", "testing"},
                                                {"deliveries/2020/" + std::to_string(deliveries[0].id())
                                                 + "/releases/production", "production"}
                                            }
                                        },
                                        {"deliveries/2020/" + std::to_string(deliveries[0].id())
                                         + "/unassigned", "Unassigned"}
                                    }
                                }
                            }
                        }
                    }
                },
                {"releases", "Releases"}
            }
        },
        {
            "unknown",
            {
                {"deliveries", "Deliveries"},
                {"releases", "Releases"}
            }
        },
        {
            "",
            {
                {"deliveries", "Deliveries"},
                {"releases", "Releases"}
            }
        },
    };

    for (const auto&[nodeId, nodes]: testData) {
        SCOPED_TRACE(nodeId);
        auto request = http::MockRequest(http::GET,
            http::URL(GET_NODE_SIBLINGS_URL).addParam("node_id", nodeId));
        request.headers.emplace("Accept", "application/x-protobuf");
        auto response = yacare::performTestRequest(request);
        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.headers.at("Content-Type"), "application/x-protobuf");

        auto sprotoTreeNodes = boost::lexical_cast<sproto_helpers::snavigation::TreeNodes>(response.body);

        EXPECT_THAT(sprotoTreeNodes.nodes(),
            ::testing::Pointwise(TreeNodeEqual(), nodes)
        ) << " got result\n" << sproto::textDump(sprotoTreeNodes);
    }
}

TEST_F(NavigationFixture, test_get_node_siblings_for_each_traversable_node)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    forEachNode(
        [](const sproto_helpers::snavigation::TreeNode& node) {
            SCOPED_TRACE(node.id());
            auto request = http::MockRequest(http::GET,
                http::URL(GET_NODE_SIBLINGS_URL).addParam("node_id", node.id()));
            request.headers.emplace("Accept", "application/x-protobuf");
            auto response = yacare::performTestRequest(request);
            EXPECT_EQ(response.status, 200);
            EXPECT_EQ(response.headers.at("Content-Type"), "application/x-protobuf");
            auto sprotoTreeNodes = boost::lexical_cast<sproto_helpers::snavigation::TreeNodes>(response.body);
        }
    );
}

TEST_F(NavigationFixture, test_get_node_bounding_box)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    struct TestDatum {
        std::string nodeId;
        std::optional<geolib3::BoundingBox> bbox;
    };

    std::vector<TestDatum> testData{
        {"", std::nullopt},
        {"deliveries/2020/1", geolib3::BoundingBox({107.97, 52.67}, {108.15, 52.76})},
        {"deliveries/2020/1/unassigned", geolib3::BoundingBox({107.97, 52.67}, {108.15, 52.76})},
        {"deliveries/2020/1/releases/new", geolib3::BoundingBox({107.97, 52.67}, {108.15, 52.76})},
        {"deliveries/2019/2", std::nullopt},
        {"releases", geolib3::BoundingBox({107.97, 52.67}, {108.15, 52.76})},
        {"releases/20", std::nullopt},
        {"unknown", std::nullopt},
    };

    for (const auto&[nodeId, optBbox]: testData) {
        SCOPED_TRACE(nodeId);
        auto request = http::MockRequest(http::GET,
            http::URL(GET_NODE_BOUNDING_BOX_URL).addParam("node_id", nodeId));
        request.headers.emplace("Accept", "application/x-protobuf");
        auto response = yacare::performTestRequest(request);

        if (!optBbox) {
            EXPECT_EQ(response.status, 204);
            continue;
        }

        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.headers.at("Content-Type"), "application/x-protobuf");
        const auto sprotoBoundingBox =
            boost::lexical_cast<sproto_helpers::snavigation::NodeBoundingBox>(response.body);
        const auto geoBbox = geolib3::sproto::decode(sprotoBoundingBox.boundingBox());
        EXPECT_THAT(geoBbox, BboxNear(optBbox.value()));
    }
}

TEST_F(NavigationFixture, test_get_node_bounding_box_for_each_traversable_node)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    forEachNode(
        [](const sproto_helpers::snavigation::TreeNode& node) {
            SCOPED_TRACE(node.id());
            auto request = http::MockRequest(http::GET,
                http::URL(GET_NODE_BOUNDING_BOX_URL).addParam("node_id", node.id()));
            request.headers.emplace("Accept", "application/x-protobuf");
            auto response = yacare::performTestRequest(request);
            if (response.status == 200) {
                EXPECT_EQ(response.headers.at("Content-Type"), "application/x-protobuf");
                auto sprotoBoundingBox =
                    boost::lexical_cast<sproto_helpers::snavigation::NodeBoundingBox>(response.body);
                geolib3::sproto::decode(sprotoBoundingBox.boundingBox());
            } else {
                EXPECT_THAT(response.status, 204);
            }
        }
    );
}

TEST_F(NavigationFixture, test_get_node_objects_count)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    struct TestDatum {
        std::string nodeId;
        int64_t mosaicsCount;
        int64_t mosaicSourcesCount;
    };

    std::vector<TestDatum> testData{
        {"", 0, 0},
        {"deliveries/2020/1", 0, 5},
        {"deliveries/2020/1/unassigned", 0, 1},
        {"deliveries/2020/1/releases/new", 0, 1},
        {"releases", 4, 0},
        {"unknown", 0, 0}
    };

    for (const auto&[nodeId, mosaicsCount, mosaicSourcesCount]: testData) {
        SCOPED_TRACE(nodeId);
        auto request = http::MockRequest(http::GET,
            http::URL(GET_NODE_OBJECTS_COUNT_URL).addParam("node_id", nodeId));
        request.headers.emplace("Accept", "application/x-protobuf");
        auto response = yacare::performTestRequest(request);

        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.headers.at("Content-Type"), "application/x-protobuf");

        auto sprotoResponse =
            boost::lexical_cast<sproto_helpers::snavigation::NodeObjectsCount>(response.body);
        EXPECT_EQ(sprotoResponse.mosaicsCount(), mosaicsCount);
        EXPECT_EQ(sprotoResponse.mosaicSourcesCount(), mosaicSourcesCount);
    }
}

TEST_F(NavigationFixture, test_get_node_objects)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    struct TestDatum {
        std::string nodeId;
        db::Ids mosaicsIds;
        db::Ids mosaicSourcesIds;
    };

    std::vector<TestDatum> testData{
        {"", {}, {}},
        {"deliveries/2020/1", {},
            {
                mosaicSources.at(0).id(),
                mosaicSources.at(1).id(),
                mosaicSources.at(2).id(),
                mosaicSources.at(3).id(),
                mosaicSources.at(4).id(),
            }
        },
        {"deliveries/2020/1/unassigned", {}, {mosaicSources.at(0).id()}},
        {"deliveries/2020/1/releases/new", {}, {mosaicSources.at(1).id()}},
        {"releases",
            {
                mosaics.at(0).id(),
                mosaics.at(1).id(),
                mosaics.at(2).id(),
                mosaics.at(3).id()
            },
            {}
        },
        {"unknown", {}, {}}
    };

    for (const auto&[nodeId, mosaicIds, mosaicSourcesIds]: testData) {
        SCOPED_TRACE(nodeId);
        auto request = http::MockRequest(http::GET,
            http::URL(GET_NODE_OBJECTS_URL).addParam("node_id", nodeId));
        request.headers.emplace("Accept", "application/x-protobuf");
        auto response = yacare::performTestRequest(request);

        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.headers.at("Content-Type"), "application/x-protobuf");

        auto sprotoResponse = boost::lexical_cast<sproto_helpers::snavigation::NodeObjects>(response.body);

        EXPECT_THAT(sprotoResponse.mosaics(),
            ::testing::UnorderedPointwise(IdsEqual(), mosaicIds));
        EXPECT_THAT(sprotoResponse.mosaicSources(),
            ::testing::UnorderedPointwise(IdsEqual(), mosaicSourcesIds));
    }
}

TEST_F(NavigationFixture, test_get_node_info)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    struct TestDatum {
        std::string nodeId;
        double areaSqrKm;
        int64_t tilesNumber;
    };

    std::vector<TestDatum> testData{
        {"", 0., 0},
        {"deliveries/2020/1", 103.772, 0},
        {"deliveries/2020/1/unassigned", 103.772, 0},
        {"releases", 102.516, 63490},
        {"unknown", 0, 0}
    };

    for (const auto&[nodeId, areaSqrKm, tilesNumber]: testData) {
        SCOPED_TRACE(nodeId);
        auto request = http::MockRequest(http::GET,
            http::URL(GET_NODE_INFO_URL).addParam("node_id", nodeId));
        request.headers.emplace("Accept", "application/x-protobuf");
        auto response = yacare::performTestRequest(request);

        EXPECT_EQ(response.status, 200);
        EXPECT_EQ(response.headers.at("Content-Type"), "application/x-protobuf");

        auto sprotoResponse = boost::lexical_cast<sproto_helpers::snavigation::NodeInfo>(response.body);

        double responseArea = sprotoResponse.areaSquareKm();
        EXPECT_THAT(responseArea,
            ::testing::DoubleNear(areaSqrKm, 0.1));
        EXPECT_EQ(sprotoResponse.tilesNumber(), tilesNumber);
    }
}

TEST_F(NavigationFixture, test_get_corresponding_node)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    struct TestDatum {
        std::optional<db::Id> releaseId;
        std::optional<db::Id> deliveryId;
        std::optional<std::string> nodeId;
    };

    std::vector<TestDatum> testData{
        {releases[0].id(), {}, "releases/new/" + std::to_string(releases[0].id())},
        {{}, deliveries[0].id(), "deliveries/2020/" + std::to_string(deliveries[0].id())},
        {100500, {}, {}},
        {{}, 100500, {}},
    };

    for (const auto&[releaseId, deliveryId, nodeId]: testData) {
        http::URL url(FIND_CORRESPONDING_NODE_URL);

        if (releaseId.has_value()) {
            url.addParam("release_id", releaseId.value());
        }

        if (deliveryId.has_value()) {
            url.addParam("delivery_id", deliveryId.value());
        }

        SCOPED_TRACE(url);

        auto request = http::MockRequest(http::GET, url);
        auto response = yacare::performTestRequest(request);

        if (nodeId.has_value()) {
            EXPECT_EQ(response.status, 200);
            EXPECT_EQ(response.body, nodeId.value());
        } else {
            EXPECT_EQ(response.status, 404);
        }

    }
}

TEST_F(NavigationFixture, test_export_mosaics)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto request = http::MockRequest(http::GET,
        http::URL(EXPORT_URL).addParam("node_id", "releases/new"));
    auto response = yacare::performTestRequest(request);

    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(response.headers.at("Content-Type"), "application/json");

    auto jsonValue = json::Value::fromString(response.body);
    EXPECT_EQ(jsonValue["type"].as<std::string>(), "FeatureCollection");
    auto jsonFeatures = jsonValue["features"];
    ASSERT_EQ(jsonFeatures.size(), 1u);
    const auto& firstFeature = jsonFeatures[0];
    EXPECT_EQ(firstFeature["geometry"]["type"].as<std::string>(), "MultiPolygon");
    const auto& jsonProperties = firstFeature["properties"];
    EXPECT_EQ(jsonProperties["name"].as<std::string>(), mosaicSources.at(1).name());
    EXPECT_EQ(jsonProperties["zmin"].as<uint32_t>(), mosaics.at(0).minZoom());
    EXPECT_EQ(jsonProperties["zmax"].as<uint32_t>(), mosaics.at(0).maxZoom());
    EXPECT_EQ(jsonProperties["zorder"].as<int>(), mosaics.at(0).zOrder());
    EXPECT_EQ(jsonProperties["release_id"].as<int>(), releases.at(0).id());
    EXPECT_EQ(jsonProperties["release_name"].as<std::string>(), releases.at(0).name());
    EXPECT_EQ(jsonProperties["release_status"].as<std::string>(), "new");
}

/*
TEST_F(NavigationFixture, test_export_mosaic_sources)
{
    yacare::tests::UserInfoFixture userInfoFixture(registerTestUser(*txnHandle()));

    auto request = http::MockRequest(http::GET,
            http::URL(EXPORT_URL).addParam("node_id", "deliveries/2020/1/releases/new"));
    auto response = yacare::performTestRequest(request);

    EXPECT_EQ(response.status, 200);
    EXPECT_EQ(response.headers.at("Content-Type"), "application/json");
    EXPECT_EQ(response.headers.at("Content-Disposition"), "attachment");

    auto jsonValue = json::Value::fromString(response.body);
    EXPECT_EQ(jsonValue["type"].as<std::string>(), "FeatureCollection");
    auto jsonFeatures = jsonValue["features"];
    ASSERT_EQ(jsonFeatures.size(), 1u);
    const auto& firstFeature = jsonFeatures[0];
    EXPECT_EQ(firstFeature["geometry"]["type"].as<std::string>(), "MultiPolygon");
    const auto& jsonProperties = firstFeature["properties"];
    EXPECT_EQ(jsonProperties["name"].as<std::string>(), mosaicSources.at(1).name());
}

TEST_F(NavigationFixture, test_navigation_no_authorization)
{
    const auto login = "john";
    auth::UserInfo userInfo{};
    userInfo.setLogin(login);
    yacare::tests::UserInfoFixture userInfoFixture(userInfo);

    http::MockRequest request(
        http::GET,
        http::URL(LIST_CHILDREN_URL)
            .addParam("node_id", "releases/new")
    );
    auto response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(GET_NODE_SIBLINGS_URL)
            .addParam("node_id", "releases/new")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(GET_NODE_BOUNDING_BOX_URL)
            .addParam("node_id", "releases/new")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(GET_NODE_OBJECTS_COUNT_URL)
            .addParam("node_id", "releases/new")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(GET_NODE_OBJECTS_URL)
            .addParam("node_id", "releases/new")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(FIND_CORRESPONDING_NODE_URL)
            .addParam("node_id", "releases/new")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(GET_NODE_INFO_URL)
            .addParam("node_id", "releases/new")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);

    request = http::MockRequest(
        http::GET,
        http::URL(EXPORT_URL)
            .addParam("node_id", "releases/new")
    );
    response = yacare::performTestRequest(request);
    EXPECT_EQ(response.status, 403);
}
*/

} // namespace maps::factory::backend::tests
