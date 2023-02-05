#include "magic_strings.h"
#include "mapper.h"

#include <library/cpp/testing/unittest/gtest.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/json/include/value.h>
#include <maps/wikimap/mapspro/libs/yt_stubs/include/execute_operation.h>

#include <boost/optional.hpp>
#include <memory>
#include <vector>

namespace mwn = maps::wiki::navi_feedback;
namespace mwy = maps::wiki::yt_stubs;

std::string buildEventValue(
    boost::optional<int> submittedLimit,
    boost::optional<double> latitude,
    boost::optional<double> longitude)
{
    maps::json::Builder builder;
    builder << [&](maps::json::ObjectBuilder object) {
        if (submittedLimit) {
            object[mwn::SUBMIT_LIM] << *submittedLimit;
        }
        if (latitude) {
            object[mwn::LATITUDE] << *latitude;
        }
        if (longitude) {
            object[mwn::LONGITUDE] << *longitude;
        }
    };
    return builder;
}

std::vector<NYT::TNode> inputToOutput(const std::vector<NYT::TNode> &inputRows)
{
    mwn::NaviSpeedLimitMapper mapper;
    return mwy::executeOperation(mapper, inputRows);
}

TEST(mapper, inputRowStructure)
{
    {
        // Event Name column is Null
        //
        NYT::TNode inputRow;
        inputRow(TString(mwn::EVENT_NAME), NYT::TNode::CreateEntity());
        ASSERT_TRUE(inputToOutput({inputRow}).empty());
    }
    {
        // Event Name column is not Event Feedback
        //
        NYT::TNode inputRow;
        inputRow(TString(mwn::EVENT_NAME), NYT::TNode("Not A Feedback"));
        ASSERT_TRUE(inputToOutput({inputRow}).empty());
    }
    {
        // Event Val column is Null
        //
        NYT::TNode inputRow;
        inputRow(TString(mwn::EVENT_NAME), NYT::TNode("Not A Feedback"));
        inputRow(TString(mwn::EVENT_VAL), NYT::TNode::CreateEntity());
        ASSERT_TRUE(inputToOutput({inputRow}).empty());
    }
    {
        // We cannot parse EVENT_VAL json
        //
        NYT::TNode inputRow;
        inputRow(TString(mwn::EVENT_NAME), NYT::TNode(mwn::EVENT_FEEDBACK));
        inputRow(TString(mwn::EVENT_VAL), NYT::TNode("not a json object"));
        ASSERT_THROW(inputToOutput({inputRow}), maps::Exception);
    }
    {
        NYT::TNode inputRow;
        inputRow(TString(mwn::EVENT_NAME), NYT::TNode(mwn::EVENT_FEEDBACK));
        inputRow(TString(mwn::EVENT_VAL), NYT::TNode(buildEventValue(1, 1., 1.)));
        ASSERT_EQ(inputToOutput({inputRow}).size(), 1);
    }
    {
        // longitude is absent
        NYT::TNode inputRow;
        inputRow(TString(mwn::EVENT_NAME), NYT::TNode(mwn::EVENT_FEEDBACK));
        inputRow(TString(mwn::EVENT_VAL), NYT::TNode(buildEventValue(1, 1., boost::none)));
        ASSERT_TRUE(inputToOutput({inputRow}).empty());
    }
    {
        // latitude is absent
        NYT::TNode inputRow;
        inputRow(TString(mwn::EVENT_NAME), NYT::TNode(mwn::EVENT_FEEDBACK));
        inputRow(TString(mwn::EVENT_VAL), NYT::TNode(buildEventValue(1, boost::none, 1.)));
        ASSERT_TRUE(inputToOutput({inputRow}).empty());
    }
    {
        // submitted_limit is absent
        NYT::TNode inputRow;
        inputRow(TString(mwn::EVENT_NAME), NYT::TNode(mwn::EVENT_FEEDBACK));
        inputRow(TString(mwn::EVENT_VAL), NYT::TNode(buildEventValue(boost::none, 1., 1.)));
        ASSERT_TRUE(inputToOutput({inputRow}).empty());
    }
}

TEST(mapper, inputToOutputConversion)
{
    {
        NYT::TNode inputRow;
        inputRow(TString(mwn::EVENT_NAME), NYT::TNode(mwn::EVENT_FEEDBACK));
        inputRow(TString(mwn::EVENT_VAL), NYT::TNode(buildEventValue(1, 3., 4.)));

        auto outputRows = inputToOutput({inputRow});
        ASSERT_EQ(outputRows.size(), 1);

        const NYT::TNode& outputRow = outputRows.front();
        ASSERT_TRUE(outputRow.IsMap());

        ASSERT_TRUE(outputRow[mwn::SUBMIT_LIM].IsInt64());
        ASSERT_EQ(outputRow[mwn::SUBMIT_LIM].AsInt64(), 1);

        ASSERT_TRUE(outputRow[mwn::LATITUDE].IsDouble());
        ASSERT_DOUBLE_EQ(outputRow[mwn::LATITUDE].AsDouble(), 3.);

        ASSERT_TRUE(outputRow[mwn::LONGITUDE].IsDouble());
        ASSERT_DOUBLE_EQ(outputRow[mwn::LONGITUDE].AsDouble(), 4.);
    }
    {
        NYT::TNode inputRow1;
        inputRow1(TString(mwn::EVENT_NAME), NYT::TNode("VotEtoEvent"));

        NYT::TNode inputRow2;
        inputRow2(TString(mwn::EVENT_NAME), NYT::TNode(mwn::EVENT_FEEDBACK));
        inputRow2(TString(mwn::EVENT_VAL), NYT::TNode(buildEventValue(60, 43.32355, 78.3333)));

        auto outputRows = inputToOutput({inputRow1, inputRow2});
        ASSERT_EQ(outputRows.size(), 1);

        const NYT::TNode& outputRow = outputRows.front();
        ASSERT_EQ(outputRow[mwn::SUBMIT_LIM].AsInt64(), 60);
        ASSERT_DOUBLE_EQ(outputRow[mwn::LATITUDE].AsDouble(), 43.32355);
        ASSERT_DOUBLE_EQ(outputRow[mwn::LONGITUDE].AsDouble(), 78.3333);
    }
}
