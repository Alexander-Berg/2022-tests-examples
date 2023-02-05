#include "run_all_tests.h"

#include "border_test.h"
#include "building_test.h"
#include "hydro_test.h"
#include "mesh_test.h"
#include "poi_test.h"
#include "primitive_count_test.h"
#include "test_runner.h"
#include "url_tile_loader.h"

#include <library/cpp/getopt/small/opt.h>
#include <maps/libs/config/include/config.h>
#include <maps/libs/log8/include/log8.h>
#include <util/string/split.h>
#include <maps/libs/common/include/exception.h>
#include <maps/renderer/libs/base/include/json_fwd.h>
#include <maps/renderer/libs/base/include/string_util.h>

#include <fstream>

namespace maps::renderer::check {

namespace {

const size_t MAX_BAD_TILES_REPORTED = 10;

class WriteBadTiles
{
public:
    WriteBadTiles(const std::vector<TileTestResult>& badTiles)
        : badTiles_(badTiles)
    {}

    void operator()(rjhelper::ArrayBuilder ab) const
    {
        for (size_t i = 0, endI = std::min(badTiles_.size(), MAX_BAD_TILES_REPORTED); i != endI; ++i) {
            ab.PutObject([&](rjhelper::ObjectBuilder tileBuilder) {
                tileBuilder.Put("x", badTiles_[i].id.x());
                tileBuilder.Put("y", badTiles_[i].id.y());
                tileBuilder.Put("z", badTiles_[i].id.z());
                tileBuilder.Put("error", badTiles_[i].error.val);
                if (!badTiles_[i].error.problemDescr.empty())
                    tileBuilder.Put("descr", badTiles_[i].error.problemDescr);
            });
        }

    }

private:
    const std::vector<TileTestResult>& badTiles_;
};

struct TestParams
{
    std::string testSetName() const
    {
        auto valueField = testSetConfig.FindMember("name");
        if (valueField == testSetConfig.MemberEnd())
            return "Unknown test set";

        return valueField->value.GetString();
    }

    std::unique_ptr<TestCreator> testCreator;
    json::Document testSetConfig;
};

json::Document runTests(const std::vector<TestParams>& tests, const Options& options)
{
    json::Document report;
    json::ObjectBuilder ob(&report, &report.GetAllocator());

    INFO() << "Starting " << options.numThreads << " threads";
    TestRunner testRunner(options.numThreads);

    ob.PutArray("results", [&](rjhelper::ArrayBuilder resultsBuilder) {
        for (const auto& test : tests) {
            auto testSetName = test.testSetName();
            INFO() << "Running test set: " << testSetName;

            auto reports = testRunner.run(*test.testCreator, test.testSetConfig,
                                          *options.testTileLoader, *options.goldenTileLoader);
            resultsBuilder.PutObject([&](rjhelper::ObjectBuilder testBuilder) {
                testBuilder.Put("name", testSetName);
                testBuilder.PutArray("tests", [&](rjhelper::ArrayBuilder ab) {
                    for (const auto& testReport : reports) {
                        ab.PutObject([&](rjhelper::ObjectBuilder subtestBuilder) {
                            subtestBuilder.Put("name", testReport.testName);
                            subtestBuilder.Put("zoom", testReport.zoom);
                            subtestBuilder.Put("status", statusToStr(testReport.status));
                            if (!testReport.report.problemDescr.empty())
                                subtestBuilder.Put("description", testReport.report.problemDescr);
                            if (!testReport.report.maxErrorTiles.empty())
                                subtestBuilder.PutArray("tiles", WriteBadTiles(testReport.report.maxErrorTiles));
                            if (!testReport.failedToLoadTiles.empty())
                                subtestBuilder.PutArray("failed_to_load", WriteBadTiles(testReport.failedToLoadTiles));
                        });
                    }
                });
            });
        }
    });

    return report;
}

bool isUrl(const std::string& src)
{
    const auto& srcView = std::string_view(src);
    return srcView.starts_with("http://") || srcView.starts_with("https://");
}

json::Document loadConfig(const std::string& configPath)
{
    std::string str = config::readConfigFile(configPath);
    json::Document config;
    config.Parse(str.c_str());
    return config;
}

} // namespace

Options::Options()
    : numThreads(1)
{
}

Options::Options(const std::string& goldenTilesSrc,
                 const std::string& testTilesSrc,
                 size_t numThreads,
                 const std::string& reportFile,
                 const std::vector<std::string>& testList)
    : numThreads(numThreads)
    , reportFile(reportFile)
    , testList(testList)
{
    REQUIRE(isUrl(goldenTilesSrc) && isUrl(testTilesSrc), "Maps list has not been implemented yet");

    goldenTileLoader = std::make_unique<UrlTileLoader>(goldenTilesSrc);
    testTileLoader = std::make_unique<UrlTileLoader>(testTilesSrc);
}

Options parseCmd(int argc, char* argv[])
{
    using namespace NLastGetopt;
    TOpts opts;
    opts.AddHelpOption('h');

    std::string goldenTilesSrc;
    opts.AddLongOption('g', "golden",
            "Source for golden tiles")
        .DefaultValue("https://core-renderer-tilesgen.maps.yandex.net/vmap2")
        .StoreResult(&goldenTilesSrc);

    std::string testTilesSrc;
    opts.AddLongOption('t', "test",
            "Source for test tiles")
        .Required()
        .StoreResult(&testTilesSrc);

    size_t numThreads;
    opts.AddLongOption('n', "threads",
            "Number of threads for tests running")
        .DefaultValue(std::thread::hardware_concurrency() * 3)
        .StoreResult(&numThreads);

    std::string reportFile;
    opts.AddLongOption('r', "report",
            "file name to store report")
        .DefaultValue("")
        .StoreResult(&reportFile);

    std::string testListStr;
    opts.AddLongOption('l', "test-list",
            "Comma separated list of test. Empty list triggers all tests")
        .DefaultValue("")
        .StoreResult(&testListStr);

    TOptsParseResult res(&opts, argc, argv);

    std::vector<std::string> testList;
    if (!testListStr.empty())
        testList = StringSplitter(testListStr).Split(',');
    return Options(goldenTilesSrc, testTilesSrc, numThreads, reportFile, testList);
}

class TestParamsFactory
{
public:
    template <class TestT>
    void addTest(const std::string& configName)
    {
        auto f = [configName]() {
            TestParams res = {std::make_unique<TestT>(), loadConfig(configName + ".json")};
            return res;
        };
        knownTests_.insert({configName, f});
        testNames_.push_back(configName);
    }

    std::vector<TestParams> createTests(const std::vector<std::string>& testList) const
    {
        std::vector<TestParams> res;
        for (const auto& testName : testList) {
            auto foundIt = knownTests_.find(testName);
            if (foundIt == knownTests_.end()) {
                WARN() << "Can't find test " << testName;
            } else {
                res.push_back(foundIt->second());
            }
        }

        return res;
    }

    const std::vector<std::string>& allTests() const
    {
        return testNames_;
    }

private:
    std::map<std::string, std::function<TestParams()>> knownTests_;
    std::vector<std::string> testNames_; // Preserves tests order
};

void runAllTests(const Options& options)
{
    TestParamsFactory testFactory;
    testFactory.addTest<HydroTestCreator>("hydro");
    testFactory.addTest<PoiTestCreator>("poi");
    testFactory.addTest<ClickablePoiTestCreator>("clickable_poi");
    testFactory.addTest<BorderTestCreator>("border_dispute");
    testFactory.addTest<MeshTestCreator>("mesh");
    testFactory.addTest<ClickableBuildingTestCreator>("clickable_building");
    testFactory.addTest<PrimitiveCountTestCreator>("primitive_count");

    std::vector<TestParams> tests = testFactory.createTests(
        !options.testList.empty() ? options.testList : testFactory.allTests());
    auto report = runTests(tests, options);
    if (!options.reportFile.empty()) {
        std::ofstream fOut(options.reportFile);
        fOut << rjhelper::ToStringPretty(report);
    }
}

} // namespace maps::renderer::check
