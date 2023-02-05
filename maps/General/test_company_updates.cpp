#include "common.h"

#include <maps/sprav/callcenter/libs/task/company_data.h>
#include <maps/sprav/callcenter/libs/test_helpers/util.h>
#include <maps/sprav/callcenter/libs/proto_tools/parsing.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/gtest_protobuf/matcher.h>

namespace maps::sprav::callcenter::task::tests {

TEST(CompanyUpdatesTest, CompanyDataSecondScreen) {
    NSprav::Company companyForCompanyUpdate;
    companyForCompanyUpdate.set_publishing_status(NSprav::PublishingStatus::NOT_ANSWERED);
    companyForCompanyUpdate.set_actualized_time(123);
    *companyForCompanyUpdate.add_phones() = parseJson<NSprav::Phone>(std::string(R"({
        "action": "actualize",
        "access": "public",
        "country_code": "7",
        "region_code": "800",
        "number": "2342480",
        "description": [],
        "type": [
            "phone"
        ],
        "mode": "world",
        "formatted": "8 (800) 234-24-80"
    })"));
    *companyForCompanyUpdate.add_phones() = parseJson<NSprav::Phone>(std::string(R"({
        "access": "public",
        "action": "actualize",
        "country_code": "90",
        "description": [],
        "formatted": "+90 442 213 20 00",
        "legality": "UNDEFINED",
        "mode": "WORLD",
        "number": "2132000",
        "rank": 1,
        "region_code": "442",
        "type": [
            "phone",
            "fax"
        ]
    })"));
    *companyForCompanyUpdate.add_names() = parseJson<NSprav::Name>(std::string(R"({
        "action": "actualize",
        "value": "Yandex",
        "type": "main",
        "lang": "en"
    })"));
    *companyForCompanyUpdate.add_names() = parseJson<NSprav::Name>(std::string(R"({
        "action": "actualize",
        "value": "Yandex2",
        "type": "main",
        "lang": "en"
    })"));
    companyForCompanyUpdate.set_allocated_owner_verification(new NSprav::OwnerVerification(
        parseJson<NSprav::OwnerVerification>(std::string(R"({
            "ts": 1
    })"))));
    
    {
        NSprav::CompanyUpdate companyUpdate;
        companyUpdate.set_allocated_company_id(new NSprav::CompanyUpdate::Id(
            test_helpers::protoFromTextFormat<NSprav::CompanyUpdate::Id>(R"(
                permalink: 1,
                commit_id: 1
        )")));
        companyUpdate.set_allocated_company(new NSprav::Company(companyForCompanyUpdate));
        companyUpdate.add_attributes(NSprav::CompanyAttribute::PUBLISH_STATUS);
        companyUpdate.add_attributes(NSprav::CompanyAttribute::PHONE);
        companyUpdate.add_attributes(NSprav::CompanyAttribute::ACTUALIZATION_DATE);
        companyUpdate.add_attributes(NSprav::CompanyAttribute::NAME);
        companyUpdate.add_attributes(NSprav::CompanyAttribute::OWNER_VERIFICATION);

        std::unique_ptr<PermalinkCompanyData> permalinkCompanyData(new PermalinkCompanyData({createRequest(1, 1), createRequest(2, 1)}));
        permalinkCompanyData->setTdsCompany(createTdsCompany(1, 1));
        std::unique_ptr<CompanyDataBase> companyData(std::move(permalinkCompanyData));

        companyData->computeSecondScreen();
        EXPECT_THAT(
            companyData->secondScreen(),
            NGTest::EqualsProto(companyData->draftlessSecondScreen())
        );

        auto taskCompanyUpdate = createTaskCompanyUpdate(1, std::nullopt, 1);
        taskCompanyUpdate.set_allocated_company_update(new NSprav::CompanyUpdate(std::move(companyUpdate)));
        companyData->setCompanyUpdate(taskCompanyUpdate);
        companyData->computeSecondScreen();
        EXPECT_THAT(
            companyData->secondScreen(),
            NGTest::EqualsProto(test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
                publishing_status: NotAnswered
                export_id: 1
                names {
                    value {
                        value: "Yandex"
                        lang {
                            locale: "EN"
                        }
                    }
                    type: Main
                }
                names {
                    value {
                        value: "Yandex2"
                        lang {
                        locale: "EN"
                        }
                    }
                    type: Main
                }
                phones {
                    number: "2342480"
                    type: Phone
                    hide: false
                    formatted: "8 (800) 234-24-80"
                }
                phones {
                    number: "2132000"
                    type: PhoneFax
                    hide: false
                    rank: 1
                    formatted: "+90 442 213 20 00"
                    legality: Legal
                }
                actualized: 123
                export_without_ext_snippet: false
                export_without_address_snippet: false
                owner_verification {
                    verification_ts: 1
                }
                commit_info {
                    commit_id: 1
                }
        )")));
    }
    {
        NSprav::CompanyUpdate companyUpdate;
        companyUpdate.set_allocated_company(new NSprav::Company(companyForCompanyUpdate));
        companyUpdate.add_attributes(NSprav::CompanyAttribute::PUBLISH_STATUS);
        companyUpdate.add_attributes(NSprav::CompanyAttribute::PHONE);
        companyUpdate.add_attributes(NSprav::CompanyAttribute::ACTUALIZATION_DATE);
        companyUpdate.add_attributes(NSprav::CompanyAttribute::NAME);
        companyUpdate.add_attributes(NSprav::CompanyAttribute::OWNER_VERIFICATION);

        auto creationRequest = createRequest(1, std::nullopt);
        auto name = creationRequest.mutable_feedback()->mutable_prepared_changes()->add_names();
        name->set_action(NSprav::ACTUALIZE);
        name->set_lang(NSprav::NLanguage::EN);
        name->set_value("TEST");

        std::unique_ptr<CompanyDataBase> companyData(new CreationCompanyData(creationRequest));
        companyData->computeSecondScreen();
        EXPECT_THAT(
            companyData->secondScreen(),
            NGTest::EqualsProto(companyData->draftlessSecondScreen())
        );

        auto taskCompanyUpdate = createTaskCompanyUpdate(1, 1, std::nullopt);
        taskCompanyUpdate.set_allocated_company_update(new NSprav::CompanyUpdate(std::move(companyUpdate)));
        companyData->setCompanyUpdate(taskCompanyUpdate);
        companyData->computeSecondScreen();
        EXPECT_THAT(
            companyData->secondScreen(),
            NGTest::EqualsProto(test_helpers::protoFromTextFormat<NSpravTDS::Company>(R"(
                publishing_status: NotAnswered
                export_id: 0
                names {
                    value {
                        value: "Yandex"
                        lang {
                            locale: "EN"
                        }
                    }
                    type: Main
                }
                names {
                    value {
                        value: "Yandex2"
                        lang {
                        locale: "EN"
                        }
                    }
                    type: Main
                }
                phones {
                    number: "2342480"
                    type: Phone
                    hide: false
                    formatted: "8 (800) 234-24-80"
                }
                phones {
                    number: "2132000"
                    type: PhoneFax
                    hide: false
                    rank: 1
                    formatted: "+90 442 213 20 00"
                    legality: Legal
                }
                actualized: 123
                export_without_ext_snippet: false
                export_without_address_snippet: false
                owner_verification {
                    verification_ts: 1
                }
                commit_info {
                    commit_id: 0
                }
        )")));
    }
}

} // maps::sprav::callcenter::task::tests
