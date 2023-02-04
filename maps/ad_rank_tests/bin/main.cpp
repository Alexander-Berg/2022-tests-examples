#include <maps/renderer/denormalization/lib/tasks/impl/ad_common/ad_rank/ad_rank.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/resource/resource.h>

#include <iostream>
#include <vector>

namespace maps::renderer::denormalization {

namespace {

const auto INPUT_RESOURCE_NAME = "/cis1.tsv";
const auto LOCAL_RANK_LIMIT = 4;

struct InputDataset {
    AdMap adMap;
    std::unordered_map<PgBigInt, std::string> names;
};

InputDataset readInputDataset(std::istream& stream)
{
    InputDataset result;

    InputAdElement element;
    std::string name;
    std::string pAdId;
    ymapsdf::ad::LevelKind levelKind;
    double x; // NOLINT(cppcoreguidelines-init-variables)
    double y; // NOLINT(cppcoreguidelines-init-variables)
    ymapsdf::DisplayClass dispClass;

    while (stream >> element.adId >> name >> levelKind >> dispClass >> element.population >>
           element.capital >> element.town >> element.area >> pAdId >> element.isocode >> x >> y) {
        if (levelKind < ymapsdf::ad::LevelKind::Locality ||
            dispClass == ymapsdf::DisplayClass::Ignore) {
            continue;
        }

        element.point = {x, y};
        element.type = AdType::Locality;
        element.populationMetro = element.population;
        element.wikiVisits = 0;
        element.area = 0;

        result.adMap[element.adId] = element;
        result.names[element.adId] = name;
    }

    REQUIRE(stream.eof(), "Failed to read whole dataset, last ad_id " << element.adId);

    return result;
}

struct CheckerInfo {
    size_t zoom{};
    bool status{};
    PgBigInt adId{};
    std::string name;
};

std::vector<CheckerInfo> readCheckerInfo(std::istream& stream)
{
    std::vector<CheckerInfo> result;

    CheckerInfo info;
    while (stream >> info.zoom >> info.status >> info.adId >> info.name) {
        result.push_back(info);
    }
    return result;
}

void checkResult(
    std::ostream& stream,
    const InputDataset& inputDataset,
    const AdRanksMap& adRanks,
    const std::vector<CheckerInfo>& checkerInfo)
{
    for (const auto& info: checkerInfo) {
        REQUIRE(adRanks.count(info.adId), "Invalid ad_id " << info.adId);

        const auto& ad = adRanks.at(info.adId);
        bool status = ad.localRank.at(info.zoom) < LOCAL_RANK_LIMIT;

        stream << info.zoom << "\t" << status << "\t" << info.adId << "\t"
               << inputDataset.names.at(info.adId) << "\n";
    }
}

} // namespace

} // namespace maps::renderer::denormalization

int main(int /*argc*/, const char* /*argv*/[])
{
    using namespace maps::renderer::denormalization;

    std::stringstream data{NResource::Find(INPUT_RESOURCE_NAME)};

    auto inputDataset = readInputDataset(data);
    auto adRanks = markupAdRank(inputDataset.adMap);

    auto checkerInfo = readCheckerInfo(std::cin);

    checkResult(std::cout, inputDataset, adRanks, checkerInfo);

    return 0;
}
