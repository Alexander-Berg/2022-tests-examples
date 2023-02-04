#pragma once

#include <yweb/robot/kiwi/tuplelib/lib/object.h>

namespace NSaas {
    void KiwiAddTuple(NKiwi::TKiwiObject* object, const ui32 timestamp, const TString& label, const TStringBuf& data);

    template <class T>
    void KiwiAddNumericTuple(NKiwi::TKiwiObject* object, const ui32 timestamp, const TString& label, const T& data) {
        KiwiAddTuple(object, timestamp, label, TStringBuf(reinterpret_cast<const char*>(&data), sizeof(T)));
    }
}
