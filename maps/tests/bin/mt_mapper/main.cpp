#include <maps/libs/cmdline/include/cmdline.h>
#include <maps/analyzer/libs/common/include/yson.h>
#include <maps/analyzer/libs/mt_jobs/include/job.h>
#include <maps/analyzer/libs/mt_jobs/include/mt_mapper.h>


using namespace maps::analyzer;
using namespace maps::analyzer::mt_jobs;

class SquareMapper: public Job {
public:
    void Start(TWriter* writer) override {
        Row outRow;
        outRow["info"] = "mapper started";
        writer->AddRow(outRow);
    }

    void Finish(TWriter* writer) override {
        Row outRow;
        outRow["info"] = "mapper finished";
        writer->AddRow(outRow);
    }
    
    void Do(TReader* reader, TWriter* writer) override
    {
        for (auto& cursor: *reader) {
            auto row = cursor.MoveRow();
            auto tableIndex = cursor.GetTableIndex();

            Row outRow;
            const auto num = row["number"].AsInt64();
            outRow["number"] = num;
            outRow["squared"] = num * num;
            outRow["table_index"] = tableIndex;

            writer->AddRow(outRow, tableIndex);
        }
    }
};

int main(int argc, char* argv[]) {
    maps::cmdline::Parser p;
    auto inputsCount = p.size_t("inputs-count").help("number of input tables, 1 by default").defaultValue(1);
    auto threads = p.size_t("threads", 't').help("number of threads used by mt_mapper").defaultValue(1);

    p.parse(argc, argv);

    maps::log8::setLevel(maps::log8::Level::DEBUG);

    JobRunner runner{
        inputsCount,
        NYson::EYsonFormat::Text
    };
    if (!runner) {
        return 0;
    }

    MTMapper<SquareMapper> mtMapper(
        [&]() { return std::make_unique<SquareMapper>(); },
        {.threads = threads}
    );
    runner.run(mtMapper);
}
