from smb.common.testing_utils import dt

from maps_adv.geosmb.doorman.proto import segments_pb2

__all__ = ["stat_el"]


def stat_el(dt_str: str, size: int = 0):
    return segments_pb2.SegmentStatisticsOnDate(
        timestamp=dt(dt_str, as_proto=True), size=size
    )
