#include "sis/attention_layer.h"
#include "sis/matrix_multiply_layer.h"
#include "sis/slice_layer.h"
#include "sis/softmax_layer.h"
#include "sis/data/attention.h"
#include "compiler_test_setup.h"
#include "sis/transpose_layer.h"
#include <gtest/gtest.h>
#include <iostream>
#include "sis/attention_composition.h"

namespace {
  using namespace NSis;

  struct DenseAttentionLayerTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
    void SetUp() override
    {
      InitSetup(GetParam());
      Config = {};
      Data.reset(new NData::TAttentionData(Config));
      Data->PrepareData();
      BuildNetwork();
      CompileTestNetwork();
    }

    INetworkDefinitionPtr BuildNetwork()
    {
      auto testValueType = GetValueType();
      auto dims = Data->GetDenseDims();
      auto q = Network->AddInput("q", testValueType, dims);
      auto k = Network->AddInput("k", testValueType, dims);
      auto v = Network->AddInput("v", testValueType, dims);
      auto lens = Network->AddInput("lens", eInt32, TDims(Config.BatchSize * Config.Heads * Config.MaxTokens, 1));

      TAttentionComposition composition(*Network);
      auto output = composition.Build(q, k, v, lens);
      Network->MarkOutput(output);
      return Network;
    }

    void PerformInference()
    {
      Output.resize(Data->GetDims().TotalSize());

      auto session = Inference->StartSession();

      session->SetInput(0, Data->QDense.size(), CastInput(Data->QDense));
      session->SetInput(1, Data->KDense.size(), CastInput(Data->KDense));
      session->SetInput(2, Data->VDense.size(), CastInput(Data->VDense));
      session->SetInput(3, Data->Lens.size(), Data->Lens.data());
      session->Run();
      session->GetOutput(0, Output.size(), Output.data());

      session->Finalize();
      CastOutput(Output);
    }

    NData::TAttentionConfig Config;
    std::unique_ptr<NData::TAttentionData> Data;
    std::vector<float> Output;
  };
}

TEST_P(DenseAttentionLayerTest, Valid)
{
  Data->PrintStats();
  PerformInference();
  size_t count = Data->Z.size();

  auto resolveBatch = [this](int vectorIndex) {
    for (size_t i = 0; i != Data->Offsets.size(); ++i)
      if (vectorIndex < Data->Offsets[i])
        return std::make_pair((int)(i - 1), Data->Offsets[i - 1]);
    return std::make_pair(-1, -1);
  };

  for (size_t i = 0; i != count; ++i)
  {
    auto batch = resolveBatch(i / Config.InnerDim / Config.Heads);
    int batchOffset = i - batch.second * Config.InnerDim * Config.Heads;
    int head = (batchOffset / Config.InnerDim) % Config.Heads;
    int token = batchOffset / Config.InnerDim / Config.Heads;
    int idx = batch.first * Config.Heads * Config.InnerDim * Config.MaxTokens + token * Config.InnerDim * Config.Heads +
              head * Config.InnerDim + i % Config.InnerDim;
    ASSERT_NEAR(Data->Z[i], Output[idx], 1e-3);
  }
}

INSTANTIATE_TEST_SUITE_P(TestDenseAttention, DenseAttentionLayerTest,
                        ::testing::ValuesIn(
                                NTest::Combine(
                                        NTest::TTestSetup::Engines({"sis"}),
                                        NTest::TTestSetup::Compilers({"stage", "fusion", "kernel_barrier"}),
                                        NTest::TTestSetup::Gemms({"cublas_lt"}),
                                        NTest::TTestSetup::ValueTypes({"float", "half"})
                                )
                        ),
                        NTest::TTestSetup::Description()
);
