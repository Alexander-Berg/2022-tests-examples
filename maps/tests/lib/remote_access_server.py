from maps.automotive.libs.large_tests.lib.http import http_request_json, http_request

REMOTE_ACCESS_SERVER = 'http://127.0.0.1:9003'
HOST = 'auto-remote-access-server.maps.yandex.net'
PANDORA_INCOMING_TOKEN = 'fake-token'


def set_url(url):
    global REMOTE_ACCESS_SERVER
    REMOTE_ACCESS_SERVER = url


def get_url():
    return REMOTE_ACCESS_SERVER


def get_host():
    return HOST


def set_pandora_incoming_token(token):
    global PANDORA_INCOMING_TOKEN
    PANDORA_INCOMING_TOKEN = token


def get_phone(token):
    return http_request_json(
        'GET', REMOTE_ACCESS_SERVER + '/v1/auth/phone',
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def bind_phone(token, device_id=None):
    headers = {
        "Authorization": "OAuth " + token,
        'Host': HOST
    }
    if device_id:
        headers["X-Ya-Device-Id"] = device_id

    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/auth/phone/bind',
        headers=headers)


def unbind_phone(token):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/auth/phone/unbind',
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def confirm_phone(token, sms_code, device_id=None):
    headers = {
        "Authorization": "OAuth " + token,
        'Host': HOST
    }
    if device_id:
        headers["X-Ya-Device-Id"] = device_id

    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/auth/phone/confirm',
        headers=headers,
        json={"code": sms_code})


def get_cars(token):
    return http_request_json(
        'GET', REMOTE_ACCESS_SERVER + '/v1/cars',
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def get_car_settings(token, car, lang=None, lang_as_get_parameter=False):
    params = {}
    headers = {
        "Authorization": "OAuth " + token,
        'Host': HOST
    }
    if lang is not None:
        if lang_as_get_parameter:
            params['lang'] = lang
        else:
            headers['lang'] = lang
    return http_request_json(
        'GET', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings' % car.id,
        params=params,
        headers=headers)


def add_telematics(telematics):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/telematics',
        params={
            "hwid": telematics.hwid,
            "login": telematics.login,
            "password": telematics.password,
            "phone": telematics.phone,
            "pin": telematics.pin
        },
        headers={
            "Authorization": "OAuth " + PANDORA_INCOMING_TOKEN,
            'Host': HOST
        })


def add_car(token, hwid, car):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/telematics',
        json={
            "hwid": hwid,
            "car": {
                "brand": car.brand,
                "model": car.model,
                "plateNumber": car.plate,
                "year": car.year
            }
        },
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def delete_car(token, car):
    return http_request_json(
        'DELETE', REMOTE_ACCESS_SERVER + '/v1/cars/%s' % car.id,
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def add_notifications_phone(token, car, phone, name=''):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings/notifications/addPhone' % car.id,
        json={
            "phone": phone,
            "name": name
        },
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def confirm_notifications_phone(token, car, code):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings/notifications/addPhone/confirm' % car.id,
        json={
            "code": code
        },
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def delete_notifications_phone(token, car, phone_id):
    return http_request_json(
        'DELETE', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings/notifications/additionalPhones' % car.id,
        json={
            "id": phone_id
        },
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def share_car(token, car, name, phone):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/%s/share' % car.id,
        json={
            "phone": phone,
            "name": name
        },
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def confirm_share_phone(token, car, sms_code):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/%s/share/confirmPhone' % car.id,
        json={
            "code": sms_code,
        },
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def complete_share(token, car, phone):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/%s/share/complete' % car.id,
        json={
            "phone": phone,
        },
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def confirm_share(token, car, sms_code):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/%s/share/confirm' % car.id,
        json={
            "code": sms_code,
        },
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def delete_shared_access(token, car, access_id):
    return http_request_json(
        'DELETE', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings/sharedAccess' % car.id,
        json={
            "id": access_id,
        },
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def get_car(token, car):
    return http_request_json(
        'GET', REMOTE_ACCESS_SERVER + '/v1/cars/%s' % car.id,
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def get_supported_cars(token):
    return http_request_json(
        'GET', REMOTE_ACCESS_SERVER + '/v1/config/supportedCars',
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def get_car_events(token, car, command_timestamp, limit=None,
                   target=None, severity=None):
    params = {"timestamp": command_timestamp}
    if limit is not None:
        params['limit'] = limit
    if target is not None:
        params['target'] = target
    if severity is not None:
        params['severity'] = severity

    return http_request_json(
        'GET', REMOTE_ACCESS_SERVER + '/v1/cars/%s/events' % car.id,
        params=params,
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def set_autostart_voltage(token, car, voltage=None, is_enabled=None):
    request_json = {}
    if voltage is not None:
        request_json['value'] = voltage
    if is_enabled is not None:
        request_json['isEnabled'] = is_enabled
    return http_request_json(
        'PATCH', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings/autostart/voltage' % car.id,
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        },
        json=request_json)


def set_autostart_temperature(token, car, temperature=None, is_enabled=None):
    request_json = {}
    if temperature is not None:
        request_json['value'] = temperature
    if is_enabled is not None:
        request_json['isEnabled'] = is_enabled
    return http_request_json(
        'PATCH', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings/autostart/temperature' % car.id,
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        },
        json=request_json)


def set_autostart_interval(token, car, interval=None, is_enabled=None):
    request_json = {}
    if interval is not None:
        request_json['valueSeconds'] = interval
    if is_enabled is not None:
        request_json['isEnabled'] = is_enabled
    return http_request_json(
        'PATCH', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings/autostart/interval' % car.id,
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        },
        json=request_json)


def set_engine_stop_time(token, car, stop_time):
    request_json = {
        'timeout': {
            'currentSeconds': stop_time
        }
    }
    return http_request_json(
        'PATCH', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings/engine/stop' % car.id,
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        },
        json=request_json)


def create_schedule(token, car, schedule):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings/autostart/schedule' % car.id,
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        },
        json=schedule)


def edit_schedule(token, car, schedule, schedule_id=None):
    schedule_id = schedule_id or schedule['id']
    return http_request_json(
        'PATCH', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings/autostart/schedule/%d' % (car.id, schedule_id),
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        },
        json=schedule)


def delete_schedule(token, car, schedule_id):
    return http_request_json(
        'DELETE', REMOTE_ACCESS_SERVER + '/v1/cars/%s/settings/autostart/schedule/%d' % (car.id, schedule_id),
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def patch_notification_settings(token, car, settings):
    return http_request_json(
        'PATCH', REMOTE_ACCESS_SERVER + "/v1/cars/%s/settings/notifications/channels" % car.id,
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        },
        json=settings)


def execute_command(token, car, command):
    return http_request_json(
        'POST',
        REMOTE_ACCESS_SERVER + '/v1/cars/' + car.id + command,
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def start_engine(token, car):
    return execute_command(token, car, '/engine/start')


def stop_engine(token, car):
    return execute_command(token, car, '/engine/stop')


def lock(token, car):
    return execute_command(token, car, '/lock')


def unlock(token, car):
    return execute_command(token, car, '/unlock')


def open_trunk(token, car):
    return execute_command(token, car, '/trunk/open')


def close_trunk(token, car):
    return execute_command(token, car, '/trunk/close')


def turn_service_mode_on(token, car):
    return execute_command(token, car, '/serviceMode/on')


def turn_service_mode_off(token, car):
    return execute_command(token, car, '/serviceMode/off')


def sell_car(token, car, name, phone):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/%s/sell' % car.id,
        json={"phone": phone},
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def confirm_sell_phone(token, car, sms_code):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/%s/sell/confirmPhone' % car.id,
        json={"code": sms_code},
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def complete_sell(token, car, phone):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/%s/sell/complete' % car.id,
        json={"phone": phone},
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def confirm_sell(token, car, sms_code):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/cars/%s/sell/confirm' % car.id,
        json={"code": sms_code},
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })


def alice_ping():
    return http_request(
        'HEAD', REMOTE_ACCESS_SERVER + '/alice/v1.0',
        headers={'Host': HOST})


def alice_get_devices(user, request_id, service_ticket, device_id=None):
    return http_request_json(
        'GET', REMOTE_ACCESS_SERVER + '/alice/v1.0/user/devices',
        headers={
            'Host': HOST,
            "X-Ya-Service-Ticket": service_ticket,
            "X-Ya-User-Id": user.oauth,
            "X-Request-Id": request_id,
            "X-Ya-Device-Id": device_id or ""  # alice will send us empty DeviceId in callback scenario
        })


def alice_query_devices(user, devices, request_id):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/alice/v1.0/user/devices/query',
        json=devices,
        headers={
            'Host': HOST,
            "X-Ya-User-Ticket": user.oauth,
            "X-Ya-Device-Id": user.device_id,
            "X-Request-Id": request_id
        })


def alice_action(user, payload, request_id):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/alice/v1.0/user/devices/action',
        json=payload,
        headers={
            'Host': HOST,
            "X-Ya-User-Ticket": user.oauth,
            "X-Ya-Device-Id": user.device_id,
            "X-Request-Id": request_id
        })


def subscribe(token, info):
    return http_request_json(
        'POST', REMOTE_ACCESS_SERVER + '/v1/push/subscribe',
        json=info,
        headers={
            "Authorization": "OAuth " + token,
            'Host': HOST
        })
