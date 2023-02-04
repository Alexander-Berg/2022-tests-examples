#include "custom_chunks_emitter_io.h"

#include <balancer/kernel/memory/split.h>

namespace NSrvKernel {

    namespace  {

        TChunkList GetPrefix(TChunkList& lst, int32_t length) {
            TChunkList prefix;
            while (!lst.Empty() && length > 0) {
                prefix.PushBack(lst.PopFront());
                length -= prefix.Back()->Length();
            }
            return prefix;
        }

    }

    TCustomChunksEmitterIo::TCustomChunksEmitterIo(TString line, EChunksEmittingPolicy emittingPolicy)
        : Line_(std::move(line))
        , LineToEmit_()
        , CopyLineToEmit_()
        , EmittingPolicy_(emittingPolicy)
    {
        for (size_t i = 0; i < Line_.Size(); i += TcpPacketLength) {
            LineToEmit_.PushBack(NewChunkNonOwning(
                TStringBuf(Line_.Data() + i, Line_.Data() + i + std::min(TcpPacketLength, Line_.Size() - i))));
        }
        CopyLineToEmit_ = LineToEmit_.Copy();
    }

    void TCustomChunksEmitterIo::Reset() {
        LineToEmit_ = CopyLineToEmit_.Copy();
    }

    bool TCustomChunksEmitterIo::Empty() {
        return LineToEmit_.Empty();
    }

    void TCustomChunksEmitterIo::Prepend(TChunkList unparsed) {
        while (!unparsed.Empty()) {
            TChunkPtr chunk = unparsed.PopBack();
            LineToEmit_.PushFront(std::move(chunk));
        }
    }

    TError TCustomChunksEmitterIo::DoRecv(TChunkList& lst, TInstant /*deadline*/) noexcept {
        if (LineToEmit_.Empty()) {
            return {};
        }
        size_t nextChunkLength = CalculateNextChunkLength();
        lst = GetPrefix(LineToEmit_, nextChunkLength);
        return {};
    }

    size_t TCustomChunksEmitterIo::CalculateNextChunkLength() const noexcept {
        size_t chunkLength;
        switch (EmittingPolicy_) {
            case EChunksEmittingPolicy::Tcp:
                chunkLength = TcpPacketLength;
                break;
            case EChunksEmittingPolicy::Random:
                chunkLength = (static_cast<size_t>(*LineToEmit_.Front()->Data() + 256) & 31u) + 1;
                break;
        }
        return chunkLength;
    }

}
