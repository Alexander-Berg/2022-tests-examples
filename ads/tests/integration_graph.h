#pragma once

#include "sis/fwd/network_definition.h"
#include "sis/dims.h"
#include "sis/weights.h"
#include <vector>

namespace NSis {
  namespace NTest {
    struct TIntegrationGraph
    {
      struct TData
      {
        std::vector<float> Input;
        std::vector<float> FullWeights;

        std::vector<float> Output;
        std::vector<float> Test;

        std::vector<float> Fully1Output;
        std::vector<float> Fully2Output;
        std::vector<float> Fully1Test;
        std::vector<float> Fully2Test;

        std::vector<float> ActivationOutput;
        std::vector<float> ActivationTest;
      };

      TIntegrationGraph();

      void Build(INetworkDefinition& network, DataType valueType);
      void FillData(TData& data) const;

      TDims InputDims;
      TDims FullDims;
      TDims OutputDims;

      TWeights Full;
    };
  }
}
