#include <maps/libs/log8/include/log8.h>
#include <maps/libs/common/include/exception.h>
#include <maps/libs/cmdline/include/cmdline.h>

#include <maps/wikimap/mapspro/services/autocart/tools/auto_toloker/test/lib/include/evaluate_classifier.h>

#include <string>

using namespace maps::wiki::autocart;

int main(int argc, char** argv)
try {
    maps::cmdline::Parser parser("Tool for testing quality of AutoToloker classifier");

    auto datasetPath = parser.string("dataset_path")
        .required()
        .help("Path to dataset");

    parser.parse(argc, argv);

    TestResult testResult = evaluateClassifier(datasetPath);

    INFO() << testResult;

    return EXIT_SUCCESS;
}
catch (const maps::Exception& e) {
    INFO() << e;
    return EXIT_FAILURE;
}
catch (const std::exception& e) {
    INFO() << e.what();
    return EXIT_FAILURE;
}
catch (...) {
    INFO() << "Caught unknown exception";
    return EXIT_FAILURE;
}
