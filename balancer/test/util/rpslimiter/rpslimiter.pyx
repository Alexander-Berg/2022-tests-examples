from util.generic.string cimport TString
from util.generic.vector cimport TVector
from util.generic.maybe cimport TMaybe
from util.datetime.base cimport TDuration, TInstant
import sys

cdef extern from "rpslimiter.h" namespace "NSrvKernel::NRpsLimiter" nogil:
    cdef cppclass TPeerQuota:
        TString Name
        TDuration Window
        double Rate

    cdef cppclass TPeerQuotas:
        TVector[TPeerQuota] Quotas
        TString Name
        TInstant Time

    TMaybe[TPeerQuotas] ParsePeerQuotasPy(TString)
    TString RenderPeerQuotas(const TPeerQuotas&)


class PeerQuota(object):
    def __init__(self, name, window, rate):
        self.name = name
        self.window = window
        self.rate = rate

    def __eq__(self, o):
        return (self.name, self.window, self.rate) == (o.name, o.window, o.rate)

    def __repr__(self):
        return str(self)

    def __str__(self):
        return "PeerQuota(name={}, window={}, rate={})".format(repr(self.name), repr(self.window), self.rate)


class PeerQuotas(object):
    def __init__(self, name, time, quotas):
        self.name = name
        self.time = time
        self.quotas = quotas

    def __eq__(self, o):
        return (self.name, self.time, self.quotas) == (o.name, o.time, o.quotas)

    def __repr__(self):
        return str(self)

    def __str__(self):
        return "PeerQuotas(name={}, time={}, quotas={})".format(repr(self.name), repr(self.time), self.quotas)


cdef TPeerQuota _pyobj_to_TPeerQuota(obj):
    assert isinstance(obj, PeerQuota)
    cdef TPeerQuota res
    res.Name = obj.name
    res.Window = TDuration.Parse(obj.window)
    res.Rate = obj.rate
    return res


cdef _TPeerQuota_to_pyobj(TPeerQuota pq):
    return PeerQuota(
        name=pq.Name,
        window=pq.Window.ToString(),
        rate=pq.Rate
    )


cdef TPeerQuotas _pyobj_to_TPeerQuotas(obj):
    assert isinstance(obj, PeerQuotas)
    cdef TPeerQuotas pq
    pq.Name = obj.name
    pq.Time = TInstant.MicroSeconds(TDuration.Parse(obj.time).MicroSeconds())
    for q in obj.quotas:
        pq.Quotas.push_back(_pyobj_to_TPeerQuota(q))
    return pq


cdef _TPeerQuotas_to_pyobj(TPeerQuotas pq):
    res = PeerQuotas(
        name=pq.Name,
        time=TDuration.MicroSeconds(pq.Time.MicroSeconds()).ToString(),
        quotas=[]
    )
    for q in pq.Quotas:
        res.quotas.append(_TPeerQuota_to_pyobj(q))
    return res


def render_peer_quotas(pq):
    return RenderPeerQuotas(_pyobj_to_TPeerQuotas(pq))


def parse_peer_quotas(pq):
    o = ParsePeerQuotasPy(pq)
    if o.Empty():
        return None
    return _TPeerQuotas_to_pyobj(o.GetRef())
