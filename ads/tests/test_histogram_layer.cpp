#include "sis/histogram_layer.h"
#include "sis/inference_session.h"
#include <sis/half.h>
#include "compiler_test_setup.h"
#include <gtest/gtest.h>
#include <numeric>
#include <random>

namespace
{
  using namespace NSis;

  struct HistogramLayerTest: ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
      Dims = TDims3(32, 128, 128);

      PrepareData();
      BuildNetwork();
      CompileTestNetwork();
      PerformInference();
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      auto input = Network->AddInput("input", GetValueType(), Dims);
      auto layer = Network->AddHistogram(input);

      layer->SetLower(Lower);
      layer->SetUpper(Upper);
      layer->SetBinsCount(BucketsCount);

      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
      EXPECT_EQ(eInt32, output->GetType());
      EXPECT_EQ(TDims(BucketsCount), output->GetDims());
      return Network;
    }

    void PrepareData()
    {
      Input.resize(Dims.TotalSize());
      std::mt19937 gen(34567);
      std::normal_distribution<float> dist((Lower + Upper) / 2, (Upper - Lower) / 2);
      for (auto& v: Input)
      {
        v = dist(gen);
        if (GetValueType() == NSis::eHalf)
          v = (float)(half_t)(v);
      }
      Output.resize(BucketsCount);

      Test = CalculateHistogram();
    }

    std::vector<float> CalculateHistogram()
    {
      std::vector<size_t> bins(BucketsCount);
      double amplitude = Upper - Lower;
      double bucketSize = amplitude / BucketsCount;

      for (auto v: Input)
      {
        if (v < Lower || v >= Upper)
          continue;
        int bin = ((double)v - Lower) / bucketSize;
        if (bin >= 0 && bin < BucketsCount)
          ++bins[bin];
      }

      std::vector<float> result;
      result.assign(bins.begin(), bins.end());
      return result;
    }

    void PerformInference()
    {
      auto session = Inference->StartSession();
      session->SetInput(0, Input.size(), CastInput(Input));
      session->Run();
      session->GetOutput(0, Output.size(), Output.data());
      session->Finalize();
      CastOutput(Output, NSis::eInt32);
    }

    const int BucketsCount = 100;
    float Lower = -1;
    float Upper = 1;
    TDims Dims;
    std::vector<float> Input;
    std::vector<float> Output;
    std::vector<float> Test;
  };
}

TEST_P(HistogramLayerTest, StepExecution)
{
  int precison = 1;
  for (size_t i = 0; i != Output.size(); ++i)
  {
    if (Test[i] != Output[i])
      ASSERT_NEAR(Test[i], Output[i], precison) << i;
  }
}

INSTANTIATE_TEST_SUITE_P(HistogramLayerTest, HistogramLayerTest,
        ::testing::ValuesIn(
        NTest::Combine(
                NTest::TTestSetup::Engines({"sis"}),
                NTest::TTestSetup::ValueTypes({"float", "half"}),
                NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"})
        )
),
        NTest::TTestSetup::Description()
);

