# -*- coding: utf-8 -*-
import ssl
# from typing import Optional, Dict, List

import requests
import json
from enum import Enum
import logging
from datetime import datetime as dt
import time
from btestlib import secrets

logging.basicConfig(format="%(levelname)s:%(message)s", level=logging.INFO)
ssl._create_default_https_context = ssl._create_unverified_context
# your OAuth token
# TOKEN = settings.TOKEN

TOKEN = secrets.get_secret(*secrets.Tokens.TESTPALM_OAUTH_TOKEN)
# Path to certificate if executed locally
PATH_TO_CERTIFICATE = "ENTER_PATH_TO_CERTIFICATE_HERE"
# name of quota. can be found by https://booking.yandex-team.ru/#/help?section=api-quotas
QUOTA_CODE = "qs_testpalm_separate_balance_ru"
BASE_URL = "https://booking.yandex-team.ru/"
# max acceptable quota (in working hours) or None if does not matter
MAX_QUOTA = None  # todo: enter your max quota or None here (Optional[float])
# do not change
PRICE_PER_WH = 120.0
# max acceptable duration of task in hours
MAX_DURATION = None  # todo: enter your max duration for booking (Optional[float])
# max ratio between actual duration and MAX_DURATION. 1.2 means 120% of MAX_DURATION is ok but no more
DURATION_MAX_RATIO = 1.2  # todo: enter your appropriate ratio here

# todo: choose start dates here
# time ranges in which you are ok to start booking
acceptable_time_ranges = [
    {
        "startTs": int(time.mktime(dt(2020, 8, 3, 12, 0, 0).timetuple())*1000),
        "endTs": int(time.mktime(dt(2020, 8, 3, 16, 0, 0).timetuple())*1000),
    },
    # {
    #     "startTs": int(time.mktime(dt(2020, 8, 3, 14, 0, 0).timetuple())*1000),
    #     "endTs": int(time.mktime(dt(2020, 8, 3, 16, 0, 0).timetuple())*1000),
    # },
    {
        "startTs": int(time.mktime(dt(2020, 8, 3, 18, 0, 0).timetuple())*1000),
        "endTs": int(time.mktime(dt(2020, 8, 3, 20, 0, 0).timetuple())*1000),
    },
]


# type of speed
# slow is for bookings with low priority
# normal - for everyday bookings
# urgent - for extremely fast bookings with short deadline (higher costs)
class Speed(Enum):
    SLOW = 0
    NORMAL = 1
    URGENT = 2

    def lower(self):
        if self == Speed.URGENT:
            return Speed.NORMAL
        elif self == Speed.NORMAL:
            return Speed.SLOW
        else:
            raise Exception("The speed mode is already the slowest")

    def higher(self):
        if self == Speed.SLOW:
            return Speed.NORMAL
        elif self == Speed.NORMAL:
            return Speed.URGENT
        else:
            raise Exception("The speed mode is already the highest possible.")


def get_next_range(start_ts):
    for item in acceptable_time_ranges:
        if item["startTs"] > start_ts:
            return item
    return None


def get_time_range(start_ts):
    for item in acceptable_time_ranges:
        if item["startTs"] == start_ts:
            return item


def sort_time_ranges(time_ranges):
    return sorted(time_ranges, key=lambda k: k["startTs"], reverse=True)


def validate_time_ranges(time_ranges):
    for time_range in time_ranges:
        if time_range["startTs"] >= time_range["endTs"]:
            raise Exception(
                "Time range has endTs "
                + str(time_range["endTs"])
                + " is less than startTs "
                + str(time_range["startTs"])
            )
    if len(time_ranges) == 0:
        raise Exception("Time ranges cannot be empty")


def create_booking(title, params, estimation):
    url = BASE_URL + "api/bookings/assessor-testing/default"
    booking_data = {"title": title, "params": params, "estimate": estimation}
    logging.info("Attempt to create: %s", booking_data)
    response = requests.post(
        url,
        data=json.dumps(booking_data),
        headers={
            "Authorization": "OAuth " + TOKEN,
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
        verify=False,
        timeout=40,
    )
    if response.status_code == 200:
        logging.info("Created booking: %s", response.json())
        return response.json()
    else:
        logging.error(response.status_code, response.json())
        return None


def check_duration(body, estimate_result):
    logging.info("Checking duration...")
    current_duration = (
        (estimate_result["deadlineTs"] - estimate_result["startTs"])
        / 1000.0
        / 60.0
        / 60.0
    )

    if (
        MAX_DURATION is not None
        and MAX_DURATION != 0
        and current_duration / MAX_DURATION > DURATION_MAX_RATIO
    ):
        logging.info(
            "Duration is %s with MAX_DURATION %s", current_duration, MAX_DURATION
        )
        current_speed = Speed[body["speedMode"]].higher()
        logging.info(
            "Speed mode was changed from %s to %s",
            body["speedMode"],
            current_speed.name,
        )
        body["speedMode"] = current_speed.name
        return False, body
    else:
        logging.info("Duration is ok!")
        return True, body


def check_max_quota(body, estimate_result):
    logging.info("Checking max quota...")
    if MAX_QUOTA is not None and PRICE_PER_WH * MAX_QUOTA < estimate_result["minQuota"]:
        diff_quota = MAX_QUOTA * PRICE_PER_WH - estimate_result["minQuota"]
        logging.info("Quota is more than %s", abs(diff_quota))
        current_speed = Speed[body["speedMode"]].lower()
        logging.info(
            "Speed mode was changed from %s to %s",
            body["speedMode"],
            current_speed.name,
        )
        body["speedMode"] = current_speed.name
        return False, body
    else:
        logging.info("Max quota is ok!")
        return True, body


def check_time_range(body, estimate_result):
    logging.info("Checking start time...")
    time_range = get_time_range(body["startTsFrom"])
    if (
        time_range["startTs"] < estimate_result["startTs"]
        or estimate_result["startTs"] > time_range["endTs"]
    ):
        logging.info("StartsTs was moved to %s", estimate_result["startTs"])
        next_time_range = get_next_range(body["startTsFrom"])
        if next_time_range is None:
            raise Exception("No acceptable time periods!")
        body["startTsFrom"] = next_time_range["startTs"]
        logging.info("Changed startTs is %s", body["startTsFrom"])
        return False, body
    else:
        logging.info("StartTs is ok!")
        return True, body


def estimate(title, body):
    """
    tries to estimate time and checks with parameters given
    checks with following logic:
    1. first tries to booking in the beginning of the earliest time period.
    2. if start time is not in the earliest time range, tries to booking to the next time range or throws Exception
    3. if start time is ok, it checks task duration. If MAX_DURATION is set and duration is more than max,
    the speed mode is increased. If speed mode is already the highest, then exception is raised.
    4. If duration is ok, it checks max quota. If MAX_QUOTA is set and is more than max, it tries to set lower speed.
    Id the speed mode is the lowest, then it raises exception.
    5. If all parameters are ok, then the booking is created.
    :param title: name of the booking
    :param body: parameters to estimate according to type
    :return: booking or None
    """
    logging.info("Estimating ", body)
    url = BASE_URL + "api/bookings/assessor-testing/default/estimate"
    response = requests.post(
        url,
        data=json.dumps(body),
        headers={
            "Authorization": "OAuth " + TOKEN,
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
        verify=False,
        timeout=40,
    )
    if response.status_code != 200:
        logging.info(response.status_code)
        logging.info(response.json())
        return None

    estimate_result = response.json()
    logging.info("Estimated result %s", estimate_result)
    print(body['startTsFrom'])
    print(estimate_result['startTs'])
    print(estimate_result['deadlineTs'])

    is_time_ok, body = check_time_range(body, estimate_result)

    if not is_time_ok:
        estimate(title, body)
    else:
        is_duration_ok, estimate_request = check_duration(body, estimate_result)
        if not is_duration_ok:
            estimate(title, estimate_request)
        else:
            is_quota_ok, estimate_request = check_max_quota(body, estimate_result)
            if not is_quota_ok:
                estimate(title, estimate_request)
            else:
                return create_booking(title, body, estimate_result)


def book_custom_volume(
    title,
    volume_amount,
    environment_distribution,
    speed,
    start_ts,
):
    """

    :param title: name of the booking
    :param volume_amount: additional volume for the booking in БО
    :param environment_distribution: distribution of environments that would be used with rate.
        Summary of rate should be equal to 1. Example,  {"64": 0.2, "63": 0.8}.
        Key is code for environment and value is for rate.
        More on https://booking.yandex-team.ru/#/help?section=api-envs
    :param speed: type of speed mode. NORMAL is recommended
    :param start_ts: preferred start time in milliseconds
    :return: created booking or None
    """
    body = {
        "quotaSource": QUOTA_CODE,
        "speedMode": speed.name,
        "volumeDescription": {
            "volumeSources": [],
            "customVolume": {
                "amount": volume_amount,
                "environmentDistribution": environment_distribution,
            },
        },
        "startTsFrom": start_ts,
    }
    return estimate(title, body)


def book_st_filter(
    title, st_filter, speed, custom_volume, start_ts
):
    """

    :param title: name of the booking
    :param st_filter: link to Startrek filter.Example, "https://st.yandex-team.ru/issues/29390"
    :param speed: type of speed mode. NORMAL is recommended
    :param custom_volume: additional volume for the booking in БО
    :param start_ts: preferred start time in milliseconds
    :return: created booking or None
    """
    body = {
        "quotaSource": QUOTA_CODE,
        "speedMode": speed.name,
        "volumeDescription": {
            "volumeSources": [{"type": "ST_TICKET_FILTER", "values": [st_filter]}],
            "customVolume": custom_volume if custom_volume > 0 else None,
        },
        "startTsFrom": start_ts,
    }
    return estimate(title, body)


def book_st_tickets(
    title, st_tickets, speed, custom_volume, start_ts
):
    """

    :param title: name of the booking
    :param st_tickets: array of links to Startek tickets. Example, ["https://st.yandex-team.ru/TESTING-1", "https://st.yandex-team.ru/TESTING-2"]
    :param speed: type of speed mode. NORMAL is recommended
    :param custom_volume: additional volume for the booking in БО
    :param start_ts: preferred start time in milliseconds
    :return: created booking or None
    """
    body = {
        "quotaSource": QUOTA_CODE,
        "speedMode": speed.name,
        "volumeDescription": {
            "volumeSources": [{"type": "ST_TICKET", "values": st_tickets}],
            "customVolume": custom_volume if custom_volume > 0 else None,
        },
        "startTsFrom": start_ts,
    }
    return estimate(title, body)


def book_testpalm_version(
    title, versions, speed, custom_volume, start_ts
):
    """

    :param title: name of the booking
    :param versions:  array of links to Testpalm versions. Example, ["https://testpalm.yandex-team.ru/test/version/v_1", "https://testpalm.yandex-team.ru/test/version/v_2"]
    :param speed: type of speed mode. NORMAL is recommended
    :param custom_volume: additional volume for the booking in БО
    :param start_ts: preferred start time in milliseconds
    :return: created booking or None
    """
    body = {
        "quotaSource": QUOTA_CODE,
        "speedMode": speed.name,
        "volumeDescription": {
            "volumeSources": [{"type": "TEST_PALM", "values": versions}],
            "customVolume": custom_volume if custom_volume > 0 else None,
        },
        "startTsFrom": start_ts,
    }
    return estimate(title, body)


def get_start_time():
    """
    Sorts and validates time ranges
    If all valid, returns startTs of first time range
    :return:
    """
    global acceptable_time_ranges
    acceptable_time_ranges = sort_time_ranges(acceptable_time_ranges)
    validate_time_ranges(acceptable_time_ranges)
    return acceptable_time_ranges[0]["startTs"]

if __name__ == "__main__":
    # Example of booking process
    # First you need to get startTs for booking
    startTs = get_start_time()
    # Then choose speed mode
    starting_speed = Speed.NORMAL

    # # Example of booking custom volume
    # try:
    #     booking = book_custom_volume("Test from api", 10, {}, starting_speed, startTs)
    # except Exception as e:
    #     print(e)
    #
    # # Example of booking Startrek filter
    # try:
    #     booking1 = book_st_filter(
    #         "https://st.yandex-team.ru/issues/29390", starting_speed, 0, startTs
    #     )
    # except Exception as e:
    #     print(e)
    #
    # # Example of booking Startrek tickets
    # try:
    #     booking2 = book_st_tickets(
    #         ["https://st.yandex-team.ru/ASSESSORTEST-283"], starting_speed, 0, startTs,
    #     )
    # except Exception as e:
    #     print(e)

    # Example of booking Testpalm versions
    try:
        booking3 = book_testpalm_version(
            'test_booking',
            [
                "https://testpalm.yandex-team.ru/balanceassessors/version/debug_hitman_version_6"
            ],
            starting_speed,
            0,
            startTs,
        )
    except Exception as e:
        print(e)