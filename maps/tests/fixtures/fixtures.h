#pragma once

#include <maps/wikimap/mapspro/libs/revision_meta/include/common.h>
#include <maps/wikimap/mapspro/libs/revision_meta/impl/preapproved_commits_relations.h>

#include <pqxx/pqxx>

namespace maps::wiki::revision_meta::tests::helpers {

void addCommitRelations(pqxx::transaction_base& txn, const Relations& relations);

} // namespace maps::wiki::revision_meta::tests::helpers
