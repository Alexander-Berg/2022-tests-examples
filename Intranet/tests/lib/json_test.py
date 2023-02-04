import pytest
from unittest.mock import patch, Mock
from datetime import date, time, datetime, timezone
from decimal import Decimal
from django.http import HttpResponse
import json

from intranet.vconf.src.lib.json import make_json_response, responding_json


def test_make_json():
    result = make_json_response(
        {
            'date': date(2017, 7, 1),
            'datetime': datetime(2018, 1, 1, 10, 22, 33, 123123, tzinfo=timezone.utc),
            'time': time(12, 22),
            'decimal': Decimal('1.22'),
            'str': 'Привет Jon',
            'gen': (n for n in range(2)),
        }
    )

    assert isinstance(result, HttpResponse)
    assert json.loads(result.content.decode('utf-8')) == {
        'response_code': 200,
        'response_text': {
            'date': '2017-07-01',
            'datetime': '2018-01-01T10:22:33.123123+00:00',
            'time': '12:22:00',
            'decimal': '1.22',
            'str': 'Привет Jon',
            'gen': [0, 1],
        }
    }
    assert result.status_code == 200

    print(result)


params = (
    (HttpResponse('a'), None, None),
    ('foo', 1, ('foo', 200)),
    (('foo', 500), 1, ('foo', 500))
)


@pytest.mark.parametrize('resp,answer,call', params)
def test_responding_json(resp, answer, call):
    mjr = Mock(return_value=1)
    with patch('intranet.vconf.src.lib.json.make_json_response', mjr):
        @responding_json
        def foo(request):
            return resp

        if answer is None:
            assert foo(1) == resp
        else:
            assert foo(1) == 1

        if call:
            mjr.assert_called_once_with(*call)
