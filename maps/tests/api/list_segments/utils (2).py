from maps_adv.geosmb.promoter.proto import segments_pb2


def parse_pb_segment_size(
    output_pb: segments_pb2.ListSegmentsOutput, segment_type: segments_pb2.SegmentType
) -> int:
    for segment_pb in output_pb.segments:
        if segment_pb.type == segment_type:
            return segment_pb.size
