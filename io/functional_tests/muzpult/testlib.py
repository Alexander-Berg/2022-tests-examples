import websocket
import tempfile
import os
import json
import uuid
from threading import Thread
from threading import Condition
from queue import Queue
import logging


logger = logging.getLogger(__name__)


class GlagolConnector:
    def __init__(self, uri, cert, token):
        self._uri = uri
        self._cert = cert
        self._token = token
        f = tempfile.NamedTemporaryFile(delete=False)
        f.write(bytearray(cert, "utf8"))
        f.write(b'\n')
        self._certfile = f.name
        f.close()
        self._quit = False
        self._cmdQueue = Queue()
        self._answersQueue = Queue()
        self._speakerState = {}
        self._stateCond = Condition()
        self._waitAnswers = {}

    def __enter__(self):
        self._wsThread = Thread(target=self._ws_thread_loop)
        self._wsThread.start()
        return self

    def __exit__(self, a, b, c):
        self._quit = True
        self._wsThread.join()
        os.unlink(self._certfile)

    def _ws_work(self):
        while not self._cmdQueue.empty():
            msg = self._cmdQueue.get()
            self._waitAnswers[msg.get('id')] = True
            self._ws.send(json.dumps(msg))

        raw = self._ws.recv()
        a = json.loads(raw)
        id = a.get('requestId')
        logger.debug("received message id = {}".format(id))
        if id is not None and self._waitAnswers.pop(id):
            self._answersQueue.put(a)

        with self._stateCond:
            self._speakerState = a.get('state')
            self._stateCond.notify()

    def _ws_thread_loop(self):
        self._reconnect()
        while not self._quit:
            try:
                self._ws_work()
            except (ConnectionResetError, websocket._exceptions.WebSocketConnectionClosedException):
                if not self._quit:
                    self._reconnect()
        self._ws.close()

    def wait_for_state(self, fn):
        checking = True
        while checking:
            with self._stateCond:
                self._stateCond.wait()
                checking = not fn(self._speakerState)

    def _reconnect(self):
        # websocket library has no passthrough for 'cadata' parameter for context.load_verify_locations()
        # so we have to pass cert via file
        self._ws = websocket.create_connection(
            self._uri,
            enable_multithread=True,
            sslopt={
                'ca_certs': self._certfile,
                'check_hostname': False,
                #  "cert_reqs": ssl.CERT_NONE
            },
        )
        logger.info("status of WS connection = {}".format(self._ws.getstatus()))

    @staticmethod
    def gen_id():
        return uuid.uuid4()

    def send_request(self, payload):
        id = str(self.gen_id())
        logger.debug("sending message id = {}".format(id))
        msg = {'id': id, 'payload': payload, "conversationToken": self._token}
        self._cmdQueue.put(msg)
        return self._answersQueue.get()

    def play_music(
        self,
        id: str,
        type: str,
        startFromId: str = None,
        startFromPosition: int = None,
        offset: float = None,
        fromStr: str = None,
        shuffle: bool = None,
        repeat: str = None,
    ):
        payload = {'id': id, 'type': type}
        if startFromId is not None:
            payload['startFromId'] = startFromId
        if startFromPosition is not None:
            payload['startFromPosition'] = startFromPosition
        if offset is not None:
            payload['offset'] = offset
        if fromStr is not None:
            payload['from'] = fromStr
        if shuffle is not None:
            payload['shuffle'] = shuffle
        if repeat is not None:
            payload['repeat'] = repeat
        return self.command('playMusic', payload)

    def command(self, cmd: str, params: dict = {}):
        params['command'] = cmd
        return self.send_request(params)

    def stop(self):
        return self.command('stop')

    def play(self):
        return self.command('play')

    def next(self):
        return self.command('next')

    def prev(self):
        return self.command('prev')

    def rewind(self, position: float):
        return self.command('rewind', {'position': position})

    def set_volume(self, vol: float):
        return self.command("setVolume", {"volume": vol})

    def software_version(self):
        return self.command('softwareVersion').get('softwareVersion')


class PlayerState:
    @staticmethod
    def is_stopped(state):
        return not state.get('playing')


def make_glagol_connector(device, backend_client, authToken):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    device.stage_logger.test_stage("Getting devices list")
    devices = backend_client.getGlagolDevicesList(authToken)
    platform = ''
    cert = ''
    for d in devices:
        if d.get('id') == device.device_id:
            platform = d.get('platform')
            cert = d.get('glagol').get('security').get('server_certificate')
            break
    device.failer.assert_fail(len(platform) != 0, "Device not found in devices list")
    device.failer.assert_fail(len(cert) != 0, "Empty certificate")
    port = device.config["glagold"]["externalPort"]
    return GlagolConnector(
        'wss://localhost:{}'.format(port), cert, backend_client.getGlagolToken(authToken, device.device_id, platform)
    )
