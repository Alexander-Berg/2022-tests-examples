from util.generic.string cimport TString
from util.generic.vector cimport TVector

cdef extern from "list_balancing_modes.h":
    TVector[TString] ListBalancingModes()

def list_balancing_modes():
    return ListBalancingModes();
