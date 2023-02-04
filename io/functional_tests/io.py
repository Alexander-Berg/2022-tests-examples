import functools
import logging
import pytest
import queue
import socket
import time
from pydub import AudioSegment
import yatest.common as yc
import yandex_io.protos.model_objects_pb2 as model_objects
import yandex_io.protos.functional_tests_pb2 as func_test_proto
import yandex_io.protos.quasar_proto_pb2 as quasar_proto
from .connector import QuasarConnector, getMessages
from .fail_wrapper import FailWrapper
from .utils import ANY_IDLING_ALICE_STATE, common_data_path
import random


logger = logging.getLogger(__name__)


class YandexIODevice:
    def __init__(self, device_id, exec, config, cwd, stage_logger, user_info=None):
        self.device_id = device_id
        self.exec = exec
        self.cwd = cwd
        self.config = config
        self.stage_logger = stage_logger
        self.failer = FailWrapper(self.stage_logger)
        self.user_uid = user_info["account"]["uid"] if user_info is not None else None
        logger.info(f"Started {self.exec.command} with PID {self.exec.process.pid} in {self.cwd}")

    def _check_for_running(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            self = args[0]
            if not self.exec.running:
                pytest.fail(f"Device {self.device_id}: PID {self.exec.process.pid} is not running, that's an error")
            return func(*args, **kwargs)

        return wrapper

    def authenticate(self, xcode):
        self.stage_logger.log(f"Authenticate device with {xcode}")
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.authenticate_request.xcode = xcode
        self._send_to_testpoint(msg)

    def get_service_connector(self, service_name, proto_logging=True):
        host = yc.get_param("target_host", "localhost")
        port = self.config[service_name].get('local_port', self.config[service_name]['port'])
        return QuasarConnector(host, port, service_name, extra_verbose=proto_logging)

    def wait_for_message(self, connector, predicate, fail_message, timeout=30):
        message = connector.wait_for_message(predicate, timeout)
        self.failer.assert_fail(message is not None, fail_message)
        return message

    def wait_for_messages(self, connector, predicates, fail_message, timeout=30):
        messages = connector.wait_for_messages(predicates, timeout)
        self.failer.assert_fail(messages is not None, fail_message)
        return messages

    def ensure_message_not_recieved(self, connector, predicate, fail_message, timeout=30):
        self.failer.assert_fail(connector.wait_for_message(predicate, timeout) is None, fail_message)

    def start_test(self, name):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.test_event.test_name = name
        msg.testpoint_message.test_event.event = func_test_proto.TestEvent.Event.START
        self._send_to_testpoint(msg)

    def end_test(self, name):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.test_event.test_name = name
        msg.testpoint_message.test_event.event = func_test_proto.TestEvent.Event.END
        self._send_to_testpoint(msg)

    def press_alice_button(self, target_state=model_objects.AliceState.State.LISTENING):
        with self.get_service_connector("aliced") as aliced:
            msgs = getMessages(aliced)
            initial_state = None
            if msgs:
                initial_state = msgs[-1].alice_state.state
            msg = quasar_proto.QuasarMessage()
            msg.testpoint_message.emulated_button.activate.SetInParent()
            self._send_to_testpoint(msg)

            def is_final_state(state):
                if target_state is not None:
                    if hasattr(target_state, "__contains__"):
                        return state in target_state
                    else:
                        return state == target_state
                else:
                    return state != initial_state

            aliced.wait_for_message(lambda m: m.HasField('alice_state') and is_final_state(m.alice_state.state))

    def start_conversation(self):
        self.stage_logger.log("Starting conversation")
        with self.get_service_connector("aliced") as aliced:
            msg = quasar_proto.QuasarMessage()
            msg.testpoint_message.emulated_button.start_conversation.SetInParent()
            self._send_to_testpoint(msg)
            self.failer.assert_fail(
                aliced.wait_for_message(
                    lambda m: m.HasField('alice_state')
                    and m.alice_state.state == model_objects.AliceState.State.LISTENING
                ),
                "Alice didn't start listening",
            )

    def volume_up(self):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_button.volume_up.SetInParent()
        self._send_to_testpoint(msg)

    def volume_down(self):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_button.volume_down.SetInParent()
        self._send_to_testpoint(msg)

    def mute(self):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_button.volume_mute.SetInParent()
        self._send_to_testpoint(msg)

    def unmute(self):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_button.volume_unmute.SetInParent()
        self._send_to_testpoint(msg)

    def toggle_mute(self):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_button.volume_toggle_mute.SetInParent()
        self._send_to_testpoint(msg)

    @_check_for_running
    def _send_to_testpoint(self, quasar_message):
        with self.get_service_connector("testpoint") as testpoint:
            testpoint.send(quasar_message)

    @_check_for_running
    def say_to_mic(self, wav_path):
        """
        Assumes 16bit pcm at 16khz to estimate "phrase duration", used to synchronize "saying" and "hearing" durations between test and tested software
        """
        wav = AudioSegment.from_wav(wav_path)
        assert wav.channels == 1
        assert wav.sample_width == 2
        assert wav.frame_rate == 16000

        io_start = time.time()
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as mic:
            host = yc.get_param("target_host", "localhost")
            audiodevice_config = self.config["audiod"]["audioDevice"]
            port = audiodevice_config.get('local_port', audiodevice_config['port'])
            mic.connect((host, port))
            self.stage_logger.log("Saying to mic {}".format(wav_path[wav_path.rfind('/') + 1 :]))
            mic.send(wav.raw_data)
            # fixme(eninng): SK-5070. keep sleep to make sure socket will not be closed until device receive all chunks
            time.sleep(1)

        io_duration = time.time() - io_start

        logger.info(
            f"Command from {wav_path} written to mic, sleeping for {wav.duration_seconds - io_duration} so that the phrase's duration is properly reflected"
        )
        if wav.duration_seconds - io_duration > 0.0:
            time.sleep(wav.duration_seconds - io_duration)

    @_check_for_running
    def say_alisa_wake_word(self):
        ALICE_WAVS = 3
        number = random.randint(1, ALICE_WAVS)
        self.say_to_mic(common_data_path("alisa_{}.wav".format(number)))

    def start_setup_mode(self):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.start_configuration_mode.SetInParent()
        self._send_to_testpoint(msg)

    def stop(self):
        try:
            self.exec.kill()
        except yc.process.InvalidExecutionStateError:
            pass

    @_check_for_running
    def wait_for_listening_start(self, spotter_model=''):
        with self.get_service_connector("aliced") as aliced:
            alice_state = None
            try:
                alice_state = aliced.wait_for_message(
                    lambda m: m.HasField('alice_state')
                    and m.alice_state.has_startup_requirements
                    and spotter_model in m.alice_state.activation_spotter_model
                    and m.alice_state.state in ANY_IDLING_ALICE_STATE
                )
            except queue.Empty:
                pytest.fail("App did not start listening and raise exception, that's a fail", pytrace=False)
            else:
                if alice_state is None:
                    pytest.fail("App did not start listening after default timeout, that's a fail", pytrace=False)
                logger.info(f"Sample app started listening for device {self.device_id}")

    @_check_for_running
    def wait_for_authenticate_completion(self):
        def _check_uid(uid):
            return self.user_uid is None and uid != "" or self.user_uid == uid

        def _has_stored_account(m):
            return (
                m.HasField("owner_startup_info")
                and _check_uid(m.owner_startup_info.passport_uid)
                and m.owner_startup_info.auth_token != ""
            )

        def _change_user_event(m):
            return (
                m.HasField("change_user_event")
                and _check_uid(m.change_user_event.passport_uid)
                and m.change_user_event.auth_token != ""
            )

        def _change_token_event(m):
            return (
                m.HasField("change_token_event")
                and _check_uid(m.change_token_event.passport_uid)
                and m.change_token_event.auth_token != ""
            )

        with self.get_service_connector("authd") as authd:
            auth_message = None
            try:
                auth_message = authd.wait_for_message(
                    lambda m: _change_user_event(m) or _change_token_event(m) or _has_stored_account(m)
                )
            except queue.Empty:
                pytest.fail("App was not able to properly authenticate", pytrace=False)
            else:
                if auth_message is None:
                    pytest.fail(f"Authentication timeout: {self.device_id}", pytrace=False)
                logger.info(f"Account changed for device {self.device_id}")

    def toggle_play_pause(self):
        self.stage_logger.log("Pressing play/pause button")
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_button.toggle_play_pause.SetInParent()
        self._send_to_testpoint(msg)

    def next(self):
        self.stage_logger.log("Pressing next button")
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_button.next.SetInParent()
        self._send_to_testpoint(msg)

    def prev(self):
        self.stage_logger.log("Pressing prev button")
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_button.prev.SetInParent()
        self._send_to_testpoint(msg)
