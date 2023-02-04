#include "sis/softmax_layer.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <cmath>

namespace {
  using namespace NSis;

  struct SoftmaxLayerTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
    }

    void BuildNetwork()
    {
      auto input = Network->AddInput("input", GetValueType(), TDims3(11, 17, GetInnerDim()));
      auto layer = Network->AddSoftmax(input);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void PrepareData()
    {
      auto inputDims = Inference->GetInput(0)->GetDims();
      auto outputDims = Inference->GetOutput(0)->GetDims();

      ASSERT_EQ(inputDims, outputDims);

      Input.resize(inputDims.InnerSize() * inputDims.OuterSize());
      Output.resize(Input.size());
      TestData.resize(Input.size());

      for (float &v: Input)
        v = (int) (10 + v) % 20 - 10;

      for (int i = 0; i != inputDims.OuterSize(); ++i)
      {
        float sum = 0;
        for (int j = 0; j != inputDims.InnerSize(); ++j)
        {
          auto v = Input[i * inputDims.InnerSize() + j];
          sum += expf(v);
        }
        float rscale = 1. / sum;
        for (int j = 0; j != inputDims.InnerSize(); ++j)
        {
          auto v = Input[i * inputDims.InnerSize() + j];
          auto &w = TestData[i * inputDims.InnerSize() + j];
          w = expf(v) * rscale;
        }
      }
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

    int InnerSize;

    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<float> TestData;
  };

  TEST_P(SoftmaxLayerTest, Correct)
  {
    BuildNetwork();
    CompileTestNetwork();
    PrepareData();
    PerformInference();

    auto precision = GetTestTypePrecision();
    for (size_t i = 0; i != TestData.size(); ++i)
      ASSERT_NEAR(TestData[i], Output[i], precision);
  }
}

INSTANTIATE_TEST_SUITE_P(SoftmaxLayerTest, SoftmaxLayerTest,
                        ::testing::ValuesIn(
                                NTest::Combine(
                                        NTest::TTestSetup::Engines({"sis", "tensor_rt"}),
                                        NTest::TTestSetup::InnerDims({32, 64, 65, 128, 256}),
                                        NTest::TTestSetup::ValueTypes({"float", "half"}),
                                        NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"})
                                )
                        ),
                        NTest::TTestSetup::Description()
);
