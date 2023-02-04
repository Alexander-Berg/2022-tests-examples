#include <maps/wikimap/mapspro/services/editor/src/srv_attrs/registry.h>

#include <maps/wikimap/mapspro/services/editor/src/observers/view_syncronizer.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/observers.h>

#include <yandex/maps/wiki/revision/revisionid.h>

namespace maps::wiki::tests {

const auto BRANCH_ID = revision::TRUNK_BRANCH_ID;


/// @return An object with a pre-defined category from the DB by its
/// index.
///
/// @param category Name of the category, the object of which type
///   must be returned.
/// @param index Index of the object to be returned.
ObjectPtr getObjectOfCategory(std::string &&category, size_t index = 0) {
    ObjectsCache cache(BranchContextFacade::acquireRead(BRANCH_ID, ""),
                       boost::none);

    auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
        revision::filters::Attr(category).defined());
    WIKI_TEST_REQUIRE_MESSAGE(revs.size() > index,
                          "Index (" << index << ") is too big, "
                          "there are only " << revs.size() << " objects "
                          "of category " << category << ".");

    auto object = cache.getExisting(revs[index].objectId());
    WIKI_TEST_REQUIRE(object);

    return object;
}

Y_UNIT_TEST_SUITE(srv_attrs)
{
WIKI_FIXTURE_TEST_CASE(should_calculate_ad_screen_label, EditorTestFixture) {
    performSaveObjectRequest("tests/data/create_test_city.json", makeObservers<ViewSyncronizer>());

    auto ad = getObjectOfCategory("cat:ad");
    UNIT_ASSERT_EQUAL(ad->screenLabel(), "Test city");
}


WIKI_FIXTURE_TEST_CASE(should_calculate_railway_platform_screen_label, EditorTestFixture) {
    performSaveObjectRequest("tests/data/create_test_platform.json", makeObservers<ViewSyncronizer>());

    auto platform = getObjectOfCategory("cat:transport_railway_platform");
    UNIT_ASSERT_EQUAL(platform->screenLabel(), "Test platform (official local)");
    UNIT_ASSERT_EQUAL(platform->renderLabel(), "Test platform (official local)");//local official name wins against non local render name
}

WIKI_FIXTURE_TEST_CASE(should_calculate_railway_platform_render_label_local, EditorTestFixture) {
    performSaveObjectRequest(
        "tests/data/create_test_platform_local_render.json",
        makeObservers<ViewSyncronizer>());

    auto platform = getObjectOfCategory("cat:transport_railway_platform");
    UNIT_ASSERT_EQUAL(platform->renderLabel(), "Test platform (render local)");//local render name wins against local official name
}


WIKI_FIXTURE_TEST_CASE(should_calculate_screen_label_for_addr_without_zipcode, EditorTestFixture) {
    const auto observers = makeObservers<ViewSyncronizer>();
    performSaveObjectRequest("tests/data/create_rd_el.xml", observers);
    performSaveObjectRequest("tests/data/create_rd.xml", observers);
    performSaveObjectRequest("tests/data/create_addr_wo_assoc_to_zipcode.xml", observers);

    auto addr = getObjectOfCategory("cat:addr");
    UNIT_ASSERT_EQUAL(addr->serviceAttrValue(srv_attrs::SRV_SCREEN_LABEL),
                      "Test Road, 24");
}


WIKI_FIXTURE_TEST_CASE(should_calculate_screen_label_for_addr_with_zipcode, EditorTestFixture) {
    const auto observers = makeObservers<ViewSyncronizer>();
    performSaveObjectRequest("tests/data/create_rd_el.xml", observers);
    performSaveObjectRequest("tests/data/create_rd.xml", observers);
    performSaveObjectRequest("tests/data/create_zipcode.xml", observers);
    performSaveObjectRequest("tests/data/create_addr_with_assoc_to_zipcode.xml", observers);

    auto addr = getObjectOfCategory("cat:addr");
    UNIT_ASSERT_EQUAL(addr->serviceAttrValue(srv_attrs::SRV_SCREEN_LABEL),
                      "Test Road, 24");
}


WIKI_FIXTURE_TEST_CASE(should_show_no_connections_between_parkings_and_zones, EditorTestFixture) {
    performSaveObjectRequest("tests/data/create_parking_lot_wo_connection_to_zone.xml");
    performSaveObjectRequest("tests/data/create_parking_lot_linear_wo_connection_to_zone.xml");

    auto parking_lot = getObjectOfCategory("cat:urban_roadnet_parking_lot");
    UNIT_ASSERT_EQUAL(parking_lot->serviceAttrValue(srv_attrs::SRV_IS_ASSIGNED_TO_ZONE),
                      srv_attrs::SRV_ATTR_FALSE);

    auto parking_lot_linear = getObjectOfCategory("cat:urban_roadnet_parking_lot_linear");
    UNIT_ASSERT_EQUAL(parking_lot_linear->serviceAttrValue(srv_attrs::SRV_IS_ASSIGNED_TO_ZONE),
                      srv_attrs::SRV_ATTR_FALSE);
}


WIKI_FIXTURE_TEST_CASE(should_show_connections_between_parkings_and_zones, EditorTestFixture) {
    const auto observers = makeObservers<ViewSyncronizer>();
    performSaveObjectRequest("tests/data/create_parking_controlled_zone.xml", observers);
    performSaveObjectRequest("tests/data/create_parking_lot_with_connection_to_zone.xml", observers);
    performSaveObjectRequest("tests/data/create_parking_lot_linear_with_connection_to_zone.xml", observers);

    auto parking_lot = getObjectOfCategory("cat:urban_roadnet_parking_lot");
    UNIT_ASSERT_EQUAL(parking_lot->serviceAttrValue(srv_attrs::SRV_IS_ASSIGNED_TO_ZONE),
                      srv_attrs::SRV_ATTR_TRUE);

    auto parking_lot_linear = getObjectOfCategory("cat:urban_roadnet_parking_lot_linear");
    UNIT_ASSERT_EQUAL(parking_lot_linear->serviceAttrValue(srv_attrs::SRV_IS_ASSIGNED_TO_ZONE),
                      srv_attrs::SRV_ATTR_TRUE);
}


WIKI_FIXTURE_TEST_CASE(should_not_calc_hotspot_for_unassigned_parkings, EditorTestFixture) {
    performSaveObjectRequest("tests/data/create_parking_lot_wo_connection_to_zone.xml");
    performSaveObjectRequest("tests/data/create_named_parking_lot_wo_connection_to_zone.xml");
    performSaveObjectRequest("tests/data/create_parking_lot_linear_wo_connection_to_zone.xml");

    auto parking_lot = getObjectOfCategory("cat:urban_roadnet_parking_lot", 0);
    UNIT_ASSERT_EQUAL(parking_lot->serviceAttrValue(srv_attrs::SRV_HOTSPOT_LABEL),
                      "");

    auto named_parking_lot = getObjectOfCategory("cat:urban_roadnet_parking_lot", 1);
    UNIT_ASSERT_EQUAL(named_parking_lot->serviceAttrValue(srv_attrs::SRV_HOTSPOT_LABEL),
                      "");

    auto parking_lot_linear = getObjectOfCategory("cat:urban_roadnet_parking_lot_linear");
    UNIT_ASSERT_EQUAL(parking_lot_linear->serviceAttrValue(srv_attrs::SRV_HOTSPOT_LABEL),
                      "");
}


WIKI_FIXTURE_TEST_CASE(should_calc_hotspot_for_assigned_parkings, EditorTestFixture) {
    const auto observers = makeObservers<ViewSyncronizer>();
    performSaveObjectRequest("tests/data/create_parking_controlled_zone.xml", observers);
    performSaveObjectRequest("tests/data/create_parking_lot_with_connection_to_zone.xml", observers);
    performSaveObjectRequest("tests/data/create_named_parking_lot_with_connection_to_zone.xml", observers);
    performSaveObjectRequest("tests/data/create_parking_lot_linear_with_connection_to_zone.xml", observers);

    auto parking_lot = getObjectOfCategory("cat:urban_roadnet_parking_lot", 0);
    UNIT_ASSERT_EQUAL(parking_lot->serviceAttrValue(srv_attrs::SRV_HOTSPOT_LABEL),
                      "Test parking zone ({{categories:urban_roadnet_parking_controlled_zone}})\n"
                      "{{categories:urban_roadnet_parking_lot}}");

    auto named_parking_lot = getObjectOfCategory("cat:urban_roadnet_parking_lot", 1);
    UNIT_ASSERT_EQUAL(named_parking_lot->serviceAttrValue(srv_attrs::SRV_HOTSPOT_LABEL),
                      "Test parking zone ({{categories:urban_roadnet_parking_controlled_zone}})\n"
                      "Test point parking");

    auto parking_lot_linear = getObjectOfCategory("cat:urban_roadnet_parking_lot_linear");
    UNIT_ASSERT_EQUAL(parking_lot_linear->serviceAttrValue(srv_attrs::SRV_HOTSPOT_LABEL),
                      "Test parking zone ({{categories:urban_roadnet_parking_controlled_zone}})\n"
                      "{{attr-values:urban_roadnet_parking_lot_linear-ft_type_id__2002}}");
}


WIKI_FIXTURE_TEST_CASE(addr_should_not_have_zipcode, EditorTestFixture) {
    performSaveObjectRequest("tests/data/create_rd_el.xml");
    performSaveObjectRequest("tests/data/create_rd.xml");
    performSaveObjectRequest("tests/data/create_addr_wo_assoc_to_zipcode.xml");

    auto addr = getObjectOfCategory("cat:addr");
    UNIT_ASSERT_EQUAL(addr->serviceAttrValue(srv_attrs::SRV_HAS_ZIPCODE), srv_attrs::SRV_ATTR_FALSE);
}


WIKI_FIXTURE_TEST_CASE(addr_should_have_zipcode, EditorTestFixture) {
    const auto observers = makeObservers<ViewSyncronizer>();
    performSaveObjectRequest("tests/data/create_rd_el.xml", observers);
    performSaveObjectRequest("tests/data/create_rd.xml", observers);
    performSaveObjectRequest("tests/data/create_zipcode.xml", observers);
    performSaveObjectRequest("tests/data/create_addr_with_assoc_to_zipcode.xml", observers);

    auto addr = getObjectOfCategory("cat:addr");
    UNIT_ASSERT_EQUAL(addr->serviceAttrValue(srv_attrs::SRV_HAS_ZIPCODE), srv_attrs::SRV_ATTR_TRUE);
}

WIKI_FIXTURE_TEST_CASE(addr_related_to_road_with_geometry, EditorTestFixture) {
    performSaveObjectRequest("tests/data/create_rd_el.xml");
    performSaveObjectRequest("tests/data/create_rd.xml");
    performSaveObjectRequest("tests/data/create_addr_wo_assoc_to_zipcode.xml");

    auto addr = getObjectOfCategory("cat:addr");
    UNIT_ASSERT_EQUAL(addr->serviceAttrValue(
        srv_attrs::ADDR_IS_RELATED_TO_ROAD_WITHOUT_GEOMETRY),
        srv_attrs::SRV_ATTR_FALSE);
}

WIKI_FIXTURE_TEST_CASE(addr_related_to_road_wo_geometry, EditorTestFixture) {
    performObjectsImport("tests/data/rd_wo_geometry_addr.json", db.connectionString());

    auto addr = getObjectOfCategory("cat:addr");
    UNIT_ASSERT_EQUAL(addr->serviceAttrValue(
        srv_attrs::ADDR_IS_RELATED_TO_ROAD_WITHOUT_GEOMETRY),
        srv_attrs::SRV_ATTR_TRUE);

    performSaveObjectRequest("tests/data/add_rd_el_to_rd.xml", makeObservers<ViewSyncronizer>());

    addr = getObjectOfCategory("cat:addr");
    UNIT_ASSERT_EQUAL(addr->serviceAttrValue(
        srv_attrs::ADDR_IS_RELATED_TO_ROAD_WITHOUT_GEOMETRY),
        srv_attrs::SRV_ATTR_FALSE);
}
}//Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
