from requests import ConnectionError
from checks import constants
import json
import sys
import requests


RETRY_COUNT = 10


def parse_args():
    args = sys.argv
    params = dict()
    for (index, element) in enumerate(args):
        if element == "--test-param":
            split_data = args[index + 1].split('=')
            params[split_data[0]] = split_data[1]

    return params


def post_request_for_validation(data, url):
    data = data["events"][0]
    data = {"status": data["status"], "metadata": json.loads(data["description"]),
            "check_type": data["service"]}
    count = 0
    while count < RETRY_COUNT:
        try:
            return requests.request("POST", "{}/v1/scheme-validators".format(url), data=json.dumps(data),
                                    headers={"Content-Type": "application/json"}).json()

        except requests.ConnectionError:
            count += 1
    else:
        return {"message": "Failed to connect to {}".format(url), "result": False}


def validate_canonized_data(data, params):
    url = params.get(constants.URL_ARGUMENT)
    if not url:
        url = constants.DEFAULT_URL

    recent_request = post_request_for_validation(data=data, url=url)
    import logging
    logging.info(recent_request)
    assert recent_request["result"] is True


def make_canonization(check_result, expected_result):
    assert expected_result == check_result

    params = parse_args()
    if params.get(constants.CANONIZATION_ARGUMENT):
        validate_canonized_data(check_result, params)

    return check_result
