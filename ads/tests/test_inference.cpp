#include <gtest/gtest.h>
#include "sis/graph/builder.h"
#include "sis/cuda/compiler.h"
#include "sis/dims.h"
#include "sis/network_definition.h"
#include "sis/tensor.h"
#include "sis/inference.h"
#include "sis/inference_session.h"

using namespace NSis;

namespace
{
  class InferenceTest : public ::testing::Test
  {
  protected:
    void SetUp() override
    {
      Builder = NSis::NGraph::CreateBuilder();
    }

    NSis::INetworkDefinitionPtr MakeInOutNetwork()
    {
      Network = Builder->CreateNetwork();
      auto input = Network->AddInput("input", eFloat, TDims2(1, 1));
      Network->MarkOutput(input);
      return Network;
    }

    NSis::IInferencePtr CompileInOutNetwork()
    {
      auto network = MakeInOutNetwork();
      NCuda::TCompilerConfig config = {};
      NCuda::TCompiler compiler;
      return compiler.Compile(config, network);
    }

    IBuilderPtr Builder;
    INetworkDefinitionPtr Network;
  };
}

TEST_F(InferenceTest, DontAllowUnnamedOutput)
{
}

TEST_F(InferenceTest, Compiles)
{
  auto network = MakeInOutNetwork();
  NCuda::TCompilerConfig config = {};
  NCuda::TCompiler compiler;
  auto inference = compiler.Compile(config, network);
  EXPECT_NE(inference, nullptr);
}

TEST_F(InferenceTest, RememberInOuts)
{
  auto inference = CompileInOutNetwork();

  EXPECT_EQ(1u, inference->GetInputsCount());
  auto input = inference->GetInput(0);
  EXPECT_STREQ("input", input->GetName());

  EXPECT_EQ(1u, inference->GetOutputsCount());
  auto output = inference->GetOutput(0);
  EXPECT_STREQ("input", output->GetName());
}

TEST_F(InferenceTest, PassthroughData)
{
  auto inference = CompileInOutNetwork();
  auto session = inference->StartSession();
  float data = 1.1f;
  session->SetInput(0, 1, &data);
  session->Run();
  float result = 0;
  session->GetOutput(0, 1, &result);
  session->Finalize();
  ASSERT_EQ(data, result);
}

TEST_F(InferenceTest, ScheduleGetBeforeRun)
{
  auto inference = CompileInOutNetwork();
  auto session = inference->StartSession();
  float data = 1.1f;
  float result = 0;

  session->SetInput(0, 1, &data);
  session->GetOutput(0, 1, &result);
  session->Run();
  session->Finalize();
  ASSERT_EQ(data, result);
}
