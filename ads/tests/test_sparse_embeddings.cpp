#include "sis/sparse_embedding_layer.h"
#include "sis/cuda/kernel/kernel_block_dim.h"
#include "compiler_test_setup.h"
#include "sis/data/sparse_embeddings.h"
#include <gtest/gtest.h>
#include <cmath>

namespace {
  using namespace NSis;
  struct TestSparseEmbeddings : ::testing::TestWithParam<NTest::TTestSetup>, NTest::TBaseTestPipeline
  {
    static const auto WithPadding = NData::TTokensGeneratorConfig::WithPadding;
    static const auto NoPadding = NData::TTokensGeneratorConfig::NoPadding;

  protected:
    void SetUp() override
    {
      InitSetup(GetParam());
    }

    void BuildNetwork(TDims embeddingsDim)
    {
      EmbeddingsConfig.EmbeddingDims = embeddingsDim;
      TokensConfig.UsePadding = UsePadding() ? WithPadding : NoPadding;

      Tokens.reset(new NData::TTokensGenerator(TokensConfig));
      Data.reset(new NData::TSparseEmbeddings(*Tokens, EmbeddingsConfig));

      TDims indicesDims(TokensConfig.BatchSize, TokensConfig.MaxTokens);
      indicesDims.SetInnerDims(2);
      if (!UsePadding())
        indicesDims.SetRaggedDimension(1);
      auto indices = Network->AddInput("indices", eInt32, indicesDims);

      auto embeddingsWeightsDim = Data->GetEmbeddingsWeightsDims();
      auto embeddingsWeights = Data->GetEmbeddingsWeights(GetValueType());

      auto layer = Network->AddSparseEmbedding(indices, embeddingsWeightsDim, embeddingsWeights);
      auto D = layer->GetOutput(0);
      TDims expectedDim(TokensConfig.BatchSize, EmbeddingsConfig.EmbeddingDims);
      if (GetEngineType() != NSis::NTest::EEngine::eTensorRT)
        ASSERT_EQ(D->GetDims(), expectedDim)
                            << "Tensor shape mismatch: " << D->GetDims().Format().c_str() << " != " << expectedDim.Format().c_str();
      Network->MarkOutput(D);
    }

    void PerformInference()
    {
      const auto& tokensOffsets = Tokens->GetOffsets();
      const auto& tokensIndices = Tokens->GetIndices();
      auto session = Inference->StartSession();
      if (UsePadding())
        session->SetInput(0, tokensIndices.size(), tokensIndices.data());
      else
        session->SetRaggedInput(0, tokensOffsets.size(), tokensOffsets.data(), tokensIndices.size(),
                                tokensIndices.data());
      session->Run();
      session->GetOutput(0, Data->Output.size(), Data->Output.data());
      session->Finalize();
      CastOutput(Data->Output);
    }

    bool UsePadding() const
    {
      return GetEngineType() == NSis::NTest::EEngine::eTensorRT;
    }

    NData::TTokensGeneratorConfig TokensConfig;
    NData::TSparseEmbeddingsConfig EmbeddingsConfig;
    std::unique_ptr<NData::ITokensGenerator> Tokens;
    std::unique_ptr<NData::TSparseEmbeddings> Data;
  };

  TEST_P(TestSparseEmbeddings, Dim1D)
  {
    TDims dims(SIS_KERNEL_BLOCK_DIM * 6);
    BuildNetwork(dims);
    CompileTestNetwork();
    PerformInference();

    auto precision = GetTestTypePrecision();
    for (size_t i = 0; i != Data->Test.size(); ++i)
    {
      if (Data->Test[i] != Data->Output[i])
      {
        ASSERT_NEAR(Data->Test[i], Data->Output[i], precision)
        << " i " << i << "\n"
        << " vectorIndex " << i / dims.TotalSize()
        ;
      }
    }
  }

  TEST_P(TestSparseEmbeddings, Dim2D)
  {
    BuildNetwork(TDims(8, 8));
    CompileTestNetwork();
    PerformInference();

    auto precision = GetTestTypePrecision();
    for (size_t i = 0; i != Data->Test.size(); ++i)
    {
      if (Data->Test[i] != Data->Output[i])
      {
        ASSERT_NEAR(Data->Test[i], Data->Output[i], fabs(Data->Test[i]) * precision) << "at i " << i;
      }
    }
  }
}

INSTANTIATE_TEST_SUITE_P(SparseEmbeddingsLayer, TestSparseEmbeddings,
                        ::testing::ValuesIn(
                                NTest::Combine(
                                        NTest::TTestSetup::Engines({"sis", "tensor_rt"}),
                                        NTest::TTestSetup::ValueTypes({"float", "half"}),
                                        NTest::TTestSetup::Compilers({"stage", "fusion", "kernel barrier"})
                                )
                        ),
                        NTest::TTestSetup::Description()
);