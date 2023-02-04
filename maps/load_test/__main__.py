#! /usr/bin/env python

import argparse


def write_request(f, request):
    f.write(str(len(request)) + '\n')
    f.write(request + '\n')

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate ammo for load testing of remote car api")
    parser.add_argument("--output-file", dest="output_file", required=True)
    parser.add_argument("--host", dest="host", required=True)
    parser.add_argument("--car_ids", dest="car_ids", required=True)
    parser.add_argument("--token", dest="token", required=True)
    args = parser.parse_args()

    REQUEST = ("{request}\r\n"
               "{headers}\r\n\r\n"
               "{data}")

    HEADERS = ("Host: {host}\r\n"
               "Authorization: OAuth {token}\r\n"
               "Content-Length: {data_length}")

    ALL_CARS_INFO_REQUEST = (
        """GET /v1/cars HTTP/1.1""")

    SUPPORTED_CARS_REQUEST = (
        """GET /v1/config/supportedCars HTTP/1.1""")

    USER_PHONE_REQUEST = (
        """GET /v1/auth/phone HTTP/1.1""")

    CAR_INFO_REQUEST = (
        """GET /v1/cars/{car_id} HTTP/1.1""")

    CAR_SETTINGS_REQUEST = (
        """GET /v1/cars/{car_id}/settings HTTP/1.1""")

    CAR_EVENTS_REQUEST = (
        """GET /v1/cars/{car_id}/events?timestamp=2019-07-26T12%3A18%3A56.197%2B0000 HTTP/1.1""")

    CAR_LOCK_REQUEST = (
        """POST /v1/cars/{car_id}/lock HTTP/1.1""")

    CAR_UNLOCK_REQUEST = (
        """POST /v1/cars/{car_id}/unlock HTTP/1.1""")

    CAR_TRUNK_OPEN_REQUEST = (
        """POST /v1/cars/{car_id}/trunk/open HTTP/1.1""")

    CAR_TRUNK_CLOSE_REQUEST = (
        """POST /v1/cars/{car_id}/trunk/close HTTP/1.1""")

    CAR_BLINK_REQUEST = (
        """POST /v1/cars/{car_id}/blink HTTP/1.1""")

    CAR_PATCH_VOLTAGE_REQUEST = (
        """PATCH /v1/cars/{car_id}/settings/autostart/voltage HTTP/1.1""")

    CAR_PATCH_TEMPERATURE_REQUEST = (
        """PATCH /v1/cars/{car_id}/settings/autostart/temperature HTTP/1.1""")

    CAR_PATCH_INTERVAL_REQUEST = (
        """PATCH /v1/cars/{car_id}/settings/autostart/interval HTTP/1.1""")

    CAR_PATCH_ENGINE_STOP_REQUEST = (
        """PATCH /v1/cars/{car_id}/settings/engine/stop HTTP/1.1""")

    CAR_REQUESTS = [
        (ALL_CARS_INFO_REQUEST, ''),
        (SUPPORTED_CARS_REQUEST, ''),
        (USER_PHONE_REQUEST, ''),
        (CAR_INFO_REQUEST, ''),
        (CAR_SETTINGS_REQUEST, ''),
        (CAR_EVENTS_REQUEST, ''),
        (CAR_LOCK_REQUEST, ''),
        (CAR_UNLOCK_REQUEST, ''),
        (CAR_TRUNK_OPEN_REQUEST, ''),
        (CAR_TRUNK_CLOSE_REQUEST, ''),
        (CAR_BLINK_REQUEST, ''),
        (CAR_PATCH_VOLTAGE_REQUEST, '{"isEnabled":true,"value":11.8}'),
        (CAR_PATCH_TEMPERATURE_REQUEST, '{"isEnabled":true,"value":-20}'),
        (CAR_PATCH_INTERVAL_REQUEST, '{"isEnabled":true,"value":14400}'),
        (CAR_PATCH_ENGINE_STOP_REQUEST, '{"timeout":{"currentSeconds":1200}}')
    ]

    with open(args.output_file, 'w') as f:
        for car_id in args.car_ids.split(','):
            for request, data in CAR_REQUESTS:
                write_request(
                    f,
                    REQUEST.format(
                        request=request.format(
                            car_id=car_id),
                        headers=HEADERS.format(
                            host=args.host,
                            token=args.token,
                            data_length=len(data)),
                        data=data))

        f.write('0')
