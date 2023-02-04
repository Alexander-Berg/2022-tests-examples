#include "common.h"
#include "helpers.h"
#include <maps/wikimap/mapspro/libs/revision/sql_strings.h>
#include <maps/wikimap/mapspro/libs/revision/context.h>
#include <maps/wikimap/mapspro/libs/revision/reader_impl.h>

#include <yandex/maps/wiki/revision/revisionsgateway.h>
#include <yandex/maps/wiki/revision/branch_manager.h>

#include <boost/test/unit_test.hpp>

namespace maps {
namespace wiki {
namespace revision {
namespace tests {

namespace {

struct FilterTestDatabaseCreator: public DbFixture
{
    FilterTestDatabaseCreator()
    {
        setTestData("sql/004-FiltersTest.sql");
    }
};

} // namespace

BOOST_FIXTURE_TEST_SUITE(FiltersTest, FilterTestDatabaseCreator)

BOOST_AUTO_TEST_CASE(test_concurrent_geometry_filters)
{
    pqxx::work txn(getConnection());
    ReaderImpl reader(
        txn,
        BranchType::Trunk,
        TRUNK_BRANCH_ID,
        DescriptionLoadingMode::Skip
    );

    auto orFilter = (
        (
            filters::Geom::intersects(0, 1, 0, 1) &&
            filters::ObjRevAttr::isRegularObject()
        ) ||
        (
            filters::Geom::intersects(2, 3, 2, 3) &&
            filters::ObjRevAttr::isRelation()
        )
    );

    auto queryForRevisionIds = reader.makeQuery(
        db::StructuredQueryProvider::revisionId(
            txn.conn(),
            orFilter
        ),
        LoadLimitations::None,
        NO_SNAPSHOT_ID,
        NO_LIMIT
    );

    const std::string EXPECTED_QUERY_FOR_REVISION_IDS =
        " SELECT o.object_id, o.commit_id "
        "FROM revision.object_revision_with_geometry o "
            "JOIN revision.commit c ON (o.commit_id = c.id)  "
        "WHERE ("
            "(((o.geometry_id > 0 AND o.geometry_id IN ("
                "SELECT id FROM revision.geometry "
                "WHERE contents && ST_SetSRID(ST_MakeBox2D(ST_MakePoint(0,1),ST_MakePoint(0,1)),3395)"
            "))) AND "
            "(o.master_object_id = 0))"
        ")";

    BOOST_CHECK_EQUAL(
        queryForRevisionIds,
        EXPECTED_QUERY_FOR_REVISION_IDS
    );


    auto queryForRevisions = reader.makeQuery(
        db::StructuredQueryProvider::revision(
            txn.conn(),
            orFilter
        ),
        LoadLimitations::None,
        NO_SNAPSHOT_ID,
        NO_LIMIT
    );

    const std::string allFields =
        "o.object_id, o.commit_id, o.deleted, o.prev_commit_id, o.next_commit_id, o.attributes_id, "
            "o.geometry_id, o.description_id, o.master_object_id, o.slave_object_id, "
            "hstore_to_array(a.contents)  AS attributes";

    const std::string EXPECTED_QUERY_FOR_REVISIONS =
        " SELECT " + allFields + " "
        "FROM revision.object_revision_with_geometry o "
            "JOIN revision.commit c ON (o.commit_id = c.id)  "
            "LEFT JOIN revision.attributes a ON (o.attributes_id = a.id)  "
        "WHERE ("
            "(((o.geometry_id > 0 AND o.geometry_id IN ("
                "SELECT id FROM revision.geometry "
                "WHERE contents && ST_SetSRID(ST_MakeBox2D(ST_MakePoint(0,1),ST_MakePoint(0,1)),3395)"
            "))) AND "
            "(o.master_object_id = 0))"
        ")";

    BOOST_CHECK_EQUAL(
        queryForRevisions,
        EXPECTED_QUERY_FOR_REVISIONS
    );
}

BOOST_AUTO_TEST_CASE(test_geometry_types_filters)
{
    pqxx::work txn(getConnection());
    ReaderImpl reader(
        txn,
        BranchType::Trunk,
        TRUNK_BRANCH_ID,
        DescriptionLoadingMode::Skip
    );

    auto orFilter =
        filters::GeomFilterExpr(
            filters::GeomFilterExpr::Operation::IntersectsPoints |
            filters::GeomFilterExpr::Operation::IntersectsLinestrings, 0, 1, 0, 1)
        ||
        filters::GeomFilterExpr(
            filters::GeomFilterExpr::Operation::IntersectsPolygons, 2, 3, 2, 3);

    auto query = reader.makeQuery(
        db::StructuredQueryProvider::revisionId(
            txn.conn(),
            orFilter
        ),
        LoadLimitations::None,
        NO_SNAPSHOT_ID,
        NO_LIMIT
    );

    const std::string EXPECTED_QUERY =
        " SELECT o.object_id, o.commit_id "
        "FROM revision.object_revision_with_geometry o "
            "JOIN revision.commit c ON (o.commit_id = c.id)  "
        "WHERE ("
            "(((o.geometry_id > 0 AND o.geometry_id IN ("
                "SELECT id FROM revision.geometry "
                "WHERE contents && ST_SetSRID(ST_MakeBox2D(ST_MakePoint(0,1),ST_MakePoint(0,1)),3395)"
                " AND ST_GeometryType(contents) IN ('ST_Point','ST_LineString')"
            ")))"
            " OR "
            "((o.geometry_id > 0 AND o.geometry_id IN ("
                "SELECT id FROM revision.geometry "
                "WHERE contents && ST_SetSRID(ST_MakeBox2D(ST_MakePoint(2,3),ST_MakePoint(2,3)),3395)"
                " AND ST_GeometryType(contents)='ST_Polygon'"
            "))))"
        ")";

    BOOST_CHECK_EQUAL(
        query,
        EXPECTED_QUERY
    );
}

BOOST_AUTO_TEST_CASE(test_no_geometry_types_filters)
{
    pqxx::work txn(getConnection());
    ReaderImpl reader(
        txn,
        BranchType::Trunk,
        TRUNK_BRANCH_ID,
        DescriptionLoadingMode::Skip
    );

    auto filter =
        filters::Attr("cat:rd").defined() &&
        !filters::Geom::defined() &&
        filters::ObjRevAttr::isNotDeleted() &&
        filters::ObjRevAttr::isNotRelation();

    auto query = reader.makeQuery(
        db::StructuredQueryProvider::revisionId(
            txn.conn(),
            filter
        ),
        LoadLimitations::None,
        SnapshotId::fromCommit(100500, BranchType::Trunk, txn),
        NO_LIMIT
    );

    const std::string EXPECTED_QUERY =
        " SELECT o.object_id, o.commit_id "
        "FROM revision.object_revision_without_geometry o JOIN revision.commit c ON (o.commit_id = c.id)  "
            "LEFT JOIN revision.attributes a ON (o.attributes_id = a.id)  "
        "WHERE "
            "(((((((a.contents ? 'cat:rd')"
            " AND (NOT (o.geometry_id > 0))))"
            " AND (NOT o.deleted))) AND (o.slave_object_id = 0)))"
            " AND ((c.id <= 100500 AND o.commit_id <= 100500))";

    BOOST_CHECK_EQUAL(
        query,
        EXPECTED_QUERY
    );
}

BOOST_AUTO_TEST_CASE(test_common_filters)
{
    pqxx::work txn(getConnection());
    ReaderImpl reader(
        txn,
        BranchType::Trunk,
        TRUNK_BRANCH_ID,
        DescriptionLoadingMode::Skip
    );

    auto filter =
        filters::Attr("cat:rd").defined() &&
        //!filters::Geom::defined() &&
        filters::ObjRevAttr::isNotDeleted() &&
        filters::ObjRevAttr::isNotRelation();

    auto query = reader.makeQuery(
        db::StructuredQueryProvider::revisionId(
            txn.conn(),
            filter
        ),
        LoadLimitations::None,
        SnapshotId::fromCommit(100500, BranchType::Trunk, txn),
        NO_LIMIT
    );

    const std::string EXPECTED_QUERY =
        " SELECT o.object_id, o.commit_id "
        "FROM revision.object_revision o JOIN revision.commit c ON (o.commit_id = c.id)  "
            "LEFT JOIN revision.attributes a ON (o.attributes_id = a.id)  "
        "WHERE "
            "(((((a.contents ? 'cat:rd')"
            " AND (NOT o.deleted))) AND (o.slave_object_id = 0)))"
            " AND ((c.id <= 100500 AND o.commit_id <= 100500))";

    BOOST_CHECK_EQUAL(
        query,
        EXPECTED_QUERY
    );
}

BOOST_AUTO_TEST_CASE(test_common_not_filters)
{
    pqxx::work txn(getConnection());
    ReaderImpl reader(
        txn,
        BranchType::Trunk,
        TRUNK_BRANCH_ID,
        DescriptionLoadingMode::Skip
    );

    auto filter =
        !filters::Attr("cat:rd").defined() &&
        //!filters::Geom::defined() &&
        filters::ObjRevAttr::isNotDeleted() &&
        filters::ObjRevAttr::isNotRelation();

    auto query = reader.makeQuery(
        db::StructuredQueryProvider::revisionId(
            txn.conn(),
            filter
        ),
        LoadLimitations::None,
        SnapshotId::fromCommit(100500, BranchType::Trunk, txn),
        NO_LIMIT
    );

    const std::string EXPECTED_QUERY =
        " SELECT o.object_id, o.commit_id "
        "FROM revision.object_revision o JOIN revision.commit c ON (o.commit_id = c.id)  "
            "LEFT JOIN revision.attributes a ON (o.attributes_id = a.id)  "
        "WHERE "
            "(((((NOT (a.contents ? 'cat:rd'))"
            " AND (NOT o.deleted))) AND (o.slave_object_id = 0)))"
            " AND ((c.id <= 100500 AND o.commit_id <= 100500))";

    BOOST_CHECK_EQUAL(
        query,
        EXPECTED_QUERY
    );
}

BOOST_AUTO_TEST_CASE(test_filters_creation)
{
    filters::Attr("cat:rd_el").defined() && (filters::Attr("rd_el:dr") == "1");
}

BOOST_AUTO_TEST_CASE(test_eval_query_object_revision_attr)
{
    const auto attrObjectId = filters::ObjRevAttr::objectId();

    {
        auto filter1 = attrObjectId.in( DBIDSet { 1 } );

        filters::Context ctx(getConnection());

        BOOST_CHECK_EQUAL(filter1.evalQuery(ctx), "o.object_id=1"); // in -> eq

        auto filter13 = attrObjectId.in( DBIDSet { 1, 3 } );
        BOOST_CHECK_EQUAL(
            filter13.evalQuery(ctx),
            "(o.object_id>0 AND o.object_id<4 AND o.object_id IN (1,3))");
    }

    DBIDSet ids { 1, 2, 3, 4, 5, 6, 7 };

    auto filter = attrObjectId.in(ids) && filters::ObjRevAttr::isRelation();

    {
        filters::Context ctx(getConnection());

        BOOST_CHECK_EQUAL(
            filter.evalQuery(ctx),
            "(((o.object_id>0 AND o.object_id<8)) AND (o.slave_object_id <> 0))");
    }
}

BOOST_AUTO_TEST_CASE(test_eval_query_object_revision_attr_between)
{
    const auto attrObjectId = filters::ObjRevAttr::objectId();
    DBIDSet ids { 2, 5 };
    auto filter = attrObjectId.between(ids);
    filters::Context ctx(getConnection());
    BOOST_CHECK_EQUAL(filter.evalQuery(ctx), "o.object_id BETWEEN 2 AND 5");
}

BOOST_AUTO_TEST_CASE(test_gateway_revision_ids_filter_simple)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gtw(txn); // Trunk
    auto snapshot = gtw.snapshot(gtw.maxSnapshotId());

    BOOST_CHECK_THROW(snapshot.tryLoadRevisionIdsByFilter(filters::True(), 0), maps::Exception);

    {
        auto filter =
            filters::Attr("cat:rd_el").defined() &&
                filters::Geom::intersects(4172793, 7393470, 4184909, 7402847);

        BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filter).size(), 629);
        {
            auto revIdsLimited = snapshot.tryLoadRevisionIdsByFilter(filter, 629);
            BOOST_REQUIRE(revIdsLimited);
            BOOST_CHECK_EQUAL((*revIdsLimited).size(), 629);
        }
        {
            auto revIdsLimited = snapshot.tryLoadRevisionIdsByFilter(filter, 630);
            BOOST_REQUIRE(revIdsLimited);
            BOOST_CHECK_EQUAL((*revIdsLimited).size(), 629);
        }
        {
            auto revIdsLimited = snapshot.tryLoadRevisionIdsByFilter(filter, 628);
            BOOST_CHECK(!revIdsLimited);
        }
    }

    {
        auto filter = filters::Attr("cat:rd_el").defined() && filters::Attr("rd_el:fc") == "6";

        BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filter).size(), 110);
        {
            auto revIdsLimited = snapshot.tryLoadRevisionIdsByFilter(filter, 110);
            BOOST_REQUIRE(revIdsLimited);
            BOOST_CHECK_EQUAL((*revIdsLimited).size(), 110);
        }
        {
            auto revIdsLimited = snapshot.tryLoadRevisionIdsByFilter(filter, 111);
            BOOST_REQUIRE(revIdsLimited);
            BOOST_CHECK_EQUAL((*revIdsLimited).size(), 110);
        }
        {
            auto revIdsLimited = snapshot.tryLoadRevisionIdsByFilter(filter, 109);
            BOOST_CHECK(!revIdsLimited);
        }
    }

    {
        auto filter = filters::Attr("cat:rd_el").defined()
            && filters::Attr("rd_el:fc") >= 6
            && filters::Attr("rd_el:fc") < 7;
        BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filter).size(), 110);
    }

    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(
        filters::Geom::defined()).size(), 1162);

    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(
        filters::Geom::intersects(4178318, 7397789, 4178518, 7397989)).size(), 1);

    {
        time_t FIRST_COMMIT_TIME = 1344862337L;

        auto filter = filters::CommitCreationTime() > FIRST_COMMIT_TIME;

        BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filter).size(), 1);
    }
}


BOOST_AUTO_TEST_CASE(test_gateway_revision_ids_filter_precedence)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gtw(txn); // Trunk
    auto snapshot = gtw.snapshot(gtw.maxSnapshotId());

    auto filter =
        filters::False() || filters::Attr("rd_el:srv_ra") == "1";

    BOOST_CHECK(snapshot.revisionIdsByFilter(filter).empty());
}


BOOST_AUTO_TEST_CASE(test_gateway_revision_ids_filter_commit)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gtw(txn); // Trunk
    auto snapshot = gtw.snapshot(gtw.maxSnapshotId());

    const size_t OBJECTS_SIZE = 1276;
    const size_t RELATIONS_SIZE = 1953;
    const size_t ALL_SIZE = OBJECTS_SIZE + RELATIONS_SIZE; // 3229

    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filters::True()).size(), ALL_SIZE);

    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filters::ObjRevAttr::isNotRelation()).size(), OBJECTS_SIZE);
    BOOST_CHECK_EQUAL(snapshot.relationsByFilter(filters::True()).size(), RELATIONS_SIZE);

    auto filter = filters::ObjRevAttr::masterObjectId().in(std::vector<DBID>(1, 1022541));

    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filter).size(), 2);
    BOOST_CHECK_EQUAL(snapshot.relationsByFilter(filter).size(), 2);
}


BOOST_AUTO_TEST_CASE(test_proxy_filter)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gtw(txn); // Trunk
    auto snapshot = gtw.snapshot(gtw.maxSnapshotId());

    filters::ProxyFilterExpr filter = filters::True();
    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filter).size(), 3229);
    filter &= filters::Attr("cat:rd_el").defined();
    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filter).size(), 629);
    filter &= filters::Attr("rd_el:fc") == "6";
    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filter).size(), 110);
}


BOOST_AUTO_TEST_CASE(test_filter_attrs_defined)
{
    typedef std::set<std::string> StringSet;

    pqxx::work txn(getConnection());

    RevisionsGateway gtw(txn); // Trunk
    auto snapshot = gtw.snapshot(gtw.maxSnapshotId());

    const size_t RD_EL_COUNT = 629;
    const size_t RD_JC_COUNT = 533;
    const size_t ROLE_COUNT = 1953;

    auto rdElCount = snapshot.revisionIdsByFilter(
         filters::Attr("cat:rd_el").defined()).size();
    BOOST_CHECK_EQUAL(rdElCount, RD_EL_COUNT);

    auto rdJcCount = snapshot.revisionIdsByFilter(
         filters::Attr("cat:rd_jc").defined()).size();
    BOOST_CHECK_EQUAL(rdJcCount, RD_JC_COUNT);

    StringSet rdCategories {"cat:rd_el"};
    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(
        filters::Attr::definedAll(rdCategories)).size(),
        RD_EL_COUNT);
    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(
        filters::Attr::definedAny(rdCategories)).size(),
        RD_EL_COUNT);

    rdCategories.insert("cat:rd_jc");
    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(
        filters::Attr::definedAll(rdCategories)).size(), 0);
    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(
        filters::Attr::definedAny(rdCategories)).size(),
        RD_EL_COUNT + RD_JC_COUNT);


    auto filterRoleDefined = filters::Attr("rel:role").defined();
    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(filterRoleDefined).size(), ROLE_COUNT);

    auto roleCountResult = snapshot.tryLoadRevisionIdsByFilter(filterRoleDefined, ROLE_COUNT);
    BOOST_REQUIRE(roleCountResult);
    BOOST_CHECK_EQUAL(roleCountResult->size(), ROLE_COUNT);

    BOOST_REQUIRE(!snapshot.tryLoadRevisionIdsByFilter(filterRoleDefined, ROLE_COUNT - 1));

    StringSet relCategories {"rel:master", "rel:slave"};
    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(
         filters::Attr::definedAll(relCategories)).size(), ROLE_COUNT);
    BOOST_CHECK_EQUAL(snapshot.revisionIdsByFilter(
         filters::Attr::definedAny(relCategories)).size(), ROLE_COUNT);

    BOOST_CHECK_EQUAL(snapshot.relationsByFilter(filterRoleDefined).size(), ROLE_COUNT);

    auto roleCountResult2 = snapshot.tryLoadRelationsByFilter(filterRoleDefined, ROLE_COUNT);
    BOOST_REQUIRE(roleCountResult2);
    BOOST_CHECK_EQUAL(roleCountResult2->size(), ROLE_COUNT);

    BOOST_REQUIRE(!snapshot.tryLoadRelationsByFilter(filterRoleDefined, ROLE_COUNT - 1));
}

BOOST_AUTO_TEST_CASE(test_filter_commit_attributes)
{
    pqxx::work txn(getConnection());

    RevisionsGateway gtw(txn); // Trunk
    auto snapshot = gtw.snapshot(gtw.maxSnapshotId());

    auto branch = BranchManager(txn).loadTrunk();
    auto commitsFilter =
        revision::filters::CommitAttribute("description").defined()
        && revision::filters::CommitAttr::isVisible(branch);
    auto commits = revision::Commit::load(txn, commitsFilter);
    BOOST_CHECK_EQUAL(commits.size(), 2);

    auto ids = revision::Commit::loadIds(txn, commitsFilter);
    BOOST_CHECK_EQUAL(ids.size(), 2);
}

BOOST_AUTO_TEST_SUITE_END()

} // namespace tests
} // namespace revision
} // namespace wiki
} // namespace maps
