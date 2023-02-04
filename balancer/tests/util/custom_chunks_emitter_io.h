#pragma once

#include <balancer/kernel/io/iobase.h>

#include <util/generic/string.h>

namespace NSrvKernel {

    class TCustomChunksEmitterIo : public IIoInput {
    public:
        enum class EChunksEmittingPolicy {
            Tcp,
            Random
        };

    public:
        explicit TCustomChunksEmitterIo(TString line, EChunksEmittingPolicy emittingPolicy = EChunksEmittingPolicy::Random);

        void Reset();

        bool Empty();

        void Prepend(TChunkList unparsed);

    private:
        TError DoRecv(TChunkList& lst, TInstant deadline) noexcept override;

        size_t CalculateNextChunkLength() const noexcept;

    private:
        static constexpr size_t TcpPacketLength = 1500;

        const TString Line_;
        TChunkList LineToEmit_;
        TChunkList CopyLineToEmit_;
        EChunksEmittingPolicy EmittingPolicy_;
    };

}
