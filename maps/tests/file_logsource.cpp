#include <iostream>
#include <thread>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/tests_data.h>

#include <maps/libs/common/include/exception.h>
#include <maps/libs/common/include/file_utils.h>
#include <maps/infra/roquefort/lib/logsource.h>

using namespace maps::roquefort;

class FileLogSourceFixture : public NUnitTest::TBaseFixture {
public:
    void SetUp(NUnitTest::TTestContext&) override {
        std::ifstream file("./files/test.log");
        std::getline(file, tskvLine_);
    }

    std::string_view getTskvLine()
    {
        return tskvLine_;
    }

    static std::string openLog(std::ofstream& log, const TString& name)
    {
        auto logFsPath = GetOutputPath() / name;
        auto logPath = logFsPath.GetPath();
        maps::common::removeFile(logPath);
        log.open(logPath);
        UNIT_ASSERT(log.is_open());
        return logPath;
    }

    static std::vector<std::string> collectLines(std::optional<LogDose> dose)
    {
        if (dose == std::nullopt)
            return {};

        std::vector<std::string> lines;
        std::optional<std::string> nextLine;
        while ((nextLine = dose->line()) != std::nullopt) {
            lines.push_back(*nextLine);
        }
        return lines;
    }

private:
    std::string tskvLine_;
};

Y_UNIT_TEST_SUITE_F(FileLogSourceTests, FileLogSourceFixture)
{
    Y_UNIT_TEST(NoLogOnStartup)
    {
        FileLogSource logsource("./does-not-exist.log", /*ignoreLogTruncation = */ false);
    }

    Y_UNIT_TEST(DontParseInitialContents)
    {
        std::ofstream log;
        auto logPath = openLog(log, "DontParseInitialContents.log");
        log << getTskvLine() << std::endl;
        log.flush();
        FileLogSource logsource(logPath, /*ignoreLogTruncation = */ false);

        log << getTskvLine() << std::endl;
        log.flush();

        auto lines = collectLines(logsource.dose());
        UNIT_ASSERT_VALUES_EQUAL(lines.size(), 1);
    }

    Y_UNIT_TEST(InitiallyEmptyThenLoseLine)
    {
        std::ofstream log;
        auto logPath = openLog(log, "InitiallyEmptyThenBlock.log");
        FileLogSource logsource(logPath, /*ignoreLogTruncation = */ false);

        log << getTskvLine() << std::endl;
        log.flush();

        auto lines = collectLines(logsource.dose());
        UNIT_ASSERT_VALUES_EQUAL(lines.size(), 0);
    }

    Y_UNIT_TEST(InitiallyEmptyThenLongWaitThenLoseLine)
    {
        std::ofstream log;
        auto logPath = openLog(log, "InitiallyEmptyThenLongWaitThenBlock.log");
        FileLogSource logsource(logPath, /*ignoreLogTruncation = */ false);

        std::this_thread::sleep_for(std::chrono::seconds(11));
        log << getTskvLine() << std::endl;
        log.flush();

        auto lines = collectLines(logsource.dose());
        UNIT_ASSERT_VALUES_EQUAL(lines.size(), 0);
    }

    Y_UNIT_TEST(OneBatchTwoLines)
    {
        std::ofstream log;
        auto logPath = openLog(log, "OneBatchTwoLines.log");
        log << getTskvLine() << std::endl;
        log.flush();
        FileLogSource logsource(logPath, /*ignoreLogTruncation = */ false);

        log << getTskvLine() << std::endl;
        log << getTskvLine() << std::endl;
        log.flush();

        auto lines = collectLines(logsource.dose());
        UNIT_ASSERT_VALUES_EQUAL(lines.size(), 2);
    }

    Y_UNIT_TEST(TwoBatchesTwoLines)
    {
        std::ofstream log;
        auto logPath = openLog(log, "TwoBatchesTwoLines.log");
        log << getTskvLine() << std::endl;
        log.flush();
        FileLogSource logsource(logPath, /*ignoreLogTruncation = */ false);

        static constexpr size_t batches = 2;
        for (size_t i = 0; i < batches; ++i) {
            log << getTskvLine() << std::endl;
            log << getTskvLine() << std::endl;
            log.flush();

            auto lines = collectLines(logsource.dose());
            UNIT_ASSERT_VALUES_EQUAL(lines.size(), 2);
        }
    }
}
