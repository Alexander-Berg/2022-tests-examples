#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/feature.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class FeatureChangeBuilderTest : public testing::Test {
public:
    FeatureChangeBuilder changeBuilder;

    NSprav::FeatureValue buildFeature(NSprav::Action action, int64_t id) {
        NSprav::FeatureValue result;
        result.set_action(action);
        result.set_featureid(id);
        return result;
    }

    NSprav::FeatureValue buildFeature(NSprav::Action action, int64_t id, bool value) {
        NSprav::FeatureValue result = buildFeature(action, id);
        result.set_boolean_value(value);
        return result;
    }

    NSprav::FeatureValue buildFeature(NSprav::Action action, int64_t id, int64_t enumValue) {
        NSprav::FeatureValue result = buildFeature(action, id);
        result.set_enum_id(enumValue);
        return result;
    }

    NSprav::FeatureValue buildFeature(NSprav::Action action, int64_t id, double from, double to) {
        NSprav::FeatureValue result = buildFeature(action, id);
        result.mutable_double_range()->set_min(from);
        result.mutable_double_range()->set_max(to);
        return result;
    }

    NSprav::FeatureValue WithUnitId(NSprav::FeatureValue feature, int64_t unitId) {
        feature.set_unitid(unitId);
        return feature;
    }

    NSprav::Company buildChanges(std::vector<NSprav::FeatureValue> features) {
        NSprav::Company result;
        *result.mutable_features() = {features.begin(), features.end()};
        return result;
    }

    NSpravTDS::FeatureValue buildTDSFeature(int64_t id) {
        NSpravTDS::FeatureValue result;
        result.set_feature_id(id);
        return result;
    }

    NSpravTDS::FeatureValue buildTDSFeature(int64_t id, bool value) {
        NSpravTDS::FeatureValue result = buildTDSFeature(id);
        result.set_value(std::to_string(value));
        return result;
    }

    NSpravTDS::FeatureValue buildTDSFeature(int64_t id, std::vector<int64_t> enumValues) {
        NSpravTDS::FeatureValue result = buildTDSFeature(id);
        *result.mutable_enum_values() = {enumValues.begin(), enumValues.end()};
        return result;
    }

    NSpravTDS::FeatureValue buildTDSFeature(int64_t id, double from, double to) {
        NSpravTDS::FeatureValue result = buildTDSFeature(id);
        result.set_min_value(from);
        result.set_max_value(to);
        return result;
    }

    NSpravTDS::FeatureValue WithUnitId(NSpravTDS::FeatureValue feature, int64_t unitId) {
        feature.set_unit_id(unitId);
        return feature;
    }

    NSpravTDS::Company buildCompany(std::vector<NSpravTDS::FeatureValue> features) {
        NSpravTDS::Company result;
        *result.mutable_feature_values() = {features.begin(), features.end()};
        return result;
    }
};

TEST_F(FeatureChangeBuilderTest, BoolDelete) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, false),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, false)});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, false),
            {},
            true
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, BoolIgnoreDeleted) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, false),
    }));

    NSpravTDS::Company company = buildCompany({});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {};

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, BoolIgnoreChanged) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, false),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, true)});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, true),
            {},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, BoolCreate) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::ACTUALIZE, 1, true),
    }));

    NSpravTDS::Company company = buildCompany({});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            {},
            {{
                buildTDSFeature(1, true),
                {1},
                NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, BoolChange) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::ACTUALIZE, 1, true),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, false)});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, false),
            {{
                buildTDSFeature(1, true),
                {1},
                NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, BoolChange2) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, false),
        buildFeature(NSprav::Action::ACTUALIZE, 1, true),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, false)});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, false),
            {{
                buildTDSFeature(1, true),
                {1},
                NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, EnumDeleteFull) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, 1l),
        buildFeature(NSprav::Action::DELETE, 1, 2l),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, {1, 2})});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, {1, 2}),
            {},
            true
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, EnumDeletePart) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, 1l),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, {1, 2})});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, {1, 2}),
            {{
                buildTDSFeature(1, std::vector{2l}), {1}, NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, EnumAdd) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::ACTUALIZE, 1, 3l),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, {1, 2})});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, {1, 2}),
            {{
                buildTDSFeature(1, {1, 2, 3}), {1}, NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, EnumAddAndDelete) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, 1l),
        buildFeature(NSprav::Action::ACTUALIZE, 1, 3l),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, {1, 2})});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, {1, 2}),
            {{
                buildTDSFeature(1, {2, 3}), {1}, NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, EnumAddExisting) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::ACTUALIZE, 1, 1l),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, {1, 2})});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, {1, 2}),
            {},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, EnumCreate) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::ACTUALIZE, 1, 1l),
    }));

    NSpravTDS::Company company = buildCompany({});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            {},
            {{
                buildTDSFeature(1, std::vector{1l}), {1}, NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, EnumDeleteNonExistent) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, 1l),
    }));

    NSpravTDS::Company company = buildCompany({});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {};

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, EnumMultipleRequests) {
    proto::Request request1 = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::ACTUALIZE, 1, 1l),
    }));

    proto::Request request2 = buildRequest(2, buildChanges({
        buildFeature(NSprav::Action::ACTUALIZE, 1, 3l),
    }));

    proto::Request request3 = buildRequest(3, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, 1l),
    }));

    proto::Request request4 = buildRequest(4, buildChanges({
        buildFeature(NSprav::Action::ACTUALIZE, 1, 1l),
    }));

    NSpravTDS::Company company = buildCompany({});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            {},
            {{
                buildTDSFeature(1, std::vector{1l}), {1, 4}, NSprav::Action::NONE
            }, {
                buildTDSFeature(1, std::vector{3l}), {2}, NSprav::Action::NONE
            }},
            false
        },
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result =
        changeBuilder.apply({request1, request2, request3, request4}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, NumberRangeDeleteDifferent) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, 2, 10),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, 1, 10)});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, 1, 10),
            {},
            false
        }
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, NumberRangeDeleteSame) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, 1, 10),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, 1, 10)});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, 1, 10),
            {},
            true
        }
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, NumberRangeChange) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::DELETE, 1, 1, 10),
        buildFeature(NSprav::Action::ACTUALIZE, 1, 2, 20),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, 1, 10)});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, 1, 10),
            {{
                buildTDSFeature(1, 2, 20), {1}, NSprav::Action::NONE
            }},
            false
        }
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, NumberRangeChangeNoDelete) {
    proto::Request request = buildRequest(1, buildChanges({
        buildFeature(NSprav::Action::ACTUALIZE, 1, 2, 20),
    }));

    NSpravTDS::Company company = buildCompany({buildTDSFeature(1, 1, 10)});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            buildTDSFeature(1, 1, 10),
            {{
                buildTDSFeature(1, 2, 20), {1}, NSprav::Action::NONE
            }},
            false
        }
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(FeatureChangeBuilderTest, NumberRangeChangeDifferentUnits) {
    proto::Request request = buildRequest(1, buildChanges({
        WithUnitId(buildFeature(NSprav::Action::ACTUALIZE, 1, 2, 20), 1),
    }));

    NSpravTDS::Company company = buildCompany({WithUnitId(buildTDSFeature(1, 1, 10), 2)});

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> expected = {
        {
            WithUnitId(buildTDSFeature(1, 1, 10), 2),
            {{
                WithUnitId(buildTDSFeature(1, 2, 20), 1), {1}, NSprav::Action::NONE
            }},
            false
        }
    };

    std::vector<AttributeChanges<NSpravTDS::FeatureValue>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

} // maps::sprav::callcenter::company_changes::tests
