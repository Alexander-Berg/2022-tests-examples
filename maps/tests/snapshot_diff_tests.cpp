#include <maps/infra/apiteka/datamodel/tests/matchers.h>
#include <maps/infra/apiteka/server/include/snapshot_diff.h>
#include <maps/infra/apiteka/server/tests/helpers.h>
#include <maps/infra/apiteka/server/tests/samples.h>

#include <gmock/gmock.h>
#include <gtest/gtest.h>

#include <typeinfo>

namespace maps::apiteka::tests {
namespace proto = yandex::maps::proto::apiteka;
namespace aptk = maps::apiteka;
using namespace testing;

TEST(SnapshotDiff, ZeroDiffOnSameSnapshot)
{
    ASSERT_TRUE(
        SnapshotDifferenceCalculator{}
            .calculate(samples::DEFAULT_SNAPSHOT, samples::DEFAULT_SNAPSHOT)
            .isEmpty());
}

TEST(SnapshotDiff, ApiKeySpecDifference)
{
    auto keyRemoval{samples::DEFAULT_SNAPSHOT};
    auto& keySpecs{*keyRemoval.mutable_key_specs()};
    keySpecs.erase(helpers::getKey(keySpecs, samples::APIKEYS_INACTIVE_KEY.id));

    SnapshotDifferenceCalculator differ;
    {
        const auto diff{differ.calculate(samples::DEFAULT_SNAPSHOT, keyRemoval)};
        EXPECT_THAT(
            diff.removedKeys, ElementsAre(samples::APIKEYS_INACTIVE_KEY.id));
        EXPECT_THAT(
            diff,
            FieldsAre(IsEmpty(), _, IsEmpty(), IsEmpty(), IsEmpty(), IsEmpty()));
    }

    const auto diff{differ.calculate(keyRemoval, samples::DEFAULT_SNAPSHOT)};
    EXPECT_THAT(
        diff.upsertedKeys,
        ElementsAre(matchers::equalsTo(samples::APIKEYS_INACTIVE_KEY)));
    EXPECT_THAT(
        diff,
        FieldsAre(_, IsEmpty(), IsEmpty(), IsEmpty(), IsEmpty(), IsEmpty()));
}

TEST(SnapshotDiff, ApiKeySpecUpdate)
{
    using ValueCase = proto::ApiKeySpec::Restriction::ValueCase;

    auto keyUpdate{samples::DEFAULT_SNAPSHOT};
    auto updatedKeyIter{helpers::getKey(
        *keyUpdate.mutable_key_specs(), samples::APIKEYS_KEY.id)};
    updatedKeyIter->set_is_active(!updatedKeyIter->is_active());

    auto ipRestriction{helpers::getRestriction(
        *updatedKeyIter->mutable_restrictions(), ValueCase::kIpAddress)};
    static const TString updatedIpAddress{"2.2.2.2"};
    ipRestriction->set_ip_address(updatedIpAddress);

    SnapshotDifferenceCalculator differ;
    const auto diff{differ.calculate(samples::DEFAULT_SNAPSHOT, keyUpdate)};
    ASSERT_THAT(
        diff.upsertedKeys,
        ElementsAre(testing::AllOf(
            matchers::idOfApiKeyIs(samples::APIKEYS_KEY.id),
            matchers::isActiveOfApiKeyIs(!samples::APIKEYS_KEY.isActive))));

    const auto& ipAddressRestriction{
        diff.upsertedKeys.front().restrictions["ip_address"]};
    ASSERT_EQ(ipAddressRestriction.size(), 1ul);
    EXPECT_EQ(ipAddressRestriction.begin()->as<std::string>(), updatedIpAddress);
}

TEST(SnapshotDiff, ProviderDifference)
{
    auto providerRemoval{samples::DEFAULT_SNAPSHOT};
    providerRemoval.clear_provider_keys_assignments();

    SnapshotDifferenceCalculator differ;
    {
        const auto diff{
            differ.calculate(samples::DEFAULT_SNAPSHOT, providerRemoval)};
        EXPECT_THAT(
            diff.removedPlans,
            ElementsAre(Pair(samples::PROVIDER.id, samples::APIKEYS_PLAN.id)));
        EXPECT_THAT(
            diff.removedAssignments,
            ElementsAre(matchers::equalsTo(Assignment{
                .providerId = std::string{samples::PROVIDER.id},
                .planId = std::string{samples::APIKEYS_PLAN.id},
                .apiKey = std::string{samples::APIKEYS_KEY.id}})));
        EXPECT_THAT(
            diff, FieldsAre(IsEmpty(), IsEmpty(), IsEmpty(), _, _, IsEmpty()));
    }

    const auto diff{
        differ.calculate(providerRemoval, samples::DEFAULT_SNAPSHOT)};
    EXPECT_THAT(
        diff.newAssignments,
        ElementsAre(matchers::equalsTo(Assignment{
            .providerId = samples::PROVIDER.id,
            .planId = samples::APIKEYS_PLAN.id,
            .apiKey = samples::APIKEYS_KEY.id})));
    EXPECT_THAT(
        diff.upsertedPlans,
        ElementsAre(matchers::equalsTo(samples::APIKEYS_PLAN)));
    EXPECT_THAT(
        diff, FieldsAre(IsEmpty(), IsEmpty(), _, IsEmpty(), IsEmpty(), _));
}

TEST(SnapshotDiff, ProviderPlanAddition)
{
    auto providerNewPlan{samples::DEFAULT_SNAPSHOT};

    static const std::string newPlanFeatures{R"json({"watermark": true})json"};
    static const Plan newPlan{
        .id = "new-plan",
        .origin = Origin::Apikeys,
        .providerId = samples::PROVIDER.id,
        .features = json::Value::fromString(newPlanFeatures)};

    static const Assignment newAssignment{
        .providerId = samples::PROVIDER.id,
        .planId = newPlan.id,
        .apiKey = samples::APIKEYS_INACTIVE_KEY.id,
    };

    auto& assignment{
        *(*providerNewPlan
               .mutable_provider_keys_assignments())[newAssignment.providerId]
             .add_assignment()};
    assignment.set_id(TString{newAssignment.planId});
    assignment.set_features(TString{newPlanFeatures});
    assignment.add_api_keys(TString{newAssignment.apiKey});

    SnapshotDifferenceCalculator differ;
    {
        const auto diff{
            differ.calculate(samples::DEFAULT_SNAPSHOT, providerNewPlan)};

        EXPECT_THAT(diff.upsertedPlans, ElementsAre(matchers::equalsTo(newPlan)));
        EXPECT_THAT(
            diff.newAssignments,
            ElementsAre(matchers::equalsTo(newAssignment)));
    }
    const auto reverseDiff(
        differ.calculate(providerNewPlan, samples::DEFAULT_SNAPSHOT));
    EXPECT_THAT(
        reverseDiff.removedPlans,
        ElementsAre(Pair(samples::PROVIDER.id, newPlan.id)));
    EXPECT_THAT(
        reverseDiff.removedAssignments,
        ElementsAre(matchers::equalsTo(newAssignment)));
}
} // namespace maps::apiteka::tests
