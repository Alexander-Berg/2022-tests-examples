#define MODULE_NAME "tests-aux"

#include <yandex/maps/wiki/validator/check.h>
#include <yandex/maps/wiki/validator/categories.h>

#include <boost/none.hpp>

namespace mwv = maps::wiki::validator;

using maps::wiki::validator::categories::RD_EL;
using maps::wiki::validator::categories::RD;
using maps::wiki::validator::categories::RD_NM;
using maps::wiki::validator::categories::AD;
using maps::wiki::validator::categories::AD_NM;
using maps::wiki::validator::categories::ADDR;
using maps::wiki::validator::categories::ADDR_NM;


VALIDATOR_SIMPLE_CHECK( do_nothing, RD_EL )
{
    (void)context;
}

VALIDATOR_SIMPLE_CHECK( report_all, RD_EL )
{
    context->objects<RD_EL>().visit([&](const mwv::RoadElement* element) {
        context->warning(
            "stub-description",
            element->geom().segmentAt(0).midpoint(),
            {element->id()});
    });
}

VALIDATOR_SIMPLE_CHECK( report_all_without_geom, RD_EL )
{
    context->objects<RD_EL>().visit([&](const mwv::RoadElement* element) {
        context->warning(
            "stub-description",
            boost::none,
            {element->id()});
    });
}

VALIDATOR_SIMPLE_CHECK( long )
{
    (void)context;
    sleep(1);
}

VALIDATOR_SIMPLE_CHECK( throwing )
{
    (void)context;
    throw std::runtime_error("something bad happened");
}

VALIDATOR_SIMPLE_CHECK( without_oids, RD_EL )
{
    context->objects<RD_EL>().visit([&](const mwv::RoadElement*) {
        context->warning("without-oids", boost::none, {});
    });
}

VALIDATOR_SIMPLE_CHECK( report_relations, RD_EL )
{
    context->objects<RD_EL>().visit([&](const mwv::RoadElement* element) {
        context->warning("rd-el", boost::none, {element->id()});
        context->warning("rd-el-relation", boost::none, {element->startJunction()});
        context->warning("rd-el-relation", boost::none, {element->endJunction()});
    });
}

VALIDATOR_SIMPLE_CHECK( report_nonexistent_id, RD_EL )
{
    context->warning("rd-el",boost::none, { 12345 });
}
