#include <gtest/gtest.h>
#include "sis/tensorrt/compiler.h"
#include "sis/tensorrt/builder.h"
#include "sis/network_definition.h"
#include "sis/identity_layer.h"
#include "sis/inference.h"
#include "sis/inference_session.h"
#include <numeric>

namespace
{
  using namespace NSis;

  class TensorRTTest : public ::testing::Test
  {
  protected:
    INetworkDefinitionPtr BuildNetwork()
    {
      Builder = NTensorRT::CreateBuilder();
      Network = Builder->CreateNetwork();
      auto input = Network->AddInput("input", eFloat, TDims3(101, 11, 128));
      auto identity = Network->AddIdentity(input);
      auto output = identity->GetOutput(0);
      Network->MarkOutput(output);
      return Network;
    }

    IInferencePtr CompileNetwork(INetworkDefinitionPtr network)
    {
      NTensorRT::TCompilerConfig config = {};

      NTensorRT::TCompiler compiler;
      Inference = compiler.Compile(config, network);
      return Inference;
    }

    IBuilderPtr Builder;
    INetworkDefinitionPtr Network;
    IInferencePtr Inference;
  };
}

TEST_F(TensorRTTest, StepExecution)
{
  NSis::INetworkDefinitionPtr network = BuildNetwork();
  NSis::IInferencePtr inference = CompileNetwork(network);
  auto session = inference->StartSession();

  auto inputDims = inference->GetInput(0)->GetDims();
  auto outputDims = inference->GetOutput(0)->GetDims();

  ASSERT_EQ(inputDims, outputDims);

  std::vector<float> input(inputDims.InnerSize() * inputDims.OuterSize());
  std::iota(input.begin(), input.end(), 0);
  std::vector<float> output(input.size());
  session->SetInput(0, input.size(), input.data());
  session->Run();
  session->GetOutput(0, output.size(), output.data());
  session->Finalize();

  for (size_t i = 0; i != output.size(); ++i)
  {
    if (input[i] != output[i])
      ASSERT_EQ(input[i], output[i]);
  }
}
