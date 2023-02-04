#pragma once

#include <robot/library/plutonium/protos/doc_wad_lumps.pb.h>
#include <robot/library/plutonium/protos/global_wad_lump.pb.h>
#include <kernel/doom/wad/wad.h>
#include <util/generic/yexception.h>

namespace NRTYServer {
    inline NPlutonium::TDocWadLumps GetWadLumpsFromCstr(const TString& shard, ui32 docId, TStringBuf wadListSerialized) {
        NPlutonium::TDocWadLumps result;
        result.SetShard(shard);
        result.SetDocId(docId);
        const bool parsed = result.MutableLumpsList()->ParseFromArray(wadListSerialized.data(), wadListSerialized.size());
        Y_ENSURE(parsed);
        return result;
    }

    inline NPlutonium::TGlobalWadLump GetGlobalLumpFromCstr(const TString& shard, TStringBuf globalLumpSerialized) {
        NPlutonium::TGlobalWadLump result;
        result.SetShard(shard);
        const bool parsed = result.MutableLump()->ParseFromArray(globalLumpSerialized.data(), globalLumpSerialized.size());
        Y_ENSURE(parsed);
        return result;
    }
}
