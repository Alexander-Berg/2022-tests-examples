import copy
import logging
import time
import yandex_io.protos.model_objects_pb2 as P

logger = logging.getLogger(__name__)


class LastMessage:
    def __init__(self, device, service_name, field_name):
        self.device_id = device.device_id
        self.service_name = service_name
        self.connector = device.get_service_connector(service_name)
        self.connector.verbose = True
        self.connector.extra_verbose = False
        self.message = None
        self.field_name = field_name
        self.failer = device.failer
        self.message_counter = 0

    def __enter__(self):
        self.connector.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.connector.close()

    def get(self, timeout=0):
        while True:
            msg = self.connector.wait_for_message(lambda m: m.HasField(self.field_name), timeout)
            if msg is None:
                if self.connector.has_messages():
                    continue
                else:
                    break
            else:
                self.message = getattr(msg, self.field_name)
                self.message_counter += 1
                timeout = 0
        return copy.deepcopy(self.message)

    def wait(self, fail_message="", timeout=30, try_next=False):
        if fail_message == "":
            fail_message = f"Awaiting \"{self.field_name}\" message failed for device_id={self.device_id}"
        if not try_next and self.message is not None:
            timeout = 0
        self.get(timeout)
        self.failer.assert_fail(self.message is not None, fail_message)
        return copy.deepcopy(self.message)

    def wait_for(self, predicate, fail_message="", timeout=30):
        if fail_message == "":
            fail_message = (
                f"Awaiting \"{self.field_name}\" message with predicate failed for device_id={self.device_id}"
            )
        start_time = time.perf_counter()
        msg = self.wait(fail_message, timeout, False)
        if predicate(msg):
            return msg
        while True:
            subtimeout = timeout - (time.perf_counter() - start_time)
            subtimeout = subtimeout if subtimeout > 0 else 0
            msg = self.wait(fail_message, subtimeout, True)
            if predicate(msg):
                return msg

            if time.perf_counter() - start_time > timeout:
                logger.error(
                    f"Timeout {timeout} sec. Last message for device={self.device_id}, service={self.service_name}"
                )
                logger.error(f"#    extracted messages from connector: {self.connector.message_counter}")
                logger.error(f"#    messages in connector queue: {self.connector.message_in_queue()}")
                logger.error(f"#    extracted \"{self.field_name}\" messages: {self.message_counter}")
                logger.error(f"# # #\n{self.message}\n# # #")
                self.failer.fail(fail_message)


class MultiroomState:
    def __init__(self, device):
        self.device_id = device.device_id
        self.last_message = LastMessage(device, "multiroomd", "multiroom_state")

    def __enter__(self):
        self.last_message.__enter__()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.last_message.__exit__(exc_type, exc_val, exc_tb)

    def get(self):
        return self.last_message.get()

    def wait(self, fail_message="", timeout=30, try_next=False):
        if fail_message == "":
            fail_message = f"Awaiting MultiroomState message failed for device_id={self.device_id}"
        msg = self.last_message.wait(fail_message, timeout, try_next)
        logger.info(f"MultiroomState.wait(...) device_id={self.device_id}:\n{msg}\n***")
        return msg

    def wait_for(self, predicate, fail_message="", timeout=30):
        if fail_message == "":
            fail_message = f"Awaiting MultiroomState message with predicate failed for device_id={self.device_id}"
        msg = self.last_message.wait_for(predicate, fail_message, timeout)
        logger.info(f"MultiroomState.wait_for(...) device_id={self.device_id}:\n{msg}\n***")
        return msg

    def wait_any_peers(self, fail_message="", timeout=30):
        if fail_message == "":
            fail_message = f"Awaiting MultiroomState message with any connected peers for device_id={self.device_id}"
        msg = self.wait_for(lambda m: len(m.peers) > 0)
        return msg

    @staticmethod
    def position(state):
        return state.multiroom_broadcast.multiroom_params.position_ns

    @staticmethod
    def music_track_id(state):
        if state is not None:
            if state.HasField("multiroom_broadcast"):
                if state.multiroom_broadcast.HasField("multiroom_params"):
                    if state.multiroom_broadcast.multiroom_params.HasField("music_params"):
                        return state.multiroom_broadcast.multiroom_params.music_params.current_track_id
                    elif state.multiroom_broadcast.multiroom_params.HasField("audio_params"):
                        if state.multiroom_broadcast.multiroom_params.audio_params.HasField("audio"):
                            return state.multiroom_broadcast.multiroom_params.audio_params.audio.id
        return None

    @staticmethod
    def is_same_playback(ms1, ms2):
        return (
            ms1.multiroom_broadcast is not None
            and ms2.multiroom_broadcast is not None
            and ms1.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            and ms2.multiroom_broadcast.state == P.MultiroomBroadcast.State.PLAYING
            and ms1.multiroom_broadcast.multiroom_params.url == ms2.multiroom_broadcast.multiroom_params.url
            and ms1.multiroom_broadcast.multiroom_params.basetime_ns
            == ms2.multiroom_broadcast.multiroom_params.basetime_ns
            and ms1.multiroom_broadcast.multiroom_params.position_ns
            == ms2.multiroom_broadcast.multiroom_params.position_ns
        )


class MultiroomStates(object):
    def __init__(self, devices):
        self.multiroom_states = [MultiroomState(device) for device in devices]

    def __enter__(self):
        for multiroom_state in self.multiroom_states:
            multiroom_state.__enter__()
        return self

    def __getitem__(self, index):
        return self.multiroom_states[index]

    def __exit__(self, exc_type, exc_val, exc_tb):
        for multiroom_state in self.multiroom_states:
            multiroom_state.__exit__(exc_type, exc_val, exc_tb)

    def wait_any_peers(self, fail_message="", timeout=30):
        for multiroom_state in self.multiroom_states:
            multiroom_state.wait_any_peers(fail_message, timeout)


class AppState:
    def __init__(self, device):
        self.device_id = device.device_id
        self.last_message = LastMessage(device, "mediad", "app_state")

    def __enter__(self):
        self.last_message.__enter__()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.last_message.__exit__(exc_type, exc_val, exc_tb)

    def get(self):
        return self.last_message.get()

    def wait(self, fail_message="", timeout=30, try_next=False):
        if fail_message == "":
            fail_message = f"Awaiting AppState message failed for device_id={self.device_id}"
        return self.last_message.wait(fail_message, timeout, try_next)

    def wait_for(self, predicate, fail_message="", timeout=30):
        if fail_message == "":
            fail_message = f"Awaiting AppState message with predicate failed for device_id={self.device_id}"
        return self.last_message.wait_for(predicate, fail_message, timeout)

    def play_pause_id(self):
        app_state = self.get()
        if app_state.HasField("music_state") and not app_state.music_state.is_paused:
            return app_state.music_state.play_pause_id
        elif (
            app_state.HasField("audio_player_state")
            and app_state.audio_player_state.HasField("state")
            and (
                app_state.audio_player_state.state == P.AudioClientState.PLAYING
                or app_state.audio_player_state.state == P.AudioClientState.BUFFERING
            )
        ):
            return app_state.audio_player_state.audio.play_pause_id
        elif app_state.HasField("multiroom_state") and not app_state.multiroom_state.is_paused:
            return app_state.multiroom_state.play_pause_id

        return None

    @staticmethod
    def _is_standalone_music_playing(app_state):
        return (
            app_state.HasField("music_state")
            and not app_state.music_state.is_paused
            and app_state.music_state.progress > 0
            or (app_state.HasField("audio_player_state") and app_state.audio_player_state.HasField("audio"))
            and app_state.audio_player_state.event == P.AudioClientEvent.HEARTBEAT
            and app_state.audio_player_state.state == P.AudioClientState.PLAYING
            and app_state.audio_player_state.audio.position_sec > 0
        )

    def is_standalone_music_playing(self):
        app_state = self.last_message.get()
        return self._is_standalone_music_playing(app_state) if app_state is not None else False

    def wait_standalone_music_playing(self):
        return self.wait_for(
            lambda m: self._is_standalone_music_playing(m),
            fail_message=f"Awating standalone music playing on device {self.device_id} failed",
        )

    @staticmethod
    def _is_multiroom_slave_playing(app_state):
        return app_state.HasField("multiroom_state") and (not app_state.multiroom_state.is_paused)

    def is_multiroom_slave_playing(self):
        app_state = self.last_message.get()
        return self._is_multiroom_slave_playing(app_state) if app_state is not None else False

    def wait_multiroom_slave_playing(self):
        return self.wait_for(
            lambda m: self._is_multiroom_slave_playing(m),
            fail_message="Awating multiroom slave playing on device {self.device_id} failed",
        )

    @staticmethod
    def _is_media_playing(app_state):
        return AppState._is_standalone_music_playing(app_state) or AppState._is_multiroom_slave_playing(app_state)

    def is_media_playing(self):
        app_state = self.last_message.get()
        return self._is_media_playing(app_state) if app_state is not None else False

    def wait_media_playing(self):
        return self.wait_for(
            lambda m: self._is_media_playing(m),
            fail_message="Awating any media playing on device {self.device_id} failed",
        )

    def wait_media_stopped(self):
        return self.wait_for(
            lambda m: not self._is_media_playing(m),
            fail_message="Awating no media playing on device {self.device_id} failed",
        )


class AppStates(object):
    def __init__(self, devices):
        self.app_states = [AppState(device) for device in devices]

    def __enter__(self):
        for app_state in self.app_states:
            app_state.__enter__()
        return self

    def __getitem__(self, index):
        return self.app_states[index]

    def __exit__(self, exc_type, exc_val, exc_tb):
        for app_state in self.app_states:
            app_state.__exit__(exc_type, exc_val, exc_tb)
