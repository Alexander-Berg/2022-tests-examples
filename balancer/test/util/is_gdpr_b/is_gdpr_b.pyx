from util.generic.maybe cimport TMaybe
from util.generic.string cimport TString
from libcpp.pair cimport pair

cdef extern from "is_gdpr_b.h":
    TMaybe[pair[int, TMaybe[int]]] ParseIsGdprB(TString val)

def parse_is_gdpr_b(b64):
    return ParseIsGdprB(b64)
