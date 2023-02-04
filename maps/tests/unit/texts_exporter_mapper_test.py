# encoding=utf-8
from datetime import datetime
import pytest

from maps.infopoint.tools.texts_exporter.lib.texts_exporter import (
    TextsMapper)


def test_export_infopoint():
    record = {
        "client_ip": "2a02:6b8:c1c:100:10d:4861:4228:0",
        "timestamp": 1592895716,
        "uuid": None,
        "miid": None,
        "value": ("eyJhdXRob3IiOiJ3d3cuZ2F0aS1vbmxpbmUucnUiLCJvd25lciI6Ind3dy"
                  "5nYXRpLW9ubGluZS5ydSIsInRhZ3MiOlsicmVjb25zdHJ1Y3Rpb24iXSwiY"
                  "mVnaW4iOjE1ODE0NTEyMDAsImVuZCI6MTU5NDA2NTU5OSwiZGVzY3JpcHRp"
                  "b24iOiLQoNCw0LHQvtGC0Ysg0L/QviDRgNC10LrQvtC90YHRgtGA0YPQutG"
                  "G0LjQuCDRgtC10L/Qu9C+0YHQtdGC0LguICjRjdGC0LDQvyAyLjEpIiwicm"
                  "VnaW9ucyI6WzIsMTAxNzQsMjI1LDEwMDAwXSwicG9zaXRpb24iOnsibG9uI"
                  "jozMC40MTIyOSwibGF0Ijo1OS44Njc0MX19"),
        "user": "urn:yandex-uid:1",
        "key": "0063b1a0-423d-56e0-bf94-38337ef55759",
        "operation_type": "point_added",
    }
    texts_mapper = TextsMapper(datetime.utcfromtimestamp(1592895000),
                               datetime.utcfromtimestamp(1592896000))
    result = next(texts_mapper(record))
    assert result == {
        'type': 'infopoint',
        "text": "Работы по реконструкции теплосети. (этап 2.1)",
        "tags": ["reconstruction"],
        'key': '0063b1a0-423d-56e0-bf94-38337ef55759'
    }


def test_export_comment():
    record = {
        "client_ip": None,
        "timestamp": 1589905101,
        "uuid": None,
        "miid": None,
        "value": ("eyJjb21tZW50X2lkIjoiMCIsImF1dGhvciI6InVybjp5YW5kZXgtdWlkO"
                  "jg5MjY2NzE1NSIsImNvbnRlbnQiOiLQqNC80YPQv9GB0LggIiwidXBkYX"
                  "RlZCI6MTU4OTkwNTEwMX0="),
        "user": "urn:yandex-uid:1",
        "key": "89795288-4816-4a16-b611-342af9763585",
        "operation_type": "comment_added",
    }
    texts_mapper = TextsMapper(datetime.utcfromtimestamp(1589905000),
                               datetime.utcfromtimestamp(1589906000))
    result = next(texts_mapper(record))
    assert result == {
        "type": "comment",
        "text": "Шмупси ",
        "key": "89795288-4816-4a16-b611-342af9763585/0"
    }


def test_beyond_the_time_segment():
    record = {
        "client_ip": None,
        "timestamp": 1589905101,
        "uuid": None,
        "miid": None,
        "value": ("eyJjb21tZW50X2lkIjoiMCIsImF1dGhvciI6InVybjp5YW5kZXgtdWlkO"
                  "jg5MjY2NzE1NSIsImNvbnRlbnQiOiLQqNC80YPQv9GB0LggIiwidXBkYX"
                  "RlZCI6MTU4OTkwNTEwMX0="),
        "user": "urn:yandex-uid:1",
        "key": "89795288-4816-4a16-b611-342af9763585",
        "operation_type": "comment_added",
    }
    texts_mapper = TextsMapper(datetime.utcfromtimestamp(1589905200),
                               datetime.utcfromtimestamp(1589906000))
    with pytest.raises(StopIteration):
        next(texts_mapper(record))

    texts_mapper = TextsMapper(datetime.utcfromtimestamp(1589904900),
                               datetime.utcfromtimestamp(1589905000))
    with pytest.raises(StopIteration):
        next(texts_mapper(record))
