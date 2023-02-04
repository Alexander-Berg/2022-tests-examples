from util.generic.string cimport TString, TStringBuf


cdef extern from "util/generic/buffer.h":
    cdef cppclass TBuffer:
        TString AsString(TString& x):
            pass


cdef extern from "util/generic/ptr.h":
    cdef cppclass TIntrusivePtr[ICodec]:
        ICodec* Get():
            pass


cdef extern from "library/cpp/codecs/codecs.h" namespace "NCodecs":
    cdef cppclass ICodec:
        @staticmethod
        TIntrusivePtr[ICodec] GetInstance(TStringBuf name):
            pass

        void Decode(TStringBuf a, TBuffer& b) except +:
            pass


def get_decompressed_object(TStringBuf cInfo, TStringBuf codecName):
    cdef TIntrusivePtr[ICodec] codec = ICodec.GetInstance(codecName);
    cdef TBuffer obj;
    cdef TString strProto;
    codec.Get().Decode(cInfo, obj);
    obj.AsString(strProto);
    return strProto;
