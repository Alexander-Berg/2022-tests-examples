#include "kiwi_util.h"

namespace NSaas {
    void KiwiAddTuple(NKiwi::TKiwiObject* object, const ui32 timestamp, const TString& label, const TStringBuf& data) {
        NKwTupleInfo::TTupleInfo info;
        info.MutableStatus()->SetLabel(label);
        if (data.size()) {
            object->Add(/*attrId*/ 0, /*branchId*/ 0, timestamp, data, info);
        } else {
            object->AddNull(/*attrId*/ 0, /*branchId*/ 0, timestamp, info);
        }
    }
}
