#include "printers.h"

#include <maps/wikimap/mapspro/libs/acl/include/group.h>
#include <maps/wikimap/mapspro/libs/acl/include/policy.h>

namespace maps::wiki::acl {

std::ostream&
operator<<(
    std::ostream& os,
    const Group& group)
{
    return os << "'" << group.name() << "'";
}

std::ostream&
operator<<(
    std::ostream& os,
    const Policy& policy)
{
    return os
        << "{agent: " << policy.agentId()
        << ", role: " << policy.roleId()
        << ", aoi: " << policy.aoiId() << "}";
}

} // namespace maps::wiki::acl
