from util.generic.hash cimport THashMap
from util.generic.string cimport TString


cdef extern from "ads/bsyeti/libs/experiments/user_ids/user_ids.h" namespace "NExperiments":
    cdef enum EUserIdType:
        pass

    THashMap[EUserIdType, TString] UserIdPrefixes


def get_prefixes():
    prefixes_map = UserIdPrefixes
    return [prefix for _type, prefix in prefixes_map]

prefixes = get_prefixes()

def from_storage_uid(uid):
    global prefixes

    for prefix in prefixes:
        if uid.startswith(prefix):
            return uid[len(prefix):]
