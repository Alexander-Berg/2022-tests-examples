# -*- coding: utf-8 -*-
import balancer.test.util.stream.io.stream as stream
import balancer.test.util.proto.http2.connection as mod_conn
import balancer.test.util.proto.http2.framing.stream as frame_stream
import balancer.test.util.proto.http2.message as mod_msg


def test_conn():
    req = mod_msg.HTTP2Request(
        mod_msg.HTTP2RequestLine('GET', 'https', 'www.google.ru', '/sample/path'), [], None,
    ).to_raw_request()
    input_stream = stream.StringStream('')
    conn = mod_conn.ClientConnection(input_stream)
    conn.write_message(req, 1)
    frame_stream.FrameStream(stream.StringStream(str(input_stream))).read_frame()
