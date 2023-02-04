#include <maps/libs/cmdline/include/cmdline.h>
#include <maps/analyzer/libs/common/include/yson.h>
#include <maps/analyzer/libs/mt_jobs/include/job.h>
#include <maps/analyzer/libs/mt_jobs/include/mt_reducer.h>


using namespace maps::analyzer;
using namespace maps::analyzer::mt_jobs;

class GreatestEven: public Job {
public:
    explicit GreatestEven(bool skip = false): skip_(skip)
    {}

    void Start(TWriter* writer) override {
        Row outRow;
        outRow["info"] = "reducer started";
        writer->AddRow(outRow);
    }

    void Finish(TWriter* writer) override {
        Row outRow;
        outRow["info"] = "reducer finished";
        writer->AddRow(outRow);
    }
    
    void Do(TReader* reader, TWriter* writer) override
    {
        if (!skip_) {
           Row outRow;
            outRow["number"] = -1;
            for (auto& cursor: *reader) {
                auto row = cursor.GetRow();

                const auto num = row["number"].AsInt64();

                if (num % 2 == 0 && num > outRow["number"].AsInt64()) {
                    outRow = row;
                }   
            }
            if (outRow["number"].AsInt64() != -1) {
                writer->AddRow(outRow);
            }    
        }
    }
private:
    bool skip_{false};
};

int main(int argc, char* argv[]) {
    maps::cmdline::Parser p;
    auto inputsCount = p.size_t("inputs-count").help("number of input tables, 1 by default").defaultValue(1);
    auto threads = p.size_t("threads", 't').help("number of threads used by mt_mapper").defaultValue(1);
    auto skip = p.flag("skip").help("skip 'Do' method");

    p.parse(argc, argv);

    maps::log8::setLevel(maps::log8::Level::DEBUG);

    JobRunner runner{
        inputsCount,
        NYson::EYsonFormat::Text
    };
    if (!runner) {
        return 0;
    }

    MTReducer<GreatestEven> mtReducer(
        [&]() { return std::make_unique<GreatestEven>(skip); },
        {.threads = threads}
    );
    runner.run(mtReducer);
}
