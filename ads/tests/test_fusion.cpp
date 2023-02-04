#include "sis/graph/builder.h"
#include "sis/cuda/compiler.h"
#include "sis/cuda/standalone/deployment.h"
#include "sis/network_definition.h"
#include "sis/identity_layer.h"
#include "sis/inference.h"
#include "sis/inference_session.h"
#include <gtest/gtest.h>
#include <numeric>

namespace
{
  using namespace NSis;
  class FusionTest : public ::testing::Test
  {
  protected:
    INetworkDefinitionPtr BuildNetwork()
    {
      Builder = NGraph::CreateBuilder();
      Network = Builder->CreateNetwork();
      auto input = Network->AddInput("input", eFloat, TDims3(11, 11, 256));
      auto identity = Network->AddIdentity(input);
      auto output = identity->GetOutput(0);
      Network->MarkOutput(output);
      return Network;
    }

    IInferencePtr CompileNetwork(INetworkDefinitionPtr network)
    {
      NCuda::TCompilerConfig config = {};
      config.Type = NSis::NCuda::eCompileFusion;
      config.DumpKernelSource = true;
      config.Packages = NProgram::NCuda::DeployPackages();
      NCuda::TCompiler compiler;
      Inference = compiler.Compile(config, network);
      return Inference;
    }

    IBuilderPtr Builder;
    INetworkDefinitionPtr Network;
    IInferencePtr Inference;
  };
}

TEST_F(FusionTest, IdentityLayer)
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

  ASSERT_EQ(input, output);
}
