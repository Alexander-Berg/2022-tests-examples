#define MODULE_NAME "base_aux_checks"

#include <maps/wikimap/mapspro/libs/validator/common/categories_list.h>

#include <yandex/maps/wiki/validator/check.h>

namespace maps {
namespace wiki {
namespace validator {

using namespace categories;


VALIDATOR_SIMPLE_CHECK( rd_base_check_aux, RD_EL, ADDR, ADDR_NM, COND, COND_DT, AD )
{
    (void)context;
}

VALIDATOR_SIMPLE_CHECK( rd_base_check_aux2, ADDR, RD, RD_EL )
{
    (void)context;
}

VALIDATOR_SIMPLE_CHECK( rd_base_check_aux3, RD_EL, RD_JC, AD_JC )
{
    (void)context;
}

VALIDATOR_SIMPLE_CHECK( polygon_feature_base_check_aux,
    URBAN_AREAL, POI_MEDICINE, TRANSPORT_RAILWAY_STATION,  TRANSPORT_RAILWAY_PLATFORM)
{
    (void)context;
}

VALIDATOR_SIMPLE_CHECK( master_min_occurs_base_check, TRANSPORT_METRO_THREAD )
{
    (void)context;
}


} // namespace validator
} // namespace wiki
} // namespace maps
