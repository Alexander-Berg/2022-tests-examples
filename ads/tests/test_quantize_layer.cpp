#include "sis/quantize_layer.h"
#include "sis/inference_session.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <map>

namespace
{
  using namespace NSis;

  struct QuantizeLayerTest: ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      Dims = TDims2(4, 256);
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      auto input = Network->AddInput("input", GetValueType(), Dims);
      auto layer = Network->AddQuantize(input);
      layer->SetAmplitude(Amplitude);
      auto output = layer->GetOutput(0);
      EXPECT_EQ(output->GetType(), eInt8);
      Network->MarkOutput(output);
      return Network;
    }

    void PrepareData()
    {
      Input.resize(Dims.TotalSize());
      auto ptr = Input.data();
      int ld = 256;
      for (int m = 0; m != 4; ++m)
      {
        for (int n = 0; n != 256; ++n)
          ptr[n] = (n - 128) * (m + 1);
        ptr += ld;
      }
      for (auto v: Input)
      {
        auto r = RoundHalfToEven(v / Amplitude);
        if (r < -128)
          r = -128;
        if (r > 127)
          r = 127;
        Test.push_back((int8_t)r);
      }
      Output.resize(Input.size());
    }

    static float RoundHalfToEven(float f)
    {
      const float r = round(f);
      const float d = r - f;

      if ((d != 0.5f) && (d != -0.5f))
        return r;

      if (fmod(r, 2.0f) == 0.0f)
        return r;

      return f - d;
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      session->SetInput(0, Input.size(), CastInput(Input));
      session->Run();
      session->GetOutput(0, Output.size(), Output.data());
      session->Finalize();
      CastOutput(Output, eInt8);
    }

    const float Amplitude = 2;
    TDims Dims;
    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<float> Test;
  };
}

TEST_P(QuantizeLayerTest, StepExecution)
{
  PrepareData();
  BuildNetwork();
  CompileTestNetwork();
  PerformInference();

  ASSERT_EQ(Test, Output);
}

INSTANTIATE_TEST_SUITE_P(QuantizeLayerTest, QuantizeLayerTest,
                        ::testing::ValuesIn(
                                NTest::Combine(
                                        NTest::TTestSetup::Engines({"sis"}),
                                        NTest::TTestSetup::ValueTypes({"float", "half"}),
                                        NTest::TTestSetup::Compilers({"stage", "fusion", "kernel_barrier"})
                                )
                        ),
                        NTest::TTestSetup::Description()
);
