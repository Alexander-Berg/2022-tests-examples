#include "sis/attention_layer.h"
#include "sis/data/attention.h"
#include "compiler_test_setup.h"
#include "sis/data/sparse_embeddings.h"
#include "sis/constant_layer.h"
#include "sis/matrix_multiply_layer.h"
#include <gtest/gtest.h>

namespace {
  using namespace NSis;

  struct AttentionLayerTest : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
    void SetUp() override
    {
      InitSetup(GetParam());

      Config = {};//NData::TDebugAttentionConfig();
      //Config.MaxTokens = 32;
      Config.Algorithm = NSis::EAttentionAlgorithm::eRaggedV2;
      Data.reset(new NData::TAttentionData(Config));
      Data->PrepareData();

      if (IsFullstack())
      {
        BuildFullstackNetwork();
      }
      else
        BuildDirectNetwork();

      CompileTestNetwork();
    }

    bool IsInt8() const
    {
      std::string_view useInt8 = GetCustom("int8");
      return useInt8 == "int8";
    }

    bool IsFullstack() const
    {
      std::string_view fullstack = GetCustom("fullstack");
      return fullstack == "fullstack";
    }

    void BuildDirectNetwork()
    {
      auto testValueType = GetValueType();
      auto q = Network->AddInput("q", testValueType, Data->GetQDims());
      auto k = Network->AddInput("k", testValueType, Data->GetKDims());
      auto v = Network->AddInput("v", testValueType, Data->GetVDims());
      auto attention = Network->AddAttention(q, k, v);

      attention->SetAlgorithm(Config.Algorithm);

      attention->SetHeads(Config.Heads);
      auto output = attention->GetOutput(0);

      Network->MarkOutput(output);
      std::string_view useInt8 = GetCustom("int8");
      if (useInt8 == "int8")
        AugmentWithInt8();
    }
    void BuildFullstackNetwork()
    {
       auto testValueType = GetValueType();

      auto input = Network->AddInput("input", testValueType, Data->GetInputDims());

       auto makeXWeights = [this, testValueType](const std::vector<float>& values) {
        TWeights weights = {
                .Type=testValueType,
                .Data=CastInput(values),
                .ElementsCount=(int64_t)values.size()
        };
        return weights;
      };

      auto weightsQ = makeXWeights(Data->QW);
      auto weightsK = makeXWeights(Data->KW);
      auto weightsV = makeXWeights(Data->VW);

      auto constQ = Network->AddConstant(Data->GetQWeightsDims(), weightsQ)->GetOutput(0);
      auto constK = Network->AddConstant(Data->GetKWeightsDims(), weightsK)->GetOutput(0);
      auto constV = Network->AddConstant(Data->GetVWeightsDims(), weightsV)->GetOutput(0);
      constQ->SetName("q_weights");
      constK->SetName("k_weights");
      constV->SetName("v_weights");

      auto q = Network->AddMatrixMultiply(input, constQ)->GetOutput(0);
      auto k = Network->AddMatrixMultiply(input, constK)->GetOutput(0);

      auto vLayer = Config.Algorithm == EAttentionAlgorithm::eRaggedV2 ?
                Network->AddMatrixMultiply(constV, input) :
                Network->AddMatrixMultiply(input, constV);
      auto v = vLayer->GetOutput(0);

      q->SetName("attention_q");
      k->SetName("attention_k");
      v->SetName("attention_v");

      auto attention = Network->AddAttention(q, k, v);
      attention->SetAlgorithm(Config.Algorithm);
      attention->SetHeads(Config.Heads);

      auto output = attention->GetOutput(0);

      Network->MarkOutput(output);
      Network->MarkOutput(q);
      Network->MarkOutput(k);
      Network->MarkOutput(v);

      std::string_view useInt8 = GetCustom("int8");
      if (useInt8 == "int8")
        AugmentWithInt8();
    }

    void SetInputs(IInferencePtr inference, IInferenceSessionPtr session)
    {
      if (IsFullstack())
        SetFullstackInputs(inference, session);
      else
        SetDirectInputs(inference, session);
    }

    void SetDirectInputs(IInferencePtr, IInferenceSessionPtr session)
    {
      session->SetRaggedInput(0, Data->Offsets.size(), Data->Offsets.data(), Data->Q.size(), CastInput(Data->Q));
      session->SetRaggedInput(1, Data->Offsets.size(), Data->Offsets.data(), Data->K.size(), CastInput(Data->K));
      const auto& v = Config.Algorithm == NSis::EAttentionAlgorithm::eRagged ? Data->V : Data->TransposedV;
      session->SetRaggedInput(2, Data->Offsets.size(), Data->Offsets.data(), v.size(), CastInput(v));
    }

    void SetFullstackInputs(IInferencePtr, IInferenceSessionPtr session)
    {
      session->SetRaggedInput(0, Data->Offsets.size(), Data->Offsets.data(), Data->X.size(), CastInput(Data->X));
    }

    void PerformInference()
    {
      Output.resize(Data->Z.size());

      if (IsFullstack())
      {
        OutputQ.resize(Data->Q.size());
        OutputK.resize(Data->K.size());
        OutputV.resize(Data->V.size());
      }

      auto session = Inference->StartSession();

      SetInputs(Inference, session);
      session->Run();
      session->GetOutput(0, Output.size(), Output.data());
      if (IsFullstack())
      {
        session->GetOutput(1, OutputQ.size(), OutputQ.data());
        session->GetOutput(2, OutputK.size(), OutputK.data());
        session->GetOutput(3, OutputV.size(), OutputV.data());
      }
      session->Finalize();
      CastOutput(Output);
      CastOutput(OutputQ);
      CastOutput(OutputK);
      CastOutput(OutputV);
    }

    NData::TAttentionConfig Config;
    std::unique_ptr<NData::TAttentionData> Data;
    std::vector<float> OutputQ;
    std::vector<float> OutputK;
    std::vector<float> OutputV;
    std::vector<float> Output;
  };
}

TEST_P(AttentionLayerTest, Valid)
{
  Data->PrintStats();
  PerformInference();

  auto precision = GetTestTypePrecision();
  if (IsInt8())
    precision = 1.5e-2;

  if (IsFullstack())
  {
    size_t count = Data->Q.size();
    const auto& v = Config.Algorithm == NSis::EAttentionAlgorithm::eRaggedV2 ?
            Data->TransposedV : Data->V;
    for (size_t i = 0; i != count; ++i)
    {
      ASSERT_NEAR(OutputQ[i], Data->Q[i], precision) << "i " << i;
      ASSERT_NEAR(OutputK[i], Data->K[i], precision) << "i " << i;
      ASSERT_NEAR(OutputV[i], v[i], precision) << "i " << i;
    }
  }

  size_t count = Data->Z.size();

  auto resolveBatch = [this](int vectorIndex) {
    for (size_t i = 0; i != Data->Offsets.size(); ++i)
      if (vectorIndex < Data->Offsets[i])
        return std::make_pair((int)(i - 1), Data->Offsets[i - 1]);
    return std::make_pair(-1, -1);
  };

  for (size_t i = 0; i != count; ++i)
  {
    auto vectorIndex = i / Config.InnerDim / Config.Heads;
    auto batch = resolveBatch(i / Config.InnerDim / Config.Heads);
    int batchOffset = i - batch.second * Config.InnerDim * Config.Heads;
    int head = (batchOffset / Config.InnerDim) % Config.Heads;
    int token = batchOffset / Config.InnerDim / Config.Heads;
    ASSERT_NEAR(Data->Z[i], Output[i], precision)
      << " i " << i << "\n"
      << " vectorIndex " << vectorIndex << "\n"
      << " batch " << batch.first << "\n"
      << " head " << head << "\n"
      << " token " << token
      ;
  }
}

INSTANTIATE_TEST_SUITE_P(TestAttention, AttentionLayerTest,
                        ::testing::ValuesIn(
                                NTest::Combine(
                                        NTest::TTestSetup::Engines({"sis"}),
                                        NTest::TTestSetup::Compilers({"stage"}),
                                        NTest::TTestSetup::ValueTypes({/*"float", */"half"}),
                                        NTest::TTestSetup::Gemms({"cublas_lt"}),
                                        NTest::TTestSetup::Custom(
                                                "fullstack",
                                                {
                                                        "fullstack",
                                                        "direct"
                                                }),
                                        NTest::TTestSetup::Custom(
                                                         "int8",
                                                         {
                                                           "int8",
                                                           "regular"
                                                         })
                                )
                        ),
                        NTest::TTestSetup::Description()
);
