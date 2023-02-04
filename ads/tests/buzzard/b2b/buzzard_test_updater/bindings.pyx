from util.digest.multi cimport MultiHash
from util.generic.string cimport TString, TStringBuf
from util.generic.vector cimport TVector


cdef extern from "ads/bsyeti/tests/buzzard/b2b/buzzard_test_updater/b2b_lb2yt_requests.h":
    TVector[TVector[TString]] GetUidLogtypePairs(TString)
    void GrepRequests(TVector[TString], TString)


def multi_hash(TString profile_id, size_t salt):
    return MultiHash(profile_id, salt)


def grep_requests(TVector[TString] chunks, TString outputDir):
    return GrepRequests(chunks, outputDir)


def parse_qyt_protopack(TString record):
    return GetUidLogtypePairs(record)
