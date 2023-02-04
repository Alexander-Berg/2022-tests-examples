#include <ads/pytorch/deploy_v2/deep_embedding_model/model.h>
#include <ads/pytorch/deploy_v2/factory/factory.h>
#include <ads/bigkv/preprocessor_primitives/base_entities/infer_converters.h>
#include <ads/bigkv/preprocessor_primitives/base_preprocessor/helpers.h>

#include <library/cpp/testing/unittest/registar.h>


namespace {

    Y_UNIT_TEST_SUITE(InferConvertersTests) {

        Y_UNIT_TEST(UnwrapBasicTypeTest) {
            auto unwraped = NTorchEntity::Unwrap(NYT::TNode()("type_name", "uint64"));
            UNIT_ASSERT_VALUES_EQUAL(unwraped.size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(unwraped[0], "uint64");
        }

        Y_UNIT_TEST(UnwrapTest) {
            auto unwraped = NTorchEntity::Unwrap(
                NProfilePreprocessing::ListType(NProfilePreprocessing::ListType("double"))
            );
            UNIT_ASSERT_VALUES_EQUAL(unwraped.size(), 3);
            UNIT_ASSERT_VALUES_EQUAL(unwraped[0], "list");
            UNIT_ASSERT_VALUES_EQUAL(unwraped[1], "list");
            UNIT_ASSERT_VALUES_EQUAL(unwraped[2], "double");
        }

        Y_UNIT_TEST(InferConvertersTest) {
            auto [cat, rv] = NTorchEntity::InferConverters(
                {"BannerID", "BannerText"},
                {"CountersAggregatedValues"},
                {"BigBQuerySelectTypes"},
                NYT::TNode::TMapType{
                    {"BannerID", NYT::TNode()("type_name", "uint64")},
                    {"CountersAggregatedValues", NProfilePreprocessing::ListType("double")},
                    {"BigBQuerySelectTypes", NProfilePreprocessing::ListType("uint64")},
                    {"BannerText", NProfilePreprocessing::ListType("uint64")},
                }
            );

            auto features = TVector<NYT::TNode>{
                NYT::TNode::CreateMap(NYT::TNode::TMapType{
                    {"BannerID", NYT::TNode(1ULL)},
                    {"BannerText", NYT::TNode::CreateList().Add(2ULL).Add(3ULL)},
                    {"CountersAggregatedValues", NYT::TNode::CreateList().Add(3.4).Add(5.6)},
                    {"BigBQuerySelectTypes", NYT::TNode::CreateList().Add(7ULL).Add(8ULL)}
                }),
                NYT::TNode::CreateMap(NYT::TNode::TMapType{
                    {"BannerID", NYT::TNode(2ULL)},
                    {"BannerText", NYT::TNode::CreateList().Add(6ULL).Add(1ULL).Add(9ULL)},
                    {"CountersAggregatedValues", NYT::TNode::CreateList().Add(7.8)},
                    {"BigBQuerySelectTypes", NYT::TNode::CreateList().Add(6ULL).Add(8ULL).Add(10ULL)}
                })
            };

            THashMap<TString, NPytorchTransport2::TCategoricalInputs> catFeatures;
            THashMap<TString, NPytorchTransport2::TRealvalueInputs>   realvalueFeatures;

            for (const auto& [name, tensorMaker]: cat) {
                UNIT_ASSERT_UNEQUAL(tensorMaker, nullptr);
                catFeatures.emplace(name, tensorMaker->MakeTensor({features}, name));
            }

            for (const auto& [name, tensorMaker]: rv) {
                UNIT_ASSERT_UNEQUAL(tensorMaker, nullptr);
                realvalueFeatures.emplace(name, tensorMaker->MakeTensor({features}, name));
            }

            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BannerID").Data.Numel(), 2);
            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BannerID").DataLens.Numel(), 2);
            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BannerID").Data.At({0}), 1ULL);
            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BannerID").Data.At({1}), 2ULL);

            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BannerText").Data.Numel(), 5);
            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BannerText").DataLens.Numel(), 2);

            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BigBQuerySelectTypes").Data.Numel(), 5);
            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BigBQuerySelectTypes").DataLens.Numel(), 2 * 3);

            UNIT_ASSERT_VALUES_EQUAL(realvalueFeatures.at("CountersAggregatedValues").Sizes().at(0), 2);
            UNIT_ASSERT_VALUES_EQUAL(realvalueFeatures.at("CountersAggregatedValues").Sizes().at(1), 2);
        }
    }

}
