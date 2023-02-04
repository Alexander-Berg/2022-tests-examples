#define MODULE_NAME "parallel-check-tests-aux"

#include <yandex/maps/wiki/validator/check.h>
#include <yandex/maps/wiki/validator/categories.h>
#include <yandex/maps/wiki/common/string_utils.h>

#include <boost/none.hpp>

using maps::wiki::validator::categories::RD_EL;

namespace mwc = maps::wiki::common;
namespace mwv = maps::wiki::validator;

VALIDATOR_CHECK_PART( multipart, part1 )
{
    context->error("part1", boost::none, { });
}

VALIDATOR_CHECK_PART( multipart, part2 )
{
    context->error("part2", boost::none, { });
}

VALIDATOR_SIMPLE_CHECK( report_all_batch, RD_EL )
{
    context->objects<RD_EL>().batchVisit([&](const mwv::RoadElement* element) {
        context->warning(
            "stub-description",
            boost::none,
            {element->id()});
    }, 1);
}

std::string getMessageKey(const mwv::Message& /*message*/)
{
    return "test_key";
}

bool compareMessages(const mwv::Message& lhs, const mwv::Message& rhs)
{
    return lhs.attributes().description > rhs.attributes().description;
}

VALIDATOR_CHECK_PART( deduplication_multipart, part1 )
{
    context->error("check1_part1", boost::none, {}, getMessageKey, compareMessages);
}

VALIDATOR_CHECK_PART( deduplication_multipart, part2 )
{
    context->error("check1_part2", boost::none, {}, getMessageKey, compareMessages);
}

VALIDATOR_SIMPLE_CHECK( deduplication_simple )
{
    context->error("check2", boost::none, {}, getMessageKey, compareMessages);
}
