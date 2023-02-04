#include <maps/wikimap/ugc/account/src/tests/medium_tests/helpers.h>

#include <maps/wikimap/ugc/account/src/lib/assignments.h>
#include <maps/wikimap/ugc/libs/common/constants.h>
#include <maps/wikimap/ugc/libs/test_helpers/test_dbpools.h>
#include <maps/wikimap/ugc/backoffice/src/lib/assignments/modify.h>
#include <maps/wikimap/ugc/backoffice/src/tests/helpers/test_request_validator.h>
#include <maps/doc/proto/converters/geolib/include/yandex/maps/geolib3/proto.h>

namespace maps::wiki::ugc::account::tests {

namespace backoffice = maps::wiki::ugc::backoffice;

using namespace std::chrono_literals;

void insertAssignments(const IDbPools& dbPools)
{
    static const int FAKETVM = 11111;
    static const backoffice::tests::TestRequestValidator requestValidator;
    {
        proto::backoffice::Task task;
        *task.add_uid() = std::to_string(UID1.value());
        *task.add_uid() = std::to_string(UID2.value());
        *task.mutable_point() = geolib3::proto::encode(POSITION);

        auto& langToMetadata = *task.mutable_lang_to_metadata();
        {
            proto::assignment::AssignmentMetadata metadata;
            auto* addressAdd = metadata.mutable_address_add_assignment();
            *addressAdd->mutable_uri() = "ymapsbm://geo?ll=37.37,55.55&z=17&lang=ru";
            langToMetadata["ru_RU"] = metadata;
        }
        {
            proto::assignment::AssignmentMetadata metadata;
            auto* addressAdd = metadata.mutable_address_add_assignment();
            *addressAdd->mutable_uri() = "ymapsbm://geo?ll=37.37,55.55&z=17&lang=en_US";
            langToMetadata["en_US"] = metadata;
        }
        backoffice::createAssignments(
            dbPools,
            /*dryRun*/ false,
            AssignmentId{"id1"},
            task,
            FAKETVM,
            3600s,
            requestValidator
        );
    }
    {
        proto::backoffice::Task task;
        *task.add_uid() = std::to_string(UID1.value());

        auto& langToMetadata = *task.mutable_lang_to_metadata();
        {
            proto::assignment::AssignmentMetadata metadata;
            auto* edit_status = metadata.mutable_organization_edit_status_assignment();
            *edit_status->mutable_uri() = "ymapsbm://org?oid=1&lang=ru";
            langToMetadata["ru_RU"] = metadata;
        }
        backoffice::createAssignments(
            dbPools,
            /*dryRun*/ false,
            AssignmentId{"id2"},
            task,
            FAKETVM,
            3600s,
            requestValidator
        );
    }
}

} // namespace maps::wiki::ugc::account::tests
