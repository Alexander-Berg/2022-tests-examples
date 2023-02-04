#include <ads/pytorch/deploy_v2/deep_embedding_model/model.h>
#include <ads/pytorch/deploy_v2/factory/factory.h>
#include <ads/bigkv/preprocessor_primitives/base_entities/entity.h>
#include <ads/bigkv/preprocessor_primitives/base_preprocessor/helpers.h>
#include <ads/bigkv/preprocessors/user_preprocessors/query_preprocessor.h>
#include <yabs/server/util/bobhash.h>
#include <ads/bigkv/text_preprocessing/text_preprocessing.h>
#include <ads/bigkv/preprocessor_primitives/base_preprocessor/post_processors.h>
#include <ads/bigkv/preprocessor_primitives/tests/test_helpers/user_proto_helpers.h>

#include <library/cpp/testing/unittest/registar.h>


namespace {

    using TQueryTextTokenizer = NProfilePreprocessing::TTokenizePostprocessor<NTextUtils::TTokenizerWrapper>;
    using TQueryHashTokenPostprocessor = NProfilePreprocessing::TComposePostprocessors<
        TQueryTextTokenizer,
        NProfilePreprocessing::TTextToIDPostprocessor
    >;
    using TQueryParser = NProfilePreprocessing::TParser<
        NProfilePreprocessing::TQueryFactorsComputer,
        TQueryHashTokenPostprocessor
    >;

    class TMyEntity: public NTorchEntity::TBaseEntity {
    public:
        TMyEntity()
            : TBaseEntity(
                std::static_pointer_cast<NProfilePreprocessing::IPreprocessor>(
                    std::make_shared<TQueryParser>(
                        NProfilePreprocessing::TQueryFactorsComputer(),
                        TQueryHashTokenPostprocessor({
                            TQueryTextTokenizer(NTextUtils::TTokenizerWrapper(true), TVector<TString>{"BigBQueryTexts"}),
                            NProfilePreprocessing::TTextToIDPostprocessor(yabs_bobhash, TVector<TString>{"BigBQueryTexts"})
                        })
                    )
                ),
                {"BigBQueryTexts"},
                {"BigBQuerySelectTypes"},
                {"BigBQueryFactors"}
            ) { }
    };

    Y_UNIT_TEST_SUITE(EntitiesTests) {
        Y_UNIT_TEST(CreateEntityTest) {
            TMyEntity entity;
            THashMap<TString, NPytorchTransport2::TCategoricalInputs> catFeatures;
            THashMap<TString, NPytorchTransport2::TRealvalueInputs> realvalueFeatures;

            NProfilePreprocessing::TUserProtoBuilder profileBuilder1, profileBuilder2, profileBuilder3;
            profileBuilder1.AddQuery("First query",  1, 2, 3, 4, 5, 6);
            profileBuilder2.AddQuery("Second query", 9, 8, 7, 6, 5, 4);
            profileBuilder2.AddQuery("Third query",  3, 2, 1, 7, 8, 9);
            profileBuilder3.AddQuery("Fourth query", 7, 8, 9, 4, 3, 2);

            entity.MakeInputs(
                {*profileBuilder1.GetProfile(), *profileBuilder2.GetProfile(), *profileBuilder3.GetProfile()}, {123, 456, 789},
                catFeatures, realvalueFeatures
            );

            UNIT_ASSERT(catFeatures.contains("BigBQueryTexts"));
            UNIT_ASSERT(catFeatures.contains("BigBQuerySelectTypes"));
            UNIT_ASSERT(realvalueFeatures.contains("BigBQueryFactors"));

            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BigBQueryTexts").DataLens.Sizes()[0], 3);
            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BigBQueryTexts").DataLens.Sizes()[1], 2);

            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BigBQuerySelectTypes").DataLens.Sizes()[0], 3);
            UNIT_ASSERT_VALUES_EQUAL(catFeatures.at("BigBQuerySelectTypes").DataLens.Sizes()[1], 2);

            UNIT_ASSERT_VALUES_EQUAL(realvalueFeatures.at("BigBQueryFactors").Sizes()[0], 3);
            UNIT_ASSERT_VALUES_EQUAL(realvalueFeatures.at("BigBQueryFactors").Sizes()[1], 2);
            UNIT_ASSERT_VALUES_EQUAL(realvalueFeatures.at("BigBQueryFactors").Sizes()[2], 5);
        }
    }

}
