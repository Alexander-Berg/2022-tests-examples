import json

from libcpp cimport bool
from util.generic.string cimport TStringBuf


cdef extern from "infra/yp_dns_api/tests/helpers/replicator_wrapper.h" namespace "NInfra::NYpDnsApi::NReplicator::NTests":
    cdef cppclass TReplicator:
        TReplicator(TStringBuf) except +
        bool Sync()


cdef class Replicator:
    cdef TReplicator* _replicator

    def __cinit__(self, config):
        json_config = bytes(json.dumps(config), 'utf8')
        self._replicator = new TReplicator(json_config)

    def __dealloc__(self):
        del self._replicator

    def sync(self):
        return self._replicator.Sync()
