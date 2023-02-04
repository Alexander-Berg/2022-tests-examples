# -*- coding: utf-8 -*-
import pytest
import balancer.test.util.stream.io.stream as stream
import balancer.test.util.proto.http2.framing.frames as frames
import balancer.test.util.proto.http2.framing.flags as flags
import balancer.test.util.proto.http2.framing.stream as frame_stream


DATA = 'Led Zeppelin'
PADDING = 'Pink Floyd'


@pytest.mark.parametrize(
    'frame',
    [
        frames.Data(length=None, flags=0, reserved=0, stream_id=0, data=DATA),
        frames.Data(length=None, flags=flags.PADDED, reserved=0, stream_id=0,
                    pad_length=None, data=DATA, padding=PADDING),
        frames.Settings(length=None, flags=0, reserved=0, data=[
            frames.Parameter(frames.Parameter.MAX_CONCURRENT_STREAMS, 5),
            frames.Parameter(frames.Parameter.MAX_FRAME_SIZE, 42),
        ]),
    ],
    ids=[
        'data',
        'padded_data',
        'settings',
    ]
)
def test_write_read_frame(frame):
    input_stream = stream.StringStream('')
    frame_stream.FrameStream(input_stream).write_frame(frame)
    result_frame = frame_stream.FrameStream(stream.StringStream(str(input_stream))).read_frame()

    assert frame == result_frame
