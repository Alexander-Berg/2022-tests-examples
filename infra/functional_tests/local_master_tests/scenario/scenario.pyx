from util.generic.string cimport TString


cdef extern from "infra/yp_service_discovery/functional_tests/local_master_tests/scenario/scenario.h":
    cdef void RunTest(const TString& address) nogil except +


def run_test(address):
    s = TString(bytes(address, 'utf-8'))
    RunTest(s)
