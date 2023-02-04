#include "sis/graph/builder.h"
#include "sis/network_definition.h"
#include <gtest/gtest.h>
#include "sis/identity_layer.h"
#include "sis/layer_norm_layer.h"
#include "sis/softmax_layer.h"
#include "sis/sparse_embedding_layer.h"
#include "sis/scale_layer.h"
#include "sis/constant_layer.h"
#include "sis/gather_layer.h"
#include "sis/element_wise_layer.h"
#include "sis/fully_connected_layer.h"
#include "sis/matrix_multiply_layer.h"
#include "sis/activation_layer.h"

namespace {
  using namespace NSis;

  struct RaggedPassthrough : ::testing::Test
  {
    RaggedPassthrough()
    {
      Builder = NGraph::CreateBuilder();
      Network = Builder->CreateNetwork();

      RaggedDims = GetRaggedDims();
    }

    TDims GetRaggedDims() const
    {
      TDims dims(16, 16, 16);
      dims.SetRaggedDimension(1);
      return dims;
    }

    ITensorPtr AddInput(const char* name="input")
    {
      return Network->AddInput(name, DataType::eFloat, RaggedDims);
    }

    TDims RaggedDims;
    IBuilderPtr Builder;
    INetworkDefinitionPtr Network;
  };

  TEST_F(RaggedPassthrough, Sanity)
  {
    ASSERT_TRUE(RaggedDims.IsRagged());
    ASSERT_EQ(1, RaggedDims.GetLastRaggedDimension());
  }

  TEST_F(RaggedPassthrough, IdentityLayer)
  {
    auto input = AddInput();
    auto layer = Network->AddIdentity(input);
    auto output = layer->GetOutput(0);
    ASSERT_TRUE(output->GetDims().IsRagged());
    ASSERT_EQ(RaggedDims.GetLastRaggedDimension(), output->GetDims().GetLastRaggedDimension());
  }

  TEST_F(RaggedPassthrough, ElementWiseLayer)
  {
    auto i0 = AddInput("i0");
    auto i1 = AddInput("i1");
    auto layer = Network->AddElementWise(i0, i1, EElementWiseOp::eSum);
    auto output = layer->GetOutput(0);
    ASSERT_TRUE(output->GetDims().IsRagged());
    ASSERT_EQ(RaggedDims.GetLastRaggedDimension(), output->GetDims().GetLastRaggedDimension());
  }

  TEST_F(RaggedPassthrough, LayerNormLayer)
  {
    auto i0 = AddInput("i0");
    auto layer = Network->AddLayerNorm(i0);
    auto output = layer->GetOutput(0);
    ASSERT_TRUE(output->GetDims().IsRagged());
    ASSERT_EQ(RaggedDims.GetLastRaggedDimension(), output->GetDims().GetLastRaggedDimension());
  }

  TEST_F(RaggedPassthrough, SoftMaxLayer)
  {
    auto i0 = AddInput("i0");
    auto layer = Network->AddSoftmax(i0);
    auto output = layer->GetOutput(0);
    ASSERT_TRUE(output->GetDims().IsRagged());
    ASSERT_EQ(RaggedDims.GetLastRaggedDimension(), output->GetDims().GetLastRaggedDimension());
  }

  TEST_F(RaggedPassthrough, SparseEmbeddingsLayer)
  {
    TDims indicesDims(16, 16);
    TDims embeddingsDim(16, 64);
    indicesDims.SetRaggedDimension(1);
    indicesDims.SetInnerDims(2);
    auto indices = Network->AddInput("indices", DataType::eInt32, indicesDims);

    auto layer = Network->AddSparseEmbedding(indices, embeddingsDim, TWeights());
    auto output = layer->GetOutput(0);
    ASSERT_FALSE(output->GetDims().IsRagged());
  }

  TEST_F(RaggedPassthrough, ScaleLayer)
  {
    auto input = AddInput();
    TWeights weights = {};
    weights.ElementsCount = RaggedDims.InnerSize();
    auto layer = Network->AddScale(input, weights, weights);
    auto output = layer->GetOutput(0);
    ASSERT_TRUE(output->GetDims().IsRagged());
    ASSERT_EQ(RaggedDims.GetLastRaggedDimension(), output->GetDims().GetLastRaggedDimension());
  }

  TEST_F(RaggedPassthrough, GatherLayer)
  {
    TDims indicesDims(16, 16);
    indicesDims.SetInnerDims(2);
    TDims embeddingsDim(16, 64);
    indicesDims.SetRaggedDimension(1);
    auto indices = Network->AddInput("indices", DataType::eInt32, indicesDims);
    auto embeddings = Network->AddConstant(embeddingsDim, {});

    auto layer = Network->AddGather(indices, embeddings->GetOutput(0));
    auto output = layer->GetOutput(0);
    ASSERT_TRUE(output->GetDims().IsRagged());
    ASSERT_EQ(indicesDims.GetLastRaggedDimension(), output->GetDims().GetLastRaggedDimension());
  }

  TEST_F(RaggedPassthrough, FullyConnectedLayer)
  {
    auto input = AddInput();
    TWeights weights = {};
    weights.ElementsCount = RaggedDims.InnerSize() * 17;
    auto layer = Network->AddFullyConnected(input, weights);
    auto output = layer->GetOutput(0);
    ASSERT_TRUE(output->GetDims().IsRagged());
    ASSERT_EQ(RaggedDims.GetLastRaggedDimension(), output->GetDims().GetLastRaggedDimension());
  }

  TEST_F(RaggedPassthrough, MatrixMultiplyLayerA)
  {
    TDims dimsA(10, 10, 16);
    dimsA.SetRaggedDimension(1);
    TDims dimsB(17, 16);
    auto A = Network->AddInput("A", NSis::eFloat, dimsA);
    auto B = Network->AddInput("B", NSis::eFloat, dimsB);
    auto layer = Network->AddMatrixMultiply(A, B);
    auto output = layer->GetOutput(0);
    ASSERT_TRUE(output->GetDims().IsRagged());
    ASSERT_EQ(RaggedDims.GetLastRaggedDimension(), output->GetDims().GetLastRaggedDimension());
  }

  TEST_F(RaggedPassthrough, ActivationLayer)
  {
    auto input = AddInput();
    auto layer = Network->AddActivation(input, NSis::EActivationType::eSigmoid);
    auto output = layer->GetOutput(0);
    ASSERT_TRUE(output->GetDims().IsRagged());
    ASSERT_EQ(RaggedDims.GetLastRaggedDimension(), output->GetDims().GetLastRaggedDimension());
  }
}
