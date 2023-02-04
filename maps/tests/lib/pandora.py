from maps.automotive.libs.large_tests.lib.http import http_request_json
import logging

PANDORA_EMULATOR = 'http://127.0.0.1:9005'
HOST = 'auto-lab-pandora-emulator.maps.yandex.net'
HEADERS = {
    "content-type": "application/x-www-form-urlencoded",
    "Host": HOST
}

logger = logging.getLogger("pandora")


def set_url(url):
    global PANDORA_EMULATOR
    PANDORA_EMULATOR = url


def get_url():
    return PANDORA_EMULATOR


def get_host():
    return HOST


def add_car(telematics):
    return http_request_json(
        'POST', PANDORA_EMULATOR + '/_service/car_and_user',
        data={
            "car_id": telematics.login,
            "user_id": telematics.login,
            "user_password": telematics.password
        },
        headers=HEADERS)


def remove_car(telematics):
    return http_request_json(
        'POST', PANDORA_EMULATOR + '/_service/delete_user',
        data={
            "user_id": telematics.login
        },
        headers=HEADERS)


def set_car_online(telematics, online=True):
    return http_request_json(
        'POST', PANDORA_EMULATOR + '/_service/car_info',
        data={
            "car_id": telematics.login,
            "isOnline": str(online).lower()
        },
        headers=HEADERS)


def get_token(telematics):
    status, response = http_request_json(
        'GET', PANDORA_EMULATOR + '/_service/tokens',
        headers={'host': HOST}
    )
    assert status == 200, response
    assigned_cars = response['assigned_cars']
    return next(token for token in assigned_cars if str(assigned_cars[token]) == telematics.login)


def get_settings(telematics, token=None):
    token = token or get_token(telematics)
    return http_request_json(
        'GET', PANDORA_EMULATOR + '/api/devices/settings',
        params={
            'access_token': token,
            'id': telematics.login
        },
        headers={'host': HOST})


def get_setting(telematics, setting):
    status, response = get_settings(telematics)
    assert status == 200, response
    return response['device_settings'][telematics.login][0]['alarm_settings'][setting]


def get_phone(telematics, phone='phone_2'):
    return get_setting(telematics, phone)


def set_settings(telematics, settings, token=None):
    token = token or get_token(telematics)
    data = {
        'access_token': token,
        'id': telematics.login,
    }
    data.update(settings)
    return http_request_json(
        'PUT', PANDORA_EMULATOR + '/api/devices/settings',
        data=data,
        headers=HEADERS)


def get_bit_state(telematics):
    response = http_request_json(
        'GET', PANDORA_EMULATOR + '/_service/bit_state',
        params={
            "car_id": telematics.login
        },
        headers=HEADERS) >> 200

    return response["bitState"]


def get_tokens():
    response = http_request_json(
        'GET', PANDORA_EMULATOR + '/_service/tokens',
        headers=HEADERS) >> 200
    return response


def mark_token_expired(token):
    response = http_request_json(
        'POST', PANDORA_EMULATOR + '/_service/expiredToken',
        data={"token": token},
        headers=HEADERS) >> 200
    return response


def set_bit_state(telematics, new_bit_state):
    return http_request_json(
        'POST', PANDORA_EMULATOR + '/_service/car_info',
        data={
            "car_id": telematics.login,
            "bitState": str(new_bit_state)
        },
        headers=HEADERS)


def set_default_state(telematics):
    return set_bit_state(telematics, 1 | 1 << 7 | 1 << 8 | 1 << 9 | 1 << 27)


def set_bit_state_specific_value(telematics, bit_number, value):
    started_bit_state = get_bit_state(telematics)
    logger.info(str(started_bit_state))
    truncated_bit_state = started_bit_state & (~(1 << bit_number))
    logger.info(str(~(1 << bit_number)))
    logger.info(str(truncated_bit_state))
    new_bit_state = truncated_bit_state | (value << bit_number)
    logger.info(str(new_bit_state))
    return set_bit_state(telematics, new_bit_state)


def set_engine(telematics, is_on):
    engine_bit = 2
    return set_bit_state_specific_value(telematics, engine_bit, is_on)


def set_alarm(telematics, is_set):
    alarm_bit = 1
    return set_bit_state_specific_value(telematics, alarm_bit, is_set)


def set_lock(telematics, is_locked):
    lock_bit = 0
    return set_bit_state_specific_value(telematics, lock_bit, is_locked)


def set_front_left_door(telematics, is_open):
    front_left_door_bit = 21
    return set_bit_state_specific_value(telematics, front_left_door_bit, is_open)


def set_front_right_door(telematics, is_open):
    front_right_door_bit = 22
    return set_bit_state_specific_value(telematics, front_right_door_bit, is_open)


def set_back_left_door(telematics, is_open):
    back_left_door_bit = 23
    return set_bit_state_specific_value(telematics, back_left_door_bit, is_open)


def set_back_right_door(telematics, is_open):
    back_right_door_bit = 24
    return set_bit_state_specific_value(telematics, back_right_door_bit, is_open)


def set_trunk(telematics, is_open):
    trunk_bit = 25
    return set_bit_state_specific_value(telematics, trunk_bit, is_open)


def set_hood(telematics, is_open):
    hood_bit = 26
    return set_bit_state_specific_value(telematics, hood_bit, is_open)


def set_service_mode(telematics, is_on):
    service_mode_bit = 34
    return set_bit_state_specific_value(telematics, service_mode_bit, is_on)
