#include <maps/wikimap/mapspro/services/editor/src/configs/config.h>
#include <maps/wikimap/mapspro/services/editor/src/observers/view_syncronizer.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/formatter.h>
#include <maps/wikimap/mapspro/services/editor/src/serialize/save_object_parser.h>
#include <maps/wikimap/mapspro/services/editor/src/utils.h>

#include "controller_tests_common_includes.h"
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/controller_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/db_helpers.h>
#include <maps/wikimap/mapspro/services/editor/src/tests/helpers/helpers.h>

#include <boost/format.hpp>

namespace maps::wiki::tests {

const std::string REQUEST_CREATE_ADDR_FORMAT =
"{"
"   \"categoryId\" : \"addr\","
"   \"uuid\" : \"cfedbb34-4465-4f66-8ff1-49c7e20a4b6e\","
"   \"geometry\" : {"
"       \"coordinates\" : [%3%],"
"       \"type\" : \"Point\""
"   },"
"   \"masters\" : {"
"       \"addr_associated_with\" : {"
"           \"diff\" : {"
"               \"added\" : [{"
"                       \"id\" : \"%1%\","
"                       \"revisionId\" : \"%2%\""
"                   }"
"               ]"
"           }"
"       }"
"   },"
"   \"attrs\" : {"
"       \"addr:disp_class\" : \"5\","
"       \"sys:blocked\" : false,"
"       \"addr_nm\" : [{"
"               \"addr_nm:name_type\" : \"official\","
"               \"addr_nm:lang\" : \"no_lang\","
"               \"addr_nm:is_local\" : false,"
"               \"addr_nm:name\" : \"111222\""
"           }"
"       ]"
"   },"
"   \"editContext\" : {"
"       \"center\" : {"
"           \"coordinates\" : [%3%],"
"           \"type\" : \"Point\""
"       },"
"       \"zoom\" : 20"
"   }"
"}";

Y_UNIT_TEST_SUITE(create_addr_and_set_lang)
{
const auto BRANCH_ID = revision::TRUNK_BRANCH_ID;

WIKI_FIXTURE_TEST_CASE(test_create_addr_and_set_lang, EditorTestFixture)
{
    {
        revision::RevisionID adRevision;

        const auto observers = makeObservers<ViewSyncronizer>();
        performSaveObjectRequest("tests/data/create_test_country.json", observers);
        {
            auto branchCtx = BranchContextFacade::acquireRead(
                BRANCH_ID, "");
            ObjectsCache cache(branchCtx, boost::none);
            auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
                revision::filters::Attr("cat:ad").defined());
            WIKI_TEST_REQUIRE_EQUAL(revs.size(), 1);
            adRevision = *revs.begin();
            auto id  = revs.begin()->objectId();
            auto contry =  cache.getExisting(id);
            WIKI_TEST_REQUIRE(contry);
            auto nameAttr = contry->tableAttributes().find("ad_nm");
            WIKI_TEST_REQUIRE_EQUAL(nameAttr.numRows(), 1);
            UNIT_ASSERT_EQUAL(nameAttr.value(0, "ad_nm:is_local"), TRUE_VALUE);
        }
        executeSqlFile("tests/sql/fill_test_contry_contour_objects_geom.sql");
        {//Try create ADDR with no lang outside AD
            const auto requestBody = str(boost::format(REQUEST_CREATE_ADDR_FORMAT) % adRevision.objectId() %
                adRevision % "37.56436751653286, 55.71477399999377");

            UNIT_CHECK_GENERATED_EXCEPTION(
                performSaveObjectRequestJsonStr(requestBody, observers),
                LogicException
            );
        }
        {//Try create ADDR with no lang inside AD
            const auto requestBody = str(boost::format(REQUEST_CREATE_ADDR_FORMAT) % adRevision.objectId() %
                adRevision % "37.00123944942093, 55.77278869985553");

            UNIT_ASSERT_NO_EXCEPTION(
                performSaveObjectRequestJsonStr(requestBody, observers)
            );
        }
    }
    {//Check successfully created ADDR for language
        auto branchCtx = BranchContextFacade::acquireRead(
            BRANCH_ID, "");
        ObjectsCache cache(branchCtx, boost::none);
        auto revs = cache.revisionsFacade().snapshot().revisionIdsByFilter(
            revision::filters::Attr("cat:addr").defined());
        WIKI_TEST_REQUIRE_EQUAL(revs.size(), 1);
        auto id  = revs.begin()->objectId();
        auto river =  cache.getExisting(id);
        WIKI_TEST_REQUIRE(river);
        auto nameAttr = river->tableAttributes().find("addr_nm");
        WIKI_TEST_REQUIRE_EQUAL(nameAttr.numRows(), 1);
        UNIT_ASSERT_EQUAL(nameAttr.value(0, "addr_nm:is_local"), TRUE_VALUE);
        UNIT_ASSERT_EQUAL(nameAttr.value(0, "addr_nm:lang"), "ru");
    }
}
} // Y_UNIT_TEST_SUITE

} // namespace maps::wiki::tests
