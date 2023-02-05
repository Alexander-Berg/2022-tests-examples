#include <maps/wikimap/mapspro/services/autocart/tools/auto_toloker/test/lib/include/evaluate_classifier.h>

#include <maps/libs/common/include/file_utils.h>
#include <maps/libs/common/include/exception.h>

#include <maps/wikimap/mapspro/services/autocart/libs/auto_toloker/include/auto_toloker.h>

#include <boost/algorithm/string.hpp>

#include <string>
#include <fstream>
#include <iostream>


namespace maps::wiki::autocart {

namespace {

static const std::string YES_ANSWER = "yes";
static const std::string NO_ANSWER = "no";

class Dataset {
public:
    struct Item {
        cv::Mat image;
        cv::Mat mask;
        std::string answer;
    };

    Dataset(const std::string& path)
        : path_(path),
          ifs_(common::joinPath(path, LIST_NAME))
    {
        REQUIRE(ifs_.is_open(), "Failed to open dataset");
        next();
    }

    bool isValid() const {
        return !ifs_.eof();
    }

    const Item& getItem() const {
        return curItem_;
    }

    void next() {
        std::string line;
        do {
            std::getline(ifs_, line);
        } while (line.empty() && !ifs_.eof());
        if (line.empty()) {
            return;
        } else {
            std::vector<std::string> items;
            boost::split(items, line, boost::is_any_of(","));
            REQUIRE(items.size() == 2, "Invalid file format");
            const std::string& name = items[0];
            const std::string& answer = items[1];
            REQUIRE(
                answer == YES_ANSWER || answer == NO_ANSWER,
                "Unknown answer: " + answer
            );
            std::string imagePath = common::joinPath(path_, IMAGE_DIR, name + ".jpg");
            curItem_.image = cv::imread(imagePath, cv::IMREAD_COLOR);
            std::string maskPath = common::joinPath(path_, MASK_DIR, name + ".png");
            curItem_.mask = cv::imread(maskPath, cv::IMREAD_GRAYSCALE);
            curItem_.answer = answer;
        }
    }

private:
    static const std::string LIST_NAME;
    static const std::string IMAGE_DIR;
    static const std::string MASK_DIR;

    std::string path_;
    std::ifstream ifs_;
    Item curItem_;
};

const std::string Dataset::LIST_NAME = "list.txt";
const std::string Dataset::IMAGE_DIR = "image";
const std::string Dataset::MASK_DIR = "mask";

} // namespace

std::ostream& operator<<(std::ostream& os, const TestResult& result) {
    os << "True positive: " << result.truePositive << "\n";
    os << "False negative: " << result.falseNegative << "\n";
    os << "True negative: " << result.trueNegative << "\n";
    os << "False negative: " << result.falsePositive << "\n";
    os << "Precision: "
       << result.truePositive / float(result.truePositive + result.falseNegative) << "\n";
    os << "Recall: "
       << result.truePositive / float(result.truePositive + result.falsePositive) << "\n";
    return os;
}

TestResult evaluateClassifier(const std::string& datasetPath) {
    AutoToloker toloker;
    TestResult testResult;
    Dataset dataset(datasetPath);

    for (; dataset.isValid(); dataset.next()) {
        const Dataset::Item& item = dataset.getItem();
        float confidence = toloker.classify(item.image, item.mask);
        if (YES_ANSWER == item.answer) {
            if (confidence >= 0.5) {
                testResult.truePositive++;
            } else {
                testResult.falseNegative++;
            }
        } else {
            if (confidence < 0.5) {
                testResult.trueNegative++;
            } else {
                testResult.falsePositive++;
            }
        }
    }

    return testResult;
}

} // namespace maps::wiki::autocart
