#include <gtest/gtest.h>
#include "sis/graph/builder.h"
#include "sis/reflection.h"
#include "sis/network_definition.h"
#include "sis/identity_layer.h"
#include "sis/activation_layer.h"
#include "sis/matrix_layout.h"
#include "sis/matrix_multiply_layer.h"
#include "sis/fully_connected_layer.h"
#include "sis/scale_layer.h"
#include "sis/layer_norm_layer.h"
#include "sis/softmax_layer.h"
#include "sis/sparse_embedding_layer.h"
#include "sis/constant_layer.h"
#include "sis/gather_layer.h"
#include "sis/attention_layer.h"
#include "sis/concatenation_layer.h"
#include "sis/dims.h"
#include "sis/tensor.h"
#include "sis/inference.h"
#include "sis/inference_session.h"
#include "sis/slice_layer.h"
#include "sis/unary_layer.h"
#include "sis/element_wise_layer.h"
#include "sis/reduce_layer.h"
#include "sis/histogram_layer.h"

using namespace NSis;

namespace
{
  class NetworkReflectionTest : public ::testing::Test
  {
  protected:
    void SetUp() override
    {
      Builder = NSis::NGraph::CreateBuilder();
      Network = Builder->CreateNetwork();
    }

    NSis::INetworkDefinitionPtr BuildInputOutputNetwork()
    {
      auto floatInput = Network->AddInput("float_input", eFloat, TDims2(1, 1));
      auto halfInput = Network->AddInput("half_input", eHalf, TDims2(1, 2));
      auto int8Input = Network->AddInput("int8_input", eInt8, TDims2(1, 4));
      auto int32Input = Network->AddInput("int32_input", eInt32, TDims2(1, 8));

      Network->MarkOutput(int32Input);
      Network->MarkOutput(int8Input);
      Network->MarkOutput(halfInput);
      Network->MarkOutput(floatInput);

      return Network;
    }

    void BuildIdentityNetwork()
    {
      auto input = Network->AddInput("input", eFloat, TDims2(1, 1));
      auto layer = Network->AddIdentity(input);
      auto output = layer->GetOutput(0);
      output->SetName("output");
      Network->MarkOutput(layer->GetOutput(0));
    }

    void BuildActivationNetwork()
    {
      auto input = Network->AddInput("input", eFloat, TDims2(1, 1));
      auto relu = Network->AddActivation(input, NSis::EActivationType::eRelu);
      auto sigmoid = Network->AddActivation(input, NSis::EActivationType::eSigmoid);
      Network->MarkOutput(relu->GetOutput(0));
      Network->MarkOutput(sigmoid->GetOutput(0));
    }

    void BuildUnaryNetwork()
    {
      auto input = Network->AddInput("input", eFloat, TDims2(1, 1));
      auto layer = Network->AddUnary(input, EUnaryOp::eSqrt);
      Network->MarkOutput(layer->GetOutput(0));
    }

    void BuildReduceNetwork()
    {
      auto input = Network->AddInput("input", eFloat, TDims2(1, 32));
      auto layer = Network->AddReduce(input, EReduceOp::eSum, 3, false);
      Network->MarkOutput(layer->GetOutput(0));
    }

    void BuildMatrixMultiplyNetwork()
    {
      auto A = Network->AddInput("A", eFloat, TDims2(4, 4, 1));
      auto B = Network->AddInput("B", eFloat, TDims2(4, 1));
      auto layer = Network->AddMatrixMultiply(A, B);

      layer->LayoutA()
      .BatchCount(4)
      .BatchStride(4)
      .Cols(1)
      .Rows(4)
      .LeadingDimension(1);

      layer->LayoutB()
      .BatchCount(4)
      .BatchStride(0)
      .Cols(1)
      .Rows(4)
      .LeadingDimension(4);

      layer->Epilogue()
      .Alpha(0.1)
      .Beta(0.2);

      Network->MarkOutput(layer->GetOutput(0));
    }

    void BuildFullyConnectedNetwork()
    {
      auto input = Network->AddInput("input", eFloat, TDims2(1, 1));
      TWeights weights = {};
      weights.ElementsCount = 1;
      auto layer = Network->AddFullyConnected(input, weights);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildScaleNetwork()
    {
      auto input = Network->AddInput("input", eFloat, TDims2(1, 1));
      TWeights weights = {};
      weights.ElementsCount = 1;
      auto layer = Network->AddScale(input, weights, weights);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildElementWiseNetwork()
    {
      auto input = Network->AddInput("input", eFloat, TDims2(1, 1));
      auto layer = Network->AddElementWise(input, input, EElementWiseOp::eMin);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildLayerNormNetwork()
    {
      auto input = Network->AddInput("input", eFloat, TDims2(1, 1));
      auto layer = Network->AddLayerNorm(input);
      layer->SetEpsilon(2);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildSoftmaxNetwork()
    {
      auto input = Network->AddInput("input", eFloat, TDims2(1, 1));
      auto layer = Network->AddSoftmax(input);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildSparseEmbeddingLayer()
    {
      TDims indicesDims(16, 16);
      indicesDims.SetRaggedDimension(1);
      indicesDims.SetInnerDims(2);
      TDims embeddingsDim(16, 64);
      auto indices = Network->AddInput("indices", DataType::eInt32, indicesDims);

      auto layer = Network->AddSparseEmbedding(indices, embeddingsDim, TWeights());
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildConstantNetwork()
    {
      TWeights weights = {};
      weights.ElementsCount = 1;
      auto layer = Network->AddConstant(TDims(1, 1), weights);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildGatherNetwork()
    {
      TDims indicesDims(16, 16);
      indicesDims.SetRaggedDimension(1);
      indicesDims.SetInnerDims(2);
      TDims embeddingsDim(16, 64);

      auto indices = Network->AddInput("indices", DataType::eInt32, indicesDims);
      auto data = Network->AddInput("data", DataType::eFloat, embeddingsDim);

      auto layer = Network->AddGather(indices, data);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildAttentionNetwork()
    {
      TDims dims(4, 4, 4, 4);
      dims.SetRaggedDimension(1);
      auto x = Network->AddInput("x", DataType::eFloat, dims);
      auto layer = Network->AddAttention(x, x, x);
      layer->SetHeads(4);
      layer->SetQKScale(0.1);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildSliceNetwork()
    {
      TDims dims(5, 4, 3);
      dims.SetRaggedDimension(1);
      TDims start(0, 0, 0);
      TDims size(5, 1, 3);

      auto x = Network->AddInput("x", DataType::eFloat, dims);
      auto layer = Network->AddSlice(x, start, size);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildConcatenationNetwork()
    {
      TDims dims(3, 3, 4);
      dims.SetRaggedDimension(1);
      auto x = Network->AddInput("x", DataType::eFloat, dims);
      auto layer = Network->AddConcatenation(x, x, 1);
      auto output = layer->GetOutput(0);
      Network->MarkOutput(output);
    }

    void BuildReflection()
    {
      Reflection = Builder->CreateNetwork();
      Reflector = CreateReflection(*Reflection);
      Network->Describe(*Reflector);
    }

    IBuilderPtr Builder;
    INetworkDefinitionPtr Network;
    INetworkDefinitionPtr Reflection;
    IReflectionPtr Reflector;
  };
}

TEST_F(NetworkReflectionTest, Inputs)
{
  BuildInputOutputNetwork();
  BuildReflection();

  ASSERT_EQ(4, Reflection->GetInputsCount());
  auto floatInput = Reflection->GetInput(0);
  ASSERT_NE(nullptr, floatInput);
  ASSERT_STREQ("float_input", floatInput->GetName());
  ASSERT_EQ(eFloat, floatInput->GetType());
  ASSERT_EQ(TDims(1, 1), floatInput->GetDims());

  auto int32Input = Reflection->GetInput(3);
  ASSERT_NE(nullptr, int32Input);
  ASSERT_STREQ("int32_input", int32Input->GetName());
  ASSERT_EQ(eInt32, int32Input->GetType());
  ASSERT_EQ(TDims(1, 8), int32Input->GetDims());
}

TEST_F(NetworkReflectionTest, Outputs)
{
  BuildInputOutputNetwork();
  BuildReflection();

  ASSERT_EQ(4, Reflection->GetOutputsCount());
  auto int32Output = Reflection->GetOutput(0);
  ASSERT_NE(nullptr, int32Output);
}

TEST_F(NetworkReflectionTest, Identity)
{
  BuildIdentityNetwork();
  BuildReflection();
  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = Reflection->GetLayer(0);
  ASSERT_NE(nullptr, dynamic_cast<const IIdentityLayer*>(layer));
  ASSERT_STREQ("output", layer->GetOutput(0)->GetName());
}

TEST_F(NetworkReflectionTest, Activation)
{
  BuildActivationNetwork();
  BuildReflection();
  ASSERT_EQ(2, Reflection->GetLayersCount());
  auto relu = dynamic_cast<const IActivationLayer*>(Reflection->GetLayer(0));
  auto sigmoid = dynamic_cast<const IActivationLayer*>(Reflection->GetLayer(1));
  ASSERT_EQ(EActivationType::eRelu, relu->GetActivationType());
  ASSERT_EQ(EActivationType::eSigmoid, sigmoid->GetActivationType());
}

TEST_F(NetworkReflectionTest, Unary)
{
  BuildUnaryNetwork();
  BuildReflection();
  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const IUnaryLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
  ASSERT_EQ(EUnaryOp::eSqrt, layer->GetOperation());
}

TEST_F(NetworkReflectionTest, Reduce)
{
  BuildReduceNetwork();
  BuildReflection();
  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const IReduceLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
  ASSERT_EQ(EReduceOp::eSum, layer->GetOperation());
  ASSERT_EQ(3u, layer->GetReduceAxes());
  ASSERT_EQ(false, layer->GetKeepDimensions());
}

TEST_F(NetworkReflectionTest, MatrixMultiply)
{
  BuildMatrixMultiplyNetwork();
  BuildReflection();
  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const IMatrixMultiplyLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);

  const auto& layoutA = layer->LayoutA();
  ASSERT_EQ(4, layoutA.GetRows());
  ASSERT_EQ(1, layoutA.GetCols());
  ASSERT_EQ(1, layoutA.GetLeadingDimension());
  ASSERT_EQ(4, layoutA.GetBatchCount());
  ASSERT_EQ(4, layoutA.GetBatchStride());

  const auto& layoutB = layer->LayoutB();
  ASSERT_EQ(4, layoutB.GetRows());
  ASSERT_EQ(1, layoutB.GetCols());
  ASSERT_EQ(4, layoutB.GetLeadingDimension());
  ASSERT_EQ(4, layoutB.GetBatchCount());
  ASSERT_EQ(0, layoutB.GetBatchStride());

  ASSERT_NEAR(0.1, layer->Epilogue().GetAlpha(), 1e-5);
  ASSERT_NEAR(0.2, layer->Epilogue().GetBeta(), 1e-5);
}

TEST_F(NetworkReflectionTest, FullyConnected)
{
  BuildFullyConnectedNetwork();
  BuildReflection();

  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const IFullyConnectedLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
}

TEST_F(NetworkReflectionTest, Scale)
{
  BuildScaleNetwork();
  BuildReflection();

  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const IScaleLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
}

TEST_F(NetworkReflectionTest, ElementWise)
{
  BuildElementWiseNetwork();
  BuildReflection();

  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const IElementWiseLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
  ASSERT_EQ(layer->GetOperation(), EElementWiseOp::eMin);
}

TEST_F(NetworkReflectionTest, LayerNorm)
{
  BuildLayerNormNetwork();
  BuildReflection();

  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const ILayerNormLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
  ASSERT_EQ(2, layer->GetEpsilon());
}

TEST_F(NetworkReflectionTest, Softmax)
{
  BuildSoftmaxNetwork();
  BuildReflection();

  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const ISoftmaxLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
}

TEST_F(NetworkReflectionTest, SparseEmbedding)
{
  BuildSparseEmbeddingLayer();
  BuildReflection();

  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const ISparseEmbeddingLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
}

TEST_F(NetworkReflectionTest, Constant)
{
  BuildConstantNetwork();
  BuildReflection();

  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const IConstantLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
}

TEST_F(NetworkReflectionTest, Gather)
{
  BuildGatherNetwork();
  BuildReflection();

  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const IGatherLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
}

TEST_F(NetworkReflectionTest, Attention)
{
  BuildAttentionNetwork();
  BuildReflection();

  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const IAttentionLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
  ASSERT_EQ(4, layer->GetHeadsCount());
  ASSERT_NEAR(0.1, layer->GetQKScale(), 1e-5);
}

TEST_F(NetworkReflectionTest, Slice)
{
  BuildSliceNetwork();
  BuildReflection();

  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const ISliceLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
  ASSERT_EQ(TDims(5, 1, 3), layer->GetSize());
}

TEST_F(NetworkReflectionTest, Concatenation)
{
  BuildConcatenationNetwork();
  BuildReflection();

  ASSERT_EQ(1, Reflection->GetLayersCount());
  auto layer = dynamic_cast<const IConcatenationLayer*>(Reflection->GetLayer(0));
  ASSERT_NE(nullptr, layer);
}
