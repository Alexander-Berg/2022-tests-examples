#include <maps/wikimap/ugc/libs/test_helpers/test_dbpools.h>
#include <maps/libs/geolib/include/point.h>

namespace maps::wiki::ugc::account::tests {

const Uid UID1{111111};
const Uid UID2{222222};
const geolib3::Point2 POSITION{37.37, 55.55};

void insertAssignments(const IDbPools& dbPools);

} // namespace maps::wiki::ugc::account::tests
