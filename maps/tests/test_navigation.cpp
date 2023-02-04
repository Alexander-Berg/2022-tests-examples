#include <maps/factory/libs/navigation/node.h>

#include <maps/factory/libs/db/delivery_gateway.h>
#include <maps/factory/libs/db/mosaic_gateway.h>
#include <maps/factory/libs/db/mosaic_source_gateway.h>
#include <maps/factory/libs/db/project_gateway.h>
#include <maps/factory/libs/db/release_gateway.h>
#include <maps/factory/libs/db/source_gateway.h>
#include <maps/factory/libs/unittest/fixture.h>

#include <maps/libs/log8/include/log8.h>
#include <maps/libs/pgpool/include/pgpool3.h>
#include <maps/libs/stringutils/include/join.h>

#include <maps/libs/introspection/include/comparison.h>
#include <maps/libs/introspection/include/hashing.h>
#include <maps/libs/introspection/include/stream_output.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::factory::navigation {

using maps::introspection::operator==;
using maps::introspection::operator!=;
using maps::introspection::operator<;
using maps::introspection::operator<=;
using maps::introspection::operator>;
using maps::introspection::operator>=;

using maps::introspection::operator<<;

} // namespace maps::factory::navigation

namespace maps::factory::navigation::tests {

using namespace testing;

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

class Fixture : public testing::Test, public unittest::BaseFixture {
public:
    Fixture()
        : pgpool(
        postgres().connectionString(),
        pgpool3::PoolConstants(1, 5, 1, 5)
    )
    {
        log8::setLevel(log8::Level::DEBUG);
        insertData();
    }

    pgpool3::Pool& pgPool() { return pgpool; }

    pgpool3::TransactionHandle txnHandle()
    {
        return pgpool.masterWriteableTransaction();
    }

    void insertData()
    {
        auto txn = txnHandle();
        releases = std::vector<db::Release>{
            db::Release("draft_release").setStatus(db::ReleaseStatus::New),
            db::Release("ready_release").setStatus(db::ReleaseStatus::Ready),
            db::Release("testing_release").setStatus(db::ReleaseStatus::Testing),
            db::Release("production_release").setStatus(db::ReleaseStatus::Production)
        };
        db::ReleaseGateway(*txn).insert(releases);

        auto source = db::Source("source_name", db::SourceType::Local, "path");
        db::SourceGateway(*txn).insert(source);

        deliveries = std::vector<db::Delivery>{
            db::Delivery(source.id(), "2020-01-01", "delivery_2020", "subfolder1").setYear(2020),
            db::Delivery(source.id(), "2019-01-01", "delivery_2019", "subfolder2").setYear(2019)
        };
        db::DeliveryGateway(*txn).insert(deliveries);

        projects = std::vector<db::Project>{{"Moscow"}, {"Nino"}};
        db::ProjectGateway(*txn).insert(projects);

        const auto addMs = [&](const std::string& n, db::MosaicSourceStatus st, db::Id proj = -1) {
            db::MosaicSource ms(n);
            ms.setStatus(st);
            ms.setDeliveryId(deliveries.at(0).id());
            if (proj >= 0) { ms.setProjectId(projects.at(proj).id()); }
            mosaicSources.push_back(ms);
        };
        mosaicSources.clear();
        addMs("unassigned", db::MosaicSourceStatus::Ready, 1);
        addMs("draft", db::MosaicSourceStatus::Ready, 0);
        addMs("ready", db::MosaicSourceStatus::Ready);
        addMs("testing", db::MosaicSourceStatus::Ready);
        addMs("production", db::MosaicSourceStatus::Ready);
        addMs("unassigned_new", db::MosaicSourceStatus::New, 1);
        db::MosaicSourceGateway(*txn).insert(mosaicSources);

        const auto addM = [&](db::Id ms, db::Id rel) {
            db::Mosaic m(mosaicSources.at(ms).id(), 0, 0, 0, MOSAIC_GEOMETRY);
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

    pgpool3::Pool pgpool;
    std::vector<db::Release> releases;
    std::vector<db::Delivery> deliveries;
    std::vector<db::Project> projects;
    std::vector<db::MosaicSource> mosaicSources;
    std::vector<db::Mosaic> mosaics;
};

TEST_F(Fixture, test_list_roots)
{
    const auto& treeExplorer = getTreeExplorer();
    auto txn = txnHandle();

    auto treeNodes = treeExplorer.listRoots(*txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode("deliveries", "Deliveries"),
                TreeNode("releases", "Releases")
            }
        )
    );
}

TEST_F(Fixture, test_children_nodes)
{
    const auto& treeExplorer = getTreeExplorer();
    auto txn = txnHandle();
    auto treeNodes = treeExplorer.listChildrenNodes({"deliveries"}, *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode("2019", "2019"),
                TreeNode("2020", "2020")
            }
        )
    );

    treeNodes = treeExplorer.listChildrenNodes(
        {"deliveries", "2020"}, *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode(std::to_string(deliveries[0].id()), deliveries[0].name())
            }
        )
    );

    ASSERT_TRUE(treeNodes[0].hasObject());
    ASSERT_TRUE(std::holds_alternative<db::Delivery>(treeNodes[0].object()));
    EXPECT_EQ(std::get<db::Delivery>(treeNodes[0].object()).id(), deliveries[0].id());

    treeNodes = treeExplorer.listChildrenNodes(
        {"deliveries", "2020", std::to_string(deliveries[0].id())}, *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode("releases", "Releases"),
                TreeNode("unassigned", "Unassigned")
            }
        )
    );

    treeNodes = treeExplorer.listChildrenNodes(
        {"deliveries", "2020", std::to_string(deliveries[0].id()), "releases"}, *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode("new", "new"),
                TreeNode("ready", "ready"),
                TreeNode("testing", "testing"),
                TreeNode("production", "production")
            }
        )
    );

    treeNodes = treeExplorer.listChildrenNodes(
        {"deliveries", "2020", std::to_string(deliveries[0].id()), "releases", "new"}, *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode(std::to_string(releases[0].id()), releases[0].name())
                    .setIsLeaf(true)
            }
        )
    );

    treeNodes = treeExplorer.listChildrenNodes(
        {"deliveries", "2020", std::to_string(deliveries[0].id()), "unassigned"}, *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode(projects[1].id(), projects[1].id())
                    .setIsLeaf(true)
            }
        )
    );

    treeNodes = treeExplorer.listChildrenNodes(
        {"deliveries", "2019", std::to_string(deliveries[1].id())}, *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode("releases", "Releases"),
                TreeNode("unassigned", "Unassigned")
            }
        )
    );

    treeNodes = treeExplorer.listChildrenNodes(
        {"deliveries", "2019", std::to_string(deliveries[1].id()), "releases"}, *txn);

    EXPECT_TRUE(treeNodes.empty());

    treeNodes = treeExplorer.listChildrenNodes(
        {"deliveries", "2019", std::to_string(deliveries[1].id()), "unassigned"}, *txn);
    EXPECT_TRUE(treeNodes.empty());

    // Test 'releases' subtree

    treeNodes = treeExplorer.listChildrenNodes({"releases"}, *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode("new", "new"),
                TreeNode("ready", "ready"),
                TreeNode("testing", "testing"),
                TreeNode("production", "production")
            }
        )
    );

    treeNodes = treeExplorer.listChildrenNodes({"releases", "new"}, *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode(std::to_string(releases[0].id()), releases[0].name())
            }
        )
    );

    ASSERT_TRUE(treeNodes[0].hasObject());
    ASSERT_TRUE(std::holds_alternative<db::Release>(treeNodes[0].object()));
    EXPECT_EQ(std::get<db::Release>(treeNodes[0].object()).id(), releases[0].id());

    treeNodes = treeExplorer.listChildrenNodes(
        {"releases", "new", std::to_string(releases[0].id())}, *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode("deliveries", "Deliveries"),
                TreeNode("projects", "Projects"),
            }
        )
    );

    treeNodes = treeExplorer.listChildrenNodes(
        {"releases", "new", std::to_string(releases[0].id()), "projects"},
        *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode(projects[0].id(), projects[0].id())
                    .setIsLeaf(true)
            }
        )
    );

    treeNodes = treeExplorer.listChildrenNodes(
        {"releases", "new", std::to_string(releases[0].id()), "deliveries"},
        *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                TreeNode(std::to_string(deliveries[0].id()), deliveries[0].name())
                    .setIsLeaf(true)
            }
        )
    );

    treeNodes = treeExplorer.listChildrenNodes(
        {"releases", "new", std::to_string(releases[0].id()), "deliveries",
            std::to_string(deliveries[0].id())},
        *txn);
    EXPECT_TRUE(treeNodes.empty());
}

TEST_F(Fixture, test_list_siblings_on_path)
{
    const auto& treeExplorer = getTreeExplorer();
    auto txn = txnHandle();
    auto treeNodes = treeExplorer.listSiblingsOnPath(
        {"deliveries", "2020", std::to_string(deliveries[0].id()), "unassigned"},
        *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                std::vector<TreeNode>{
                    TreeNode("deliveries", "Deliveries"),
                    TreeNode("releases", "Releases")
                },
                std::vector<TreeNode>{
                    TreeNode("2019", "2019"),
                    TreeNode("2020", "2020")
                },
                std::vector<TreeNode>{
                    TreeNode(std::to_string(deliveries[0].id()), deliveries[0].name())
                },
                std::vector<TreeNode>{
                    TreeNode("releases", "Releases"),
                    TreeNode("unassigned", "Unassigned")
                }
            }
        )
    );

    treeNodes = treeExplorer.listSiblingsOnPath(
        {"deliveries", "not_exists", "unassigned"},
        *txn);

    EXPECT_THAT(treeNodes,
        ElementsAreArray(
            {
                std::vector<TreeNode>{
                    TreeNode("deliveries", "Deliveries"),
                    TreeNode("releases", "Releases")
                },
                std::vector<TreeNode>{
                    TreeNode("2019", "2019"),
                    TreeNode("2020", "2020")
                },
                std::vector<TreeNode>{}
            }
        )
    );
}

TEST_F(Fixture, test_make_filter)
{
    auto txn = txnHandle();
    const auto& treeExplorer = getTreeExplorer();

    struct TestData {
        NodePath nodePath;
        std::optional<std::vector<int64_t>> expectedMosaicIds;
        std::optional<std::vector<int64_t>> expectedMosaicSourceIds;
    };

    std::vector<TestData> testData{
        TestData{
            {},
            std::vector<int64_t>{mosaics[0].id(), mosaics[1].id(), mosaics[2].id(), mosaics[3].id()},
            std::vector<int64_t>{mosaicSources[0].id(), mosaicSources[1].id(), mosaicSources[2].id(),
                mosaicSources[3].id(), mosaicSources[4].id()}
        },
        TestData{
            {"releases"},
            std::vector<int64_t>{mosaics[0].id(), mosaics[1].id(), mosaics[2].id(), mosaics[3].id()},
            std::nullopt
        },
        TestData{
            {"releases", "new"},
            std::vector<int64_t>{mosaics[0].id()},
            std::nullopt
        },
        TestData{
            {"deliveries"},
            std::nullopt,
            std::vector<int64_t>{mosaicSources[0].id(), mosaicSources[1].id(), mosaicSources[2].id(),
                mosaicSources[3].id(), mosaicSources[4].id()}
        },
        TestData{
            {"deliveries", "2020", std::to_string(deliveries[0].id())},
            std::nullopt,
            std::vector<int64_t>{mosaicSources[0].id(), mosaicSources[1].id(), mosaicSources[2].id(),
                mosaicSources[3].id(), mosaicSources[4].id()}
        },
        TestData{
            {"deliveries", "2020", std::to_string(deliveries[0].id())},
            std::nullopt,
            std::vector<int64_t>{mosaicSources[0].id(), mosaicSources[1].id(), mosaicSources[2].id(),
                mosaicSources[3].id(), mosaicSources[4].id()}
        },
        TestData{
            {"deliveries", "2020", std::to_string(deliveries[0].id()), "releases", "new"},
            std::nullopt,
            std::vector<int64_t>{mosaicSources[1].id()}
        },
        TestData{
            {"deliveries", "2020", std::to_string(deliveries[0].id()), "unassigned"},
            std::nullopt,
            std::vector<int64_t>{mosaicSources[0].id()}
        }
    };

    for (const auto& testDatum: testData) {
        std::string nodePathStr = stringutils::join(testDatum.nodePath, "/");
        SCOPED_TRACE(nodePathStr);
        auto mosaicFilter = treeExplorer.makeFilterForMosaic(testDatum.nodePath);

        ASSERT_EQ(testDatum.expectedMosaicIds.has_value(),
            mosaicFilter.has_value());

        if (mosaicFilter.has_value()) {
            EXPECT_THAT(
                db::MosaicGateway(* txn).loadIds(mosaicFilter.value()),
                UnorderedElementsAreArray(testDatum.expectedMosaicIds.value())
            );
        }

        auto mosaicSourceFilter = treeExplorer.makeFilterForMosaicSource(testDatum.nodePath);
        ASSERT_EQ(testDatum.expectedMosaicSourceIds.has_value(),
            mosaicSourceFilter.has_value());

        if (mosaicSourceFilter.has_value()) {
            EXPECT_THAT(
                db::MosaicSourceGateway(* txn).loadIds(mosaicSourceFilter.value()),
                UnorderedElementsAreArray(testDatum.expectedMosaicSourceIds.value())
            );
        }
    }
}

} // namespace maps::factory::navigation::tests
