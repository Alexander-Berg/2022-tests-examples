from testlib import BluetoothEmulator, is_bt_playing, is_bt_paused, has_bt_meta_data, get_bt_song
from yandex_io.pylibs.functional_tests.utils import regression, test_data_path, tus_mark, sample_app_mark
from yandex_io.pylibs.functional_tests.matchers import is_music_playing, is_music_stop, is_radio_playing, is_radio_stop
import pytest

tus = tus_mark()
sample_app = sample_app_mark()


@tus
@sample_app
@regression
def test_bluetooth_voice_control(device):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    with device.get_service_connector("testpoint") as testpoint, device.get_service_connector("aliced") as aliced:
        bluetooth_emulator = BluetoothEmulator(testpoint, device.stage_logger)

        bluetooth_emulator.connect()
        bluetooth_emulator.play()

        device.wait_for_message(aliced, is_bt_playing, "Bluetooth didn't start to play")
        song = get_bt_song(device.wait_for_message(aliced, lambda m: has_bt_meta_data(m), "Failed to receive bt meta"))

        device.start_conversation()
        device.say_to_mic(test_data_path("next.wav"))

        device.wait_for_message(
            aliced, lambda m: has_bt_meta_data(m) and get_bt_song(m) != song, "Failed to get next track"
        )

        device.start_conversation()
        device.say_to_mic(test_data_path("prev.wav"))

        device.wait_for_message(
            aliced, lambda m: has_bt_meta_data(m) and get_bt_song(m) == song, "Failed to get prev track"
        )

        device.start_conversation()
        device.say_to_mic(test_data_path("pause.wav"))

        device.wait_for_message(aliced, is_bt_paused, "Failed to pause bt")

        device.start_conversation()
        device.say_to_mic(test_data_path("play.wav"))

        device.wait_for_message(aliced, is_bt_playing, "Failed to resume bt")


@pytest.mark.parametrize(
    "media", [("play_music.wav", is_music_playing, is_music_stop), ("play_radio.wav", is_radio_playing, is_radio_stop)]
)
@tus
@sample_app
@regression
@pytest.mark.with_yandex_plus
def test_bluetooth_media_switching(device, media):
    device.wait_for_authenticate_completion()
    device.wait_for_listening_start()

    wav, play_matcher, stop_matcher = media

    with device.get_service_connector("testpoint") as testpoint, device.get_service_connector("aliced") as aliced:

        bluetooth_emulator = BluetoothEmulator(testpoint, device.stage_logger)

        bluetooth_emulator.connect()
        bluetooth_emulator.play()

        device.wait_for_message(aliced, is_bt_playing, "Bluetooth didn't start to play")

        device.start_conversation()
        device.say_to_mic(test_data_path(wav))

        device.wait_for_messages(aliced, [play_matcher, is_bt_paused], "Media failed to break in")

        bluetooth_emulator.play()

        device.wait_for_messages(aliced, [is_bt_playing, stop_matcher], "Bluetooth failed to break in")
