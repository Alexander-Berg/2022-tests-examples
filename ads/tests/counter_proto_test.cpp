#include <ads/bsyeti/libs/primitives/counter.h>
#include <ads/bsyeti/libs/counter_lib/counter_packer/proto_extension_utils.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/common/env.h>
#include <library/cpp/protobuf/json/json2proto.h>
#include <library/cpp/protobuf/json/proto2json.h>
#include <library/cpp/logger/global/global.h>
#include <google/protobuf/repeated_field.h>

#include <util/string/vector.h>
#include <util/string/builder.h>
#include <util/system/file.h>
#include <util/stream/file.h>
#include <util/string/builder.h>
#include <util/generic/xrange.h>

#include <cstdlib>

using namespace NBSYeti;
    Y_UNIT_TEST_SUITE(CounterProto) {
        Y_UNIT_TEST(TestProtoBase) {
            bool testIsPassed = true;
            THashMap<TCounterId, i64> usedTimeCounters;

            for (const auto& ind : xrange( NCounter::ECounterId_descriptor()->value_count())) {
                const auto* enumDescriptor = NCounter::ECounterId_descriptor()->value(ind);
                const auto& enumNumber = enumDescriptor->number();
                if (enumNumber == 0) {
                    continue;
                }
                auto descriptionExtension = enumDescriptor->options().GetExtension(NCounter::CounterDescription);

                DEBUG_LOG << "Counter id: " << enumNumber << "\n";
                if (descriptionExtension.GetIdFunction() != NCounter::EIdFunction::IF_ID &&
                    descriptionExtension.GetIdFunction() != NCounter::EIdFunction::IF_POLY2 &&
                    descriptionExtension.GetIdFunction() != NCounter::EIdFunction::IF_POLY3) {
                    UNIT_FAIL(TStringBuilder{} << "Counter " << enumNumber << " has incorrect id function [field: IdFunction].");
                }
                if (descriptionExtension.GetEngineNamespace() == NCounter::EEngineNamespace::EN_UNDEFINED) {
                    UNIT_FAIL(TStringBuilder{} << "Counter " << enumNumber << " has incorrect engine namespace [field: EngineNamespace].");
                }
                // Check that id function and namespaces correspond:
                if ((descriptionExtension.GetSecondaryEngineNamespace() != NCounter::EEngineNamespace::EN_UNDEFINED) !=
                    (descriptionExtension.GetIdFunction() != NCounter::EIdFunction::IF_ID)) {
                    UNIT_FAIL(TStringBuilder{} << "Counter " << enumNumber << " has id function that not correspondes to its namespace");
                }
                if ((descriptionExtension.GetTertiaryEngineNamespace() != NCounter::EEngineNamespace::EN_UNDEFINED) &&
                    (descriptionExtension.GetSecondaryEngineNamespace() == NCounter::EEngineNamespace::EN_UNDEFINED)) {
                    UNIT_FAIL(TStringBuilder{} << "Counter " << enumNumber << " has TertiaryEngineNamespace, but has no SecondaryEngineNamespace");
                }
                if ((descriptionExtension.GetTertiaryEngineNamespace() !=  NCounter::EEngineNamespace::EN_UNDEFINED) &&
                    (descriptionExtension.GetIdFunction() != NCounter::EIdFunction::IF_POLY3)) {
                    UNIT_FAIL(TStringBuilder{} << "Counter " << enumNumber << " has TertiaryEngineNamespace, but has id function that not support it");
                }

                // Check that counter and its time counter has same namespace & id function
                if (descriptionExtension.GetTimeCounterId() != NCounter::ECounterId::CI_UNDEFINED) {
                    const auto timeDescription = GetCounterDescription(descriptionExtension.GetTimeCounterId());
                    if (timeDescription.GetIdFunction() != descriptionExtension.GetIdFunction()) {
                        UNIT_FAIL(TStringBuilder{}
                            << "Counter " << enumNumber
                            << " has IdFunction that differs from its time counters' one [field: IdFunction].");
                    }
                    if (timeDescription.GetEngineNamespace() != descriptionExtension.GetEngineNamespace()
                        || timeDescription.GetSecondaryEngineNamespace() != descriptionExtension.GetSecondaryEngineNamespace()) {
                        UNIT_FAIL(TStringBuilder{}
                            << "Counter " << enumNumber
                            << " has IdFunction that differs from its time counters' one [fields:  EngineNamespace, SecondaryEngineNamespace].");
                    }
                    // Check that time counter has age value type:
                    if (timeDescription.GetValueFunction() != NCounter::EValueFunction::VF_AGE) {
                        UNIT_FAIL(TStringBuilder{} << "Counter " << enumNumber
                                << " has " << static_cast<int>(descriptionExtension.GetTimeCounterId())
                                << "as time counter, but that counter is not time typed.");
                    }

                    // Check that there is no counters that has same time counter
                    if (usedTimeCounters.contains(descriptionExtension.GetTimeCounterId()) &&
                        timeDescription.GetProfileType() == NCounter::EProfileType::PT_PROFILE)
                    {
                        UNIT_FAIL(TStringBuilder{} << "Counter " << enumNumber
                            << " has " << static_cast<int>(descriptionExtension.GetTimeCounterId())
                            << "as time counter, but same counter is used by"
                            << static_cast<int>(usedTimeCounters.at(descriptionExtension.GetTimeCounterId()))
                        );
                    } else {
                        usedTimeCounters[descriptionExtension.GetTimeCounterId()] = enumNumber;
                    }
                }


                // Check that description is correct
                {
                    const auto& textDescription = descriptionExtension.GetDescription();
                    if (textDescription == "") {
                        UNIT_FAIL(TStringBuilder{} << "Counter " << enumNumber
                                << " has empty description.");
                    }
                    auto position = textDescription.find_first_of("\"\n");
                    if (position != TString::npos) {
                        UNIT_FAIL(TStringBuilder{} << "Counter " << enumNumber
                                << " has forbidden character \"" << textDescription[position]
                                << "\" at position " << position << ".");
                    }
                }


                // Check that counters with EN_BIAS has userscpace namespace
                {
                    constexpr i64 maximumCounterIdWithoutWideNameSpace = 1086;
                    if (descriptionExtension.GetEngineNamespace() == NCounter::EEngineNamespace::EN_BIAS) {
                        if (descriptionExtension.GetTimeCounterId() != NCounter::ECounterId::CI_UNDEFINED) {
                            const auto timeDescription = GetCounterDescription(descriptionExtension.GetTimeCounterId());
                            if (timeDescription.GetCleanUpSettings().GetMaxRecords() > 1) {
                                if (descriptionExtension.GetWideNamespace() == NCounter::EWideNamespace::WN_UNDEFINED) {
                                    if (enumNumber > maximumCounterIdWithoutWideNameSpace) {
                                        testIsPassed = false;
                                        ERROR_LOG << "Counter " << enumNumber
                                            << " has bias namespace, more than one record, bu hasn't WideNamespace.\n";
                                    } else {
                                        WARNING_LOG <<"Counter " << enumNumber
                                            << " has bias namespace, more than one record, but hasn't WideNamespace.\n";
                                    }
                                }
                            }
                        }
                    }
                }

           }

            if (!testIsPassed) {
                UNIT_FAIL("Tests haven't passed, see the messages above");
            }
        }
    };

