#include "integration_graph.h"

#include "sis/network_definition.h"
#include "sis/dims.h"
#include "sis/activation_type.h"
#include "sis/matrix_multiply_layer.h"
#include "sis/fully_connected_layer.h"
#include "sis/constant_layer.h"
#include "sis/activation_layer.h"
#include "sis/layer_norm_layer.h"
#include "sis/element_wise_layer.h"
#include "sis/data/fill_identity.h"

#include <cmath>
#include <numeric>
#include "sis/sis_assert.h"

namespace NSis {
  namespace NTest {
    namespace {
      template <typename T>
      void FillOnes(std::vector<T>& data)
      {
        data.assign(data.size(), 1);
      }

      template <typename T>
      void FillInnerSin(TDims dims, std::vector<T>& data)
      {
        auto innerSize = dims.InnerSize();
        auto outerSize = dims.OuterSize();
        for (int o = 0; o != outerSize; ++o)
        {
          for (int i = 0; i != innerSize; ++i)
          {
            data[o * innerSize + i] = sinf(i);
          }
        }
      }
    }

    TIntegrationGraph::TIntegrationGraph()
    {
      InputDims = TDims2(32, 64);
      FullDims = TDims2(64, 64);
      OutputDims = TDims2(32, 64);
    }

    void TIntegrationGraph::Build(INetworkDefinition &network, DataType valueType)
    {
      auto input = network.AddInput("input", valueType, InputDims);

      auto b1 = network.AddConstant(FullDims, Full)->GetOutput(0);
      auto full1 = network.AddMatrixMultiply(input, b1)->GetOutput(0);
      full1->SetName("full1");
      network.MarkOutput(full1);

      auto full2 = network.AddFullyConnected(input, Full)->GetOutput(0);
      network.MarkOutput(full2);
      full2->SetName("full2");

      auto activation = network.AddActivation(full2, EActivationType::eRelu)->GetOutput(0);
      network.MarkOutput(activation);

      auto residual = network.AddElementWise(activation, input, EElementWiseOp::eSum)->GetOutput(0);
      auto norm = network.AddLayerNorm(residual);

      network.MarkOutput(norm->GetOutput(0));
    }

    void TIntegrationGraph::FillData(TData &data) const
    {
      data.Input.resize(InputDims.TotalSize());
      FillInnerSin(InputDims, data.Input);

      data.FullWeights.resize(FullDims.TotalSize());
      NData::FillIdentity(FullDims, data.FullWeights.data());

      data.Output.resize(OutputDims.TotalSize());
      data.Test.resize(OutputDims.TotalSize());

      data.Fully1Output.resize(InputDims.TotalSize());
      data.Fully2Output.resize(InputDims.TotalSize());
      data.Fully1Test = data.Input;
      data.Fully2Test = data.Input;

      data.ActivationOutput.resize(OutputDims.TotalSize());
      data.ActivationTest.resize(OutputDims.TotalSize());

      std::vector<float> residualValue(InputDims.InnerSize());
      for (int i = 0; i != InputDims.InnerSize(); ++i)
      {
        auto v = data.Input[i];
        auto act = v > 0 ? v : 0.f;
        auto residual = v + act;
        residualValue[i] = residual;
      }

      double sum = std::accumulate(residualValue.begin(), residualValue.end(), 0.);
      float mean = sum / InputDims.InnerSize();

      double var = 0;
      for (int i = 0; i != InputDims.InnerSize(); ++i)
      {
        auto residual = residualValue[i];
        var += (residual - mean) * (residual - mean);
      }

      var = 1.f / sqrt(1e-6 + var / InputDims.InnerSize());

      {
        auto innerSize = OutputDims.InnerSize();
        auto outerSize = OutputDims.OuterSize();
        for (int o = 0; o != outerSize; ++o)
        {
          for (int i = 0; i != innerSize; ++i)
          {
            auto index = o * innerSize + i;
            auto v = data.Input[index];
            auto act = v > 0 ? v : 0.f;
            data.ActivationTest[index] = act;

            auto residual = v + act;
            data.Test[index] = (residual - mean) * var;
          }
        }
      }
    }
  }
}
