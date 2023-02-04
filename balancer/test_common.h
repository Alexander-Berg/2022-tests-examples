#pragma once

#include <balancer/kernel/io/iobase.h>
#include <balancer/kernel/memory/chunks.h>

namespace NSrvKernel {
    template <class F>
    class TOutputMock : public IIoOutput {
    public:
        TOutputMock(F f)
            : Action_(std::move(f)) {}

    private:
        TError DoSend(TChunkList lst, TInstant) noexcept override {
            return Action_(std::move(lst));
        }

    private:
        F Action_;
    };

    template <class F>
    TOutputMock<F> MakeOutputMock(F f) {
        return TOutputMock<F>(std::move(f));
    }

    template <class F>
    class TInputMock : public IIoInput {
    public:
        TInputMock(F f)
            : Action_(std::move(f)) {}

    private:
        TError DoRecv(TChunkList& lst, TInstant deadline = TInstant::Max()) noexcept override {
            return Action_(lst, deadline);
        }

    private:
        F Action_;
    };

    template <class F>
    TInputMock<F> MakeInputMock(F f) {
        return TInputMock<F>(std::move(f));
    }
}
