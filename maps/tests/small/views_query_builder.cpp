#include <maps/wikimap/mapspro/libs/views/include/query_builder.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::tests {

Y_UNIT_TEST_SUITE(views_query_builder) {

Y_UNIT_TEST(test_invalid_query_builder)
{
    for (auto branchId : {0,1}) {
        views::QueryBuilder qb(branchId);
        UNIT_CHECK_GENERATED_EXCEPTION(qb.query(), maps::Exception);

        qb.selectFields("id");
        UNIT_CHECK_GENERATED_EXCEPTION(qb.query(), maps::Exception);

        UNIT_CHECK_GENERATED_EXCEPTION(qb.fromTable("suggest_data", "sd"), maps::Exception); // only objects

        qb.fromTable(views::TABLE_OBJECTS, "o"); // without bm
        UNIT_CHECK_GENERATED_EXCEPTION(qb.query(), maps::Exception);

        qb.whereClause("True");
        UNIT_ASSERT_EQUAL(qb.query(), "SELECT id FROM objects o WHERE True");
    }
}

Y_UNIT_TEST(test_query_builder_with_bm)
{
    for (auto branchId : {0,1}) {
        views::QueryBuilder qb(branchId);
        qb.selectFields("id");
        qb.fromTable(views::TABLE_OBJECTS_C, "c");
        qb.whereClause("id=$1");

        if (branchId) {
            UNIT_ASSERT_EQUAL(
                qb.query(),
                "WITH bm AS (SELECT branch_mask_id FROM vrevisions_stable.branch_mask WHERE branches ? '1') "
                "SELECT id FROM vrevisions_stable.objects_c c "
                "WHERE "
                    "c.branch_mask_id IN (SELECT branch_mask_id FROM bm) AND ("
                    "id=$1"
                    ")");
        } else {
            UNIT_ASSERT_EQUAL(qb.query(), "SELECT id FROM objects_c c WHERE id=$1");
        }
    }
}

Y_UNIT_TEST(test_query_builder_with_bm_and_with)
{
    for (auto branchId : {0,1}) {
        views::QueryBuilder qb(branchId);
        qb.with("ggg", "id", "VALUES (100500::bigint)");
        qb.selectFields("c.id");
        qb.fromTable(views::TABLE_OBJECTS_C, "c");
        qb.fromWith("ggg");
        qb.whereClause("c.id=ggg.id");

        if (branchId) {
            UNIT_ASSERT_EQUAL(
                qb.query(),
                "WITH bm AS (SELECT branch_mask_id FROM vrevisions_stable.branch_mask WHERE branches ? '1') "
                ", ggg(id) AS (VALUES (100500::bigint)) "
                "SELECT c.id FROM ggg, vrevisions_stable.objects_c c "
                "WHERE "
                    "c.branch_mask_id IN (SELECT branch_mask_id FROM bm) AND ("
                    "c.id=ggg.id"
                    ")");
        } else {
            UNIT_ASSERT_EQUAL(
                qb.query(),
                "WITH ggg(id) AS (VALUES (100500::bigint)) "
                "SELECT c.id FROM ggg, objects_c c WHERE c.id=ggg.id");
        }
    }
}

Y_UNIT_TEST(test_query_builder_load_contour_object_elements)
{
    const std::string fields = "l.id, l.commit_id";
    const std::string whereClause =
        " r.slave_id = r_next.master_id AND r_next.slave_id = l.id "
        " AND r.domain_attrs->'rel:role'='ad_fc'"
        " AND r_next.domain_attrs->'rel:role'='ad_el'"
        " AND r.master_id = 100500";

    for (auto branchId : {0,1}) {
        views::QueryBuilder qb(branchId);
        qb.selectFields(fields);
        qb.fromTable(views::TABLE_OBJECTS_R, "r", "r_next");
        qb.fromTable(views::TABLE_OBJECTS_L, "l");
        qb.whereClause(whereClause);

        if (branchId) {
            UNIT_ASSERT_EQUAL(
                qb.query(),
                "WITH bm AS (SELECT branch_mask_id FROM vrevisions_stable.branch_mask WHERE branches ? '1') "
                "SELECT " + fields +
                " FROM vrevisions_stable.objects_l l, vrevisions_stable.objects_r r, vrevisions_stable.objects_r r_next"
                " WHERE "
                    "l.branch_mask_id IN (SELECT branch_mask_id FROM bm) AND "
                    "r.branch_mask_id IN (SELECT branch_mask_id FROM bm) AND "
                    "r_next.branch_mask_id IN (SELECT branch_mask_id FROM bm) AND "
                    "(" + whereClause + ")");
        } else {
            UNIT_ASSERT_EQUAL(
                qb.query(),
                "SELECT " + fields +
                " FROM objects_l l, objects_r r, objects_r r_next"
                " WHERE " + whereClause);
        }
    }
}

} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
