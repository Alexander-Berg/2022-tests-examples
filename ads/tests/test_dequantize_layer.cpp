#include "sis/dequantize_layer.h"
#include "sis/inference_session.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <map>

namespace
{
  using namespace NSis;

  struct DequantizeLayerTest: ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      QuantType = DecodeTypeName(GetCustom("quant"));
      Dims = TDims2(1, 256);
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      auto input = Network->AddInput("input", QuantType, Dims);
      auto layer = Network->AddDequantize(GetValueType(), input);
      layer->SetAmplitude(Amplitude);
      auto output = layer->GetOutput(0);
      EXPECT_EQ(output->GetType(), GetValueType());
      Network->MarkOutput(output);
      return Network;
    }

    void PrepareData()
    {
      Input.resize(Dims.TotalSize());
      for (int n = 0; n != 256; ++n)
        Input[n] = (n - 128);
      for (auto v: Input)
        Test.push_back(Amplitude * float(v));
      Output.resize(Input.size());
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      session->SetInput(0, Input.size(), CastInput(QuantType, Input));
      session->Run();
      session->GetOutput(0, Output.size(), Output.data());
      session->Finalize();
      CastOutput(Output, GetValueType());
    }

    const float Amplitude = 1.f / 127.f;
    DataType QuantType;
    TDims Dims;
    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<float> Test;
  };
}

TEST_P(DequantizeLayerTest, Correct)
{
  PrepareData();
  BuildNetwork();
  CompileTestNetwork();
  PerformInference();

  auto precision = GetTestTypePrecision();
  for (size_t i = 0; i != Output.size(); ++i)
  {
    if (Test[i] != Output[i])
      ASSERT_NEAR(Test[i], Output[i], precision);
  }
}

INSTANTIATE_TEST_SUITE_P(DequantizeLayerTest, DequantizeLayerTest,
                        ::testing::ValuesIn(
                                NTest::Combine(
                                        NTest::TTestSetup::Engines({"sis"}),
                                        NTest::TTestSetup::ValueTypes({"float", "half"}),
                                        NTest::TTestSetup::Compilers({"stage", "fusion", "kernel_barrier"}),
                                        NTest::TTestSetup::Custom("quant", [](auto v) {return std::to_string(v);}, {eInt8, eInt32})
                                )
                        ),
                        NTest::TTestSetup::Description()
);
