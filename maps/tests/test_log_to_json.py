import json
from datetime import datetime
import log_to_json


def test_parse_simple_line():
    line = '[2019-12-13 16:55:48.561] <seq=0> <pid=295> info: Starting server'
    record = log_to_json.LogRecord()
    record.parse(line)
    excepted_record = {
        'time': '2019-12-13 16:55:48.561',
        'timestamp': 1576245348561,
        'seq': 0,
        'pid': 295,
        'tid': None,
        'reqid': None,
        'level': 'info',
        'message': 'Starting server',
        'duration': None,
        'service_id': '',
    }
    assert record.to_json(sort_keys=True) == json.dumps(excepted_record, sort_keys=True)


def test_parse_in_request_line():
    line = '[2019-12-14 00:02:41.594] <seq=8503> <req=6119> <tid=,7f21ac23-99972e38-c23ba74-52e4b40> <pid=295> info:       20 ms: Querying'
    record = log_to_json.LogRecord()
    record.parse(line)
    excepted_record = {
        'time': '2019-12-14 00:02:41.594',
        'timestamp': 1576270961594,
        'seq': 8503,
        'pid': 295,
        'tid': ',7f21ac23-99972e38-c23ba74-52e4b40',
        'reqid': 6119,
        'level': 'info',
        'message': 'Querying',
        'duration': 20,
        'service_id': '',
    }
    assert record.to_json(sort_keys=True) == json.dumps(excepted_record, sort_keys=True)


def test_parse_request_line():
    line = '[2019-12-13 16:56:44.518] <seq=14> <req=10> <pid=295> info: ::1 GET /unistat? => HTTP 200 (10 ms, 500 b)'
    record = log_to_json.LogRecord()
    record.parse(line)
    excepted_record = {
        'time': '2019-12-13 16:56:44.518',
        'timestamp': 1576245404518,
        'seq': 14,
        'pid': 295,
        'tid': None,
        'reqid': 10,
        'level': 'info',
        'message': '',
        'duration': 10,
        'service_id': '',
        'ip': '::1',
        'method': 'GET',
        'uri': '/unistat?',
        'status_code': 200,
        'size': 500,
    }
    assert record.to_json(sort_keys=True) == json.dumps(excepted_record, sort_keys=True)


def test_parse_line_with_prefix():
    line = '1;2;3;[2019-12-13 16:55:48.561] <tid=abc> <pid=295> info: Starting server'
    record = log_to_json.LogRecord()
    record.parse(line)
    excepted_record = {
        'time': '2019-12-13 16:55:48.561',
        'timestamp': 1576245348561,
        'seq': None,
        'pid': 295,
        'tid': "abc",
        'reqid': None,
        'level': 'info',
        'message': 'Starting server',
        'duration': None,
        'service_id': '',
    }
    assert record.to_json(sort_keys=True) == "1;2;3;" + json.dumps(excepted_record, sort_keys=True)


def test_parse_wrong_line():
    lines = [
        '[2019-12-13 16:55:48.561] <tid=abc> <pid=295>',
        '[2019-12-13 16:55:48.561] <tid=abc> <pid=aa295> info: Starting server',
        '[2019-12-13 16:55:48.561] <req=6119> <seq=8503> <tid=,7f21ac23-99972e38-c23ba74-52e4b40> <pid=295> info: Starting server',
    ]
    for line in lines:
        record = log_to_json.LogRecord()
        try:
            record.parse(line)
        except ValueError as e:
            assert str(e).startswith("Line not match ")
        else:
            assert False


def test_fake_record():
    line = 'message in invalid format\n\n'
    record = log_to_json.LogRecord()
    now = datetime.now()
    record.make_fake_record(line, now=now)
    excepted_record = {
        'time': now.isoformat(sep=' ', timespec='milliseconds'),
        'timestamp': int(1000 * now.timestamp()),
        'seq': None,
        'pid': 0,
        'tid': None,
        'reqid': None,
        'level': '',
        'message': 'message in invalid format',
        'duration': None,
        'service_id': '',
    }
    assert record.to_json(sort_keys=True) == json.dumps(excepted_record, sort_keys=True)
