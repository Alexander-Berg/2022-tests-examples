import json
import logging
from parameterized import parameterized
import requests
import unittest
from library.python import resource

from .common import (
    API_ENDPOINT,
    ADD_CALENDAR_PLANNING_TASK_QUERY,
    CLOUD_TYPE,
    create_calendar_planning_task,
    make_request,
    post_cp_task,
    wait_cp_task
)

RTC_CLOUD_PARAMS = {
    'task_timeout': 300,
    'invalid_apikey_error': requests.codes.unauthorized
}
AWS_CLOUD_PARAMS = {
    'task_timeout': 700,
    'invalid_apikey_error': requests.codes.forbidden
}
TEST_PARAMS = RTC_CLOUD_PARAMS if CLOUD_TYPE == 'rtc' else AWS_CLOUD_PARAMS


class CalendarPlanningApikeyTest(unittest.TestCase):
    def test_invalid_apikey(self):
        url = API_ENDPOINT + ADD_CALENDAR_PLANNING_TASK_QUERY
        task = create_calendar_planning_task()
        resp = make_request("post", url, json=task, params={'apikey': "invalid_apikey"})
        self.assertEqual(resp.status_code, TEST_PARAMS['invalid_apikey_error'])

    @parameterized.expand(['aptrade', 'dan', 'megapolis_v2', 'megapolis_v3'])
    def test_ok(self, task_name):
        task = json.loads(resource.find(f'cp_examples/cp_{task_name}_request.json'))
        task_id = post_cp_task(task)['id']
        logging.info(f'Waiting for {task_name} task {task_id}')
        wait_cp_task(task_id, timeout=TEST_PARAMS['task_timeout'])
