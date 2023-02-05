#include "gen_doc/save_edge_test_formatter.h"
#include "gen_doc/move_node_test_formatter.h"
#include "gen_doc/face_validation_test_formatter.h"
#include "gen_doc/snap_nodes_test_formatter.h"

#include "tests/suite.h"

#include <yandex/maps/geotex/document.h>

#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>

#include <iostream>
#include <fstream>
#include <vector>
#include <iterator>
#include <algorithm>

using namespace maps::wiki::topo;
using namespace maps::wiki::topo::doc;
using namespace maps::wiki::topo::test;

template <class TestTypeT, class TestFormatterT>
void format(const TestTypeT& t, maps::geotex::Document& doc)
{
    TestFormatterT formatter(doc);
    formatter.format(t);
}

#define INST_VISIT_SUITE( test_type, test_formatter)\
virtual void visit(const TestSuite< test_type >& suite)\
{\
    maps::geotex::Document doc(paperSize_, pageRect_, preamblePath_);\
    for (const auto& test : suite) {\
        format<test_type, test_formatter>(test, doc);\
    }\
    boost::filesystem::create_directories(path_);\
    doc.print(boost::filesystem::path(path_ + "/" + suite.name() + ".tex").string());\
}\

#define INST_VISIT_TEST( test_type, test_formatter)\
virtual void visit(const std::string& suiteName, const test_type & test)\
{\
    maps::geotex::Document doc(paperSize_, pageRect_, preamblePath_);\
    format<test_type, test_formatter>(test, doc);\
    boost::filesystem::create_directories(path_);\
    doc.print(boost::filesystem::path(path_ + "/" + suiteName + ".tex").string());\
}\

class TestPrinter
    : public Visitor<SaveEdgeTestData, MoveNodeTestData, FaceValidationTestData, SnapNodesTestData>
{
public:
    TestPrinter(
            const std::string& preamblePath,
            const std::string& path,
            const maps::geotex::Size& paperSize,
            const maps::geolib3::BoundingBox& pageRect)
        : preamblePath_(preamblePath)
        , path_(path)
        , paperSize_(paperSize)
        , pageRect_(pageRect)
    {}

    INST_VISIT_SUITE(SaveEdgeTestData, SaveEdgeTestFormatter)
    INST_VISIT_TEST(SaveEdgeTestData, SaveEdgeTestFormatter)

    INST_VISIT_SUITE(MoveNodeTestData, MoveNodeTestFormatter)
    INST_VISIT_TEST(MoveNodeTestData, MoveNodeTestFormatter)

    INST_VISIT_SUITE(FaceValidationTestData, FaceValidationTestFormatter)
    INST_VISIT_TEST(FaceValidationTestData, FaceValidationTestFormatter)

    INST_VISIT_SUITE(SnapNodesTestData, SnapNodesTestFormatter)
    INST_VISIT_TEST(SnapNodesTestData, SnapNodesTestFormatter)

private:
    std::string preamblePath_;
    std::string path_;
    maps::geotex::Size paperSize_;
    maps::geolib3::BoundingBox pageRect_;
};

#undef INST_VISIT_SUITE
#undef INST_VISIT_TEST

using namespace maps;
using namespace maps::wiki::topo;

namespace po = boost::program_options;

const std::string OPT_PREAMBLE_PATH = "preamble-path";
const std::string OPT_OUTPUT_DIR = "output-dir";
const std::string OPT_SUITE_NAME = "suite-name";
const std::string OPT_TEST_NAME = "test-name";

int main(int argc, char** argv)
{
    try {
        po::options_description desc("Options");
        desc.add_options()
            (OPT_PREAMBLE_PATH.c_str(), po::value<std::string>(),
                "Path to style tex preamble file")
            (OPT_OUTPUT_DIR.c_str(), po::value<std::string>(),
                "Output directory for pdf files")
            (OPT_SUITE_NAME.c_str(), po::value<std::string>(),
                "Test suite name (optional)")
            (OPT_TEST_NAME.c_str(), po::value<std::string>(),
                "Test name (optional)");

        po::variables_map vm;
        po::store(po::parse_command_line(argc, argv, desc), vm);
        po::notify(vm);

        REQUIRE(vm.count(OPT_PREAMBLE_PATH), "Preamble file path not set");
        std::string preamblePath = vm[OPT_PREAMBLE_PATH].as<std::string>();

        REQUIRE(vm.count(OPT_OUTPUT_DIR), "Output dir is not set");
        std::string path = vm[OPT_OUTPUT_DIR].as<std::string>();

        std::string suiteName;
        if (vm.count(OPT_SUITE_NAME)) {
            suiteName = vm[OPT_SUITE_NAME].as<std::string>();
        }
        std::string testName;
        if (vm.count(OPT_TEST_NAME)) {
            REQUIRE(!suiteName.empty(), "Test name set but suite name is empty");
            testName = vm[OPT_TEST_NAME].as<std::string>();
        }

        TestPrinter printer(
            preamblePath,
            path,
            maps::geotex::Size{25.0, 40.0},
            maps::geolib3::BoundingBox{{1, 1}, {24, 39}});

        if (suiteName.empty()) {
            mainTestSuite()->visit(printer);
        } else if (testName.empty()) {
            mainTestSuite()->visit(printer, suiteName);
        } else {
            mainTestSuite()->visit(printer, suiteName, testName);
        }

        return 0;
    } catch (const maps::Exception& e) {
        std::cerr << "Error: " << e;
        return 1;
    } catch (const std::exception& e) {
        std::cerr << "Error: " << e.what();
        return 1;
    } catch (...) {
        std::cerr << "Unknown error";
        return 2;
    }
}
