#include "sis/scale_layer.h"
#include "sis/inference_session.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>

namespace
{
  using namespace NSis;

  struct ScaleLayerTest: ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      Dims = TDims2(11, 128);
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      const auto testValueType = GetValueType();

      TWeights shiftWeights = {};
      shiftWeights.Type = testValueType;
      shiftWeights.ElementsCount = Shift.size();
      shiftWeights.Data = CastInput(Shift);

      TWeights scaleWeights = {};
      scaleWeights.Type = testValueType;
      scaleWeights.ElementsCount = Scale.size();
      scaleWeights.Data = CastInput(Scale);

      auto input = Network->AddInput("input", testValueType, Dims);
      auto layer = Network->AddScale(input, shiftWeights, scaleWeights);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
      return Network;
    }

    void PrepareData()
    {
      Input.resize(Dims.TotalSize());
      Output.resize(Dims.TotalSize());
      Test.resize(Dims.TotalSize(), 1);

      Shift.resize(Dims.InnerSize());
      Scale.resize(Dims.InnerSize());

      for (int n = 0; n != Dims.InnerSize(); ++n)
      {
        Shift[n] = 1 + n;
        Scale[n] = 1.f / (1 + n);
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

    TDims Dims;
    std::vector<float> Shift;
    std::vector<float> Scale;

    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<float> Test;
  };
}

TEST_P(ScaleLayerTest, StepExecution)
{
  PrepareData();
  BuildNetwork();
  CompileTestNetwork();
  PerformInference();


  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Output.size(); ++i)
    ASSERT_NEAR(Test[i], Output[i], precision);
}

INSTANTIATE_TEST_SUITE_P(ScaleLayerTest, ScaleLayerTest,
                        ::testing::ValuesIn(
                                NTest::Combine(
                                        NTest::TTestSetup::Engines({"sis", "tensor_rt"}),
                                        NTest::TTestSetup::InnerDims({33}),
                                        NTest::TTestSetup::ValueTypes({"float", "half"}),
                                        NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"})
                                )
                        ),
                        NTest::TTestSetup::Description()
);
