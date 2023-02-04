#!/usr/bin/env python
# coding=utf-8

import hashlib
import base64
import pytest
import socket
import time

from configs import HTTPBalancerConfig, WSAdditionalConfig, WSAdditionalAntirobotConfig, CompressorConfig
from balancer.test.util.proto.handler.server.http import HTTPConfig, HTTPServerHandler
from balancer.test.util.predef.handler.server.http import SimpleConfig, SimpleDelayedConfig
from balancer.test.util.proto.ws.stream import WebSocketFrame, WebSocketOpcode, WSReaderException
from balancer.test.util import asserts
from balancer.test.util.predef import http
from multiprocessing import Process
import balancer.test.util.stream.io.stream as s


def check_ascii(string):
    try:
        string.decode('ascii')
    except UnicodeDecodeError:
        return False
    return True


def safe_keylen(string):
    if string is None:
        return 0
    dec = ""
    try:
        dec = base64.b64decode(string)
    except TypeError:
        return 0

    return len(dec)


def safe_int(string):
    try:
        return int(string)
    except ValueError:
        return -1


class WSResponseHandler(HTTPServerHandler):
    def _send_bad_request(self, stream, version, error):
        stream.write_line("{0} 400 Bad request".format(version))
        stream.write_header("Content-Length", len(error))
        stream.write_header("Content-Type", "text/html; charset=UTF-8")
        stream.end_headers()
        stream.write_line(error)

    def _send_redirect_request(self, stream, version, url):
        stream.write_line("{0} 301 Moved Permanently".format(version))
        stream.write_header("Connection", "close")
        stream.write_header("Location", url)
        stream.write_header("Content-Length", 0)  # don't try to read response as chunked data
        stream.end_headers()

    def handle_request(self, stream):
        if not self._ws_status():
            # Connection initialization
            raw_request = stream.read_request()
            self.append_request(raw_request)

            request_line = raw_request.request_line
            request_headers = raw_request.headers

            if request_line.version != "HTTP/1.1":
                self._send_bad_request(stream, request_line.version, "Can upgrade only from HTTP/1.1")
                return
            if request_line.method != "GET":
                self._send_bad_request(stream, request_line.version, "Can upgrade only from GET method")
                return

            if request_line.path == "/redirect":
                self._send_redirect_request(stream, request_line.version, "/redirect_here")
                return

            host = request_headers.get_one("Host")
            if host is None:
                self._send_bad_request(stream, request_line.version, "Can upgrade only with Host header")
                return

            connection = request_headers.get_one("Connection")
            if connection is None or connection not in ("Upgrade", "upgrade"):
                self._send_bad_request(stream, request_line.version, "Can upgrade only with Connection: Upgrade header")
                return

            upgrade = request_headers.get_one("Upgrade")
            if upgrade is None or upgrade not in ("Websocket", "websocket", "h2c", "TLS/1.0"):
                self._send_bad_request(stream, request_line.version, "Can upgrade only with Upgrade: websocket/h2c/TLS header")
                return

            websocket_key = request_headers.get_one("Sec-WebSocket-Key")
            if websocket_key is None or not check_ascii(websocket_key) or safe_keylen(websocket_key) != 16:  # Symbols are base64-encoded, should be ASCII
                self._send_bad_request(stream, request_line.version, "Can upgrade only with Set-WebSocket-Key: ASCII")
                return

            websocket_version = request_headers.get_one("Sec-WebSocket-Version")
            if websocket_version is None or safe_int(websocket_version) != 13:
                self._send_bad_request(stream, request_line.version, "Can upgrade only with Set-WebSocket-Version: 13")
                return

            websocket_key += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
            key_hash = hashlib.sha1(websocket_key).digest()
            accept_key = base64.b64encode(key_hash)

            # Response with the most secure solted hash
            stream.write_line("HTTP/1.1 101 Switching Protocols")
            stream.write_header('Connection', 'Upgrade')
            stream.write_header('Upgrade', "websocket")
            if self.config.have_content_length:
                stream.write_header('Content-Length', 0)
            stream.write_header("Sec-WebSocket-Accept", accept_key)
            stream.end_headers()

            self._ws_on()
        else:

            raw_request = stream.read_request()
            self.append_request(raw_request)

            close = False
            raw_response = None
            if self.config.close_after is not None and self.config.close_after == self.config.response_counter:
                close = True

            if self.config.response:
                raw_response = self.config.response
                if isinstance(self.config.response, list):
                    raw_response = self.config.response[self.config.response_counter % len(self.config.response)]
                else:
                    raw_response = self.config.response
            elif raw_request.opcode == WebSocketOpcode.WS_PING_FRAME:
                raw_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_PONG_FRAME, mask=0, payload=raw_request.payload)
            elif raw_request.opcode == WebSocketOpcode.WS_CONN_CLOSE_FRAME:
                raw_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_CONN_CLOSE_FRAME, mask=0, payload="\x03\xe8")
                close = True
            else:
                raw_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_CONN_CLOSE_FRAME, mask=0, payload="\x03\xe8")
            stream.write_response(raw_response)

            self.config.response_counter += 1
            if close:
                self.force_close()


class WSResponseConfig(HTTPConfig):
    HANDLER_TYPE = WSResponseHandler

    def __init__(self, response=None, close_after=None, have_content_length=False):
        super(WSResponseConfig, self).__init__()
        self.response = response
        self.response_counter = 0
        self.close_after = close_after
        self.websocket_mode = False
        self.have_content_length = have_content_length


def test_switching_http_10(ctx):
    """
    Для HTTP/1.0 websocket не определен
    """
    ctx.start_backend(WSResponseConfig(), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_line('GET / HTTP/1.0')
        stream.end_headers()
        response = stream.read_response()
        asserts.status(response, 400)


def test_switching_http_host(ctx):
    """
    Для запросов без HOST мы должны отвалиться. Origin для не браузеров можно не указывать
    """
    ctx.start_backend(WSResponseConfig(), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_line('GET / HTTP/1.1')
        stream.end_headers()
        response = stream.read_response()
        asserts.status(response, 400)


def test_switching_post(ctx):
    """
    Для не GET запросов websocket хэндшей должен заканчиваться 400
    """
    ctx.start_backend(WSResponseConfig(), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_line('POST / HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.end_headers()
        response = stream.read_response()
        asserts.status(response, 400)


def test_switching_connection(ctx):
    """
    Connection так же должен присутстовать
    """
    ctx.start_backend(WSResponseConfig(), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_line('GET / HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 400)


def test_switching_upgrade(ctx):
    """
    Проверяем Upgrade со значением не TLS/1.0, h2c, websocket
    """
    ctx.start_backend(WSResponseConfig(), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream.write_line('GET / HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "smthng")
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 400)


def test_switching_websocket_key(ctx):
    """
    Без Sec-WebSocket-Key тоже завершаем хендшейк
    """
    ctx.start_backend(WSResponseConfig(), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET / HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', '')
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 400)

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET / HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'A'*23)
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 400)

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET / HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', "\0x00\0x01\0x02")
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 400)


def test_switching_websocket_version(ctx):
    """
    Sec-WebSocket-Version версии 13. Все сабпротоколы мы не проверяем.
    """
    ctx.start_backend(WSResponseConfig(), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET / HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 'a')
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 400)

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET / HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 12)  # Reserved by IANA
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 400)


def test_switching_upgrade_redirect(ctx):
    """
    Обработка редиректа на попытку соединения. Будем считать, что если 3xx обрабатывается,
    то и авторизация тоже работает.
    """
    ctx.start_backend(WSResponseConfig(), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET /redirect HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 13)
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 301)
        asserts.header_value(response, "Location", "/redirect_here")


@pytest.mark.parametrize("explicit_connection_header", [True, False])
def test_switching_websocket_switching_done(ctx, explicit_connection_header):
    """
    Пробуем самый секьюрный способ аутентификации
    """
    ctx.start_backend(WSResponseConfig(), name="backend")
    ctx.start_balancer(HTTPBalancerConfig(
        allow_connection_upgrade_without_connection_header=not explicit_connection_header,
    ))

    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET /ws HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        if explicit_connection_header:
            stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 13)
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 101)
        asserts.header_value(response, "Sec-WebSocket-Accept", "SUeCT5AIyDftvfPRgVEj38G/wsA=")


@pytest.mark.parametrize('have_content_length', [False, True])
def test_websocket_ping(ctx, have_content_length):
    """
    Сначала отправляем пустой пинг и ждем ответа, затем с Application data.
    """
    ctx.start_backend(WSResponseConfig(have_content_length=have_content_length), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET /ping HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 13)
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 101)
        asserts.header_value(response, "Sec-WebSocket-Accept", "SUeCT5AIyDftvfPRgVEj38G/wsA=")

        stream = stream.switch_ws()

        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_PING_FRAME, mask=1, payload="")
        stream.write_request(raw_request)
        response = stream.read_response()
        assert response.opcode == WebSocketOpcode.WS_PONG_FRAME

        payload = "Hello world"
        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_PING_FRAME, mask=1, payload=payload)
        stream.write_request(raw_request)
        response = stream.read_response()
        assert response.opcode == WebSocketOpcode.WS_PONG_FRAME
        assert response.payload == payload


@pytest.mark.parametrize('compressor', [True, False])
def test_websocket_text_message(ctx, compressor):
    """
    Отправляем текстовое сообщение и принимаем текстовый ответ
    """
    world_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=0, payload="world")
    yes_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=0, payload="yes")
    no_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=0, payload="no")

    ctx.start_backend(WSResponseConfig(response=[world_response, yes_response, no_response]), name="backend")
    if compressor:
        ctx.start_balancer(CompressorConfig())
    else:
        ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET /text HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        if compressor:
            stream.write_header('Accept-Encoding', 'gzip')
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 13)
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 101)
        asserts.header_value(response, "Sec-WebSocket-Accept", "SUeCT5AIyDftvfPRgVEj38G/wsA=")
        ctx.backend.state.get_request()  # skip first request

        stream = stream.switch_ws()

        time.sleep(1)
        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=1, payload="Hello")
        stream.write_request(raw_request)
        response = stream.read_response()
        backend_request = ctx.backend.state.get_request()
        assert response.opcode == WebSocketOpcode.WS_TEXT_FRAME
        assert backend_request.payload == "Hello"
        assert response.payload == "world"

        time.sleep(2)
        ctx.backend.server_config.response = yes_response
        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=1, payload="No")
        stream.write_request(raw_request)
        response = stream.read_response()
        backend_request = ctx.backend.state.get_request()
        assert response.opcode == WebSocketOpcode.WS_TEXT_FRAME
        assert backend_request.payload == "No"
        assert response.payload == "yes"

        time.sleep(3)
        ctx.backend.server_config.response = no_response
        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=1, payload="Yes")
        stream.write_request(raw_request)
        response = stream.read_response()
        backend_request = ctx.backend.state.get_request()
        assert response.opcode == WebSocketOpcode.WS_TEXT_FRAME
        assert backend_request.payload == "Yes"
        assert response.payload == "no"


def test_websocket_control_frame_payload(ctx):
    """
    Отправляем control frame с payload больше 125. По стандарту фрагментация запрещена.
    """
    ctx.start_backend(WSResponseConfig(), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET /ctrlf HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 13)
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 101)
        asserts.header_value(response, "Sec-WebSocket-Accept", "SUeCT5AIyDftvfPRgVEj38G/wsA=")

        stream = stream.switch_ws()

        with pytest.raises(WSReaderException):
            raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_CONN_CLOSE_FRAME, mask=1, payload="H"*256)
            stream.write_request(raw_request)


def test_websocket_binary_message(ctx):
    """
    Отправляем бинарное сообщение и принимаем бинарный ответ
    """
    binout = "\xc3\x92\xc4\x73\xc3\xb1\xc4\x73\xc3\xa8\xc4\x77\xc3\xa8\xc4\x73\xc4\x81\x13\xc3\xa5\x13\x5a\x62\x62\x5a\x5f\x58\x21\x13\xc3\x87\xc3\xa8\xc4\x7b\xc3\xa8\xc3\xa5\xc3\xb1\x21" \
        "\x13\xc3\x94\xc3\xad\xc4\x73\xc3\xab\xc3\xb0\xc3\xab\xc3\xb0\xc3\xa6\x13\xc3\xa6\xc3\xa3\xc4\x73\xc3\xa3\xc3\xb0\xc4\x75\xc3\xab\xc4\x73\xc3\xb1\xc3\xa5\xc3\xa3\xc3\xb0"
    binin = "\xc3\x8d\xc3\xb1\xc3\xb0\xc3\xa8\xc4\x7a\xc3\xb0\xc3\xb1\x13\xc3\xb2\xc4\x73\xc3\xb1\xc4\x73\xc3\xa8\xc4\x77\xc3\xa8\xc4\x73\xc4\x7f\x13\xc3\xaf\xc3\xa8\xc3\xb0\xc4\x82"

    world_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_BIN_FRAME, mask=0, payload=binin)
    ctx.start_backend(WSResponseConfig(response=world_response), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET /text HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 13)
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 101)
        asserts.header_value(response, "Sec-WebSocket-Accept", "SUeCT5AIyDftvfPRgVEj38G/wsA=")

        stream = stream.switch_ws()

        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_BIN_FRAME, mask=1, payload=binout)
        stream.write_request(raw_request)
        response = stream.read_response()
        ctx.backend.state.get_request()  # skip first
        backend_request = ctx.backend.state.get_request()
        assert response.opcode == WebSocketOpcode.WS_BIN_FRAME
        assert backend_request.payload == binout
        assert response.payload == binin


def test_websocket_close(ctx):
    """
    И проверим хэндшейк для закрытия соединения
    """
    ctx.start_backend(WSResponseConfig(), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET /close HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 13)
        stream.end_headers()
        response = stream.read_response()
        ctx.backend.state.get_request()

        asserts.status(response, 101)
        asserts.header_value(response, "Sec-WebSocket-Accept", "SUeCT5AIyDftvfPRgVEj38G/wsA=")

        stream = stream.switch_ws()

        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_CONN_CLOSE_FRAME, mask=1, payload="\x03\xe9")
        stream.write_request(raw_request)
        response = stream.read_response()
        backend_request = ctx.backend.state.get_request()
        assert response.opcode == WebSocketOpcode.WS_CONN_CLOSE_FRAME
        assert backend_request.payload == "\x03\xe9"
        assert response.payload == "\x03\xe8"

        time.sleep(1)
        with pytest.raises(s.EndOfStream):
            raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_CONN_CLOSE_FRAME, mask=1, payload="\x03\xe9")
            stream.write_request(raw_request)
            response = stream.read_response()

        # time.sleep(3)  # Connect after timeout/Broken pipe
        # with pytest.raises(s.StreamRst):
        #     raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_CONN_CLOSE_FRAME, mask=1, payload="\x03\xe9")
        #     stream.write_request(raw_request)
        #     response = stream.read_response()


@pytest.mark.parametrize('hedged_delay', [0, 1])
def test_websocket_balancer2_second_attempt(ctx, hedged_delay):
    """
    Мы не тестируем один attempt, т.к. это не тест балансировки. Смотрим, что мы не ломаем перезапрос
    """
    world_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=0, payload="world")
    ctx.start_backend(SimpleDelayedConfig(response_delay=5, response=http.response.service_unavailable()), name="backend1")
    ctx.start_backend(WSResponseConfig(response=world_response), name="backend2")
    ctx.start_balancer(WSAdditionalConfig(attempts=2, mode="balancer2", hedged_delay=hedged_delay))
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET /balancer2_sa HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend1.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 13)
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 101)
        asserts.header_value(response, "Sec-WebSocket-Accept", "SUeCT5AIyDftvfPRgVEj38G/wsA=")

        stream = stream.switch_ws()

        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=1, payload="Hello")
        stream.write_request(raw_request)
        response = stream.read_response()
        ctx.backend2.state.get_request()  # skip first request
        backend_request = ctx.backend2.state.get_request()
        assert response.opcode == WebSocketOpcode.WS_TEXT_FRAME
        assert backend_request.payload == "Hello"
        assert response.payload == "world"
        assert ctx.backend2.state.accepted.value == 1


def test_websocket_balancer2_second_attempt_without_handshake(ctx):
    """
    Пробуем послать websocket фрейм без хендшейка
    """
    maxlen = 4096
    world_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=0, payload="world")
    ctx.start_fake_backend(name="backend1")
    ctx.start_backend(WSResponseConfig(response=world_response), name="backend2")
    ctx.start_balancer(WSAdditionalConfig(attempts=2, mode="balancer2", maxlen=maxlen))
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()
        stream = stream.switch_ws()

        with pytest.raises(s.EndOfStream):
            raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=1, payload="a" * maxlen)
            stream.write_request(raw_request)
            response = stream.read_response()
            assert response.payload != "world"  # workaround for pyflakes
        assert ctx.backend2.state.accepted.value == 0


def test_websocket_balancer2_first_goaway(ctx):
    """
    Проверяем что в случае проблем с бекендом мы не переключимся на другой бекенд
    """
    world_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=0, payload="world")
    ctx.start_backend(WSResponseConfig(response=world_response, close_after=0), name="backend1")
    ctx.start_backend(WSResponseConfig(response=world_response), name="backend2")
    ctx.start_balancer(WSAdditionalConfig(attempts=2, mode="balancer2"))
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET /balancer2_goaway HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend1.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 13)
        stream.end_headers()
        response = stream.read_response()
        ctx.backend1.state.get_request()

        asserts.status(response, 101)
        asserts.header_value(response, "Sec-WebSocket-Accept", "SUeCT5AIyDftvfPRgVEj38G/wsA=")

        stream = stream.switch_ws()

        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=1, payload="Hello")
        stream.write_request(raw_request)
        response = stream.read_response()
        backend_request = ctx.backend1.state.get_request()
        assert response.opcode == WebSocketOpcode.WS_TEXT_FRAME
        assert backend_request.payload == "Hello"
        assert response.payload == "world"

        time.sleep(2)  # Connect before timeout
        with pytest.raises(s.EndOfStream):
            stream.write_request(raw_request)
            response = stream.read_response()

        time.sleep(3)  # Connect after timeout/Broken pipe
        with pytest.raises(socket.error):
            stream.write_request(raw_request)
            response = stream.read_response()

        assert ctx.backend1.state.accepted.value == 1
        assert ctx.backend2.state.accepted.value == 0


def test_websocket_antirobot_catch(ctx):
    """
    Проверяем что произойдет в случае если мы поймали капчу
    """
    maxlen = 4096
    world_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=0, payload="world")
    ctx.start_backend(WSResponseConfig(response=world_response, close_after=0), name="backend1")
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'X-ForwardToUser-Y': '1'}, data="captcha")), name="antirobot1")
    ctx.start_balancer(WSAdditionalAntirobotConfig(attempts=2, mode="antirobot", maxlen=maxlen))
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET /antirobot_catch HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend1.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 13)
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 200)
        asserts.content(response, 'captcha')

        stream = stream.switch_ws()

        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=1, payload="a" * maxlen)
        time.sleep(2)  # Connect before timeout
        with pytest.raises(s.EndOfStream):
            stream.write_request(raw_request)
            response = stream.read_response()

        assert ctx.backend1.state.accepted.value == 0


def test_websocket_antirobot_pass(ctx):
    """
    Проверяем что после прохождения капчи сессия работает корректно
    """
    world_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=0, payload="world")
    yes_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=0, payload="yes")
    no_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=0, payload="no")

    ctx.start_backend(WSResponseConfig(response=[world_response, yes_response, no_response]), name="backend1")
    ctx.start_backend(SimpleConfig(http.response.ok(headers={'X-ForwardToUser-Y': '0'}, data="captcha")), name="antirobot1")
    ctx.start_balancer(WSAdditionalAntirobotConfig(attempts=2, mode="antirobot"))
    with ctx.create_http_connection() as conn:
        stream = conn.create_stream()

        stream.write_line('GET /antirobot_catch HTTP/1.1')
        stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
        stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend1.server_config.port))
        stream.write_header('Connection', 'Upgrade')
        stream.write_header('Upgrade', "websocket")
        stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
        stream.write_header('Sec-WebSocket-Version', 13)
        stream.end_headers()
        response = stream.read_response()

        asserts.status(response, 101)
        asserts.header_value(response, "Sec-WebSocket-Accept", "SUeCT5AIyDftvfPRgVEj38G/wsA=")
        ctx.backend1.state.get_request()  # skip first request

        stream = stream.switch_ws()

        time.sleep(1)
        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=1, payload="Hello")
        stream.write_request(raw_request)
        response = stream.read_response()
        backend_request = ctx.backend1.state.get_request()
        assert response.opcode == WebSocketOpcode.WS_TEXT_FRAME
        assert backend_request.payload == "Hello"
        assert response.payload == "world"

        time.sleep(2)
        ctx.backend1.server_config.response = yes_response
        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=1, payload="No")
        stream.write_request(raw_request)
        response = stream.read_response()
        backend_request = ctx.backend1.state.get_request()
        assert response.opcode == WebSocketOpcode.WS_TEXT_FRAME
        assert backend_request.payload == "No"
        assert response.payload == "yes"

        time.sleep(3)
        ctx.backend1.server_config.response = no_response
        raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=1, payload="Yes")
        stream.write_request(raw_request)
        response = stream.read_response()
        backend_request = ctx.backend1.state.get_request()
        assert response.opcode == WebSocketOpcode.WS_TEXT_FRAME
        assert backend_request.payload == "Yes"
        assert response.payload == "no"

        assert ctx.backend1.state.accepted.value == 1


def test_websocket_graceful_shutdown(ctx):
    """
    Проверяем в этом скрипте graceful shutdown, чтобы не переносить кастомные хендлеры в тесты админки
    """
    def request_loop():
        with ctx.create_http_connection() as conn:
            stream = conn.create_stream()

            stream.write_line('GET /graceful HTTP/1.1')
            stream.write_header('Host', 'localhost:{0}'.format(ctx.balancer.config.port))
            stream.write_header('Origin', 'http://localhost:{0}'.format(ctx.backend.server_config.port))
            stream.write_header('Connection', 'Upgrade')
            stream.write_header('Upgrade', "websocket")
            stream.write_header('Sec-WebSocket-Key', 'poewQpxRF/cEo50ENhfRIA==')
            stream.write_header('Sec-WebSocket-Version', 13)
            stream.end_headers()
            response = stream.read_response()

            asserts.status(response, 101)
            asserts.header_value(response, "Sec-WebSocket-Accept", "SUeCT5AIyDftvfPRgVEj38G/wsA=")
            ctx.backend.state.get_request()  # skip first request

            stream = stream.switch_ws()
            try:
                while True:
                    raw_request = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=1, payload="Hello")
                    stream.write_request(raw_request)
                    response = stream.read_response()
                    backend_request = ctx.backend.state.get_request()
                    assert response.opcode == WebSocketOpcode.WS_TEXT_FRAME
                    assert backend_request.payload == "Hello"
                    assert response.payload == "world"
            except:
                pass

    world_response = WebSocketFrame(fin=1, opcode=WebSocketOpcode.WS_TEXT_FRAME, mask=0, payload="world")
    ctx.start_backend(WSResponseConfig(response=world_response), name="backend")
    ctx.start_balancer(HTTPBalancerConfig())
    thread = Process(target=request_loop)
    thread.start()
    time.sleep(2)

    ctx.graceful_shutdown(timeout="1s")
    time.sleep(2)
    assert not ctx.balancer.is_alive()
    ctx.balancer.set_finished()
    thread.join(10)

    if thread.is_alive():
        thread.terminate()
