from ads.bsyeti.big_rt.lib.supplier.yt_sorted_table.fullstate.proto.caesar_service_pb2 import TCaesarService, TFullState
from ads.bsyeti.caesar.tests.ft.common.event import make_event


def make_service_event(profile_id, time, full_state=None):
    body = TCaesarService()
    if full_state:
        for name, data in full_state.items():
            body.FullStates[name].CopyFrom(data)
    return make_event(profile_id, time, body)


def make_full_state(exist_state=TFullState.EFullStateExist.FS_UNKNOWN, data=None):
    full_state = TFullState()
    full_state.ExistState = exist_state
    if data is not None:
        full_state.Data = data.SerializeToString()
    return full_state
