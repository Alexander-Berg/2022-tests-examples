from typing import Tuple

from maps_adv.geosmb.doorman.proto import segments_pb2


def parse_pb_segment_sizes(
    output_pb: segments_pb2.ListSegmentsOutput, segment_type: segments_pb2.SegmentType
) -> Tuple[int, int]:
    for segment_pb in output_pb.segments:
        if segment_pb.type == segment_type:
            return segment_pb.current_size, segment_pb.previous_size
