#include "../include/tools.h"
#include "../include/contexts.hpp"

using namespace maps::renderer5;
using namespace maps::renderer5::labeler;

BOOST_AUTO_TEST_SUITE( OnlineRenderer )
BOOST_AUTO_TEST_SUITE( updateLabels  )

namespace {

template<class ParentContext = EmptyContext>
class SmallTextMapContext: public OnlineRendererContext<ParentContext>
{
public:
    SmallTextMapContext()
    {
        const std::string SMALL_TEXT_MAP_NAME = "tests/boost-tests/maps/SmallTextMap.xml";
        BOOST_REQUIRE_NO_THROW(this->renderer->open(SMALL_TEXT_MAP_NAME));
    }
};

}  // namespace

BOOST_FIXTURE_TEST_CASE( insertOp, SmallTextMapContext<TransactionContext<>> )
{
    const unsigned int zoom = 17;

    LabelingOperation labelOp;
    labelOp.bb1 = core::BoundingBox(4000000.43, 7000000.86, 5000000.31, 8000000.77);
    labelOp.featureId = 993771056;
    labelOp.opType = LabelingOperation::opInsert;
    labelOp.layerId = 2;

    core::BoundingBox bb;
    BOOST_CHECK_NO_THROW(bb = renderer->updateLabels(zoom, labelOp, *trans));

    BOOST_CHECK(bb.width() > 200);
}

BOOST_FIXTURE_TEST_CASE( insertOpBigPriority, SmallTextMapContext<TransactionContext<>> )
{
    const unsigned int zoom = 17;

    LabelingOperation labelOp;
    labelOp.bb1 = core::BoundingBox(4000000.43, 7000000.86, 5000000.31, 8000000.77);
    labelOp.featureId = 993771056;
    labelOp.opType = LabelingOperation::opInsert;
    labelOp.layerId = 3;

    core::BoundingBox bb;
    BOOST_CHECK_NO_THROW(bb = renderer->updateLabels(zoom, labelOp, *trans));

    BOOST_CHECK(bb.width() > 200);
}

namespace {
std::vector<char> toBinary(const std::string& wkbStrHex)
{
    BOOST_REQUIRE((wkbStrHex.size() % 2) == 0);

    std::vector<char> wkbBin(wkbStrHex.size() / 2, 0);
    for (size_t i = 0; i < wkbBin.size(); ++i)
    {
        std::istringstream iss(wkbStrHex.substr(i*2, 2));
        unsigned int val = 0;
        iss >> std::hex >> val;
        wkbBin[i] = static_cast<char>(val);
    }

    return wkbBin;
}
}

BOOST_FIXTURE_TEST_CASE( deleteOp, SmallTextMapContext<TransactionContext<>> )
{
    const unsigned int zoom = 17;

    LabelingOperation labelOp;
    labelOp.bb1 = core::BoundingBox(4000000.43, 7000000.86, 5000000.31, 8000000.77);
    labelOp.featureId = 993771056;
    labelOp.opType = LabelingOperation::opDelete;
    labelOp.layerId = 2;

    core::BoundingBox bb;
    BOOST_CHECK_NO_THROW(bb = renderer->updateLabels(zoom, labelOp, *trans));
}

BOOST_AUTO_TEST_SUITE_END() // updateLabels
BOOST_AUTO_TEST_SUITE_END() // OnlineRenderer
