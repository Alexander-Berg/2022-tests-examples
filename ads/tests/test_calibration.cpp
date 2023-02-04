#include "sis/network_calibration.h"
#include "sis/inference_session.h"
#include "sis/element_wise_layer.h"
#include "sis/constant_layer.h"
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <map>

namespace
{
  using namespace NSis;

  struct CalibrationTest: ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      Dims = TDims(128, 128);
      PrepareData();
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      auto input = Network->AddInput("input", GetValueType(), Dims);
      auto identity = Network->AddElementWise(input, input, EElementWiseOp::eProd);
      auto output = identity->GetOutput(0);
      output->SetName("output");
      Network->MarkOutput(output);
      EXPECT_EQ(GetValueType(), output->GetType());
      return Network;
    }

    void PrepareData()
    {
      Input.resize(Dims.TotalSize());
      std::iota(Input.begin(), Input.end(), 1);
      for (auto &v: Input)
      {
        v = v * M_PI / 4 / Input.size();
        v = sinf(v);
      }
    }

    void SetInputs(IInferencePtr, IInferenceSessionPtr session) override
    {
      session->SetInput(0, Input.size(), CastInput(Input));
    }

    TDims Dims;
    std::vector<float> Input;
  };
}

TEST_P(CalibrationTest, StepExecution)
{
  PrepareData();
  BuildNetwork();

  float amplitude = sinf(M_PI / 4);

  auto calibration = StartCalibration(*Network);
  auto result = calibration->Calibrate(*this);
  auto histInput = result->GetHistogram("input");
  auto histOutput = result->GetHistogram("output");
  auto totalInput = std::accumulate(histInput.begin(), histInput.end(), 0u);
  auto totalOutput = std::accumulate(histOutput.begin(), histOutput.end(), 0u);
  ASSERT_NEAR(amplitude, result->GetAmplitude("input"), GetTestTypePrecision());
  ASSERT_NEAR(amplitude * amplitude, result->GetAmplitude("output"), GetTestTypePrecision());
  ASSERT_EQ(Input.size(), totalInput);
  ASSERT_EQ(Input.size(), totalOutput);
  ASSERT_EQ(14, histInput[0]);
}

INSTANTIATE_TEST_SUITE_P(CalibrationTest, CalibrationTest,
        ::testing::ValuesIn(
        NTest::Combine(
                NTest::TTestSetup::Engines({"sis"}),
                NTest::TTestSetup::ValueTypes({"float", "half"}),
                NTest::TTestSetup::Compilers({"stage"})
        )
),
        NTest::TTestSetup::Description()
);
