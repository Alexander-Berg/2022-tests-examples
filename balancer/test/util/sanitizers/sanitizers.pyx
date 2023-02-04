from libcpp cimport bool

cdef extern from "sanitizers_wrap.h" namespace "NSan":
    bool ASanIsOn();
    bool MSanIsOn();
    bool TSanIsOn();
    bool UBSanIsOn();
    bool SanIsOn();

def asan_enabled():
    return ASanIsOn();

def msan_enabled():
    return MSanIsOn();

def tsan_enabled():
    return TSanIsOn();

def ubsan_enabled():
    return UBSanIsOn();

def san_enabled():
    return SanIsOn();
