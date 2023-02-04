#include "sis/constant_layer.h"
#include "sis/inference_session.h"
#include "compiler_test_setup.h"
#include "sis/data/fill_sin.h"
#include <gtest/gtest.h>
#include <numeric>
#include <map>

namespace {
  using namespace NSis;

  struct ConstantLayerTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      Dimensions = TDims(13, 8, 4);
      Weights.resize(Dimensions.TotalSize());
      NData::FillSin(Dimensions.OuterSize(), Dimensions.InnerSize(), Weights.data());
      Output.resize(Weights.size());

      TWeights weights = {};
      weights.Type = GetValueType();
      weights.ElementsCount = Weights.size();
      weights.Data = CastInput(Weights);

      auto layer = Network->AddConstant(Dimensions, weights);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
      return Network;
    }

    TDims Dimensions;
    std::vector<float> Weights;
    std::vector<float> Output;
  };
}

TEST_P(ConstantLayerTest, Correct)
{
  NSis::INetworkDefinitionPtr network = BuildNetwork();
  NSis::IInferencePtr inference = CompileTestNetwork();
  auto session = inference->StartSession();

  auto outputDims = inference->GetOutput(0)->GetDims();
  ASSERT_EQ(Dimensions, outputDims);

  session->Run();
  session->GetOutput(0, Output.size(), Output.data());
  session->Finalize();
  CastOutput(Output);

  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Output.size(); ++i)
      ASSERT_NEAR(Weights[i], Output[i], precision);
}

INSTANTIATE_TEST_SUITE_P(ConstantLayerTest, ConstantLayerTest,
        ::testing::ValuesIn(
        NTest::Combine(
                NTest::TTestSetup::Engines({"sis", "tensor_rt"}),
                NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"}),
                NTest::TTestSetup::ValueTypes({"float", "half"})
        )
),
        NTest::TTestSetup::Description()
);
