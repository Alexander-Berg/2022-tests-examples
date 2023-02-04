# coding=utf-8
import pytest
import json
import marshmallow

import yatest

import ads.watchman.timeline.api.lib.app as app
from ads.watchman.timeline.api.lib.modules.tickets import api
import ads.watchman.timeline.api.tests.helpers as helpers


MOCK_RESPONSE_FILE = yatest.common.source_path('ads/watchman/timeline/api/tests/resources/startrek/dictionary_with_mock_responces.json')


def mock_notification():
    with open(MOCK_RESPONSE_FILE, 'rb') as f:
        return json.load(f)["notification"]

TEST_STARTREK_NOTIFICATION = mock_notification()


@pytest.fixture(scope='session')
def flask_app_client():
    config = helpers.TestingConfig()
    return app.make_app(config).test_client()


def test_validation_error_comment():
    data = {}

    class TestSchema(marshmallow.Schema):
        test_field = marshmallow.fields.String(required=True)

    try:
        TestSchema(strict=True).load(data)
    except marshmallow.ValidationError as e:
        expected_str = u"test\n* test_field\n  * Missing data for required field.\n"
        assert expected_str == api.comment_from_validation_error(e, u"test")
