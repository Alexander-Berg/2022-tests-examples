from util.generic.ptr cimport THolder
from util.generic.string cimport TString, TStringBuf
from util.generic.vector cimport TVector
from util.system.types cimport ui32, ui64

from collections import namedtuple


ShardKey = namedtuple('ShardKey', ['project', 'cluster', 'service'])


cdef extern from "<solomon/libs/cpp/multi_shard/multi_shard.h>" namespace "NSolomon::NMultiShard" nogil:
    cdef cppclass IMultiShardContinuousChunkDecoder:
        void Decode(TStringBuf buf)

    cdef cppclass IMessageHandler:
        bint OnShardData(...)
        bint OnHeader(...)
        void OnError(...)
        void OnStreamEnd()

    cdef THolder[IMultiShardContinuousChunkDecoder] CreateMultiShardContinuousChunkDecoder(...)

cdef extern from "<solomon/agent/tests/multishard_msg_handler.h>" namespace "NSolomon" nogil:
    cdef cppclass TBasicMsgHandler:
        struct THeader:
            ui32 FormatVersion
            TString ContinuationToken

        struct TShardData:
            TString Project
            TString Cluster
            TString Service
            TString Data

        THeader Header
        TVector[TShardData] ShardsData

        TBasicMsgHandler()

        bint OnShardData(...)
        bint OnHeader(...)
        void OnError(...)
        void OnStreamEnd()


def decode_multishard_data(data):
    """
    Collects all decoded data into a python list as is and returns it
    """
    cdef TBasicMsgHandler mh = TBasicMsgHandler()

    cdef THolder[IMultiShardContinuousChunkDecoder] decoder = CreateMultiShardContinuousChunkDecoder(mh)
    cdef TStringBuf buf = TStringBuf(data, len(data))
    decoder.Get().Decode(buf)

    res = {}
    for shardData in mh.ShardsData:
        shard_key = ShardKey(
            project=shardData.Project,
            cluster=shardData.Cluster,
            service=shardData.Service,
        )
        res[shard_key] = shardData.Data,

    return (mh.Header.ContinuationToken, res,)
