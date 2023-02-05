#include "../include/execute_operation.h"

#include <mapreduce/yt/interface/client.h>
#include <library/cpp/testing/unittest/registar.h>

namespace maps::wiki::yt_stubs::test {

namespace {

class Mapper : public
    NYT::IMapper<NYT::TTableReader<NYT::TNode>,
    NYT::TTableWriter<NYT::TNode>>
{
public:
    void Do(TReader* reader, TWriter* writer) override
    {
        for (; reader->IsValid(); reader->Next()) {
            writer->AddRow(reader->GetRow());
        }
    }
};

} // unnamed namespace

Y_UNIT_TEST_SUITE(suite) {

Y_UNIT_TEST(test)
{
    std::vector<NYT::TNode> rows {NYT::TNode("privet")};

    Mapper mapper;
    auto resRows = executeOperation(mapper, rows);

    UNIT_ASSERT(resRows.size() == 1);
    UNIT_ASSERT(rows.front() == resRows.front());
}

}

} // namespace maps::wiki::yt_stubs::test
