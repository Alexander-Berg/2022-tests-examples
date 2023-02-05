#include "common.h"
#include "pretty_print.h"

#include <maps/sprav/callcenter/libs/company_changes/address.h>

#include <library/cpp/testing/gtest/gtest.h>

namespace maps::sprav::callcenter::company_changes::tests {

class AddressChangeBuilderTest : public testing::Test {
public:
    AddressChangeBuilder changeBuilder;

    NSprav::Address buildAddress(
        NSprav::Action action, NSprav::Address::Source source,
        double lat, double lon, const std::string& oneLine
    ) {
        NSprav::Address result;
        result.set_action(action);
        result.set_source(source);
        result.mutable_coordinates()->set_lat(lat);
        result.mutable_coordinates()->set_lon(lon);
        result.set_lang(NSprav::NLanguage::RU);
        result.set_one_line(oneLine.c_str());
        result.set_region_code("RU");
        result.set_geo_id(0);
        return result;
    }

    NSprav::Company buildChanges(std::vector<NSprav::Address> addresses) {
        NSprav::Company result;
        *result.mutable_addresses() = {addresses.begin(), addresses.end()};
        return result;
    }

    NSpravTDS::Address buildTDSAddress(double lat, double lon, const std::string& oneLine, bool isAuto) {
        NSpravTDS::Address result;
        result.mutable_pos()->mutable_point()->set_lon(lon);
        result.mutable_pos()->mutable_point()->set_lat(lat);
        result.set_is_auto(isAuto);
        result.mutable_formatted()->set_value(oneLine.c_str());
        result.mutable_formatted()->mutable_lang()->set_locale("RU");
        result.mutable_region_code()->set_code(234);
        result.set_geo_id(0);
        return result;
    }

    NSpravTDS::Company buildCompany(const NSpravTDS::Address& address) {
        NSpravTDS::Company result;
        result.mutable_address()->CopyFrom(address);
        return result;
    }
};

TEST_F(AddressChangeBuilderTest, Change) {
    proto::Request request = buildRequest(1, buildChanges({
        buildAddress(
            NSprav::Action::DELETE,
            NSprav::Address::COORDINATES,
            1.0, 1.0,
            "Адрес 1"
        ),
        buildAddress(
            NSprav::Action::ACTUALIZE,
            NSprav::Address::COORDINATES,
            2.0, 2.0,
            "Адрес 2"
        ),
    }));


    NSpravTDS::Company company = buildCompany(buildTDSAddress(1.001, 1.001, "Адрес 1", true));

    std::vector<AttributeChanges<AddressWithSource>> expected = {
        {
            AddressWithSource{buildTDSAddress(1.001, 1.001, "Адрес 1", true), NSprav::Address::ORIGINAL},
            {{
                AddressWithSource{buildTDSAddress(2.0, 2.0, "Адрес 2", false), NSprav::Address::COORDINATES},
                {1},
                NSprav::Action::NONE
            }},
            false
        }
    };

    std::vector<AttributeChanges<AddressWithSource>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(AddressChangeBuilderTest, ChangeCoordinates) {
    proto::Request request = buildRequest(1, buildChanges({
        buildAddress(
            NSprav::Action::DELETE,
            NSprav::Address::COORDINATES,
            37.587874, 55.73367,
            "Россия, Москва, улица Льва Толстого, 16"
        ),
        buildAddress(
            NSprav::Action::ACTUALIZE,
            NSprav::Address::COORDINATES,
            37.58910717621646, 55.734155197192784,
            "Россия, Москва, улица Льва Толстого, 16"
        ),
    }));


    NSpravTDS::Company company = buildCompany(buildTDSAddress(
        37.587874, 55.73367, "Россия, Москва, улица Льва Толстого, 16", true
    ));

    std::vector<AttributeChanges<AddressWithSource>> expected = {
        {
            AddressWithSource{
                buildTDSAddress(
                    37.587874, 55.73367, "Россия, Москва, улица Льва Толстого, 16", true
                ), NSprav::Address::ORIGINAL
            },
            {{
                AddressWithSource{
                    buildTDSAddress(
                        37.58910717621646, 55.734155197192784, "Россия, Москва, улица Льва Толстого, 16", false
                    ), NSprav::Address::COORDINATES
                },
                {1},
                NSprav::Action::NONE
            }},
            false
        }
    };

    std::vector<AttributeChanges<AddressWithSource>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(AddressChangeBuilderTest, Change2) {
    proto::Request request = buildRequest(1, buildChanges({
        buildAddress(
            NSprav::Action::ACTUALIZE,
            NSprav::Address::ORIGINAL,
            2.0, 2.0,
            "Адрес 1"
        ),
    }));


    NSpravTDS::Company company = buildCompany(buildTDSAddress(3.0, 3.0, "Адрес 2", true));

    std::vector<AttributeChanges<AddressWithSource>> expected = {
        {
            AddressWithSource{buildTDSAddress(3.0, 3.0, "Адрес 2", true), NSprav::Address::ORIGINAL},
            {{
                AddressWithSource{buildTDSAddress(2.0, 2.0, "Адрес 1", false), NSprav::Address::ORIGINAL},
                {1},
                NSprav::Action::NONE
            }},
            false
        }
    };

    std::vector<AttributeChanges<AddressWithSource>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}


TEST_F(AddressChangeBuilderTest, Delete) {
    proto::Request request = buildRequest(1, buildChanges({
        buildAddress(
            NSprav::Action::DELETE,
            NSprav::Address::ORIGINAL,
            1.0, 1.0,
            "Адрес"
        ),
    }));


    NSpravTDS::Company company = buildCompany(buildTDSAddress(1.0, 1.0, "Адрес", true));

    std::vector<AttributeChanges<AddressWithSource>> expected = {
        {
            AddressWithSource{buildTDSAddress(1.0, 1.0, "Адрес", true), NSprav::Address::ORIGINAL},
            {},
            true
        }
    };

    std::vector<AttributeChanges<AddressWithSource>> result = changeBuilder.apply({request}, company);

    EXPECT_THAT(result, ::testing::UnorderedPointwise(AttributeChangesEqual(), expected));
}

} // maps::sprav::callcenter::company_changes::tests
