#include "fixtures.h"

#include <yandex/maps/wiki/common/string_utils.h>

namespace maps::wiki::revision_meta::tests::helpers {

void addCommitRelations(pqxx::transaction_base& txn, const Relations& relations)
{
    txn.exec(
        "INSERT INTO revision_meta.preapproved_commits_relations (commit_id, relates_to) VALUES " +
        common::join(
            relations,
            [] (const auto& rel) {
                return "(" + std::to_string(rel.commitId) + ","+ std::to_string(rel.relatesTo) + ")";
            },
            ","
        )
    );
}

} // namespace maps::wiki::revision_meta::tests::helpers
