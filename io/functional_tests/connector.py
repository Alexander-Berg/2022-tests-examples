import logging
import os
import socket
import struct
import time
import threading
import queue
import yandex_io.protos.quasar_proto_pb2 as P

from google.protobuf import text_format


logger = logging.getLogger(__name__)


def proto_to_utf8_str(proto_msg):
    return text_format.MessageToString(proto_msg, as_utf8=True)


class QuasarConnector:
    def __init__(self, host, port, name, verbose=True, extra_verbose=False):
        self.messages = queue.Queue()
        self.daemon_name = name
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.host = host
        self.port = port
        self.thread = threading.Thread(target=self.receive, daemon=True)
        self.connected = False
        self.running = False
        self.verbose = verbose
        self.extra_verbose = extra_verbose
        self.message_counter = 0

    def start(self):
        if self.verbose:
            logger.debug(f"{self.daemon_name} connector starting")
        self.running = True
        self.thread.start()
        while not self.connected and self.running:
            time.sleep(0.05)  # Wait for connection to be established
        if not self.running:
            raise RuntimeError(f"Failed to start connector to service {self.daemon_name}, port {self.port}")

    def __enter__(self):
        self.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()

    def connect(self):
        if self.verbose:
            logger.info(f"Connecting to {self.daemon_name} on {self.host}:{self.port}")
        ret = self.socket.connect_ex((self.host, self.port))
        retry_count = 0
        while ret != 0 and retry_count < 1000:
            retry_count += 1
            if self.verbose:
                logger.error(f"Error while trying to connect to {self.daemon_name}: {os.strerror(ret)}")
            time.sleep(0.05)  # So that we won't spam errors
            ret = self.socket.connect_ex((self.host, self.port))
        self.socket.settimeout(1)
        self.connected = ret == 0
        if self.verbose:
            logger.info(f"Connected to {self.daemon_name}")

    def receive(self):
        def read(socket, size):
            buf = b''
            while size > 0:
                data = socket.recv(size)
                if data == b'':
                    raise RuntimeError(f'{self.daemon_name} connector\'s connection closed unexpectedly')
                buf += data
                size -= len(data)
            return buf

        self.connect()

        if not self.connected:
            logger.error("Failed to connect")
            self.running = False

        while self.running:
            try:
                try:
                    msg_len = struct.unpack('<I', read(self.socket, 4))[0]
                    msg_buf = read(self.socket, msg_len)
                except RuntimeError as e:
                    if self.verbose:
                        logger.error(e)
                    return
                msg = P.QuasarMessage()
                msg.ParseFromString(msg_buf)
                debug_msg = proto_to_utf8_str(msg)
                if self.extra_verbose:
                    logger.debug(f"{self.daemon_name} connector got a message of length {msg_len}:\n{debug_msg}")
                self.messages.put(msg)
            except socket.timeout:
                if not self.running:
                    break

    def send(self, obj):
        if not self.connected:
            raise RuntimeError("Send when not connected")
        msg = obj.SerializeToString()
        msg_len = struct.pack('<I', len(msg))
        self.socket.sendall(msg_len)
        self.socket.sendall(msg)
        debug_msg = proto_to_utf8_str(obj)
        if self.extra_verbose:
            logger.debug(f"{self.daemon_name} connector sent a message of length {len(msg)}:\n{debug_msg}")

    def wait_for_message(self, predicate=lambda _: True, timeout=30):
        msgs = self.wait_for_messages({predicate}, timeout=timeout)
        return None if msgs is None else msgs[0]

    def wait_for_messages(self, predicates=None, timeout=30):
        start_time = time.perf_counter()
        predicates = set(predicates) if predicates is not None else {lambda _: True}
        messages = []

        def process_message(message, predicates):
            new_preds = set()
            for pred in predicates:
                if not pred(message):
                    new_preds.add(pred)
            return new_preds

        while True:
            iteration_timeout = timeout - (time.perf_counter() - start_time) if timeout is not None else 0
            if iteration_timeout < 0:
                iteration_timeout = 0
            try:
                msg = self.messages.get(timeout=iteration_timeout)
                self.message_counter += 1
            except queue.Empty:
                return None
            predicates_len = len(predicates)
            predicates = process_message(msg, predicates)
            if len(predicates) < predicates_len:
                messages.append(msg)
            if not predicates:
                return messages
            if timeout is None or time.perf_counter() - start_time > timeout:
                return None

    def _clear_queue(self):
        while True:
            try:
                self.messages.get(timeout=0.01)
                self.message_counter += 1
            except queue.Empty:
                return

    def close(self):
        self.running = False
        self._clear_queue()
        self.thread.join()

    def clear_message_queue(self):
        with self.messages.mutex:
            self.messages.queue.clear()

    def has_messages(self):
        return self.messages.qsize() > 0

    def message_in_queue(self):
        return self.messages.qsize()


def getMessages(service, max_messages=30):
    msgs = []
    message_counter = 0
    while True:
        if message_counter == max_messages:
            break
        try:
            msgs.append(service.messages.get_nowait())
        except queue.Empty:
            break
        message_counter += 1
    return msgs
