#include "helpers.h"

#include <maps/wikimap/mapspro/libs/assessment/include/gateway.h>

namespace maps::wiki::assessment::tests {

std::vector<Console> createConsoles(pqxx::transaction_base& txn, const std::vector<TUid>& uids)
{
    std::vector<Console> consoles;
    for (const TUid uid : uids) {
        consoles.emplace_back(Gateway(txn).console(uid));
    }
    return consoles;
}

TId createUnit(pqxx::transaction_base& txn, const Unit& unit)
{
    return Gateway(txn).getOrCreateUnit(unit.entity, unit.action);
}

} // maps::wiki::assessment::tests
