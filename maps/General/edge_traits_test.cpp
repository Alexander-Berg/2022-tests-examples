#include "base_test.h"
#include "util.h"

#include <optional>

using namespace maps::road_graph;

class EdgeTraitsTest : public BaseTest {
public:
    void operator()() const override {
        size_t checkedCount = 0;
        size_t unequalCategoryCount = 0;
        size_t unequalTypeCount = 0;
        size_t unequalStructTypeCount = 0;
        size_t unequalAccessIdCount = 0;
        size_t unequalIsTollCount = 0;
        size_t unequalEndsWithTrafficLightCount = 0;
        size_t unequalHasMasstransitLaneCount = 0;
        size_t unequalSpeedCount = 0;
        size_t unequalSpeedLimitCount = 0;
        size_t unequalTruckSpeedLimitCount = 0;
        size_t unequalLengthCount = 0;
        size_t unequalIsPavedCount = 0;
        size_t unequalIsInPoorConditionCount = 0;
        size_t unequalIsResidentialCount = 0;
        size_t unequalIsRestrictedForTrucksCount = 0;
        size_t unequalIsoCodeCount = 0;
        size_t unequalTrafficSideCount = 0;

        std::optional<double> expMinSpeed;
        std::optional<double> expMaxSpeed;
        std::optional<double> expMinLength;
        std::optional<double> expMaxLength;
        std::optional<double> etlMinSpeed;
        std::optional<double> etlMaxSpeed;
        std::optional<double> etlMinLength;
        std::optional<double> etlMaxLength;

        for (EdgeId edgeId{0}; edgeId < experimentRoadGraph().edgesNumber(); ++edgeId) {
            if (!experimentRoadGraph().isBase(edgeId)) {
                continue;
            }

            LongEdgeId longEdgeId = experimentPersistentIndex().findLongId(edgeId).value();

            auto etlEdgeId = etalonPersistentIndex().findShortId(longEdgeId);
            if (!etlEdgeId) {
                continue;
            }

            ++checkedCount;

            const EdgeData& expEdgeData = experimentRoadGraph().edgeData(edgeId);
            const EdgeData& etlEdgeData = etalonRoadGraph().edgeData(*etlEdgeId);

            if (expEdgeData.category() != etlEdgeData.category()) {
                ++unequalCategoryCount;
            }
            if (expEdgeData.type() != etlEdgeData.type()) {
                ++unequalTypeCount;
            }
            if (expEdgeData.structType() != etlEdgeData.structType()) {
                ++unequalStructTypeCount;
            }
            if (expEdgeData.accessIdMask() != etlEdgeData.accessIdMask()) {
                ++unequalAccessIdCount;
            }
            if (expEdgeData.isToll() != etlEdgeData.isToll()) {
                ++unequalIsTollCount;
            }
            if (expEdgeData.endsWithTrafficLight() != etlEdgeData.endsWithTrafficLight()) {
                ++unequalEndsWithTrafficLightCount;
            }
            if (expEdgeData.hasMasstransitLane() != etlEdgeData.hasMasstransitLane()) {
                ++unequalHasMasstransitLaneCount;
            }
            if (expEdgeData.speed() != etlEdgeData.speed()) {
                ++unequalSpeedCount;
            }
            if (expEdgeData.speedLimit(AccessId::Automobile) != etlEdgeData.speedLimit(AccessId::Automobile)) {
                ++unequalSpeedLimitCount;
            }
            if (expEdgeData.speedLimit(AccessId::Truck) != etlEdgeData.speedLimit(AccessId::Truck)) {
                ++unequalTruckSpeedLimitCount;
            }
            if (expEdgeData.length() != etlEdgeData.length()) {
                ++unequalLengthCount;
            }
            if (expEdgeData.isPaved() != etlEdgeData.isPaved()) {
                ++unequalIsPavedCount;
            }
            if (expEdgeData.isInPoorCondition() != etlEdgeData.isInPoorCondition()) {
                ++unequalIsInPoorConditionCount;
            }
            if (expEdgeData.isResidential() != etlEdgeData.isResidential()) {
                ++unequalIsResidentialCount;
            }
            if (expEdgeData.isRestrictedForTrucks() != etlEdgeData.isRestrictedForTrucks()) {
                ++unequalIsRestrictedForTrucksCount;
            }
            if (expEdgeData.isoCode() != etlEdgeData.isoCode()) {
                ++unequalIsoCodeCount;
            }
            if (expEdgeData.trafficSide() != etlEdgeData.trafficSide()) {
                ++unequalTrafficSideCount;
            }

            if (!expMinSpeed || *expMinSpeed > expEdgeData.speed()) {
                expMinSpeed = expEdgeData.speed();
            }
            if (!expMaxSpeed || *expMaxSpeed < expEdgeData.speed()) {
                expMaxSpeed = expEdgeData.speed();
            }
            if (!expMinLength || *expMinLength > expEdgeData.length()) {
                expMinLength = expEdgeData.length();
            }
            if (!expMaxLength || *expMaxLength < expEdgeData.length()) {
                expMaxLength = expEdgeData.length();
            }

            if (!etlMinSpeed || *etlMinSpeed > etlEdgeData.speed()) {
                etlMinSpeed = etlEdgeData.speed();
            }
            if (!etlMaxSpeed || *etlMaxSpeed < etlEdgeData.speed()) {
                etlMaxSpeed = etlEdgeData.speed();
            }
            if (!etlMinLength || *etlMinLength > etlEdgeData.length()) {
                etlMinLength = etlEdgeData.length();
            }
            if (!etlMaxLength || *etlMaxLength < etlEdgeData.length()) {
                etlMaxLength = etlEdgeData.length();
            }
        }

        INFO() << "\tChecked edges: " <<
            checkedCount << " out of " << experimentRoadGraph().edgesNumber();

        auto printCheckResults = [](
                size_t checkedCount, size_t unequalCount, const std::string& name) {
            INFO() << "\tUnequal " << name << ": " <<
                outOfStr(unequalCount, checkedCount);
        };

        printCheckResults(checkedCount, unequalCategoryCount, "category");
        printCheckResults(checkedCount, unequalTypeCount, "type");
        printCheckResults(checkedCount, unequalStructTypeCount, "structType");
        printCheckResults(checkedCount, unequalAccessIdCount, "accessId");
        printCheckResults(checkedCount, unequalIsTollCount, "isToll");
        printCheckResults(checkedCount, unequalEndsWithTrafficLightCount, "endsWithTrafficLight");
        printCheckResults(checkedCount, unequalHasMasstransitLaneCount, "hasMasstransitLane");
        printCheckResults(checkedCount, unequalSpeedCount, "speed");
        printCheckResults(checkedCount, unequalSpeedLimitCount, "speedLimit");
        printCheckResults(checkedCount, unequalTruckSpeedLimitCount, "truckSpeedLimit");
        printCheckResults(checkedCount, unequalLengthCount, "length");
        printCheckResults(checkedCount, unequalIsPavedCount, "isPaved");
        printCheckResults(checkedCount, unequalIsInPoorConditionCount, "isInPoorCondition");
        printCheckResults(checkedCount, unequalIsResidentialCount, "isResidential");
        printCheckResults(checkedCount, unequalIsRestrictedForTrucksCount, "isRestrictedForTrucks");
        printCheckResults(checkedCount, unequalIsoCodeCount, "isoCode");
        printCheckResults(checkedCount, unequalTrafficSideCount, "trafficSide");

        auto printOptional = [](const auto& o, const std::string& name) {
            INFO() << "\t" << name << ": " << (o ? std::to_string(*o) : "-");
        };

        printOptional(expMinSpeed, "experiment min speed");
        printOptional(expMaxSpeed, "experiment max speed");
        printOptional(expMinLength, "experiment min length");
        printOptional(expMaxLength, "experiment max length");

        printOptional(etlMinSpeed, "etalon min speed");
        printOptional(etlMaxSpeed, "etalon max speed");
        printOptional(etlMinLength, "etalon min length");
        printOptional(etlMaxLength, "etalon max length");
    }
};

DECLARE_TEST(EdgeTraitsTest)
