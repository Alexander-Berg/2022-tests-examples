# coding: utf-8

import unittest
import json

from ads.ml_monitoring.api.lib.app import make_app
from ads.ml_monitoring.api.lib.config import TestingConfig


class TestUpdates(unittest.TestCase):
    def setUp(self):
        self.maxDiff = None
        config = TestingConfig()
        self.client = make_app(config).test_client()
        self.db = config.DB_CLASS

    def test_200(self):
        self.assertEqual(self.client.get("production/updates").status_code, 200)

    def test_200_with_filter_last(self):
        self.assertEqual(self.client.get("production/updates?filter=last").status_code, 200)

    def test_419_with_filter_last_and_id(self):
        self.assertEqual(self.client.get("production/updates?filter=last&lm_id=1").status_code, 419)

    def test_419_with_filter_last_and_date(self):
        self.assertEqual(self.client.get("production/updates?filter=last&date=2017-10-10").status_code, 419)

    def test_200_with_filter_all_and_id(self):
        self.assertEqual(self.client.get("production/updates?filter=all&lm_id=1").status_code, 200)

    def test_200_with_filter_all_and_date_and_id(self):
        self.assertEqual(self.client.get("production/updates?filter=all&date=2017-10-10&lm_id=1").status_code, 200)

    def test_200_with_filter_all_and_date(self):
        self.assertEqual(self.client.get("production/updates?filter=all&date=2017-10-10").status_code, 200)

    def test_419_with_filter_all(self):
        self.assertEqual(self.client.get("production/updates?filter=all").status_code, 419)

    def test_419_with_filter_all_and_malformat_date(self):
        self.assertEqual(self.client.get("production/updates?filter=all&date=20171010").status_code, 419)

    def test_api_answer_for_last(self):
        result = sorted(json.loads(self.client.get("production/updates").data)['events'])
        etalon = sorted(self.db.last_response())
        self.assertEqual(result, etalon)


class TestUpdate(unittest.TestCase):
    def setUp(self):
        self.maxDiff = None
        config = TestingConfig()
        self.client = make_app(config).test_client()
        self.db = config.DB_CLASS

    def test_200(self):
        self.assertEqual(self.client.get("production/update/21").status_code, 200)

    def test_419_not_exists(self):
        self.assertEqual(self.client.get("production/update/100500").status_code, 419)

    def test_api_answer(self):
        result = json.loads(self.client.get("production/update/21").data)['event']
        self.assertEqual(result, self.db.row_extended_response())


class TestModel(unittest.TestCase):
    def setUp(self):
        self.maxDiff = None
        config = TestingConfig()
        self.client = make_app(config).test_client()
        self.db = config.DB_CLASS

    def test_200(self):
        self.assertEqual(self.client.get("production/model/1").status_code, 200)

    def test_419_not_exists(self):
        self.assertEqual(self.client.get("production/model/100500").status_code, 419)

    def test_api_answer(self):
        result = json.loads(self.client.get("production/model/2").data)
        self.assertEqual(result, self.db.row_model_response())


class TestExtractionsWorkflowsByPeriod(unittest.TestCase):
    def setUp(self):
        self.maxDiff = None
        config = TestingConfig()
        self.client = make_app(config).test_client()
        self.nirvana = config.NIRVANA_CLASS

    def test_200(self):
        from_date = "2017-09-05T18:00:00"
        to_date = "2017-09-05T18:10:00"
        url = "learn/workflows?from_date={}&to_date={}".format(from_date, to_date)
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)

        result = json.loads(response.data)['flows']
        etalon = [self.nirvana.get_response()]
        self.assertEqual(result, etalon)


class TestExtractionWorkflowsByIds(unittest.TestCase):
    def setUp(self):
        self.maxDiff = None
        config = TestingConfig()
        self.client = make_app(config).test_client()
        self.nirvana = config.NIRVANA_CLASS

    def test_200(self):
        response = self.client.post(
            'learn/workflows',
            data=json.dumps({
                'instances': [
                    {"workflow_id": 1, "instance_id": 1},
                    {"workflow_id": 1, "instance_id": 2}
                ]
            }),
            content_type='application/json')
        self.assertEqual(response.status_code, 200)

        result = json.loads(response.data)['flows']
        etalon = [self.nirvana.get_response(), self.nirvana.get_response()]
        self.assertEqual(result, etalon)


class TestWorkflow(unittest.TestCase):
    def setUp(self):
        self.maxDiff = None
        config = TestingConfig()
        self.client = make_app(config).test_client()
        self.nirvana = config.NIRVANA_CLASS

    def test_200(self):
        response = self.client.get("learn/workflow/1")
        self.assertEqual(response.status_code, 200)

        result = json.loads(response.data)
        etalon = self.nirvana.get_response()
        self.assertEqual(result, etalon)


class TestWorkflowInstance(unittest.TestCase):
    def setUp(self):
        self.maxDiff = None
        config = TestingConfig()
        self.client = make_app(config).test_client()
        self.nirvana = config.NIRVANA_CLASS

    def test_200(self):
        response = self.client.get("learn/workflow/1/1")
        self.assertEqual(response.status_code, 200)

        result = json.loads(response.data)
        etalon = self.nirvana.get_response()
        self.assertEqual(result, etalon)
