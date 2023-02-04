import yandex_io.protos.model_objects_pb2 as P
from yandex_io.pylibs.functional_tests.utils import test_data_path, common_data_path, regression, tus_mark
from yandex_io.pylibs.functional_tests.matchers import (
    is_music_playing,
    is_music_stop,
    has_radio_state,
    has_output_speech,
    has_music_state,
    is_radio_playing,
    is_radio_stop,
    has_audio_player_state,
)
from testlib import (
    cancel_all_alarms,
    check_alarm_stopped,
    set_alarm,
    check_alarm_fired,
    check_alarm_started,
    has_media_state,
)
import pytest
import json
import re

tus = tus_mark()


def get_music_title(app_state):
    if not app_state.music_state.is_paused:
        return app_state.music_state.title
    else:
        return app_state.audio_player_state.audio.metadata.title


def get_radio_id(app_state):
    return app_state.radio_state.radio_title


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2255")
@regression
@pytest.mark.with_yandex_plus
def test_media_alarm(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("aliced") as aliced, device.get_service_connector("alarmd") as alarmd:

        cancel_all_alarms(device, alarmd)

        # set song for alarm
        device.start_conversation()
        device.say_to_mic(test_data_path("set_song.wav"))

        rus_title = "Богемская рапсодия"
        eng_title = "Bohemian Rhapsody"

        # check alarm media settings have song
        media_alarm_settings = json.loads(
            device.wait_for_message(
                alarmd, has_media_state, "Media was not set on alarm"
            ).alarms_state.media_alarm_setting
        )["sound_alarm_setting"]
        device.failer.assert_fail(
            media_alarm_settings["type"] == "music",
            "Wrong media alarm type. Expected: {}, Actual: {}".format("music", media_alarm_settings["type"]),
        )
        device.failer.assert_fail(
            media_alarm_settings["info"]["title"] == rus_title or media_alarm_settings["info"]["title"] == eng_title,
            "Wrong song was set on media alarm. Expected: {}, Actual: {}".format(
                rus_title, media_alarm_settings["info"]["title"]
            ),
        )

        device.stage_logger.test_stage("Song {} was set to alarm".format(rus_title))

        # set alarm
        alarm, _ = set_alarm(device, alarmd, "alarm_in_20sec.wav", 20)

        # check alarm fired
        check_alarm_fired(alarmd, alarm, device)

        # check alarm started
        alarm_message = check_alarm_started(alarmd, alarm, device)
        device.failer.assert_fail(
            alarm_message.alarm_type == P.Alarm.AlarmType.MEDIA_ALARM, "Invalid alarm type. Expected media alarm"
        )

        alarm_app_state = device.wait_for_message(aliced, is_music_playing, "Music failed to start").app_state

        title = get_music_title(alarm_app_state)
        device.failer.assert_fail(
            title == rus_title or title == eng_title,
            "Wrong song started to play on media alarm. Expected: {}, Actual: {}".format([rus_title, eng_title], title),
        )

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        device.wait_for_message(aliced, is_music_stop, "Music should stop")

        # Check that alarm was actually stopped
        alarm_message = check_alarm_stopped(alarmd, device)
        device.failer.assert_fail(
            alarm_message.alarm_type == P.Alarm.AlarmType.MEDIA_ALARM, "Invalid alarm type. Expected media alarm"
        )


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2128")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2130")
@regression
@pytest.mark.with_yandex_plus
@pytest.mark.parametrize(
    "media",
    [
        # (media_type, wav, media_id getter (song title, radio name, etc))
        ("music", "play_music.wav", get_music_title),
        ("radio", "play_radio_dfm.wav", get_radio_id),
    ],
)
def test_set_media_alarm_while_media_playing(device, media):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()
    media_type, wav, id_getter = media
    with device.get_service_connector("aliced") as aliced, device.get_service_connector("alarmd") as alarmd:

        cancel_all_alarms(device, alarmd)

        # start media playing

        device.start_conversation()
        device.say_to_mic(test_data_path(wav))

        init_app_state = device.wait_for_message(
            aliced, lambda m: is_music_playing(m) or is_radio_playing(m), "{} should start to play".format(media_type)
        ).app_state
        media_id = id_getter(init_app_state)

        device.stage_logger.test_stage("{} started to play".format(media_type))

        # set playing song

        device.start_conversation()
        device.say_to_mic(test_data_path("set_media_on_alarm.wav"))

        # check update alarm media settings

        media_alarm_settings = json.loads(
            device.wait_for_message(
                alarmd, has_media_state, "Media was not set on alarm"
            ).alarms_state.media_alarm_setting
        )["sound_alarm_setting"]
        device.failer.assert_fail(
            media_alarm_settings["type"] == media_type,
            "Wrong media alarm type. Expected: {}, Actual: {}".format(media_type, media_alarm_settings["type"]),
        )
        device.failer.assert_fail(
            media_alarm_settings["info"]["title"] == media_id,
            "Wrong song was set on media alarm. Expected: {}, Actual: {}".format(
                media_id, media_alarm_settings["info"]["title"]
            ),
        )

        device.stage_logger.test_stage(
            "current playing song: \"{}\" was set to alarm".format(media_alarm_settings["info"]["title"])
        )

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        device.wait_for_message(
            aliced,
            lambda m: True
            if not (has_music_state(m) or has_audio_player_state(m))
            else is_music_stop(m) and True
            if not has_radio_state(m)
            else is_radio_stop(m),
            "{} should stop".format(media_type),
        )

        device.stage_logger.test_stage("{} stopped".format(media_type))


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2365")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2366")
@regression
@pytest.mark.with_yandex_plus
@pytest.mark.parametrize(
    "media",
    [
        # (media_type, wav)
        ("music", "set_alarm_with_song.wav"),
        ("radio", "set_alarm_with_radio.wav"),
    ],
)
def test_set_media_on_alarm_one_request(device, media):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    media_type, wav = media

    with device.get_service_connector("alarmd") as alarmd:

        cancel_all_alarms(device, alarmd)

        _, alarmd_message = set_alarm(device, alarmd, wav, 600)
        if has_media_state(alarmd_message):
            media_settings = json.loads(alarmd_message.alarms_state.media_alarm_setting)["sound_alarm_setting"]
        else:
            media_settings = json.loads(
                device.wait_for_message(
                    alarmd, has_media_state, "Media was not set on alarm"
                ).alarms_state.media_alarm_setting
            )["sound_alarm_setting"]

        device.failer.assert_fail(
            media_settings["type"] == media_type,
            "Wrong media alarm type. Expected: {}, Actual: {}".format(media_type, media_settings["type"]),
        )

        device.stage_logger.test_stage("Set media alarm in one request")
        cancel_all_alarms(device, alarmd)


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2132")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2133")
@regression
@pytest.mark.with_yandex_plus
def test_change_track(device):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()
    with device.get_service_connector("aliced") as aliced, device.get_service_connector("alarmd") as alarmd:

        cancel_all_alarms(device, alarmd)

        # play music

        device.start_conversation()
        device.say_to_mic(test_data_path("play_music.wav"))

        init_app_state = device.wait_for_message(aliced, is_music_playing, "Music should start to play").app_state
        title = get_music_title(init_app_state)

        device.stage_logger.test_stage("Start music")

        # set playing song

        device.start_conversation()
        device.say_to_mic(test_data_path("set_media_on_alarm.wav"))

        # check update alarm media settings

        media_alarm_settings = json.loads(
            device.wait_for_message(
                alarmd, has_media_state, "Failed to receive media alarm setting"
            ).alarms_state.media_alarm_setting
        )["sound_alarm_setting"]
        device.failer.assert_fail(
            media_alarm_settings["type"] == "music",
            "Wrong media alarm type. Expected: {}, Actual: {}".format("music", media_alarm_settings["type"]),
        )
        device.failer.assert_fail(
            media_alarm_settings["info"]["title"] == title,
            "Wrong song was set on media alarm. Expected: {}, Actual: {}".format(
                title, media_alarm_settings["info"]["title"]
            ),
        )

        device.stage_logger.test_stage("Set playing song on alarm")

        # set next song

        device.start_conversation()
        device.say_to_mic(test_data_path("next.wav"))

        new_app_state = device.wait_for_message(
            aliced,
            lambda m: is_music_playing(m) and get_music_title(m.app_state) != title,
            "Music should change after next command",
        ).app_state
        title = get_music_title(new_app_state)

        device.stage_logger.test_stage("Skip track")

        device.start_conversation()
        device.say_to_mic(test_data_path("set_media_on_alarm.wav"))

        # check song changed in alarm media settings

        media_alarm_settings = json.loads(
            device.wait_for_message(
                alarmd, has_media_state, "Failed to recieve alarm media settings"
            ).alarms_state.media_alarm_setting
        )["sound_alarm_setting"]
        device.failer.assert_fail(
            media_alarm_settings["type"] == "music",
            "Wrong media alarm type. Expected: {}, Actual: {}".format("music", media_alarm_settings["type"]),
        )
        device.failer.assert_fail(
            media_alarm_settings["info"]["title"] == title,
            "Wrong song was set on media alarm. Expected: {}, Actual: {}".format(
                title, media_alarm_settings["info"]["title"]
            ),
        )

        device.stage_logger.test_stage("Change media alarm track")

        device.start_conversation()
        device.say_to_mic(common_data_path("enough.wav"))

        device.wait_for_message(aliced, is_music_stop, "Music should stop")

        # delete song from alarm and check clear alarm media settings

        device.start_conversation()
        device.say_to_mic(test_data_path("set_default.wav"))
        device.wait_for_message(
            alarmd,
            lambda m: m.HasField("alarms_state") and not m.alarms_state.HasField("media_alarm_setting"),
            "Alarm sound should fallback to default",
        )

        device.stage_logger.test_stage("Set alarm sound back to default")


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2255")
@regression
@pytest.mark.with_yandex_plus
@pytest.mark.parametrize(
    "media_meta_info",
    [
        # (regexp to find in vins_response, wav, bool func to check alarm media settings)
        (
            [r"Bohemian Rhapsody", r"Богемская рапсодия"],
            "set_song.wav",
            lambda settings: settings["info"]["title"] == "Bohemian Rhapsody"
            or settings["info"]["title"] == "Богемская рапсодия",
        ),
        ([r"рок"], "set_genre.wav", lambda settings: settings["info"]["filters"]["genre"] == "rock"),
        (
            [r"Nirvana"],
            "set_artist.wav",
            lambda settings: any(
                map(lambda item: item.get("name", "") == "Nirvana", settings["info"]["first_track"]["artists"])
            ),
        ),
        (
            [r"энергичн.. музык."],
            "set_energetic_music.wav",
            lambda settings: settings["info"]["filters"]["mood"] == "energetic",
        ),
    ],
)
def test_alice_response_on_media_alarm(device, media_meta_info):
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()
    key_words, wav, matcher = media_meta_info
    with device.get_service_connector("aliced") as aliced, device.get_service_connector("alarmd") as alarmd:

        cancel_all_alarms(device, alarmd)

        aliced.clear_message_queue()
        alarmd.clear_message_queue()

        # set something on alarm

        device.start_conversation()
        device.say_to_mic(test_data_path(wav))

        device.wait_for_message(
            aliced,
            lambda m: has_output_speech(m)
            and any(map(lambda key_word: re.search(key_word, m.alice_state.vins_response.output_speech), key_words)),
            "Alice respond to {} was incorrect. Used key words: {}".format(wav, key_words),
        )

        # wait alice to answer about successful setting it

        media_alarm_settings = json.loads(
            device.wait_for_message(
                alarmd, has_media_state, "Failed to recieve alarm media settings"
            ).alarms_state.media_alarm_setting
        )["sound_alarm_setting"]
        device.failer.assert_fail(
            media_alarm_settings["type"] == "music",
            "Wrong media alarm type. Expected: {}, Actual: {}".format("music", media_alarm_settings["type"]),
        )
        device.failer.assert_fail(matcher(media_alarm_settings), "Matching media meta info failed")

        device.stage_logger.test_stage("Set {} on alarm".format(key_words[0]))


@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2246")
@pytest.mark.testpalm("https://testpalm.yandex-team.ru/alice/testsuite/5d09de6f3d42cb5e76b4cf06?testcase=2369")
@tus
@regression
@pytest.mark.no_plus
def test_media_alarm_no_plus(device):

    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("alarmd") as alarmd, device.get_service_connector("aliced") as aliced:

        cancel_all_alarms(device, alarmd)

        aliced.clear_message_queue()
        alarmd.clear_message_queue()

        set_alarm(device, alarmd, "set_alarm_with_song.wav", 600)

        device.wait_for_message(
            aliced,
            lambda m: has_output_speech(m) and re.search("Яндекс.Плюс", m.alice_state.vins_response.output_speech),
            "Alice should answer about Yandex.Plus",
        )

        device.stage_logger.test_stage("Alice respond that Yandex.Plus is required to settings media on alarm")

        cancel_all_alarms(device, alarmd)
