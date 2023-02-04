#include "sis/layer_norm_layer.h"
#include "sis/data/layer_norm.h"
#include "sis/data/fill_sin.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <cmath>

namespace
{
  using namespace NSis;

  struct LayerNormLayerTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());

      Dims = TDims3(11, 17, GetInnerDim());
    }

    void BuildNetwork()
    {
      Network = Builder->CreateNetwork();
      auto input = Network->AddInput("input", GetValueType(), Dims);
      auto layer = Network->AddLayerNorm(input);
      auto output = layer->GetOutput(0);
      TDims outputDims = output->GetDims();
      ASSERT_EQ(Dims, output->GetDims()) << Dims.Format() << " != " << outputDims.Format();
      Network->MarkOutput(output);
    }

    void PrepareData()
    {
      auto inputDims = Inference->GetInput(0)->GetDims();
      auto outputDims = Inference->GetOutput(0)->GetDims();

      ASSERT_EQ(inputDims, outputDims);

      Input.resize(inputDims.InnerSize() * inputDims.OuterSize());
      Output.resize(Input.size());

      NData::FillSin(Dims.OuterSize(), Dims.InnerSize(), Input.data());

      TestData = Input;
      float eps = 1e-6;
      NData::LayerNorm(inputDims.OuterSize(), inputDims.InnerSize(), TestData.data(), eps);
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      session->SetInput(0, Input.size(), CastInput(Input));
      session->Run();
      session->GetOutput(0, Output.size(), Output.data());
      session->Finalize();
      CastOutput(Output);
    }

    TDims Dims;

    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<float> TestData;
  };

  TEST_P(LayerNormLayerTest, Correct)
  {
    BuildNetwork();
    CompileTestNetwork();
    PrepareData();
    PerformInference();

    auto precision = GetTestTypePrecision();
    if (GetEngineType() == NSis::NTest::EEngine::eTensorRT)
    {
      precision = 1e-3;
      if (GetValueType() != NSis::eFloat)
        precision = 1e-2;
    }
    else
    {
      if (GetValueType() == NSis::eHalf)
        precision = 3e-3;
    }

    for (size_t i = 0; i != TestData.size(); ++i)
      ASSERT_NEAR(TestData[i], Output[i], precision);
  }
}

INSTANTIATE_TEST_SUITE_P(LayerNormLayerTest, LayerNormLayerTest,
                        ::testing::ValuesIn(
                                NTest::Add(
                                  NTest::Combine(
                                          NTest::TTestSetup::Engines({"sis", "tensor_rt"}),
                                          NTest::TTestSetup::InnerDims({64}),
                                          NTest::TTestSetup::ValueTypes({"float", "half"}),
                                          NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"})
                                  ),
                                  NTest::Combine(
                                          NTest::TTestSetup::Engines({"sis"}),
                                          NTest::TTestSetup::InnerDims({32, 65, 128, 256}),
                                          NTest::TTestSetup::ValueTypes({"float", "half"}),
                                          NTest::TTestSetup::Compilers({"stage", "fusion"})
                                  )
                                )
                        ),
                        NTest::TTestSetup::Description()
);
