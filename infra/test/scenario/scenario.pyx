from util.generic.string cimport TString


cdef extern from "infra/libs/yp_replica/test/scenario/scenario.h":
    cdef void RunTest(const TString& address) nogil except +


def run_test(address):
    s = TString(bytes(address, 'utf-8'))
    RunTest(s)
