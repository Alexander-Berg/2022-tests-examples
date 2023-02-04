from .connector import QuasarConnector, getMessages
from .matchers import (
    has_alice_state,
    has_output_speech,
    is_alice_listening,
    is_alice_speaking,
    has_music_state,
    has_audio_player_state,
    has_app_state,
    has_radio_state,
)
import threading
import yatest.common as yc
from google.protobuf import text_format
import yandex_io.protos.model_objects_pb2 as P
import yandex_io.protos.functional_tests_pb2 as func_test_proto
import yandex_io.protos.quasar_proto_pb2 as quasar_proto
import yandex_io.protos.remoting_pb2 as remoting_proto
import datetime
import logging
import typing as tp
import json

logger = logging.getLogger(__name__)

MAX_DEVICE_ID_LENGHT = 15


class StageLogger(object):
    def __init__(self, path, config, device_id="", log_service_events=True):
        self.file = open(path, "a+")
        self.config = config
        self.device_id = '[' + device_id + " " * (MAX_DEVICE_ID_LENGHT - len(device_id)) + ']'
        self.thread = threading.Thread(target=self._listen_services, daemon=True)
        self.running = True
        self.host = yc.get_param("target_host", "localhost")
        if self.config is not None:
            self.testpoint_connector = QuasarConnector(self.host, self.config["testpoint"]["port"], "testpoint")
            self.testpoint_connector.start()
        else:
            self.testpoint_connector = None
        if log_service_events:
            self.thread.start()

    def test_stage(self, message):
        stage_log = "".join(["Stagged passed: ", message, "\n"])
        self.file.write(self._add_timestamp(stage_log))
        self.file.flush()
        self._send_to_testpoint(stage_log)

    def _send_to_testpoint(self, log):
        if self.testpoint_connector is not None:
            msg = quasar_proto.QuasarMessage()
            msg.testpoint_message.test_event.event = func_test_proto.TestEvent.Event.LOG
            msg.testpoint_message.test_event.log = log
            self.testpoint_connector.send(msg)

    @staticmethod
    def _get_current_time():
        current_time = datetime.datetime.now()
        return current_time.strftime("['%Y-%m-%d %H:%M:%S']")

    def _add_timestamp(self, message):
        return " ".join([self._get_current_time(), message])

    def log(self, message):
        log = "".join([message, "\n"])
        self.file.write(self._add_timestamp(log))
        self.file.flush()
        self._send_to_testpoint(log)

    def log_fail(self, message):
        log = "".join(["Test FAILED: ", message, "\n"])
        self.file.write(self._add_timestamp(log))
        self.file.flush()
        self._send_to_testpoint(log)

    def _listen_services(self):
        aliced_logger = AlicedLogger(self.host, self.config["aliced"].get('local_port', self.config["aliced"]["port"]))
        mediad_logger = MediadLogger(self.host, self.config["mediad"].get('local_port', self.config["mediad"]["port"]))
        testpoint_logger = TestpointLogger(
            self.host, self.config["testpoint"].get('local_port', self.config["testpoint"]["port"])
        )
        alarm_logger = AlarmLogger(self.host, self.config["alarmd"].get('local_port', self.config["alarmd"]["port"]))
        wifi_logger = WifiLogger(self.host, self.config["wifid"].get('local_port', self.config["wifid"]["port"]))

        while self.running:
            self._print_log_events(aliced_logger)
            self._print_log_events(mediad_logger)
            self._print_log_events(testpoint_logger)
            self._print_log_events(alarm_logger)
            self._print_log_events(wifi_logger)

        aliced_logger.finish()
        mediad_logger.finish()
        testpoint_logger.finish()
        alarm_logger.finish()
        wifi_logger.finish()

    def _print_log_events(self, logger):
        for event in logger.make_log_events():
            self.file.write(" ".join([self._get_current_time(), self.device_id, event]) + "\n")
            self.file.flush()

    def end_logging(self):
        self.running = False
        try:
            self.thread.join()
        except RuntimeError:
            pass
        if self.testpoint_connector is not None:
            self.testpoint_connector.close()
        self.file.close()


class ServiceLogger(object):
    def __init__(self, host, port, name):
        self.connector = QuasarConnector(host, port, name, True)
        self.connector.start()

    def collect_messages(self):
        return getMessages(self.connector)

    def make_log_events(self):
        log_events = []
        for message in self.collect_messages():
            if message is None:
                continue
            events = self._parse_message(message)
            if events:
                for event in events:
                    log_events.append(event)
        return log_events

    def _parse_message(self, message):
        return [text_format.MessageToString(message, as_utf8=True)]

    def finish(self):
        self.connector.close()


class AlicedLogger(ServiceLogger):
    def __init__(self, host, port):
        super().__init__(host, port, "aliced")
        self.last_logged_response = None
        self.last_recognized_phrase = None
        self.last_req_id = None
        self.last_alice_state = P.AliceState.State.IDLE

    def _get_last_state_log(self):
        if self.last_alice_state == P.AliceState.State.LISTENING and self.last_recognized_phrase is not None:
            event = "".join(
                [
                    "Alice recognized phrase: ",
                    "\"",
                    self.last_recognized_phrase,
                    "\"",
                    ", request_id: ",
                    self.last_req_id,
                ]
            )
            self.last_recognized_phrase = None
            return [event]
        elif self.last_alice_state == P.AliceState.State.SPEAKING:
            return ["".join(["Alice respond: \"", self.last_logged_response, "\""])]

    def _handle_listening(self, message):
        self.last_recognized_phrase = message.alice_state.recognized_phrase
        self.last_req_id = message.alice_state.request_id

    def _parse_message(self, message):
        if not has_alice_state(message):
            return None
        if is_alice_listening(message) and message.alice_state.HasField("recognized_phrase"):
            self.last_recognized_phrase = message.alice_state.recognized_phrase
            self.last_req_id = message.alice_state.request_id
        elif is_alice_speaking(message) and has_output_speech(message):
            self.last_logged_response = message.alice_state.vins_response.output_speech
        if message.alice_state.state != self.last_alice_state:
            log = self._get_last_state_log()
            self.last_alice_state = message.alice_state.state
            return log
        return None


class MediadLogger(ServiceLogger):
    def __init__(self, host, port):
        super().__init__(host, port, "mediad")
        self.media_state = {
            "Thick music": (False, ""),
            "Thin music": (False, ""),
            "Radio": (False, ""),
            "Bluetooth": (False, ""),
        }

    @staticmethod
    def _has_media_state(type, message):
        if type == "Thick music":
            return has_music_state(message)
        elif type == "Thin music":
            return has_audio_player_state(message)
        elif type == "Radio":
            return has_radio_state(message)
        elif type == "Bluetooth":
            return message.app_state.HasField("bluetooth_player_state")
        raise RuntimeError("Unknown type {}".format(type))

    @staticmethod
    def _get_title(type, message):
        if type == "Thick music":
            return message.app_state.music_state.title
        elif type == "Thin music":
            return message.app_state.audio_player_state.audio.metadata.title
        elif type == "Radio":
            return message.app_state.radio_state.radio_title
        elif type == "Bluetooth":
            return message.app_state.bluetooth_player_state.track_meta_info.title
        raise RuntimeError("Unknown type {}".format(type))

    @staticmethod
    def _get_playing_state(type, message):
        if type == "Thick music":
            return not message.app_state.music_state.is_paused
        elif type == "Thin music":
            return (
                message.app_state.audio_player_state.event == P.AudioClientEvent.HEARTBEAT
                and message.app_state.audio_player_state.state == P.AudioClientState.PLAYING
            )
        elif type == "Radio":
            return not message.app_state.radio_state.is_paused
        elif type == "Bluetooth":
            return not message.app_state.bluetooth_player_state.is_paused
        else:
            raise RuntimeError("Unknown type {}".format(type))

    def _parse_message(self, message):
        if not has_app_state(message):
            return None
        results = []
        for type, state in self.media_state.items():
            if not self._has_media_state(type, message):
                continue
            playing, title = state
            new_playing_state = self._get_playing_state(type, message)
            new_title = self._get_title(type, message)

            if new_playing_state != playing or (new_playing_state and new_title != title):
                self.media_state[type] = (new_playing_state, new_title)
                playing_state_message = " started to play, track: " if new_playing_state else " stopped, track: "
                results.append("".join([type, playing_state_message, "\"", new_title, "\""]))
        return results


class TestpointLogger(ServiceLogger):
    def __init__(self, host, post):
        super().__init__(host, post, "testpoint")
        self.message = "Handled directives: "
        self.blacklist = {"pilot_command"}

    def _format_directive(self, directive):
        directive_str = "\"" + directive.name + "\"" + " request_id: " + directive.request_id
        if directive.name == "sound_file_play":
            payload = json.loads(directive.json_payload)
            data = payload["method_data"].encode()
            msg = remoting_proto.Remoting.FilePlayerCapabilityMethod()
            msg.ParseFromString(data)
            directive_str = directive_str + ", file: " + msg.file_name
        return directive_str

    def _get_directives(self, message):
        directives = []
        if not message.HasField("testpoint_message"):
            return directives
        for directive in message.testpoint_message.incoming_directive:
            if directive.name not in self.blacklist:
                directives.append(self._format_directive(directive))
        return directives

    def _parse_message(self, message):
        directives = self._get_directives(message)
        if not directives:
            return None
        return [self.message + ", ".join(directives)]


class AlarmLogger(ServiceLogger):
    def __init__(self, host, port) -> None:
        super().__init__(host, port, "alarm")
        self.__last_ical_state = ""

    @staticmethod
    def _get_ical(message) -> tp.Optional[str]:
        if message.HasField("alarms_state") and message.alarms_state.HasField("icalendar_state"):
            return message.alarms_state.icalendar_state

    def _parse_message(self, message) -> tp.Optional[tp.List[str]]:
        ical = self._get_ical(message)
        if ical is not None and ical != self.__last_ical_state:
            self.__last_ical_state = ical
            return [f"New ical state: \n{ical}"]


class WifiLogger(ServiceLogger):
    def __init__(self, host, port) -> None:
        super().__init__(host, port, "wifi")
        self._last_internet_status = None

    def _parse_message(self, message) -> tp.Optional[tp.List[str]]:
        if message.HasField("wifi_status") and message.wifi_status.HasField("internet_reachable"):
            if (
                self._last_internet_status is None
                or self._last_internet_status != message.wifi_status.internet_reachable
            ):
                self._last_internet_status = message.wifi_status.internet_reachable
                return [f"New internet reachable state: {self._last_internet_status}"]
