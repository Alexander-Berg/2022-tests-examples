# coding: utf-8

"""
Tests which are independent from database (but need to check imports)
"""

import json
import datetime
import random

import collections

import pytest

import ads.watchman.timeline.api.lib.app as app
from ads.watchman.timeline.api.lib.modules.events import models, schemas, resource_manager

import ads.watchman.timeline.api.tests.helpers as helpers


@pytest.fixture(scope='session')
def flask_app_client():
    config = helpers.model_generators.TestingConfig()
    return app.make_app(config).test_client()


class TestCase(object):
    def __init__(self):
        self.method = None
        self.url = None
        self.headers = None
        self.params = None
        self.body = None
        self.status_code = None
        self.expected_response = None


class GetEventsByValidFilters(TestCase):
    def __init__(self, filter_params):
        super(GetEventsByValidFilters, self).__init__()
        self.method = 'GET'
        self.url = '/events'
        self.params = filter_params
        self.status_code = 200
        self.expected_response_schema = schemas.EventSchema(strict=True, many=True)


class PutValidEventCase(TestCase):
    def __init__(self, event_type):
        super(PutValidEventCase, self).__init__()

        self.method = 'PUT'
        self.url = '/events/{}'.format(event_type.name)

        schema = helpers.model_generators.TestModelGenerator.get_schema_class(event_type)
        event_serialized = schema(strict=True).dump(helpers.model_generators.TestModelGenerator.create_event(event_type=event_type))

        self.body = json.dumps(event_serialized.data)
        self.status_code = 200
        self.expected_response_schema = schemas.EventReferenceSchema(strict=True)


class PutNotValidEventCase(TestCase):
    def __init__(self, event_type):
        super(PutNotValidEventCase, self).__init__()

        self.method = 'PUT'
        self.url = '/events/{}'.format(event_type.name)
        self.body = json.dumps({})
        self.status_code = 422

        self.expected_response_schema = schemas.ValidationErrorSchema(strict=True)


def generate_valid_filters():
    r_manager = resource_manager.ResourceManager()
    for i in xrange(100):
        filter_params = {}

        start_time = random.choice(["MISSING_ATTR", None, datetime.datetime(2017, 10, 1)])

        if start_time != "MISSING_ATTR":
            filter_params["start_time"] = None if start_time is None else start_time.isoformat()

        end_time = random.choice(["MISSING_ATTR", None, datetime.datetime(2017, 10, 2)])

        if end_time != "MISSING_ATTR":
            filter_params["end_time"] = None if end_time is None else end_time.isoformat()

        event_type = random.choice(["MISSING_ATTR", None, [models.EventType.fiasco, models.EventType.holiday]])

        if event_type != "MISSING_ATTR":
            if event_type is None:
                filter_params["event_type"] = None
            elif isinstance(event_type, list):
                filter_params["event_type"] = [d.name for d in event_type]
            else:
                filter_params["event_type"] = event_type.name

        duration_type = random.choice(["MISSING_ATTR", None, [models.DurationType.permanent, models.DurationType.temporary]])

        if duration_type != "MISSING_ATTR":
            if duration_type is None:
                filter_params["duration_type"] = None
            elif isinstance(duration_type, list):
                filter_params["duration_type"] = [d.name for d in duration_type]
            else:
                filter_params["duration_type"] = duration_type.name

        source_type = random.choice(["MISSING_ATTR", None, r_manager.get_names('source_type')[0:2]])

        if source_type != "MISSING_ATTR":
            filter_params["source_type"] = source_type

        product_type = random.choice(["MISSING_ATTR", None, r_manager.get_names('product_type')[0:2]])

        if product_type != "MISSING_ATTR":
            filter_params["product_type"] = product_type

        geo_type = random.choice(["MISSING_ATTR", None, r_manager.get_names('geo_type')[0:2]])

        if geo_type != "MISSING_ATTR":
            filter_params["geo_type"] = geo_type

        page_type = random.choice(["MISSING_ATTR", None, r_manager.get_names('page_type')[0:2]])
        if page_type != "MISSING_ATTR":
            filter_params["page_type"] = page_type

        yield filter_params


REQUESTS = collections.OrderedDict(
    [('PutNotValidEventCase__{}'.format(event_type.name), PutNotValidEventCase(event_type)) for event_type in models.EventType] +
    [('PutValidEventCase__{}'.format(event_type.name), PutValidEventCase(event_type)) for event_type in models.EventType]
)


@pytest.mark.parametrize(u'request', REQUESTS.values(), ids=REQUESTS.keys())
def test_status_code(flask_app_client, request):
    response = flask_app_client.open(method=request.method, content_type=u'application/json',
                                     path=request.url, data=request.body, query_string=request.params)
    assert response.status_code == request.status_code


def test_status_code_filters(flask_app_client):
    for params in generate_valid_filters():
        request = GetEventsByValidFilters(params)
        response = flask_app_client.open(method=request.method, content_type=u'application/json',
                                         path=request.url, data=request.body, query_string=request.params)
        assert response.status_code == request.status_code


@pytest.mark.parametrize(u'request', REQUESTS.values(), ids=REQUESTS.keys())
def test_response(flask_app_client, request):
    response = flask_app_client.open(method=request.method, content_type=u'application/json',
                                     path=request.url, data=request.body, query_string=request.params)

    result = json.loads(response.data)
    assert request.expected_response_schema.validate(result) == {}


def test_response_filters(flask_app_client):
    for params in generate_valid_filters():
        request = GetEventsByValidFilters(params)
        response = flask_app_client.open(method=request.method, content_type=u'application/json',
                                         path=request.url, data=request.body, query_string=request.params)
        result = json.loads(response.data)
        assert request.expected_response_schema.validate(result) == {}
