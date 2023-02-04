#include "compiler_test_setup.h"
#include "sis/inference.h"
#include <gtest/gtest.h>
#include <numeric>
#include "sis/sparse_embedding_layer.h"
#include "sis/network_definition.h"
#include "sis/attention_layer.h"

#include <cmath>
#include <numeric>
#include "sis/sis_assert.h"

namespace
{
  using namespace NSis;
  struct MultiRagsTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      SetDumpKernelSource(true);

      InputDims = TDims(2, 5, 1, 11);
      InputDims.SetRaggedDimension(1);
      InputDims.SetRaggedDimension(3);
      InputDims.SetInnerDims(4);
      EmbedDims = TDims(10, 32);

      FillData();
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      Network = Builder->CreateNetwork();
      auto indices = Network->AddInput("input", eInt32, InputDims);

      TWeights embedWeights = {};
      embedWeights.Type = GetValueType();
      embedWeights.Data = CastInput(EmbedWeights);
      embedWeights.ElementsCount = EmbedWeights.size();
      auto embed = Network->AddSparseEmbedding(indices, EmbedDims, embedWeights)->GetOutput(0);

      auto attention = Network->AddAttention(embed, embed, embed);
      attention->SetHeads(1);
      attention->SetQKScale(1.0f / std::sqrt(32.0f));
      Network->MarkOutput(attention->GetOutput(0));

      return Network;
    }

    void FillData() {
      Input = {
        0, 1,
        2, 3, 4,

        5,
        6, 7,
        6, 7, 8
      };

      EmbedWeights.resize(32 * 10);
      for (size_t i = 0; i < 10; ++i) {
        for (size_t j = 0; j < 32; ++j) {
          EmbedWeights[i * 32 + j] = (j + i) / 200.0;
        }
      }

      InputRags = {{0, 2, 5}, {0, 2, 5, 6, 8, 11}};
      Output.resize(2 * 5 * 32);

      Expected = {
        0.0263028, 0.0389656, 0.0516285, 0.0642913, 0.0769541, 0.0896170,
        0.1022798, 0.1149427, 0.1276055, 0.1402684, 0.1529312, 0.1655941,
        0.1782569, 0.1909198, 0.2035826, 0.2162455, 0.2289083, 0.2415711,
        0.2542340, 0.2668968, 0.2795597, 0.2922225, 0.3048854, 0.3175482,
        0.3302110, 0.3428739, 0.3555368, 0.3681996, 0.3808624, 0.3935253,
        0.4061881, 0.4188510,
        0.0271973, 0.0399719, 0.0527466, 0.0655212, 0.0782959, 0.0910706,
        0.1038452, 0.1166199, 0.1293945, 0.1421692, 0.1549439, 0.1677185,
        0.1804932, 0.1932678, 0.2060425, 0.2188172, 0.2315918, 0.2443665,
        0.2571411, 0.2699158, 0.2826904, 0.2954651, 0.3082398, 0.3210144,
        0.3337891, 0.3465637, 0.3593384, 0.3721130, 0.3848877, 0.3976624,
        0.4104370, 0.4232117,
        0.0671360, 0.0774030, 0.0876700, 0.0979370, 0.1082040, 0.1184710,
        0.1287380, 0.1390050, 0.1492720, 0.1595390, 0.1698060, 0.1800730,
        0.1903400, 0.2006070, 0.2108740, 0.2211410, 0.2314080, 0.2416750,
        0.2519420, 0.2622090, 0.2724760, 0.2827430, 0.2930100, 0.3032770,
        0.3135440, 0.3238110, 0.3340780, 0.3443450, 0.3546120, 0.3648790,
        0.3751460, 0.3854130,
        0.0695206, 0.0800857, 0.0906508, 0.1012159, 0.1117809, 0.1223460,
        0.1329111, 0.1434762, 0.1540413, 0.1646063, 0.1751714, 0.1857365,
        0.1963016, 0.2068666, 0.2174317, 0.2279968, 0.2385619, 0.2491270,
        0.2596920, 0.2702571, 0.2808222, 0.2913873, 0.3019524, 0.3125174,
        0.3230825, 0.3336476, 0.3442127, 0.3547777, 0.3653428, 0.3759079,
        0.3864730, 0.3970380,
        0.0718692, 0.0827278, 0.0935865, 0.1044451, 0.1153038, 0.1261624,
        0.1370211, 0.1478797, 0.1587384, 0.1695970, 0.1804557, 0.1913143,
        0.2021730, 0.2130316, 0.2238903, 0.2347489, 0.2456076, 0.2564662,
        0.2673249, 0.2781835, 0.2890421, 0.2999008, 0.3107595, 0.3216181,
        0.3324768, 0.3433354, 0.3541940, 0.3650527, 0.3759114, 0.3867700,
        0.3976287, 0.4084873
      };
    }

    TDims InputDims;
    TDims EmbedDims;

    std::vector<int> Input;
    std::vector<std::vector<int>> InputRags;
    std::vector<float> Output;
    std::vector<float> Expected;
    std::vector<float> EmbedWeights;
  };
}

TEST_P(MultiRagsTest, Pass)
{
  NSis::INetworkDefinitionPtr network = BuildNetwork();
  NSis::IInferencePtr inference = CompileTestNetwork();
  auto session = inference->StartSession();

  int64_t offsetsCount[TDims::MAX_DIMS];
  const int* offsets[TDims::MAX_DIMS];
  for (size_t i = 0; i < InputRags.size(); ++i) {
    offsets[i] = InputRags[i].data();
    offsetsCount[i] =  InputRags[i].size();
  }
  session->SetRaggedInput(0, InputRags.size(), offsetsCount, offsets, Input.size(), static_cast<void*>(Input.data()));

  session->Run();
  session->GetOutput(0, inference->GetOutput(0)->GetDims().TotalSize(), Output.data());
  session->Finalize();

  CastOutput(Output);
  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Expected.size(); ++i)
    ASSERT_NEAR(Expected[i], Output[i], precision);

}

INSTANTIATE_TEST_SUITE_P(MultiRagsTest, MultiRagsTest,
                         ::testing::ValuesIn(
                                 NTest::Combine(
                                         NTest::TTestSetup::Engines({"sis"}),
                                         NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"}),
                                         NTest::TTestSetup::ValueTypes({"float", "half"})
                                 )
                         ),
                         NTest::TTestSetup::Description()
);
