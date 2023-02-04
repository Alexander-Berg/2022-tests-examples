import yandex_io.protos.quasar_proto_pb2 as quasar_proto


class BluetoothEmulator(object):
    def __init__(self, testpoint, logger):
        self.testpoint = testpoint
        self.logger = logger

    def connect(self):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_bluetooth.connect.SetInParent()
        self.logger.log("Connecting to bluetooth")
        self.testpoint.send(msg)

    def disconnect(self):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_bluetooth.disconnect.SetInParent()
        self.logger.log("Disconnecting to bluetooth")
        self.testpoint.send(msg)

    def play(self):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_bluetooth.play.SetInParent()
        self.logger.log("Bluetooth player starts to play")
        self.testpoint.send(msg)

    def pause(self):
        msg = quasar_proto.QuasarMessage()
        msg.testpoint_message.emulated_bluetooth.pause.SetInParent()
        self.logger.log("Bluetooth player stops")
        self.testpoint.send(msg)


def is_bt_playing(m):
    return (
        m.HasField("app_state")
        and m.app_state.HasField("bluetooth_player_state")
        and not m.app_state.bluetooth_player_state.is_paused
    )


def is_bt_paused(m):
    return (
        m.HasField("app_state")
        and m.app_state.HasField("bluetooth_player_state")
        and m.app_state.bluetooth_player_state.is_paused
    )


def has_bt_meta_data(m):
    return (
        m.HasField("app_state")
        and m.app_state.HasField("bluetooth_player_state")
        and m.app_state.bluetooth_player_state.HasField("track_meta_info")
        and m.app_state.bluetooth_player_state.track_meta_info.HasField("title")
    )


def get_bt_song(m):
    return m.app_state.bluetooth_player_state.track_meta_info.title
